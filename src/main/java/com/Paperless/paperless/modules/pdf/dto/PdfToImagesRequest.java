package com.Paperless.paperless.modules.pdf.dto;

public class PdfToImagesRequest {
    private String fileUrl;

    public PdfToImagesRequest() {}

    public PdfToImagesRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}