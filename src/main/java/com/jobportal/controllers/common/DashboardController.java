package com.jobportal.controllers.common;

import com.jobportal.App;
import com.jobportal.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class DashboardController {
    
    @FXML
    public VBox navMenu;
    
    @FXML
    public Pane contentArea;
    
    @FXML
    public Label usernameLabel;
    
    @FXML
    public Label userRoleLabel;
    
    @FXML
    public Text pageTitle;
    
    @FXML
    public TextField searchField;
    
    public User currentUser;
    protected String activeNavId;
    private Map<String, Node> navButtons = new HashMap<>();
    
    public void initialize(User user) {
        this.currentUser = user;
        
        // Set user info in sidebar
        if (usernameLabel != null && user != null) {
            usernameLabel.setText(user.getFullName());
        }
        
        if (userRoleLabel != null && user != null && user.getRole() != null) {
            userRoleLabel.setText(user.getRole().toString());
        }
        
        // Set up search field
        if (searchField != null) {
            searchField.setOnAction(e -> {
                String searchTerm = searchField.getText().trim();
                if (!searchTerm.isEmpty()) {
                    handleSearch(searchTerm);
                }
            });
        }
    }
    
    protected void addNavButton(String id, String text, String iconLiteral, Runnable action) {
        Button button = new Button(text);
        button.setId(id + "Nav");
        button.getStyleClass().add("nav-menu-item");
        
        // Set icon
        FontIcon icon = new FontIcon(iconLiteral);
        button.setGraphic(icon);
        
        // Set action
        button.setOnAction(e -> {
            setActiveNav(id);
            action.run();
        });
        
        // Add to map for reference
        navButtons.put(id, button);
        
        // Add to menu
        navMenu.getChildren().add(button);
    }
    
    protected void setActiveNav(String navId) {
        // Remove active class from current active
        if (activeNavId != null && navButtons.containsKey(activeNavId)) {
            navButtons.get(activeNavId).getStyleClass().remove("nav-menu-item-active");
        }
        
        // Set new active
        activeNavId = navId;
        if (navButtons.containsKey(navId)) {
            navButtons.get(navId).getStyleClass().add("nav-menu-item-active");
        }
    }
    
    protected void setPageTitle(String title) {
        if (pageTitle != null) {
            pageTitle.setText(title);
        }
    }
    
    protected void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Node content = loader.load();
            
            // Check if controller needs parent controller
            Object controller = loader.getController();
            if (controller instanceof DashboardControllerAware) {
                ((DashboardControllerAware) controller).setParentController(this);
            }
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
            // Show error message
        }
    }
    
    protected void handleSearch(String searchTerm) {
        // To be implemented by subclasses
    }
    
    @FXML
    private void onLogoutClick() {
        try {
            // Navigate back to login page
            App.setRoot("login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Interface for controllers that need access to the parent dashboard controller
    public interface DashboardControllerAware {
        void setParentController(DashboardController controller);
    }
}
