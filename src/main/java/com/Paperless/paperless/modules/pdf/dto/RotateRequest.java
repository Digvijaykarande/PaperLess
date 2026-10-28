package com.Paperless.paperless.modules.pdf.dto;

public class RotateRequest {
    private String fileUrl;
    private int degrees; // e.g., 90, 180, 270

    public RotateRequest() {}

    public RotateRequest(String fileUrl, int degrees) {
        this.fileUrl = fileUrl;
        this.degrees = degrees;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getDegrees() {
        return degrees;
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }
}