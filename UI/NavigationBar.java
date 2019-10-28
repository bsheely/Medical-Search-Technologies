package com.mst.machineLearningUi.view;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.util.ArrayList;
import java.util.List;

class NavigationBar extends HorizontalLayout {
    public enum IncrementNavigation {
        PREVIOUS {public String toString() { return "<< Prev"; }},
        NEXT {public String toString() { return "Next >>"; }}
    }
    private int reportCount;
    private int current;
    private int start;
    private int stop;
    private ReportView reportView;
    private List<Button> buttons;
    private final Button previousNavigation;
    private final Button nextNavigation;
    private HorizontalLayout layout;

    NavigationBar(ReportView reportView) {
        this.reportView = reportView;
        start = 1;
        stop = 10;
        current = 1;
        setWidth(50, Unit.EM);
        layout = new HorizontalLayout();
        previousNavigation = new Button(IncrementNavigation.PREVIOUS.toString());
        previousNavigation.addStyleNames(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_TINY, "v-slot-v-button", "v-slot-v-button-v-button-caption-normal");
        previousNavigation.addClickListener(this::handleButtonClick);
        previousNavigation.setVisible(false);
        nextNavigation = new Button(IncrementNavigation.NEXT.toString());
        nextNavigation.addStyleNames(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_TINY, "v-slot-v-button", "v-slot-v-button-v-button-caption-normal");
        nextNavigation.addClickListener(this::handleButtonClick);
        addComponent(layout);
        setComponentAlignment(layout, Alignment.BOTTOM_CENTER);
    }

    void setReportCount(int reportCount) {
        this.reportCount = reportCount;
        buttons = new ArrayList<>();
        layout.removeAllComponents();
        layout.addComponent(previousNavigation);
        previousNavigation.setVisible(false);
        for (int i = 1; i <= reportCount; ++i) {
            Button button = new Button(String.valueOf(i));
            button.addStyleNames(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_TINY, "v-slot-v-button");
            button.addClickListener(this::handleButtonClick);
            layout.addComponent(button);
            buttons.add(button);
            if (i == 1)
                button.addStyleName("v-slot-v-button-v-button-caption-selected");
            else
                button.addStyleName("v-slot-v-button-v-button-caption-normal");
            if (i > stop)
                button.setVisible(false);
        }
        layout.addComponent(nextNavigation);
        if (reportCount <= stop)
            nextNavigation.setVisible(false);
    }

    private void handleButtonClick(Button.ClickEvent event) {
        Button previouslySelected = buttons.get(current - 1);
        previouslySelected.removeStyleName("v-slot-v-button-v-button-caption-selected");
        previouslySelected.addStyleName("v-slot-v-button-v-button-caption-normal");
        String caption = event.getButton().getCaption();
        if (caption.equals(IncrementNavigation.PREVIOUS.toString()) && start > 1) {
            start -= 10;
            stop -= 10;
            current = start;
            processNavigationClick();
        }
        else if (caption.equals(IncrementNavigation.NEXT.toString()) && stop < reportCount) {
            start += 10;
            stop += 10;
            current = start;
            processNavigationClick();
        }
        else if (!caption.equals(IncrementNavigation.PREVIOUS.toString()) && !caption.equals(IncrementNavigation.NEXT.toString())) {
            current = Integer.parseInt(caption);
            Button selected = buttons.get(current - 1);
            selected.removeStyleName("v-slot-v-button-v-button-caption-normal");
            selected.addStyleName("v-slot-v-button-v-button-caption-selected");
        }
        if (start != 1)
            previousNavigation.setVisible(true);
        else
            previousNavigation.setVisible(false);
        if (reportCount > stop)
            nextNavigation.setVisible(true);
        else
            nextNavigation.setVisible(false);
        reportView.displayReportAtIndex(current - 1);
    }

    private void processNavigationClick() {
        for (int i = 1; i <= reportCount; ++i) {
            Button button = buttons.get(i - 1);
            if (i >= start && i <= stop)
                button.setVisible(true);
            else
                button.setVisible(false);
            if (i == current) {
                button.removeStyleName("v-slot-v-button-v-button-caption-normal");
                button.addStyleName("v-slot-v-button-v-button-caption-selected");
            }
        }
    }
}
