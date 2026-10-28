package com.Paperless.paperless.modules.queue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfJob implements Serializable {
    private String jobId;
    private String action; // MERGE, SPLIT, etc.
    private JobStatus status;
    private List<String> fileUrls;
    private String resultUrl;
    private String errorMessage;
}