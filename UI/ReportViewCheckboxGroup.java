package com.mst.machineLearningUi.view;

import com.mst.machineLearningUi.model.HighlightSelection;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.List;

class ReportViewCheckboxGroup extends HorizontalLayout {
    private List<ReportViewCheckboxItem> items;
    private ReportView reportView;

    class ReportViewCheckboxItem extends HorizontalLayout {
        private final String caption;
        private final String text;
        private final CheckBox checkBox;
        private final Label label;
        private final Label colorBar;
        private final String hexCode;

        ReportViewCheckboxItem(String caption, String text, String hexCode) {
            this.caption = caption;
            this.text = text;
            this.hexCode = hexCode;
            checkBox = new CheckBox();
            checkBox.addValueChangeListener(event -> handleCheckboxChangeValue());
            label = new Label(caption);
            colorBar = new Label();
            colorBar.setContentMode(ContentMode.HTML);
            colorBar.setHeight(12, Sizeable.Unit.PIXELS);
            colorBar.setValue("<span style=\"background-color:"+ hexCode + "\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>");
            addComponents(checkBox, label, colorBar);
        }

        @Override
        public String getCaption() {
            return caption;
        }

        private String getText() {
            return text;
        }

        private CheckBox getCheckBox() {
            return checkBox;
        }

        private String getHexCode() {
            return hexCode;
        }

        void handleCheckboxChangeValue() {
            List<HighlightSelection> highlightSelections = new ArrayList<>();
            List<ReportViewCheckboxItem> selectedItems = getSelectedItems();
            for (ReportViewCheckboxItem item : selectedItems)
                highlightSelections.add(new HighlightSelection(item.getCaption(), item.getText(), item.getHexCode()));
            getReportView().highlightReport(highlightSelections);
        }
    }

    ReportViewCheckboxGroup(ReportView reportView) {
        this.reportView = reportView;
        items = new ArrayList<>();
    }

    int getSize() {
        return items.size();
    }

    void setAllCheckboxesTrue() {
        for (ReportViewCheckboxItem item : items)
            item.getCheckBox().setValue(true);
    }

    void clear() {
        items.clear();
        removeAllComponents();
        setVisible(false);
    }

    void addItem(String caption, String text, String hexCode) {
        ReportViewCheckboxItem item = new ReportViewCheckboxItem(caption, text, hexCode);
        items.add(item);
        addComponent(item);
    }

    private ReportView getReportView() {
        return reportView;
    }

    private List<ReportViewCheckboxItem> getSelectedItems() {
        List<ReportViewCheckboxItem> selectedItems = new ArrayList<>();
        for (ReportViewCheckboxItem item : items)
            if (item.getCheckBox().getValue())
                selectedItems.add(item);
        return selectedItems;
    }
}