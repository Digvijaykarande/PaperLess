package com.Paperless.paperless.modules.pdf.dto;

public class WatermarkRequest {
    private String fileUrl;
    private String watermarkText;

    public WatermarkRequest() {}

    public WatermarkRequest(String fileUrl, String watermarkText) {
        this.fileUrl = fileUrl;
        this.watermarkText = watermarkText;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }
}