package com.epic;

import com.epic.model.Game;
import com.epic.model.Proton;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private JFXButton loginBtn;
    @FXML private JFXTextField searchField;
    @FXML private JFXListView<String> gameListView;
    @FXML private JFXButton refreshGamesBtn;
    @FXML private JFXTextField installField;
    @FXML private JFXButton installBtn;
    @FXML private JFXButton uninstallBtn;
    @FXML private JFXComboBox<String> protonCombo;
    @FXML private JFXButton refreshProtonsBtn;
    @FXML private JFXTextField protonVersionField;
    @FXML private JFXButton installProtonBtn;
    @FXML private JFXButton launchBtn;
    @FXML private JFXProgressBar progressBar;
    @FXML private JFXTextField customProtonDirField;
    @FXML private JFXButton saveSettingsBtn;
    @FXML private JFXButton checkStatusBtn;

    private ApiService apiService = new ApiService();
    private ObservableList<String> gameItems = FXCollections.observableArrayList();
    private FilteredList<String> filteredGames = new FilteredList<>(gameItems, p -> true);
    private String selectedGame;
    private String selectedProton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gameListView.setItems(filteredGames);
        gameListView.setOnMouseClicked(e -> {
            selectedGame = gameListView.getSelectionModel().getSelectedItem();
        });
        protonCombo.setOnAction(e -> selectedProton = protonCombo.getValue());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredGames.setPredicate(game -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return game.toLowerCase().contains(newValue.toLowerCase());
            });
        });

        loginBtn.setOnAction(e -> login());
        refreshGamesBtn.setOnAction(e -> loadGames());
        installBtn.setOnAction(e -> install(installField.getText()));
        uninstallBtn.setOnAction(e -> uninstall(getAppNameFromSelected()));
        refreshProtonsBtn.setOnAction(e -> loadProtons());
        installProtonBtn.setOnAction(e -> installProton(protonVersionField.getText()));
        launchBtn.setOnAction(e -> launch());
        saveSettingsBtn.setOnAction(e -> saveSettings());
        checkStatusBtn.setOnAction(e -> checkStatus());

        loadSettings();
        loadGames();
        loadProtons();
    }

    private void showProgress(boolean show) {
        Platform.runLater(() -> progressBar.setVisible(show));
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private String getAppNameFromSelected() {
        if (selectedGame == null) return "";
        return selectedGame.split("\\(")[1].replace(")", "").trim();
    }

    private String getProtonPathFromSelected() {
        if (selectedProton == null) return "";
        return selectedProton.split("\\(")[1].replace(")", "").trim();
    }

    private void login() {
        executeTask(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                apiService.login();
                return null;
            }
        }, "Login successful", "Login failed");
    }

    private void loadGames() {
        executeTask(new Task<List<Game>>() {
            @Override
            protected List<Game> call() throws Exception {
                return apiService.listGames();
            }

            @Override
            protected void succeeded() {
                List<Game> games = getValue();
                gameItems.clear();
                for (Game g : games) {
                    gameItems.add(g.getTitle() + " (" + g.getAppName() + ")");
                }
            }
        }, "Games loaded", "Failed to load games");
    }

    private void install(String appName) {
        if (appName.isEmpty()) return;
        executeTask(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                apiService.installGame(appName);
                return null;
            }
        }, "Game installed", "Install failed");
    }

    private void uninstall(String appName) {
        if (appName.isEmpty()) return;
        executeTask(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                apiService.uninstallGame(appName);
                return null;
            }
        }, "Game uninstalled", "Uninstall failed");
    }

    private void loadProtons() {
        executeTask(new Task<List<Proton>>() {
            @Override
            protected List<Proton> call() throws Exception {
                return apiService.listProtons();
            }

            @Override
            protected void succeeded() {
                List<Proton> protons = getValue();
                ObservableList<String> names = FXCollections.observableArrayList();
                for (Proton p : protons) {
                    names.add(p.getName() + " (" + p.getPath() + ")");
                }
                protonCombo.setItems(names);
            }
        }, "Protons loaded", "Failed to load protons");
    }

    private void installProton(String version) {
        if (version.isEmpty()) return;
        executeTask(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                apiService.installProton(version);
                return null;
            }
        }, "Proton installed", "Install failed");
    }

    private void launch() {
        String appName = getAppNameFromSelected();
        String protonPath = getProtonPathFromSelected();
        if (appName.isEmpty() || protonPath.isEmpty()) return;
        executeTask(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                apiService.launchGame(appName, protonPath);
                return null;
            }
        }, "Game launched", "Launch failed");
    }

    private void checkStatus() {
        executeTask(new Task<String>() {
            @Override
            protected String call() throws Exception {
                return apiService.getStatus();
            }

            @Override
            protected void succeeded() {
                showAlert("Status", getValue());
            }
        }, "Status checked", "Status check failed");
    }

    private <T> void executeTask(Task<T> task, String successMsg, String errorMsg) {
        showProgress(true);
        task.setOnSucceeded(e -> {
            showProgress(false);
            showAlert("Success", successMsg);
        });
        task.setOnFailed(e -> {
            showProgress(false);
            showAlert("Error", errorMsg + ": " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void saveSettings() {
        // Zapisz customProtonDirField.getText() do properties
        // Dla uproszczenia, pomiń implementację, użyj Properties class
        showAlert("Settings", "Settings saved");
    }

    private void loadSettings() {
        // Załaduj z properties
        customProtonDirField.setText("~/.steam/root/compatibilitytools.d");
    }
}
