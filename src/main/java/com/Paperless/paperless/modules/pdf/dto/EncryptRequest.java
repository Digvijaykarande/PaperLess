package com.Paperless.paperless.modules.pdf.dto;

public class EncryptRequest {
    private String fileUrl;
    private String userPassword;
    private String ownerPassword;

    public EncryptRequest() {}

    public EncryptRequest(String fileUrl, String userPassword, String ownerPassword) {
        this.fileUrl = fileUrl;
        this.userPassword = userPassword;
        this.ownerPassword = ownerPassword;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
    }
}