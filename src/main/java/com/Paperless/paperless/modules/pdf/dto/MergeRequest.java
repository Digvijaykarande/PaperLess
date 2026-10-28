package com.Paperless.paperless.modules.pdf.dto;


import lombok.Data;
import java.util.List;

@Data
public class MergeRequest {
    private List<String> fileUrls;

	
}