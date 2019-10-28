package com.mst.model.compliance;

import com.mst.model.businessRule.BusinessRule.LogicalOperator;

public class FollowupDescriptor {
    private String descriptor;
    private LogicalOperator logicalOperator;

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }
}
