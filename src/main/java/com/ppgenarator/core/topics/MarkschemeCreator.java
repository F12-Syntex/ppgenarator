package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenarator.utils.FileUtils;
import com.ppgenarator.utils.FormattingUtils;
import com.ppgenerator.types.Question;

public class MarkschemeCreator {

    private static final float MARGIN = 50f;
    private static final int MIN_PAGE_CONTENT_LENGTH = 10;
    private static final String MARKSCHEME_FILENAME = "mock_markscheme.pdf";

    /**
     * Creates a comprehensive markscheme PDF for a mock test
     */
    public File createMockTestMarkscheme(List<Question> questions, File mockTestDir) throws IOException {
        List<Question> validQuestions = filterQuestionsWithMarkschemes(questions);

        if (validQuestions.isEmpty()) {
            System.out.println("📝 No markschemes found for this mock test");
            return null;
        }

        File markschemeFile = new File(mockTestDir, MARKSCHEME_FILENAME);
        List<File> tempFiles = new ArrayList<>();

        try {
            PDFMergerUtility merger = createMerger(markschemeFile);

            // Add index page
            File indexPage = createIndexPage(validQuestions, mockTestDir);
            merger.addSource(indexPage);
            tempFiles.add(indexPage);

            // Add processed markschemes
            addMarkschemes(merger, validQuestions, tempFiles);

            merger.mergeDocuments(null);
            System.out.println("✅ Created markscheme: " + markschemeFile.getAbsolutePath());

            return markschemeFile;

        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    private List<Question> filterQuestionsWithMarkschemes(List<Question> questions) {
        return questions.stream()
                .filter(q -> q.getMarkScheme() != null && q.getMarkScheme().exists())
                .toList();
    }

    private PDFMergerUtility createMerger(File outputFile) {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFile.getAbsolutePath());
        return merger;
    }

    private File createIndexPage(List<Question> questions, File mockTestDir) throws IOException {
        File indexFile = new File(mockTestDir, "temp_index.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                renderIndexPage(content, page, questions);
            }

            document.save(indexFile);
        }

        return indexFile;
    }

    private void renderIndexPage(PDPageContentStream content, PDPage page, List<Question> questions)
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - MARGIN;

        // Modern header
        y = drawHeader(content, pageWidth, y);
        y = drawSeparator(content, pageWidth, y - 30);

        // Table
        y = drawTableHeader(content, y - 40);
        drawTableRows(content, questions, y - 30);

        // Footer
        drawFooter(content, pageWidth);
    }

    private float drawHeader(PDPageContentStream content, float pageWidth, float y) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 28);
        String title = "MARKSCHEME INDEX";
        float titleWidth = getTextWidth(title, PDType1Font.HELVETICA_BOLD, 28);
        content.newLineAtOffset((pageWidth - titleWidth) / 2, y);
        content.showText(title);
        content.endText();
        return y;
    }

    private float drawSeparator(PDPageContentStream content, float pageWidth, float y) throws IOException {
        content.setLineWidth(3);
        content.moveTo(MARGIN, y);
        content.lineTo(pageWidth - MARGIN, y);
        content.stroke();
        return y;
    }

    private float drawTableHeader(PDPageContentStream content, float y) throws IOException {
        String[] headers = {"Question", "Year", "Marks", "Page"};
        float[] positions = {MARGIN, MARGIN + 150, MARGIN + 250, MARGIN + 350};

        for (int i = 0; i < headers.length; i++) {
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.newLineAtOffset(positions[i], y);
            content.showText(headers[i]);
            content.endText();
        }

        // Underline headers
        content.setLineWidth(1.5f);
        content.moveTo(MARGIN, y - 8);
        content.lineTo(MARGIN + 400, y - 8);
        content.stroke();

        return y;
    }

    private void drawTableRows(PDPageContentStream content, List<Question> questions, float startY)
            throws IOException {
        float y = startY;
        int currentPage = 2; // After index
        Set<String> processed = new HashSet<>();

        for (Question question : questions) {
            String hash = FileUtils.getFileMd5Hash(question.getMarkScheme());
            if (processed.contains(hash)) {
                continue;
            }

            processed.add(hash);
            drawTableRow(content, question, y, currentPage);

            currentPage += countValidPages(question.getMarkScheme()) + 1; // +1 for header page
            y -= 20;

            if (y < MARGIN + 100) {
                break; // Prevent overflow

                    }}
    }

    private void drawTableRow(PDPageContentStream content, Question question, float y, int pageNum)
            throws IOException {
        float[] positions = {MARGIN, MARGIN + 150, MARGIN + 250, MARGIN + 350};
        String[] values = {
            FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
            question.getYear(),
            String.valueOf(question.getMarks()),
            String.valueOf(pageNum)
        };

        for (int i = 0; i < values.length; i++) {
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(positions[i], y);
            content.showText(values[i]);
            content.endText();
        }
    }

    private void drawFooter(PDPageContentStream content, float pageWidth) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_OBLIQUE, 11);
        String footer = "Navigate efficiently with this comprehensive index";
        float footerWidth = getTextWidth(footer, PDType1Font.HELVETICA_OBLIQUE, 11);
        content.newLineAtOffset((pageWidth - footerWidth) / 2, MARGIN);
        content.showText(footer);
        content.endText();
    }

    private void addMarkschemes(PDFMergerUtility merger, List<Question> questions, List<File> tempFiles)
            throws IOException {
        Set<String> processed = new HashSet<>();

        for (Question question : questions) {
            String hash = FileUtils.getFileMd5Hash(question.getMarkScheme());
            if (processed.contains(hash)) {
                System.out.println("⏭️  Skipping duplicate: " + question.getQuestionNumber());
                continue;
            }

            processed.add(hash);

            File processedFile = processMarkscheme(question);
            if (processedFile != null) {
                merger.addSource(processedFile);
                tempFiles.add(processedFile);
            }
        }
    }

    private File processMarkscheme(Question question) throws IOException {
        try (PDDocument original = PDDocument.load(question.getMarkScheme()); PDDocument processed = new PDDocument()) {

            // Add header page
            addHeaderPage(processed, question);

            // Add valid pages only
            addValidPages(original, processed);

            File tempFile = File.createTempFile("markscheme_", ".pdf");
            processed.save(tempFile);

            return tempFile;

        } catch (Exception e) {
            System.err.println("❌ Error processing markscheme for " + question.getQuestionNumber()
                    + ": " + e.getMessage());
            return null;
        }
    }

    private void addHeaderPage(PDDocument document, Question question) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            renderHeaderPage(content, page, question);
        }
    }

    private void renderHeaderPage(PDPageContentStream content, PDPage page, Question question)
            throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        // Modern border
        content.setLineWidth(3);
        content.addRect(MARGIN, MARGIN, pageWidth - 2 * MARGIN, pageHeight - 2 * MARGIN);
        content.stroke();

        // Title
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 24);
        String title = "MARKSCHEME";
        float titleWidth = getTextWidth(title, PDType1Font.HELVETICA_BOLD, 24);
        content.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - MARGIN - 80);
        content.showText(title);
        content.endText();

        // Question details in a clean layout
        float y = pageHeight - MARGIN - 150;
        Map<String, String> details = Map.of(
                "Question", FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                "Year", question.getYear(),
                "Board", question.getBoard().toString(),
                "Marks", String.valueOf(question.getMarks()));

        for (Map.Entry<String, String> entry : details.entrySet()) {
            drawDetailLine(content, entry.getKey(), entry.getValue(), y);
            y -= 30;
        }
    }

    private void drawDetailLine(PDPageContentStream content, String label, String value, float y)
            throws IOException {
        // Label
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 14);
        content.newLineAtOffset(MARGIN + 50, y);
        content.showText(label + ":");
        content.endText();

        // Value
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 14);
        content.newLineAtOffset(MARGIN + 150, y);
        content.showText(value);
        content.endText();
    }

    private void addValidPages(PDDocument original, PDDocument processed) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        Set<Integer> seenHashes = new HashSet<>();

        for (int i = 0; i < original.getNumberOfPages(); i++) {
            String text = extractPageText(stripper, original, i);

            if (isValidPage(text) && !isDuplicate(text, seenHashes)) {
                processed.importPage(original.getPage(i));
            }
        }
    }

    private String extractPageText(PDFTextStripper stripper, PDDocument document, int pageIndex)
            throws IOException {
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        return stripper.getText(document).trim();
    }

    private boolean isValidPage(String text) {
        return text.length() >= MIN_PAGE_CONTENT_LENGTH;
    }

    private boolean isDuplicate(String text, Set<Integer> seenHashes) {
        int hash = text.hashCode();
        return !seenHashes.add(hash);
    }

    private int countValidPages(File markschemeFile) throws IOException {
        try (PDDocument document = PDDocument.load(markschemeFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            Set<Integer> seenHashes = new HashSet<>();
            int count = 0;

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                String text = extractPageText(stripper, document, i);
                if (isValidPage(text) && !isDuplicate(text, seenHashes)) {
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
