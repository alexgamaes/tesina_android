package com.alexgamaes.tesina;

public interface ProgressInterface {

    public void showProgress(String rowNumber, String checksum, String progress);
    public void addResult(String result);
}
