package com.jobportal.controllers.admin;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.User;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> roleFilterComboBox;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private TableView<User> usersTable;
    
    @FXML
    private TableColumn<User, String> userIdColumn;
    
    @FXML
    private TableColumn<User, String> usernameColumn;
    
    @FXML
    private TableColumn<User, String> fullNameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> phoneColumn;
    
    @FXML
    private TableColumn<User, String> roleColumn;
    
    @FXML
    private TableColumn<User, String> registrationDateColumn;
    
    @FXML
    private TableColumn<User, String> lastLoginColumn;
    
    @FXML
    private TableColumn<User, Void> actionsColumn;
    
    @FXML
    private Pagination usersPagination;
    
    private DashboardController parentController;
    private UserService userService;
    
    private String searchQuery = "";
    private User.Role selectedRole = null;
    private int itemsPerPage = 20;
    private int totalUsers = 0;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        
        // Initialize role filter
        roleFilterComboBox.getItems().addAll(
                "All Roles",
                "Job Seeker",
                "Employer",
                "Admin"
        );
        roleFilterComboBox.getSelectionModel().select(0);
        
        // Set up table columns
        initializeTableColumns();
        
        // Set up pagination
        usersPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            loadUsers(newVal.intValue());
        });
        
        // Set up role filter listener
        roleFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.equals("All Roles")) {
                    selectedRole = null;
                } else if (newVal.equals("Job Seeker")) {
                    selectedRole = User.Role.JOB_SEEKER;
                } else if (newVal.equals("Employer")) {
                    selectedRole = User.Role.EMPLOYER;
                } else if (newVal.equals("Admin")) {
                    selectedRole = User.Role.ADMIN;
                }
                
                // Reset pagination to first page and reload users
                usersPagination.setCurrentPageIndex(0);
                loadUsers(0);
            }
        });
    }
    
    private void initializeTableColumns() {
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getId() != null ? data.getValue().getId().toString() : ""));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        
        roleColumn.setCellValueFactory(data -> {
            User.Role role = data.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.toString() : "");
        });
        
        registrationDateColumn.setCellValueFactory(data -> {
            LocalDateTime date = data.getValue().getRegistrationDate();
            if (date == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });
        
        lastLoginColumn.setCellValueFactory(data -> {
            LocalDateTime date = data.getValue().getLastLogin();
            if (date == null) return new SimpleStringProperty("Never");
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });
        
        // Set up actions column with edit and delete buttons
        actionsColumn.setCellFactory(createActionsColumnCellFactory());
    }
    
    private Callback<TableColumn<User, Void>, TableCell<User, Void>> createActionsColumnCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            
            {
                editButton.getStyleClass().add("button-icon");
                editButton.setGraphic(new FontIcon("fas-edit"));
                editButton.setTooltip(new Tooltip("Edit User"));
                
                deleteButton.getStyleClass().add("button-icon");
                deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
                deleteButton.setTooltip(new Tooltip("Delete User"));
                
                // Set button actions
                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });
                
                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttonsBox = new HBox(10, editButton, deleteButton);
                    setGraphic(buttonsBox);
                }
            }
        };
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Load users
        loadUsers(0);
    }
    
    @FXML
    private void onSearchButtonClick() {
        searchQuery = searchField.getText().trim();
        usersPagination.setCurrentPageIndex(0);
        loadUsers(0);
    }
    
    @FXML
    private void onAddUserClick() {
        try {
            showUserDialog(null);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open user dialog", 
                    "An error occurred while trying to open the add user dialog.");
        }
    }
    
    private void loadUsers(int page) {
        // Create a background task to avoid freezing the UI
        Task<UserData> usersTask = new Task<>() {
            @Override
            protected UserData call() throws Exception {
                UserData data = new UserData();
                
                // Get total count for pagination
                data.totalCount = userService.getUserCount(searchQuery, selectedRole);
                
                // Get paginated users
                data.users = userService.getPaginatedUsers(
                        searchQuery, 
                        selectedRole, 
                        page, 
                        itemsPerPage
                );
                
                return data;
            }
        };
        
        usersTask.setOnSucceeded(event -> {
            UserData data = usersTask.getValue();
            totalUsers = data.totalCount;
            
            // Update pagination
            int pageCount = (int) Math.ceil((double) totalUsers / itemsPerPage);
            usersPagination.setPageCount(Math.max(1, pageCount));
            
            // Update table
            Platform.runLater(() -> {
                usersTable.setItems(FXCollections.observableArrayList(data.users));
            });
        });
        
        usersTask.setOnFailed(event -> {
            Throwable exception = usersTask.getException();
            exception.printStackTrace();
            
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users", 
                    "An error occurred while loading users: " + exception.getMessage());
        });
        
        // Start the task
        new Thread(usersTask).start();
    }
    
    private void editUser(User user) {
        try {
            showUserDialog(user);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open user dialog", 
                    "An error occurred while trying to open the edit user dialog.");
        }
    }
    
    private void deleteUser(User user) {
        // Confirm deletion
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete User");
        confirmDialog.setContentText("Are you sure you want to delete the user '" + user.getUsername() + "'? This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = userService.deleteUser(user.getId());
                
                if (success) {
                    // Reload users
                    loadUsers(usersPagination.getCurrentPageIndex());
                    
                    showAlert(Alert.AlertType.INFORMATION, "Success", "User Deleted", 
                            "The user has been deleted successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete User", 
                            "An error occurred while trying to delete the user.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete User", 
                        "An error occurred: " + e.getMessage());
            }
        }
    }
    
    private void showUserDialog(User user) throws IOException {
        // Load the user dialog
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/admin/dialogs/userDialog.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add User" : "Edit User");
        dialog.getDialogPane().setContent(loader.load());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Get the controller and initialize it
        UserDialogController controller = loader.getController();
        controller.initializeDialog(user);
        
        // Show the dialog and process the result
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            User resultUser = controller.processResult();
            
            if (resultUser != null) {
                // Reload users to reflect changes
                loadUsers(usersPagination.getCurrentPageIndex());
                
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                        user == null ? "User Added" : "User Updated", 
                        user == null ? "The user has been added successfully." : "The user has been updated successfully.");
            }
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Helper class for user data
    private static class UserData {
        List<User> users;
        int totalCount;
    }
}
