package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.*;
import com.mst.machineLearningModel.Component;
import com.mst.machineLearningUi.service.*;
import com.mst.machineLearningUi.utility.PluralizationUtility;
import com.vaadin.server.Page;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.mst.machineLearningModel.ApplicationName.MACHINE_LEARNING_UI;
import static com.mst.machineLearningModel.ExecutionStatus.FAILED;
import static com.mst.machineLearningModel.ExecutionStatus.SUCCESS;
import static com.mst.machineLearningModel.Component.*;
import static com.mst.machineLearningUi.view.MachineLearningProcessingTab.DateType.*;
import static com.vaadin.ui.Alignment.MIDDLE_RIGHT;
import static com.vaadin.ui.themes.ValoTheme.OPTIONGROUP_HORIZONTAL;
import static com.mst.machineLearningUi.view.MachineLearningProcessingTab.InputMethod.*;
import static com.mst.machineLearningModel.RunningMode.*;

class MachineLearningProcessingTab extends VerticalLayout implements Button.ClickListener {
    public enum InputMethod {
        MANUAL {public String toString() { return "Manual"; }},
        FILE_UPLOAD {public String toString() { return "File Upload"; }}
    }
    public enum DateType {
        REPORT_DATE {public String toString() { return "Report Date"; }},
        DATE_PROCESSED {public String toString() { return "Date Processed"; }}
    }
    private final UserService userService;
    private final TokenClassService tokenClassService;
    private final AuditService auditService;
    private final OrganizationSpecificConfigurationService organizationConfigurationService;
    private ComboBox<Component> processInput;
    private RadioButtonGroup<RunningMode> runningModeRadioButton;
    private HorizontalLayout modeLayout;
    private RadioButtonGroup<InputMethod> inputMethodRadioButton;
    private Label uploadedFileLabel;
    private HorizontalLayout uploadLayout;
    private RadioButtonGroup<DateType> dateTypeRadioButton;
    private HorizontalLayout dateTypeLayout;
    private DateField fromDate;
    private DateField toDate;
    private Label dateLabel;
    private ComboBox<Organization> organizationInput;
    private ListSelect<OrganizationName> referenceOrganizationsInput;
    private ComboBox<String> comboboxInput;
    private Label comboboxLabel;
    private TextField accessionNumbers;
    private CheckBox asynchronous;
    private TextField textField;
    private Label textFieldLabel;
    private TextArea textArea;
    private Label textAreaLabel;
    private TextArea leftTextArea;
    private TextArea rightTextArea;
    private Label doubleTextAreaLabel;
    private Label doubleTextAreaRightTopLabel;
    private Label doubleTextAreaRightMiddleLabel;
    private CheckBox multipurpose;
    private Button submitButton;
    private ByteArrayOutputStream stream;
    private HorizontalLayout inputMethodLayout;
    private HorizontalLayout organizationLayout;
    private HorizontalLayout comboboxLayout;
    private HorizontalLayout textFieldLayout;
    private HorizontalLayout textAreaLayout;
    private HorizontalLayout doubleTextAreaLayout;
    private HorizontalLayout dateLayout;
    private HorizontalLayout referenceOrganizationsLayout;
    private HorizontalLayout accessionNumbersLayout;
    private HorizontalLayout asynchronousLayout;
    private Label multipurposeLabel;
    private HorizontalLayout multipurposeLayout;
    private ConfirmationWindow confirmationWindow;
    private Grid<SingularPlural> grid;

    MachineLearningProcessingTab(TabSheet tabSheet, UserService userService, OrganizationService organizationService, TokenClassService tokenClassService, AuditService auditService, OrganizationSpecificConfigurationService organizationConfigurationService) {
        this.userService = userService;
        this.tokenClassService = tokenClassService;
        this.auditService = auditService;
        this.organizationConfigurationService = organizationConfigurationService;
        final int CAPTION_WIDTH = 12;
        processInput = new ComboBox<>();
        processInput.setWidth(30, Unit.EM);
        processInput.setItems(Component.getMachineLearningProcesses());
        processInput.setValue(Component.getMachineLearningProcesses().get(0));
        processInput.setTextInputAllowed(false);
        processInput.setEmptySelectionAllowed(false);
        processInput.addValueChangeListener(event -> handleProcessValueChange());
        Label processLabel = new Label("Process");
        processLabel.setStyleName("caption-right");
        processLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout processLayout = new HorizontalLayout(processLabel, processInput);
        processLayout.setComponentAlignment(processLabel, Alignment.MIDDLE_RIGHT);

        runningModeRadioButton = new RadioButtonGroup<>();
        runningModeRadioButton.setItems(NORMAL, MODELLING);
        runningModeRadioButton.setSelectedItem(NORMAL);
        runningModeRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        Label modeLabel = new Label("Mode");
        modeLabel.setStyleName("caption-right");
        modeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        modeLayout = new HorizontalLayout(modeLabel, runningModeRadioButton);
        modeLayout.setComponentAlignment(modeLabel, Alignment.MIDDLE_RIGHT);
        modeLayout.setVisible(false);

        inputMethodRadioButton = new RadioButtonGroup<>();
        inputMethodRadioButton.setItems(MANUAL, FILE_UPLOAD);
        inputMethodRadioButton.setSelectedItem(MANUAL);
        inputMethodRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        inputMethodRadioButton.addValueChangeListener(event -> handleInputMethodValueChange());
        Label inputMethodLabel = new Label("Input Method");
        inputMethodLabel.setStyleName("caption-right");
        inputMethodLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        inputMethodLayout = new HorizontalLayout(inputMethodLabel, inputMethodRadioButton);
        inputMethodLayout.setComponentAlignment(inputMethodLabel, Alignment.MIDDLE_RIGHT);

        Upload upload = new Upload();
        stream = new ByteArrayOutputStream();
        upload.setReceiver((Upload.Receiver) (filename, mimeType) -> stream);
        upload.addSucceededListener((Upload.SucceededListener) this::handleUploadSucceeded);
        upload.setImmediateMode(true);
        Label uploadLabel = new Label("Select file to upload");
        uploadLabel.setStyleName("caption-right");
        uploadLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        uploadedFileLabel = new Label();
        uploadedFileLabel.setStyleName("blue");
        uploadLayout = new HorizontalLayout(uploadLabel, upload, uploadedFileLabel);
        uploadLayout.setComponentAlignment(uploadLabel, Alignment.MIDDLE_RIGHT);
        uploadLayout.setVisible(false);

        dateTypeRadioButton = new RadioButtonGroup<>();
        dateTypeRadioButton.setItems(DATE_PROCESSED, REPORT_DATE);
        dateTypeRadioButton.setSelectedItem(DATE_PROCESSED);
        dateTypeRadioButton.setStyleName(OPTIONGROUP_HORIZONTAL);
        dateTypeRadioButton.addValueChangeListener(event -> handleDateTypeValueChange());
        Label dateTypeLabel = new Label("Date Type");
        dateTypeLabel.setStyleName("caption-right");
        dateTypeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        dateTypeLayout = new HorizontalLayout(dateTypeLabel, dateTypeRadioButton);
        dateTypeLayout.setComponentAlignment(dateTypeLabel, MIDDLE_RIGHT);
        dateTypeLayout.setVisible(false);

        fromDate = new DateField();
        fromDate.setValue(LocalDate.now());
        toDate = new DateField();
        toDate.setValue(LocalDate.now());
        dateLabel = new Label(dateTypeRadioButton.getValue().toString());
        dateLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        dateLabel.setStyleName("caption-right");
        Label to = new Label("to");
        to.addStyleName("caption");
        dateLayout = new HorizontalLayout(dateLabel, fromDate, to, toDate);
        dateLayout.setComponentAlignment(dateLabel, Alignment.MIDDLE_RIGHT);
        dateLayout.setComponentAlignment(to, Alignment.MIDDLE_CENTER);
        dateLayout.setVisible(false);

        organizationInput = new ComboBox<>();
        organizationInput.setWidth(18, Unit.EM);
        organizationInput.setItems(organizationService.getOrganizations());
        organizationInput.setValue(organizationService.getOrganizations().get(0));
        organizationInput.setEmptySelectionAllowed(false);
        organizationInput.setTextInputAllowed(false);
        organizationInput.addValueChangeListener(event -> handleProcessValueChange());
        Label organizationLabel = new Label("Organization");
        organizationLabel.setStyleName("caption-right");
        organizationLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        organizationLayout = new HorizontalLayout(organizationLabel, organizationInput);
        organizationLayout.setComponentAlignment(organizationLabel, Alignment.MIDDLE_RIGHT);

        referenceOrganizationsInput = new ListSelect<>();
        List<OrganizationName> referenceOrganizationNames = ((MachineLearningUi)UI.getCurrent()).getServerAddress().equals("10.0.4.163") ? OrganizationName.getProductionReferenceOrganizationNames() : OrganizationName.getReferenceOrganizationNames();
        referenceOrganizationsInput.setItems(referenceOrganizationNames);
        referenceOrganizationsInput.setRows(referenceOrganizationNames.size() < 3 ? referenceOrganizationNames.size() : 3);
        referenceOrganizationsInput.setRequiredIndicatorVisible(true);
        referenceOrganizationsInput.addValueChangeListener(event -> handleInputValueChange());
        Label organizationsLabel = new Label("Reference Organizations");
        organizationsLabel.setStyleName("caption-right");
        organizationsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        referenceOrganizationsLayout = new HorizontalLayout(organizationsLabel, referenceOrganizationsInput);
        referenceOrganizationsLayout.setComponentAlignment(organizationsLabel, Alignment.TOP_RIGHT);
        referenceOrganizationsLayout.setVisible(false);

        accessionNumbers = new TextField();
        accessionNumbers.setDescription("Comma Delimited");
        accessionNumbers.setWidth(26, Unit.EM);
        Label accessionNumbersLabel = new Label("Accession Numbers");
        accessionNumbersLabel.setStyleName("caption-right");
        accessionNumbersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        accessionNumbersLayout = new HorizontalLayout(accessionNumbersLabel, accessionNumbers);
        accessionNumbersLayout.setComponentAlignment(accessionNumbersLabel, Alignment.MIDDLE_RIGHT);
        accessionNumbersLayout.setVisible(false);

        asynchronous = new CheckBox();
        asynchronous.setValue(true);
        Label asynchronousLabel = new Label("Asynchronous");
        asynchronousLabel.setStyleName("caption-right");
        asynchronousLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        asynchronousLayout = new HorizontalLayout(asynchronousLabel, asynchronous);
        asynchronousLayout.setComponentAlignment(asynchronousLabel, Alignment.TOP_RIGHT);
        asynchronousLayout.setComponentAlignment(asynchronous, Alignment.BOTTOM_LEFT);
        asynchronousLayout.setVisible(false);

        multipurpose = new CheckBox();
        multipurposeLabel = new Label("Multipurpose");
        multipurposeLabel.setStyleName("caption-right");
        multipurposeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        multipurposeLayout = new HorizontalLayout(multipurposeLabel, multipurpose);
        multipurposeLayout.setComponentAlignment(multipurposeLabel, Alignment.TOP_RIGHT);
        multipurposeLayout.setComponentAlignment(multipurpose, Alignment.BOTTOM_LEFT);
        multipurposeLayout.setVisible(false);

        comboboxInput = new ComboBox<>();
        comboboxInput.setWidth(18, Unit.EM);
        comboboxInput.setTextInputAllowed(false);
        comboboxInput.addValueChangeListener(event -> handleInputValueChange());
        comboboxLabel = new Label("DYNAMIC - NEED TO SET");
        comboboxLabel.setStyleName("caption-right");
        comboboxLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        comboboxLayout = new HorizontalLayout(comboboxLabel, comboboxInput);
        comboboxLayout.setComponentAlignment(comboboxLabel, Alignment.MIDDLE_RIGHT);
        comboboxLayout.setVisible(false);

        textField = new TextField();
        textField.setWidth(22, Unit.EM);
        textField.addValueChangeListener(event -> handleInputValueChange());
        textFieldLabel = new Label("DYNAMIC - NEED TO SET");
        textFieldLabel.setStyleName("caption-right");
        textFieldLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        textFieldLayout = new HorizontalLayout(textFieldLabel, textField);
        textFieldLayout.setComponentAlignment(textFieldLabel, MIDDLE_RIGHT);
        textFieldLayout.setVisible(false);

        textArea = new TextArea();
        textArea.setWidth("100%");
        textArea.setRows(15);
        textArea.setRequiredIndicatorVisible(true);
        textArea.addValueChangeListener(event -> handleInputValueChange());
        textAreaLabel = new Label("Data");
        textAreaLabel.setStyleName("caption-right");
        textAreaLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        textAreaLayout = new HorizontalLayout(textAreaLabel, textArea);
        textAreaLayout.setWidth("100%");
        textAreaLayout.setSizeFull();
        textAreaLayout.setComponentAlignment(textAreaLabel, Alignment.TOP_RIGHT);
        textAreaLayout.setExpandRatio(textArea, 1.0f);

        doubleTextAreaLabel = new Label("DYNAMIC - NEED TO SET");
        doubleTextAreaLabel.setStyleName("caption-right");
        doubleTextAreaLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        leftTextArea = new TextArea();
        leftTextArea.setWidth(25, Unit.EM);
        leftTextArea.setRows(15);
        leftTextArea.addValueChangeListener(event -> handleInputValueChange());
        doubleTextAreaRightTopLabel = new Label("To");
        doubleTextAreaRightTopLabel.setStyleName("caption-right");
        doubleTextAreaRightTopLabel.setWidth(3, Unit.EM);
        doubleTextAreaRightMiddleLabel = new Label("=");
        doubleTextAreaRightMiddleLabel.setStyleName("caption");
        doubleTextAreaRightMiddleLabel.setWidth(1, Unit.EM);
        rightTextArea = new TextArea();
        rightTextArea.setWidth(25, Unit.EM);
        rightTextArea.setRows(15);
        rightTextArea.setRequiredIndicatorVisible(true);
        rightTextArea.addValueChangeListener(event -> handleInputValueChange());
        VerticalLayout verticalLayout = new VerticalLayout(doubleTextAreaRightTopLabel, doubleTextAreaRightMiddleLabel, new Label());
        verticalLayout.setMargin(false);
        verticalLayout.setHeight("100%");
        verticalLayout.setComponentAlignment(doubleTextAreaRightTopLabel, Alignment.TOP_RIGHT);
        verticalLayout.setComponentAlignment(doubleTextAreaRightMiddleLabel, Alignment.MIDDLE_CENTER);
        HorizontalLayout layout = new HorizontalLayout(leftTextArea, verticalLayout, rightTextArea);
        layout.setComponentAlignment(verticalLayout, Alignment.MIDDLE_CENTER);
        doubleTextAreaLayout = new HorizontalLayout(doubleTextAreaLabel, layout);
        doubleTextAreaLayout.setComponentAlignment(doubleTextAreaLabel, Alignment.TOP_RIGHT);
        doubleTextAreaRightTopLabel.setVisible(false);
        doubleTextAreaRightMiddleLabel.setVisible(false);
        doubleTextAreaLayout.setVisible(false);

        submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButtonClick());
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        submitButton.setEnabled(false);
        Label spacer = new Label("");
        spacer.setStyleName("caption-right");
        spacer.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout buttonLayout = new HorizontalLayout(spacer, submitButton);
        buttonLayout.setComponentAlignment(spacer, Alignment.MIDDLE_RIGHT);

        grid = new Grid<>();
        grid.setSizeFull();
        grid.setVisible(false);

        addComponents(processLayout,modeLayout,inputMethodLayout,uploadLayout,dateTypeLayout,dateLayout,organizationLayout,referenceOrganizationsLayout,accessionNumbersLayout,asynchronousLayout,multipurposeLayout,comboboxLayout,textFieldLayout,textAreaLayout,doubleTextAreaLayout,buttonLayout,grid);
        tabSheet.addTab(this, MACHINE_LEARNING_PROCESSING.toString());
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getButton().getCaption().equalsIgnoreCase("yes")) {
            String url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/process";
            MachineLearningProcessRequest processRequest = new MachineLearningProcessRequest();
            processRequest.setProcessType(processInput.getValue());
            processRequest.setUsername(userService.getUser().getUsername());
            processRequest.setOrganizationName(organizationInput.getValue().getName());
            processRequest.setExecuteAsynchronously(false);
            processRequest.setData(new ArrayList<>(Collections.singletonList(textField.getValue())));
            ResponseEntity<AsyncResponseData> response = new RestTemplate().postForEntity(url, processRequest, AsyncResponseData.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AsyncResponseData asyncResponseData = response.getBody();
                Notification.show(asyncResponseData.getResponseMessage(), Notification.Type.HUMANIZED_MESSAGE);
            } else {
                Notification.show(response.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
            }
        }
        confirmationWindow.close();
        submitButton.setEnabled(true);
    }

    private void hideInputs() {
        modeLayout.setVisible(false);
        inputMethodLayout.setVisible(false);
        referenceOrganizationsLayout.setVisible(false);
        comboboxLayout.setVisible(false);
        textFieldLayout.setVisible(false);
        textAreaLayout.setVisible(false);
        doubleTextAreaLayout.setVisible(false);
        dateTypeLayout.setVisible(false);
        dateLayout.setVisible(false);
        accessionNumbersLayout.setVisible(false);
        asynchronousLayout.setVisible(false);
        multipurposeLayout.setVisible(false);
    }

    private void clearTextInputs() {
        textArea.clear();
        textField.clear();
        leftTextArea.clear();
        rightTextArea.clear();
    }

    private SentenceTextRequest createSentenceTextRequest(String text) {
        SentenceTextRequest sentenceTextRequest = new SentenceTextRequest();
        sentenceTextRequest.setOrganizationName(organizationInput.getValue().getName());
        sentenceTextRequest.setText(text);
        sentenceTextRequest.setSource(MachineLearningProcessingTab.class.getSimpleName());
        Report report = new Report();
        report.setOrganizationName(organizationInput.getValue().getName());
        report.setReportDate(LocalDate.now());
        Patient patient = new Patient();
        patient.setPatientDob(LocalDate.now());
        patient.setPatientSex(Sex.MALE);
        report.setPatient(patient);
        sentenceTextRequest.setReport(report);
        return sentenceTextRequest;
    }

    private void handleDateTypeValueChange() {
        dateLabel.setValue(dateTypeRadioButton.getValue().toString());
    }

    private void handleInputMethodValueChange() {
        switch (inputMethodRadioButton.getValue()) {
            case MANUAL:
                uploadLayout.setVisible(false);
                textArea.setReadOnly(false);
                break;
            case FILE_UPLOAD:
                uploadLayout.setVisible(true);
                textArea.setReadOnly(true);
        }
    }

    private void handleProcessValueChange() {
        switch (processInput.getValue()) {
            case PROCESS_TEXT:
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(false, true, true, false, false, false, true,false, false, false, false, false);
                break;
            case MACHINE_LEARNING:
            case TOKEN_PROCESSING:
                setInputsVisibility(true, false, true, true, false,false, false,false,true, true, true, false);
                break;
            case CO_OCCURRENCE:
                setInputsVisibility(false, false, true, true, false,false, false,false,true, true, true, false);
                break;
            case EDGE_WEIGHT1:
                setInputsVisibility(true, false, true, true, false,false, false,false,true, true, true, false);
                break;
            case AUTO_CORRECT:
                setInputsVisibility(true, false, true, false, false,false, false,false,true, false, true, false);
                break;
            case PATTERN_RATIO_ANALYSIS:
                setInputsVisibility(false, false, false, false, false,false, false,false,false, false, false, false);
                break;
            case SET_EDGE_WEIGHT_FLAG:
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(true, false, true, false, false,false, true,false,false, false, true, false);
                break;
            case SET_COMPLEX_PATTERN:
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(false, false, false, false, false,false, true,false,false, false, false, false);
                break;
            case EDGE_WEIGHT2:
            case VERB_PROCESSING:
                setInputsVisibility(true, false, true, true, false,false, false,false,true, true, true, false);
                break;
            case CALCULATE_CONFIDENCE_SCORES:
                setInputsVisibility(false, false, true, false, false,false, false,false,true, false, false, false);
                break;
            case PERFORM_MACHINE_LEARNING_MANDATORY_STEPS_ONLY:
            case PERFORM_MACHINE_LEARNING_ALL_STEPS:
                setInputsVisibility(false, false, true, true, false,false, false,false,true, true, false, false);
                break;
            case PREP_PHRASE_TOKEN_PROCESSING:
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,false, true,false,true, false, true, false);
                break;
            case SAVE_CLASS:
                List<String> classes = new ArrayList<>(organizationConfigurationService.getClasses(organizationInput.getValue().getName()));
                if (!classes.isEmpty()) {
                    comboboxLabel.setValue("Class");
                    comboboxInput.clear();
                    comboboxInput.setEmptySelectionAllowed(true);
                    comboboxInput.setItems(classes);
                    comboboxInput.setRequiredIndicatorVisible(true);
                    textAreaLabel.setValue("Data");
                    textArea.setDescription("The values to be associated with the specified class");
                    clearTextInputs();
                    setInputsVisibility(false, false, true, true, true, false, true, false, false, false, false, false);
                } else {
                    hideInputs();
                    Notification.show("No classes found for " + userService.getUser().getOrganizationName(), Notification.Type.WARNING_MESSAGE);
                }
                break;
            case SAVE_ABBREVIATION:
            case SAVE_CONCEPT:
            case SAVE_EXACT_SYNONYM:
            case SAVE_CLOSE_SYNONYM:
                doubleTextAreaLabel.setValue("Data");
                doubleTextAreaRightTopLabel.setVisible(false);
                doubleTextAreaRightMiddleLabel.setVisible(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, true, false,false,false, true,false, false, false, false);
                break;
            case DELETE_ABBREVIATION:
                textFieldLabel.setValue("Abbreviation");
                setInputRequired(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,true,false, false,false, false, false, false);
                break;
            case DELETE_CLASS:
                textFieldLabel.setValue("Class");
                setInputRequired(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,true,false, false,false, false, false, false);
                break;
            case DELETE_CLOSE_SYNONYM:
                textFieldLabel.setValue("Close Synonym");
                setInputRequired(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,true,false, false,false, false, false, false);
                break;
            case DELETE_CONCEPT:
                textFieldLabel.setValue("Concept");
                setInputRequired(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,true,false, false,false, false, false, false);
                break;
            case DELETE_EXACT_SYNONYM:
                textFieldLabel.setValue("Exact Synonym");
                setInputRequired(true);
                clearTextInputs();
                setInputsVisibility(false, false, true, false, false,true,false, false,false, false, false, false);
                break;
            case LOAD_ICD_CODES:
                multipurposeLabel.setValue("Is Custom Code");
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(false, false, true, true, false,false, true,false,false, false, false, true);
                break;
            case LOAD_EXAMS_FOR_CONFIDENCE_SCORE:
                setInputsVisibility(false, false, false, false, false,false, false,false,false, false, false, false);
                break;
            case LOAD_TOKEN_CLASS_PATTERN:
                textAreaLabel.setValue("Data");
                textArea.setDescription("Each line should be in the form: suffix, meaning, class. Suffix is 1 or more suffixes where multiple suffixes are delimited by commas and every suffix has a hyphen as a prefix to indicate that it is a suffix. The input file may contain prefixes and roots in addition to suffixes. Prefixes have a hyphen as a suffix to indicate that it is a prefix. Roots are identified by having no hyphen. Roots that can have a variation for the endings are identified by appending to the base root a forward slash followed by the variation ending. The meaning applies to the term for which the suffix applies and may contain commas, semicolons, or other punctuation. The class defines the master class for which the term that the suffix applies to applies.");
                clearTextInputs();
                setInputsVisibility(false, true, false, false, false,false, true,false,false, false, false, false);
                break;
            case SAVE_GRAPH_RELATIONSHIP:
                List<String> graphRelationships = new ArrayList<>(organizationConfigurationService.getGraphRelationships(organizationInput.getValue().getName()));
                if (!graphRelationships.isEmpty()) {
                    comboboxLabel.setValue("Graph Relationship");
                    comboboxInput.setEmptySelectionAllowed(false);
                    comboboxInput.setItems(graphRelationships);
                    comboboxInput.setValue(graphRelationships.get(0));
                    comboboxInput.setRequiredIndicatorVisible(false);
                    doubleTextAreaLabel.setValue("From");
                    doubleTextAreaRightTopLabel.setVisible(true);
                    doubleTextAreaRightMiddleLabel.setVisible(false);
                    clearTextInputs();
                    setInputsVisibility(false, false, true, false, true, false, false, true, false, false, false, false);
                } else {
                    hideInputs();
                    Notification.show("No graph relationships found for " + userService.getUser().getOrganizationName(), Notification.Type.WARNING_MESSAGE);
                }
                break;
            case GENERATE_PLURALS:
                textAreaLabel.setValue("Data");
                textArea.setDescription("");
                clearTextInputs();
                setInputsVisibility(false, true, false, false, false,false, true,false,false, false, false, false);
        }
        handleInputValueChange();
        grid.setVisible(false);
    }

    private void handleInputValueChange() {
        switch (processInput.getValue()) {
            case PROCESS_TEXT:
            case SET_EDGE_WEIGHT_FLAG:
            case SET_COMPLEX_PATTERN:
            case PREP_PHRASE_TOKEN_PROCESSING:
            case LOAD_TOKEN_CLASS_PATTERN:
            case GENERATE_PLURALS:
                submitButton.setEnabled(!textArea.isEmpty());
                break;
            case MACHINE_LEARNING:
            case TOKEN_PROCESSING:
            case CO_OCCURRENCE:
            case EDGE_WEIGHT1:
            case EDGE_WEIGHT2:
            case VERB_PROCESSING:
            case PERFORM_MACHINE_LEARNING_MANDATORY_STEPS_ONLY:
            case PERFORM_MACHINE_LEARNING_ALL_STEPS:
                submitButton.setEnabled(!referenceOrganizationsInput.isEmpty());
                break;
            case AUTO_CORRECT:
            case PATTERN_RATIO_ANALYSIS:
            case CALCULATE_CONFIDENCE_SCORES:
            case LOAD_EXAMS_FOR_CONFIDENCE_SCORE:
                submitButton.setEnabled(true);
                break;
            case SAVE_ABBREVIATION:
            case SAVE_EXACT_SYNONYM:
            case SAVE_CLOSE_SYNONYM:
            case SAVE_CONCEPT:
                List<String> leftData = leftTextArea.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(leftTextArea.getValue().split("\\r?\\n"));
                List<String> rightData = rightTextArea.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(rightTextArea.getValue().split("\\r?\\n"));
                submitButton.setEnabled(!referenceOrganizationsInput.isEmpty() && !leftTextArea.getValue().isEmpty() && leftData.size() == rightData.size());
                break;
            case DELETE_ABBREVIATION:
            case DELETE_EXACT_SYNONYM:
            case DELETE_CLOSE_SYNONYM:
            case DELETE_CONCEPT:
            case DELETE_CLASS:
                submitButton.setEnabled(!textField.isEmpty());
                break;
            case LOAD_ICD_CODES:
            case SAVE_CLASS:
                submitButton.setEnabled(!referenceOrganizationsInput.isEmpty() && !textArea.isEmpty() && !comboboxInput.isEmpty());
                break;
            case SAVE_GRAPH_RELATIONSHIP:
                leftData = leftTextArea.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(leftTextArea.getValue().split("\\r?\\n"));
                rightData = rightTextArea.getValue().isEmpty() ? new ArrayList<>() : Arrays.asList(rightTextArea.getValue().split("\\r?\\n"));
                submitButton.setEnabled(!leftTextArea.getValue().isEmpty() && leftData.size() == rightData.size());
        }
    }

    private void setInputsVisibility(boolean mode, boolean inputMethod, boolean organization, boolean referenceOrganizations, boolean combobox, boolean textField, boolean textArea, boolean doubleTextArea, boolean date, boolean accessionNumbers, boolean asynchronous, boolean multipurpose) {
        modeLayout.setVisible(mode);
        inputMethodLayout.setVisible(inputMethod);
        organizationLayout.setVisible(organization);
        referenceOrganizationsLayout.setVisible(referenceOrganizations);
        comboboxLayout.setVisible(combobox);
        textFieldLayout.setVisible(textField);
        textAreaLayout.setVisible(textArea);
        doubleTextAreaLayout.setVisible(doubleTextArea);
        dateTypeLayout.setVisible(date);
        dateLayout.setVisible(date);
        accessionNumbersLayout.setVisible(accessionNumbers);
        asynchronousLayout.setVisible(asynchronous);
        multipurposeLayout.setVisible(multipurpose);
    }

    private void setInputRequired(boolean textField) {
        this.textField.setRequiredIndicatorVisible(textField);
    }

    private void handleUploadSucceeded(Upload.SucceededEvent event) {
        BufferedReader reader = new BufferedReader(new StringReader(stream.toString()));
        List<String> lines = reader.lines().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lines)
            if (line != null && !line.isEmpty())
                stringBuilder.append(line).append(System.lineSeparator());
        textArea.setValue(stringBuilder.toString()); //stream.toString() does not work on Windows;
        uploadedFileLabel.setCaption(event.getFilename());
        uploadedFileLabel.setVisible(true);
    }

    private void handleSubmitButtonClick() {
        uploadedFileLabel.setVisible(false);
        String url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/process";
        MachineLearningProcessRequest processRequest = new MachineLearningProcessRequest();
        processRequest.setProcessType(processInput.getValue());
        processRequest.setUsername(userService.getUser().getUsername());
        MachineLearningLoadRequest loadRequest = new MachineLearningLoadRequest();
        loadRequest.setUsername(userService.getUser().getUsername());
        loadRequest.setOrganizationName(userService.getUser().getOrganizationName());
        GraphNodeRelationshipRequest graphRelationshipRequest = new GraphNodeRelationshipRequest();
        graphRelationshipRequest.setUsername(userService.getUser().getUsername());
        submitButton.setEnabled(false);
        switch (processInput.getValue()) {
            case PROCESS_TEXT:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setSentenceTextRequest(createSentenceTextRequest(textArea.getValue()));
                break;
            case MACHINE_LEARNING:
            case TOKEN_PROCESSING:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : null);
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case CO_OCCURRENCE:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : null);
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case EDGE_WEIGHT1:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : null);
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case AUTO_CORRECT:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case PATTERN_RATIO_ANALYSIS:
                break;
            case SET_EDGE_WEIGHT_FLAG:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case SET_COMPLEX_PATTERN:
                processRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                break;
            case EDGE_WEIGHT2:
            case VERB_PROCESSING:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : null);
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case CALCULATE_CONFIDENCE_SCORES:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                break;
            case PERFORM_MACHINE_LEARNING_ALL_STEPS:
            case PERFORM_MACHINE_LEARNING_MANDATORY_STEPS_ONLY:
                processRequest.setRunningMode(runningModeRadioButton.getValue());
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : null);
                processRequest.setExecuteAsynchronously(true);
                break;
            case PREP_PHRASE_TOKEN_PROCESSING:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                    processRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                else
                    processRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                processRequest.setExecuteAsynchronously(asynchronous.getValue());
                break;
            case SAVE_CLASS:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                List<String> data = Arrays.asList(textArea.getValue().split("\\r?\\n"));
                String suffix = comboboxInput.getValue() + "=";
                for (ListIterator itr = data.listIterator(); itr.hasNext(); ) {
                    //noinspection unchecked
                    itr.set(suffix + itr.next());
                }
                processRequest.setData(data);
                break;
            case SAVE_ABBREVIATION:
            case SAVE_CONCEPT:
            case SAVE_EXACT_SYNONYM:
            case SAVE_CLOSE_SYNONYM:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                processRequest.setData(getPairsFromTextArea(Arrays.asList(leftTextArea.getValue().split("\\r?\\n")), Arrays.asList(rightTextArea.getValue().split("\\r?\\n"))));
                break;
            case DELETE_ABBREVIATION:
            case DELETE_CLASS:
            case DELETE_CLOSE_SYNONYM:
            case DELETE_CONCEPT:
            case DELETE_EXACT_SYNONYM:
                confirmationWindow = new ConfirmationWindow("Confirm Deletion", this);
                UI.getCurrent().addWindow(confirmationWindow);
                return;
            case LOAD_ICD_CODES:
                processRequest.setOrganizationName(organizationInput.getValue().getName());
                processRequest.setReferenceOrganizationNames(referenceOrganizationsInput.getValue() != null ? new ArrayList<>(referenceOrganizationsInput.getValue()) : new ArrayList<>());
                processRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                processRequest.setMultiPurposeFlag(multipurpose.getValue());
                break;
            case LOAD_EXAMS_FOR_CONFIDENCE_SCORE:
                url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/load";
                loadRequest.setLoadType(LOAD_EXAMS_FOR_CONFIDENCE_SCORE);
                break;
            case LOAD_TOKEN_CLASS_PATTERN:
                loadTokenClassPatterns(textArea.getValue());
                submitButton.setEnabled(true);
                return;
            case SAVE_GRAPH_RELATIONSHIP:
                url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/saveGraphRelationship";
                graphRelationshipRequest.setComponent(SAVE_GRAPH_RELATIONSHIP);
                graphRelationshipRequest.setOrganizationName(organizationInput.getValue().getName());
                graphRelationshipRequest.setGraphRelationships(getGraphRelationships(comboboxInput.getValue(), Arrays.asList(leftTextArea.getValue().split("\\r?\\n")), Arrays.asList(rightTextArea.getValue().split("\\r?\\n"))));
                break;
            case GENERATE_PLURALS:
                generatePlurals(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                submitButton.setEnabled(false);
                return;
            default:
                Notification.show(processInput.getValue().toString() + " is not implemented", Notification.Type.WARNING_MESSAGE);
                submitButton.setEnabled(true);
                return;
        }
        ResponseEntity<AsyncResponseData> response;
        try {
            switch (processInput.getValue()) {
                case LOAD_EXAMS_FOR_CONFIDENCE_SCORE:
                    response = new RestTemplate().postForEntity(url, loadRequest, AsyncResponseData.class);
                    break;
                case SAVE_GRAPH_RELATIONSHIP:
                    response = new RestTemplate().postForEntity(url, graphRelationshipRequest, AsyncResponseData.class);
                    break;
                default:
                    response = new RestTemplate().postForEntity(url, processRequest, AsyncResponseData.class);
            }
            if (response != null && response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AsyncResponseData asyncResponseData = response.getBody();
                if (asyncResponseData.getRequestNumber() != null && !asyncResponseData.getRequestNumber().isEmpty()) {
                    Notification notification = new Notification("Request Number " + asyncResponseData.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                    notification.setDelayMsec(120000);
                    notification.show(Page.getCurrent());
                }
                else
                    Notification.show("No response message from server", Notification.Type.WARNING_MESSAGE);
            } else if (response != null)
                Notification.show(response.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
        } catch (Exception e) {
            Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
        } finally {
            submitButton.setEnabled(true);
        }
    }

    private List<String> getPairsFromTextArea(List<String> leftTextArea, List<String> rightTextArea) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < leftTextArea.size(); ++i)
            data.add(leftTextArea.get(i) + "=" + rightTextArea.get(i));
        return data;
    }

    private List<GraphNodeRelationshipRequest.GraphRelationship> getGraphRelationships(String relationship, List<String> fromValues, List<String> toValues) {
        List<GraphNodeRelationshipRequest.GraphRelationship> graphRelationships = new ArrayList<>();
        for (int i = 0; i < fromValues.size(); ++i)
            graphRelationships.add(new GraphNodeRelationshipRequest().new GraphRelationship(fromValues.get(i), relationship, toValues.get(i)));
        return graphRelationships;
    }

    private void loadTokenClassPatterns(String input) {
        List<TokenClassPattern> tokenClassPatterns = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(input));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String suffixes = line.substring(0, line.charAt(0) != '"' ? line.indexOf(',') + 1 : line.indexOf(',', line.indexOf('"', 1)) + 1);
                String meaning = line.substring(suffixes.length(), line.lastIndexOf(','));
                String masterClass = line.substring(line.lastIndexOf(',') + 1);
                if (masterClass.isEmpty())
                    continue;
                int index = suffixes.indexOf('-');
                while (index != -1) {
                    String suffix = suffixes.substring(index + 1, suffixes.indexOf(',', index));
                    tokenClassPatterns.add(new TokenClassPattern(suffix, meaning, masterClass));
                    index = suffixes.indexOf('-', index + 1);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            recordActivity(e.getMessage(), LOAD_TOKEN_CLASS_PATTERN.toString(), FAILED);
        }
        if (tokenClassService.saveTokenClassPatterns(tokenClassPatterns)) {
            Notification notification = new Notification("Token-class patterns successfully saved", Notification.Type.HUMANIZED_MESSAGE);
            notification.setDelayMsec(3000);
            notification.show(Page.getCurrent());
            recordActivity(tokenClassPatterns.toString(), LOAD_TOKEN_CLASS_PATTERN.toString(), SUCCESS);
        }
        else {
            Notification.show("Error saving token-class patterns", Notification.Type.ERROR_MESSAGE);
            recordActivity(tokenClassPatterns.toString(), LOAD_TOKEN_CLASS_PATTERN.toString(), FAILED);
        }
    }

    private void generatePlurals(List<String> singulars) {
        List<SingularPlural> plurals = new ArrayList<>();
        for (String singular : singulars)
            plurals.add(PluralizationUtility.getPlural(singular));
        grid.setVisible(true);
        grid.setItems(plurals);
        grid.removeAllColumns();
        grid.setHeightByRows(plurals.size());
        grid.addColumn(SingularPlural::getSingular).setCaption("Singular");
        grid.addColumn(SingularPlural::getPlural).setCaption("Plural");
        grid.addColumn(SingularPlural::getRule).setCaption("Rule");
        recordActivity(plurals.toString(), GENERATE_PLURALS.toString(), SUCCESS);
    }

    private void recordActivity(String input, String component, ExecutionStatus status) {
        Audit activity = new Audit();
        activity.setApplicationName(MACHINE_LEARNING_UI);
        activity.setUserName(userService.getUser().getUsername());
        activity.setOrganizationName(userService.getUser().getOrganizationName());
        activity.setComponentName(component);
        activity.setTimestamp(LocalDateTime.now());
        activity.setRequest(input);
        activity.setRequestNumber("N/A");
        activity.setExecutionStatus(status);
        auditService.saveActivity(activity);
    }
}
