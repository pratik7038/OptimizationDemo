package com.pratik.optimizationDemo.performance.model;

import lombok.Data;


@Data
public class MetricAggregate {

    private String itemId;
    private String dimensionId;
    private long passed;
    private long failed;
    private long error;

    public MetricAggregate(String itemId, String dimensionId, long passed, long failed, long error) {
        this.itemId = itemId;
        this.dimensionId = dimensionId;
        this.passed = passed;
        this.failed = failed;
        this.error = error;
    }

    public double getPassRatePercentage() {
        long total = passed + failed;
        if (total == 0) {
            return 0.0;
        }
        return (double) passed / total * 100.0;
    }

    public long getTotal() {
        return passed + failed + error;
    }

    @Override
    public String toString() {
        return String.format(
                "MetricAggregate{itemId='%s', dimensionId='%s', passed=%d, failed=%d, error=%d, passRate=%.1f%%}",
                itemId, dimensionId, passed, failed, error, getPassRatePercentage()
        );
    }
}
