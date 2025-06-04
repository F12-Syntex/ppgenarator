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
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenarator.utils.FileUtils;
import com.ppgenarator.utils.FormattingUtils;
import com.ppgenerator.types.Question;

public class PdfMerger {

    private MarkschemeIndexCreator markschemeIndexCreator;

    public PdfMerger() {
        this.markschemeIndexCreator = new MarkschemeIndexCreator();
    }

    public void createMergedPdfs(List<Question> questions, File questionsDir, File markschemesDir) {
        try {
            // Sort questions by year and question number for consistent ordering
            questions.sort((q1, q2) -> {
                int yearCompare = q1.getYear().compareTo(q2.getYear());
                if (yearCompare != 0) {
                    return yearCompare;
                }
                return q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
            });

            // Create merged questions PDF
            createMergedQuestionsPdf(questions, questionsDir);

            // Create improved merged markschemes PDF with index
            createMergedMarkschemesPdf(questions, markschemesDir);

        } catch (IOException e) {
            System.err.println("Error creating merged PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMergedQuestionsPdf(List<Question> questions, File questionsDir) throws IOException {
        PDFMergerUtility questionsMerger = new PDFMergerUtility();
        questionsMerger.setDestinationFileName(new File(questionsDir, "questions.pdf").getAbsolutePath());

        List<File> tempQuestionFiles = new ArrayList<>();
        Set<String> questionMd5Hashes = new HashSet<>();
        boolean hasQuestions = false;

        for (Question question : questions) {
            if (question.getQuestion() != null && question.getQuestion().exists()) {
                String md5Hash = FileUtils.getFileMd5Hash(question.getQuestion());

                if (!questionMd5Hashes.contains(md5Hash)) {
                    questionMd5Hashes.add(md5Hash);

                    PDDocument document = PDDocument.load(question.getQuestion());
                    File tempQuestionFile = File.createTempFile("temp_question_", ".pdf");
                    document.save(tempQuestionFile);
                    document.close();

                    questionsMerger.addSource(tempQuestionFile);
                    tempQuestionFiles.add(tempQuestionFile);
                    hasQuestions = true;
                } else {
                    System.out.println("Skipping duplicate question: " + question.getYear() + "_" +
                            question.getQuestionNumber());
                }
            }
        }

        // Merge the questions PDF if there are any
        if (hasQuestions) {
            questionsMerger.mergeDocuments(null);
            System.out.println("Created merged questions PDF: " + questionsMerger.getDestinationFileName());
        }

        // Clean up temporary files
        for (File tempFile : tempQuestionFiles) {
            tempFile.delete();
        }
    }

    private void createMergedMarkschemesPdf(List<Question> questions, File markschemesDir) throws IOException {
        // Filter questions that have markschemes
        List<Question> questionsWithMarkschemes = new ArrayList<>();
        for (Question question : questions) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                questionsWithMarkschemes.add(question);
            }
        }

        if (questionsWithMarkschemes.isEmpty()) {
            System.out.println("No markschemes found to merge");
            return;
        }

        // Create the final markscheme PDF
        PDFMergerUtility markschemesMerger = new PDFMergerUtility();
        File markschemeOutputFile = new File(markschemesDir, "markscheme.pdf");
        markschemesMerger.setDestinationFileName(markschemeOutputFile.getAbsolutePath());

        // Create index page
        File indexPageFile = markschemeIndexCreator.createMarkschemeIndexPage(questionsWithMarkschemes, markschemesDir);
        markschemesMerger.addSource(indexPageFile);

        List<File> tempMarkSchemeFiles = new ArrayList<>();
        Set<String> processedMarkschemes = new HashSet<>();

        // Process each markscheme
        for (Question question : questionsWithMarkschemes) {
            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                String markschemeHash = FileUtils.getFileMd5Hash(question.getMarkScheme());

                // Skip duplicates
                if (processedMarkschemes.contains(markschemeHash)) {
                    System.out.println("Skipping duplicate markscheme: " +
                            question.getYear() + "_" + question.getQuestionNumber());
                    continue;
                }

                processedMarkschemes.add(markschemeHash);

                // Load and process the markscheme PDF
                File processedMarkscheme = processMarkscheme(question.getMarkScheme(), question);
                if (processedMarkscheme != null) {
                    markschemesMerger.addSource(processedMarkscheme);
                    tempMarkSchemeFiles.add(processedMarkscheme);

                    System.out.println("Added markscheme for " + question.getYear() + "_" +
                            question.getQuestionNumber());
                }
            }
        }

        // Merge all markschemes
        markschemesMerger.mergeDocuments(null);
        System.out.println("Created improved merged markschemes PDF: " + markschemeOutputFile.getAbsolutePath());

        // Clean up temporary files
        indexPageFile.delete();
        for (File tempFile : tempMarkSchemeFiles) {
            tempFile.delete();
        }
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

            // Draw decorative border
            contentStream.setLineWidth(2);
            contentStream.addRect(margin, margin, pageWidth - 2 * margin, pageHeight - 2 * margin);
            contentStream.stroke();

            // Add inner border for elegance
            float innerMargin = margin + 10;
            contentStream.setLineWidth(1);
            contentStream.addRect(innerMargin, innerMargin,
                    pageWidth - 2 * innerMargin, pageHeight - 2 * innerMargin);
            contentStream.stroke();

            // Header with background
            float headerY = pageHeight - margin - 50;
            float headerHeight = 60;

            // Background for header
            contentStream.setNonStrokingColor(new PDColor(new float[] { 0.95f, 0.95f, 0.95f }, PDDeviceRGB.INSTANCE));
            contentStream.addRect(innerMargin + 10, headerY - 10,
                    pageWidth - 2 * innerMargin - 20, headerHeight);
            contentStream.fill();

            // Reset to black for text
            contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));

            // Main header
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
            String header = "MARKSCHEME";
            float headerWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(header) / 1000 * 24;
            contentStream.newLineAtOffset((pageWidth - headerWidth) / 2, headerY + 20);
            contentStream.showText(header);
            contentStream.endText();

            // Subtitle
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 14);
            String subtitle = "Answer Guidelines and Mark Allocation";
            float subtitleWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(subtitle) / 1000 * 14;
            contentStream.newLineAtOffset((pageWidth - subtitleWidth) / 2, headerY);
            contentStream.showText(subtitle);
            contentStream.endText();

            // Question details section
            float detailsY = pageHeight - margin - 160;
            float boxHeight = 140;

            // Details box background
            contentStream.setNonStrokingColor(new PDColor(new float[] { 0.98f, 0.98f, 0.98f }, PDDeviceRGB.INSTANCE));
            contentStream.addRect(innerMargin + 20, detailsY - boxHeight,
                    pageWidth - 2 * innerMargin - 40, boxHeight);
            contentStream.fill();

            // Details box border
            contentStream.setStrokingColor(new PDColor(new float[] { 0.8f, 0.8f, 0.8f }, PDDeviceRGB.INSTANCE));
            contentStream.setLineWidth(1);
            contentStream.addRect(innerMargin + 20, detailsY - boxHeight,
                    pageWidth - 2 * innerMargin - 40, boxHeight);
            contentStream.stroke();

            // Reset colors for text
            contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
            contentStream.setStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));

            // Question details
            float yPos = detailsY - 30;
            float leftMargin = innerMargin + 40;

            // Question info
            QuestionDetail[] details = {
                    new QuestionDetail("Question:", FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                            true),
                    new QuestionDetail("Year:", question.getYear(), false),
                    new QuestionDetail("Exam Board:", question.getBoard().toString(), false),
                    new QuestionDetail("Total Marks:", String.valueOf(question.getMarks()), true)
            };

            for (QuestionDetail detail : details) {
                // Label
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, detail.important ? 16 : 14);
                contentStream.newLineAtOffset(leftMargin, yPos);
                contentStream.showText(detail.label);
                contentStream.endText();

                // Value
                contentStream.beginText();
                contentStream.setFont(detail.important ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA,
                        detail.important ? 16 : 14);
                contentStream.newLineAtOffset(leftMargin + 120, yPos);
                contentStream.showText(detail.value);
                contentStream.endText();

                yPos -= detail.important ? 35 : 25;
            }

            // Footer note
            float footerY = margin + 60;
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            String footerNote = "This markscheme provides detailed guidance for awarding marks. " +
                    "Please read all marking points carefully.";

            // Word wrap the footer
            String[] words = footerNote.split(" ");
            StringBuilder line = new StringBuilder();
            float lineY = footerY;

            for (String word : words) {
                if (line.length() == 0) {
                    line.append(word);
                } else {
                    String testLine = line + " " + word;
                    float testWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(testLine) / 1000 * 10;

                    if (testWidth < pageWidth - 2 * innerMargin - 80) {
                        line.append(" ").append(word);
                    } else {
                        // Print current line and start new one
                        float lineWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(line.toString()) / 1000 * 10;
                        contentStream.newLineAtOffset((pageWidth - lineWidth) / 2, lineY);
                        contentStream.showText(line.toString());
                        contentStream.endText();

                        line = new StringBuilder(word);
                        lineY -= 12;

                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                    }
                }
            }

            // Print last line
            if (line.length() > 0) {
                float lineWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(line.toString()) / 1000 * 10;
                contentStream.newLineAtOffset((pageWidth - lineWidth) / 2, lineY);
                contentStream.showText(line.toString());
            }
            contentStream.endText();
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

    // Helper class for question details
    private static class QuestionDetail {
        final String label;
        final String value;
        final boolean important;

        QuestionDetail(String label, String value, boolean important) {
            this.label = label;
            this.value = value;
            this.important = important;
        }
    }
}