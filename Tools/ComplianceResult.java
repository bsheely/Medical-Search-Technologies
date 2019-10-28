package com.mst.model.compliance;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComplianceResult {
    private BucketCompliance bucketCompliance;
    private FollowupRecommendation followupRecommendation;

    public static class BucketCompliance {
        private Map<String, Boolean> bucketNamesComplianceResults;

        BucketCompliance(Map<String, Boolean> bucketNamesComplianceResults) {
            this.bucketNamesComplianceResults = bucketNamesComplianceResults;
        }

        Map<String, Boolean> getBucketNamesComplianceResults() {
            return bucketNamesComplianceResults;
        }
    }

    public ComplianceResult() {
        bucketCompliance = new BucketCompliance(new LinkedHashMap<>());
    }

    public Map<String, Boolean> getBucketCompliance() {
        return bucketCompliance.getBucketNamesComplianceResults();
    }

    public String getBucketNames() {
        Map<String, Boolean> complianceResults = bucketCompliance.getBucketNamesComplianceResults();
        StringBuilder bucketName = new StringBuilder();
        Iterator<Map.Entry<String, Boolean>> itr = complianceResults.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Boolean> mapEntry = itr.next();
            bucketName.append(mapEntry.getKey());
            if (itr.hasNext())
                bucketName.append(", ");
        }
        return bucketName.toString();
    }

    public String getIsCompliant() {
        Map<String, Boolean> complianceResults = bucketCompliance.getBucketNamesComplianceResults();
        StringBuilder isCompliant = new StringBuilder();
        Iterator<Map.Entry<String, Boolean>> itr = complianceResults.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Boolean> mapEntry = itr.next();
            isCompliant.append(mapEntry.getValue());
            if (itr.hasNext())
                isCompliant.append(", ");
        }
        return isCompliant.toString();
    }

    public void addBucketCompliance(String bucketName, boolean isCompliant) {
        bucketCompliance.bucketNamesComplianceResults.put(bucketName, isCompliant);
    }

    public FollowupRecommendation getFollowupRecommendation() {
        return followupRecommendation;
    }

    public void setFollowupRecommendation(FollowupRecommendation followupRecommendation) {
        this.followupRecommendation = followupRecommendation;
    }
}
