package com.alexgamaes.tesina;

public class AutomaticTestInfo {
    public int modificationDelay;
    public int numberOfTests;
    public int percentage;
    public long databaseSize;

    public AutomaticTestInfo(int modificationDelay, int numberOfTests, int percentage, long databaseSize) {
        this.modificationDelay = modificationDelay;
        this.numberOfTests = numberOfTests;
        this.percentage = percentage;
        this.databaseSize = databaseSize;
    }

    public AutomaticTestInfo() {}

    @Override
    public String toString() {
        return "AutomaticTestInfo{" +
                "modificationDelay=" + modificationDelay +
                ", numberOfTests=" + numberOfTests +
                ", percentage=" + percentage +
                ", databaseSize=" + databaseSize +
                '}';
    }
}
