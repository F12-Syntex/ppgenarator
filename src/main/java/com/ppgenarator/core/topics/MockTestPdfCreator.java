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
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import com.ppgenarator.utils.FormattingUtils;
import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class MockTestPdfCreator {

    private int questionCounter = 1;
    private MarkschemeCreator markschemeCreator;

    public MockTestPdfCreator() {
        this.markschemeCreator = new MarkschemeCreator();
    }

    public void createMockTestPdfs(List<Question> questions, File mockTestDir, File coverPageFile) {
        try {
            // Reset question counter
            questionCounter = 1;

            // Create questions PDF with cover page
            PDFMergerUtility questionsMerger = new PDFMergerUtility();
            questionsMerger.setDestinationFileName(new File(mockTestDir, "mock.pdf").getAbsolutePath());

            // Add cover page with headers
            File processedCoverPage = addHeadersToPdf(coverPageFile, null, 0);
            questionsMerger.addSource(processedCoverPage);

            // Check if we need to include any extracts
            Set<File> extractFiles = findRequiredExtracts(questions);

            // Add extracts with headers
            List<File> tempExtractFiles = addExtractsWithHeaders(extractFiles, questionsMerger);

            // Add questions with headers
            List<File> tempQuestionFiles = addQuestionsWithHeaders(questions, questionsMerger);

            // Create markschemes PDF for this mock
            markschemeCreator.createMockTestMarkscheme(questions, mockTestDir);

            // Merge the questions PDF
            if (!tempQuestionFiles.isEmpty() || processedCoverPage != null) {
                questionsMerger.mergeDocuments(null);
                System.out.println("Created mock PDF: " + new File(mockTestDir, "mock.pdf").getAbsolutePath());
            }

            // Clean up temporary files
            cleanupTempFiles(tempQuestionFiles, tempExtractFiles, coverPageFile, processedCoverPage);

        } catch (IOException e) {
            System.err.println("Error creating mock test PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Set<File> findRequiredExtracts(List<Question> questions) {
        Set<File> extractFiles = new HashSet<>();

        for (Question question : questions) {
            if (QuestionUtils.isQuestion6(question.getQuestionNumber())) {
                File extractFile = findExtractFile(question);
                if (extractFile != null && extractFile.exists()) {
                    extractFiles.add(extractFile);
                }
            }
        }

        return extractFiles;
    }

    private File findExtractFile(Question question) {
        if (question.getQuestion() == null || !question.getQuestion().exists()) {
            return null;
        }

        try {
            File questionFile = question.getQuestion();
            File paperDir = questionFile.getParentFile();

            if (paperDir != null) {
                File extractFile = new File(paperDir, "extract.pdf");
                if (extractFile.exists()) {
                    return extractFile;
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println(
                    "Error finding extract file for question " + question.getQuestionNumber() + ": " + e.getMessage());
            return null;
        }
    }

    private List<File> addExtractsWithHeaders(Set<File> extractFiles, PDFMergerUtility questionsMerger)
            throws IOException {
        List<File> tempExtractFiles = new ArrayList<>();

        for (File extractFile : extractFiles) {
            if (extractFile.exists()) {
                // Add headers to extract pages
                File processedExtract = addHeadersToPdf(extractFile, null, 0);
                questionsMerger.addSource(processedExtract);
                tempExtractFiles.add(processedExtract);
                System.out.println("Added extract with headers: " + extractFile.getName());
            }
        }

        return tempExtractFiles;
    }

    private List<File> addQuestionsWithHeaders(List<Question> questions, PDFMergerUtility questionsMerger)
            throws IOException {
        List<File> tempQuestionFiles = new ArrayList<>();

        for (Question question : questions) {
            if (question.getQuestion() != null && question.getQuestion().exists()) {
                // Process the question PDF to add headers
                File processedQuestionFile = addHeadersToPdf(question.getQuestion(), question, questionCounter);

                if (processedQuestionFile != null) {
                    questionsMerger.addSource(processedQuestionFile);
                    tempQuestionFiles.add(processedQuestionFile);
                    questionCounter++;
                }
            }
        }

        return tempQuestionFiles;
    }

    private File addHeadersToPdf(File pdfFile, Question question, int questionNumber) {
        try {
            // Load the original document
            PDDocument originalDoc = PDDocument.load(pdfFile);

            // Create a new document for the output
            PDDocument newDoc = new PDDocument();

            // Create header text
            String headerText = createHeaderText(question, questionNumber);
            boolean isQuestionPage = (question != null);

            // Process each page
            for (int i = 0; i < originalDoc.getNumberOfPages(); i++) {
                PDPage originalPage = originalDoc.getPage(i);

                // Create new page with same dimensions
                PDPage newPage = new PDPage(originalPage.getMediaBox());
                newDoc.addPage(newPage);

                // First, copy the original page content
                copyPageContent(originalDoc, newDoc, originalPage, newPage);

                // Then add the header on top
                addSimpleHeader(newDoc, newPage, headerText, isQuestionPage);
            }

            // Save to temporary file
            File tempFile = File.createTempFile("header_", ".pdf");
            newDoc.save(tempFile);

            // Clean up
            originalDoc.close();
            newDoc.close();

            return tempFile;

        } catch (Exception e) {
            System.err.println("Error adding headers: " + e.getMessage());
            e.printStackTrace();
            return pdfFile; // Return original if failed
        }
    }

    private String createHeaderText(Question question, int questionNumber) {
        if (question != null) {
            // For question pages
            String paperType = question.getPaperIdentifier();
            String month = QuestionUtils.getFormattedMonth(question).toLowerCase();
            String originalQuestionNumber = FormattingUtils.formatOriginalQuestionNumber(question.getQuestionNumber());

            return String.format("Question %d ( paper %s %s %s Q%s )",
                    questionNumber, paperType, month, question.getYear(), originalQuestionNumber);
        } else {
            // For cover page and extracts
            return "Best Tutors - Mock Test";
        }
    }

    private void copyPageContent(PDDocument sourceDoc, PDDocument targetDoc, PDPage sourcePage, PDPage targetPage) {
        try {
            // Import the page content using PDFBox's built-in functionality
            if (sourcePage.getContents() != null) {
                targetPage.setContents(
                        new org.apache.pdfbox.pdmodel.common.PDStream(targetDoc, sourcePage.getContents()));
            }
            targetPage.setResources(sourcePage.getResources());
            targetPage.setRotation(sourcePage.getRotation());

            // Copy any annotations if present
            if (sourcePage.getAnnotations() != null) {
                for (int j = 0; j < sourcePage.getAnnotations().size(); j++) {
                    targetPage.getAnnotations().add(sourcePage.getAnnotations().get(j));
                }
            }

        } catch (Exception e) {
            System.err.println("Error copying page content: " + e.getMessage());
        }
    }

    private void addSimpleHeader(PDDocument document, PDPage page, String headerText, boolean isQuestionPage) {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true)) {

            // Get page dimensions
            PDRectangle pageBox = page.getMediaBox();
            float pageWidth = pageBox.getWidth();
            float pageHeight = pageBox.getHeight();

            // Simple, fixed positioning
            if (isQuestionPage) {
                // Question header - top left, black, bold
                addQuestionHeader(contentStream, headerText, pageWidth, pageHeight);
            } else {
                // Cover/extract header - top right, gray, normal
                addCoverHeader(contentStream, headerText, pageWidth, pageHeight);
            }

        } catch (Exception e) {
            System.err.println("Error adding header to page: " + e.getMessage());
        }
    }

    private void addQuestionHeader(PDPageContentStream contentStream, String headerText,
            float pageWidth, float pageHeight) throws IOException {

        // Fixed positioning values
        final float LEFT_MARGIN = 20f;
        final float TOP_MARGIN = 15f;
        final float FONT_SIZE = 9f;

        // Position calculation
        float x = LEFT_MARGIN;
        float y = pageHeight - TOP_MARGIN;

        // Truncate text if too long
        String displayText = truncateText(headerText, FONT_SIZE, pageWidth - (2 * LEFT_MARGIN));

        // Set up text
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
        contentStream.setNonStrokingColor(0f, 0f, 0f); // Black
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(displayText);
        contentStream.endText();

        // Add underline
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(displayText) / 1000f * FONT_SIZE;
        contentStream.setLineWidth(0.5f);
        contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
        contentStream.moveTo(x, y - 2);
        contentStream.lineTo(x + textWidth, y - 2);
        contentStream.stroke();

        System.out.println("Added question header: " + displayText);
    }

    private void addCoverHeader(PDPageContentStream contentStream, String headerText,
            float pageWidth, float pageHeight) throws IOException {

        // Fixed positioning values
        final float RIGHT_MARGIN = 20f;
        final float TOP_MARGIN = 15f;
        final float FONT_SIZE = 8f;

        // Calculate position
        float textWidth = PDType1Font.HELVETICA.getStringWidth(headerText) / 1000f * FONT_SIZE;
        float x = pageWidth - RIGHT_MARGIN - textWidth;
        float y = pageHeight - TOP_MARGIN;

        // Set up text
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f); // Gray
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(headerText);
        contentStream.endText();
    }

    private String truncateText(String text, float fontSize, float maxWidth) {
        try {
            float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000f * fontSize;

            if (textWidth <= maxWidth) {
                return text;
            }

            // Truncate and add ellipsis
            String truncated = text;
            while (textWidth > maxWidth && truncated.length() > 10) {
                truncated = truncated.substring(0, truncated.length() - 1);
                textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(truncated + "...") / 1000f * fontSize;
            }

            return truncated + "...";

        } catch (Exception e) {
            return text; // Return original if calculation fails
        }
    }

    private void cleanupTempFiles(List<File> tempQuestionFiles, List<File> tempExtractFiles,
            File coverPageFile, File processedCoverPage) {
        // Clean up temporary question files
        for (File tempFile : tempQuestionFiles) {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        // Clean up temporary extract files
        for (File tempFile : tempExtractFiles) {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        // Delete the original cover page file
        if (coverPageFile.exists()) {
            coverPageFile.delete();
        }

        // Delete the processed cover page file
        if (processedCoverPage != null && processedCoverPage.exists()) {
            processedCoverPage.delete();
        }
    }
}