package com.Paperless.paperless.modules.pdf.dto;

public class PdfToDocxRequest {
    private String fileUrl;

    public PdfToDocxRequest() {}

    public PdfToDocxRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}