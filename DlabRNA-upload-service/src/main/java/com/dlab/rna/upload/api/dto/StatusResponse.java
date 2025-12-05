package com.dlab.rna.upload.api.dto;

import java.util.List;

public class StatusResponse {
    private String state;
    private int doneChunks;
    private int totalChunks;
    private double percent;
    private String errorMessage;
    private List<Integer> uploadedIndices;
    private List<Integer> missingIndices;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getDoneChunks() { return doneChunks; }
    public void setDoneChunks(int doneChunks) { this.doneChunks = doneChunks; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<Integer> getUploadedIndices() { return uploadedIndices; }
    public void setUploadedIndices(List<Integer> uploadedIndices) { this.uploadedIndices = uploadedIndices; }
    public List<Integer> getMissingIndices() { return missingIndices; }
    public void setMissingIndices(List<Integer> missingIndices) { this.missingIndices = missingIndices; }
}