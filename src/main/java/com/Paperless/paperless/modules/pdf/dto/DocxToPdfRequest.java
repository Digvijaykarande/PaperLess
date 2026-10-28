package com.Paperless.paperless.modules.pdf.dto;

public class DocxToPdfRequest {
    private String fileUrl;

    public DocxToPdfRequest() {}

    public DocxToPdfRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}