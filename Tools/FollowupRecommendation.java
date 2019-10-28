package com.mst.model.compliance;


import java.util.List;

public class FollowupRecommendation {
    private int time;
    private String unitOfMeasure;
    private boolean ongoing;
    private List<FollowupDescriptor> followupDescriptors;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public boolean isOngoing() {
        return ongoing;
    }

    public void setOngoing(boolean ongoing) {
        this.ongoing = ongoing;
    }

    public List<FollowupDescriptor> getFollowupDescriptors() {
        return followupDescriptors;
    }

    public void setFollowupDescriptors(List<FollowupDescriptor> followupDescriptors) {
        this.followupDescriptors = followupDescriptors;
    }

    public String getFollupFrequency() {
        return String.valueOf(time) + " " + unitOfMeasure;
    }
}
