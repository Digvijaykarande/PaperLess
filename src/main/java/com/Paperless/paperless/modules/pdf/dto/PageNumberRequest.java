package com.Paperless.paperless.modules.pdf.dto;

public class PageNumberRequest {
    private String fileUrl;
    private String position; // "BOTTOM_CENTER", "BOTTOM_RIGHT", "TOP_CENTER"
    private String prefix;   // e.g., "Page "

    public PageNumberRequest() {}

    public PageNumberRequest(String fileUrl, String position, String prefix) {
        this.fileUrl = fileUrl;
        this.position = position;
        this.prefix = prefix;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}