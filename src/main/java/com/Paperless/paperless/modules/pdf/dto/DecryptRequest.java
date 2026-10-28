package com.Paperless.paperless.modules.pdf.dto;

public class DecryptRequest {
    private String fileUrl;
    private String password;

    public DecryptRequest() {}

    public DecryptRequest(String fileUrl, String password) {
        this.fileUrl = fileUrl;
        this.password = password;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}