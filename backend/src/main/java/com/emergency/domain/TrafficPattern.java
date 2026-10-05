package com.emergency.domain;

public enum TrafficPattern {
    NORMAL(1.0, "100% baseline traffic"),
    HIGH(2.0, "~2x elevated load"),
    BURST(3.0, "Short burst of rapid requests"),
    SPIKE(5.0, "Sudden massive traffic spike");

    private final double multiplier;
    private final String description;

    TrafficPattern(double multiplier, String description) {
        this.multiplier = multiplier;
        this.description = description;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getDescription() {
        return description;
    }
}
