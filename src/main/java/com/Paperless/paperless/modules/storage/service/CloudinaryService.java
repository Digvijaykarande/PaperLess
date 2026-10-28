package com.Paperless.paperless.modules.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map<String, Object> generateUploadSignature() {
        long timestamp = System.currentTimeMillis() / 1000L;
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", "paperless_raw");

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("apiKey", cloudinary.config.apiKey);
        response.put("cloudName", cloudinary.config.cloudName);
        response.put("folder", "paperless_raw");

        return response;
    }

    // Overloaded method defaulting to "pdf" for backward compatibility
    public String uploadBytes(byte[] fileData, String folder) throws Exception {
        return uploadBytes(fileData, folder, "pdf");
    }

    // Main upload method with dynamic format support (.pdf, .zip, etc.)
    public String uploadBytes(byte[] fileData, String folder, String format) throws Exception {
        String publicId = "processed_" + System.currentTimeMillis() + "." + format;

        Map uploadResult = cloudinary.uploader().upload(fileData, ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "raw",
                "format", format,
                "public_id", publicId
        ));
        return (String) uploadResult.get("secure_url");
    }
}