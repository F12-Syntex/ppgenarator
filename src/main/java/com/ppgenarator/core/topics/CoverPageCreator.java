package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.ppgenarator.utils.FormattingUtils;
import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class CoverPageCreator {

    private static final float MARGIN = 50;
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_NORMAL = PDType1Font.HELVETICA;

    public File createCoverPage(int mockTestNumber,
                                List<Question> questions,
                                int totalMarks,
                                int estimatedMinutes,
                                String qualification,
                                String topic,
                                File mockTestDir) throws IOException {

        File coverPageFile = new File(mockTestDir, "cover_page.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawCoverPage(cs, page, questions, totalMarks, estimatedMinutes, topic);
            }

            document.save(coverPageFile);
        }

        return coverPageFile;
    }

    private void drawCoverPage(PDPageContentStream cs,
                               PDPage page,
                               List<Question> questions,
                               int totalMarks,
                               int estimatedMinutes,
                               String unitOrTopic) throws IOException {

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - 60;

        // Border
        cs.setLineWidth(1.2f);
        cs.addRect(MARGIN - 20, MARGIN - 20,
                pageWidth - (2 * MARGIN) + 40,
                pageHeight - (2 * MARGIN) + 40);
        cs.stroke();

        // Title
        cs.beginText();
        cs.setFont(FONT_BOLD, 28);
        centerText(cs, "BEST TUTORS", pageWidth, y);
        cs.endText();
        y -= 50;

        // Name, Ref, Date lines (no "Student Information")
        drawLineWithLabel(cs, MARGIN, y, "Name:", 250);
        drawLineWithLabel(cs, pageWidth - 200, y, "Ref:", 120);
        y -= 40;

        drawLineWithLabel(cs, MARGIN, y, "Date:", 200);
        y -= 60;

        // Mock Title (just "Theme X Mock Test")
        cs.beginText();
        cs.setFont(FONT_BOLD, 16);
        centerText(cs, unitOrTopic + " Mock Test", pageWidth, y);
        cs.endText();
        y -= 50;

        // Stats Boxes
        float boxWidth = 140, boxHeight = 70, spacing = 40;
        float totalWidth = (3 * boxWidth) + (2 * spacing);
        float startX = (pageWidth - totalWidth) / 2;
        String[] headers = {"Questions", "Total Marks", "Time Allowed"};
        String[] values = {
                String.valueOf(questions.size()),
                String.valueOf(totalMarks),
                estimatedMinutes + " min"
        };

        for (int i = 0; i < 3; i++) {
            float x = startX + i * (boxWidth + spacing);
            drawBoxWithHeader(cs, x, y, boxWidth, boxHeight, headers[i], values[i]);
        }
        y -= (boxHeight + 60);

        // Table Header
        float tableX = MARGIN;
        float tableWidth = pageWidth - 2 * MARGIN;
        float rowHeight = 22;
        drawTableHeader(cs, tableX, y, tableWidth, rowHeight, "Question", "Marks");
        y -= rowHeight;

        // Question Rows
        for (Question q : questions) {
            // Simplified style: "paper 2 June 2020 Q6a"
            String ref = String.format("paper %s %s %s Q%s",
                    q.getPaperIdentifier().replaceAll("[^0-9]", ""), // paper number
                    QuestionUtils.getFormattedMonth(q),
                    q.getYear(),
                    FormattingUtils.formatOriginalQuestionNumber(q.getQuestionNumber()));

            drawTableRow(cs, tableX, y, tableWidth, rowHeight, ref,
                    "/ " + q.getMarks(), true);
            y -= rowHeight;
        }

        // Total Row
        drawTableRow(cs, tableX, y, tableWidth, rowHeight, "Total", "/ " + totalMarks, true);
    }

    // === Helper Drawing Methods ===

    private void centerText(PDPageContentStream cs, String text, float pageWidth, float y) throws IOException {
        float textWidth = FONT_BOLD.getStringWidth(text) / 1000 * 16;
        cs.newLineAtOffset((pageWidth - textWidth) / 2, y);
        cs.showText(text);
    }

    private void drawLineWithLabel(PDPageContentStream cs, float x, float y,
                                   String label, float width) throws IOException {
        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.moveTo(x + 50, y - 3);
        cs.lineTo(x + width, y - 3);
        cs.stroke();
    }

    private void drawBoxWithHeader(PDPageContentStream cs,
                                   float x, float y,
                                   float width, float height,
                                   String header, String value) throws IOException {
        cs.addRect(x, y - height, width, height);
        cs.stroke();

        // Header
        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        float headerWidth = FONT_BOLD.getStringWidth(header) / 1000 * 12;
        cs.newLineAtOffset(x + (width - headerWidth) / 2, y - 18);
        cs.showText(header);
        cs.endText();

        // Value
        cs.beginText();
        cs.setFont(FONT_BOLD, 20);
        float valWidth = FONT_BOLD.getStringWidth(value) / 1000 * 20;
        cs.newLineAtOffset(x + (width - valWidth) / 2, y - 47);
        cs.showText(value);
        cs.endText();
    }

    private void drawTableHeader(PDPageContentStream cs,
                                 float x, float y,
                                 float width, float height,
                                 String col1, String col2) throws IOException {
        float col2Width = 80;
        float col1Width = width - col2Width;

        cs.addRect(x, y - height, width, height);
        cs.stroke();

        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        cs.newLineAtOffset(x + 5, y - 15);
        cs.showText(col1);
        cs.endText();

        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        cs.newLineAtOffset(x + col1Width + 5, y - 15);
        cs.showText(col2);
        cs.endText();
    }

    private void drawTableRow(PDPageContentStream cs,
                              float x, float y,
                              float width, float height,
                              String col1, String col2,
                              boolean bold) throws IOException {
        float col2Width = 80;
        float col1Width = width - col2Width;

        cs.addRect(x, y - height, width, height);
        cs.stroke();

        PDFont font = bold ? FONT_BOLD : FONT_NORMAL;

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(x + 5, y - 15);
        cs.showText(col1);
        cs.endText();

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(x + col1Width + 5, y - 15);
        cs.showText(col2);
        cs.endText();
    }
}