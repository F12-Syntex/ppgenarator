package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;

import com.ppgenarator.utils.FormattingUtils;
import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class CoverPageCreator {

    private static final float MARGIN = 50;
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_NORMAL = PDType1Font.HELVETICA;

    public File createCoverPage(int mockTestNumber, List<Question> questions, int totalMarks,
            int estimatedMinutes, String qualification, String topic, File mockTestDir) throws IOException {

        File coverPageFile = new File(mockTestDir, "cover_page.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createSimpleCoverPage(contentStream, page, questions, totalMarks, estimatedMinutes,
                        qualification, topic, false, mockTestNumber);
            }

            document.save(coverPageFile);
        }
        return coverPageFile;
    }

    public File createQ1To5CoverPage(int mockTestNumber, List<Question> questions, int totalMarks,
            int estimatedMinutes, String qualification, String topic, File mockTestDir) throws IOException {

        File coverPageFile = new File(mockTestDir, "cover_page.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createSimpleCoverPage(contentStream, page, questions, totalMarks, estimatedMinutes,
                        qualification, topic, true, mockTestNumber);
            }

            document.save(coverPageFile);
        }
        return coverPageFile;
    }

    private void createSimpleCoverPage(PDPageContentStream contentStream, PDPage page, List<Question> questions,
            int totalMarks, int estimatedMinutes, String qualification, String topic,
            boolean isQ1To5Only, int mockTestNumber) throws IOException {

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float yPosition = pageHeight - MARGIN;

        // Simple border
        contentStream.setLineWidth(1.5f);
        contentStream.addRect(MARGIN - 20, MARGIN - 20, pageWidth - (2 * MARGIN) + 40, pageHeight - (2 * MARGIN) + 40);
        contentStream.stroke();

        yPosition -= 20;

        // Header
        yPosition = createHeader(contentStream, pageWidth, yPosition);
        yPosition -= 40;

        // Student information section
        yPosition = createStudentSection(contentStream, pageWidth, yPosition);
        yPosition -= 80;

        // Subject information
        yPosition = createSubjectInfo(contentStream, pageWidth, yPosition, qualification, topic);
        yPosition -= 60;

        // Test type (if Q1-5)
        // if (isQ1To5Only) {
        // yPosition = createTestType(contentStream, pageWidth, yPosition);
        // yPosition -= 60;
        // }

        // Test details (improved boxes)
        yPosition = createTestDetails(contentStream, pageWidth, yPosition, questions.size(),
                totalMarks, estimatedMinutes);
        yPosition -= 100;

        // Instructions
        createInstructions(contentStream, pageWidth, yPosition);
    }

    private float createHeader(PDPageContentStream contentStream, float pageWidth, float yPosition) throws IOException {

        // Main title only
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 32);
        String title = "BEST TUTORS";
        float titleWidth = FONT_BOLD.getStringWidth(title) / 1000 * 32;
        contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
        contentStream.showText(title);
        contentStream.endText();

        return yPosition - 40;
    }

    private float createSubjectInfo(PDPageContentStream contentStream, float pageWidth, float yPosition,
            String qualification, String topic) throws IOException {

        // Subject
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 18);
        String subjectText = "Subject: " + FormattingUtils.formatTopicName(topic);
        float subjectWidth = FONT_BOLD.getStringWidth(subjectText) / 1000 * 18;
        contentStream.newLineAtOffset((pageWidth - subjectWidth) / 2, yPosition);
        contentStream.showText(subjectText);
        contentStream.endText();

        // Qualification
        contentStream.beginText();
        contentStream.setFont(FONT_NORMAL, 14);
        String qualText = "Qualification: " + FormattingUtils.formatQualificationName(qualification);
        float qualWidth = FONT_NORMAL.getStringWidth(qualText) / 1000 * 14;
        contentStream.newLineAtOffset((pageWidth - qualWidth) / 2, yPosition - 30);
        contentStream.showText(qualText);
        contentStream.endText();

        return yPosition - 40;
    }

    private float createTestType(PDPageContentStream contentStream, float pageWidth, float yPosition)
            throws IOException {

        // Box for test type
        float boxWidth = 300;
        float boxHeight = 40;
        float boxX = (pageWidth - boxWidth) / 2;

        contentStream.setLineWidth(2f);
        contentStream.addRect(boxX, yPosition - boxHeight, boxWidth, boxHeight);
        contentStream.stroke();

        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 16);
        String testType = "Foundation Test (Questions 1-5)";
        float textWidth = FONT_BOLD.getStringWidth(testType) / 1000 * 16;
        contentStream.newLineAtOffset((pageWidth - textWidth) / 2, yPosition - 25);
        contentStream.showText(testType);
        contentStream.endText();

        return yPosition - 50;
    }

    private float createTestDetails(PDPageContentStream contentStream, float pageWidth, float yPosition,
            int questionCount, int totalMarks, int estimatedMinutes) throws IOException {

        float boxWidth = 150;
        float boxHeight = 80;
        float spacing = 30;
        float totalWidth = (3 * boxWidth) + (2 * spacing);
        float startX = (pageWidth - totalWidth) / 2;

        String[] labels = { "Questions", "Total Marks", "Time Allowed" };
        String[] values = { String.valueOf(questionCount), String.valueOf(totalMarks), estimatedMinutes + " min" };

        for (int i = 0; i < 3; i++) {
            float boxX = startX + (i * (boxWidth + spacing));

            // Draw box
            contentStream.setLineWidth(2f);
            contentStream.addRect(boxX, yPosition - boxHeight, boxWidth, boxHeight);
            contentStream.stroke();

            // Label
            contentStream.beginText();
            contentStream.setFont(FONT_BOLD, 12);
            float labelWidth = FONT_BOLD.getStringWidth(labels[i]) / 1000 * 12;
            contentStream.newLineAtOffset(boxX + (boxWidth - labelWidth) / 2, yPosition - 20);
            contentStream.showText(labels[i]);
            contentStream.endText();

            // Value (larger)
            contentStream.beginText();
            contentStream.setFont(FONT_BOLD, 24);
            float valueWidth = FONT_BOLD.getStringWidth(values[i]) / 1000 * 24;
            contentStream.newLineAtOffset(boxX + (boxWidth - valueWidth) / 2, yPosition - 55);
            contentStream.showText(values[i]);
            contentStream.endText();
        }

        return yPosition - boxHeight;
    }

    private float createStudentSection(PDPageContentStream contentStream, float pageWidth, float yPosition)
            throws IOException {

        // Student Information title
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 14);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Student Information:");
        contentStream.endText();

        // Simple underline
        contentStream.setLineWidth(1f);
        contentStream.moveTo(MARGIN, yPosition - 5);
        contentStream.lineTo(MARGIN + 140, yPosition - 5);
        contentStream.stroke();

        yPosition -= 30;

        // Name section
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 12);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Name:");
        contentStream.endText();

        contentStream.setLineWidth(1f);
        contentStream.moveTo(MARGIN + 50, yPosition - 3);
        contentStream.lineTo(pageWidth - 150, yPosition - 3);
        contentStream.stroke();

        // Reference section
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 12);
        contentStream.newLineAtOffset(pageWidth - 130, yPosition);
        contentStream.showText("Ref:");
        contentStream.endText();

        contentStream.moveTo(pageWidth - 100, yPosition - 3);
        contentStream.lineTo(pageWidth - MARGIN, yPosition - 3);
        contentStream.stroke();

        // Date section
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 12);
        contentStream.newLineAtOffset(MARGIN, yPosition - 30);
        contentStream.showText("Date:");
        contentStream.endText();

        contentStream.moveTo(MARGIN + 50, yPosition - 33);
        contentStream.lineTo(MARGIN + 150, yPosition - 33);
        contentStream.stroke();

        return yPosition - 40;
    }

    private void createInstructions(PDPageContentStream contentStream, float pageWidth, float yPosition)
            throws IOException {

        // Instructions header
        contentStream.beginText();
        contentStream.setFont(FONT_BOLD, 14);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Instructions:");
        contentStream.endText();

        // Simple underline
        contentStream.setLineWidth(1f);
        contentStream.moveTo(MARGIN, yPosition - 5);
        contentStream.lineTo(MARGIN + 90, yPosition - 5);
        contentStream.stroke();

        // Instructions list
        String[] instructions = {
                "• Write your name and reference number clearly in the spaces above",
                "• Read all questions carefully before answering",
                "• Answer ALL questions in the spaces provided",
                "• Show all working where applicable",
                "• Mobile phones and calculators are not permitted unless stated"
        };

        float instructionY = yPosition - 25;
        for (String instruction : instructions) {
            contentStream.beginText();
            contentStream.setFont(FONT_NORMAL, 10);
            contentStream.newLineAtOffset(MARGIN, instructionY);
            contentStream.showText(instruction);
            contentStream.endText();
            instructionY -= 15;
        }
    }
}