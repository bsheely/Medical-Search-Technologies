package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.Component;
import com.mst.machineLearningModel.OrganizationName;
import com.mst.machineLearningModel.User;
import com.mst.machineLearningUi.service.*;
import com.mst.machineLearningUi.utility.ApplicationProperties;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static com.mst.machineLearningModel.Component.*;

@Theme("mytheme")
@SpringUI
public class MachineLearningUi extends UI implements View {
    static final String LOGIN = "login";
    private VerticalLayout layout;
    private HorizontalLayout header;
    private TabSheet tabSheet;
    private final ApplicationProperties applicationProperties;
    private final UserService userService;
    private final ReportService reportService;
    private final CptService cptService;
    private final IcdService icdService;
    private final OrganizationService organizationService;
    private final UserLocationService userLocationService;
    private final AuditService auditService;
    private final StatisticsService statisticsService;
    private final SequentialSearchQueryService sequentialSearchQueryService;
    private final TokenClassService tokenClassService;
    private final OrganizationSpecificConfigurationService organizationSpecificConfigurationService;
    private final GraphNodeRelationshipService graphNodeRelationshipService;
    private final MongoTemplate mongoTemplate;
    private CodingTab codingTab;
    private LoginTab loginTab;
    @Value("${spring.config.additional-location}")
    private String serverAddress;

    @Autowired
    public MachineLearningUi(ApplicationProperties applicationProperties, UserService userService, ReportService reportService, CptService cptService, IcdService icdService, OrganizationService organizationService, UserLocationService userLocationService, MongoTemplate mongoTemplate, AuditService auditService, StatisticsService statisticsService, SequentialSearchQueryService sequentialSearchQueryService, TokenClassService tokenClassService, OrganizationSpecificConfigurationService organizationSpecificConfigurationService, GraphNodeRelationshipService graphNodeRelationshipService) {
        this.applicationProperties = applicationProperties;
        this.userService = userService;
        this.reportService = reportService;
        this.cptService = cptService;
        this.icdService = icdService;
        this.organizationService = organizationService;
        this.userLocationService = userLocationService;
        this.mongoTemplate = mongoTemplate;
        this.auditService = auditService;
        this.statisticsService = statisticsService;
        this.sequentialSearchQueryService = sequentialSearchQueryService;
        this.tokenClassService = tokenClassService;
        this.organizationSpecificConfigurationService = organizationSpecificConfigurationService;
        this.graphNodeRelationshipService = graphNodeRelationshipService;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        layout = new VerticalLayout();
        layout.setSizeFull();
        setContent(layout);
        loginTab = new LoginTab(userService, organizationService);
        Navigator navigator = new Navigator(this, layout);
        navigator.addView("", loginTab);
    }

    CodingTab getCodingTab() {
        return codingTab;
    }

    void setOrganizationName(OrganizationName organizationName) {
        getSession().setAttribute("organizationName", organizationName);
    }

    void setUser(User user) {
        getSession().setAttribute("user", user);
    }

    void configureTabs(List<Component> viewableTabs) {
        Button logout = new Button("Logout", (Button.ClickListener) event -> handleLogout());
        logout.addStyleNames(ValoTheme.BUTTON_TINY);
        Label user = new Label(userService.getUserName());
        HorizontalLayout userLayout = new HorizontalLayout(user, logout);
        header = new HorizontalLayout(new Image(null, new ThemeResource("images/mst_medium.png")), userLayout);
        header.setWidth("100%");
        header.setComponentAlignment(userLayout, Alignment.MIDDLE_RIGHT);
        layout.addComponent(header);
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.addStyleName(ValoTheme.TABSHEET_FRAMED);
        layout.addComponent(tabSheet);
        layout.setExpandRatio(tabSheet, 1.0f);
        tabSheet.setVisible(true);
        HomeTab homeTab = null;
        if (viewableTabs.contains(AUDIT)) {
            AuditTab tab = new AuditTab(tabSheet, userService, organizationService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(ACCOUNTS)) {
            AccountsTab tab = new AccountsTab(tabSheet, userService, organizationService, organizationSpecificConfigurationService, userLocationService, auditService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(CLASSIFICATION)) {
            ClassificationTab tab = new ClassificationTab(tabSheet, userService, organizationService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(CODING)) {
            codingTab = new CodingTab(tabSheet, userService, reportService, cptService, icdService, userLocationService, organizationService, auditService);
            tabSheet.setSelectedTab(codingTab);
        }
        List<Component> collectionsSubTabs = Component.getCollectionsSubTabs();
        for (Component subTab : collectionsSubTabs)
            if (viewableTabs.contains(subTab)) {
                CollectionsTab tab = new CollectionsTab(tabSheet, userService, auditService, statisticsService, organizationService, organizationSpecificConfigurationService, graphNodeRelationshipService);
                tabSheet.setSelectedTab(tab);
                break;
            }
        if (viewableTabs.contains(DATABASE)) {
            DatabaseTab tab = new DatabaseTab(tabSheet, mongoTemplate, userService, auditService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(DEIDENTIFICATION)) {
            DeIdentificationTab tab = new DeIdentificationTab(tabSheet, reportService, userService, auditService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(FILE_DOWNLOAD)) {
            FileDownloadTab tab = new FileDownloadTab(tabSheet, organizationService, userService, auditService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(GENETIC)) {
            GeneticTab tab = new GeneticTab(tabSheet, reportService, userService, auditService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(HOME))
            homeTab = new HomeTab(tabSheet);
        if (viewableTabs.contains(MACHINE_LEARNING_PROCESSING)) {
            MachineLearningProcessingTab tab = new MachineLearningProcessingTab(tabSheet, userService, organizationService, tokenClassService, auditService, organizationSpecificConfigurationService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(MACHINE_LEARNING_REPORTS)) {
            MachineLearningReportsTab tab = new MachineLearningReportsTab(tabSheet, userService, organizationService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(REPORTS)) {
            ReportsTab tab = new ReportsTab(tabSheet, userService, organizationService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(SEARCH)) {
            SearchTab tab = new SearchTab(tabSheet);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(SEQUENTIAL_SEARCH)) {
            SequentialSearchTab tab = new SequentialSearchTab(tabSheet, userService, sequentialSearchQueryService);
            tabSheet.setSelectedTab(tab);
        }
        if (viewableTabs.contains(HOME))
            tabSheet.setSelectedTab(homeTab);
        loginTab.setVisible(false);
    }

    private void handleLogout() {
        layout.removeComponent(header);
        layout.removeComponent(tabSheet);
        setUser(null);
        setOrganizationName(null);
        loginTab.clear();
        loginTab.setVisible(true);
    }
}
