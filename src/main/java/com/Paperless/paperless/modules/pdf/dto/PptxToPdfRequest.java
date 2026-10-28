package com.Paperless.paperless.modules.pdf.dto;

public class PptxToPdfRequest {
    private String fileUrl;

    public PptxToPdfRequest() {}

    public PptxToPdfRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}