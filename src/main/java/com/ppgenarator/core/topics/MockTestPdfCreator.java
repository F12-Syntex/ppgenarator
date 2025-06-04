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
            // Reset question counter for each mock test
            questionCounter = 1;

            // Create questions PDF with cover page
            PDFMergerUtility questionsMerger = new PDFMergerUtility();
            questionsMerger.setDestinationFileName(new File(mockTestDir, "mock.pdf").getAbsolutePath());

            // Add cover page with headers to all pages
            File processedCoverPage = addHeadersToPdf(coverPageFile, null, 0);
            questionsMerger.addSource(processedCoverPage);

            // Check if we need to include any extracts
            Set<File> extractFiles = findRequiredExtracts(questions);

            // Add extracts with headers
            List<File> tempExtractFiles = addExtractsWithHeaders(extractFiles, questionsMerger);

            // Add questions with headers
            List<File> tempQuestionFiles = addQuestionsWithHeaders(questions, questionsMerger);

            // Create markschemes PDF for mock test
            markschemeCreator.createMockTestMarkscheme(questions, mockTestDir);

            // Merge the questions PDF
            if (!tempQuestionFiles.isEmpty() || processedCoverPage != null) {
                questionsMerger.mergeDocuments(null);
                System.out.println("Created mock test PDF: " + new File(mockTestDir, "mock.pdf").getAbsolutePath());
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
            PDDocument document = PDDocument.load(pdfFile);

            // Create header text based on context
            String headerText;
            boolean isQuestionPage = false;

            if (question != null) {
                // For question pages - format: "Question 1 ( paper 2 june 2018 Q3 )"
                String paperType = question.getPaperIdentifier();
                String month = QuestionUtils.getFormattedMonth(question).toLowerCase();
                String originalQuestionNumber = FormattingUtils
                        .formatOriginalQuestionNumber(question.getQuestionNumber());

                headerText = String.format("Question %d ( paper %s %s %s Q%s )",
                        questionNumber,
                        paperType,
                        month,
                        question.getYear(),
                        originalQuestionNumber);
                isQuestionPage = true;
            } else {
                // For cover page and extracts - use discrete header
                headerText = "Best Tutors - Mock Test";
                isQuestionPage = false;
            }

            // Add header to each page
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.PREPEND, false)) {

                    addHeaderToPage(contentStream, page, headerText, isQuestionPage);
                }
            }

            // Save to temporary file
            File tempFile = File.createTempFile("processed_pdf_", ".pdf");
            document.save(tempFile);
            document.close();

            return tempFile;

        } catch (IOException e) {
            System.err.println("Error adding headers to PDF: " + e.getMessage());
            return null;
        }
    }

    private void addHeaderToPage(PDPageContentStream contentStream, PDPage page, String headerText,
            boolean isQuestionPage) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        if (isQuestionPage) {
            // Bold, prominent header for question pages
            float margin = 20;
            float topMargin = 20;

            contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE)); // Black
                                                                                                              // color
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12); // Bold font, larger size

            // Center-align the text
            float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(headerText) / 1000 * 12;
            float textX = (pageWidth - textWidth) / 2; // Center horizontally
            float textY = pageHeight - topMargin - 12;

            contentStream.newLineAtOffset(textX, textY);
            contentStream.showText(headerText);
            contentStream.endText();
        } else {
            // Discrete header for cover page and extracts (original style)
            float margin = 10;
            float topMargin = 10;

            contentStream.setNonStrokingColor(new PDColor(new float[] { 0.5f, 0.5f, 0.5f }, PDDeviceRGB.INSTANCE)); // Gray
                                                                                                                    // color
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 9); // Regular font, smaller size

            // Right-align the text
            float textWidth = PDType1Font.HELVETICA.getStringWidth(headerText) / 1000 * 9;
            float textX = pageWidth - margin - textWidth;
            float textY = pageHeight - topMargin - 9;

            contentStream.newLineAtOffset(textX, textY);
            contentStream.showText(headerText);
            contentStream.endText();

            // Reset color
            contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
        }
    }

    private void addHeaderToPage(PDPageContentStream contentStream, PDPage page, String headerText) throws IOException {
        // Calculate positions - place header prominently at the top center
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 20;
        float topMargin = 20;

        // Bold, clear text header at top center
        contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE)); // Black color
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12); // Bold font, larger size

        // Center-align the text
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(headerText) / 1000 * 12;
        float textX = (pageWidth - textWidth) / 2; // Center horizontally
        float textY = pageHeight - topMargin - 12;

        contentStream.newLineAtOffset(textX, textY);
        contentStream.showText(headerText);
        contentStream.endText();
    }

    private void cleanupTempFiles(List<File> tempQuestionFiles, List<File> tempExtractFiles,
            File coverPageFile, File processedCoverPage) {
        // Clean up temporary question files
        for (File tempFile : tempQuestionFiles) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }

        // Clean up temporary extract files
        for (File tempFile : tempExtractFiles) {
            if (tempFile.exists()) {
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