package com.Paperless.paperless.modules.pdf.dto;

import java.util.List;

public class ImagesToPdfRequest {
    private List<String> imageUrls;

    public ImagesToPdfRequest() {}

    public ImagesToPdfRequest(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}