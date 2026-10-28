package com.Paperless.paperless.modules.storage.controller;


import com.Paperless.paperless.modules.storage.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/signature")
    public ResponseEntity<Map<String, Object>> getUploadSignature() {
        return ResponseEntity.ok(cloudinaryService.generateUploadSignature());
    }
}
