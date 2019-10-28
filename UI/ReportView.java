package com.mst.machineLearningUi.view;

import com.mst.machineLearningModel.Report;
import com.mst.machineLearningModel.Component;
import com.mst.machineLearningUi.model.*;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mst.machineLearningModel.Component.*;

class ReportView extends VerticalLayout {
    private Label accessionNumber;
    private Label reportDate;
    private Label patientName;
    private Label patientDob;
    private Label patientRecordNumber;
    private Label patientSex;
    private Label icdCodes;
    private Label cptCodes;
    private List<Report> reports;
    private ReportViewCheckboxGroup highlightSelection1;
    private ReportViewCheckboxGroup highlightSelection2;
    private ReportViewCheckboxGroup highlightSelection3;
    private RichTextArea textArea;
    private Map<String, List<HighlightSelection>> highlightSelectionsByReport;
    private int currentReportIndex;
    private NavigationBar navigationBar;
    private Component tabType;

    ReportView(Component tabType) {
        this.tabType = tabType;
        setWidth("100%");
        highlightSelectionsByReport = new HashMap<>();
        reports = new ArrayList<>();
        accessionNumber = new Label();
        reportDate = new Label();
        patientName = new Label();
        patientDob = new Label();
        patientSex = new Label();
        patientRecordNumber = new Label();
        icdCodes = new Label();
        cptCodes = new Label();
        HorizontalLayout accessionNumberLayout = new HorizontalLayout(new Label("<b>Report ID:<b> ", ContentMode.HTML), accessionNumber);
        HorizontalLayout reportDateLayout = new HorizontalLayout(new Label("<b>Report Date:<b> ", ContentMode.HTML), reportDate);
        HorizontalLayout patientNameLayout = new HorizontalLayout(new Label("<b>Patient Name:<b> ", ContentMode.HTML), patientName);
        HorizontalLayout patientDobLayout = new HorizontalLayout(new Label("<b>Patient DOB:<b> ", ContentMode.HTML), patientDob);
        HorizontalLayout sexLayout = new HorizontalLayout(new Label("<b>Sex:<b> ", ContentMode.HTML), patientSex);
        HorizontalLayout patientMRNLayout = new HorizontalLayout(new Label("<b>Patient MRN:<b> ", ContentMode.HTML), patientRecordNumber);
        HorizontalLayout icdLayout = new HorizontalLayout(new Label("<b>ICD Codes:<b> ", ContentMode.HTML), icdCodes);
        HorizontalLayout cptLayout = new HorizontalLayout(new Label("<b>CPT Codes:<b> ", ContentMode.HTML), cptCodes);
        highlightSelection1 = new ReportViewCheckboxGroup(this);
        highlightSelection2 = new ReportViewCheckboxGroup(this);
        highlightSelection3 = new ReportViewCheckboxGroup(this);
        highlightSelection1.setVisible(false);
        highlightSelection2.setVisible(false);
        highlightSelection3.setVisible(false);
        addComponent(highlightSelection1);
        addComponent(highlightSelection2);
        addComponent(highlightSelection3);
        addComponent(new HorizontalLayout(accessionNumberLayout, reportDateLayout, patientNameLayout, patientDobLayout, sexLayout, patientMRNLayout));
        if (tabType.equals(CODING))
            addComponent(new HorizontalLayout(icdLayout, cptLayout));
        textArea = new RichTextArea();
        textArea.setWidth("100%");
        textArea.setReadOnly(true);
        textArea.addStyleName("v-richtextarea");
        Panel reportPanel = new Panel();
        reportPanel.setContent(textArea);
        addComponent(reportPanel);
        navigationBar = new NavigationBar(this);
        if (!tabType.equals(CODING) && !tabType.equals(SEQUENTIAL_SEARCH))
            addComponent(navigationBar);
    }

    Report getCurrentlySelectedReport() {
        return reports.get(currentReportIndex);
    }

    void setHighlightSelectionsByReport(Map<String, List<HighlightSelection>> highlightSelectionsByReport) {
        this.highlightSelectionsByReport = highlightSelectionsByReport;
    }

    void setReports(List<Report> reports) {
        this.reports = new ArrayList<>(reports);
        currentReportIndex = 0;
        if (!reports.isEmpty()) {
            Report report = reports.get(currentReportIndex);
            setReportHeader(report);
            textArea.setValue(appendReportNotesToReportText(report.getReportText(), report.getReportNotes()));
            int size = reports.size();
            navigationBar.setReportCount(size);
            if (size > 1) {
                navigationBar.setVisible(true);
            } else
                navigationBar.setVisible(false);
        }
    }

    void appendNotesToReportText(String notes) {
        textArea.setValue(appendReportNotesToReportText(reports.get(currentReportIndex).getReportText(), notes));
    }

    void displayReportAtIndex(int index) {
        currentReportIndex = index;
        if (reports == null || reports.size() <= currentReportIndex)
            return;
        Report report = reports.get(currentReportIndex);
        setReportHeader(report);
        List<HighlightSelection> highlightSelections = highlightSelectionsByReport.get(report.getAccessionNumber());
        if (highlightSelections != null && !highlightSelections.isEmpty()) {
            setCheckboxItems(highlightSelections);
            highlightReport(highlightSelections);
        }
        String notes = report.getReportNotes();
        if (notes != null)
            textArea.setValue(appendReportNotesToReportText(report.getReportText(), notes));
    }

    void highlightReport(List<HighlightSelection> highlightSelections) {
        final String OPEN_SPAN_COLOR = "<span style=\"background-color:";
        final String CLOSE_COLOR = "\">";
        final String CLOSE_SPAN = "</span>";
        if (reports.isEmpty())
            return;
        Report report = reports.get(currentReportIndex);
        String text = report.getReportText();
        textArea.setValue(text); //removes any HTML
        for (HighlightSelection highlightSelection : highlightSelections) {
            String token = highlightSelection.getText();
            if (text.contains(token))
                text = text.replace(token, OPEN_SPAN_COLOR + highlightSelection.getHexCode() + CLOSE_COLOR + token + CLOSE_SPAN);
        }
        textArea.setValue(text);
    }

    private void setCheckboxItems(List<HighlightSelection> highlightSelections) {
        highlightSelection1.clear();
        highlightSelection2.clear();
        highlightSelection3.clear();
        int index = 0;
        for (HighlightSelection highlightSelection : highlightSelections) {
            if (index < 6) {
                highlightSelection1.setVisible(true);
                highlightSelection1.addItem(highlightSelection.getCaption(), highlightSelection.getText(), highlightSelection.getHexCode());
                highlightSelection1.setAllCheckboxesTrue();
            }
            else if (index < 12) {
                highlightSelection2.setVisible(true);
                highlightSelection2.addItem(highlightSelection.getCaption(), highlightSelection.getText(), highlightSelection.getHexCode());
                highlightSelection2.setAllCheckboxesTrue();
            } else if (index < 18) {
                highlightSelection3.setVisible(true);
                highlightSelection3.addItem(highlightSelection.getCaption(), highlightSelection.getText(), highlightSelection.getHexCode());
                highlightSelection3.setAllCheckboxesTrue();
            }
            ++index;
        }
    }

    private String appendReportNotesToReportText(String reportText, String reportNotes) {
        String result = reportText;
        if (reportNotes != null)
            result += " " + "<b>NOTES:</b> " + reportNotes;
        return result;
    }

    private void setReportHeader(Report report) {
        accessionNumber.setValue(report.getAccessionNumber());
        reportDate.setValue(report.getReportDate() != null ? report.getReportDate().format(Formatting.dateFormatter) : "");
        patientRecordNumber.setValue(report.getPatient().getPatientRecordNumber());
        patientName.setValue(report.getPatient().getPatientName());
        patientDob.setValue(report.getPatient().getPatientDob() != null ? report.getPatient().getPatientDob().format(Formatting.dateFormatter) : "");
        patientSex.setValue(report.getPatient().getPatientSex() != null ? report.getPatient().getPatientSex().toString() : "");
        patientRecordNumber.setValue(report.getPatient().getPatientRecordNumber());
        if (tabType.equals(CODING)) {
            icdCodes.setValue(report.getIcdCodes() != null ? report.getIcdCodesAsString() : report.getPossibleIcdCodes() != null ? report.getPossibleIcdCodesAsString() : "");
            cptCodes.setValue(report.getCptCodes() != null ? report.getCptCodesAsString() : "");
        }
    }
}
