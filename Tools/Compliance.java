package com.mst.model.compliance;

public class Compliance {
    private FollowupRecommendation followupRecommendation;
    private String bucketNames;
    private String areCompliant;

    public FollowupRecommendation getFollowupRecommendation() {
        return followupRecommendation;
    }

    public void setFollowupRecommendation(FollowupRecommendation followupRecommendation) {
        this.followupRecommendation = followupRecommendation;
    }

    public String getBucketNames() {
        return bucketNames;
    }

    public void setBucketNames(String bucketNames) {
        this.bucketNames = bucketNames;
    }

    public String getAreCompliant() {
        return areCompliant;
    }

    public void setAreCompliant(String areCompliant) {
        this.areCompliant = areCompliant;
    }
}
