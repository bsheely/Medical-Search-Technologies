package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.*;
import com.mst.machineLearningModel.Component;
import com.mst.machineLearningUi.service.*;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.mst.machineLearningModel.ApplicationName.MACHINE_LEARNING_UI;
import static com.mst.machineLearningModel.ExecutionStatus.SUCCESS;
import static com.mst.machineLearningModel.Component.ACCOUNTS;
import static com.mst.machineLearningUi.view.AccountsTab.Entity.*;
import static com.mst.machineLearningUi.view.AccountsTab.Operation.*;
import static com.vaadin.ui.Alignment.TOP_RIGHT;
import static com.vaadin.ui.Grid.SelectionMode.*;

public class AccountsTab extends VerticalLayout implements Button.ClickListener {
    public enum Operation {
        CREATE {public String toString() { return "Create"; }},
        UPDATE {public String toString() { return "Update"; }},
        DEACTIVATE {public String toString() { return "Deactivate"; }},
    }
    public enum Entity {
        ORGANIZATION {public String toString() { return "Organization"; }},
        ORGANIZATION_CONFIGURATION {public String toString() { return "Organization Configuration"; }},
        USER {public String toString() { return "User"; }},
        USER_LOCATION {public String toString() { return "User Location"; }}
    }
    private final UserService userService;
    private final OrganizationService organizationService;
    private final OrganizationSpecificConfigurationService organizationSpecificConfigurationService;
    private final UserLocationService userLocationService;
    private final AuditService auditService;
    private ComboBox<Entity> entityInput;
    private ComboBox<Operation> operationInput;
    private ComboBox<OrganizationName> organizationNameInput;
    private HorizontalLayout organizationNameLayout;
    private TwinColSelect<Component> viewableTabInput;
    private TwinColSelect<Component> optionalTabInput;
    private HorizontalLayout viewableTabLayout;
    private TwinColSelect<Component> viewableReportsInput;
    private TwinColSelect<Component> optionalReportsInput;
    private HorizontalLayout viewableReportsLayout;
    private TextField nameInput;
    private HorizontalLayout nameLayout;
    private TextField lastNameInput;
    private HorizontalLayout lastNameLayout;
    private TextField usernameInput;
    private HorizontalLayout usernameLayout;
    private PasswordField passwordInput;
    private HorizontalLayout passwordLayout;
    private TextField emailInput;
    private HorizontalLayout emailLayout;
    private TwinColSelect<Component> userViewableTabInput;
    private HorizontalLayout userViewableTabLayout;
    private TwinColSelect<Component> userViewableReportsInput;
    private HorizontalLayout userViewableReportsLayout;
    private Button submitButton;
    private TextField cityInput;
    private ComboBox<Location.State> stateInput;
    private ListSelect<String> userInput;
    private HorizontalLayout cityLayout;
    private HorizontalLayout stateLayout;
    private HorizontalLayout userLayout;
    private TextArea classes;
    private TextArea uneditableClasses;
    private HorizontalLayout classesLayout;
    private HorizontalLayout updateClassesLayout;
    private TextArea graphRelationships;
    private TextArea uneditableGraphRelationships;
    private HorizontalLayout graphRelationshipsLayout;
    private HorizontalLayout updateGraphRelationshipsLayout;
    private Grid<Organization> organizationGrid;
    private Grid<User> userGrid;
    private Grid<UserLocation> userLocationGrid;
    private Grid<OrganizationSpecificConfiguration> organizationConfigurationGrid;
    private ConfirmationWindow confirmationWindow;
    private UserLocation selectedUserLocation;
    private User selectedUser;
    private Organization selectedOrganization;
    private OrganizationSpecificConfiguration selectedOrganizationConfiguration;
    private boolean isInitializingViewableValues;
    private boolean isInitializingOptionalValues;
    private List<OrganizationName> creatableOrganizations;
    private List<OrganizationName> creatableOrganizationConfigurations;

    AccountsTab(TabSheet tabSheet, UserService userService, OrganizationService organizationService, OrganizationSpecificConfigurationService organizationSpecificConfigurationService, UserLocationService userLocationService, AuditService auditService) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.organizationSpecificConfigurationService = organizationSpecificConfigurationService;
        this.userLocationService = userLocationService;
        this.auditService = auditService;
        final int CAPTION_WIDTH = 10;

        entityInput = new ComboBox<>();
        entityInput.setWidth(18, Unit.EM);
        entityInput.setEmptySelectionAllowed(false);
        entityInput.setTextInputAllowed(false);
        entityInput.setItems(organizationService.getOrganizationName() == OrganizationName.MST ? new ArrayList<>(Arrays.asList(ORGANIZATION, ORGANIZATION_CONFIGURATION, USER, USER_LOCATION)) : new ArrayList<>(Arrays.asList(USER, USER_LOCATION)));
        entityInput.setSelectedItem(organizationService.getOrganizationName() == OrganizationName.MST ? ORGANIZATION : USER);
        entityInput.addValueChangeListener(event -> handleEntityValueChange());
        Label entityLabel = new Label("Entity");
        entityLabel.setStyleName("caption-right");
        entityLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout entityLayout = new HorizontalLayout(entityLabel, entityInput);
        entityLayout.setComponentAlignment(entityLabel, Alignment.MIDDLE_RIGHT);

        operationInput = new ComboBox<>();
        operationInput.setWidth(10, Unit.EM);
        operationInput.setEmptySelectionAllowed(false);
        operationInput.setTextInputAllowed(false);
        operationInput.setItems(new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
        operationInput.setSelectedItem(UPDATE);
        operationInput.addValueChangeListener(event -> handleOperationValueChange());
        Label operationLabel = new Label("Operation");
        operationLabel.setStyleName("caption-right");
        operationLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout operationLayout = new HorizontalLayout(operationLabel, operationInput);
        operationLayout.setComponentAlignment(operationLabel, Alignment.MIDDLE_RIGHT);

        //Organization
        organizationNameInput = new ComboBox<>();
        organizationNameInput.setWidth(18, Unit.EM);
        creatableOrganizations = new ArrayList<>();
        List<OrganizationName> organizations = OrganizationName.getOrganizationNames();
        for (OrganizationName organization : organizations)
            if (!organizationService.isOrganizationFound(organization))
                creatableOrganizations.add(organization);
        organizationNameInput.setItems(creatableOrganizations);
        organizationNameInput.setRequiredIndicatorVisible(true);
        organizationNameInput.setEmptySelectionAllowed(false);
        organizationNameInput.setTextInputAllowed(false);
        organizationNameInput.addValueChangeListener(event -> handleInputValueChange());
        Label organizationNameLabel = new Label("Organization Name");
        organizationNameLabel.setStyleName("caption-right");
        organizationNameLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        organizationNameLayout = new HorizontalLayout(organizationNameLabel, organizationNameInput);
        organizationNameLayout.setComponentAlignment(organizationNameLabel, Alignment.MIDDLE_RIGHT);
        organizationNameLayout.setVisible(false);
        viewableTabInput = new TwinColSelect<>();
        viewableTabInput.setRequiredIndicatorVisible(true);
        viewableTabInput.setItems(Component.getTabTypes());
        viewableTabInput.addSelectionListener(event -> handleViewableTabSelection());
        Label viewableTabLabel = new Label("Viewable Tabs");
        viewableTabLabel.setStyleName("caption-right");
        viewableTabLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        optionalTabInput = new TwinColSelect<>();
        optionalTabInput.setItems(Component.getTabTypes());
        optionalTabInput.addSelectionListener(event -> handleOptionalTabSelection());
        Label optionalTabLabel = new Label("Optional Tabs");
        optionalTabLabel.setStyleName("caption-right");
        optionalTabLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        viewableTabLayout = new HorizontalLayout(viewableTabLabel, viewableTabInput, optionalTabLabel, optionalTabInput);
        viewableTabLayout.setComponentAlignment(viewableTabLabel, Alignment.MIDDLE_RIGHT);
        viewableTabLayout.setComponentAlignment(optionalTabLabel, Alignment.MIDDLE_RIGHT);
        viewableTabLayout.setVisible(false);
        viewableReportsInput = new TwinColSelect<>();
        viewableReportsInput.setItems(Component.getClientReportTypes());
        viewableReportsInput.addValueChangeListener(event -> handleViewableReportSelection());
        Label viewableReportsLabel = new Label("Viewable Reports");
        viewableReportsLabel.setStyleName("caption-right");
        viewableReportsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        optionalReportsInput = new TwinColSelect<>();
        optionalReportsInput.setItems(Component.getClientReportTypes());
        optionalReportsInput.addValueChangeListener(event -> handleOptionalReportSelection());
        Label optionalReportsLabel = new Label("Optional Reports");
        optionalReportsLabel.setStyleName("caption-right");
        optionalReportsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        viewableReportsLayout = new HorizontalLayout(viewableReportsLabel, viewableReportsInput, optionalReportsLabel, optionalReportsInput);
        viewableReportsLayout.setComponentAlignment(viewableReportsLabel, Alignment.MIDDLE_RIGHT);
        viewableReportsLayout.setComponentAlignment(optionalReportsLabel, Alignment.MIDDLE_RIGHT);
        viewableReportsLayout.setVisible(false);
        organizationGrid = new Grid<>();
        organizationGrid.setSizeFull();
        organizationGrid.setSelectionMode(SINGLE);
        organizationGrid.addSelectionListener(event -> handleOrganizationGridSelection());
        loadOrganizationGrid();

        //Organization Configuration
        creatableOrganizationConfigurations = new ArrayList<>();
        for (OrganizationName organization : organizations)
            if (!organizationSpecificConfigurationService.isOrganizationFound(organization))
                creatableOrganizationConfigurations.add(organization);
        classes = new TextArea();
        classes.setRows(15);
        classes.setWidth(20, Unit.EM);
        classes.addValueChangeListener(event -> handleInputValueChange());
        Label classesLabel = new Label("Classes");
        classesLabel.setStyleName("caption-right");
        classesLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        Button addClassesButton = new Button(">", (Button.ClickListener) event -> addClasses());
        addClassesButton.addStyleName(ValoTheme.BUTTON_TINY);
        uneditableClasses = new TextArea();
        uneditableClasses.setRows(15);
        uneditableClasses.setWidth(20, Unit.EM);
        uneditableClasses.setReadOnly(true);
        VerticalLayout addClassesButtonLayout = new VerticalLayout(new Label(), addClassesButton, new Label());
        addClassesButtonLayout.setMargin(false);
        addClassesButtonLayout.setHeight("100%");
        addClassesButtonLayout.setComponentAlignment(addClassesButton, Alignment.MIDDLE_CENTER);
        updateClassesLayout = new HorizontalLayout(addClassesButtonLayout, uneditableClasses);
        classesLayout = new HorizontalLayout(classesLabel, classes, updateClassesLayout);
        classesLayout.setComponentAlignment(classesLabel, TOP_RIGHT);
        classesLayout.setVisible(false);
        graphRelationships = new TextArea();
        graphRelationships.setRows(15);
        graphRelationships.setWidth(20, Unit.EM);
        graphRelationships.addValueChangeListener(event -> handleInputValueChange());
        Label graphRelationshipsLabel = new Label("Graph Relationships");
        graphRelationshipsLabel.setStyleName("caption-right");
        graphRelationshipsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        Button addGraphRelationshipsButton = new Button(">", (Button.ClickListener) event -> addGraphRelationships());
        addGraphRelationshipsButton.addStyleName(ValoTheme.BUTTON_TINY);
        uneditableGraphRelationships = new TextArea();
        uneditableGraphRelationships.setRows(15);
        uneditableGraphRelationships.setWidth(20, Unit.EM);
        uneditableGraphRelationships.setReadOnly(true);
        VerticalLayout addGraphRelationshipsButtonLayout = new VerticalLayout(new Label(), addGraphRelationshipsButton, new Label());
        addGraphRelationshipsButtonLayout.setMargin(false);
        addGraphRelationshipsButtonLayout.setHeight("100%");
        addGraphRelationshipsButtonLayout.setComponentAlignment(addGraphRelationshipsButton, Alignment.MIDDLE_CENTER);
        updateGraphRelationshipsLayout = new HorizontalLayout(addGraphRelationshipsButtonLayout, uneditableGraphRelationships);
        graphRelationshipsLayout = new HorizontalLayout(graphRelationshipsLabel, graphRelationships, updateGraphRelationshipsLayout);
        graphRelationshipsLayout.setComponentAlignment(graphRelationshipsLabel, TOP_RIGHT);
        graphRelationshipsLayout.setVisible(false);
        organizationConfigurationGrid = new Grid<>();
        organizationConfigurationGrid.setVisible(false);
        organizationConfigurationGrid.setSizeFull();

        //User
        nameInput = new TextField();
        nameInput.setRequiredIndicatorVisible(true);
        nameInput.addValueChangeListener(event -> handleInputValueChange());
        Label nameLabel = new Label("Name");
        nameLabel.setStyleName("caption-right");
        nameLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        nameLayout = new HorizontalLayout(nameLabel, nameInput);
        nameLayout.setComponentAlignment(nameLabel, Alignment.MIDDLE_RIGHT);
        nameLayout.setVisible(false);
        lastNameInput = new TextField();
        Label lastNameLabel = new Label("Last Name");
        lastNameLabel.setStyleName("caption-right");
        lastNameLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        lastNameLayout = new HorizontalLayout(lastNameLabel, lastNameInput);
        lastNameLayout.setComponentAlignment(lastNameLabel, Alignment.MIDDLE_RIGHT);
        lastNameLayout.setVisible(false);
        usernameInput = new TextField();
        usernameInput.setRequiredIndicatorVisible(true);
        usernameInput.addValueChangeListener(event -> handleInputValueChange());
        Label usernameLabel = new Label("Username");
        usernameLabel.setStyleName("caption-right");
        usernameLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        usernameLayout = new HorizontalLayout(usernameLabel, usernameInput);
        usernameLayout.setComponentAlignment(usernameLabel, Alignment.MIDDLE_RIGHT);
        usernameLayout.setVisible(false);
        passwordInput = new PasswordField();
        passwordInput.setRequiredIndicatorVisible(true);
        passwordInput.addValueChangeListener(event -> handleInputValueChange());
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyleName("caption-right");
        passwordLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        passwordLayout = new HorizontalLayout(passwordLabel, passwordInput);
        passwordLayout.setComponentAlignment(passwordLabel, Alignment.MIDDLE_RIGHT);
        passwordLayout.setVisible(false);
        emailInput = new TextField();
        emailInput.setWidth(20, Unit.EM);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyleName("caption-right");
        emailLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        emailLayout = new HorizontalLayout(emailLabel, emailInput);
        emailLayout.setComponentAlignment(emailLabel, Alignment.MIDDLE_RIGHT);
        emailLayout.setVisible(false);
        userViewableTabInput = new TwinColSelect<>();
        userViewableTabInput.setItems(organizationService.getOptionalViewableTabs());
        Label userViewableTabLabel = new Label("Viewable Tabs");
        userViewableTabLabel.setStyleName("caption-right");
        userViewableTabLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        userViewableTabLayout = new HorizontalLayout(userViewableTabLabel, userViewableTabInput);
        userViewableTabLayout.setComponentAlignment(userViewableTabLabel, Alignment.MIDDLE_RIGHT);
        userViewableTabLayout.setVisible(false);
        userViewableReportsInput = new TwinColSelect<>();
        userViewableReportsInput.setItems(organizationService.getOptionalViewableReports());
        Label userViewableReportsLabel = new Label("Viewable Reports");
        userViewableReportsLabel.setStyleName("caption-right");
        userViewableReportsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        userViewableReportsLayout = new HorizontalLayout(userViewableReportsLabel, userViewableReportsInput);
        userViewableReportsLayout.setComponentAlignment(userViewableReportsLabel, Alignment.MIDDLE_RIGHT);
        userViewableReportsLayout.setVisible(false);
        userGrid = new Grid<>();
        userGrid.setVisible(false);
        userGrid.setSizeFull();

        //user location
        cityInput = new TextField();
        cityInput.setRequiredIndicatorVisible(true);
        cityInput.addValueChangeListener(event -> handleInputValueChange());
        Label cityLabel = new Label("City");
        cityLabel.setStyleName("caption-right");
        cityLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        cityLayout = new HorizontalLayout(cityLabel, cityInput);
        cityLayout.setComponentAlignment(cityLabel, Alignment.MIDDLE_RIGHT);
        cityLayout.setVisible(false);
        stateInput = new ComboBox<>();
        stateInput.setItems(Location.State.getStates());
        stateInput.setRequiredIndicatorVisible(true);
        stateInput.setEmptySelectionAllowed(false);
        stateInput.setTextInputAllowed(false);
        stateInput.addValueChangeListener(event -> handleInputValueChange());
        Label stateLabel = new Label("State");
        stateLabel.setStyleName("caption-right");
        stateLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        stateLayout = new HorizontalLayout(stateLabel, stateInput);
        stateLayout.setComponentAlignment(stateLabel, Alignment.MIDDLE_RIGHT);
        stateLayout.setVisible(false);
        userInput = new ListSelect<>();
        userInput.setItems(userService.getUserNames(organizationService.getOrganizationName()));
        userInput.setRequiredIndicatorVisible(true);
        userInput.addValueChangeListener(event -> handleInputValueChange());
        Label userLabel = new Label("User");
        userLabel.setStyleName("caption-right");
        userLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        userLayout = new HorizontalLayout(userLabel, userInput);
        userLayout.setComponentAlignment(userLabel, Alignment.MIDDLE_RIGHT);
        userLayout.setVisible(false);
        userLocationGrid = new Grid<>();
        userLocationGrid.setVisible(false);
        userLocationGrid.setSizeFull();

        submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButtonClick());
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        submitButton.setEnabled(false);
        Label spacer = new Label("");
        spacer.setStyleName("caption-right");
        spacer.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout buttonLayout = new HorizontalLayout(spacer, submitButton);
        buttonLayout.setComponentAlignment(spacer, Alignment.MIDDLE_RIGHT);

        addComponent(entityLayout);
        addComponent(operationLayout);
        addComponent(nameLayout);
        addComponent(lastNameLayout);
        addComponent(usernameLayout);
        addComponent(passwordLayout);
        addComponent(emailLayout);
        addComponent(userViewableTabLayout);
        addComponent(userViewableReportsLayout);
        addComponent(organizationNameLayout);
        addComponent(viewableTabLayout);
        addComponent(viewableReportsLayout);
        addComponent(cityLayout);
        addComponent(stateLayout);
        addComponent(userLayout);
        addComponents(classesLayout, graphRelationshipsLayout);
        addComponent(buttonLayout);
        addComponents(organizationGrid, organizationConfigurationGrid, userGrid, userLocationGrid);
        tabSheet.addTab(this, ACCOUNTS.toString());
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getButton().getCaption().equalsIgnoreCase("yes"))
            switch (entityInput.getValue()) {
                case USER:
                    recordActivity(DEACTIVATE + " " + selectedUser.serialize());
                    selectedUser.setActive(false);
                    selectedUser.setDateDeactivated(LocalDate.now());
                    userService.saveUser(selectedUser);
                    loadUserGrid();
                    break;
                case ORGANIZATION:
                    recordActivity(DEACTIVATE + " " + selectedOrganization.serialize());
                    selectedOrganization.setActive(false);
                    selectedOrganization.setDateDeactivated(LocalDate.now());
                    organizationService.saveOrganization(selectedOrganization);
                    loadOrganizationGrid();
                    break;
                case USER_LOCATION:
                    recordActivity(DEACTIVATE + " " + selectedUserLocation.serialize());
                    selectedUserLocation.setActive(false);
                    selectedUserLocation.setDateDeactivated(LocalDate.now());
                    userLocationService.saveUserLocation(selectedUserLocation);
                    loadUserLocationGrid();
            }
        confirmationWindow.close();
    }

    private void addClasses() {
        List<String> combinedClasses = new ArrayList<>(uneditableClasses.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(uneditableClasses.getValue().split("\\r?\\n")));
        combinedClasses.addAll(classes.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(classes.getValue().split("\\r?\\n")));
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : combinedClasses)
            stringBuilder.append(str).append("\n");
        uneditableClasses.setValue(stringBuilder.toString());
        classes.clear();
    }

    private void addGraphRelationships() {
        List<String> combinedGraphRelationships = new ArrayList<>(uneditableGraphRelationships.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(uneditableGraphRelationships.getValue().split("\\r?\\n")));
        combinedGraphRelationships.addAll(graphRelationships.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(graphRelationships.getValue().split("\\r?\\n")));
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : combinedGraphRelationships)
            stringBuilder.append(str).append("\n");
        uneditableGraphRelationships.setValue(stringBuilder.toString());
        graphRelationships.clear();
    }

    private void hideInputLayouts() {
        organizationNameLayout.setVisible(false);
        viewableTabLayout.setVisible(false);
        viewableReportsLayout.setVisible(false);
        classesLayout.setVisible(false);
        graphRelationshipsLayout.setVisible(false);
        nameLayout.setVisible(false);
        lastNameLayout.setVisible(false);
        usernameLayout.setVisible(false);
        passwordLayout.setVisible(false);
        emailLayout.setVisible(false);
        userViewableTabLayout.setVisible(false);
        userViewableReportsLayout.setVisible(false);
        cityLayout.setVisible(false);
        stateLayout.setVisible(false);
        userLayout.setVisible(false);
    }

    private void hideGrids() {
        organizationGrid.setVisible(false);
        organizationConfigurationGrid.setVisible(false);
        userGrid.setVisible(false);
        userLocationGrid.setVisible(false);
    }

    private void clearUserInputs() {
        nameInput.clear();
        lastNameInput.clear();
        usernameInput.clear();
        passwordInput.clear();
        emailInput.clear();
        userViewableTabInput.clear();
        userViewableReportsInput.clear();
    }

    private void clearOrganizationInputs() {
        organizationNameInput.clear();
        viewableTabInput.clear();
        optionalTabInput.clear();
        viewableReportsInput.clear();
        optionalReportsInput.clear();
    }

    private void clearUserLocationInputs() {
        cityInput.clear();
        stateInput.clear();
        userInput.clear();
    }

    private void clearOrganizationConfigurationInputs() {
        organizationNameInput.clear();
        classes.clear();
        graphRelationships.clear();
    }

    private void handleEntityValueChange() {
        hideInputLayouts();
        hideGrids();
        switch (entityInput.getValue()) {
            case ORGANIZATION:
                organizationNameInput.clear();
                organizationNameInput.setItems(creatableOrganizations);
                if (operationInput.getValue() == CREATE || isOrganizationGridSelected()) {
                    organizationNameLayout.setVisible(operationInput.getValue() == CREATE);
                    viewableTabLayout.setVisible(true);
                    viewableReportsLayout.setVisible(true);
                }
                operationInput.setItems(new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
                organizationGrid.setVisible(true);
                loadOrganizationGrid();
                if (operationInput.getValue() != CREATE) {
                    organizationGrid.setSelectionMode(SINGLE);
                    organizationGrid.addSelectionListener(event -> handleOrganizationGridSelection());
                }
                break;
            case ORGANIZATION_CONFIGURATION:
                organizationNameInput.clear();
                organizationNameInput.setItems(creatableOrganizationConfigurations);
                if (operationInput.getValue() == CREATE || isOrganizationConfigurationGridSelected()) {
                    organizationNameLayout.setVisible(operationInput.getValue() == CREATE);
                    classesLayout.setVisible(true);
                    graphRelationshipsLayout.setVisible(true);
                }
                operationInput.setItems(new ArrayList<>(Arrays.asList(CREATE, UPDATE)));
                organizationConfigurationGrid.setVisible(true);
                loadOrganizationConfigurationGrid();
                if (operationInput.getValue() != CREATE) {
                    organizationConfigurationGrid.setSelectionMode(SINGLE);
                    organizationConfigurationGrid.addSelectionListener(event -> handleOrganizationConfigurationGridSelection());
                    submitButton.setEnabled(false);
                }
                break;
            case USER:
                if (operationInput.getValue() == CREATE || isUserGridSelected()) {
                    nameLayout.setVisible(true);
                    lastNameLayout.setVisible(true);
                    usernameLayout.setVisible(true);
                    passwordLayout.setVisible(true);
                    emailLayout.setVisible(true);
                    userViewableTabLayout.setVisible(true);
                    userViewableReportsLayout.setVisible(true);
                }
                operationInput.setItems(new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
                userGrid.setVisible(true);
                loadUserGrid();
                if (operationInput.getValue() != CREATE) {
                    userGrid.setSelectionMode(SINGLE);
                    userGrid.addSelectionListener(event -> handleUserGridSelection());
                }
                break;
            case USER_LOCATION:
                if (operationInput.getValue() == CREATE || isUserLocationGridSelected()) {
                    cityLayout.setVisible(true);
                    stateLayout.setVisible(true);
                    userLayout.setVisible(true);
                }
                operationInput.setItems(new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
                userLocationGrid.setVisible(true);
                loadUserLocationGrid();
                if (operationInput.getValue() != CREATE) {
                    userLocationGrid.setSelectionMode(SINGLE);
                    userLocationGrid.addSelectionListener(event -> handleUserLocationGridSelection());
                }
        }
    }

    private void handleOperationValueChange() {
        hideInputLayouts();
        switch (operationInput.getValue()) {
            case CREATE:
                switch (entityInput.getValue()) {
                    case USER:
                        nameLayout.setVisible(true);
                        lastNameLayout.setVisible(true);
                        usernameLayout.setVisible(true);
                        passwordLayout.setVisible(true);
                        emailLayout.setVisible(true);
                        userViewableTabLayout.setVisible(true);
                        userViewableReportsLayout.setVisible(true);
                        clearUserInputs();
                        userGrid.setSelectionMode(NONE);
                        break;
                    case ORGANIZATION_CONFIGURATION:
                        clearOrganizationConfigurationInputs();
                        organizationNameLayout.setVisible(true);
                        classesLayout.setVisible(true);
                        graphRelationshipsLayout.setVisible(true);
                        organizationConfigurationGrid.setSelectionMode(NONE);
                        classes.setRequiredIndicatorVisible(true);
                        graphRelationships.setRequiredIndicatorVisible(true);
                        updateClassesLayout.setVisible(false);
                        updateGraphRelationshipsLayout.setVisible(false);
                        break;
                    case USER_LOCATION:
                        cityLayout.setVisible(true);
                        stateLayout.setVisible(true);
                        userLayout.setVisible(true);
                        clearUserLocationInputs();
                        userGrid.setSelectionMode(NONE);
                        break;
                    case ORGANIZATION:
                    default:
                        clearOrganizationInputs();
                        viewableTabLayout.setVisible(true);
                        viewableReportsLayout.setVisible(true);
                        organizationNameLayout.setVisible(true);
                        organizationGrid.setSelectionMode(NONE);
                }
                break;
            case UPDATE:
                switch (entityInput.getValue()) {
                    case USER:
                        nameLayout.setVisible(isUserLocationGridSelected());
                        lastNameLayout.setVisible(isUserLocationGridSelected());
                        usernameLayout.setVisible(isUserLocationGridSelected());
                        passwordLayout.setVisible(isUserLocationGridSelected());
                        emailLayout.setVisible(isUserLocationGridSelected());
                        userViewableTabLayout.setVisible(isUserLocationGridSelected());
                        userViewableReportsLayout.setVisible(isUserLocationGridSelected());
                        userGrid.setSelectionMode(SINGLE);
                        userGrid.addSelectionListener(event -> handleUserGridSelection());
                        break;
                    case ORGANIZATION:
                        organizationNameLayout.setVisible(false);
                        viewableTabLayout.setVisible(isOrganizationGridSelected());
                        viewableReportsLayout.setVisible(isOrganizationGridSelected());
                        organizationGrid.setSelectionMode(SINGLE);
                        organizationGrid.addSelectionListener(event -> handleOrganizationGridSelection());
                        break;
                    case ORGANIZATION_CONFIGURATION:
                        organizationNameLayout.setVisible(false);
                        classesLayout.setVisible(isOrganizationConfigurationGridSelected());
                        graphRelationshipsLayout.setVisible(isOrganizationConfigurationGridSelected());
                        organizationConfigurationGrid.setSelectionMode(SINGLE);
                        organizationConfigurationGrid.addSelectionListener(event -> handleOrganizationConfigurationGridSelection());
                        classes.setRequiredIndicatorVisible(false);
                        graphRelationships.setRequiredIndicatorVisible(false);
                        updateClassesLayout.setVisible(true);
                        updateGraphRelationshipsLayout.setVisible(true);
                        submitButton.setEnabled(false);
                        break;
                    case USER_LOCATION:
                        cityLayout.setVisible(isUserLocationGridSelected());
                        stateLayout.setVisible(isUserLocationGridSelected());
                        userLayout.setVisible(isUserLocationGridSelected());
                        userLocationGrid.setSelectionMode(SINGLE);
                        userLocationGrid.addSelectionListener(event -> handleUserLocationGridSelection());
                }
                break;
            case DEACTIVATE:
                switch (entityInput.getValue()) {
                    case USER:
                        userGrid.setSelectionMode(SINGLE);
                        userGrid.addSelectionListener(event -> handleUserGridSelection());
                        break;
                    case ORGANIZATION:
                        organizationGrid.setSelectionMode(SINGLE);
                        organizationGrid.addSelectionListener(event -> handleOrganizationGridSelection());
                        break;
                    case ORGANIZATION_CONFIGURATION:
                        organizationConfigurationGrid.setSelectionMode(SINGLE);
                        organizationConfigurationGrid.addSelectionListener(event -> handleOrganizationConfigurationGridSelection());
                        break;
                    case USER_LOCATION:
                        userLocationGrid.setSelectionMode(SINGLE);
                        userLocationGrid.addSelectionListener(event -> handleUserLocationGridSelection());
                }
        }
    }

    private void handleInputValueChange() {
        switch (entityInput.getValue()) {
            case ORGANIZATION:
                submitButton.setEnabled(!organizationNameInput.isEmpty() && !viewableTabInput.getValue().isEmpty());
                break;
            case ORGANIZATION_CONFIGURATION:
                switch (operationInput.getValue()) {
                    case CREATE:
                        submitButton.setEnabled(!organizationNameInput.isEmpty() && !classes.getValue().isEmpty() && !graphRelationships.getValue().isEmpty());
                        break;
                    case UPDATE:
                        Optional<OrganizationSpecificConfiguration> reference = organizationConfigurationGrid.getSelectionModel().getFirstSelectedItem();
                        submitButton.setEnabled(reference.isPresent() && classes.isEmpty() && graphRelationships.isEmpty());
                }
                break;
            case USER:
                submitButton.setEnabled(!nameInput.isEmpty() && !usernameInput.isEmpty() && !passwordInput.isEmpty() && !organizationNameInput.isEmpty());
                break;
            case USER_LOCATION:
                submitButton.setEnabled(!cityInput.isEmpty());
        }
    }

    private List<Component> getTabs() {
        List<Component> tabs = new ArrayList<>();
        switch (operationInput.getValue()) {
            case CREATE:
                tabs = Component.getTabTypes();
                break;
            case UPDATE:
                tabs = selectedOrganization.getViewableTabs() != null ? selectedOrganization.getViewableTabs() : tabs;
        }
        return tabs;
    }

    private void handleViewableTabSelection() {
        if (!isInitializingViewableValues) {
            List<Component> tabs = getTabs();
            tabs.removeAll(viewableTabInput.getSelectedItems());
            Set<Component> selectedTabs = optionalTabInput.getSelectedItems();
            isInitializingOptionalValues = true;
            optionalTabInput.setItems(tabs);
            optionalTabInput.setValue(selectedTabs);
            handleInputValueChange();
            isInitializingOptionalValues = false;
        }
    }

    private void handleOptionalTabSelection() {
        if (!isInitializingOptionalValues) {
            List<Component> tabs = getTabs();
            tabs.removeAll(optionalTabInput.getSelectedItems());
            Set<Component> selectedTabs = viewableTabInput.getSelectedItems();
            isInitializingViewableValues = true;
            viewableTabInput.setItems(tabs);
            viewableTabInput.setValue(selectedTabs);
            handleInputValueChange();
            isInitializingViewableValues = false;
        }
    }

    private List<Component> getReports() {
        List<Component> reports = new ArrayList<>();
        switch (operationInput.getValue()) {
            case CREATE:
                reports = Component.getClientReportTypes();
                break;
            case UPDATE:
                reports = selectedOrganization.getViewableReports() != null ? selectedOrganization.getViewableReports() : reports;
        }
        return reports;
    }

    private void handleViewableReportSelection() {
        if (!isInitializingViewableValues) {
            List<Component> reports = getReports();
            reports.removeAll(viewableReportsInput.getSelectedItems());
            Set<Component> selectedReports = optionalReportsInput.getSelectedItems();
            isInitializingOptionalValues = true;
            optionalReportsInput.setItems(reports);
            optionalReportsInput.setValue(selectedReports);
            handleInputValueChange();
            isInitializingOptionalValues = false;
        }
    }

    private void handleOptionalReportSelection() {
        if (!isInitializingOptionalValues) {
            List<Component> reports = getReports();
            reports.removeAll(optionalReportsInput.getSelectedItems());
            Set<Component> selectedReports = viewableReportsInput.getSelectedItems();
            isInitializingViewableValues = true;
            viewableReportsInput.setItems(reports);
            viewableReportsInput.setValue(selectedReports);
            handleInputValueChange();
            isInitializingViewableValues = false;
        }
    }

    private void loadOrganizationGrid() {
        List<Organization> organizations = organizationService.getOrganizations();
        if (organizations.isEmpty()) {
            Notification.show("No results found", Notification.Type.WARNING_MESSAGE);
            return;
        }
        organizationGrid.setItems(organizations);
        organizationGrid.removeAllColumns();
        organizationGrid.setHeightByRows(organizations.size());
        organizationGrid.addColumn(Organization::getName).setCaption("Name");
        organizationGrid.addColumn(Organization::getViewableTabsAsString).setCaption("Viewable Tabs");
        organizationGrid.addColumn(Organization::getOptionalViewableTabsAsString).setCaption("Optional Viewable Tabs");
        organizationGrid.addColumn(Organization::getViewableReportsAsString).setCaption("Viewable Reports");
        organizationGrid.addColumn(Organization::getOptionalViewableReportsAsString).setCaption("Optional Viewable Reports");
    }

    private void loadOrganizationConfigurationGrid() {
        List<OrganizationSpecificConfiguration> configurations = organizationSpecificConfigurationService.getConfigurations();
        if (configurations.isEmpty()) {
            Notification.show("No results found", Notification.Type.WARNING_MESSAGE);
            return;
        }
        organizationConfigurationGrid.setItems(configurations);
        organizationConfigurationGrid.removeAllColumns();
        organizationConfigurationGrid.setHeightByRows(configurations.size());
        organizationConfigurationGrid.addColumn(OrganizationSpecificConfiguration::getOrganizationName).setCaption("Organization");
        organizationConfigurationGrid.addColumn(OrganizationSpecificConfiguration::getClasses).setCaption("Classes");
        organizationConfigurationGrid.addColumn(OrganizationSpecificConfiguration::getGraphRelationships).setCaption("Graph Relationships");
    }

    private void loadUserGrid() {
        List<User> users = userService.getUsers(organizationService.getOrganizationName());
        if (users.isEmpty()) {
            Notification.show("No results found", Notification.Type.WARNING_MESSAGE);
            return;
        }
        userGrid.setItems(users);
        userGrid.removeAllColumns();
        userGrid.setHeightByRows(users.size());
        userGrid.addColumn(User::getUsername).setCaption("Username");
        userGrid.addColumn(User::getName).setCaption("Name");
        userGrid.addColumn(User::getLastName).setCaption("Last Name");
        userGrid.addColumn(User::getOrganizationName).setCaption("Organization");
        userGrid.addColumn(User::getEmail).setCaption("Email");
        userGrid.addColumn(User::getViewableTabsAsString).setCaption("Viewable Tabs");
        userGrid.addColumn(User::getViewableReportsAsString).setCaption("Viewable Reports");
    }

    private void loadUserLocationGrid() {
        List<UserLocation> userLocations = userLocationService.getUserLocations(organizationService.getOrganizationName());
        if (userLocations.isEmpty()) {
            Notification.show("No results found", Notification.Type.WARNING_MESSAGE);
            return;
        }
        userLocationGrid.setItems(userLocations);
        userLocationGrid.removeAllColumns();
        userLocationGrid.setHeightByRows(userLocations.size());
        userLocationGrid.addColumn(UserLocation::getLocation).setCaption("Location");
        userLocationGrid.addColumn(UserLocation::getUsersAsString).setCaption("Users");
    }

    private boolean isOrganizationGridSelected() {
        return organizationGrid.getSelectionModel().getFirstSelectedItem().isPresent();
    }

    private boolean isOrganizationConfigurationGridSelected() {
        return organizationConfigurationGrid.getSelectionModel().getFirstSelectedItem().isPresent();
    }

    private boolean isUserGridSelected() {
        return userGrid.getSelectionModel().getFirstSelectedItem().isPresent();
    }

    private boolean isUserLocationGridSelected() {
        return userLocationGrid.getSelectionModel().getFirstSelectedItem().isPresent();
    }

    private void handleOrganizationGridSelection() {
        Optional<Organization> reference = organizationGrid.getSelectionModel().getFirstSelectedItem();
        reference.ifPresent(organization -> selectedOrganization = organization);
        operationInput.setItems(reference.isPresent() ? new ArrayList<>(Arrays.asList(UPDATE, DEACTIVATE)) : new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
        if (operationInput.getValue() != DEACTIVATE) {
            organizationNameLayout.setVisible(operationInput.getValue() == CREATE && !reference.isPresent());
            viewableTabLayout.setVisible(reference.isPresent());
            viewableReportsLayout.setVisible(reference.isPresent());
            if (operationInput.getValue() == UPDATE) {
                organizationNameInput.setSelectedItem(selectedOrganization.getName());
                List<Component> tabs = Component.getTabTypes();
                if (selectedOrganization.getOptionalViewableTabs() != null)
                    tabs.removeAll(selectedOrganization.getOptionalViewableTabs());
                isInitializingViewableValues = true;
                viewableTabInput.setItems(tabs);
                viewableTabInput.setValue(selectedOrganization.getViewableTabs() != null ? new HashSet<>(selectedOrganization.getViewableTabs()) : new HashSet<>());
                tabs = Component.getTabTypes();
                if (selectedOrganization.getViewableTabs() != null)
                    tabs.removeAll(selectedOrganization.getViewableTabs());
                isInitializingOptionalValues = true;
                optionalTabInput.setItems(tabs);
                optionalTabInput.setValue(selectedOrganization.getOptionalViewableTabs() != null ? new HashSet<>(selectedOrganization.getOptionalViewableTabs()) : new HashSet<>());
                List<Component> reports = Component.getClientReportTypes();
                if (selectedOrganization.getOptionalViewableReports() != null)
                    reports.removeAll(selectedOrganization.getOptionalViewableReports());
                viewableReportsInput.setItems(reports);
                reports = Component.getClientReportTypes();
                if (selectedOrganization.getViewableReports() != null)
                    reports.removeAll(selectedOrganization.getViewableReports());
                optionalReportsInput.setItems(reports);
                viewableReportsInput.setValue(selectedOrganization.getViewableReports() != null ? new HashSet<>(selectedOrganization.getViewableReports()) : new HashSet<>());
                optionalReportsInput.setValue(selectedOrganization.getOptionalViewableReports() != null ? new HashSet<>(selectedOrganization.getOptionalViewableReports()) : new HashSet<>());
                isInitializingViewableValues = false;
                isInitializingOptionalValues = false;
            }
        } else
            hideInputLayouts();
        submitButton.setEnabled(reference.isPresent());
    }

    private void handleOrganizationConfigurationGridSelection() {
        Optional<OrganizationSpecificConfiguration> reference = organizationConfigurationGrid.getSelectionModel().getFirstSelectedItem();
        reference.ifPresent(organizationSpecificConfiguration -> selectedOrganizationConfiguration = organizationSpecificConfiguration);
        operationInput.setItems(reference.isPresent() ? new ArrayList<>(Collections.singletonList(UPDATE)) : new ArrayList<>(Arrays.asList(CREATE, UPDATE)));
        organizationNameLayout.setVisible(operationInput.getValue() == CREATE && !reference.isPresent());
        classesLayout.setVisible(reference.isPresent());
        graphRelationshipsLayout.setVisible(reference.isPresent());
        if (reference.isPresent()) {
            if (operationInput.getValue() == UPDATE) {
                organizationNameInput.setSelectedItem(reference.get().getOrganizationName());
                Set<String> orgClasses = reference.get().getClasses();
                StringBuilder stringBuilder = new StringBuilder();
                for (String str : orgClasses)
                    stringBuilder.append(str).append("\n");
                uneditableClasses.setValue(stringBuilder.toString());
                Set<String> orgGraphRelationships = reference.get().getGraphRelationships();
                stringBuilder = new StringBuilder();
                for (String str : orgGraphRelationships)
                    stringBuilder.append(str).append("\n");
                uneditableGraphRelationships.setValue(stringBuilder.toString());
            }
        }
        submitButton.setEnabled(reference.isPresent());
    }

    private void handleUserGridSelection() {
        Optional<User> reference = userGrid.getSelectionModel().getFirstSelectedItem();
        reference.ifPresent(user -> selectedUser = user);
        operationInput.setItems(reference.isPresent() ? new ArrayList<>(Arrays.asList(UPDATE, DEACTIVATE)) : new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
        if (operationInput.getValue() != DEACTIVATE) {
            nameLayout.setVisible(reference.isPresent());
            lastNameLayout.setVisible(reference.isPresent());
            usernameLayout.setVisible(reference.isPresent());
            passwordLayout.setVisible(reference.isPresent());
            emailLayout.setVisible(reference.isPresent());
            userViewableTabLayout.setVisible(reference.isPresent());
            userViewableReportsLayout.setVisible(reference.isPresent());
            if (operationInput.getValue() == UPDATE) {
                usernameInput.setValue(selectedUser.getUsername());
                nameInput.setValue(selectedUser.getName());
                lastNameInput.setValue(selectedUser.getLastName());
                organizationNameInput.setSelectedItem(selectedUser.getOrganizationName());
                emailInput.setValue(selectedUser.getEmail());
                userViewableTabInput.setValue(selectedUser.getViewableTabs() != null ? new HashSet<>(selectedUser.getViewableTabs()) : new HashSet<>());
                userViewableReportsInput.setValue(selectedUser.getViewableReports() != null ? new HashSet<>(selectedUser.getViewableReports()) : new HashSet<>());
            }
        } else
            hideInputLayouts();
        submitButton.setEnabled(reference.isPresent());
    }

    private void handleUserLocationGridSelection() {
        Optional<UserLocation> reference = userLocationGrid.getSelectionModel().getFirstSelectedItem();
        reference.ifPresent(userLocation -> selectedUserLocation = userLocation);
        operationInput.setItems(reference.isPresent() ? new ArrayList<>(Arrays.asList(UPDATE, DEACTIVATE)) : new ArrayList<>(Arrays.asList(CREATE, UPDATE, DEACTIVATE)));
        if (operationInput.getValue() != DEACTIVATE) {
            cityLayout.setVisible(reference.isPresent());
            stateLayout.setVisible(reference.isPresent());
            userLayout.setVisible(reference.isPresent());
            if (operationInput.getValue() == UPDATE) {
                cityInput.setValue(selectedUserLocation.getLocation().getCity());
                stateInput.setSelectedItem(selectedUserLocation.getLocation().getState());
                userInput.setValue(selectedUserLocation.getUsers() != null ? new HashSet<>(selectedUserLocation.getUsers()) : new HashSet<>());
            }
        } else
            hideInputLayouts();
        submitButton.setEnabled(reference.isPresent());
    }

    private void handleSubmitButtonClick() {
        switch (entityInput.getValue()) {
            case ORGANIZATION:
                switch (operationInput.getValue()) {
                    case CREATE:
                        Organization organization = new Organization();
                        organization.setName(organizationNameInput.getValue());
                        organization.setViewableTabs(new ArrayList<>(viewableTabInput.getValue()));
                        organization.setOptionalViewableTabs(new ArrayList<>(optionalTabInput.getValue()));
                        organization.setViewableReports(new ArrayList<>(viewableReportsInput.getValue()));
                        organization.setOptionalViewableReports(new ArrayList<>(optionalReportsInput.getValue()));
                        organization.setActive(true);
                        organization.setDateCreated(LocalDate.now());
                        if (organizationService.saveOrganization(organization)) {
                            recordActivity(CREATE + " " + organization.serialize());
                            Notification.show("Organization " + organization.getName() + " successfully saved", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        creatableOrganizations.removeIf(organizationName -> organizationName.equals(organizationNameInput.getValue()));
                        organizationNameInput.clear();
                        organizationNameInput.setItems(creatableOrganizations);
                        break;
                    case UPDATE:
                        selectedOrganization.setViewableTabs(new ArrayList<>(viewableTabInput.getValue()));
                        selectedOrganization.setOptionalViewableTabs(new ArrayList<>(optionalTabInput.getValue()));
                        selectedOrganization.setViewableReports(new ArrayList<>(viewableReportsInput.getValue()));
                        selectedOrganization.setOptionalViewableReports(new ArrayList<>(optionalReportsInput.getValue()));
                        selectedOrganization.setDateLastUpdated(LocalDate.now());
                        if (organizationService.saveOrganization(selectedOrganization)) {
                            recordActivity(UPDATE + " " + selectedOrganization.serialize());
                            Notification.show("Organization " + selectedOrganization.getName() + " successfully updated", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        break;
                    case DEACTIVATE:
                        confirmationWindow = new ConfirmationWindow("Confirm Deactivation", this);
                        UI.getCurrent().addWindow(confirmationWindow);
                }
                clearOrganizationConfigurationInputs();
                loadOrganizationGrid();
                break;
            case ORGANIZATION_CONFIGURATION:
                switch (operationInput.getValue()) {
                    case CREATE:
                        OrganizationSpecificConfiguration organizationConfiguration = new OrganizationSpecificConfiguration();
                        organizationConfiguration.setOrganizationName(organizationNameInput.getValue());
                        organizationConfiguration.setClasses(new HashSet<>(Arrays.asList(classes.getValue().split("\\r?\\n"))));
                        organizationConfiguration.setGraphRelationships(new HashSet<>(Arrays.asList(graphRelationships.getValue().split("\\r?\\n"))));
                        if (organizationSpecificConfigurationService.saveOrganizationConfiguration(organizationConfiguration)) {
                            recordActivity(CREATE + " " + organizationConfiguration.toString());
                            Notification.show("OrganizationSpecificConfiguration " + organizationConfiguration.getOrganizationName() + " successfully saved", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        creatableOrganizationConfigurations.removeIf(organizationName -> organizationName.equals(organizationNameInput.getValue()));
                        organizationNameInput.clear();
                        organizationNameInput.setItems(creatableOrganizationConfigurations);
                        break;
                    case UPDATE:
                        selectedOrganizationConfiguration.setClasses(new HashSet<>(Arrays.asList(uneditableClasses.getValue().split("\\r?\\n"))));
                        selectedOrganizationConfiguration.setGraphRelationships(new HashSet<>(Arrays.asList(uneditableGraphRelationships.getValue().split("\\r?\\n"))));
                        if (organizationSpecificConfigurationService.saveOrganizationConfiguration(selectedOrganizationConfiguration)) {
                            recordActivity(UPDATE + " " + selectedOrganizationConfiguration.toString());
                            Notification.show("OrganizationSpecificConfiguration " + selectedOrganizationConfiguration.getOrganizationName() + " successfully updated", Notification.Type.HUMANIZED_MESSAGE);
                        }
                }
                clearOrganizationConfigurationInputs();
                loadOrganizationConfigurationGrid();
                break;
            case USER:
                switch (operationInput.getValue()) {
                    case CREATE:
                        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                        User user = new User();
                        user.setName(nameInput.getValue());
                        user.setLastName(lastNameInput.getValue());
                        user.setUsername(usernameInput.getValue());
                        user.setPassword(passwordEncoder.encode(passwordInput.getValue()));
                        user.setEmail(emailInput.getValue());
                        user.setOrganizationName(organizationService.getOrganizationName());
                        user.setViewableTabs(new ArrayList<>(userViewableTabInput.getValue()));
                        user.setViewableReports(new ArrayList<>(userViewableReportsInput.getValue()));
                        user.setActive(true);
                        user.setDateCreated(LocalDate.now());
                        if (userService.saveUser(user)) {
                            recordActivity(CREATE + " " + user.serialize());
                            Notification.show("User " + user.getUsername() + " successfully saved", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        break;
                    case UPDATE:
                        passwordEncoder = new BCryptPasswordEncoder();
                        selectedUser.setName(nameInput.getValue());
                        selectedUser.setLastName(lastNameInput.getValue());
                        selectedUser.setUsername(usernameInput.getValue());
                        selectedUser.setPassword(passwordInput.getValue() != null && !passwordInput.getValue().isEmpty() ? passwordEncoder.encode(passwordInput.getValue()) : selectedUser.getPassword());
                        selectedUser.setEmail(emailInput.getValue());
                        selectedUser.setOrganizationName(organizationService.getOrganizationName());
                        selectedUser.setViewableTabs(new ArrayList<>(userViewableTabInput.getValue()));
                        selectedUser.setViewableReports(new ArrayList<>(userViewableReportsInput.getValue()));
                        selectedUser.setDateLastUpdated(LocalDate.now());
                        if (userService.saveUser(selectedUser)) {
                            recordActivity(UPDATE + " " + selectedUser.serialize());
                            Notification.show("User " + selectedUser.getUsername() + " successfully updated", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        break;
                    case DEACTIVATE:
                        confirmationWindow = new ConfirmationWindow("Confirm Deactivation", this);
                        UI.getCurrent().addWindow(confirmationWindow);
                }
                clearUserInputs();
                loadUserGrid();
                break;
            case USER_LOCATION:
                switch (operationInput.getValue()) {
                    case CREATE:
                        UserLocation userLocation = new UserLocation();
                        userLocation.setLocation(new Location(cityInput.getValue(), stateInput.getValue()));
                        userLocation.setUsers(new ArrayList<>(userInput.getValue()));
                        if (userLocationService.saveUserLocation(userLocation)) {
                            recordActivity(CREATE + " " + userLocation.serialize());
                            Notification.show("User location " + userLocation + " successfully saved", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        break;
                    case UPDATE:
                        selectedUserLocation.setLocation(new Location(cityInput.getValue(), stateInput.getValue()));
                        selectedUserLocation.setUsers(new ArrayList<>(userInput.getValue()));
                        if (userLocationService.saveUserLocation(selectedUserLocation)) {
                            recordActivity(UPDATE + " " + selectedUserLocation.serialize());
                            Notification.show("User location " + selectedUserLocation + " successfully updated", Notification.Type.HUMANIZED_MESSAGE);
                        }
                        break;
                    case DEACTIVATE:
                        confirmationWindow = new ConfirmationWindow("Confirm Deactivation", this);
                        UI.getCurrent().addWindow(confirmationWindow);
                }
                clearUserLocationInputs();
                loadUserLocationGrid();
        }
        submitButton.setEnabled(false);
    }

    private void recordActivity(String input) {
        Audit activity = new Audit();
        activity.setApplicationName(MACHINE_LEARNING_UI);
        activity.setUserName(userService.getUser().getUsername());
        activity.setOrganizationName(userService.getUser().getOrganizationName());
        activity.setComponentName(ACCOUNTS.toString());
        activity.setTimestamp(LocalDateTime.now());
        activity.setRequest(input);
        activity.setRequestNumber("N/A");
        activity.setExecutionStatus(SUCCESS);
        auditService.saveActivity(activity);
    }
}
