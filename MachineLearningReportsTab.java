package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.*;
import com.mst.machineLearningModel.Component;
import com.mst.machineLearningModel.EdgeRankingReport;
import com.mst.machineLearningModel.Header;
import com.mst.machineLearningModel.ReportInput.SearchBy;
import com.mst.machineLearningUi.model.DateType;
import com.mst.machineLearningUi.service.OrganizationService;
import com.mst.machineLearningUi.service.UserService;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.mst.machineLearningModel.ApplicationName.MACHINE_LEARNING_UI;
import static com.mst.machineLearningModel.Component.*;
import static com.mst.machineLearningUi.model.DateType.*;
import static com.mst.machineLearningUi.view.MachineLearningReportsTab.DisplayType.*;
import static com.mst.machineLearningModel.ReportInput.SearchBy.*;
import static com.mst.machineLearningUi.view.MachineLearningReportsTab.ResponseType.*;
import static com.mst.machineLearningUi.view.MachineLearningReportsTab.InputMethod.*;
import static com.vaadin.ui.Alignment.MIDDLE_RIGHT;
import static com.vaadin.ui.Grid.SelectionMode.NONE;
import static com.vaadin.ui.themes.ValoTheme.OPTIONGROUP_HORIZONTAL;

class MachineLearningReportsTab extends VerticalLayout {
    public enum DisplayType {
        SUMMARY {public String toString() { return "Summary"; }},
        DETAILED_SUMMARY {public String toString() { return "Detailed Summary"; }},
        BY_REPORT {public String toString() { return "By Report"; }}
    }
    public enum ResponseType {
        TOKEN_REPORT,
        TOKEN_PAIR_REPORT,
        WORD_EMBEDDING_REPORT,
        EDGE_RANKING_REPORT,
        EDGE_COMPARATOR_REPORT,
        CLASSIFICATION_EMBEDDING_REPORT
    }
    public enum InputMethod {
        MANUAL {public String toString() { return "Manual"; }},
        FILE_UPLOAD {public String toString() { return "File Upload"; }}
    }
    private final UserService userService;
    private ComboBox<Component> machineLearningReportTypeInput;
    private RadioButtonGroup<DateType> dateTypeRadioButton;
    private HorizontalLayout dateTypeLayout;
    private DateField fromDate;
    private DateField toDate;
    private Label dateLabel;
    private HorizontalLayout dateLayout;
    private ComboBox<Organization> organizationInput;
    private HorizontalLayout organizationLayout;
    private RadioButtonGroup<SearchBy> searchByInput;
    private TextField patientRecordNumbers;
    private HorizontalLayout patientRecordNumbersLayout;
    private TextField accessionNumbers;
    private HorizontalLayout accessionNumbersLayout;
    private CheckBox normalizeNumbers;
    private HorizontalLayout normalizeNumbersLayout;
    private HorizontalLayout searchByLayout;
    private ListSelect<WordEmbeddingType> wordEmbeddingTypesInput;
    private HorizontalLayout wordEmbeddingsLayout;
    private TextField textField;
    private Label textFieldLabel;
    private HorizontalLayout textFieldLayout;
    private RadioButtonGroup<InputMethod> inputMethodRadioButton;
    private HorizontalLayout inputMethodLayout;
    private Label uploadedFileLabel;
    private HorizontalLayout uploadLayout;
    private TextArea textArea;
    private Label textAreaLabel;
    private HorizontalLayout textAreaLayout;
    private CheckBox asynchronous;
    private HorizontalLayout asynchronousLayout;
    private ByteArrayOutputStream stream;
    private HorizontalLayout queryInputInstanceLayout;
    private ComboBox<WordEmbeddingType> wordEmbeddingInput;
    private TextField fromTokenInput;
    private TextField toTokenInput;
    private TextField edgeRankInput;
    private TextField consecutiveFrequencyInput;
    private TextField nonConsecutiveFrequencyInput;
    private CheckBox notAndAllInput;
    private HorizontalLayout notAndAllLayout;
    private CheckBox addCloseSynonyms;
    private HorizontalLayout addCloseSynonymsLayout;
    private CheckBox addExactSynonyms;
    private HorizontalLayout addExactSynonymsLayout;
    private CheckBox addAbbreviations;
    private HorizontalLayout addAbbreviationsLayout;
    private CheckBox addClasses;
    private HorizontalLayout addClassesLayout;
    private CheckBox addConcepts;
    private HorizontalLayout addConceptsLayout;
    private TextField numberField;
    private Label numberFieldLabel;
    private HorizontalLayout numberFieldLayout;
    private TextField samplePercentage;
    private HorizontalLayout samplePercentageLayout;
    private TextField classifyByToTokens;
    private HorizontalLayout classifyByToTokensLayout;
    private TextField classifyByFromTokens;
    private HorizontalLayout classifyByFromTokensLayout;
    private ListSelect<String> modalities;
    private HorizontalLayout modalitiesLayout;
    private ListSelect<Header> headers;
    private HorizontalLayout headersLayout;

    private List<QueryInputInstance> queryInputInstances;
    private ComboBox<DisplayType> displayModeInput;
    private Button submitButton;
    private Button exportButton;
    private Grid<Token> tokenDetailedSummaryGrid;
    private Grid<TokenPair> tokenPairDetailedSummaryGrid;
    private Grid<WordEmbedding> wordEmbeddingDetailedSummaryGrid;
    private Grid<FromTokenRelationship> edgeRankingDetailedSummaryGrid;
    private Grid<SentenceComparision> edgeComparatorDetailedSummaryGrid;
    private Grid<ClassificationEmbedding> classificationEmbeddingDetailedSummaryGrid;
    private ReportView reportView;
    private FileDownloader fileDownloader;
    private TokenReport tokenReport;
    private TokenPairReport tokenPairReport;
    private WordEmbeddingReport wordEmbeddingReport;
    private EdgeRankingReport edgeRankingReport;
    private EdgeComparatorReport edgeComparatorReport;
    private ClassificationEmbeddingReport classificationEmbeddingReport;

    MachineLearningReportsTab(TabSheet tabSheet, UserService userService, OrganizationService organizationService) {
        this.userService = userService;
        final int CAPTION_WIDTH = 12;
        final int PANEL_CAPTION_WIDTH = 14;
        final int NUMBER_ROWS = 10;
        queryInputInstances = new ArrayList<>();
        machineLearningReportTypeInput = new ComboBox<>();
        machineLearningReportTypeInput.setWidth(22, Unit.EM);
        machineLearningReportTypeInput.setItems(Component.getMachineLearningReportTypes());
        machineLearningReportTypeInput.setValue(Component.getMachineLearningReportTypes().get(0));
        machineLearningReportTypeInput.setEmptySelectionAllowed(false);
        machineLearningReportTypeInput.addValueChangeListener(event -> handleReportTypeValueChange());
        machineLearningReportTypeInput.setTextInputAllowed(false);
        Label machineLearningReportTypeLabel = new Label("Report");
        machineLearningReportTypeLabel.setStyleName("caption-right");
        machineLearningReportTypeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout machineLearningReportLayout = new HorizontalLayout(machineLearningReportTypeLabel, machineLearningReportTypeInput);
        machineLearningReportLayout.setComponentAlignment(machineLearningReportTypeLabel, MIDDLE_RIGHT);

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
        dateLayout.setComponentAlignment(dateLabel, MIDDLE_RIGHT);
        dateLayout.setComponentAlignment(to, Alignment.MIDDLE_CENTER);
        dateLayout.setVisible(false);

        patientRecordNumbers = new TextField();
        patientRecordNumbers.setDescription("Comma Delimited");
        patientRecordNumbers.setWidth(26, Unit.EM);
        Label patientRecordNumbersLabel = new Label("Patient MRN");
        patientRecordNumbersLabel.setStyleName("caption-right");
        patientRecordNumbersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        patientRecordNumbersLayout = new HorizontalLayout(patientRecordNumbersLabel, patientRecordNumbers);
        patientRecordNumbersLayout.setComponentAlignment(patientRecordNumbersLabel, MIDDLE_RIGHT);
        patientRecordNumbersLayout.setVisible(false);

        accessionNumbers = new TextField();
        accessionNumbers.setDescription("Comma Delimited");
        accessionNumbers.setWidth(26, Unit.EM);
        Label accessionNumbersLabel = new Label("Accession Numbers");
        accessionNumbersLabel.setStyleName("caption-right");
        accessionNumbersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        accessionNumbersLayout = new HorizontalLayout(accessionNumbersLabel, accessionNumbers);
        accessionNumbersLayout.setComponentAlignment(accessionNumbersLabel, MIDDLE_RIGHT);
        accessionNumbersLayout.setVisible(false);

        normalizeNumbers = new CheckBox();
        normalizeNumbers.setDescription("Convert numbers to text");
        Label normalizeNumbersLabel = new Label("Normalize Numbers");
        normalizeNumbersLabel.setStyleName("caption-right");
        normalizeNumbersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        normalizeNumbersLayout = new HorizontalLayout(normalizeNumbersLabel, normalizeNumbers);
        normalizeNumbersLayout.setComponentAlignment(normalizeNumbersLabel, Alignment.TOP_RIGHT);
        normalizeNumbersLayout.setComponentAlignment(normalizeNumbers, Alignment.BOTTOM_LEFT);
        normalizeNumbersLayout.setVisible(false);

        organizationInput = new ComboBox<>();
        organizationInput.setWidth(18, Unit.EM);
        organizationInput.setItems(organizationService.getOrganizations());
        organizationInput.setValue(organizationService.getOrganizations().get(0));
        organizationInput.setItemCaptionGenerator(Organization::toString);
        organizationInput.setEmptySelectionAllowed(false);
        organizationInput.setTextInputAllowed(false);
        Label organizationLabel = new Label("Organization");
        organizationLabel.setStyleName("caption-right");
        organizationLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        organizationLayout = new HorizontalLayout(organizationLabel, organizationInput);
        organizationLayout.setComponentAlignment(organizationLabel, MIDDLE_RIGHT);
        organizationLayout.setVisible(false);

        searchByInput = new RadioButtonGroup<>();
        searchByInput.setDescription("Find and process only documents that contain one of the input tokens as [selected searchBy option]");
        searchByInput.setItems(FROM_TOKEN, TO_TOKEN, FROM_TOKEN_OR_TO_TOKEN);
        searchByInput.setSelectedItem(FROM_TOKEN_OR_TO_TOKEN);
        searchByInput.setStyleName(OPTIONGROUP_HORIZONTAL);
        Label searchByLabel = new Label("Search By");
        searchByLabel.setStyleName("caption-right");
        searchByLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        searchByLayout = new HorizontalLayout(searchByLabel, searchByInput);
        searchByLayout.setComponentAlignment(searchByLabel, MIDDLE_RIGHT);
        searchByLayout.setVisible(false);

        inputMethodRadioButton = new RadioButtonGroup<>();
        inputMethodRadioButton.setItems(MANUAL, FILE_UPLOAD);
        inputMethodRadioButton.setSelectedItem(MANUAL);
        inputMethodRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        inputMethodRadioButton.addValueChangeListener(event -> handleInputMethodValueChange());
        Label inputMethodLabel = new Label("Input Method");
        inputMethodLabel.setStyleName("caption-right");
        inputMethodLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        inputMethodLayout = new HorizontalLayout(inputMethodLabel, inputMethodRadioButton);
        inputMethodLayout.setComponentAlignment(inputMethodLabel, MIDDLE_RIGHT);
        inputMethodLayout.setVisible(false);

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
        uploadLayout.setComponentAlignment(uploadLabel, MIDDLE_RIGHT);
        uploadLayout.setVisible(false);

        textArea = new TextArea();
        textArea.setWidth("100%");
        textArea.setRows(15);
        textArea.setRequiredIndicatorVisible(false);
        textAreaLabel = new Label("DYNAMIC - NEED TO SET");
        textAreaLabel.setStyleName("caption-right");
        textAreaLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        textAreaLayout = new HorizontalLayout(textAreaLabel, textArea);
        textAreaLayout.setWidth("100%");
        textAreaLayout.setSizeFull();
        textAreaLayout.setComponentAlignment(textAreaLabel, Alignment.TOP_RIGHT);
        textAreaLayout.setExpandRatio(textArea, 1.0f);

        textField = new TextField();
        textField.setDescription("Find and process only documents that contain one of these comma separated tokens");
        textField.setWidth(22, Unit.EM);
        textFieldLabel = new Label("DYNAMIC - NEED TO SET");
        textFieldLabel.setStyleName("caption-right");
        textFieldLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        textFieldLayout = new HorizontalLayout(textFieldLabel, textField);
        textFieldLayout.setComponentAlignment(textFieldLabel, MIDDLE_RIGHT);
        textFieldLayout.setVisible(false);

        asynchronous = new CheckBox();
        asynchronous.setValue(false); // logic is reversed because 'display report' is synchronous
        Label asynchronousLabel = new Label("Display Report");
        asynchronousLabel.setStyleName("caption-right");
        asynchronousLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        asynchronousLayout = new HorizontalLayout(asynchronousLabel, asynchronous);
        asynchronousLayout.setComponentAlignment(asynchronousLabel, Alignment.TOP_RIGHT);
        asynchronousLayout.setComponentAlignment(asynchronous, Alignment.BOTTOM_LEFT);
        asynchronousLayout.setVisible(false);

        wordEmbeddingTypesInput = new ListSelect<>();
        wordEmbeddingTypesInput.setDescription("Find and process only documents that contain one of these selected wordEmbedding types");
        wordEmbeddingTypesInput.setItems(WordEmbeddingType.getWordEmbeddingTypes());
        wordEmbeddingTypesInput.setRows(NUMBER_ROWS);
        Label wordEmbeddingsLabel = new Label("Word Embedding Types");
        wordEmbeddingsLabel.setStyleName("caption-right");
        wordEmbeddingsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        wordEmbeddingsLayout = new HorizontalLayout(wordEmbeddingsLabel, wordEmbeddingTypesInput);
        wordEmbeddingsLayout.setComponentAlignment(wordEmbeddingsLabel, MIDDLE_RIGHT);
        wordEmbeddingsLayout.setVisible(false);

        Panel panel = new Panel();
        panel.setSizeUndefined();

        wordEmbeddingInput = new ComboBox<>();
        wordEmbeddingInput.setWidth(10, Unit.EM);
        wordEmbeddingInput.setItems(WordEmbeddingType.getWordEmbeddingTypes());
        wordEmbeddingInput.setValue(WordEmbeddingType.getWordEmbeddingTypes().get(0));
        wordEmbeddingInput.setEmptySelectionAllowed(true);
        wordEmbeddingInput.setTextInputAllowed(false);
        Label wordEmbeddingLabel = new Label("Word Embedding Type");
        wordEmbeddingLabel.setStyleName("caption-right");
        wordEmbeddingLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout wordEmbeddingLayout = new HorizontalLayout(wordEmbeddingLabel, wordEmbeddingInput);
        wordEmbeddingLayout.setComponentAlignment(wordEmbeddingLabel, MIDDLE_RIGHT);

        fromTokenInput = new TextField();
        fromTokenInput.setWidth(10, Unit.EM);
        fromTokenInput.setRequiredIndicatorVisible(true);
        Label fromTokenLabel = new Label("From Token");
        fromTokenLabel.setStyleName("caption-right");
        fromTokenLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout fromTokenLayout = new HorizontalLayout(fromTokenLabel, fromTokenInput);
        fromTokenLayout.setComponentAlignment(fromTokenLabel, MIDDLE_RIGHT);

        toTokenInput = new TextField();
        toTokenInput.setWidth(10, Unit.EM);
        toTokenInput.setRequiredIndicatorVisible(true);
        Label toTokenLabel = new Label("To Token");
        toTokenLabel.setStyleName("caption-right");
        toTokenLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout toTokenLayout = new HorizontalLayout(toTokenLabel, toTokenInput);
        toTokenLayout.setComponentAlignment(toTokenLabel, MIDDLE_RIGHT);

        edgeRankInput = new TextField();
        edgeRankInput.setWidth(3, Unit.EM);
        Label edgeRankLabel = new Label("Edge Ranking");
        edgeRankLabel.setStyleName("caption-right");
        edgeRankLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout edgeRankLayout = new HorizontalLayout(edgeRankLabel, edgeRankInput);
        edgeRankLayout.setComponentAlignment(edgeRankLabel, MIDDLE_RIGHT);

        consecutiveFrequencyInput = new TextField();
        consecutiveFrequencyInput.setWidth(3, Unit.EM);
        Label consecutiveFrequencyLabel = new Label("Consecutive Frequency");
        consecutiveFrequencyLabel.setStyleName("caption-right");
        consecutiveFrequencyLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout consecutiveFrequencyLayout = new HorizontalLayout(consecutiveFrequencyLabel, consecutiveFrequencyInput);
        consecutiveFrequencyLayout.setComponentAlignment(consecutiveFrequencyLabel, MIDDLE_RIGHT);

        nonConsecutiveFrequencyInput = new TextField();
        nonConsecutiveFrequencyInput.setWidth(3, Unit.EM);
        Label nonConsecutiveFrequencyLabel = new Label("Non-consecutive Frequency");
        nonConsecutiveFrequencyLabel.setStyleName("caption-right");
        nonConsecutiveFrequencyLabel.setWidth(PANEL_CAPTION_WIDTH, Unit.EM);
        HorizontalLayout nonConsecutiveFrequencyLayout = new HorizontalLayout(nonConsecutiveFrequencyLabel, nonConsecutiveFrequencyInput);
        nonConsecutiveFrequencyLayout.setComponentAlignment(nonConsecutiveFrequencyLabel, MIDDLE_RIGHT);

        Button addQueryInputInstanceButton = new Button("Add", (Button.ClickListener) event -> addQueryInputInstance());
        addQueryInputInstanceButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        VerticalLayout verticalLayout = new VerticalLayout(wordEmbeddingLayout, fromTokenLayout, toTokenLayout, edgeRankLayout, consecutiveFrequencyLayout, nonConsecutiveFrequencyLayout, addQueryInputInstanceButton);
        verticalLayout.setComponentAlignment(addQueryInputInstanceButton, MIDDLE_RIGHT);
        panel.setContent(verticalLayout);

        Label queryInputInstanceLabel = new Label("Query Input Instance");
        queryInputInstanceLabel.setStyleName("caption-right");
        queryInputInstanceLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        queryInputInstanceLayout = new HorizontalLayout(queryInputInstanceLabel, panel);
        queryInputInstanceLayout.setComponentAlignment(queryInputInstanceLabel, Alignment.TOP_RIGHT);
        queryInputInstanceLayout.setVisible(false);

        notAndAllInput = new CheckBox();
        Label notAndAllLabel = new Label("And Not All");
        notAndAllLabel.setStyleName("caption-right");
        notAndAllLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        notAndAllLayout = new HorizontalLayout(notAndAllLabel, notAndAllInput);
        notAndAllLayout.setComponentAlignment(notAndAllLabel, MIDDLE_RIGHT);
        notAndAllLayout.setVisible(false);

        addCloseSynonyms = new CheckBox();
        addCloseSynonyms.setDescription("Add MasterCloseSynonym collection data for input tokens, if provided, and process them");
        Label addCloseSynonymsLabel = new Label("Add Close Synonyms");
        addCloseSynonymsLabel.setStyleName("caption-right");
        addCloseSynonymsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        addCloseSynonymsLayout = new HorizontalLayout(addCloseSynonymsLabel, addCloseSynonyms);
        addCloseSynonymsLayout.setComponentAlignment(addCloseSynonymsLabel, Alignment.TOP_RIGHT);
        addCloseSynonymsLayout.setComponentAlignment(addCloseSynonyms, Alignment.BOTTOM_LEFT);
        addCloseSynonymsLayout.setVisible(false);

        addExactSynonyms = new CheckBox();
        addExactSynonyms.setDescription("Add MasterExactSynonym collection data for input tokens, if provided, and process them");
        Label addExactSynonymsLabel = new Label("Add Exact Synonyms");
        addExactSynonymsLabel.setStyleName("caption-right");
        addExactSynonymsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        addExactSynonymsLayout = new HorizontalLayout(addExactSynonymsLabel, addExactSynonyms);
        addExactSynonymsLayout.setComponentAlignment(addExactSynonymsLabel, Alignment.TOP_RIGHT);
        addExactSynonymsLayout.setComponentAlignment(addExactSynonyms, Alignment.BOTTOM_LEFT);
        addExactSynonymsLayout.setVisible(false);

        addAbbreviations = new CheckBox();
        addAbbreviations.setDescription("Add MasterAbbreviation collection data for input tokens, if provided, and process them");
        Label addAbbreviationsLabel = new Label("Add Abbreviations");
        addAbbreviationsLabel.setStyleName("caption-right");
        addAbbreviationsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        addAbbreviationsLayout = new HorizontalLayout(addAbbreviationsLabel, addAbbreviations);
        addAbbreviationsLayout.setComponentAlignment(addAbbreviationsLabel, Alignment.TOP_RIGHT);
        addAbbreviationsLayout.setComponentAlignment(addAbbreviations, Alignment.BOTTOM_LEFT);
        addAbbreviationsLayout.setVisible(false);

        addClasses = new CheckBox();
        addClasses.setDescription("Add MasterClass collection data for input tokens, if provided, and process them");
        Label addClassesLabel = new Label("Add Classes");
        addClassesLabel.setStyleName("caption-right");
        addClassesLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        addClassesLayout = new HorizontalLayout(addClassesLabel, addClasses);
        addClassesLayout.setComponentAlignment(addClassesLabel, Alignment.TOP_RIGHT);
        addClassesLayout.setComponentAlignment(addClasses, Alignment.BOTTOM_LEFT);
        addClassesLayout.setVisible(false);

        addConcepts = new CheckBox();
        addConcepts.setDescription("Add MasterConcept collection data for input tokens, if provided, and process them");
        Label addConceptsLabel = new Label("Add Concepts");
        addConceptsLabel.setStyleName("caption-right");
        addConceptsLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        addConceptsLayout = new HorizontalLayout(addConceptsLabel, addConcepts);
        addConceptsLayout.setComponentAlignment(addConceptsLabel, Alignment.TOP_RIGHT);
        addConceptsLayout.setComponentAlignment(addConcepts, Alignment.BOTTOM_LEFT);
        addConceptsLayout.setVisible(false);

        numberField = new TextField();
        numberField.setWidth(2, Unit.EM);
        numberFieldLabel = new Label("DYNAMIC - NEED TO SET");
        numberFieldLabel.setStyleName("caption-right");
        numberFieldLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        numberFieldLayout = new HorizontalLayout(numberFieldLabel, numberField);
        numberFieldLayout.setComponentAlignment(numberFieldLabel, MIDDLE_RIGHT);
        numberFieldLayout.setVisible(false);

        samplePercentage = new TextField();
        samplePercentage.setDescription("The percentage of output data exported to CSV file/displayed on UI");
        samplePercentage.setWidth(3, Unit.EM);
        Label samplePercentageLabel = new Label("Sample Percentage");
        samplePercentageLabel.setStyleName("caption-right");
        samplePercentageLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        samplePercentageLayout = new HorizontalLayout(samplePercentageLabel, samplePercentage);
        samplePercentageLayout.setComponentAlignment(samplePercentageLabel, MIDDLE_RIGHT);
        samplePercentageLayout.setVisible(false);

        classifyByToTokens = new TextField();
        classifyByToTokens.setDescription("Add wordEmbeddings containing one of these tokens as toToken to classification input list");
        classifyByToTokens.setWidth(22, Unit.EM);
        Label classifyByToTokensLabel = new Label("Classify By toTokens");
        classifyByToTokensLabel.setStyleName("caption-right");
        classifyByToTokensLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        classifyByToTokensLayout = new HorizontalLayout(classifyByToTokensLabel, classifyByToTokens);
        classifyByToTokensLayout.setComponentAlignment(classifyByToTokensLabel, MIDDLE_RIGHT);
        classifyByToTokensLayout.setVisible(false);

        classifyByFromTokens = new TextField();
        classifyByFromTokens.setDescription("Add wordEmbeddings containing one of these tokens as fromToken to classification input list");
        classifyByFromTokens.setWidth(22, Unit.EM);
        Label classifyByFromTokensLabel = new Label("Classify By fromTokens");
        classifyByFromTokensLabel.setStyleName("caption-right");
        classifyByFromTokensLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        classifyByFromTokensLayout = new HorizontalLayout(classifyByFromTokensLabel, classifyByFromTokens);
        classifyByFromTokensLayout.setComponentAlignment(classifyByFromTokensLabel, MIDDLE_RIGHT);
        classifyByFromTokensLayout.setVisible(false);

        String url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/distinct-values";
        DistinctValuesQueryRequest distinctValuesQueryRequest = new DistinctValuesQueryRequest("modality");
        distinctValuesQueryRequest.setUsername(userService.getUser().getUsername());
        distinctValuesQueryRequest.setOrganizationName(userService.getUser().getOrganizationName());
        modalities = new ListSelect<>();
        modalities.setDescription("Find and process only reports that contain one of these selected modalities");
        ResponseEntity<DistinctValuesQueryResponse> response = new RestTemplate().postForEntity(url, distinctValuesQueryRequest, DistinctValuesQueryResponse.class);
        if (response != null && response.getBody() != null) {
            List<String> possibleModalities = response.getBody().getDistinctValues();
            possibleModalities.removeIf(Objects::isNull); //remove null values from list
            modalities.setItems(possibleModalities);
            modalities.setRows(possibleModalities.size() < 3 ? possibleModalities.size() : 3);
        }
        Label modalitiesLabel = new Label("Modality");
        modalitiesLabel.setStyleName("caption-right");
        modalitiesLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        modalitiesLayout = new HorizontalLayout(modalitiesLabel, modalities);
        modalitiesLayout.setComponentAlignment(modalitiesLabel, Alignment.TOP_RIGHT);
        modalitiesLayout.setVisible(false);

        headers = new ListSelect<>();
        headers.setDescription("Find and process sections of report from one of these selected headers");
        List<Header> possibleHeaders = Header.getHeaders(userService.getUser().getOrganizationName());
        headers.setItems(possibleHeaders);
        headers.setRows(possibleHeaders.size() < 3 ? possibleHeaders.size() : 3);
        Label headersLabel = new Label("Report Headers");
        headersLabel.setStyleName("caption-right");
        headersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        headersLayout = new HorizontalLayout(headersLabel, headers);
        headersLayout.setComponentAlignment(headersLabel, Alignment.TOP_RIGHT);
        headersLayout.setVisible(false);

        displayModeInput = new ComboBox<>();
        displayModeInput.setWidth(12, Unit.EM);
        displayModeInput.setItems(new ArrayList<>(Arrays.asList(DETAILED_SUMMARY, BY_REPORT)));
        displayModeInput.setEmptySelectionAllowed(false);
        displayModeInput.setTextInputAllowed(false);
        displayModeInput.setValue(DETAILED_SUMMARY);
        displayModeInput.setVisible(false);

        submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButtonClick());
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        Label submitButtonSpacer = new Label(" ");
        submitButtonSpacer.setStyleName("caption-right");
        submitButtonSpacer.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout submitButtonLayout = new HorizontalLayout(submitButtonSpacer, displayModeInput, submitButton);
        submitButtonLayout.setComponentAlignment(submitButtonSpacer, MIDDLE_RIGHT);

        tokenDetailedSummaryGrid = new Grid<>();
        tokenDetailedSummaryGrid.setSelectionMode(NONE);
        tokenDetailedSummaryGrid.setSizeFull();
        tokenDetailedSummaryGrid.setVisible(false);
        tokenPairDetailedSummaryGrid = new Grid<>();
        tokenPairDetailedSummaryGrid.setSelectionMode(NONE);
        tokenPairDetailedSummaryGrid.setSizeFull();
        tokenPairDetailedSummaryGrid.setVisible(false);
        wordEmbeddingDetailedSummaryGrid = new Grid<>();
        wordEmbeddingDetailedSummaryGrid.setSelectionMode(NONE);
        wordEmbeddingDetailedSummaryGrid.setSizeFull();
        wordEmbeddingDetailedSummaryGrid.setVisible(false);
        edgeRankingDetailedSummaryGrid = new Grid<>();
        edgeRankingDetailedSummaryGrid.setSelectionMode(NONE);
        edgeRankingDetailedSummaryGrid.setSizeFull();
        edgeRankingDetailedSummaryGrid.setVisible(false);
        edgeComparatorDetailedSummaryGrid = new Grid<>();
        edgeComparatorDetailedSummaryGrid.setSelectionMode(NONE);
        edgeComparatorDetailedSummaryGrid.setSizeFull();
        edgeComparatorDetailedSummaryGrid.setVisible(false);
        classificationEmbeddingDetailedSummaryGrid = new Grid<>();
        classificationEmbeddingDetailedSummaryGrid.setSelectionMode(NONE);
        classificationEmbeddingDetailedSummaryGrid.setSizeFull();
        classificationEmbeddingDetailedSummaryGrid.setVisible(false);
        exportButton = new Button("Export");
        exportButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        exportButton.setVisible(false);
        reportView = new ReportView(MACHINE_LEARNING_REPORTS);
        reportView.setVisible(false);
        fileDownloader = new FileDownloader(new StreamResource((StreamResource.StreamSource) () -> null, "temp.xlsx"));
        fileDownloader.extend(exportButton);
        addComponents(machineLearningReportLayout,organizationLayout,dateTypeLayout,dateLayout,patientRecordNumbersLayout,accessionNumbersLayout,searchByLayout,textFieldLayout,inputMethodLayout,uploadLayout,textAreaLayout,wordEmbeddingsLayout,queryInputInstanceLayout,notAndAllLayout,normalizeNumbersLayout,samplePercentageLayout,addCloseSynonymsLayout,addExactSynonymsLayout,addAbbreviationsLayout,addClassesLayout,addConceptsLayout,numberFieldLayout,classifyByToTokensLayout,classifyByFromTokensLayout,modalitiesLayout,headersLayout,asynchronousLayout);
        addComponent(submitButtonLayout);
        addComponent(tokenDetailedSummaryGrid);
        addComponent(tokenPairDetailedSummaryGrid);
        addComponent(wordEmbeddingDetailedSummaryGrid);
        addComponent(edgeRankingDetailedSummaryGrid);
        addComponent(edgeComparatorDetailedSummaryGrid);
        addComponent(classificationEmbeddingDetailedSummaryGrid);
        addComponent(exportButton);
        addComponent(reportView);
        tabSheet.addTab(this, MACHINE_LEARNING_REPORTS.toString());
        handleReportTypeValueChange();
    }

    private void handleDateTypeValueChange() {
        dateLabel.setValue(dateTypeRadioButton.getValue().toString());
    }

    private void handleReportTypeValueChange() {
        switch (machineLearningReportTypeInput.getValue()) {
            case MACHINE_LEARNING_FILES:
                setInputVisibility(true, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(false, false);
                break;
            case CONFIDENCE_SCORE_REPORT:
                setInputVisibility(true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(false, false);
                break;
            case TOKEN_SIMILARITY_REPORT:
                textFieldLabel.setValue("File Path");
                textAreaLabel.setValue("Text");
                textArea.setDescription("");
                setInputVisibility(true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false);
                setInputRequired(false, false);
                break;
            case THREE_TOKENS_PATTERN_REPORT:
                textFieldLabel.setValue("File Path");
                textAreaLabel.setValue("Text");
                textArea.setDescription("");
                setInputVisibility(true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false);
                setInputRequired(false, false);
                break;
            case FOUR_TOKENS_PATTERN_REPORT:
                textFieldLabel.setValue("File Path");
                textAreaLabel.setValue("Text");
                textArea.setDescription("");
                setInputVisibility(true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false);
                setInputRequired(false, false);
                break;
            case N_TOKENS_PATTERN_REPORT:
                numberFieldLabel.setValue("Length (1-7)");
                textFieldLabel.setValue("File Path");
                textAreaLabel.setValue("Text");
                textArea.setDescription("");
                setInputVisibility(true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, true, false, false, false);
                setInputRequired(false, false);
                break;
            case PATTERN_ANALYSIS_REPORT:
                textFieldLabel.setValue("File Path");
                textAreaLabel.setValue("Text");
                textArea.setDescription("");
                setInputVisibility(true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false);
                setInputRequired(false, false);
                break;
            case TOKEN_CLASS:
                textAreaLabel.setValue("Tokens");
                textArea.setDescription("Tokens to be contained in the report for which the class is to be determined");
                setInputVisibility(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false);
                break;
            case TOKEN:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false);
                setInputRequired(true, false);
                break;
            case TOKEN_PAIR:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, true, true, true, true, true, false, true, false, false, false, true, true, true, true, true, false, false, false, false, false, false, true, true);
                setInputRequired(true, false);
                break;
            case WORD_EMBEDDING:
                setInputVisibility(true, true, true, true, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(true, true);
                break;
            case WORD_EMBEDDING_AND_RANK:
                setInputVisibility(true, true, true, true, true, true, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(true, true);
                break;
            case EXISTENCE_TOKEN:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, true, true, true, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(false, false);
                break;
            case EDGE_RANKING:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(false, false);
                break;
            case EDGE_WEIGHT:
            case EDGE_COMPARATOR:
                setInputVisibility(true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(false, false);
            case TOKEN_AND_WEIGHT:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, true, true, true, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
                setInputRequired(true, false);
                break;
            case DYNAMIC_QUERY:
                break;
            case REPORT_LEVEL_WORD_EMBEDDING:
                textFieldLabel.setValue("Tokens");
                setInputVisibility(true, true, true, true, true, true, false, true, true, false, false, true, true, true, true, true, false, false, false, false, false, false, true, true);
                setInputRequired(false, false);
                break;
            case LABEL_WORD_EMBEDDING_CLASSIFICATION:
                textFieldLabel.setValue("Tokens");
                numberFieldLabel.setValue("Number Top Embeddings");
                setInputVisibility(true, true, true, false, false, true, false, true, true, false, false, true, true, true, false, false, true, true, true, false, false, false, false, false);
                setInputRequired(false, false);
                break;
            case TOKEN_PAIR_WIDE_OPEN:
                setInputVisibility(true, true, true, true, true, false, false, false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, true);
        }
        initNewQuery();
    }

    private void setInputVisibility(boolean organizationName, boolean asynchronous, boolean date, boolean patientRecordNumbers, boolean accessionNumbers, boolean normalizeNumbers, boolean searchBy, boolean textField, boolean wordEmbeddings, boolean queryInputInstance, boolean andNotAll, boolean addCloseSynonyms, boolean addExactSynonyms, boolean addAbbreviations, boolean addClasses, boolean addConcepts, boolean numberField, boolean classifyByToTokens, boolean classifyByFromTokens, boolean inputMethod, boolean textArea, boolean samplePercentage, boolean modalities, boolean headers) {
        organizationLayout.setVisible(organizationName);
        asynchronousLayout.setVisible(asynchronous);
        dateTypeLayout.setVisible(date);
        dateLayout.setVisible(date);
        patientRecordNumbersLayout.setVisible(patientRecordNumbers);
        accessionNumbersLayout.setVisible(accessionNumbers);
        normalizeNumbersLayout.setVisible(normalizeNumbers);
        searchByLayout.setVisible(searchBy);
        textFieldLayout.setVisible(textField);
        wordEmbeddingsLayout.setVisible(wordEmbeddings);
        queryInputInstanceLayout.setVisible(queryInputInstance);
        notAndAllLayout.setVisible(andNotAll);
        addCloseSynonymsLayout.setVisible(addCloseSynonyms);
        addExactSynonymsLayout.setVisible(addExactSynonyms);
        addAbbreviationsLayout.setVisible(addAbbreviations);
        addClassesLayout.setVisible(addClasses);
        addConceptsLayout.setVisible(addConcepts);
        numberFieldLayout.setVisible(numberField);
        this.numberField.setRequiredIndicatorVisible(true);
        classifyByToTokensLayout.setVisible(classifyByToTokens);
        classifyByFromTokensLayout.setVisible(classifyByFromTokens);
        inputMethodLayout.setVisible(inputMethod);
        textAreaLayout.setVisible(textArea);
        samplePercentageLayout.setVisible(samplePercentage);
        modalitiesLayout.setVisible(modalities);
        headersLayout.setVisible(headers);
    }

    private void setInputRequired(boolean text, boolean wordEmbeddings) {
        textField.setRequiredIndicatorVisible(text);
        wordEmbeddingTypesInput.setRequiredIndicatorVisible(wordEmbeddings);
    }

    private void addQueryInputInstance() {
        QueryInputInstance queryInputInstance = new QueryInputInstance(wordEmbeddingInput.getValue(), fromTokenInput.getValue(), toTokenInput.getValue());
        if (edgeRankInput.getValue() != null && !edgeRankInput.getValue().isEmpty())
            queryInputInstance.setEdgeRank(Integer.valueOf(edgeRankInput.getValue()));
        if (consecutiveFrequencyInput.getValue() != null && !consecutiveFrequencyInput.getValue().isEmpty())
            queryInputInstance.setConsecutiveFrequency(Double.valueOf(consecutiveFrequencyInput.getValue()));
        if (nonConsecutiveFrequencyInput.getValue() != null && !nonConsecutiveFrequencyInput.getValue().isEmpty())
            queryInputInstance.setNonConsecutiveFrequency(Double.valueOf(nonConsecutiveFrequencyInput.getValue()));
        queryInputInstances.add(queryInputInstance);
        wordEmbeddingInput.clear();
        fromTokenInput.clear();
        toTokenInput.clear();
        edgeRankInput.clear();
        consecutiveFrequencyInput.clear();
        nonConsecutiveFrequencyInput.clear();
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
        if (machineLearningReportTypeInput.getValue().getApplication() == MACHINE_LEARNING_UI) {
           System.out.println("ERROR - not yet implemented");
        }
        else {
            String url;
            boolean isAsynchronous = !asynchronous.getValue();
            if (machineLearningReportTypeInput.getValue().getApplication() == ApplicationName.MACHINE_LEARNING) {
                url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/report";
                MachineLearningReportRequest reportRequest = new MachineLearningReportRequest();
                reportRequest.setUsername(userService.getUser().getUsername());
                switch (machineLearningReportTypeInput.getValue()) {
                    case MACHINE_LEARNING_FILES:
                        reportRequest.setReportType(MACHINE_LEARNING_FILES);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        break;
                    case CONFIDENCE_SCORE_REPORT:
                        reportRequest.setReportType(CONFIDENCE_SCORE_REPORT);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        break;
                    case TOKEN_SIMILARITY_REPORT:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/pattern/token-similarity";
                        reportRequest.setReportType(TOKEN_SIMILARITY_REPORT);
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        break;
                    case THREE_TOKENS_PATTERN_REPORT:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/pattern/three-tokens";
                        reportRequest.setReportType(THREE_TOKENS_PATTERN_REPORT);
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        break;
                    case FOUR_TOKENS_PATTERN_REPORT:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/pattern/four-tokens";
                        reportRequest.setReportType(FOUR_TOKENS_PATTERN_REPORT);
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        break;
                    case N_TOKENS_PATTERN_REPORT:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/pattern/n-tokens";
                        reportRequest.setReportType(N_TOKENS_PATTERN_REPORT);
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setNumber(Integer.parseInt(numberField.getValue()));
                        break;
                    case PATTERN_ANALYSIS_REPORT:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning/pattern-analysis";
                        reportRequest.setReportType(PATTERN_ANALYSIS_REPORT);
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setData(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        break;
                    default:
                        Notification.show("Implementation does not exist for " + machineLearningReportTypeInput.getValue(), Notification.Type.ERROR_MESSAGE);
                        return;
                }
                ResponseEntity<AsyncResponseData> response = new RestTemplate().postForEntity(url, reportRequest, AsyncResponseData.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    AsyncResponseData asyncResponseData = response.getBody();
                    Notification notification = new Notification("Request Number " + asyncResponseData.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                    notification.setDelayMsec(120000);
                    notification.show(Page.getCurrent());
                } else
                    Notification.show(response.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
            } else if (machineLearningReportTypeInput.getValue().getApplication() == ApplicationName.MACHINE_LEARNING_QUERY) {
                ResponseType responseType = null;
                MachineLearningQueryReportRequest reportRequest = new MachineLearningQueryReportRequest();
                reportRequest.setUsername(userService.getUser().getUsername());
                switch (machineLearningReportTypeInput.getValue()) {
                    case TOKEN:
                        responseType = TOKEN_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/token-report";
                        reportRequest.setRequestType(TOKEN);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setSamplePercentage(samplePercentage.getValue() != null && !samplePercentage.getValue().isEmpty() ? Integer.parseInt(samplePercentage.getValue()) : 0);
                        ReportInput reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setSearchBy(searchByInput.getValue());
                        reportInput.setTokens(Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<TokenReport> tokenResponse = new RestTemplate().postForEntity(url, reportRequest, TokenReport.class);
                        if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
                            tokenReport = tokenResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + tokenReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!tokenReport.getTokens().isEmpty()) {
                                setDetailedSummaryGrid(TOKEN_REPORT);
                                showDetailedSummaryGrid(TOKEN_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(tokenResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case TOKEN_PAIR:
                        responseType = TOKEN_PAIR_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/token-pair-report";
                        reportRequest.setRequestType(TOKEN_PAIR);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setAddCloseSynonyms(addCloseSynonyms.getValue());
                        reportInput.setAddExactSynonyms(addExactSynonyms.getValue());
                        reportInput.setAddAbbreviations(addAbbreviations.getValue());
                        reportInput.setAddClasses(addClasses.getValue());
                        reportInput.setAddConcepts(addConcepts.getValue());
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setTokens(Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
                        reportInput.setModalities(new ArrayList<>(modalities.getValue()));
                        reportInput.setHeaders(new ArrayList<>(headers.getValue()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<TokenPairReport> tokenPairResponse = new RestTemplate().postForEntity(url, reportRequest, TokenPairReport.class);
                        if (tokenPairResponse.getStatusCode() == HttpStatus.OK && tokenPairResponse.getBody() != null) {
                            tokenPairReport = tokenPairResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + tokenPairReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!tokenPairReport.getTokenPairs().isEmpty()) {
                                setDetailedSummaryGrid(TOKEN_PAIR_REPORT);
                                showDetailedSummaryGrid(TOKEN_PAIR_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(tokenPairResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case WORD_EMBEDDING:
                        responseType = WORD_EMBEDDING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/word-embedding-report";
                        reportRequest.setRequestType(WORD_EMBEDDING);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setWordEmbeddingTypes(new ArrayList<>(wordEmbeddingTypesInput.getValue()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<WordEmbeddingReport> wordEmbeddingResponse = new RestTemplate().postForEntity(url, reportRequest, WordEmbeddingReport.class);
                        if (wordEmbeddingResponse.getStatusCode() == HttpStatus.OK && wordEmbeddingResponse.getBody() != null) {
                            wordEmbeddingReport = wordEmbeddingResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + wordEmbeddingReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!wordEmbeddingReport.getWordEmbeddings().isEmpty()) {
                                setDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                                showDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(wordEmbeddingResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case WORD_EMBEDDING_AND_RANK:
                        responseType = WORD_EMBEDDING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/word-embedding-report";
                        reportRequest.setRequestType(WORD_EMBEDDING_AND_RANK);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportRequest.setReportInput(reportInput);
                        QueryInput queryInput = new QueryInput();
                        queryInput.setOrganizationName(organizationInput.getValue().getName());
                        queryInput.setQueryInstances(queryInputInstances);
                        queryInput.setAndNotAll(notAndAllInput.getValue());
                        reportRequest.setQueryInput(queryInput);
                        ResponseEntity<WordEmbeddingReport> wordEmbeddingAndRankResponse = new RestTemplate().postForEntity(url, reportRequest, WordEmbeddingReport.class);
                        if (wordEmbeddingAndRankResponse.getStatusCode() == HttpStatus.OK && wordEmbeddingAndRankResponse.getBody() != null) {
                            wordEmbeddingReport = wordEmbeddingAndRankResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + wordEmbeddingReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!wordEmbeddingReport.getWordEmbeddings().isEmpty()) {
                                setDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                                showDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(wordEmbeddingAndRankResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case EXISTENCE_TOKEN:
                        responseType = WORD_EMBEDDING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/word-embedding-report";
                        reportRequest.setRequestType(EXISTENCE_TOKEN);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setTokens(Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<WordEmbeddingReport> existenceTokenResponse = new RestTemplate().postForEntity(url, reportRequest, WordEmbeddingReport.class);
                        if (existenceTokenResponse.getStatusCode() == HttpStatus.OK && existenceTokenResponse.getBody() != null) {
                            wordEmbeddingReport = existenceTokenResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + wordEmbeddingReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!wordEmbeddingReport.getWordEmbeddings().isEmpty()) {
                                setDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                                showDetailedSummaryGrid(WORD_EMBEDDING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(existenceTokenResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case EDGE_RANKING:
                        responseType = EDGE_RANKING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/edge-ranking-report";
                        reportRequest.setRequestType(EDGE_RANKING);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(false);
                        reportInput = new ReportInput();
                        reportInput.setSearchBy(searchByInput.getValue());
                        reportInput.setTokens(Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<EdgeRankingReport> edgeRankingResponse = new RestTemplate().postForEntity(url, reportRequest, EdgeRankingReport.class);
                        if (edgeRankingResponse.getStatusCode() == HttpStatus.OK && edgeRankingResponse.getBody() != null) {
                            edgeRankingReport = edgeRankingResponse.getBody();
                            if (edgeRankingReport.getFromTokenRelationships() != null && !edgeRankingReport.getFromTokenRelationships().isEmpty()) {
                                setDetailedSummaryGrid(EDGE_RANKING_REPORT);
                                showDetailedSummaryGrid(EDGE_RANKING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(edgeRankingResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case EDGE_WEIGHT:
                        responseType = TOKEN_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/token-report";
                        reportRequest.setRequestType(EDGE_WEIGHT);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<TokenReport> edgeWeightResponse = new RestTemplate().postForEntity(url, reportRequest, TokenReport.class);
                        if (edgeWeightResponse.getStatusCode() == HttpStatus.OK && edgeWeightResponse.getBody() != null) {
                            tokenReport = edgeWeightResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + tokenReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!tokenReport.getTokens().isEmpty()) {
                                setDetailedSummaryGrid(TOKEN_REPORT);
                                showDetailedSummaryGrid(TOKEN_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(edgeWeightResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case EDGE_COMPARATOR:
                        responseType = EDGE_COMPARATOR_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/edge-comparator-report";
                        reportRequest.setRequestType(EDGE_COMPARATOR);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<EdgeComparatorReport> edgeComparatorResponse = new RestTemplate().postForEntity(url, reportRequest, EdgeComparatorReport.class);
                        if (edgeComparatorResponse.getStatusCode() == HttpStatus.OK && edgeComparatorResponse.getBody() != null) {
                            edgeComparatorReport = edgeComparatorResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + edgeComparatorReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!edgeComparatorReport.getSentenceComparisons().isEmpty()) {
                                setDetailedSummaryGrid(EDGE_COMPARATOR_REPORT);
                                showDetailedSummaryGrid(EDGE_COMPARATOR_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(edgeComparatorResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case TOKEN_AND_WEIGHT:
                        responseType = TOKEN_PAIR_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/token-pair-report";
                        reportRequest.setRequestType(TOKEN_AND_WEIGHT);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setTokens(Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<TokenPairReport> tokenAndWeightResponse = new RestTemplate().postForEntity(url, reportRequest, TokenPairReport.class);
                        if (tokenAndWeightResponse.getStatusCode() == HttpStatus.OK && tokenAndWeightResponse.getBody() != null) {
                            tokenPairReport = tokenAndWeightResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + tokenPairReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!tokenPairReport.getTokenPairs().isEmpty()) {
                                setDetailedSummaryGrid(EDGE_COMPARATOR_REPORT);
                                showDetailedSummaryGrid(EDGE_COMPARATOR_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(tokenAndWeightResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case DYNAMIC_QUERY:
                        break;
                    case REPORT_LEVEL_WORD_EMBEDDING:
                        responseType = CLASSIFICATION_EMBEDDING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/embedding-classification-report";
                        reportRequest.setRequestType(REPORT_LEVEL_WORD_EMBEDDING);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setTokens(!textField.getValue().isEmpty() ? Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput.setWordEmbeddingTypes(!wordEmbeddingTypesInput.getValue().isEmpty() ? new ArrayList<>(wordEmbeddingTypesInput.getValue()) : new ArrayList<>());
                        reportInput.setAddCloseSynonyms(addCloseSynonyms.getValue());
                        reportInput.setAddExactSynonyms(addExactSynonyms.getValue());
                        reportInput.setAddAbbreviations(addAbbreviations.getValue());
                        reportInput.setAddClasses(addClasses.getValue());
                        reportInput.setAddConcepts(addConcepts.getValue());
                        reportInput.setModalities(new ArrayList<>(modalities.getValue()));
                        reportInput.setHeaders(new ArrayList<>(headers.getValue()));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<ClassificationEmbeddingReport> reportLevelWordEmbeddingResponse = new RestTemplate().postForEntity(url, reportRequest, ClassificationEmbeddingReport.class);
                        if (reportLevelWordEmbeddingResponse.getStatusCode() == HttpStatus.OK && reportLevelWordEmbeddingResponse.getBody() != null) {
                            classificationEmbeddingReport = reportLevelWordEmbeddingResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + classificationEmbeddingReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!classificationEmbeddingReport.getWordEmbeddings().isEmpty()) {
                                setDetailedSummaryGrid(CLASSIFICATION_EMBEDDING_REPORT);
                                showDetailedSummaryGrid(CLASSIFICATION_EMBEDDING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(reportLevelWordEmbeddingResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case LABEL_WORD_EMBEDDING_CLASSIFICATION:
                        responseType = CLASSIFICATION_EMBEDDING_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/embedding-classification-report";
                        reportRequest.setRequestType(LABEL_WORD_EMBEDDING_CLASSIFICATION);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportInput = new ReportInput();
                        reportInput.setNormalizeNumbers(normalizeNumbers.getValue());
                        reportInput.setTokens(!textField.getValue().isEmpty() ? Arrays.stream(textField.getValue().split(",")).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput.setWordEmbeddingTypes(!wordEmbeddingTypesInput.getValue().isEmpty() ? new ArrayList<>(wordEmbeddingTypesInput.getValue()) : new ArrayList<>());
                        reportInput.setAddCloseSynonyms(addCloseSynonyms.getValue());
                        reportInput.setAddExactSynonyms(addExactSynonyms.getValue());
                        reportInput.setAddAbbreviations(addAbbreviations.getValue());
                        reportInput.setNumOfTopEmbeddings(Integer.parseInt(numberField.getValue()));
                        reportInput.setClassifyFromTokens(!classifyByFromTokens.getValue().isEmpty() ? Arrays.stream(classifyByFromTokens.getValue().split(",")).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput.setClassifyToTokens(!classifyByToTokens.getValue().isEmpty() ? Arrays.stream(classifyByToTokens.getValue().split(",")).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<ClassificationEmbeddingReport> labelWordEmbeddingClassificationResponse = new RestTemplate().postForEntity(url, reportRequest, ClassificationEmbeddingReport.class);
                        if (labelWordEmbeddingClassificationResponse.getStatusCode() == HttpStatus.OK && labelWordEmbeddingClassificationResponse.getBody() != null) {
                            classificationEmbeddingReport = labelWordEmbeddingClassificationResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + classificationEmbeddingReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!classificationEmbeddingReport.getWordEmbeddings().isEmpty()) {
                                setDetailedSummaryGrid(CLASSIFICATION_EMBEDDING_REPORT);
                                showDetailedSummaryGrid(CLASSIFICATION_EMBEDDING_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else {
                            Notification.show(labelWordEmbeddingClassificationResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        }
                        break;
                    case TOKEN_PAIR_WIDE_OPEN:
                        responseType = TOKEN_PAIR_REPORT;
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/token-pair-report";
                        reportRequest.setRequestType(TOKEN_PAIR_WIDE_OPEN);
                        reportRequest.setOrganizationName(organizationInput.getValue().getName());
                        reportRequest.setExecuteAsynchronously(isAsynchronous);
                        if (dateTypeRadioButton.getValue().equals(DATE_PROCESSED))
                            reportRequest.setDateProcessedRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        else
                            reportRequest.setReportDateRange(new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())));
                        reportRequest.setPatientRecordNumbers(patientRecordNumbers.getValue() != null && !patientRecordNumbers.getValue().isEmpty() ? Arrays.stream(patientRecordNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportRequest.setAccessionNumbers(accessionNumbers.getValue() != null && !accessionNumbers.getValue().isEmpty() ? Arrays.stream(accessionNumbers.getValue().split(",", -1)).map(String::trim).collect(Collectors.toList()) : new ArrayList<>());
                        reportInput = new ReportInput();
                        reportInput.setAddCloseSynonyms(addCloseSynonyms.getValue());
                        reportInput.setAddExactSynonyms(addExactSynonyms.getValue());
                        reportInput.setAddAbbreviations(addAbbreviations.getValue());
                        reportInput.setAddClasses(addClasses.getValue());
                        reportInput.setAddConcepts(addConcepts.getValue());
                        reportInput.setHeaders(new ArrayList<>(headers.getValue()));
                        reportRequest.setReportInput(reportInput);
                        tokenPairResponse = new RestTemplate().postForEntity(url, reportRequest, TokenPairReport.class);
                        if (tokenPairResponse.getStatusCode() == HttpStatus.OK && tokenPairResponse.getBody() != null) {
                            tokenPairReport = tokenPairResponse.getBody();
                            if (isAsynchronous) {
                                Notification notification = new Notification("Request Number " + tokenPairReport.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                                notification.setDelayMsec(120000);
                                notification.show(Page.getCurrent());
                            } else if (!tokenPairReport.getTokenPairs().isEmpty()) {
                                setDetailedSummaryGrid(TOKEN_PAIR_REPORT);
                                showDetailedSummaryGrid(TOKEN_PAIR_REPORT);
                            } else {
                                Notification notification = new Notification("No results found", Notification.Type.HUMANIZED_MESSAGE);
                                notification.setDelayMsec(5000);
                                notification.show(Page.getCurrent());
                            }
                        } else
                            Notification.show(tokenPairResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    case TOKEN_CLASS:
                        url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/report";
                        reportRequest.setRequestType(TOKEN_CLASS);
                        reportRequest.setOrganizationName(userService.getUser().getOrganizationName());
                        reportRequest.setExecuteAsynchronously(true);
                        reportInput = new ReportInput();
                        reportInput.setTokens(Arrays.asList(textArea.getValue().split("\\r?\\n")));
                        reportRequest.setReportInput(reportInput);
                        ResponseEntity<AsyncResponseData> asyncResponseDataResponse = new RestTemplate().postForEntity(url, reportRequest, AsyncResponseData.class);
                        if (asyncResponseDataResponse.getStatusCode() == HttpStatus.OK && asyncResponseDataResponse.getBody() != null) {
                            AsyncResponseData asyncResponseData = asyncResponseDataResponse.getBody();
                            Notification notification = new Notification("Request Number " + asyncResponseData.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
                            notification.setDelayMsec(120000);
                            notification.show(Page.getCurrent());
                        } else
                            Notification.show(asyncResponseDataResponse.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
                        break;
                    default:
                        Notification.show("Not yet implemented", Notification.Type.HUMANIZED_MESSAGE);
                }
                if (responseType != null) {
                    StreamResource detailedSummaryFileStream = createFile(responseType, DETAILED_SUMMARY);
                    switch (displayModeInput.getValue()) {
                        case SUMMARY:
                            fileDownloader.setFileDownloadResource(createFile(responseType, SUMMARY));
                            break;
                        case DETAILED_SUMMARY:
                            fileDownloader.setFileDownloadResource(detailedSummaryFileStream);
                    }
                }
                reportView.displayReportAtIndex(0);
            }
        }
    }

    private void setDetailedSummaryGrid(ResponseType responseType) {
        switch (responseType) {
            case TOKEN_REPORT:
                tokenDetailedSummaryGrid.setItems(tokenReport.getTokens());
                tokenDetailedSummaryGrid.removeAllColumns();
                tokenDetailedSummaryGrid.setHeightByRows(tokenReport.getTokens().size());
                tokenDetailedSummaryGrid.addColumn(Token::getReportNumber).setCaption("Accession Number");
                tokenDetailedSummaryGrid.addColumn(Token::getFromToken).setCaption("From Token");
                tokenDetailedSummaryGrid.addColumn(Token::getToToken).setCaption("To Token");
                tokenDetailedSummaryGrid.addColumn(Token::getSentence).setCaption("Sentence");
                break;
            case TOKEN_PAIR_REPORT:
                tokenPairDetailedSummaryGrid.setItems(tokenPairReport.getTokenPairs());
                tokenPairDetailedSummaryGrid.removeAllColumns();
                tokenPairDetailedSummaryGrid.setHeightByRows(tokenPairReport.getTokenPairs().size());
                tokenPairDetailedSummaryGrid.addColumn(TokenPair::getReportNumber).setCaption("Accession Number");
                tokenPairDetailedSummaryGrid.addColumn(TokenPair::getFromTokensAsString).setCaption("From Tokens");
                tokenPairDetailedSummaryGrid.addColumn(TokenPair::getToToken).setCaption("To Token");
                tokenPairDetailedSummaryGrid.addColumn(TokenPair::getSentence).setCaption("Sentence");
                break;
            case WORD_EMBEDDING_REPORT:
                wordEmbeddingDetailedSummaryGrid.setItems(wordEmbeddingReport.getWordEmbeddings());
                wordEmbeddingDetailedSummaryGrid.removeAllColumns();
                wordEmbeddingDetailedSummaryGrid.setHeightByRows(wordEmbeddingReport.getWordEmbeddings().size());
                wordEmbeddingDetailedSummaryGrid.addColumn(WordEmbedding::getReportNumber).setCaption("Accession Number");
                wordEmbeddingDetailedSummaryGrid.addColumn(WordEmbedding::getFromToken).setCaption("From Token");
                wordEmbeddingDetailedSummaryGrid.addColumn(WordEmbedding::getToToken).setCaption("To Token");
                wordEmbeddingDetailedSummaryGrid.addColumn(WordEmbedding::getWordEmbeddingType).setCaption("Word Embedding Type");
                wordEmbeddingDetailedSummaryGrid.addColumn(WordEmbedding::getSentence).setCaption("Sentence");
                break;
            case EDGE_RANKING_REPORT:
                edgeRankingDetailedSummaryGrid.setItems(edgeRankingReport.getFromTokenRelationships());
                edgeRankingDetailedSummaryGrid.removeAllColumns();
                edgeRankingDetailedSummaryGrid.setHeightByRows(edgeRankingReport.getFromTokenRelationships().size());
                edgeRankingDetailedSummaryGrid.addColumn(FromTokenRelationship::getToTokenWord).setCaption("To Token");
                edgeRankingDetailedSummaryGrid.addColumn(FromTokenRelationship::getFromTokenWord).setCaption("From Token");
                edgeRankingDetailedSummaryGrid.addColumn(FromTokenRelationship::getEdgeRanking).setCaption("Edge Ranking");
                edgeRankingDetailedSummaryGrid.addColumn(FromTokenRelationship::getConsecutiveFrequency).setCaption("Consecutive Frequency");
                edgeRankingDetailedSummaryGrid.addColumn(FromTokenRelationship::getNonConsecutiveFrequency).setCaption("Non-consecutive Frequency");
                break;
            case EDGE_COMPARATOR_REPORT:
                edgeComparatorDetailedSummaryGrid.setItems(edgeComparatorReport.getSentenceComparisons());
                edgeComparatorDetailedSummaryGrid.removeAllColumns();
                edgeComparatorDetailedSummaryGrid.setHeightByRows(edgeComparatorReport.getSentenceComparisons().size());
                edgeComparatorDetailedSummaryGrid.addColumn(SentenceComparision::getCollectionName).setCaption("Collection");
                edgeComparatorDetailedSummaryGrid.addColumn(SentenceComparision::getSentence).setCaption("Sentence");
                edgeComparatorDetailedSummaryGrid.addColumn(SentenceComparision::getNumberTokenRelationships).setCaption("Token Relationship Count");
                edgeComparatorDetailedSummaryGrid.addColumn(SentenceComparision::getTokenRelationships).setCaption("Token Relationships");
                break;
            case CLASSIFICATION_EMBEDDING_REPORT:
                classificationEmbeddingDetailedSummaryGrid.setItems(classificationEmbeddingReport.getWordEmbeddings());
                classificationEmbeddingDetailedSummaryGrid.removeAllColumns();
                classificationEmbeddingDetailedSummaryGrid.setHeightByRows(classificationEmbeddingReport.getWordEmbeddings().size());
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getReportNumber).setCaption("Report Number");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getFromToken).setCaption("From Token");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getToToken).setCaption("To Token");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getEdgeName).setCaption("Edge Name");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getWordEmbeddingType).setCaption("Word Embedding Type");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getSentence).setCaption("Sentence");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getFromTokensAsString).setCaption("From Tokens");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getWordEmbeddingFrequency).setCaption("Word Embedding Frequency");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getReportFrequency).setCaption("Report Frequency");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getClassificationLetter).setCaption("Classification Letter");
                classificationEmbeddingDetailedSummaryGrid.addColumn(ClassificationEmbedding::getSentencePosition).setCaption("Sentence Position");
        }
    }

    private void showDetailedSummaryGrid(ResponseType responseType) {
        switch (responseType) {
            case TOKEN_REPORT:
                edgeComparatorDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(false);
                tokenDetailedSummaryGrid.setVisible(true);
                classificationEmbeddingDetailedSummaryGrid.setVisible(false);
                break;
            case TOKEN_PAIR_REPORT:
                edgeComparatorDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(false);
                tokenDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(true);
                classificationEmbeddingDetailedSummaryGrid.setVisible(false);
                break;
            case WORD_EMBEDDING_REPORT:
                edgeComparatorDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(false);
                tokenDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(true);
                classificationEmbeddingDetailedSummaryGrid.setVisible(false);
                break;
            case EDGE_RANKING_REPORT:
                edgeComparatorDetailedSummaryGrid.setVisible(false);
                tokenDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(true);
                classificationEmbeddingDetailedSummaryGrid.setVisible(false);
                break;
            case EDGE_COMPARATOR_REPORT:
                tokenDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(false);
                edgeComparatorDetailedSummaryGrid.setVisible(true);
                classificationEmbeddingDetailedSummaryGrid.setVisible(false);
                break;
            case CLASSIFICATION_EMBEDDING_REPORT:
                tokenDetailedSummaryGrid.setVisible(false);
                tokenPairDetailedSummaryGrid.setVisible(false);
                wordEmbeddingDetailedSummaryGrid.setVisible(false);
                edgeRankingDetailedSummaryGrid.setVisible(false);
                edgeComparatorDetailedSummaryGrid.setVisible(false);
                classificationEmbeddingDetailedSummaryGrid.setVisible(true);
        }
        exportButton.setVisible(true);
    }

    private void initNewQuery() {
        edgeComparatorDetailedSummaryGrid.setVisible(false);
        edgeRankingDetailedSummaryGrid.setVisible(false);
        tokenDetailedSummaryGrid.setVisible(false);
        tokenPairDetailedSummaryGrid.setVisible(false);
        wordEmbeddingDetailedSummaryGrid.setVisible(false);
        classificationEmbeddingDetailedSummaryGrid.setVisible(false);
        exportButton.setVisible(false);
        reportView.setVisible(false);
        submitButton.setEnabled(true);
    }

    @SuppressWarnings("unchecked")
    private StreamResource createFile(ResponseType responseType, DisplayType displayType) {
        return new StreamResource((StreamResource.StreamSource) () -> {
            Workbook workbook = new XSSFWorkbook();
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            Row headerRow = sheet.createRow(0);
            List<String> columns = new ArrayList<>();
            switch (displayType) {
                case SUMMARY:
                    break;
                case DETAILED_SUMMARY:
                    switch (responseType) {
                        case TOKEN_REPORT:
                            tokenDetailedSummaryGrid.getColumns().forEach(col -> columns.add(col.getCaption()));
                            break;
                        case TOKEN_PAIR_REPORT:
                            tokenPairDetailedSummaryGrid.getColumns().forEach(col -> columns.add(col.getCaption()));
                            break;
                        case WORD_EMBEDDING_REPORT:
                            wordEmbeddingDetailedSummaryGrid.getColumns().forEach(col -> columns.add(col.getCaption()));
                            break;
                        case EDGE_RANKING_REPORT:
                            edgeRankingDetailedSummaryGrid.getColumns().forEach(col -> columns.add(col.getCaption()));
                            break;
                        case EDGE_COMPARATOR_REPORT:
                            edgeComparatorDetailedSummaryGrid.getColumns().forEach(col -> columns.add(col.getCaption()));
                    }
            }
            int index = 0;
            for (String caption : columns) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(caption);
                cell.setCellStyle(headerCellStyle);
                ++index;
            }
            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat(Formatting.dateFormat));
            index = 0;
            switch (displayType) {
                case SUMMARY:
                    break;
                case DETAILED_SUMMARY:
                    switch (responseType) {
                        case TOKEN_REPORT:
                            ListDataProvider<Token> tokenDataProvider = (ListDataProvider<Token>) tokenDetailedSummaryGrid.getDataProvider();
                            List<Token> tokens = (ArrayList<Token>) tokenDataProvider.getItems();
                            for (Token token : tokens) {
                                Row row = sheet.createRow(++index);
                                row.createCell(0).setCellValue(token.getReportNumber());
                                row.createCell(1).setCellValue(token.getFromToken());
                                row.createCell(2).setCellValue(token.getToToken());
                                row.createCell(3).setCellValue(token.getSentence());
                            }
                            break;
                        case TOKEN_PAIR_REPORT:
                            ListDataProvider<TokenPair> tokenPairDataProvider = (ListDataProvider<TokenPair>) tokenPairDetailedSummaryGrid.getDataProvider();
                            List<TokenPair> tokenPairs = (ArrayList<TokenPair>) tokenPairDataProvider.getItems();
                            for (TokenPair tokenPair : tokenPairs) {
                                Row row = sheet.createRow(++index);
                                row.createCell(0).setCellValue(tokenPair.getReportNumber());
                                row.createCell(1).setCellValue(tokenPair.getFromTokensAsString());
                                row.createCell(2).setCellValue(tokenPair.getToToken());
                                row.createCell(3).setCellValue(tokenPair.getSentence());
                            }
                            break;
                        case WORD_EMBEDDING_REPORT:
                            ListDataProvider<WordEmbedding> wordEmbeddingDataProvider = (ListDataProvider<WordEmbedding>) wordEmbeddingDetailedSummaryGrid.getDataProvider();
                            List<WordEmbedding> wordEmbeddings = (ArrayList<WordEmbedding>) wordEmbeddingDataProvider.getItems();
                            for (WordEmbedding wordEmbedding : wordEmbeddings) {
                                Row row = sheet.createRow(++index);
                                row.createCell(0).setCellValue(wordEmbedding.getReportNumber());
                                row.createCell(1).setCellValue(wordEmbedding.getFromToken());
                                row.createCell(2).setCellValue(wordEmbedding.getToToken());
                                row.createCell(3).setCellValue(wordEmbedding.getWordEmbeddingType().toString());
                                row.createCell(4).setCellValue(wordEmbedding.getSentence());
                            }
                            break;
                        case EDGE_RANKING_REPORT:
                            ListDataProvider<FromTokenRelationship> edgeRankingDataProvider = (ListDataProvider<FromTokenRelationship>) edgeRankingDetailedSummaryGrid.getDataProvider();
                            List<FromTokenRelationship> fromTokenRelationships = (ArrayList<FromTokenRelationship>) edgeRankingDataProvider.getItems();
                            for (FromTokenRelationship fromTokenRelationship : fromTokenRelationships) {
                                Row row = sheet.createRow(++index);
                                row.createCell(0).setCellValue(fromTokenRelationship.getToTokenWord());
                                row.createCell(1).setCellValue(fromTokenRelationship.getFromTokenWord());
                                row.createCell(2).setCellValue(fromTokenRelationship.getEdgeRanking());
                                row.createCell(3).setCellValue(fromTokenRelationship.getConsecutiveFrequency());
                                row.createCell(4).setCellValue(fromTokenRelationship.getNonConsecutiveFrequency());
                            }
                            break;
                        case EDGE_COMPARATOR_REPORT:
                            ListDataProvider<SentenceComparision> edgeComparatorDataProvider = (ListDataProvider<SentenceComparision>) edgeComparatorDetailedSummaryGrid.getDataProvider();
                            List<SentenceComparision> sentenceComparisons = (ArrayList<SentenceComparision>) edgeComparatorDataProvider.getItems();
                            for (SentenceComparision sentenceComparision : sentenceComparisons) {
                                Row row = sheet.createRow(++index);
                                row.createCell(0).setCellValue(sentenceComparision.getCollectionName().toString());
                                row.createCell(1).setCellValue(sentenceComparision.getSentence());
                                row.createCell(2).setCellValue(sentenceComparision.getNumberTokenRelationships());
                                row.createCell(3).setCellValue(sentenceComparision.getTokenRelationships());
                            }
                    }
            }
            for (int i = 0; i < columns.size(); i++)
                sheet.autoSizeColumn(i);
            try {
                File file = new File("temp.xlsx");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                workbook.write(fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                workbook.close();
                return new FileInputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, displayType.toString() + ".xlsx");
    }
}
