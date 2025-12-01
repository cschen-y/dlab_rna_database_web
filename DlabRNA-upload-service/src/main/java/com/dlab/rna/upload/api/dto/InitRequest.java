package com.dlab.rna.upload.api.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InitRequest {
    @NotBlank
    private String filename;
    @NotNull
    @Min(1)
    private Long size;
    @NotNull
    @Min(1)
    private Integer chunkSize;
    private String sha256;

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}