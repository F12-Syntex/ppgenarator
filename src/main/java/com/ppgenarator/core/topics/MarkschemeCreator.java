package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public File createMockTestMarkscheme(List<Question> questions, File mockTestDir) throws IOException {
        // Filter questions that have markschemes
        List<Question> questionsWithMarkschemes = new ArrayList<>();
        for (Question question : questions) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                questionsWithMarkschemes.add(question);
            }
        }

        if (questionsWithMarkschemes.isEmpty()) {
            System.out.println("No markschemes found for this mock test");
            return null;
        }

        // Create the final markscheme PDF
        PDFMergerUtility markschemesMerger = new PDFMergerUtility();
        File markschemeOutputFile = new File(mockTestDir, "mock_markscheme.pdf");
        markschemesMerger.setDestinationFileName(markschemeOutputFile.getAbsolutePath());

        // Create index page
        File indexPageFile = createMockMarkschemeIndexPage(questionsWithMarkschemes, mockTestDir);
        markschemesMerger.addSource(indexPageFile);

        List<File> tempMarkSchemeFiles = new ArrayList<>();
        Set<String> processedMarkschemes = new HashSet<>();

        // Process each markscheme
        for (Question question : questionsWithMarkschemes) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                String markschemeHash = FileUtils.getFileMd5Hash(question.getMarkScheme());

                // Skip duplicates
                if (processedMarkschemes.contains(markschemeHash)) {
                    System.out.println("Skipping duplicate markscheme in mock test: " +
                            question.getYear() + "_" + question.getQuestionNumber());
                    continue;
                }

                processedMarkschemes.add(markschemeHash);

                // Load and process the markscheme PDF
                File processedMarkscheme = processMarkscheme(question.getMarkScheme(), question);
                if (processedMarkscheme != null) {
                    markschemesMerger.addSource(processedMarkscheme);
                    tempMarkSchemeFiles.add(processedMarkscheme);
                }
            }
        }

        // Merge all markschemes
        markschemesMerger.mergeDocuments(null);
        System.out.println("Created mock test markschemes PDF: " + markschemeOutputFile.getAbsolutePath());

        // Clean up temporary files
        indexPageFile.delete();
        for (File tempFile : tempMarkSchemeFiles) {
            tempFile.delete();
        }

        return markschemeOutputFile;
    }

    private File createMockMarkschemeIndexPage(List<Question> questions, File mockTestDir) throws IOException {
        File indexPageFile = new File(mockTestDir, "mock_index_page.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createIndexPageContent(contentStream, page, questions);
            }

            document.save(indexPageFile);
        }

        return indexPageFile;
    }

    private void createIndexPageContent(PDPageContentStream contentStream, PDPage page, List<Question> questions)
            throws IOException {
        float margin = 50;
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float yPosition = pageHeight - margin;

        // Header
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        String title = "MOCK TEST MARKSCHEME INDEX";
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 24;
        contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
        contentStream.showText(title);
        contentStream.endText();

        yPosition -= 50;

        // Draw a line under the title
        contentStream.setLineWidth(2);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(pageWidth - margin, yPosition);
        contentStream.stroke();

        yPosition -= 30;

        // Create table headers
        yPosition = createTableHeaders(contentStream, margin, pageWidth, yPosition);
        yPosition -= 20;

        // Create table content
        createTableContent(contentStream, questions, margin, pageWidth, yPosition);

        // Footer
        createIndexFooter(contentStream, pageWidth, margin);
    }

    private float createTableHeaders(PDPageContentStream contentStream, float margin, float pageWidth, float yPosition)
            throws IOException {
        String[] headers = { "Question", "Year", "Marks", "Page" };
        float[] positions = { margin, margin + 150, margin + 220, margin + 300 };

        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(positions[i], yPosition);
            contentStream.showText(headers[i]);
            contentStream.endText();
        }

        // Draw line under headers
        contentStream.setLineWidth(1);
        contentStream.moveTo(margin, yPosition - 5);
        contentStream.lineTo(pageWidth - margin, yPosition - 5);
        contentStream.stroke();

        return yPosition;
    }

    private void createTableContent(PDPageContentStream contentStream, List<Question> questions,
            float margin, float pageWidth, float yPosition) throws IOException {
        int currentPage = 2; // Start after index page
        Set<String> processedMarkschemes = new HashSet<>();

        for (Question question : questions) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                String markschemeHash = FileUtils.getFileMd5Hash(question.getMarkScheme());

                // Skip duplicates in the index
                if (processedMarkschemes.contains(markschemeHash)) {
                    continue;
                }
                processedMarkschemes.add(markschemeHash);

                // Create table row
                createTableRow(contentStream, question, margin, yPosition, currentPage);
                yPosition -= 18;

                // Calculate pages for this markscheme
                try (PDDocument markschemeDoc = PDDocument.load(question.getMarkScheme())) {
                    int validPages = countValidPages(markschemeDoc);
                    currentPage += validPages;
                }

                // Check if we need a new page
                if (yPosition < margin + 50) {
                    break; // Simple implementation - just break if running out of space
                }
            }
        }
    }

    private void createTableRow(PDPageContentStream contentStream, Question question,
            float margin, float yPosition, int currentPage) throws IOException {
        float[] positions = { margin, margin + 150, margin + 220, margin + 300 };
        String[] values = {
                FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                question.getYear(),
                String.valueOf(question.getMarks()),
                String.valueOf(currentPage)
        };

        for (int i = 0; i < values.length; i++) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.newLineAtOffset(positions[i], yPosition);
            contentStream.showText(values[i]);
            contentStream.endText();
        }
    }

    private void createIndexFooter(PDPageContentStream contentStream, float pageWidth, float margin)
            throws IOException {
        float yPosition = margin + 30;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
        String footer = "Use this index to quickly navigate to specific question markschemes.";
        float footerWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(footer) / 1000 * 10;
        contentStream.newLineAtOffset((pageWidth - footerWidth) / 2, yPosition);
        contentStream.showText(footer);
        contentStream.endText();
    }

    private File processMarkscheme(File markschemeFile, Question question) throws IOException {
        try (PDDocument document = PDDocument.load(markschemeFile)) {
            PDDocument processedDoc = new PDDocument();

            // Add a header page for this question's markscheme
            addMarkschemeHeaderPage(processedDoc, question);

            // Process original markscheme pages (remove empty and duplicate pages)
            processOriginalMarkschemePages(document, processedDoc, question);

            // Save the processed document
            File tempFile = File.createTempFile("processed_ms_", ".pdf");
            processedDoc.save(tempFile);
            processedDoc.close();

            return tempFile;

        } catch (Exception e) {
            System.err
                    .println("Error processing markscheme for " + question.getQuestionNumber() + ": " + e.getMessage());
            return null;
        }
    }

    private void addMarkschemeHeaderPage(PDDocument processedDoc, Question question) throws IOException {
        PDPage headerPage = new PDPage(PDRectangle.A4);
        processedDoc.addPage(headerPage);

        try (PDPageContentStream contentStream = new PDPageContentStream(processedDoc, headerPage)) {
            float margin = 50;
            float pageWidth = headerPage.getMediaBox().getWidth();
            float pageHeight = headerPage.getMediaBox().getHeight();

            // Draw border
            contentStream.setLineWidth(2);
            contentStream.addRect(margin, margin, pageWidth - 2 * margin, pageHeight - 2 * margin);
            contentStream.stroke();

            // Header
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
            String header = "MARKSCHEME";
            float headerWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(header) / 1000 * 20;
            contentStream.newLineAtOffset((pageWidth - headerWidth) / 2, pageHeight - margin - 50);
            contentStream.showText(header);
            contentStream.endText();

            // Question details
            float yPos = pageHeight - margin - 120;
            String[] details = {
                    "Question: " + FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                    "Year: " + question.getYear(),
                    "Exam Board: " + question.getBoard().toString(),
                    "Marks: " + question.getMarks()
            };

            for (String detail : details) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.newLineAtOffset(margin + 30, yPos);
                contentStream.showText(detail);
                contentStream.endText();
                yPos -= 25;
            }
        }
    }

    private void processOriginalMarkschemePages(PDDocument document, PDDocument processedDoc, Question question)
            throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        Set<String> seenPageContent = new HashSet<>();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            textStripper.setStartPage(i + 1);
            textStripper.setEndPage(i + 1);
            String pageText = textStripper.getText(document).trim();

            // Skip empty pages or pages with minimal content
            if (pageText.length() < 10) {
                System.out.println("Skipping empty page " + (i + 1) + " in markscheme for " +
                        question.getYear() + "_" + question.getQuestionNumber());
                continue;
            }

            // Skip duplicate pages
            String pageHash = Integer.toString(pageText.hashCode());
            if (seenPageContent.contains(pageHash)) {
                System.out.println("Skipping duplicate page " + (i + 1) + " in markscheme for " +
                        question.getYear() + "_" + question.getQuestionNumber());
                continue;
            }

            seenPageContent.add(pageHash);

            // Add this valid page
            PDPage originalPage = document.getPage(i);
            processedDoc.importPage(originalPage);
        }
    }

    private int countValidPages(PDDocument document) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        Set<String> seenPageContent = new HashSet<>();
        int validPages = 0;

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            textStripper.setStartPage(i + 1);
            textStripper.setEndPage(i + 1);
            String pageText = textStripper.getText(document).trim();

            // Skip empty pages
            if (pageText.length() < 10) {
                continue;
            }

            // Skip duplicate pages
            String pageHash = Integer.toString(pageText.hashCode());
            if (seenPageContent.contains(pageHash)) {
                continue;
            }

            seenPageContent.add(pageHash);
            validPages++;
        }

        return validPages + 1;
    }
}