package com.Paperless.paperless.modules.pdf.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

@Service
public class PdfProcessingService {

	public byte[] mergePdfFiles(List<String> fileUrls) throws Exception {
		PDFMergerUtility merger = new PDFMergerUtility();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		for (String urlString : fileUrls) {
			try (InputStream inputStream = new URL(urlString).openStream()) {
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

					PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
					gs.setNonStrokingAlphaConstant(0.3f);
					cs.setGraphicsStateParameters(gs);

					cs.setFont(font, 50);
					cs.setNonStrokingColor(0.588f, 0.588f, 0.588f);

					float pageWidth = page.getMediaBox().getWidth();
					float pageHeight = page.getMediaBox().getHeight();
					float textWidth = font.getStringWidth(watermarkText) / 1000 * 50;

					cs.beginText();
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

			float dpi = 150f;
			float imageQuality = 0.6f;

			if ("HIGH".equalsIgnoreCase(level)) {
				dpi = 96f;
				imageQuality = 0.35f;
			} else if ("LOW".equalsIgnoreCase(level)) {
				dpi = 200f;
				imageQuality = 0.8f;
			}

			PDFRenderer pdfRenderer = new PDFRenderer(originalDoc);
			PDDocument compressedDoc = new PDDocument();

			for (int pageIndex = 0; pageIndex < originalDoc.getNumberOfPages(); pageIndex++) {
				BufferedImage bim = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
				PDImageXObject pdImage = JPEGFactory.createFromImage(compressedDoc, bim, imageQuality);

				PDPage originalPage = originalDoc.getPage(pageIndex);
				PDRectangle mediaBox = originalPage.getMediaBox();
				PDPage newPage = new PDPage(mediaBox);
				compressedDoc.addPage(newPage);

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
					if (bufferedImage == null)
						continue;

					PDRectangle pageRect = new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight());
					PDPage page = new PDPage(pageRect);
					document.addPage(page);

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
					BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 150);

					ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
					ImageIO.write(bim, "png", imgBaos);

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

			AccessPermission ap = new AccessPermission();
			ap.setCanPrint(true);
			ap.setCanExtractContent(true);

			String ownerPass = (ownerPassword != null && !ownerPassword.isBlank()) ? ownerPassword : userPassword;

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
				PDDocument document = Loader.loadPDF(inputStream.readAllBytes(), password)) {

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

				try (PDPageContentStream cs = new PDPageContentStream(document, page,
						PDPageContentStream.AppendMode.APPEND, true, true)) {

					cs.setFont(font, 10);
					cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);

					float stringWidth = font.getStringWidth(pageText) / 1000 * 10;
					float pageWidth = page.getMediaBox().getWidth();
					float pageHeight = page.getMediaBox().getHeight();

					float x = (pageWidth - stringWidth) / 2;
					float y = 30;

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

	// ===================== DOCX -> PDF (unchanged) =====================

	private static final int MAX_DOCX_BYTES = 5 * 1024 * 1024;
	private static final int MAX_PDF_BYTES = 10 * 1024 * 1024;

	private static final Semaphore DOCX_LIMITER = new Semaphore(2, true);

	public byte[] convertDocxToPdf(String fileUrl) throws Exception {
		if (fileUrl == null || !fileUrl.startsWith("https://res.cloudinary.com/")) {
			throw new IllegalArgumentException("Invalid file URL.");
		}

		byte[] docxBytes = downloadWithLimit(fileUrl, MAX_DOCX_BYTES);

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
			byte[] data = in.readNBytes(maxBytes + 1);
			if (data.length > maxBytes) {
				throw new IllegalArgumentException(
						"File too large. Maximum allowed size is " + (maxBytes / (1024 * 1024)) + " MB.");
			}
			return data;
		}
	}

	// ===================== PDF -> DOCX (layout-preserving) =====================

	/** Collects every character with its position, font and color. */
	private static class LayoutStripper extends PDFTextStripper {
		final List<TextPosition> chars = new ArrayList<>();
		final Map<TextPosition, Integer> colors = new IdentityHashMap<>();

		LayoutStripper() throws IOException {
			super();
		}

		@Override
		protected void processTextPosition(TextPosition text) {
			int rgb = 0;
			try {
				rgb = getGraphicsState().getNonStrokingColor().toRGB() & 0xFFFFFF;
			} catch (Exception ignored) {
			}
			chars.add(text);
			colors.put(text, rgb);
		}
	}

	private static class Piece {
		String text = "";
		String font = "Calibri";
		double size = 11;
		boolean bold, italic, tab;
		int rgb;
		double x; // start x (used for tabs)

		boolean sameStyle(Piece o) {
			return !tab && !o.tab && font.equals(o.font) && Math.abs(size - o.size) < 0.1 && bold == o.bold
					&& italic == o.italic && rgb == o.rgb;
		}
	}

	private static class Line {
		double y, minX = Double.MAX_VALUE, maxX = 0, maxSize = 0;
		List<Piece> pieces = new ArrayList<>();
		boolean hasTab;
	}

	public byte[] convertPdfToDocx(String fileUrl) throws Exception {
		if (fileUrl == null || !fileUrl.startsWith("https://res.cloudinary.com/")) {
			throw new IllegalArgumentException("Invalid file URL.");
		}
		byte[] pdfBytes = downloadWithLimit(fileUrl, MAX_PDF_BYTES);

		try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes);
				XWPFDocument docx = new XWPFDocument();
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			int pageCount = pdfDoc.getNumberOfPages();
			PDRectangle firstBox = pdfDoc.getPage(0).getMediaBox();
			double pageW = firstBox.getWidth();
			double pageH = firstBox.getHeight();

			// 1. Extract and group characters into lines, page by page
			List<List<Line>> pages = new ArrayList<>();
			double leftM = Double.MAX_VALUE, rightEdge = 0, topM = Double.MAX_VALUE;

			for (int p = 1; p <= pageCount; p++) {
				LayoutStripper stripper = new LayoutStripper();
				stripper.setStartPage(p);
				stripper.setEndPage(p);
				stripper.getText(pdfDoc); // drives the engine; we collect characters ourselves

				List<Line> lines = buildLines(stripper);
				pages.add(lines);

				for (Line l : lines) {
					leftM = Math.min(leftM, l.minX);
					rightEdge = Math.max(rightEdge, l.maxX);
				}
				if (!lines.isEmpty()) {
					Line first = lines.get(0);
					topM = Math.min(topM, first.y - first.maxSize * 0.96);
				}
			}

			if (leftM == Double.MAX_VALUE) {
				throw new IllegalArgumentException(
						"No selectable text found. Scanned or image-only PDFs are not supported.");
			}

			leftM = Math.max(10, leftM);
			double rightM = Math.max(10, pageW - rightEdge);
			topM = Math.max(10, topM);
			double textW = pageW - leftM - rightM;

			setPageLayout(docx, pageW, pageH, leftM, rightM, topM, 20);

			// 2. Write paragraphs
			for (int p = 0; p < pages.size(); p++) {
				List<Line> lines = pages.get(p);
				Line prev = null;
				boolean firstOnPage = true;

				for (Line line : lines) {
					XWPFParagraph para = docx.createParagraph();
					double lineH = line.maxSize * 1.2;

					if (firstOnPage) {
						if (p > 0)
							para.setPageBreak(true);
						para.setSpacingBefore(0);
					} else {
						double gap = line.y - prev.y;
						int before = (int) Math.round(Math.max(0, Math.min(gap - lineH, 300)) * 20);
						para.setSpacingBefore(before);
					}
					para.setSpacingAfter(0);
					para.setSpacingBetween(lineH, LineSpacingRule.EXACT);

					double leftOff = line.minX - leftM;
					double rightOff = (pageW - rightM) - line.maxX;

					if (!line.hasTab && leftOff > 10 && Math.abs(leftOff - rightOff) < 6) {
						para.setAlignment(ParagraphAlignment.CENTER);
					} else if (leftOff > 3) {
						para.setIndentationLeft((int) Math.round(leftOff * 20));
					}

					writeRuns(para, line, leftM, textW, pageW - rightM);

					prev = line;
					firstOnPage = false;
				}
			}

			docx.write(out);
			return out.toByteArray();
		}
	}

	private List<Line> buildLines(LayoutStripper stripper) {
		List<TextPosition> chars = new ArrayList<>(stripper.chars);
		chars.sort(Comparator.comparingDouble(TextPosition::getYDirAdj));

		// Group by baseline
		List<List<TextPosition>> groups = new ArrayList<>();
		List<TextPosition> current = null;
		double currentY = 0;
		for (TextPosition t : chars) {
			double tol = Math.max(2.0, t.getFontSizeInPt() * 0.4);
			if (current == null || Math.abs(t.getYDirAdj() - currentY) > tol) {
				current = new ArrayList<>();
				groups.add(current);
				currentY = t.getYDirAdj();
			}
			current.add(t);
		}

		List<Line> lines = new ArrayList<>();
		for (List<TextPosition> group : groups) {
			group.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
			Line line = new Line();
			double sumY = 0;
			Piece last = null;
			TextPosition prevChar = null;

			for (TextPosition t : group) {
				String u = t.getUnicode();
				if (u == null || u.isEmpty())
					continue;
				boolean blank = u.isBlank();
				if (blank && last == null)
					continue; // skip leading spaces

				double size = t.getFontSizeInPt();
				Piece style = styleOf(t, stripper.colors.getOrDefault(t, 0));

				if (prevChar != null) {
					double gap = t.getXDirAdj() - (prevChar.getXDirAdj() + prevChar.getWidthDirAdj());
					double refSize = Math.max(size, 1);
					if (gap > refSize * 2.5 && !blank) {
						Piece tab = new Piece();
						tab.tab = true;
						tab.x = t.getXDirAdj();
						line.pieces.add(tab);
						line.hasTab = true;
						last = null;
					} else if (gap > refSize * 0.15 && !blank && last != null && !last.text.endsWith(" ")) {
						last.text += " ";
					}
				}

				if (last != null && last.sameStyle(style)) {
					last.text += u;
				} else {
					style.text = u;
					style.x = t.getXDirAdj();
					line.pieces.add(style);
					last = style;
				}

				if (!blank) {
					line.minX = Math.min(line.minX, t.getXDirAdj());
					line.maxX = Math.max(line.maxX, t.getXDirAdj() + t.getWidthDirAdj());
					line.maxSize = Math.max(line.maxSize, size);
				}
				sumY += t.getYDirAdj();
				prevChar = t;
			}

			if (line.pieces.isEmpty() || line.minX == Double.MAX_VALUE)
				continue;
			line.y = sumY / group.size();
			if (line.maxSize < 1)
				line.maxSize = 11;

			// Trim trailing spaces on last text piece
			for (int i = line.pieces.size() - 1; i >= 0; i--) {
				Piece pc = line.pieces.get(i);
				if (pc.tab)
					continue;
				pc.text = pc.text.stripTrailing();
				break;
			}
			lines.add(line);
		}
		return lines;
	}

	private Piece styleOf(TextPosition t, int rgb) {
		Piece p = new Piece();
		p.size = t.getFontSizeInPt();
		p.rgb = rgb;

		String rawName = t.getFont() != null && t.getFont().getName() != null ? t.getFont().getName() : "Calibri";
		String lower = rawName.toLowerCase();
		boolean bold = lower.contains("bold") || lower.contains("black") || lower.contains("heavy");
		boolean italic = lower.contains("italic") || lower.contains("oblique");

		try {
			PDFontDescriptor fd = t.getFont().getFontDescriptor();
			if (fd != null) {
				if (fd.getFontWeight() >= 700)
					bold = true;
				if (fd.isItalic())
					italic = true;
			}
		} catch (Exception ignored) {
		}
		p.bold = bold;
		p.italic = italic;
		p.font = cleanFontName(rawName);
		return p;
	}

	private String cleanFontName(String name) {
		int plus = name.indexOf('+');
		if (plus == 6)
			name = name.substring(7); // remove subset prefix like ABCDEF+
		String family = name.split("[-,]")[0];
		family = family.replaceAll("(PSMT|MT)$", "");
		switch (family) {
		case "TimesNewRoman":
		case "TimesNewRomanPS":
			return "Times New Roman";
		case "Carlito":
			return "Calibri";
		case "LiberationSans":
			return "Arial";
		case "LiberationSerif":
			return "Times New Roman";
		case "CourierNew":
			return "Courier New";
		default:
			return family.isBlank() ? "Calibri" : family;
		}
	}

	private void writeRuns(XWPFParagraph para, Line line, double leftM, double textW, double rightEdge) {
		// Tab stops: the last tab becomes a right tab if its text ends at the right
		// margin
		int tabCount = 0;
		for (Piece pc : line.pieces)
			if (pc.tab)
				tabCount++;

		if (tabCount > 0) {
			CTPPr ppr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
			CTTabs tabs = ppr.addNewTabs();
			int seen = 0;
			for (Piece pc : line.pieces) {
				if (!pc.tab)
					continue;
				seen++;
				CTTabStop stop = tabs.addNewTab();
				boolean isLast = seen == tabCount;
				if (isLast && line.maxX >= rightEdge - 15) {
					stop.setVal(STTabJc.RIGHT);
					stop.setPos(BigInteger.valueOf(Math.round(textW * 20)));
				} else {
					stop.setVal(STTabJc.LEFT);
					stop.setPos(BigInteger.valueOf(Math.round((pc.x - leftM) * 20)));
				}
			}
		}

		for (Piece pc : line.pieces) {
			XWPFRun run = para.createRun();
			if (pc.tab) {
				run.addTab();
				continue;
			}
			run.setText(pc.text);
			run.setFontFamily(pc.font);
			run.setBold(pc.bold);
			run.setItalic(pc.italic);
			run.setColor(String.format("%06X", pc.rgb));

			CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
			rpr.addNewSz().setVal(BigInteger.valueOf(Math.round(pc.size * 2))); // half-points
		}
	}

	private void setPageLayout(XWPFDocument docx, double pageW, double pageH, double left, double right, double top,
			double bottom) {
		CTSectPr sect = docx.getDocument().getBody().isSetSectPr() ? docx.getDocument().getBody().getSectPr()
				: docx.getDocument().getBody().addNewSectPr();

		CTPageSz sz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
		sz.setW(BigInteger.valueOf(Math.round(pageW * 20)));
		sz.setH(BigInteger.valueOf(Math.round(pageH * 20)));

		CTPageMar mar = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
		mar.setLeft(BigInteger.valueOf(Math.round(left * 20)));
		mar.setRight(BigInteger.valueOf(Math.round(right * 20)));
		mar.setTop(BigInteger.valueOf(Math.round(top * 20)));
		mar.setBottom(BigInteger.valueOf(Math.round(bottom * 20)));
		mar.setHeader(BigInteger.ZERO);
		mar.setFooter(BigInteger.ZERO);
	}
	
	
	public byte[] convertExcelToPdf(String fileUrl) throws Exception {
	    try (InputStream inputStream = new URL(fileUrl).openStream();
	         Workbook workbook = new XSSFWorkbook(inputStream);
	         PDDocument pdfDoc = new PDDocument();
	         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

	        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	        float fontSize = 9f;
	        float leading = 1.2f * fontSize;
	        float margin = 40f;
	        float rowHeight = 20f;

	        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
	            Sheet sheet = workbook.getSheetAt(s);
	            if (sheet.getPhysicalNumberOfRows() == 0) continue;

	            PDPage page = new PDPage(PDRectangle.A4);
	            pdfDoc.addPage(page);

	            float pageWidth = page.getMediaBox().getWidth() - 2 * margin;
	            float startX = margin;
	            float startY = page.getMediaBox().getHeight() - margin;
	            float currentY = startY;

	            PDPageContentStream cs = new PDPageContentStream(pdfDoc, page);

	            // Find maximum number of columns across all rows
	            int maxCols = 0;
	            for (Row row : sheet) {
	                if (row.getLastCellNum() > maxCols) {
	                    maxCols = row.getLastCellNum();
	                }
	            }

	            if (maxCols == 0) continue;
	            float colWidth = pageWidth / maxCols;

	            // Sheet Title Header
	            cs.beginText();
	            cs.setFont(boldFont, 14);
	            cs.newLineAtOffset(startX, currentY);
	            cs.showText("Sheet: " + sheet.getSheetName());
	            cs.endText();
	            currentY -= 30;

	            for (Row row : sheet) {
	                // Check page height boundary
	                if (currentY - rowHeight < margin) {
	                    cs.close();
	                    page = new PDPage(PDRectangle.A4);
	                    pdfDoc.addPage(page);
	                    cs = new PDPageContentStream(pdfDoc, page);
	                    currentY = startY;
	                }

	                for (int col = 0; col < maxCols; col++) {
	                    Cell cell = row.getCell(col);
	                    String cellValue = "";

	                    if (cell != null) {
	                        switch (cell.getCellType()) {
	                            case STRING -> cellValue = cell.getStringCellValue();
	                            case NUMERIC -> {
	                                if (DateUtil.isCellDateFormatted(cell)) {
	                                    cellValue = cell.getDateCellValue().toString();
	                                } else {
	                                    cellValue = String.valueOf(cell.getNumericCellValue());
	                                }
	                            }
	                            case BOOLEAN -> cellValue = String.valueOf(cell.getBooleanCellValue());
	                            case FORMULA -> cellValue = cell.getCellFormula();
	                            default -> cellValue = "";
	                        }
	                    }

	                    // Sanitize text for WinAnsiEncoding
	                    cellValue = cellValue.replaceAll("[^\\x00-\\x7F\\xA0-\\xFF]", "").trim();

	                    float cellX = startX + (col * colWidth);

	                    // Draw Table Cell Grid Lines
	                    cs.setStrokingColor(0.8f, 0.8f, 0.8f);
	                    cs.addRect(cellX, currentY - rowHeight, colWidth, rowHeight);
	                    cs.stroke();

	                    // Render Cell Text inside grid bounds
	                    if (!cellValue.isEmpty()) {
	                        cs.beginText();
	                        cs.setFont(font, fontSize);
	                        cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
	                        cs.newLineAtOffset(cellX + 5, currentY - 14);

	                        // Truncate text if it exceeds cell width
	                        if (font.getStringWidth(cellValue) / 1000 * fontSize > colWidth - 10) {
	                            while (cellValue.length() > 3 && (font.getStringWidth(cellValue + "...") / 1000 * fontSize) > colWidth - 10) {
	                                cellValue = cellValue.substring(0, cellValue.length() - 1);
	                            }
	                            cellValue += "...";
	                        }

	                        cs.showText(cellValue);
	                        cs.endText();
	                    }
	                }
	                currentY -= rowHeight;
	            }
	            cs.close();
	        }

	        pdfDoc.save(outputStream);
	        return outputStream.toByteArray();
	    }
	}
	
	
	public byte[] convertPdfToExcel(String fileUrl) throws Exception {
	    if (fileUrl == null || !fileUrl.startsWith("https://res.cloudinary.com/")) {
	        throw new IllegalArgumentException("Invalid file URL.");
	    }
	    byte[] pdfBytes = downloadWithLimit(fileUrl, MAX_PDF_BYTES);

	    try (PDDocument pdfDoc = Loader.loadPDF(pdfBytes);
	         Workbook workbook = new XSSFWorkbook();
	         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	        int totalPages = pdfDoc.getNumberOfPages();

	        for (int p = 1; p <= totalPages; p++) {
	            LayoutStripper stripper = new LayoutStripper();
	            stripper.setStartPage(p);
	            stripper.setEndPage(p);
	            stripper.getText(pdfDoc);

	            List<Line> lines = buildLines(stripper);
	            Sheet sheet = workbook.createSheet("Page " + p);

	            int rowIndex = 0;
	            for (Line line : lines) {
	                Row excelRow = sheet.createRow(rowIndex++);
	                int colIndex = 0;

	                for (Piece piece : line.pieces) {
	                    if (piece.tab) {
	                        colIndex++; // Move to next column on tab boundary
	                        continue;
	                    }

	                    String text = piece.text.trim();
	                    if (!text.isEmpty()) {
	                        Cell cell = excelRow.createCell(colIndex);

	                        // Auto-detect numbers vs string values for Excel formatting
	                        try {
	                            double numVal = Double.parseDouble(text.replace(",", ""));
	                            cell.setCellValue(numVal);
	                        } catch (NumberFormatException e) {
	                            cell.setCellValue(text);
	                        }
	                        colIndex++;
	                    }
	                }
	            }

	            // Auto-fit column widths
	            if (rowIndex > 0 && sheet.getRow(0) != null) {
	                int lastColIndex = sheet.getRow(0).getLastCellNum();
	                for (int c = 0; c < lastColIndex; c++) {
	                    sheet.autoSizeColumn(c);
	                }
	            }
	        }

	        workbook.write(out);
	        return out.toByteArray();
	    }
	}
	
	
	public byte[] convertPptxToPdf(String fileUrl) throws Exception {
	    if (fileUrl == null || !fileUrl.startsWith("https://res.cloudinary.com/")) {
	        throw new IllegalArgumentException("Invalid file URL.");
	    }

	    byte[] pptxBytes = downloadWithLimit(fileUrl, MAX_DOCX_BYTES);

	    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pptxBytes);
	         XMLSlideShow ppt = new XMLSlideShow(inputStream);
	         PDDocument pdfDoc = new PDDocument();
	         ByteArrayOutputStream out = new ByteArrayOutputStream()) {

	        Dimension pgsize = ppt.getPageSize();
	        float width = (float) pgsize.getWidth();
	        float height = (float) pgsize.getHeight();

	        for (XSLFSlide slide : ppt.getSlides()) {
	            BufferedImage img = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_RGB);
	            Graphics2D graphics = img.createGraphics();

	            // Enable high-quality anti-aliasing for text and shapes
	            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

	            // Clear background
	            graphics.setPaint(java.awt.Color.WHITE);
	            graphics.fill(new java.awt.Rectangle(0, 0, (int) width, (int) height));

	            // Render presentation slide onto Graphics2D buffer
	            slide.draw(graphics);
	            graphics.dispose();

	            // Create PDF Page with slide dimensions
	            PDRectangle pageRect = new PDRectangle(width, height);
	            PDPage page = new PDPage(pageRect);
	            pdfDoc.addPage(page);

	            PDImageXObject pdImage = JPEGFactory.createFromImage(pdfDoc, img, 0.9f);

	            try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page)) {
	                contentStream.drawImage(pdImage, 0, 0, width, height);
	            }
	        }

	        pdfDoc.save(out);
	        return out.toByteArray();
	    }
	}
	
}