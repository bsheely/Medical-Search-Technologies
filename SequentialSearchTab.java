package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.*;
import com.mst.machineLearningUi.model.SequentialSearchQuery;
import com.mst.machineLearningUi.service.SequentialSearchQueryService;
import com.mst.machineLearningUi.service.UserService;
import com.mst.machineLearningUi.utility.StringUtility;
import com.vaadin.data.provider.Query;
import com.vaadin.server.Page;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static com.mst.machineLearningModel.Component.SEQ_SEARCH;
import static com.mst.machineLearningModel.EdgeName.*;
import static com.mst.machineLearningUi.view.SequentialSearchTab.QueryType.*;
import static com.mst.machineLearningUi.view.SequentialSearchTab.SaveType.*;
import static com.mst.machineLearningUi.view.SequentialSearchTab.State.*;

class SequentialSearchTab extends VerticalLayout implements Button.ClickListener {
    public enum QueryType {
        CREATE {public String toString() { return "Create New Query"; }},
        LOAD {public String toString() { return "Load Saved Query"; }},
        RUN {public String toString() { return "Run Saved Query"; }}
    }
    public enum SaveType {
        NEW {public String toString() { return "Create New Query"; }},
        UPDATE {public String toString() { return "Update Existing Query"; }},
        RENAME {public String toString() { return "Rename Existing Query"; }}
    }
    public enum State {
        DELETE,
        SAVE
    }
    private final UserService userService;
    private final SequentialSearchQueryService sequentialSearchQueryService;
    private final OrganizationName organizationName;
    private Map<ObjectId, SentenceQueryInput> sentenceQueryInputs;
    private ComboBox<QueryType> queryType;
    private ComboBox<SequentialSearchQuery> savedQuery;
    private HorizontalLayout savedQueryLayout;
    private Button addQueryInputButton;
    private Button submitButton;
    private Button deleteButton;
    private VerticalLayout sentenceQueryInputLayouts;
    private SentenceQueryInputLayout sentenceQueryInputLayout;
    private ConfirmationWindow confirmationWindow;
    private SaveQueryWindow saveQueryWindow;
    private SequentialSearchQueryInput sequentialSearchQueryInput;
    private State state;

    SequentialSearchTab(TabSheet tabSheet, UserService userService, SequentialSearchQueryService sequentialSearchQueryService) {
        this.userService = userService;
        this.sequentialSearchQueryService = sequentialSearchQueryService;
        this.organizationName = userService.getUser().getOrganizationName();
        final int CAPTION_WIDTH = 6;
        sentenceQueryInputs = new HashMap<>();

        queryType = new ComboBox<>();
        queryType.setWidth(12, Unit.EM);
        queryType.setItems(CREATE, LOAD, RUN);
        queryType.setSelectedItem(CREATE);
        queryType.setTextInputAllowed(false);
        queryType.setEmptySelectionAllowed(false);
        queryType.addValueChangeListener(event -> handleQueryTypeValueChange());
        Label queryTypeLabel = new Label("Query Type");
        queryTypeLabel.setStyleName("caption-right");
        queryTypeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        HorizontalLayout queryTypeLayout = new HorizontalLayout(queryTypeLabel, queryType);
        queryTypeLayout.setComponentAlignment(queryTypeLabel, Alignment.MIDDLE_RIGHT);

        savedQuery = new ComboBox<>();
        savedQuery.setWidth(18, Unit.EM);
        savedQuery.setTextInputAllowed(false);
        savedQuery.addValueChangeListener(event -> handleSavedQueryValueChange());
        Label savedQueryLabel = new Label("Saved Query");
        savedQueryLabel.setStyleName("caption-right");
        savedQueryLabel.setWidth(CAPTION_WIDTH, Unit.EM);
        savedQueryLayout = new HorizontalLayout(savedQueryLabel, savedQuery);
        savedQueryLayout.setComponentAlignment(savedQueryLabel, Alignment.MIDDLE_RIGHT);
        savedQueryLayout.setVisible(false);

        addQueryInputButton = new Button("+ Input", event -> handleAddSentenceQueryInputLayout());
        addQueryInputButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        sentenceQueryInputLayouts = new VerticalLayout();
        sentenceQueryInputLayouts.setSizeFull();
        submitButton = new Button("Submit", (Button.ClickListener) event -> handleSubmitButtonClick());
        submitButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        submitButton.setEnabled(false);
        deleteButton = new Button("Delete", (Button.ClickListener) event -> handleDelete());
        deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
        deleteButton.setVisible(false);

        addComponents(queryTypeLayout, savedQueryLayout);
        addComponents(addQueryInputButton, sentenceQueryInputLayouts, new HorizontalLayout(submitButton, deleteButton));
        tabSheet.addTab(this, SEQ_SEARCH.toString());
        handleAddSentenceQueryInputLayout();
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getButton().getCaption().equalsIgnoreCase("yes")) {
            switch (state) {
                case DELETE:
                    sequentialSearchQueryService.deleteQuery(savedQuery.getValue());
                    clearSentenceQueryInputs();
                    break;
                case SAVE:
                    if (saveQueryWindow == null) {
                        saveQueryWindow = new SaveQueryWindow();
                        UI.getCurrent().addWindow(saveQueryWindow);
                    }

            }
        }
        savedQuery.setItems(sequentialSearchQueryService.getQueries(userService.getUser().getUsername()));
        savedQuery.setSelectedItem(null);
        state = null;
        confirmationWindow.close();
    }

    private void clearSentenceQueryInputs() {
        sentenceQueryInputLayouts.removeAllComponents();
        handleAddSentenceQueryInputLayout();
        sequentialSearchQueryInput = null;
    }

    private void handleDelete() {
        state = DELETE;
        confirmationWindow = new ConfirmationWindow("Do you want to delete this savedQuery?", this);
        UI.getCurrent().addWindow(confirmationWindow);
    }

    private void handleQueryTypeValueChange() {
        if (savedQuery.getDataProvider().fetch(new Query<>()).count() == 0) {
            savedQuery.setItems(sequentialSearchQueryService.getQueries(userService.getUser().getUsername()));
            savedQuery.setItemCaptionGenerator(SequentialSearchQuery::getQueryName);
        }
        switch (queryType.getValue()) {
            case CREATE:
                addQueryInputButton.setVisible(true);
                sentenceQueryInputLayouts.setVisible(true);
                savedQueryLayout.setVisible(false);
                deleteButton.setVisible(false);
                clearSentenceQueryInputs();
                break;
            case LOAD:
                addQueryInputButton.setVisible(true);
                sentenceQueryInputLayouts.setVisible(true);
                savedQueryLayout.setVisible(true);
                deleteButton.setVisible(savedQuery.getValue() != null);
                savedQuery.setItems(sequentialSearchQueryService.getQueries(userService.getUser().getUsername()));
                savedQuery.setSelectedItem(null);
                break;
            case RUN:
                addQueryInputButton.setVisible(false);
                sentenceQueryInputLayouts.setVisible(false);
                savedQueryLayout.setVisible(true);
                deleteButton.setVisible(false);
        }
    }

    private void handleSavedQueryValueChange() {
        if (queryType.getValue().equals(LOAD) && savedQuery.getValue() != null)
            loadSavedQuery(savedQuery.getValue().getQueryInput());
        deleteButton.setVisible(queryType.getValue().equals(LOAD) && savedQuery.getValue() != null);
        submitButton.setEnabled(true);
    }

    private void handleAddSentenceQueryInputLayout() {
        sentenceQueryInputLayout = new SentenceQueryInputLayout();
        sentenceQueryInputLayouts.addComponent(sentenceQueryInputLayout);
    }

    private void loadSavedQuery(SequentialSearchQueryInput sequentialSearchQueryInput) {
        List<SentenceQueryInput> sentenceQueryInputs = sequentialSearchQueryInput.getSentenceQueryInputs();
        int inputCount = 1;
        for (SentenceQueryInput sentenceQueryInput : sentenceQueryInputs) {
            if (inputCount > 1) {
                sentenceQueryInputLayout = new SentenceQueryInputLayout();
                sentenceQueryInputLayouts.addComponent(sentenceQueryInputLayout);
            }
            SentenceQueryInstanceLayout sentenceQueryInstanceLayout = sentenceQueryInputLayout.sentenceQueryInstanceLayout;
            List<SentenceQueryInstance> sentenceQueryInstances = sentenceQueryInput.getSentenceQueryInstances();
            int instanceCount = 1;
            for (SentenceQueryInstance sentenceQueryInstance : sentenceQueryInstances) {
                if (inputCount > 1 || instanceCount > 1) {
                    sentenceQueryInstanceLayout = new SentenceQueryInstanceLayout(sentenceQueryInputLayout.sentenceQueryInstances, sentenceQueryInputLayout);
                    sentenceQueryInputLayout.sentenceQueryInstanceLayouts.addComponent(sentenceQueryInstanceLayout);
                }
                sentenceQueryInstanceLayout.tokens.setValue(StringUtility.convertToCommaDelimitedString(sentenceQueryInstance.getTokens()));
                List<EdgeQuery> edges = sentenceQueryInstance.getEdges();
                if (edges != null) {
                    List<EdgeName> edgeNames = new ArrayList<>(EdgeName.getExistenceEdgeNames());
                    Set<EdgeName> existenceEdges = new HashSet<>();
                    for (EdgeName edgeName : edgeNames)
                        if (doesEdgeExist(edges, edgeName))
                            existenceEdges.add(edgeName);
                    sentenceQueryInstanceLayout.existence.setValue(existenceEdges);
                    sentenceQueryInstanceLayout.simpleCystModifiers.setValue(getEdgeValues(edges, SIMPLE_CYST_MODIFIERS));
                    sentenceQueryInstanceLayout.diseaseModifier.setValue(getEdgeValues(edges, DISEASE_MODIFIER));
                    sentenceQueryInstanceLayout.diseaseLocation.setValue(getEdgeValues(edges, DISEASE_LOCATION));
                    sentenceQueryInstanceLayout.laterality.setValue(getEdgeValues(edges, LATERALITY));
                    sentenceQueryInstanceLayout.findingSiteModifier.setValue(getEdgeValues(edges, FINDING_SITE_MODIFIER));
                    sentenceQueryInstanceLayout.heterogeneousFindingSite.setValue(getEdgeValues(edges, HETEROGENEOUS_FINDING_SITE));
                    sentenceQueryInstanceLayout.enlargedFindingSite.setValue(getEdgeValues(edges, ENLARGED_FINDING_SITE));
                    List<String> measurements = new ArrayList<>(getEdgeValues(edges, MEASUREMENT));
                    if (!measurements.isEmpty()) {
                        sentenceQueryInstanceLayout.minMeasurement.setValue(measurements.get(0));
                        sentenceQueryInstanceLayout.maxMeasurement.setValue(measurements.get(1));
                    }
                }
                sentenceQueryInstanceLayout.measurementClassification.setSelectedItem(sentenceQueryInstance.getMeasurementClassification());
                sentenceQueryInstanceLayout.appender.setSelectedItem(sentenceQueryInstance.getAppender());
                ++instanceCount;
            }
            if (sentenceQueryInput.getSentenceQueryFilter() != null) {
                SentenceQueryFilter filter = sentenceQueryInput.getSentenceQueryFilter();
                sentenceQueryInputLayout.fromDate.setValue(filter.getReportDateRange() != null && !filter.getReportDateRange().isEmpty() ? filter.getReportDateRange().get(0) : null);
                sentenceQueryInputLayout.toDate.setValue(filter.getReportDateRange() != null && !filter.getReportDateRange().isEmpty() ? filter.getReportDateRange().get(1) : null);
                sentenceQueryInputLayout.minAge.setValue(filter.getPatientAgeRange() != null && !filter.getPatientAgeRange().isEmpty() ? String.valueOf(filter.getPatientAgeRange().get(0)) : "");
                sentenceQueryInputLayout.maxAge.setValue(filter.getPatientAgeRange() != null && !filter.getPatientAgeRange().isEmpty() ? String.valueOf(filter.getPatientAgeRange().get(1)) : "");
                sentenceQueryInputLayout.patientSex.setSelectedItem(filter.getPatientSex());
                sentenceQueryInputLayout.modalities.setValue(filter.getModalities() != null ? new HashSet<>(filter.getModalities()) : new HashSet<>());
                sentenceQueryInputLayout.readingLocations.setValue(filter.getReadingLocations() != null ? new HashSet<>(filter.getReadingLocations()) : new HashSet<>());
                sentenceQueryInputLayout.statuses.setValue(filter.getStatuses()!= null ? new HashSet<>(filter.getStatuses()) : new HashSet<>());
            }
            sentenceQueryInputLayout.notAndAll.setValue(sentenceQueryInput.isNotAndAll());
            sentenceQueryInputLayout.scope.setSelectedItem(sentenceQueryInput.getSequentialSearchScope());
            sentenceQueryInputLayout.headers.setValue(sentenceQueryInput.getHeaders() != null ? new HashSet<>(sentenceQueryInput.getHeaders()) : new HashSet<>());
            ++inputCount;
        }
    }

    private boolean doesEdgeExist(List<EdgeQuery> edges, EdgeName edgeName) {
        for (EdgeQuery edge : edges)
            if (edge.getName().equals(edgeName))
                return true;
        return false;
    }

    private Set<String> getEdgeValues(List<EdgeQuery> edges, EdgeName edgeName) {
        for (EdgeQuery edge : edges)
            if (edge.getName().equals(edgeName))
                return edge.getValues();
        return new HashSet<>();
    }

    private void handleSubmitButtonClick() {
        String url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/seq-search-query";
        sequentialSearchQueryInput = null;
        switch (queryType.getValue()) {
            case CREATE:
            case LOAD:
                sequentialSearchQueryInput = new SequentialSearchQueryInput();
                sequentialSearchQueryInput.setUsername(userService.getUser().getUsername());
                sequentialSearchQueryInput.setOrganizationName(organizationName);
                sequentialSearchQueryInput.setExecuteAsynchronously(true);
                sequentialSearchQueryInput.setSentenceQueryInputs(new ArrayList<>(sentenceQueryInputs.values()));
                break;
            case RUN:
                sequentialSearchQueryInput = savedQuery.getValue().getQueryInput();
        }
        ResponseEntity<SentenceQueryOutput> response;
        try {
            response = new RestTemplate().postForEntity(url, sequentialSearchQueryInput, SentenceQueryOutput.class);
        } catch (HttpClientErrorException e) {
            Notification.show(e.toString(), Notification.Type.ERROR_MESSAGE);
            return;
        }
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            SentenceQueryOutput sentenceQueryOutput = response.getBody();
            Notification notification = new Notification("Request Number " + sentenceQueryOutput.getRequestNumber(), Notification.Type.TRAY_NOTIFICATION);
            notification.setDelayMsec(120000);
            notification.show(Page.getCurrent());
        } else {
            Notification.show(response.getStatusCode().toString(), Notification.Type.ERROR_MESSAGE);
        }
        submitButton.setEnabled(false);
        switch (queryType.getValue()) {
            case CREATE:
            case LOAD:
                state = SAVE;
                confirmationWindow = new ConfirmationWindow("Do you want to save this savedQuery?", this);
                UI.getCurrent().addWindow(confirmationWindow);
        }
    }

    private class SentenceQueryInputLayout extends VerticalLayout {
        final int CAPTION_WIDTH = 8;
        ObjectId objectId;
        Map<ObjectId, SentenceQueryInstance> sentenceQueryInstances;
        VerticalLayout sentenceQueryInstanceLayouts;
        SentenceQueryInstanceLayout sentenceQueryInstanceLayout;

        //Filters
        DateField fromDate;
        DateField toDate;
        TextField minAge;
        TextField maxAge;
        ComboBox<Sex> patientSex;
        ListSelect<String> modalities;
        ListSelect<String> readingLocations;
        ListSelect<String> statuses;
        Button clearFilterButton;
        Panel filterPanel;

        CheckBox notAndAll;
        ComboBox<SequentialSearchScope> scope;
        ListSelect<Header> headers;
        Button addButton;
        Button deleteButton;
        Button clearButton;

        SentenceQueryInputLayout() {
            objectId = new ObjectId();
            sentenceQueryInstances = new HashMap<>();
            sentenceQueryInstanceLayouts = new VerticalLayout();
            Button addQueryInstanceButton = new Button("+ Query", event -> handleAddQueryLayout());
            addQueryInstanceButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);

            //start filters
            fromDate = new DateField(event -> handleFilterValueChange());
            fromDate.setWidth(8, Unit.EM);
            toDate = new DateField(event -> handleFilterValueChange());
            toDate.setWidth(8, Unit.EM);
            Label reportDateLabel = new Label("Report Date");
            reportDateLabel.setStyleName("caption");
            Label to = new Label("to");
            to.addStyleName("caption");
            HorizontalLayout dateLayout = new HorizontalLayout(reportDateLabel, fromDate, to, toDate);
            dateLayout.setComponentAlignment(reportDateLabel, Alignment.MIDDLE_RIGHT);
            dateLayout.setComponentAlignment(to, Alignment.MIDDLE_CENTER);

            minAge = new TextField(event -> handleFilterValueChange());
            minAge.setWidth(3, Unit.EM);
            maxAge = new TextField(event -> handleFilterValueChange());
            maxAge.setWidth(3, Unit.EM);
            Label ageLabel = new Label("Patient Age");
            ageLabel.setStyleName("caption");
            Label toLabel = new Label("to");
            toLabel.addStyleName("caption");
            HorizontalLayout ageLayout = new HorizontalLayout(ageLabel, minAge, toLabel, maxAge);
            ageLayout.setComponentAlignment(ageLabel, Alignment.MIDDLE_RIGHT);
            ageLayout.setComponentAlignment(toLabel, Alignment.MIDDLE_CENTER);

            patientSex = new ComboBox<>();
            patientSex.addValueChangeListener(event -> handleFilterValueChange());
            patientSex.setWidth(4, Unit.EM);
            patientSex.setItems(Sex.values());
            patientSex.setTextInputAllowed(false);
            Label patientSexLabel = new Label("Sex");
            patientSexLabel.setStyleName("caption");
            HorizontalLayout patientSexLayout = new HorizontalLayout(patientSexLabel, patientSex);
            patientSexLayout.setComponentAlignment(patientSexLabel, Alignment.MIDDLE_RIGHT);

            String url = "http://" + ((MachineLearningUi) UI.getCurrent()).getServerAddress() + "/machine-learning-query/distinct-values";
            DistinctValuesQueryRequest distinctValuesQueryRequest = new DistinctValuesQueryRequest("modality");
            distinctValuesQueryRequest.setUsername(userService.getUser().getUsername());
            distinctValuesQueryRequest.setOrganizationName(userService.getUser().getOrganizationName());
            modalities = new ListSelect<>();
            ResponseEntity<DistinctValuesQueryResponse> response;
            try {
                response = new RestTemplate().postForEntity(url, distinctValuesQueryRequest, DistinctValuesQueryResponse.class);
            } catch (HttpClientErrorException e) {
                Notification.show(e.toString(), Notification.Type.ERROR_MESSAGE);
                return;
            }
            if (response != null && response.getBody() != null) {
                List<String> possibleModalities = response.getBody().getDistinctValues();
                possibleModalities.removeIf(Objects::isNull); //remove null values from list
                modalities.setItems(possibleModalities);
                modalities.setRows(possibleModalities.size() < 3 ? possibleModalities.size() : 3);
            }
            modalities.addValueChangeListener(event -> handleFilterValueChange());
            Label modalitiesLabel = new Label("Modality");
            modalitiesLabel.setStyleName("caption");
            HorizontalLayout modalitiesLayout = new HorizontalLayout(modalitiesLabel, modalities);
            modalitiesLayout.setComponentAlignment(modalitiesLabel, Alignment.TOP_RIGHT);

            readingLocations = new ListSelect<>();
            distinctValuesQueryRequest = new DistinctValuesQueryRequest("readingLocation");
            distinctValuesQueryRequest.setUsername(userService.getUser().getUsername());
            distinctValuesQueryRequest.setOrganizationName(userService.getUser().getOrganizationName());
            response = new RestTemplate().postForEntity(url, distinctValuesQueryRequest, DistinctValuesQueryResponse.class);
            if (response != null && response.getBody() != null) {
                List<String> possibleReadingLocations = response.getBody().getDistinctValues();
                possibleReadingLocations.removeIf(Objects::isNull); //remove null values from list
                readingLocations.setItems(possibleReadingLocations);
                readingLocations.setRows(possibleReadingLocations.size() < 3 ? possibleReadingLocations.size() : 3);
            }
            readingLocations.addValueChangeListener(event -> handleFilterValueChange());
            Label readingLocationsLabel = new Label("Reading Location");
            readingLocationsLabel.setStyleName("caption");
            HorizontalLayout readingLocationsLayout = new HorizontalLayout(readingLocationsLabel, readingLocations);
            readingLocationsLayout.setComponentAlignment(readingLocationsLabel, Alignment.TOP_RIGHT);

            statuses = new ListSelect<>();
            distinctValuesQueryRequest = new DistinctValuesQueryRequest("status");
            distinctValuesQueryRequest.setUsername(userService.getUser().getUsername());
            distinctValuesQueryRequest.setOrganizationName(userService.getUser().getOrganizationName());
            response = new RestTemplate().postForEntity(url, distinctValuesQueryRequest, DistinctValuesQueryResponse.class);
            if (response != null && response.getBody() != null) {
                List<String> possibleStatuses = response.getBody().getDistinctValues();
                possibleStatuses.removeIf(Objects::isNull); //remove null values from list
                statuses.setItems(possibleStatuses);
                statuses.setRows(possibleStatuses.size() < 3 ? possibleStatuses.size() : 3);
            }
            statuses.addValueChangeListener(event -> handleFilterValueChange());
            Label statusesLabel = new Label("Status");
            statusesLabel.setStyleName("caption");
            HorizontalLayout statusesLayout = new HorizontalLayout(statusesLabel, statuses);
            statusesLayout.setComponentAlignment(statusesLabel, Alignment.TOP_RIGHT);

            filterPanel = new Panel();
            filterPanel.setSizeUndefined();
            Button filterButton = new Button("Filters", (Button.ClickListener) event -> handleFilterButton());
            filterButton.addStyleNames(ValoTheme.BUTTON_BORDERLESS_COLORED);
            HorizontalLayout filterButtonLayout = new HorizontalLayout(filterButton);
            filterButtonLayout.setWidth(CAPTION_WIDTH, Unit.EM);
            filterButtonLayout.setComponentAlignment(filterButton, Alignment.MIDDLE_RIGHT);

            clearFilterButton = new Button("Clear", (Button.ClickListener) event -> clearFilters());
            clearFilterButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            clearFilterButton.setEnabled(false);

            HorizontalLayout sentenceQueryFilterLayout = new HorizontalLayout(filterButtonLayout, filterPanel);
            sentenceQueryFilterLayout.setComponentAlignment(filterButtonLayout, Alignment.TOP_RIGHT);
            VerticalLayout filterPanelLayout = new VerticalLayout(new HorizontalLayout(dateLayout, ageLayout, patientSexLayout), new HorizontalLayout(modalitiesLayout, readingLocationsLayout, statusesLayout), clearFilterButton);
            filterPanelLayout.setComponentAlignment(clearFilterButton, Alignment.MIDDLE_RIGHT);
            filterPanel.setContent(filterPanelLayout);
            //end filters

            notAndAll = new CheckBox();
            notAndAll.addValueChangeListener(event -> handleValueChange());
            Label notAndAllLabel = new Label("Not And All");
            notAndAllLabel.setStyleName("caption-right");
            notAndAllLabel.setWidth(CAPTION_WIDTH, Unit.EM);
            HorizontalLayout notAndAllLayout = new HorizontalLayout(notAndAllLabel, this.notAndAll);
            notAndAllLayout.setComponentAlignment(notAndAllLabel, Alignment.TOP_RIGHT);
            notAndAllLayout.setComponentAlignment(notAndAll, Alignment.BOTTOM_LEFT);

            scope = new ComboBox<>();
            scope.setWidth(12, Unit.EM);
            scope.setRequiredIndicatorVisible(true);
            scope.setItems(SequentialSearchScope.getSequentialSearchScopes());
            scope.setTextInputAllowed(false);
            scope.addValueChangeListener(event -> handleScopeValueChange());
            Label sequentialSearchScopeLabel = new Label("Scope");
            sequentialSearchScopeLabel.setStyleName("caption-right");
            sequentialSearchScopeLabel.setWidth(CAPTION_WIDTH, Unit.EM);
            HorizontalLayout sequentialSearchScopeLayout = new HorizontalLayout(sequentialSearchScopeLabel, scope);
            sequentialSearchScopeLayout.setComponentAlignment(sequentialSearchScopeLabel, Alignment.MIDDLE_RIGHT);

            headers = new ListSelect<>();
            List<Header> possibleHeaders = Header.getHeaders(organizationName);
            headers.setItems(possibleHeaders);
            headers.setRows(possibleHeaders.size() < 3 ? possibleHeaders.size() : 3);
            headers.addValueChangeListener(event -> handleValueChange());
            Label headersLabel = new Label("Report Headers");
            headersLabel.setStyleName("caption-right");
            headersLabel.setWidth(CAPTION_WIDTH, Unit.EM);
            HorizontalLayout headersLayout = new HorizontalLayout(headersLabel, headers);
            headersLayout.setComponentAlignment(headersLabel, Alignment.TOP_RIGHT);

            addButton = new Button("Add", (Button.ClickListener) event -> handleAdd());
            addButton.setEnabled(false);
            addButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            clearButton = new Button("Clear", (Button.ClickListener) event -> handleClear());
            clearButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            clearButton.setEnabled(false);
            deleteButton = new Button("Delete", (Button.ClickListener) event -> handleDelete());
            deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
            HorizontalLayout buttonLayout = new HorizontalLayout(addButton, clearButton, deleteButton);

            VerticalLayout panelLayout = new VerticalLayout(addQueryInstanceButton, sentenceQueryInstanceLayouts, sentenceQueryFilterLayout, notAndAllLayout, sequentialSearchScopeLayout, headersLayout, buttonLayout);
            panelLayout.setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
            Panel panel = new Panel(panelLayout);
            panel.setSizeUndefined();
            addComponent(panel);
            handleAddQueryLayout();
        }

        void handleValueChange() {
            clearButton.setEnabled(!isEmpty());
        }

        void handleClear() {
            notAndAll.clear();
            scope.clear();
            headers.clear();
        }

        void handleFilterValueChange() {
            clearFilterButton.setEnabled(!isFilterEmpty());
        }

        void handleAddQueryLayout() {
            sentenceQueryInstanceLayout = new SentenceQueryInstanceLayout(sentenceQueryInstances, this);
            sentenceQueryInstanceLayouts.addComponent(sentenceQueryInstanceLayout);
        }

        void handleFilterButton() {
            filterPanel.setVisible(!filterPanel.isVisible());
        }

        void handleScopeValueChange() {
            handleValueChange();
            addButton.setEnabled(!sentenceQueryInstances.isEmpty() && scope.getValue() != null);
        }

        void handleAdd() {
            SentenceQueryInput sentenceQueryInput = new SentenceQueryInput();
            sentenceQueryInput.setUsername(userService.getUser().getUsername());
            sentenceQueryInput.setOrganizationName(organizationName);
            sentenceQueryInput.setExecuteAsynchronously(true);
            sentenceQueryInput.setSentenceQueryInstances(new ArrayList<>(sentenceQueryInstances.values()));
            SentenceQueryFilter sentenceQueryFilter = new SentenceQueryFilter();
            sentenceQueryFilter.setReportDateRange(fromDate.getValue() != null && toDate.getValue() != null ? new ArrayList<>(Arrays.asList(fromDate.getValue(), toDate.getValue())) : null);
            sentenceQueryFilter.setPatientAgeRange(!minAge.getValue().isEmpty() && !maxAge.getValue().isEmpty() ? new ArrayList<>(Arrays.asList(Integer.parseInt(minAge.getValue()), Integer.parseInt(maxAge.getValue()))) : null);
            sentenceQueryFilter.setPatientSex(patientSex.getValue());
            sentenceQueryFilter.setModalities(modalities.getValue() != null && !modalities.getValue().isEmpty() ? new ArrayList<>(modalities.getValue()) : null);
            sentenceQueryFilter.setReadingLocations(readingLocations.getValue() != null && !readingLocations.getValue().isEmpty() ? new ArrayList<>(readingLocations.getValue()) : null);
            sentenceQueryFilter.setStatuses(statuses.getValue() != null && !statuses.getValue().isEmpty() ? new ArrayList<>(statuses.getValue()) : null);
            sentenceQueryInput.setSentenceQueryFilter(sentenceQueryFilter);
            sentenceQueryInput.setNotAndAll(notAndAll.getValue());
            sentenceQueryInput.setPerformSequentialSearch(true);
            sentenceQueryInput.setSequentialSearchScope(scope.getValue());
            sentenceQueryInput.setHeaders(new ArrayList<>(headers.getValue()));
            sentenceQueryInputs.put(objectId, sentenceQueryInput);
            addButton.setEnabled(false);
            submitButton.setEnabled(true);
            sentenceQueryInstances = new HashMap<>();
        }

        void handleDelete() {
            sentenceQueryInputs.remove(objectId);
            this.setVisible(false);
            submitButton.setEnabled(!sentenceQueryInputs.isEmpty());
        }

        private void clearFilters() {
            fromDate.clear();
            toDate.clear();
            minAge.clear();
            maxAge.clear();
            patientSex.clear();
            modalities.clear();
            readingLocations.clear();
            statuses.clear();
        }

        boolean isFilterEmpty() {
            return fromDate.isEmpty() && toDate.isEmpty() && minAge.isEmpty() && maxAge.isEmpty() && patientSex.isEmpty() && modalities.isEmpty() && readingLocations.isEmpty() && statuses.isEmpty();
        }

        boolean isEmpty() {
            return notAndAll.isEmpty() && scope.isEmpty() && headers.isEmpty();
        }
    }

    private class SentenceQueryInstanceLayout extends VerticalLayout {
        Map<ObjectId, SentenceQueryInstance> sentenceQueryInstances;
        ObjectId objectId;
        TextField tokens;
        ListSelect<EdgeName> existence;
        ListSelect<String> simpleCystModifiers;
        ListSelect<String> diseaseModifier;
        ListSelect<String> diseaseLocation;
        ListSelect<String> laterality;
        ListSelect<String> findingSiteModifier;
        ListSelect<String> heterogeneousFindingSite;
        ListSelect<String> enlargedFindingSite;
        TextField minMeasurement;
        TextField maxMeasurement;
        ComboBox<MeasurementClassification> measurementClassification;
        ComboBox<LogicalOperator> appender;
        Button addButton;
        Button clearButton;
        Button deleteButton;
        SentenceQueryInputLayout sentenceQueryInputLayout;

        SentenceQueryInstanceLayout(Map<ObjectId, SentenceQueryInstance> sentenceQueryInstances, SentenceQueryInputLayout sentenceQueryInputLayout) {
            this.sentenceQueryInstances = sentenceQueryInstances;
            this.sentenceQueryInputLayout = sentenceQueryInputLayout;
            objectId = new ObjectId();
            tokens = new TextField();
            tokens.setWidth(28, Unit.EM);
            tokens.setDescription("Comma Delimited");
            tokens.setRequiredIndicatorVisible(true);
            tokens.addValueChangeListener(event -> handleTokenValueChange());
            Label tokensLabel = new Label("Search Terms");
            tokensLabel.setStyleName("caption");
            HorizontalLayout tokensLayout = new HorizontalLayout(tokensLabel, tokens);
            tokensLayout.setComponentAlignment(tokensLabel, Alignment.MIDDLE_RIGHT);

            existence = new ListSelect<>();
            List<EdgeName> existenceOptions = new ArrayList<>(EdgeName.getExistenceEdgeNames());
            existence.setItems(existenceOptions);
            existence.setRows(existenceOptions.size() < 3 ? existenceOptions.size() : 3);
            existence.addValueChangeListener(event -> handleInputValueChange());
            Label existenceLabel = new Label("Existence");
            existenceLabel.setStyleName("caption");
            HorizontalLayout existenceLayout = new HorizontalLayout(existenceLabel, existence);
            existenceLayout.setComponentAlignment(existenceLabel, Alignment.TOP_RIGHT);

            simpleCystModifiers = new ListSelect<>();
            List<String> simpleCystModifierOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, SIMPLE_CYST_MODIFIERS));
            simpleCystModifiers.setItems(simpleCystModifierOptions);
            simpleCystModifiers.setRows(simpleCystModifierOptions.size() < 3 ? simpleCystModifierOptions.size() : 3);
            simpleCystModifiers.addValueChangeListener(event -> handleInputValueChange());
            Label simpleCystModifiersLabel = new Label("Simple Cyst Modifiers");
            simpleCystModifiersLabel.setStyleName("caption");
            HorizontalLayout simpleCystModifiersLayout = new HorizontalLayout(simpleCystModifiersLabel, simpleCystModifiers);
            simpleCystModifiersLayout.setComponentAlignment(simpleCystModifiersLabel, Alignment.TOP_RIGHT);

            diseaseModifier = new ListSelect<>();
            List<String> diseaseModifierOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, DISEASE_MODIFIER));
            diseaseModifier.setItems(diseaseModifierOptions);
            diseaseModifier.setRows(diseaseModifierOptions.size() < 3 ? diseaseModifierOptions.size() : 3);
            diseaseModifier.addValueChangeListener(event -> handleInputValueChange());
            Label diseaseModifierLabel = new Label("Disease Modifier");
            diseaseModifierLabel.setStyleName("caption");
            HorizontalLayout diseaseModifierLayout = new HorizontalLayout(diseaseModifierLabel, diseaseModifier);
            diseaseModifierLayout.setComponentAlignment(diseaseModifierLabel, Alignment.TOP_RIGHT);

            diseaseLocation = new ListSelect<>();
            List<String> diseaseLocationOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, DISEASE_LOCATION));
            diseaseLocation.setItems(diseaseLocationOptions);
            diseaseLocation.setRows(diseaseLocationOptions.size() < 3 ? diseaseLocationOptions.size() : 3);
            diseaseLocation.addValueChangeListener(event -> handleInputValueChange());
            Label diseaseLocationLabel = new Label("Disease Location");
            diseaseLocationLabel.setStyleName("caption");
            HorizontalLayout diseaseLocationLayout = new HorizontalLayout(diseaseLocationLabel, diseaseLocation);
            diseaseLocationLayout.setComponentAlignment(diseaseLocationLabel, Alignment.TOP_RIGHT);

            laterality = new ListSelect<>();
            List<String> lateralityOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, LATERALITY));
            laterality.setItems(lateralityOptions);
            laterality.setRows(lateralityOptions.size() < 3 ? lateralityOptions.size() : 3);
            laterality.addValueChangeListener(event -> handleInputValueChange());
            Label lateralityLabel = new Label("Laterality");
            lateralityLabel.setStyleName("caption");
            HorizontalLayout lateralityLayout = new HorizontalLayout(lateralityLabel, laterality);
            lateralityLayout.setComponentAlignment(lateralityLabel, Alignment.TOP_RIGHT);

            findingSiteModifier = new ListSelect<>();
            List<String> findingSiteModifierOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, FINDING_SITE_MODIFIER));
            findingSiteModifier.setItems(findingSiteModifierOptions);
            findingSiteModifier.setRows(findingSiteModifierOptions.size() < 3 ? findingSiteModifierOptions.size() : 3);
            findingSiteModifier.addValueChangeListener(event -> handleInputValueChange());
            Label findingSiteModifierLabel = new Label("Finding Site Modifier");
            findingSiteModifierLabel.setStyleName("caption");
            HorizontalLayout findingSiteModifierLayout = new HorizontalLayout(findingSiteModifierLabel, findingSiteModifier);
            findingSiteModifierLayout.setComponentAlignment(findingSiteModifierLabel, Alignment.TOP_RIGHT);

            heterogeneousFindingSite = new ListSelect<>();
            List<String> heterogeneousFindingSiteOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, HETEROGENEOUS_FINDING_SITE));
            heterogeneousFindingSite.setItems(heterogeneousFindingSiteOptions);
            heterogeneousFindingSite.setRows(heterogeneousFindingSiteOptions.size() < 3 ? heterogeneousFindingSiteOptions.size() : 3);
            heterogeneousFindingSite.addValueChangeListener(event -> handleInputValueChange());
            Label heterogeneousFindingSiteLabel = new Label("Heterogeneous Finding Site");
            heterogeneousFindingSiteLabel.setStyleName("caption");
            HorizontalLayout heterogeneousFindingSiteLayout = new HorizontalLayout(heterogeneousFindingSiteLabel, heterogeneousFindingSite);
            heterogeneousFindingSiteLayout.setComponentAlignment(heterogeneousFindingSiteLabel, Alignment.TOP_RIGHT);

            enlargedFindingSite = new ListSelect<>();
            List<String> enlargedFindingSiteOptions = new ArrayList<>(PossibleEdgeValue.getPossibleEdgeValues(organizationName, ENLARGED_FINDING_SITE));
            enlargedFindingSite.setItems(enlargedFindingSiteOptions);
            enlargedFindingSite.setRows(enlargedFindingSiteOptions.size() < 3 ? enlargedFindingSiteOptions.size() : 3);
            enlargedFindingSite.addValueChangeListener(event -> handleInputValueChange());
            Label enlargedFindingSiteLabel = new Label("Enlarged Finding Site");
            enlargedFindingSiteLabel.setStyleName("caption");
            HorizontalLayout enlargedFindingSiteLayout = new HorizontalLayout(enlargedFindingSiteLabel, enlargedFindingSite);
            enlargedFindingSiteLayout.setComponentAlignment(enlargedFindingSiteLabel, Alignment.TOP_RIGHT);

            minMeasurement = new TextField();
            minMeasurement.setWidth(4, Unit.EM);
            minMeasurement.addValueChangeListener(event -> handleInputValueChange());
            maxMeasurement = new TextField();
            maxMeasurement.setWidth(4, Unit.EM);
            maxMeasurement.addValueChangeListener(event -> handleInputValueChange());
            Label measurementLabel = new Label("Measurement");
            measurementLabel.setStyleName("caption");
            Label toLabel = new Label("to");
            toLabel.addStyleName("caption");
            HorizontalLayout measurementLayout = new HorizontalLayout(measurementLabel, minMeasurement, toLabel, maxMeasurement);
            measurementLayout.setComponentAlignment(measurementLabel, Alignment.MIDDLE_RIGHT);
            measurementLayout.setComponentAlignment(toLabel, Alignment.MIDDLE_CENTER);

            measurementClassification = new ComboBox<>();
            measurementClassification.setWidth(12, Unit.EM);
            measurementClassification.setItems(MeasurementClassification.getMeasurementClassifications());
            measurementClassification.setTextInputAllowed(false);
            measurementClassification.setEmptySelectionAllowed(false);
            measurementClassification.addValueChangeListener(event -> handleInputValueChange());
            Label measurementClassificationLabel = new Label("Measurement Classification");
            measurementClassificationLabel.setStyleName("caption");
            HorizontalLayout measurementClassificationLayout = new HorizontalLayout(measurementClassificationLabel, measurementClassification);
            measurementClassificationLayout.setComponentAlignment(measurementClassificationLabel, Alignment.MIDDLE_RIGHT);

            appender = new ComboBox<>();
            appender.setWidth(10, Unit.EM);
            appender.setItems(LogicalOperator.getSeqSearchLogicalOperators());
            appender.setTextInputAllowed(false);
            appender.setEmptySelectionAllowed(false);
            appender.addValueChangeListener(event -> handleInputValueChange());
            Label appenderLabel = new Label("Logical Operator");
            appenderLabel.setStyleName("caption");
            HorizontalLayout appenderLayout = new HorizontalLayout(appenderLabel, appender);
            appenderLayout.setComponentAlignment(appenderLabel, Alignment.MIDDLE_RIGHT);

            addButton = new Button("Add", (Button.ClickListener) event -> handleAdd());
            addButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            addButton.setEnabled(false);
            clearButton = new Button("Clear", (Button.ClickListener) event -> handleClear());
            clearButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            clearButton.setEnabled(false);
            deleteButton = new Button("Delete", (Button.ClickListener) event -> handleDelete());
            deleteButton.addStyleName(ValoTheme.BUTTON_DANGER);
            HorizontalLayout buttonLayout = new HorizontalLayout(addButton, clearButton, deleteButton);

            VerticalLayout panelLayout = new VerticalLayout(tokensLayout, new HorizontalLayout(existenceLayout, simpleCystModifiersLayout, diseaseModifierLayout, diseaseLocationLayout), new HorizontalLayout(lateralityLayout, findingSiteModifierLayout, heterogeneousFindingSiteLayout, enlargedFindingSiteLayout), new HorizontalLayout(measurementLayout, measurementClassificationLayout, appenderLayout), buttonLayout);
            panelLayout.setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
            Panel panel = new Panel(panelLayout);
            panel.setSizeUndefined();
            addComponent(panel);
        }

        void handleTokenValueChange() {
            addButton.setEnabled(!tokens.getValue().isEmpty());
            handleInputValueChange();
        }

        void handleInputValueChange() {
            clearButton.setEnabled(!isEmpty());
        }

        void handleAdd() {
            SentenceQueryInstance sentenceQueryInstance = new SentenceQueryInstance();
            sentenceQueryInstance.setTokens(tokens.getValue().isEmpty() ? new ArrayList<>() : Arrays.stream(tokens.getValue().split(",")).map(String::trim).collect(Collectors.toList()));
            List<EdgeQuery> edges = new ArrayList<>();
            if (!existence.getValue().isEmpty()) {
                for (EdgeName existenceEdge : existence.getValue()) {
                    EdgeQuery edgeQuery = new EdgeQuery();
                    edgeQuery.setName(existenceEdge);
                    edges.add(edgeQuery);
                }
            }
            if (!simpleCystModifiers.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(SIMPLE_CYST_MODIFIERS);
                edgeQuery.setValues(simpleCystModifiers.getValue());
                edges.add(edgeQuery);
            }
            if (!diseaseModifier.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(DISEASE_MODIFIER);
                edgeQuery.setValues(diseaseModifier.getValue());
                edges.add(edgeQuery);
            }
            if (!diseaseLocation.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(DISEASE_LOCATION);
                edgeQuery.setValues(diseaseLocation.getValue());
                edges.add(edgeQuery);
            }
            if (!laterality.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(LATERALITY);
                edgeQuery.setValues(laterality.getValue());
                edges.add(edgeQuery);
            }
            if (!findingSiteModifier.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(FINDING_SITE_MODIFIER);
                edgeQuery.setValues(findingSiteModifier.getValue());
                edges.add(edgeQuery);
            }
            if (!heterogeneousFindingSite.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(HETEROGENEOUS_FINDING_SITE);
                edgeQuery.setValues(heterogeneousFindingSite.getValue());
                edges.add(edgeQuery);
            }
            if (!enlargedFindingSite.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(ENLARGED_FINDING_SITE);
                edgeQuery.setValues(enlargedFindingSite.getValue());
                edges.add(edgeQuery);
            }
            if (!minMeasurement.getValue().isEmpty() || !maxMeasurement.getValue().isEmpty()) {
                EdgeQuery edgeQuery = new EdgeQuery();
                edgeQuery.setName(MEASUREMENT);
                edgeQuery.setValues(new LinkedHashSet<>(new ArrayList<>(Arrays.asList(minMeasurement.getValue(), maxMeasurement.getValue()))));
                edges.add(edgeQuery);
            }
            sentenceQueryInstance.setEdges(edges);
            sentenceQueryInstance.setMeasurementClassification(measurementClassification.getValue());
            sentenceQueryInstance.setAppender(appender.getValue());
            sentenceQueryInstances.put(objectId, sentenceQueryInstance);
            addButton.setVisible(false);
            clearButton.setVisible(false);
            sentenceQueryInputLayout.addButton.setEnabled(!sentenceQueryInputLayout.scope.isEmpty());
        }

        void handleClear() {
            tokens.clear();
            existence.clear();
            simpleCystModifiers.clear();
            diseaseModifier.clear();
            diseaseLocation.clear();
            laterality.clear();
            findingSiteModifier.clear();
            heterogeneousFindingSite.clear();
            enlargedFindingSite.clear();
            minMeasurement.clear();
            maxMeasurement.clear();
            measurementClassification.clear();
            appender.clear();
        }

        void handleDelete() {
            sentenceQueryInstances.remove(objectId);
            this.setVisible(false);
            sentenceQueryInputLayout.addButton.setEnabled(!sentenceQueryInstances.isEmpty());
        }

        boolean isEmpty() {
            return tokens.isEmpty() && existence.isEmpty() && simpleCystModifiers.isEmpty() && diseaseModifier.isEmpty() && diseaseLocation.isEmpty() && laterality.isEmpty() && findingSiteModifier.isEmpty() && heterogeneousFindingSite.isEmpty() && enlargedFindingSite.isEmpty() && minMeasurement.isEmpty() && maxMeasurement.isEmpty() && measurementClassification.isEmpty() && appender.isEmpty();
        }
    }

    class SaveQueryWindow extends Window {
        private ComboBox<SaveType> saveType;
        private TextField newName;
        private Button saveButton;

        SaveQueryWindow() {
            super("Save Query");
            saveType = new ComboBox<>();
            saveType.setWidth(18, Unit.EM);
            saveType.setItems(NEW, UPDATE, RENAME);
            saveType.setSelectedItem(NEW);
            saveType.setTextInputAllowed(false);
            saveType.addValueChangeListener(event -> handleSaveTypeValueChange());
            newName = new TextField();
            newName.setWidth(18, Unit.EM);
            newName.setRequiredIndicatorVisible(true);
            newName.addValueChangeListener(event -> handleNameValueChange());
            Label newNameLabel = new Label("Query Name");
            newNameLabel.setStyleName("caption");
            HorizontalLayout newNameLayout = new HorizontalLayout(newNameLabel, newName);
            newNameLayout.setComponentAlignment(newNameLabel, Alignment.MIDDLE_RIGHT);
            saveButton = new Button("Save", event -> handleSaveButtonClick());
            saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            saveButton.setEnabled(false);
            Button closeButton = new Button("Cancel", event -> handleCloseButtonClick());
            closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, closeButton);
            VerticalLayout layout = new VerticalLayout(saveType, newNameLayout, buttonLayout);
            layout.setComponentAlignment(buttonLayout, Alignment.BOTTOM_RIGHT);
            setContent(layout);
        }

        private void handleSaveTypeValueChange() {
            switch (saveType.getValue()) {
                case NEW:
                case RENAME:
                    newName.setVisible(true);
                    saveButton.setEnabled(false);
                    break;
                case UPDATE:
                    newName.setVisible(false);
                    saveButton.setEnabled(true);
            }
        }

        private void handleNameValueChange() {
            saveButton.setEnabled(true);
        }

        private void handleSaveButtonClick() {
            switch (queryType.getValue()) {
                case CREATE:
                    SequentialSearchQuery sequentialSearchQuery = new SequentialSearchQuery();
                    sequentialSearchQuery.setUsername(userService.getUser().getUsername());
                    sequentialSearchQuery.setQueryName(newName.getValue());
                    sequentialSearchQuery.setQueryInput(sequentialSearchQueryInput);
                    sequentialSearchQueryService.saveQuery(sequentialSearchQuery);
                    break;
                case LOAD:
                    switch (saveType.getValue()) {
                        case NEW:
                            sequentialSearchQuery = new SequentialSearchQuery();
                            sequentialSearchQuery.setUsername(userService.getUser().getUsername());
                            sequentialSearchQuery.setQueryName(newName.getValue());
                            sequentialSearchQuery.setQueryInput(sequentialSearchQueryInput);
                            sequentialSearchQueryService.saveQuery(sequentialSearchQuery);
                            break;
                        case UPDATE:
                            sequentialSearchQuery = savedQuery.getValue();
                            sequentialSearchQuery.setQueryInput(sequentialSearchQueryInput);
                            sequentialSearchQueryService.saveQuery(sequentialSearchQuery);
                            break;
                        case RENAME:
                            sequentialSearchQuery = savedQuery.getValue();
                            sequentialSearchQuery.setQueryName(newName.getValue());
                            sequentialSearchQuery.setQueryInput(sequentialSearchQueryInput);
                            sequentialSearchQueryService.saveQuery(sequentialSearchQuery);
                    }
            }
            handleCloseButtonClick();
            clearSentenceQueryInputs();
        }

        private void handleCloseButtonClick() {
            close();
            saveQueryWindow = null;
        }
    }
}