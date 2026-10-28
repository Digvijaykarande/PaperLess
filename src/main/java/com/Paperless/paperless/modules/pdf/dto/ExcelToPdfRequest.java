package com.Paperless.paperless.modules.pdf.dto;

public class ExcelToPdfRequest {
    private String fileUrl;

    public ExcelToPdfRequest() {}

    public ExcelToPdfRequest(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}