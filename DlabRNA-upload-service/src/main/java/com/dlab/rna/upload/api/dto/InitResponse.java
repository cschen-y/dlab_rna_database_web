package com.dlab.rna.upload.api.dto;

import java.util.List;

public class InitResponse {
    private String fileId;
    private String objectName;
    private boolean resumed;
    private Integer chunkSize;
    private Integer totalChunks;
    private List<Integer> uploadedIndices;
    private List<Integer> missingIndices;

    public InitResponse() {}
    public InitResponse(String fileId, String objectName) {
        this.fileId = fileId;
        this.objectName = objectName;
    }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public boolean isResumed() { return resumed; }
    public void setResumed(boolean resumed) { this.resumed = resumed; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
    public List<Integer> getUploadedIndices() { return uploadedIndices; }
    public void setUploadedIndices(List<Integer> uploadedIndices) { this.uploadedIndices = uploadedIndices; }
    public List<Integer> getMissingIndices() { return missingIndices; }
    public void setMissingIndices(List<Integer> missingIndices) { this.missingIndices = missingIndices; }
}