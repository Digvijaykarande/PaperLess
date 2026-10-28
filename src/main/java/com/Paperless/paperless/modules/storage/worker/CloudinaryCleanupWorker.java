package com.Paperless.paperless.modules.storage.worker;


import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CloudinaryCleanupWorker {

    @Autowired
    private Cloudinary cloudinary;

    // Runs every 15 minutes (900,000 ms)
    @Scheduled(fixedRate = 900000)
    public void cleanupExpiredPdfs() {
        try {
            // Search for raw assets inside paperless folders
            ApiResponse response = cloudinary.search()
                    .expression("folder:paperless_raw OR folder:paperless_processed")
                    .maxResults(100)
                    .execute();

            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");
            if (resources == null || resources.isEmpty()) return;

            long currentTime = System.currentTimeMillis();
            long oneHourInMillis = 3600 * 1000; // 1 Hour TTL

            for (Map<String, Object> resource : resources) {
                String publicId = (String) resource.get("public_id");
                String createdAtStr = (String) resource.get("created_at");

                long createdTime = java.time.Instant.parse(createdAtStr).toEpochMilli();

                // If file is older than 1 hour, delete it
                if ((currentTime - createdTime) > oneHourInMillis) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
                    System.out.println("[PaperLess Worker] Deleted expired file: " + publicId);
                }
            }
        } catch (Exception e) {
            System.err.println("[PaperLess Worker] Auto-cleanup error: " + e.getMessage());
        }
    }
}