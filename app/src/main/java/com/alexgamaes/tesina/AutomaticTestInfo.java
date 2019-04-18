package com.alexgamaes.tesina;

public class AutomaticTestInfo {
    public int modificationDelay;
    public int numberOfTests;
    public int percentage;

    public AutomaticTestInfo(int modificationDelay, int numberOfTests, int percentage) {
        this.modificationDelay = modificationDelay;
        this.numberOfTests = numberOfTests;
        this.percentage = percentage;
    }
}
