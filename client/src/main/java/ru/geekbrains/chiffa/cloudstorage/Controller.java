package ru.geekbrains.chiffa.cloudstorage;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class Controller implements Initializable {
    public static final String WARNING_TITLE = "Warning!";
    public static final String SUCCESS_TITLE = "Success!";
    private final BooleanProperty loadingInProgress = new SimpleBooleanProperty(false);
    private final BooleanProperty unsignedIn = new SimpleBooleanProperty(true);
    private Client client;

    //upload unit
    @FXML
    private TextField filePathTextField;
    @FXML
    private Button uploadButton;
    @FXML
    private Button browseButton;

    //work with storageFiles unit
    @FXML
    private Button renameButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button downloadButton;
    @FXML
    private ListView<String> storageFilesListView;

    //sign in unit
    @FXML
    private TextField usernameTextField;
    @FXML
    private Button signInButton;

    public void openBrowseDialog(ActionEvent actionEvent) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose file");
        Window window = ((Node) actionEvent.getTarget()).getScene().getWindow();
        File chosenFile = fc.showOpenDialog(window);
        if (chosenFile != null) {
            filePathTextField.setText(chosenFile.getPath());
        }
    }

    public String openRenameDialog(String oldName) {
        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle("Rename");
        dialog.setContentText("Enter new filename:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public void initialize(URL location, ResourceBundle resources) {
        uploadButton.disableProperty()
                .bind(Bindings.length(filePathTextField.textProperty()).isEqualTo(0)
                        .or(loadingInProgress)
                        .or(unsignedIn));
        browseButton.disableProperty().bind(loadingInProgress.or(unsignedIn));
        filePathTextField.disableProperty().bind(loadingInProgress.or(unsignedIn));

        storageFilesListView.disableProperty().bind(loadingInProgress.or(unsignedIn));
        refreshButton.disableProperty().bind(loadingInProgress.or(unsignedIn));

        renameButton.disableProperty().bind(
                Bindings.isEmpty(storageFilesListView.getSelectionModel().getSelectedItems())
                        .or(loadingInProgress)
                        .or(unsignedIn));
        deleteButton.disableProperty().bind(
                Bindings.isEmpty(storageFilesListView.getSelectionModel().getSelectedItems())
                        .or(loadingInProgress)
                        .or(unsignedIn));
        downloadButton.disableProperty().bind(
                Bindings.isEmpty(storageFilesListView.getSelectionModel().getSelectedItems())
                        .or(loadingInProgress)
                        .or(unsignedIn));

        usernameTextField.disableProperty().bind(loadingInProgress.or(unsignedIn.not()));
        signInButton.disableProperty().bind(loadingInProgress.or(unsignedIn.not()));
    }

    public void upload() {
        loadingInProgress.set(true);
        client.upload(Paths.get(filePathTextField.getText())).thenAccept(files -> {
            try {
                Platform.runLater(() -> setFilesListView(files));
            } catch (Exception e) {
                showAlert(WARNING_TITLE, e.getMessage());
                e.printStackTrace();
            }
            loadingInProgress.set(false);
        });
    }

    public void refresh() {
        loadingInProgress.set(true);
        client.refresh().thenAccept(files -> {
            Platform.runLater(() -> setFilesListView(files));
            loadingInProgress.set(false);
        });
    }

    public void signIn() {
        client = new Client(usernameTextField.getText(), Executors.newSingleThreadExecutor());
        refresh();
        unsignedIn.set(false);
    }

    public void delete() {
        loadingInProgress.set(true);

        client.delete(storageFilesListView.getSelectionModel().getSelectedItem()).thenAccept(files -> {
            Platform.runLater(() -> setFilesListView(files));
            loadingInProgress.set(false);
        });
    }

    public void download() {
        loadingInProgress.set(true);
        String fileName = storageFilesListView.getSelectionModel().getSelectedItem();
        client.download(fileName).thenAccept(success -> {
            if (success) {
                Platform.runLater(() -> showAlert(SUCCESS_TITLE, String.format("File %s downloaded successfully.", fileName)));
            }
            loadingInProgress.set(false);
        });
    }

    public void rename() {
        String fileName = storageFilesListView.getSelectionModel().getSelectedItem();
        String newFileName = openRenameDialog(fileName);
        if (newFileName != null) {
            if (!isFilenameAcceptable(newFileName)) {
                showAlert(WARNING_TITLE, "Unacceptable filename.");
            } else {
                loadingInProgress.set(true);
                client.rename(fileName, newFileName).thenAccept(files -> {
                    setFilesListView(files);
                    loadingInProgress.set(false);
                });
            }
        }
    }

    private void setFilesListView(List<String> listFiles) {
        storageFilesListView.setItems(FXCollections.observableArrayList(listFiles));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isFilenameAcceptable(String fileName) {
        return !(fileName.length() == 0
                || fileName.contains("<")
                || fileName.contains(">")
                || fileName.contains(":")
                || fileName.contains("|")
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("*")
                || fileName.contains("?")
                || fileName.endsWith("\\s")
                || fileName.endsWith("."));
    }
}
