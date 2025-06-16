package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenarator.utils.FileUtils;
import com.ppgenarator.utils.FormattingUtils;
import com.ppgenerator.types.Question;

public class MarkschemeCreator {

    private static final float MARGIN = 50f;
    private static final int MIN_PAGE_CONTENT_LENGTH = 20;
    private static final PDColor BLUE_COLOR = new PDColor(new float[] { 0.2f, 0.4f, 0.8f }, PDDeviceRGB.INSTANCE);
    private static final PDColor LIGHT_GRAY = new PDColor(new float[] { 0.95f, 0.95f, 0.95f }, PDDeviceRGB.INSTANCE);
    private static final PDColor DARK_GRAY = new PDColor(new float[] { 0.3f, 0.3f, 0.3f }, PDDeviceRGB.INSTANCE);

    /**
     * Creates a comprehensive markscheme PDF with index and clean formatting
     */
    public File createMockTestMarkscheme(List<Question> questions, File outputDir) throws IOException {
        List<Question> validQuestions = getUniqueQuestionsWithMarkschemes(questions);

        if (validQuestions.isEmpty()) {
            System.out.println("📝 No markschemes found for this mock test");
            return null;
        }

        File markschemeFile = new File(outputDir, "markscheme.pdf");
        List<File> tempFiles = new ArrayList<>();

        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.setDestinationFileName(markschemeFile.getAbsolutePath());

            // Create and add index page
            File indexPage = createIndexPage(validQuestions, outputDir);
            merger.addSource(indexPage);
            tempFiles.add(indexPage);

            // Add markschemes with clean headers
            addProcessedMarkschemes(merger, validQuestions, tempFiles);

            merger.mergeDocuments(null);
            System.out.println("✅ Created markscheme.pdf: " + markschemeFile.getAbsolutePath());

            return markschemeFile;

        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    /**
     * Get unique questions with markschemes, removing duplicates by file hash
     */
    private List<Question> getUniqueQuestionsWithMarkschemes(List<Question> questions) {
        Set<String> seenHashes = new LinkedHashSet<>();
        List<Question> uniqueQuestions = new ArrayList<>();

        for (Question question : questions) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                String hash = FileUtils.getFileMd5Hash(question.getMarkScheme());
                if (!seenHashes.contains(hash)) {
                    seenHashes.add(hash);
                    uniqueQuestions.add(question);
                }
            }
        }

        return uniqueQuestions;
    }

    /**
     * Create a beautiful, modern index page
     */
    private File createIndexPage(List<Question> questions, File outputDir) throws IOException {
        File indexFile = new File(outputDir, "temp_index.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                renderModernIndexPage(content, page, questions);
            }

            document.save(indexFile);
        }

        return indexFile;
    }

    /**
     * Render a clean, modern index page
     */
    private void renderModernIndexPage(PDPageContentStream content, PDPage page, List<Question> questions)
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - MARGIN;

        // Modern header with accent color
        y = drawModernHeader(content, pageWidth, y);

        // Stylish table
        y = drawStyledTable(content, questions, y - 60, pageWidth);

        // Clean footer
        drawModernFooter(content, pageWidth);
    }

    private float drawModernHeader(PDPageContentStream content, float pageWidth, float y) throws IOException {
        // Blue accent bar
        content.setNonStrokingColor(BLUE_COLOR);
        content.addRect(MARGIN, y - 10, pageWidth - 2 * MARGIN, 8);
        content.fill();

        // Main title
        content.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 32);
        String title = "MARKSCHEME INDEX";
        float titleWidth = getTextWidth(title, PDType1Font.HELVETICA_BOLD, 32);
        content.newLineAtOffset((pageWidth - titleWidth) / 2, y - 50);
        content.showText(title);
        content.endText();

        // Subtitle
        content.setNonStrokingColor(DARK_GRAY);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 14);
        String subtitle = "Navigate to question markschemes efficiently";
        float subtitleWidth = getTextWidth(subtitle, PDType1Font.HELVETICA, 14);
        content.newLineAtOffset((pageWidth - subtitleWidth) / 2, y - 75);
        content.showText(subtitle);
        content.endText();

        return y - 90;
    }

    private float drawStyledTable(PDPageContentStream content, List<Question> questions, float startY, float pageWidth)
            throws IOException {
        float tableWidth = pageWidth - 2 * MARGIN;
        float[] colWidths = { 120, 80, 80, 100 }; // Question, Year, Marks, Page
        String[] headers = { "Question", "Year", "Marks", "Page" };
        float rowHeight = 25;
        float y = startY;

        // Table header background
        content.setNonStrokingColor(LIGHT_GRAY);
        content.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
        content.fill();

        // Header border
        content.setStrokingColor(BLUE_COLOR);
        content.setLineWidth(2);
        content.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
        content.stroke();

        // Header text
        content.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
        float currentX = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            float textWidth = getTextWidth(headers[i], PDType1Font.HELVETICA_BOLD, 12);
            content.newLineAtOffset(currentX + (colWidths[i] - textWidth) / 2, y - 17);
            content.showText(headers[i]);
            content.endText();
            currentX += colWidths[i];
        }

        y -= rowHeight;

        // Table rows
        int pageNum = 2; // Start after index
        boolean alternate = false;

        for (Question question : questions) {
            // Alternating row colors
            if (alternate) {
                content.setNonStrokingColor(new PDColor(new float[] { 0.98f, 0.98f, 0.98f }, PDDeviceRGB.INSTANCE));
                content.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
                content.fill();
            }

            // Row data
            content.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
            String[] rowData = {
                    FormattingUtils.formatQuestionNumber(question.getQuestionNumber().replace("Question", "")).trim(),
                    question.getYear(),
                    String.valueOf(question.getMarks()),
                    String.valueOf(pageNum)
            };

            currentX = MARGIN;
            for (int i = 0; i < rowData.length; i++) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                float textWidth = getTextWidth(rowData[i], PDType1Font.HELVETICA, 11);
                float textX = (i == 0) ? currentX + 10 : currentX + (colWidths[i] - textWidth) / 2;
                content.newLineAtOffset(textX, y - 17);
                content.showText(rowData[i]);
                content.endText();
                currentX += colWidths[i];
            }

            // Light border
            content.setStrokingColor(new PDColor(new float[] { 0.9f, 0.9f, 0.9f }, PDDeviceRGB.INSTANCE));
            content.setLineWidth(0.5f);
            content.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
            content.stroke();

            y -= rowHeight;
            alternate = !alternate;
            pageNum += countValidPages(question.getMarkScheme()) + 1; // +1 for header page

            // Prevent overflow
            if (y < MARGIN + 80)
                break;
        }

        return y;
    }

    private void drawModernFooter(PDPageContentStream content, float pageWidth) throws IOException {
        // Instructions box
        float boxWidth = pageWidth - 2 * MARGIN;
        float boxHeight = 40;
        float boxY = MARGIN + 20;

        // Background
        content.setNonStrokingColor(new PDColor(new float[] { 0.96f, 0.98f, 1f }, PDDeviceRGB.INSTANCE));
        content.addRect(MARGIN, boxY, boxWidth, boxHeight);
        content.fill();

        // Border
        content.setStrokingColor(BLUE_COLOR);
        content.setLineWidth(1);
        content.addRect(MARGIN, boxY, boxWidth, boxHeight);
        content.stroke();

        // Text
        content.setNonStrokingColor(DARK_GRAY);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(MARGIN + 15, boxY + 25);
        content.showText("Quick Navigation:");
        content.endText();

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 10);
        content.newLineAtOffset(MARGIN + 15, boxY + 10);
        content.showText("Use the page numbers in the rightmost column to jump directly to specific markschemes.");
        content.endText();
    }

    /**
     * Add processed markschemes with clean header pages
     */
    private void addProcessedMarkschemes(PDFMergerUtility merger, List<Question> questions, List<File> tempFiles)
            throws IOException {
        for (Question question : questions) {
            File processedFile = createCleanMarkscheme(question);
            if (processedFile != null) {
                merger.addSource(processedFile);
                tempFiles.add(processedFile);
                System.out.println("📄 Added markscheme: " + question.getQuestionNumber());
            }
        }
    }

    /**
     * Create a clean markscheme with simple header page
     */
    private File createCleanMarkscheme(Question question) throws IOException {
        try (PDDocument original = PDDocument.load(question.getMarkScheme());
                PDDocument processed = new PDDocument()) {

            // Add simple, clean header page
            addCleanHeaderPage(processed, question);

            // Add only valid, non-duplicate pages
            addValidPagesOnly(original, processed);

            File tempFile = File.createTempFile("markscheme_", ".pdf");
            processed.save(tempFile);
            return tempFile;

        } catch (Exception e) {
            System.err.println("❌ Error processing markscheme for " + question.getQuestionNumber()
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Add a clean, simple header page
     */
    private void addCleanHeaderPage(PDDocument document, Question question) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // Blue accent stripe
            content.setNonStrokingColor(BLUE_COLOR);
            content.addRect(0, pageHeight - 80, pageWidth, 80);
            content.fill();

            // White title area
            content.setNonStrokingColor(new PDColor(new float[] { 1f, 1f, 1f }, PDDeviceRGB.INSTANCE));
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 28);
            String title = "MARKSCHEME";
            float titleWidth = getTextWidth(title, PDType1Font.HELVETICA_BOLD, 28);
            content.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 55);
            content.showText(title);
            content.endText();

            // Question details in clean layout
            content.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
            float centerX = pageWidth / 2;
            float startY = pageHeight - 150;

            drawCenteredDetail(content,
                    "Question " + FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                    centerX, startY, PDType1Font.HELVETICA_BOLD, 20);
            drawCenteredDetail(content, question.getYear(), centerX, startY - 40, PDType1Font.HELVETICA, 16);
            drawCenteredDetail(content, question.getMarks() + " marks", centerX, startY - 70, PDType1Font.HELVETICA,
                    14);

            // Simple bottom border
            content.setStrokingColor(BLUE_COLOR);
            content.setLineWidth(3);
            content.moveTo(MARGIN, MARGIN + 50);
            content.lineTo(pageWidth - MARGIN, MARGIN + 50);
            content.stroke();
        }
    }

    private void drawCenteredDetail(PDPageContentStream content, String text, float centerX, float y,
            org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        float textWidth = getTextWidth(text, font, fontSize);
        content.newLineAtOffset(centerX - textWidth / 2, y);
        content.showText(text);
        content.endText();
    }

    /**
     * Add only valid, non-duplicate pages from the original markscheme
     */
    private void addValidPagesOnly(PDDocument original, PDDocument processed) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        Set<Integer> seenHashes = new LinkedHashSet<>();

        for (int i = 0; i < original.getNumberOfPages(); i++) {
            String pageText = extractPageText(stripper, original, i);

            if (isValidPage(pageText) && !isDuplicatePage(pageText, seenHashes)) {
                processed.importPage(original.getPage(i));
            }
        }
    }

    private String extractPageText(PDFTextStripper stripper, PDDocument document, int pageIndex) throws IOException {
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        return stripper.getText(document).trim();
    }

    private boolean isValidPage(String text) {
        return text.length() >= MIN_PAGE_CONTENT_LENGTH &&
                !text.toLowerCase().contains("blank page") &&
                !text.trim().isEmpty();
    }

    private boolean isDuplicatePage(String text, Set<Integer> seenHashes) {
        int hash = text.hashCode();
        return !seenHashes.add(hash);
    }

    /**
     * Count valid pages in a markscheme file
     */
    private int countValidPages(File markschemeFile) throws IOException {
        try (PDDocument document = PDDocument.load(markschemeFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            Set<Integer> seenHashes = new LinkedHashSet<>();
            int count = 0;

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                String text = extractPageText(stripper, document, i);
                if (isValidPage(text) && !isDuplicatePage(text, seenHashes)) {
                    count++;
                }
            }

            return count;
        }
    }

    private float getTextWidth(String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize)
            throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    private void cleanupTempFiles(List<File> tempFiles) {
        tempFiles.forEach(file -> {
            if (file.exists() && !file.delete()) {
                file.deleteOnExit();
            }
        });
    }
}