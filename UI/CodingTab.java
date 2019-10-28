package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.*;
import com.mst.machineLearningUi.model.HighlightSelection;
import com.mst.machineLearningUi.service.*;
import com.mst.machineLearningUi.utility.DateUtility;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.LocalDateRenderer;
import com.vaadin.ui.themes.ValoTheme;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.mst.machineLearningModel.ApplicationName.MACHINE_LEARNING_UI;
import static com.mst.machineLearningModel.ExecutionStatus.SUCCESS;
import static com.mst.machineLearningModel.IcdCodeStatus.*;
import static com.mst.machineLearningModel.Component.CODING;
import static com.mst.machineLearningUi.model.Color.getHightlightColors;
import static com.mst.machineLearningUi.view.CodingTab.UserAction.*;
import static com.mst.machineLearningUi.view.CodingTab.CodeSelection.*;
import static com.mst.machineLearningUi.view.CodingTab.SortBy.*;
import static com.vaadin.ui.Grid.SelectionMode.SINGLE;
import static com.vaadin.ui.themes.ValoTheme.OPTIONGROUP_HORIZONTAL;

class CodingTab extends VerticalLayout {
    public enum UserAction {
        QUERY {public String toString() { return "Query"; }},
        EDIT {public String toString() { return "Edit"; }}
    }
    public enum CodeSelection {
        SELECTED_CODES {public String toString() { return "Selected Codes"; }},
        ALL_CODES {public String toString() { return "All Codes"; }},
        POSSIBLE_CODES {public String toString() { return "Possible Codes"; }}
    }
    public enum SortBy {
        CODES {public String toString() { return "Codes"; }},
        DESCRIPTIONS {public String toString() { return "Descriptions"; }}
    }
    private final int CODE_WIDTH = 7;
    private final int DESCRIPTION_WIDTH = 35;
    private final UserService userService;
    private final ReportService reportService;
    private final CptService cptService;
    private final IcdService icdService;
    private final OrganizationService organizationService;
    private final AuditService auditService;
    private HorizontalLayout searchInputs;
    private Button searchButton;
    private DateField fromDate;
    private DateField toDate;
    private RadioButtonGroup<CodeSelection> cptCodeSelectionRadioButton;
    private RadioButtonGroup<SortBy> sortCptByRadioButton;
    private ListSelect<CptCode> cptCodesListSelect;
    private Button submitButton;
    private ComboBox<IcdCodeStatus> statusComboBox;
    private Grid<Report> detailedSummaryGrid;
    private ReportView reportView;
    private HorizontalLayout cptCodesListLayout;
    private HorizontalLayout cptCodeSelectionLayout;
    private List<IcdCode> icdCodesSortedByCode;
    private List<IcdCode> icdCodesSortedByDescription;
    private List<CptCode> cptCodesSortedByCode;
    private List<CptCode> cptCodesSortedByDescription;
    private Report currentlySelectedReport;
    private ComboBox<String> assigneeCombobox;
    private ComboBox<UserLocation> userLocationComboBox;
    private Panel summaryPanel;
    private Panel reportPanel;
    private Button summaryButton;
    private Button reportButton;
    private EditWindow editWindow;
    private Button editButton;

    CodingTab(TabSheet tabSheet, UserService userService, ReportService reportService, CptService cptService, IcdService icdService, UserLocationService userLocationService, OrganizationService organizationService, AuditService auditService) {
        this.userService = userService;
        this.reportService = reportService;
        this.cptService = cptService;
        this.icdService = icdService;
        this.organizationService = organizationService;
        this.auditService = auditService;
        final int NUM_ROW_DISPLAY = 10;
        DummyDataService dummyDataService = new DummyDataService(reportService);
        icdCodesSortedByCode = icdService.getIcdCodes();
        icdCodesSortedByCode.sort(Comparator.comparing(IcdCode::getCode));
        cptCodesSortedByCode = cptService.getCptCodes();
        try {
            cptCodesSortedByCode.sort(Comparator.comparing(CptCode::getCode));
        } catch (Exception e) {
            Notification.show("cptCode collection contains a null value", Notification.Type.ERROR_MESSAGE);
        }
        fromDate = new DateField();
        fromDate.setDescription("Report Date: From");
        fromDate.setValue(DateUtility.getPreviousMonthStartDate());
        fromDate.addValueChangeListener(event -> handleSelectionChange());
        toDate = new DateField();
        toDate.setDescription("Report Date: To");
        toDate.setValue(DateUtility.getPreviousMonthEndDate());
        toDate.addValueChangeListener(event -> handleSelectionChange());
        HorizontalLayout dateLayout = new HorizontalLayout(fromDate, toDate);
        dateLayout.addStyleName("border");
        RadioButtonGroup<CodeSelection> icdCodeSelectionRadioButton = new RadioButtonGroup<>();
        icdCodeSelectionRadioButton.setItems(ALL_CODES, SELECTED_CODES);
        icdCodeSelectionRadioButton.setSelectedItem(ALL_CODES);
        icdCodeSelectionRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        icdCodeSelectionRadioButton.addValueChangeListener(event -> handleFilteringSelectionValueChange());
        RadioButtonGroup<SortBy> sortIcdByRadioButton = new RadioButtonGroup<>();
        sortIcdByRadioButton.setItems(CODES, DESCRIPTIONS);
        sortIcdByRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        sortIcdByRadioButton.setSelectedItem(CODES);
        sortIcdByRadioButton.addValueChangeListener(event -> handleSortByValueChange());
        sortCptByRadioButton = new RadioButtonGroup<>();
        sortCptByRadioButton.setItems(CODES, DESCRIPTIONS);
        sortCptByRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        sortCptByRadioButton.setSelectedItem(CODES);
        sortCptByRadioButton.addValueChangeListener(event -> handleSortByValueChange());
        cptCodeSelectionRadioButton = new RadioButtonGroup<>();
        cptCodeSelectionRadioButton.setDescription("CPT Codes");
        cptCodeSelectionRadioButton.setItems(ALL_CODES, SELECTED_CODES);
        cptCodeSelectionRadioButton.setSelectedItem(ALL_CODES);
        cptCodeSelectionRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
        cptCodeSelectionRadioButton.addStyleName("border");
        cptCodeSelectionRadioButton.addValueChangeListener(event -> handleFilteringSelectionValueChange());
        cptCodesListSelect = new ListSelect<>();
        cptCodesListSelect.setItems(cptCodesSortedByCode);
        cptCodesListSelect.setRows(NUM_ROW_DISPLAY);
        cptCodesListSelect.setItemCaptionGenerator(CptCode::getCode);
        List<UserLocation> userLocations = userLocationService.getUserLocations(organizationService.getOrganizationName());
        userLocationComboBox = new ComboBox<>();
        userLocationComboBox.setDescription("Location");
        userLocationComboBox.setItems(userLocations);
        if (!userLocations.isEmpty())
            userLocationComboBox.setSelectedItem(userLocations.get(0));
        userLocationComboBox.addValueChangeListener(event -> handleLocationValueChange());
        userLocationComboBox.setEmptySelectionAllowed(false);
        userLocationComboBox.setTextInputAllowed(false);
        userLocationComboBox.setItemCaptionGenerator(UserLocation::getLocationAsString);
        assigneeCombobox = new ComboBox<>();
        assigneeCombobox.setDescription("Assignee");
        assigneeCombobox.setTextInputAllowed(false);
        assigneeCombobox.addValueChangeListener(event -> handleAssigneeValueChange());
        statusComboBox = new ComboBox<>();
        statusComboBox.setDescription("Coding Status");
        statusComboBox.setWidth(14, Unit.EM);
        statusComboBox.setItems(IcdCodeStatus.getAll());
        statusComboBox.setSelectedItem(DIRECT_BILL_REPORTS);
        statusComboBox.setEmptySelectionAllowed(false);
        statusComboBox.setTextInputAllowed(false);
        statusComboBox.addValueChangeListener(event -> handleStatusChange());
        submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButton());
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        detailedSummaryGrid = new Grid<>();
        detailedSummaryGrid.setSelectionMode(SINGLE);
        detailedSummaryGrid.addSelectionListener(event -> handleGridSelection());
        detailedSummaryGrid.setVisible(false);
        detailedSummaryGrid.setSizeFull();
        reportView = new ReportView(CODING);
        reportView.setVisible(false);
        cptCodesListLayout = new HorizontalLayout(cptCodesListSelect, sortCptByRadioButton);
        cptCodesListLayout.setVisible(false);
        cptCodeSelectionLayout = new HorizontalLayout(cptCodeSelectionRadioButton, cptCodesListLayout);
        searchInputs = new HorizontalLayout(dateLayout, statusComboBox, userLocationComboBox, assigneeCombobox, cptCodeSelectionLayout, submitButton);
        searchButton = new Button("Search", (Button.ClickListener) event -> handleSearchButton());
        searchButton.addStyleNames(ValoTheme.BUTTON_TINY, ValoTheme.BUTTON_BORDERLESS_COLORED);
        searchButton.setVisible(false);
        Panel searchPanel = new Panel(new VerticalLayout(searchButton, searchInputs));
        searchPanel.getContent().setSizeUndefined(); //this call will make scrollable
        addComponent(searchPanel);
        summaryButton = new Button("Summary", (Button.ClickListener) event -> handleSummaryButton());
        summaryButton.addStyleNames(ValoTheme.BUTTON_TINY, ValoTheme.BUTTON_BORDERLESS_COLORED);
        summaryButton.setVisible(false);
        summaryPanel = new Panel(new VerticalLayout(summaryButton, detailedSummaryGrid));
        summaryPanel.setVisible(false);
        addComponent(summaryPanel);
        reportButton = new Button("Report", (Button.ClickListener) event -> handleReportButton());
        reportButton.addStyleNames(ValoTheme.BUTTON_TINY, ValoTheme.BUTTON_BORDERLESS_COLORED);
        reportButton.setVisible(false);
        reportPanel = new Panel(new VerticalLayout(reportButton, reportView));
        reportPanel.setVisible(false);
        addComponent(reportPanel);
        editButton = new Button("Edit", (Button.ClickListener) event -> handleEditButton());
        editButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        editButton.setVisible(false);
        addComponent(editButton);
        tabSheet.addTab(this, "Coding");
        if (((MachineLearningUi) UI.getCurrent()).getServerAddress().equals("10.0.129.218:8081"))
            dummyDataService.loadDummyCodingReports();
        handleLocationValueChange();
    }

    void showReportView(IcdCodeStatus status) {
        statusComboBox.setValue(status);
        //TODO handle call from Home tab
        handleSubmitButton();
    }

    private void handleSearchButton() {
        searchInputs.setVisible(!searchInputs.isVisible());
    }

    private void handleSummaryButton() {
        detailedSummaryGrid.setVisible(!detailedSummaryGrid.isVisible());
        if (!detailedSummaryGrid.isVisible() && !reportView.isVisible())
            editButton.setVisible(false);
    }

    private void handleReportButton() {
        reportView.setVisible(!reportView.isVisible());
        if (!detailedSummaryGrid.isVisible() && !reportView.isVisible())
            editButton.setVisible(false);
    }

    private void handleEditButton() {
        Optional<Report> reference = detailedSummaryGrid.getSelectionModel().getFirstSelectedItem();
        if (reference.isPresent()) {
            currentlySelectedReport = reference.get();
            if (editWindow == null) {
                editWindow = new EditWindow();
                UI.getCurrent().addWindow(editWindow);
            }
            editWindow.init();
        }
    }

    private void handleSubmitButton() {
        searchInputs.setVisible(false);
        searchButton.setVisible(true);
        summaryPanel.setVisible(true);
        summaryButton.setVisible(true);
        List<CptCode> selectedCptCodes = cptCodeSelectionRadioButton.getValue().equals(CodeSelection.SELECTED_CODES) ? new ArrayList<>(cptCodesListSelect.getSelectedItems()) : null;
        String selectedCptCodesAsString = selectedCptCodes != null ? CptCode.getCodesAsString(selectedCptCodes) : "ALL";
        detailedSummaryGrid.removeAllColumns();
        reportService.setCount(fromDate.getValue(), toDate.getValue(), statusComboBox.getValue(), userLocationComboBox.getValue().getLocation(), assigneeCombobox.getValue(), selectedCptCodes);
        recordActivity(QUERY + " from[" + fromDate.getValue() + "], to[" + toDate.getValue() + "], status[" + statusComboBox.getValue() + "], userLocation[" + userLocationComboBox.getValue().serialize() + "], assignee[" + assigneeCombobox.getValue() + "], selectedCptCodes[" + selectedCptCodesAsString + "]");
        detailedSummaryGrid.setDataProvider((sortOrders, offset, limit) -> {
                Map<String, Boolean> sortOrder = sortOrders.stream().collect(Collectors.toMap(QuerySortOrder::getSorted, sort -> sort.getDirection() == SortDirection.ASCENDING));
                return reportService.getReportsLazyLoading(offset, limit, sortOrder, fromDate.getValue(), toDate.getValue(), statusComboBox.getValue(), userLocationComboBox.getValue().getLocation(), assigneeCombobox.getValue(), selectedCptCodes).stream();
            }, reportService::getCount);
        detailedSummaryGrid.addColumn(Report::getAccessionNumber).setCaption("Accession Number");
        detailedSummaryGrid.addColumn(Report::getReportDate, new LocalDateRenderer(Formatting.dateFormat)).setCaption("Report Date");
        detailedSummaryGrid.addColumn(Report::getLocation).setCaption("Location");
        detailedSummaryGrid.addColumn(Report::getAssignee).setCaption("Assignee");
        detailedSummaryGrid.addColumn(Report::getIcdCodesAsString).setCaption("ICD Codes");
        if (statusComboBox.getValue() == PENDING_EDITS)
            detailedSummaryGrid.addColumn(Report::getPendingIcdCodesAsString).setCaption("Pending ICD Code Changes");
        detailedSummaryGrid.addColumn(Report::getCptCodesAsString).setCaption("CPT Codes");
        if (statusComboBox.getValue() == PENDING_EDITS)
            detailedSummaryGrid.addColumn(Report::getPendingCptCodesAsString).setCaption("Pending CPT Code Changes");
        detailedSummaryGrid.addColumn(Report::getErrorCode).setCaption("Error Code");
        submitButton.setEnabled(false);
        detailedSummaryGrid.setVisible(true);
    }

    private void handleGridSelection() {
        Optional<Report> reference = detailedSummaryGrid.getSelectionModel().getFirstSelectedItem();
        if (reference.isPresent()) {
            currentlySelectedReport = reference.get();
            List<Report> reports = new ArrayList<>(Collections.singletonList(currentlySelectedReport));
            reportView.setReports(reports);
            reportView.setHighlightSelectionsByReport(createHighlightSelectionsByReport(reports));
            reportView.displayReportAtIndex(0);
            reportPanel.setVisible(true);
            reportView.setVisible(true);
            reportButton.setVisible(true);
            editButton.setVisible(true);
        }
    }

    private Map<String, List<HighlightSelection>> createHighlightSelectionsByReport(List<Report> reports) {
        Map<String, List<HighlightSelection>> highlightSelectionsByReport = new HashMap<>();
        List<String> colors = getHightlightColors();
        for (Report report : reports) {
            List<HighlightSelection> highlightSelections = new ArrayList<>();
            int index = 0;
            List<IcdMatchPhrase> icdMatchPhrases = report.getIcdMatchPhrases();
            if (icdMatchPhrases != null) {
                for (IcdMatchPhrase icdMatchPhrase : icdMatchPhrases) {
                    highlightSelections.add(new HighlightSelection(icdMatchPhrase.getCode(), icdMatchPhrase.getMatchPhrase(), colors.get(index)));
                    ++index;
                }
                highlightSelectionsByReport.put(report.getAccessionNumber(), highlightSelections);
            }
        }
        return highlightSelectionsByReport;
    }

    private void handleStatusChange() {
        submitButton.setEnabled(true);
    }

    private void handleSelectionChange() {
        submitButton.setEnabled(true);
    }

    private void handleFilteringSelectionValueChange() {
        submitButton.setEnabled(true);
        cptCodesListLayout.setVisible(cptCodeSelectionRadioButton.getValue().equals(SELECTED_CODES));
        if (cptCodeSelectionRadioButton.getValue().equals(SELECTED_CODES)) {
            cptCodeSelectionRadioButton.removeStyleName("border");
            cptCodeSelectionLayout.addStyleName("border");
        } else {
            cptCodeSelectionRadioButton.addStyleName("border");
            cptCodeSelectionLayout.removeStyleName("border");
        }
    }

    private void handleSortByValueChange() {
        if (sortCptByRadioButton.getValue() == CODES) {
            cptCodesListSelect.setWidth(CODE_WIDTH, Unit.EM);
            cptCodesListSelect.setItems(cptCodesSortedByCode);
            cptCodesListSelect.setItemCaptionGenerator(CptCode::getCode);
        } else {
            if (cptCodesSortedByDescription == null) {
                cptCodesSortedByDescription = cptService.getCptCodes();
                cptCodesSortedByDescription.sort(Comparator.comparing(CptCode::getDescription));
            }
            cptCodesListSelect.setWidth(DESCRIPTION_WIDTH, Unit.EM);
            cptCodesListSelect.setItems(cptCodesSortedByDescription);
            cptCodesListSelect.setItemCaptionGenerator(CptCode::getDescription);
        }
    }

    private void handleAssigneeValueChange() {
        submitButton.setEnabled(true);
    }

    private void handleLocationValueChange() {
        if (userLocationComboBox.getValue() != null)
            assigneeCombobox.setItems(userLocationComboBox.getValue().getUsers());
        submitButton.setEnabled(true);
    }

    private void processReportEdits(String assignee, String notes, List<IcdCode> icdCodes, List<CptCode> cptCodes) {
        if (assignee != null)
            currentlySelectedReport.setAssignee(assignee);
        if (notes != null) {
            currentlySelectedReport.setReportNotes(notes);
            reportView.appendNotesToReportText(notes);
        }
        if (icdCodes != null) {
            currentlySelectedReport.setPendingIcdCodes(icdCodes);
            currentlySelectedReport.setIcdCodeStatus(PENDING_EDITS);
        }
        if (cptCodes != null) {
            currentlySelectedReport.setPendingCptCodes(cptCodes);
            currentlySelectedReport.setIcdCodeStatus(PENDING_EDITS);
        }
        reportService.updateReport(currentlySelectedReport);
        handleSubmitButton();
    }

    private void recordActivity(String input) {
        Audit activity = new Audit();
        activity.setApplicationName(MACHINE_LEARNING_UI);
        activity.setUserName(userService.getUser().getUsername());
        activity.setOrganizationName(userService.getUser().getOrganizationName());
        activity.setComponentName(CODING.toString());
        activity.setTimestamp(LocalDateTime.now());
        activity.setRequest(input);
        activity.setRequestNumber("N/A");
        activity.setExecutionStatus(SUCCESS);
        auditService.saveActivity(activity);
    }

    class EditWindow extends Window {
        private final int MIN_WIDTH = 350;
        private final int MIN_HEIGHT = 250;
        private CheckBox assignCheckbox;
        private ComboBox<User> assignToCombobox;
        private HorizontalLayout assignToLayout;
        private final CheckBox notesCheckbox;
        private Label notesCheckboxLabel;
        private TextArea notesArea;
        private RadioButtonGroup<CodeSelection> editIcdCodeSelectionRadioButton;
        private final RadioButtonGroup<SortBy> sortEditIcdByRadioButton;
        private ComboboxColSelect<IcdCode> editIcdCodesSelect;
        private final CheckBox editIcdCodesCheckbox;
        private HorizontalLayout editIcdCodeLayout;
        private HorizontalLayout displayLayout;
        private final RadioButtonGroup<SortBy> sortEditCptByRadioButton;
        private ComboboxColSelect<CptCode> editCptCodesSelect;
        private final CheckBox editCptCodesCheckbox;
        private HorizontalLayout editCptCodeLayout;

        EditWindow() {
            super("Edit Report");
            setHeight(MIN_HEIGHT, Unit.PIXELS);
            setWidth(MIN_WIDTH, Unit.PIXELS);
            int captionWidth = 8;
            setPosition(0,0);
            setClosable(false);
            Label assignLabel = new Label("Assign");
            assignLabel.setWidth(captionWidth, Unit.EM);
            assignLabel.setStyleName("caption-right");
            assignCheckbox = new CheckBox();
            assignCheckbox.addValueChangeListener(event -> handleAssignCheckboxValueChange());
            Label assignToLabel = new Label("Assign to");
            assignToLabel.setStyleName("caption-right");
            assignToCombobox = new ComboBox<>();
            assignToCombobox.setItems(userService.getUsers(organizationService.getOrganizationName()));
            assignToCombobox.setEmptySelectionAllowed(false);
            assignToCombobox.setTextInputAllowed(false);
            assignToLayout = new HorizontalLayout(assignToLabel, assignToCombobox);
            assignToLayout.setVisible(false);
            HorizontalLayout assignLayout = new HorizontalLayout(assignLabel, assignCheckbox, assignToLayout);
            notesCheckboxLabel = new Label();
            notesCheckboxLabel.setStyleName("caption-right");
            notesCheckboxLabel.setWidth(captionWidth, Unit.EM);
            notesCheckbox = new CheckBox();
            notesCheckbox.addValueChangeListener(event -> handleNotesCheckboxValueChange());
            notesArea = new TextArea();
            notesArea.setWidth(30, Unit.EM);
            notesArea.setVisible(false);
            HorizontalLayout notesLayout = new HorizontalLayout(notesCheckboxLabel, notesCheckbox, notesArea);
            Label editIcdLabel = new Label("Edit ICD Codes");
            editIcdLabel.setWidth(captionWidth, Unit.EM);
            editIcdLabel.setStyleName("caption-right");
            editIcdCodesCheckbox = new CheckBox();
            editIcdCodesCheckbox.addValueChangeListener(event -> handleEditIcdCheckboxValueChange());
            editIcdCodesSelect = new ComboboxColSelect<>();
            editIcdCodesSelect.setItemCaptionGenerator(IcdCode::getCode);
            Label displayLabel = new Label("Display");
            displayLabel.setStyleName("caption");
            editIcdCodeSelectionRadioButton = new RadioButtonGroup<>();
            editIcdCodeSelectionRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
            editIcdCodeSelectionRadioButton.setItems(POSSIBLE_CODES, ALL_CODES);
            editIcdCodeSelectionRadioButton.addValueChangeListener(event -> handleEditIcdCodeSelectionChange());
            editIcdCodeSelectionRadioButton.setSelectedItem(ALL_CODES);
            displayLayout = new HorizontalLayout(displayLabel, editIcdCodeSelectionRadioButton);
            displayLayout.setVisible(false);
            Label sortIcdByLabel = new Label("Sort By");
            sortIcdByLabel.setStyleName("caption");
            sortEditIcdByRadioButton = new RadioButtonGroup<>();
            sortEditIcdByRadioButton.setItems(CODES, DESCRIPTIONS);
            sortEditIcdByRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
            sortEditIcdByRadioButton.setSelectedItem(CODES);
            sortEditIcdByRadioButton.addValueChangeListener(event -> handleSortEditByValueChange());
            editIcdCodeLayout = new HorizontalLayout(displayLayout, editIcdCodesSelect, sortIcdByLabel, sortEditIcdByRadioButton);
            editIcdCodeLayout.setVisible(false);
            HorizontalLayout icdLayout = new HorizontalLayout(editIcdLabel, editIcdCodesCheckbox, editIcdCodeLayout);
            Label editCptLabel = new Label("Edit CPT Codes");
            editCptLabel.setWidth(captionWidth, Unit.EM);
            editCptLabel.setStyleName("caption-right");
            editCptCodesCheckbox = new CheckBox();
            editCptCodesCheckbox.addValueChangeListener(event -> handleEditCptCheckboxValueChange());
            editCptCodesSelect = new ComboboxColSelect<>();
            editCptCodesSelect.setItemCaptionGenerator(CptCode::getCode);
            Label sortCptByLabel = new Label("Sort By");
            sortCptByLabel.setStyleName("caption");
            sortEditCptByRadioButton = new RadioButtonGroup<>();
            sortEditCptByRadioButton.setItems(CODES, DESCRIPTIONS);
            sortEditCptByRadioButton.addStyleName(OPTIONGROUP_HORIZONTAL);
            sortEditCptByRadioButton.setSelectedItem(CODES);
            sortEditCptByRadioButton.addValueChangeListener(event -> handleSortEditByValueChange());
            editCptCodeLayout = new HorizontalLayout(editCptCodesSelect, sortCptByLabel, sortEditCptByRadioButton);
            editCptCodeLayout.setVisible(false);
            HorizontalLayout cptLayout = new HorizontalLayout(editCptLabel, editCptCodesCheckbox, editCptCodeLayout);
            Button saveButton = new Button("Save", event -> handleSaveButtonClick());
            saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            Button closeButton = new Button("Close", event -> handleCloseButtonClick());
            closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, closeButton);
            VerticalLayout layout = new VerticalLayout(assignLayout, notesLayout, icdLayout, cptLayout, buttonLayout);
            layout.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);
            setContent(layout);
            init();
        }

        void init() {
            setCaption("Edit Selected Report");
            assignToCombobox.clear();
            String notes = currentlySelectedReport.getReportNotes();
            notesCheckboxLabel.setValue(notes != null && !notes.isEmpty() ? "Edit Notes" : "Add Notes");
            if (notes != null)
                notesArea.setValue(notes);
            else
                notesArea.clear();
            editIcdCodesSelect.clear();
            editCptCodesSelect.clear();
        }

        void processNewReportSelection() {
            String notes = currentlySelectedReport.getReportNotes();
            if (notes == null) {
                notesCheckboxLabel.setValue("Add Notes");
                notesArea.clear();
            } else {
                notesCheckboxLabel.setValue("Edit Notes");
                notesArea.setValue(notes);
            }
        }

        private void handleAssignCheckboxValueChange() {
            assignToLayout.setVisible(assignCheckbox.getValue());
            resize();
        }

        private void handleNotesCheckboxValueChange() {
            notesArea.setVisible(notesCheckbox.getValue());
            if (notesCheckbox.getValue() && currentlySelectedReport.getReportNotes() != null)
                notesArea.setValue(currentlySelectedReport.getReportNotes());
            resize();
        }

        private void handleEditIcdCheckboxValueChange() {
            boolean hasPossibleIcdCodes = currentlySelectedReport.getPossibleIcdCodes() != null && !currentlySelectedReport.getPossibleIcdCodes().isEmpty();
            displayLayout.setVisible(editIcdCodesCheckbox.getValue() && hasPossibleIcdCodes);
            editIcdCodeLayout.setVisible(editIcdCodesCheckbox.getValue());
            resize();
        }

        private void handleEditIcdCodeSelectionChange() {
            List<IcdCode> icdCodes = null;
            switch (editIcdCodeSelectionRadioButton.getValue()) {
                case ALL_CODES:
                    icdCodes = icdService.getIcdCodes();
                    break;
                case POSSIBLE_CODES:
                    icdCodes = reportView.getCurrentlySelectedReport().getPossibleIcdCodes();
            }
            if (icdCodes == null)
                return;
            editIcdCodesSelect.setItems(icdCodes);
        }

        private void handleEditCptCheckboxValueChange() {
            editCptCodeLayout.setVisible(editCptCodesCheckbox.getValue());
            if (editCptCodesCheckbox.getValue())
                editCptCodesSelect.setItems(cptCodesSortedByCode);
            resize();
        }

        private void handleSortEditByValueChange() {
            if (sortEditIcdByRadioButton.getValue() == CODES) {
                editIcdCodesSelect.setWidth(CODE_WIDTH, Unit.EM);
                switch (editIcdCodeSelectionRadioButton.getValue()) {
                    case ALL_CODES:
                        editIcdCodesSelect.setItems(icdCodesSortedByCode);
                        break;
                    case POSSIBLE_CODES:
                        List<IcdCode> icdCodes = reportView.getCurrentlySelectedReport().getPossibleIcdCodes();
                        icdCodes.sort(Comparator.comparing(IcdCode::getCode));
                        editIcdCodesSelect.setItems(icdCodes);
                }
                editIcdCodesSelect.setItemCaptionGenerator(IcdCode::getCode);
            } else {
                editIcdCodesSelect.setWidth(DESCRIPTION_WIDTH, Unit.EM);
                switch (editIcdCodeSelectionRadioButton.getValue()) {
                    case ALL_CODES:
                        if (icdCodesSortedByDescription == null) {
                            icdCodesSortedByDescription = icdService.getIcdCodes();
                            icdCodesSortedByDescription.sort(Comparator.comparing(IcdCode::getDescription));
                        }
                        editIcdCodesSelect.setItems(icdCodesSortedByDescription);
                        break;
                    case POSSIBLE_CODES:
                        List<IcdCode> icdCodes = reportView.getCurrentlySelectedReport().getPossibleIcdCodes();
                        icdCodes.sort(Comparator.comparing(IcdCode::getDescription));
                        editIcdCodesSelect.setItems(icdCodes);
                }
                editIcdCodesSelect.setItemCaptionGenerator(IcdCode::getDescription);
            }
            if (sortEditCptByRadioButton.getValue() == CODES) {
                editCptCodesSelect.setWidth(CODE_WIDTH, Unit.EM);
                editCptCodesSelect.setItems(cptCodesSortedByCode);
                editCptCodesSelect.setItemCaptionGenerator(CptCode::getCode);
            } else {
                if (cptCodesSortedByDescription == null) {
                    cptCodesSortedByDescription = cptService.getCptCodes();
                    cptCodesSortedByDescription.sort(Comparator.comparing(CptCode::getDescription));
                }
                editCptCodesSelect.setWidth(DESCRIPTION_WIDTH, Unit.EM);
                editCptCodesSelect.setItems(cptCodesSortedByDescription);
                editCptCodesSelect.setItemCaptionGenerator(CptCode::getDescription);
            }
        }

        private void handleSaveButtonClick() {
            String assignee = assignCheckbox.getValue() ? assignToCombobox.getValue().toString() : null;
            String notes = notesCheckbox.getValue() ? notesArea.getValue() : null;
            List<IcdCode> icdCodes = editIcdCodesCheckbox.getValue() ? editIcdCodesSelect.getValue() : null;
            List<CptCode> cptCodes = editCptCodesCheckbox.getValue() ? editCptCodesSelect.getValue() : null;
            recordActivity(EDIT + " assignee[" + assignee + "], notes[" + notes + "], icdCodes[" + IcdCode.getCodesAsString(icdCodes) + "], cptCodes[" + CptCode.getCodesAsString(cptCodes) + "]");
            processReportEdits(assignee, notes, icdCodes, cptCodes);
            editIcdCodesSelect.clear();
            editCptCodesSelect.clear();
        }

        private void handleCloseButtonClick() {
            close();
            editWindow = null;
        }

        private void resize() {
            final int ASSIGN_WIDTH = 150;
            final int ASSIGN_HEIGHT = 20;
            final int NOTES_WIDTH = 350;
            final int NOTES_HEIGHT = 105;
            final int EDIT_CODES_WIDTH = 850;
            final int EDIT_CODES_HEIGHT = 215;
            final int DISPLAY_WIDTH = 325;
            int addedWidth = 0;
            int addedHeight = 0;
            if (editIcdCodeLayout.isVisible() || editCptCodeLayout.isVisible())
                addedWidth = EDIT_CODES_WIDTH + (displayLayout.isVisible() ? DISPLAY_WIDTH : 0);
            else if (notesArea.isVisible())
                addedWidth = NOTES_WIDTH;
            else if (assignToLayout.isVisible())
                addedWidth = ASSIGN_WIDTH;
            setWidth(MIN_WIDTH + addedWidth, Unit.PIXELS);
            if (editIcdCodeLayout.isVisible())
                addedHeight += EDIT_CODES_HEIGHT;
            if (editCptCodeLayout.isVisible())
                addedHeight += EDIT_CODES_HEIGHT;
            if (notesArea.isVisible())
                addedHeight += NOTES_HEIGHT;
            if (assignToLayout.isVisible())
                addedHeight += ASSIGN_HEIGHT;
            setHeight(MIN_HEIGHT + addedHeight, Unit.PIXELS);
        }
    }
}
