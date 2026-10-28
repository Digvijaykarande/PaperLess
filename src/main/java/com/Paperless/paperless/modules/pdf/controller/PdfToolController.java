package com.Paperless.paperless.modules.pdf.controller;

import com.Paperless.paperless.modules.pdf.dto.CompressRequest;
import com.Paperless.paperless.modules.pdf.dto.DecryptRequest;
import com.Paperless.paperless.modules.pdf.dto.DocxToPdfRequest;
import com.Paperless.paperless.modules.pdf.dto.ImagesToPdfRequest;
import com.Paperless.paperless.modules.pdf.dto.MergeRequest;
import com.Paperless.paperless.modules.pdf.dto.PageManagementRequest;
import com.Paperless.paperless.modules.pdf.dto.PageNumberRequest;
import com.Paperless.paperless.modules.pdf.dto.PdfToDocxRequest;
import com.Paperless.paperless.modules.pdf.dto.PdfToExcelRequest;
import com.Paperless.paperless.modules.pdf.dto.PdfToImagesRequest;
import com.Paperless.paperless.modules.pdf.dto.PptxToPdfRequest;
import com.Paperless.paperless.modules.pdf.dto.RotateRequest;
import com.Paperless.paperless.modules.pdf.dto.SplitRequest;
import com.Paperless.paperless.modules.pdf.dto.WatermarkRequest;
import com.Paperless.paperless.modules.pdf.dto.EncryptRequest;
import com.Paperless.paperless.modules.pdf.dto.ExcelToPdfRequest;
import com.Paperless.paperless.modules.pdf.service.PdfProcessingService;
import com.Paperless.paperless.modules.storage.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pdf")
public class PdfToolController {

    @Autowired
    private PdfProcessingService pdfProcessingService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/merge")
    public ResponseEntity<Map<String, String>> mergePdfs(@RequestBody MergeRequest request) {
        try {
            byte[] mergedPdfBytes = pdfProcessingService.mergePdfFiles(request.getFileUrls());
            String outputUrl = cloudinaryService.uploadBytes(mergedPdfBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/split")
    public ResponseEntity<Map<String, String>> splitPdf(@RequestBody SplitRequest request) {
        try {
            byte[] splitPdfBytes = pdfProcessingService.splitPdfFile(
                    request.getFileUrl(),
                    request.getStartPage(),
                    request.getEndPage()
            );

            String outputUrl = cloudinaryService.uploadBytes(splitPdfBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/watermark")
    public ResponseEntity<Map<String, String>> watermarkPdf(@RequestBody WatermarkRequest request) {
        try {
            byte[] watermarkedBytes = pdfProcessingService.watermarkPdfFile(
                    request.getFileUrl(),
                    request.getWatermarkText()
            );

            String outputUrl = cloudinaryService.uploadBytes(watermarkedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/compress")
    public ResponseEntity<Map<String, String>> compressPdf(@RequestBody CompressRequest request) {
        try {
            String level = request.getLevel() != null ? request.getLevel() : "MEDIUM";
            byte[] compressedBytes = pdfProcessingService.compressPdfFile(request.getFileUrl(), level);
            String outputUrl = cloudinaryService.uploadBytes(compressedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/images-to-pdf")
    public ResponseEntity<Map<String, String>> convertImagesToPdf(@RequestBody ImagesToPdfRequest request) {
        try {
            byte[] pdfBytes = pdfProcessingService.convertImagesToPdf(request.getImageUrls());
            String outputUrl = cloudinaryService.uploadBytes(pdfBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/pdf-to-images")
    public ResponseEntity<Map<String, String>> convertPdfToImages(@RequestBody PdfToImagesRequest request) {
        try {
            byte[] zipBytes = pdfProcessingService.convertPdfToImagesZip(request.getFileUrl());
            
            // Pass "zip" as the format parameter so Cloudinary names it .zip
            String outputUrl = cloudinaryService.uploadBytes(zipBytes, "paperless_processed", "zip");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/protect")
    public ResponseEntity<Map<String, String>> encryptPdf(@RequestBody EncryptRequest request) {
        try {
            byte[] encryptedBytes = pdfProcessingService.encryptPdfFile(
                    request.getFileUrl(),
                    request.getUserPassword(),
                    request.getOwnerPassword()
            );

            String outputUrl = cloudinaryService.uploadBytes(encryptedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<Map<String, String>> decryptPdf(@RequestBody DecryptRequest request) {
        try {
            byte[] decryptedBytes = pdfProcessingService.decryptPdfFile(
                    request.getFileUrl(),
                    request.getPassword()
            );

            String outputUrl = cloudinaryService.uploadBytes(decryptedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Incorrect password or failed decryption: " + e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }   
    
    
    @PostMapping("/rotate")
    public ResponseEntity<Map<String, String>> rotatePdf(@RequestBody RotateRequest request) {
        try {
            byte[] rotatedBytes = pdfProcessingService.rotatePdfPages(
                    request.getFileUrl(),
                    request.getDegrees()
            );

            String outputUrl = cloudinaryService.uploadBytes(rotatedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/reorder")
    public ResponseEntity<Map<String, String>> reorderPdf(@RequestBody PageManagementRequest request) {
        try {
            byte[] reorderedBytes = pdfProcessingService.reorderOrDeletePages(
                    request.getFileUrl(),
                    request.getPageOrder()
            );

            String outputUrl = cloudinaryService.uploadBytes(reorderedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/add-page-numbers")
    public ResponseEntity<Map<String, String>> addPageNumbers(@RequestBody PageNumberRequest request) {
        try {
            byte[] numberedBytes = pdfProcessingService.addPageNumbers(
                    request.getFileUrl(),
                    request.getPosition(),
                    request.getPrefix()
            );

            String outputUrl = cloudinaryService.uploadBytes(numberedBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/docx-to-pdf")
    public ResponseEntity<Map<String, String>> convertDocxToPdf(@RequestBody DocxToPdfRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            byte[] pdfBytes = pdfProcessingService.convertDocxToPdf(request.getFileUrl());
            String outputUrl = cloudinaryService.uploadBytes(pdfBytes, "paperless_processed");

            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            response.put("status", "FAILED");
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            response.put("error", e.getMessage());
            response.put("status", "BUSY");
            return ResponseEntity.status(503).body(response);

        } catch (Exception e) {
            response.put("error", "Conversion failed: " + e.getMessage());
            response.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/pdf-to-docx")
    public ResponseEntity<Map<String, String>> convertPdfToDocx(@RequestBody PdfToDocxRequest request) {
        try {
            byte[] docxBytes = pdfProcessingService.convertPdfToDocx(request.getFileUrl());
            
            // Pass "docx" format so Cloudinary stores and serves it as a Word document
            String outputUrl = cloudinaryService.uploadBytes(docxBytes, "paperless_processed", "docx");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    
    @PostMapping("/excel-to-pdf")
    public ResponseEntity<Map<String, String>> convertExcelToPdf(@RequestBody ExcelToPdfRequest request) {
        try {
            byte[] pdfBytes = pdfProcessingService.convertExcelToPdf(request.getFileUrl());
            String outputUrl = cloudinaryService.uploadBytes(pdfBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    
    @PostMapping("/pdf-to-excel")
    public ResponseEntity<Map<String, String>> convertPdfToExcel(@RequestBody PdfToExcelRequest request) {
        try {
            byte[] excelBytes = pdfProcessingService.convertPdfToExcel(request.getFileUrl());
            String outputUrl = cloudinaryService.uploadBytes(excelBytes, "paperless_processed", "xlsx");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/pptx-to-pdf")
    public ResponseEntity<Map<String, String>> convertPptxToPdf(@RequestBody PptxToPdfRequest request) {
        try {
            byte[] pdfBytes = pdfProcessingService.convertPptxToPdf(request.getFileUrl());
            String outputUrl = cloudinaryService.uploadBytes(pdfBytes, "paperless_processed");

            Map<String, String> response = new HashMap<>();
            response.put("downloadUrl", outputUrl);
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "FAILED");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
}