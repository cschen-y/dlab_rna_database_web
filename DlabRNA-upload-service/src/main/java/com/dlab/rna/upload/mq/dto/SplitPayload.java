package com.dlab.rna.upload.mq.dto;

public class SplitPayload {
    private String fileId;
    private String object;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
}