package com.Paperless.paperless.modules.pdf.dto;

public class SplitRequest {
    private String fileUrl;
    private int startPage;
    private int endPage;

    // Default Constructor
    public SplitRequest() {}

    // Constructor with fields
    public SplitRequest(String fileUrl, int startPage, int endPage) {
        this.fileUrl = fileUrl;
        this.startPage = startPage;
        this.endPage = endPage;
    }

    // Explicit Getters and Setters
    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }
}