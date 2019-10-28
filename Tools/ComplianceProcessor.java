package com.mst.sentenceprocessing;

import com.mst.metadataProviders.DiscreteDataCustomFieldNames;
import com.mst.model.SentenceQuery.SentenceQueryEdgeResult;
import com.mst.model.businessRule.BusinessRule;
import com.mst.model.businessRule.BusinessRule.*;
import com.mst.model.businessRule.Compliance;
import com.mst.model.businessRule.Compliance.*;
import com.mst.model.compliance.ComplianceResult;
import com.mst.model.compliance.FollowupDescriptor;
import com.mst.model.compliance.FollowupRecommendation;
import com.mst.model.discrete.*;
import com.mst.model.sentenceProcessing.SentenceDb;
import com.mst.model.sentenceProcessing.TokenRelationship;
import com.mst.model.metadataTypes.ComplianceBucket.BucketType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mst.model.businessRule.BusinessRule.LogicalOperator.*;
import static com.mst.model.metadataTypes.EdgeNames.*;
import static com.mst.model.metadataTypes.FollowupDescriptor.*;
import static com.mst.model.metadataTypes.UnitOfMeasure.*;
import static com.mst.model.metadataTypes.ComplianceBucket.BucketType.*;


public class ComplianceProcessor {

    public ComplianceResult process(DiscreteData discreteData, SentenceDb queryResult, List<SentenceQueryEdgeResult> queryResultEdgeResults, List<SentenceDb> sentences, BusinessRule businessRule, boolean setFollowupRecommendation) {
        ComplianceResult result = new ComplianceResult();
        List<TokenRelationship> queryResultTokenRelationships = queryResult.getTokenRelationships();
        List<TokenRelationship> allTokenRelationships = new ArrayList<>();
        for (SentenceDb sentence : sentences)
            allTokenRelationships.addAll(sentence.getTokenRelationships());
        List<BusinessRule> rules = businessRule.getRules();
        for (BusinessRule baseRule : rules) {
            Compliance rule = (Compliance) baseRule;
            Map<String, List<String>> edgesToMatch = rule.getEdgesToMatch();
            if (areEdgesToMatchFound(queryResultTokenRelationships, edgesToMatch)) {
                List<Bucket> buckets = rule.getBuckets();
                boolean isCompliant = false;
                for (Bucket bucket : buckets) {
                    if (discreteDataMatches(discreteData, bucket) && sizeMatches(queryResultTokenRelationships, queryResultEdgeResults, bucket)) {
                        BucketType bucketType = bucket.getBucketType();
                        if (!isCompliant || bucketType == COMPLIANCE)
                            isCompliant = isCompliant(allTokenRelationships, bucket);
                        else
                            isCompliant = false;
                        result.addBucketCompliance(bucket.getBucketName(), isCompliant);
                        if (setFollowupRecommendation)
                            result.setFollowupRecommendation(bucket.getFollowupRecommendation());
                    }
                }
                break;
            }
        }
        return result;
    }

    private boolean isCompliant(List<TokenRelationship> tokenRelationships, Bucket bucket) {
        FollowupRecommendation followup = bucket.getFollowupRecommendation();
        boolean processingOr = false;
        boolean foundOr = false;
        List<FollowupDescriptor> descriptors = followup.getFollowupDescriptors();
        if (descriptors != null)
            for (FollowupDescriptor followupDescriptor : descriptors) {
                LogicalOperator logicalOperator = followupDescriptor.getLogicalOperator();
                String descriptor = followupDescriptor.getDescriptor().toLowerCase();
                if (doTokenRelationshipsContainDescriptor(tokenRelationships, descriptor) && OR.equals(logicalOperator))
                    foundOr = true;
                if (!doTokenRelationshipsContainDescriptor(tokenRelationships, descriptor) && ((logicalOperator == null && !processingOr) || (!OR.equals(logicalOperator) && processingOr && !foundOr)))
                    return false;
                processingOr = logicalOperator == OR;
                if (!processingOr)
                    foundOr = false;
            }
        int followupTime = followup.getTime();
        boolean ongoing = followup.isOngoing(); // NOTE: No TokenRelationship exists for frequency of an event
        if (followupTime > 0) {
            String followupUnitOfMeasure = followup.getUnitOfMeasure();
            for (TokenRelationship tokenRelationship : tokenRelationships) {
                String edgeName = tokenRelationship.getEdgeName();
                String fromToken = tokenRelationship.getFromToken().getToken().toLowerCase();
                String toToken = tokenRelationship.getToToken().getToken().toLowerCase();
                switch (edgeName) {
                    case time:
                        int time = 0;
                        try {
                            time = Integer.parseInt(tokenRelationship.getFromToken().getToken());
                        } catch (Exception e) {
                            if (followupTime == 6 && followupUnitOfMeasure.equals(MONTHS)) {
                                if (fromToken.equals("six") && (toToken.equals("months") || toToken.equals("month")))
                                    return true;
                            } else if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS)) {
                                if (fromToken.equals("twelve") && (toToken.equals("months") || toToken.equals("month")))
                                    return true;
                                else if (fromToken.equals("every") && toToken.equals("year"))
                                    return true;
                            } else if (followupTime == 3 && followupUnitOfMeasure.equals(YEARS)) {
                                if (fromToken.equals("three") && (toToken.equals("year") || toToken.equals("years")))
                                    return true;
                            } else if (followupTime == 5 && followupUnitOfMeasure.equals(YEARS)) {
                                if (fromToken.equals("five") && (toToken.equals("year") || toToken.equals("years")))
                                    return true;
                            }
                        }
                        String unitOfMeasure = tokenRelationship.getToToken().getToken();
                        if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS) && time == 1 && unitOfMeasure.equals(YEAR))
                            return true;
                        else if (followupTime == 1 && followupUnitOfMeasure.equals(YEAR) && time == 12 && unitOfMeasure.equals(MONTHS))
                            return true;
                        else if (followupTime == time && followupUnitOfMeasure.equals(unitOfMeasure))
                            return true;
                        break;
                    case "procedure timing":
                        if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS) && fromToken.equals("annual"))
                            return true;
                        else if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS) && fromToken.equals("annually"))
                            return true;
                        else if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS) && fromToken.equals("yearly"))
                            return true;
                        else if (followupTime == 1 && followupUnitOfMeasure.equals(YEAR) && fromToken.equals("annual"))
                            return true;
                        else if (followupTime == 1 && followupUnitOfMeasure.equals(YEAR) && fromToken.equals("annually"))
                            return true;
                        else if (followupTime == 1 && followupUnitOfMeasure.equals(YEAR) && fromToken.equals("yearly"))
                            return true;
                        break;
                    case suppcare:
                        if (followupTime == 12 && followupUnitOfMeasure.equals(MONTHS)
                                && (fromToken.equals("annual") || fromToken.equals("yearly"))
                                && (toToken.equals("follow-up") || toToken.equals("intervals") || toToken.equals("surveillance")))
                            return true;
                        break;
                }
            }
            return false;
        }
        return true;
    }

    private boolean doTokenRelationshipsContainDescriptor(List<TokenRelationship> tokenRelationships, String descriptor) {
        for (TokenRelationship tokenRelationship : tokenRelationships) {
            String fromToken = tokenRelationship.getFromToken().getToken().toLowerCase();
            String toToken = tokenRelationship.getToToken().getToken().toLowerCase();
            if (descriptor.equals(NO_FOLLOWUP) && tokenRelationship.getEdgeName().equals(existenceNo)
                    && (fromToken.equals("no") || fromToken.equals("not") || fromToken.equals("ovarian") || fromToken.equals("this") || fromToken.equals("imaging") || fromToken.equals("there"))
                    && (toToken.equals("follow") || toToken.equals("followup") || toToken.equals("follow-up") || toToken.equals("necessary") || toToken.equals("dedicated") || toToken.equals("indicated") || toToken.equals("require") || toToken.equals("imaging") || toToken.equals("reevaluated") || toToken.equals("radiographic") || toToken.equals("specific") || toToken.equals("us") || toToken.equals("felt") || toToken.equals("mandatory") || toToken.equals("meet") || toToken.equals("recommended") || toToken.equals("routine") || toToken.equals("warrant") || toToken.equals("radiology") || toToken.equals("already") || toToken.equals("nodules")))
                return true;
            else if (descriptor.equals(NO_FOLLOWUP) && tokenRelationship.getEdgeName().equals(existenceNo)
                    && (toToken.equals("no") || toToken.equals("not"))
                    && (fromToken.equals("follow") || fromToken.equals("followup") || fromToken.equals("follow-up")))
                return true;
            else if (descriptor.equals(NO_FOLLOWUP) && tokenRelationship.getEdgeName().equals(existenceNo)
                    && fromToken.equals("no") && (toToken.equals("additional") || toToken.equals("further")))
                return true;
            else if (descriptor.equals(NO_FOLLOWUP) && tokenRelationship.getEdgeName().equals(existenceNo)
                    && fromToken.equals("not") && toToken.equals("requiring"))
                return true;
            else if (descriptor.equals(NO_FOLLOWUP) && tokenRelationship.getEdgeName().equals(negation)
                    && toToken.equals("imaging"))
                return true;
            else if (fromToken.equals(descriptor) || toToken.equals(descriptor))
                return true;
            else if (descriptor.equals(fromToken + " " + toToken))
                return true;
            else if (descriptor.equals(ULTRASOUND) && ((fromToken.equals("imaging") || toToken.equals("imaging"))
                    || (fromToken.equals("us") || toToken.equals("us"))))
                return true;
            else if (descriptor.equals(VASCULAR_CONSULTATION) && fromToken.equals("vascular")
                    && (toToken.equals("consultation") || toToken.equals("consult") || toToken.equals("surgeon") || toToken.equals("surgery")))
                return true;
            else if (descriptor.equals(SURGICAL_EVALUATION)
                    && (fromToken.equals("surgeon") || fromToken.equals("surgeons"))
                    && (toToken.equals("consultation") || toToken.equals("consultations")
                    || toToken.equals("evaluations") || toToken.equals("consult")))
                return true;
        }
        return false;
    }

    private boolean discreteDataMatches(DiscreteData discreteData, Bucket bucket) {
        if (discreteData == null)
            return false;
        int minAge = bucket.getMinAge();
        int maxAge = bucket.getMaxAge();
        if (minAge != 0 || maxAge != 0) {
            int age = discreteData.getPatientAge();
            if (age < minAge || (maxAge > 0 && age > maxAge))
                return false;
        }
        String menopausalStatus = bucket.getMenopausalStatus();
        if (menopausalStatus != null) {
            List<DiscreteDataCustomField> customFields = discreteData.getCustomFields();
            Map<String, DiscreteDataCustomField> customFieldsByName = customFields.stream().collect(Collectors.toMap(DiscreteDataCustomField::getFieldName, x -> x));
            if (!customFieldsByName.containsKey(DiscreteDataCustomFieldNames.menopausalStatus))
                return false;
            return menopausalStatus.equals(customFieldsByName.get(DiscreteDataCustomFieldNames.menopausalStatus).getValue());
        }
        return true;
    }

    private boolean sizeMatches(List<TokenRelationship> tokenRelationships, List<SentenceQueryEdgeResult> queryResultEdgeResults, Bucket bucket) {
        double minSize = bucket.getMinSize();
        double maxSize = bucket.getMaxSize();
        if (minSize > 0 || maxSize > 0) {
            for (TokenRelationship tokenRelationship : tokenRelationships)
                if (tokenRelationship.getEdgeName().equals(measurement)) {
                    double size = Double.parseDouble(tokenRelationship.getFromToken().getToken());
                    return size >= minSize && size <= maxSize;
                }
            for (SentenceQueryEdgeResult edgeResult : queryResultEdgeResults) {
                if (edgeResult.getEdgeName().equals(measurement)) {
                    double size = Double.parseDouble(edgeResult.getMatchedValue());
                    return size >= minSize && size <= maxSize;
                }
            }
            return false;
        }
        return true;
    }

    private boolean isEdgeToMatchFound(List<TokenRelationship> tokenRelationships, String edgeToMatch, List<String> edgeValues) {
        for (TokenRelationship tokenRelationship : tokenRelationships) {
            if (edgeToMatch.equals(tokenRelationship.getEdgeName()))
                if (edgeValues == null || (edgeValues.contains(tokenRelationship.getFromToken().getToken()) || edgeValues.contains(tokenRelationship.getToToken().getToken())))
                    return true;
        }
        return false;
    }

    private boolean areEdgesToMatchFound(List<TokenRelationship> tokenRelationships, Map<String, List<String>> edgesToMatch) {
        if (tokenRelationships == null || tokenRelationships.isEmpty())
            return false;
        for (Map.Entry<String, List<String>> entry : edgesToMatch.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty() && !isEdgeToMatchFound(tokenRelationships, entry.getKey(), values))
                return false;
            else if (!isEdgeToMatchFound(tokenRelationships, entry.getKey(), null))
                return false;
        }
        return true;
    }
}