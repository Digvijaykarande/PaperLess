package com.Paperless.paperless.modules.pdf.dto;

import java.util.List;

public class PageManagementRequest {
    private String fileUrl;
    private List<Integer> pageOrder; // 1-based page index array, e.g., [1, 3, 4] to drop page 2

    public PageManagementRequest() {}

    public PageManagementRequest(String fileUrl, List<Integer> pageOrder) {
        this.fileUrl = fileUrl;
        this.pageOrder = pageOrder;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public List<Integer> getPageOrder() {
        return pageOrder;
    }

    public void setPageOrder(List<Integer> pageOrder) {
        this.pageOrder = pageOrder;
    }
}