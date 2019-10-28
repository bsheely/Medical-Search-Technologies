package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.Component;
import com.mst.machineLearningModel.OrganizationName;
import com.mst.machineLearningModel.User;
import com.mst.machineLearningUi.service.OrganizationService;
import com.mst.machineLearningUi.service.UserService;
import com.vaadin.event.Action;
import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static com.mst.machineLearningUi.view.MachineLearningUi.LOGIN;

@SpringView(name = LOGIN)
class LoginTab extends Panel implements View, Action.Handler {
    private final UserService userService;
    private final OrganizationService organizationService;
    private TextField username;
    private PasswordField password;
    private Action submitAction;

    LoginTab(UserService userService, OrganizationService organizationService) {
        this.userService = userService;
        this.organizationService = organizationService;
        final int CAPTION_WIDTH = 5;
        setSizeFull();
        VerticalLayout loginForm = new VerticalLayout();
        loginForm.setSizeUndefined();
        loginForm.setStyleName("v-formlayout");
        Image logo = new Image(null, new ThemeResource("images/mst.png"));

        username = new TextField();
        username.focus();
        Label usernameLabel = new Label("Username");
        usernameLabel.setStyleName("caption-right");
        usernameLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout usernameLayout = new HorizontalLayout(usernameLabel, username);
        usernameLayout.setComponentAlignment(usernameLabel, Alignment.MIDDLE_RIGHT);

        password = new PasswordField();
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyleName("caption-right");
        passwordLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout passwordLayout = new HorizontalLayout(passwordLabel, password);
        passwordLayout.setComponentAlignment(passwordLabel, Alignment.MIDDLE_RIGHT);

        Button submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButtonClick(username.getValue(), password.getValue()));
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        Label spacer = new Label("");
        spacer.setStyleName("caption-right");
        spacer.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout buttonLayout = new HorizontalLayout(spacer, submitButton);
        buttonLayout.setComponentAlignment(spacer, Alignment.MIDDLE_RIGHT);

        submitAction = new ShortcutAction(null, ShortcutAction.KeyCode.ENTER, (int[]) null);
        loginForm.addComponents(logo, usernameLayout, passwordLayout, buttonLayout);
        VerticalLayout verticalLayout = new VerticalLayout(loginForm); // required for setComponentAlignment
        verticalLayout.setSizeFull();
        verticalLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
        addActionHandler(this);
        setContent(verticalLayout);
        setStyleName("v-app");
    }

    @Override
    public Action[] getActions(Object target, Object sender) {
        return new Action[]{submitAction};
    }

    @Override
    public void handleAction(Action action, Object sender, Object target) {
        if (action == submitAction && password.getValue() != null && !password.getValue().isEmpty())
            handleSubmitButtonClick(username.getValue(), password.getValue());
        else
            password.focus();
    }

    void clear() {
        username.clear();
        password.clear();
    }

    private void handleSubmitButtonClick(String username, String password) {
        User user = userService != null ? userService.getUser(username) : null;
        if (user != null) {
            if (!user.isActive()) {
                Notification.show(user.getUsername() + " has been deactivated", Notification.Type.ERROR_MESSAGE);
                return;
            }
            String userPassword = user.getPassword();
            if (IsPasswordValid(password, userPassword)) {
                MachineLearningUi mstUI = ((MachineLearningUi) UI.getCurrent());
                OrganizationName organizationName = user.getOrganizationName();
                if (organizationName == null) {
                    Notification.show("No organization has been assigned to " + username, Notification.Type.WARNING_MESSAGE);
                    return;
                }
                if (!organizationService.isActive(organizationName)) {
                    Notification.show(user.getUsername() + " belongs to an organization that has been deactivated", Notification.Type.WARNING_MESSAGE);
                    return;
                }
                mstUI.setOrganizationName(organizationName);
                mstUI.setUser(user);
                List<Component> viewableTabs = organizationService.getViewableTabs();
                viewableTabs.addAll(userService.getViewableTabs());
                if (viewableTabs.isEmpty())
                    Notification.show(organizationName + " has no viewable tabs", Notification.Type.ERROR_MESSAGE);
                else
                    mstUI.configureTabs(viewableTabs);
            } else
                Notification.show("Invalid Password", Notification.Type.WARNING_MESSAGE);
        } else
            Notification.show("Unable to verify username", Notification.Type.ERROR_MESSAGE);
    }

    private boolean IsPasswordValid(String plainText, String encrypted) {
        return new BCryptPasswordEncoder().matches(plainText, encrypted);
    }
}
