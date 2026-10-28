package com.Paperless.paperless.modules.pdf.dto;

public class CompressRequest {
    private String fileUrl;
    private String level; // "LOW", "MEDIUM", "HIGH"

    public CompressRequest() {}

    public CompressRequest(String fileUrl, String level) {
        this.fileUrl = fileUrl;
        this.level = level;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}