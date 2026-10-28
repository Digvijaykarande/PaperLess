package com.Paperless.paperless.modules.pdf.dto;

public class PdfToExcelRequest {
    private String fileUrl;

    public PdfToExcelRequest() {}

    public PdfToExcelRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}