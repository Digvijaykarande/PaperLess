package com.Paperless.paperless.modules.pdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDResources;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

@Service
public class PdfProcessingService {

	public byte[] mergePdfFiles(List<String> fileUrls) throws Exception {
		PDFMergerUtility merger = new PDFMergerUtility();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		for (String urlString : fileUrls) {
			try (InputStream inputStream = new URL(urlString).openStream()) {
				// Apache PDFBox 3.x requires RandomAccessReadBuffer instead of raw InputStream
				byte[] bytes = inputStream.readAllBytes();
				merger.addSource(new RandomAccessReadBuffer(bytes));
			}
		}

		merger.setDestinationStream(outputStream);
		merger.mergeDocuments(null);

		return outputStream.toByteArray();
	}

	public byte[] splitPdfFile(String fileUrl, int startPage, int endPage) throws Exception {
		try (InputStream inputStream = new URL(fileUrl).openStream();
				PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

			Splitter splitter = new Splitter();
			splitter.setStartPage(startPage);
			splitter.setEndPage(endPage);
			splitter.setSplitAtPage(endPage - startPage + 1);

			List<PDDocument> splitDocuments = splitter.split(document);

			if (splitDocuments.isEmpty()) {
				throw new IllegalArgumentException("Invalid page range specified.");
			}

			PDDocument resultDoc = splitDocuments.get(0);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			resultDoc.save(outputStream);

			// Clean up resources
			for (PDDocument doc : splitDocuments) {
				doc.close();
			}

			return outputStream.toByteArray();
		}
	}

	public byte[] watermarkPdfFile(String fileUrl, String watermarkText) throws Exception {
		try (InputStream inputStream = new URL(fileUrl).openStream();
				PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

			PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

			for (PDPage page : document.getPages()) {
				try (PDPageContentStream cs = new PDPageContentStream(document, page,
						PDPageContentStream.AppendMode.APPEND, true, true)) {

					// Set semi-transparent opacity (30%)
					PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
					gs.setNonStrokingAlphaConstant(0.3f);
					cs.setGraphicsStateParameters(gs);

					cs.setFont(font, 50);

					// Fixed: PDFBox 3.x requires RGB values normalized between 0.0f and 1.0f
					cs.setNonStrokingColor(0.588f, 0.588f, 0.588f); // Light Gray (150/255)

					// Calculate center position
					float pageWidth = page.getMediaBox().getWidth();
					float pageHeight = page.getMediaBox().getHeight();
					float textWidth = font.getStringWidth(watermarkText) / 1000 * 50;

					cs.beginText();
					// Rotate text 45 degrees diagonally across page center
					cs.setTextMatrix(
							Matrix.getRotateInstance(Math.toRadians(45), (pageWidth - textWidth) / 2, pageHeight / 2));
					cs.showText(watermarkText);
					cs.endText();
				}
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			document.save(outputStream);
			return outputStream.toByteArray();
		}
	}

	public byte[] compressPdfFile(String fileUrl, String level) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument originalDoc = Loader.loadPDF(inputStream.readAllBytes())) {

	        // Set DPI resolution and JPEG quality based on user choice
	        float dpi = 150f;
	        float imageQuality = 0.6f;

	        if ("HIGH".equalsIgnoreCase(level)) {
	            dpi = 96f;          // Extreme Compression: Low DPI & high JPEG compression
	            imageQuality = 0.35f;
	        } else if ("LOW".equalsIgnoreCase(level)) {
	            dpi = 200f;         // Low Compression: High DPI & high image clarity
	            imageQuality = 0.8f;
	        }

	        PDFRenderer pdfRenderer = new PDFRenderer(originalDoc);
	        PDDocument compressedDoc = new PDDocument();

	        for (int pageIndex = 0; pageIndex < originalDoc.getNumberOfPages(); pageIndex++) {
	            // Render original page to image at specified DPI
	            BufferedImage bim = pdfRenderer.renderImageWithDPI(pageIndex, dpi);

	            // Create compressed JPEG image object
	            PDImageXObject pdImage = JPEGFactory.createFromImage(compressedDoc, bim, imageQuality);

	            // Create new page matching original page dimensions
	            PDPage originalPage = originalDoc.getPage(pageIndex);
	            PDRectangle mediaBox = originalPage.getMediaBox();
	            PDPage newPage = new PDPage(mediaBox);
	            compressedDoc.addPage(newPage);

	            // Draw compressed image across the page
	            try (PDPageContentStream contentStream = new PDPageContentStream(compressedDoc, newPage)) {
	                contentStream.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
	            }
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        compressedDoc.save(outputStream);
	        compressedDoc.close();

	        return outputStream.toByteArray();
	    }
	}
	
	public byte[] convertImagesToPdf(List<String> imageUrls) throws Exception {
	    try (PDDocument document = new PDDocument()) {
	        for (String urlString : imageUrls) {
	            try (InputStream inputStream = new URL(urlString).openStream()) {
	                BufferedImage bufferedImage = ImageIO.read(inputStream);
	                if (bufferedImage == null) continue;

	                // Create page matching image dimensions
	                PDRectangle pageRect = new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight());
	                PDPage page = new PDPage(pageRect);
	                document.addPage(page);

	                // Convert BufferedImage to PDFXObject
	                PDImageXObject pdImage = JPEGFactory.createFromImage(document, bufferedImage, 0.85f);

	                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
	                    contentStream.drawImage(pdImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
	                }
	            }
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        document.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}
	
	public byte[] convertPdfToImagesZip(String fileUrl) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

	        PDFRenderer pdfRenderer = new PDFRenderer(document);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();

	        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
	            for (int page = 0; page < document.getNumberOfPages(); page++) {
	                // Render page as high-quality image (300 DPI)
	                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 150);

	                ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
	                ImageIO.write(bim, "png", imgBaos);

	                // Add entry into Zip stream
	                ZipEntry entry = new ZipEntry("page_" + (page + 1) + ".png");
	                zos.putNextEntry(entry);
	                zos.write(imgBaos.toByteArray());
	                zos.closeEntry();
	            }
	        }

	        return baos.toByteArray();
	    }
	}
	
	public byte[] encryptPdfFile(String fileUrl, String userPassword, String ownerPassword) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

	        // Define access permissions (allow printing and reading)
	        AccessPermission ap = new AccessPermission();
	        ap.setCanPrint(true);
	        ap.setCanExtractContent(true);

	        // Fallback owner password to user password if not explicitly supplied
	        String ownerPass = (ownerPassword != null && !ownerPassword.isBlank()) ? ownerPassword : userPassword;

	        // Apply 128-bit AES encryption
	        StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPass, userPassword, ap);
	        spp.setEncryptionKeyLength(128);
	        spp.setPermissions(ap);

	        document.protect(spp);

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        document.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}

	public byte[] decryptPdfFile(String fileUrl, String password) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         // Pass the password directly into Loader.loadPDF
	         PDDocument document = Loader.loadPDF(inputStream.readAllBytes(), password)) {

	        // Remove security restrictions
	        document.setAllSecurityToBeRemoved(true);

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        document.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}
	
	
	public byte[] rotatePdfPages(String fileUrl, int degrees) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

	        for (PDPage page : document.getPages()) {
	            page.setRotation(page.getRotation() + degrees);
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        document.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}

	public byte[] reorderOrDeletePages(String fileUrl, List<Integer> pageOrder) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument originalDoc = Loader.loadPDF(inputStream.readAllBytes());
	         PDDocument newDoc = new PDDocument()) {

	        int totalPages = originalDoc.getNumberOfPages();

	        for (int pageNum : pageOrder) {
	            // Convert 1-based index to 0-based index
	            int pageIndex = pageNum - 1;
	            if (pageIndex >= 0 && pageIndex < totalPages) {
	                newDoc.importPage(originalDoc.getPage(pageIndex));
	            }
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        newDoc.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}
	
	public byte[] addPageNumbers(String fileUrl, String position, String prefix) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

	        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	        int totalPages = document.getNumberOfPages();
	        String textPrefix = (prefix != null && !prefix.isBlank()) ? prefix : "Page ";

	        for (int i = 0; i < totalPages; i++) {
	            PDPage page = document.getPage(i);
	            String pageText = textPrefix + (i + 1) + " of " + totalPages;

	            try (PDPageContentStream cs = new PDPageContentStream(
	                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

	                cs.setFont(font, 10);
	                cs.setNonStrokingColor(0.3f, 0.3f, 0.3f); // Dark Gray

	                float stringWidth = font.getStringWidth(pageText) / 1000 * 10;
	                float pageWidth = page.getMediaBox().getWidth();
	                float pageHeight = page.getMediaBox().getHeight();

	                float x = (pageWidth - stringWidth) / 2; // Default: BOTTOM_CENTER
	                float y = 30; // Bottom margin

	                if ("BOTTOM_RIGHT".equalsIgnoreCase(position)) {
	                    x = pageWidth - stringWidth - 40;
	                } else if ("TOP_CENTER".equalsIgnoreCase(position)) {
	                    y = pageHeight - 30;
	                }

	                cs.beginText();
	                cs.newLineAtOffset(x, y);
	                cs.showText(pageText);
	                cs.endText();
	            }
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        document.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}
	
	// Max 5 MB .docx input (change to fit your plan)
	private static final int MAX_DOCX_BYTES = 5 * 1024 * 1024;

	// Only 2 conversions can run at the same time (fair queue)
	private static final Semaphore DOCX_LIMITER = new Semaphore(2, true);
	
	public byte[] convertDocxToPdf(String fileUrl) throws Exception {
	    // 1. Validate the URL (only your Cloudinary files, https only)
	    if (fileUrl == null || !fileUrl.startsWith("https://res.cloudinary.com/")) {
	        throw new IllegalArgumentException("Invalid file URL.");
	    }

	    // 2. Download with a size cap and timeouts (before taking a conversion slot)
	    byte[] docxBytes = downloadWithLimit(fileUrl, MAX_DOCX_BYTES);

	    // 3. Wait for a free slot; reject if the server is busy
	    if (!DOCX_LIMITER.tryAcquire(30, TimeUnit.SECONDS)) {
	        throw new IllegalStateException("Server is busy. Please try again in a moment.");
	    }

	    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(docxBytes);
	         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

	        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(inputStream);
	        Docx4J.toPDF(wordMLPackage, outputStream);

	        return outputStream.toByteArray();
	    } finally {
	        DOCX_LIMITER.release();
	    }
	}

	private byte[] downloadWithLimit(String urlString, int maxBytes) throws Exception {
	    URLConnection conn = new URL(urlString).openConnection();
	    conn.setConnectTimeout(10_000);
	    conn.setReadTimeout(20_000);

	    try (InputStream in = conn.getInputStream()) {
	        // Read at most maxBytes + 1 so we can detect oversize files without loading them fully
	        byte[] data = in.readNBytes(maxBytes + 1);
	        if (data.length > maxBytes) {
	            throw new IllegalArgumentException(
	                "File too large. Maximum allowed size is " + (maxBytes / (1024 * 1024)) + " MB.");
	        }
	        return data;
	    }
	}
	
}