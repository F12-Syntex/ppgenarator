package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
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

public class UnitMockCoverPageCreator {
    
    private static final float MARGIN = 50;
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_NORMAL = PDType1Font.HELVETICA;
    private static final PDFont FONT_OBLIQUE = PDType1Font.HELVETICA_OBLIQUE;
    
    public File createUnitMockCoverPage(int mockTestNumber,
                                        List<Question> questions,
                                        int totalMarks,
                                        int estimatedMinutes,
                                        int themeNumber,
                                        File mockTestDir) throws IOException {
        
        File coverPageFile = new File(mockTestDir, "cover_page.pdf");

        // sort questions by first topic for stable order
        questions.sort(Comparator.comparing(q -> {
            if (q.getTopics() != null && q.getTopics().length > 0) {
                return q.getTopics()[0];
            }
            return "";
        }));
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawMainCoverPage(cs, page, questions, totalMarks, estimatedMinutes, themeNumber, mockTestNumber);
            }
            
            document.save(coverPageFile);
        }
        
        return coverPageFile;
    }
    
    private void drawMainCoverPage(PDPageContentStream cs,
                                   PDPage page,
                                   List<Question> questions,
                                   int totalMarks,
                                   int estimatedMinutes,
                                   int themeNumber,
                                   int mockTestNumber) throws IOException {
        
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - 80;
        
        // Border
        cs.setLineWidth(1.2f);
        cs.addRect(MARGIN - 20, MARGIN - 20,
                pageWidth - (2 * MARGIN) + 40,
                pageHeight - (2 * MARGIN) + 40);
        cs.stroke();
        
        // Title
        cs.beginText();
        centerText(cs, "BEST TUTORS", FONT_BOLD, 28, pageWidth, y);
        cs.endText();
        y -= 60;
        
        // Name / Ref / Date lines
        drawLineWithLabel(cs, MARGIN, y, "Name:", 250);
        drawLineWithLabel(cs, pageWidth - 200, y, "Ref:", 120);
        y -= 40;
        drawLineWithLabel(cs, MARGIN, y, "Date:", 200);
        y -= 80;
        
        // Theme Title
        cs.beginText();
        String themeTitle = "Theme " + themeNumber + " Unit Test - Mock " + mockTestNumber;
        centerText(cs, themeTitle, FONT_BOLD, 18, pageWidth, y);
        cs.endText();
        y -= 40;
        
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
        y -= (boxHeight + 80);
        
        // Table Header
        float tableX = MARGIN;
        float tableWidth = pageWidth - 2 * MARGIN;
        float rowHeight = 22;
        drawTableHeader(cs, tableX, y, tableWidth, rowHeight, "Question", "Marks");
        y -= rowHeight;
        
        // Question rows
        for (Question q : questions) {
            String topicStr = "";
            if (q.getTopics() != null && q.getTopics().length > 0) {
                topicStr = "[" + trimTopic(q.getTopics()[0], 50) + "]";
            }
            String ref = String.format("paper %s %s %s Q%s %s",
                q.getPaperIdentifier().replaceAll("[^0-9]", ""),
                QuestionUtils.getFormattedMonth(q),
                q.getYear(),
                FormattingUtils.formatOriginalQuestionNumber(q.getQuestionNumber()),
                topicStr);
            
            drawTableRow(cs, tableX, y, tableWidth, rowHeight, ref, "/ " + q.getMarks(), false);
            y -= rowHeight;
        }
        
        // Total row
        drawTableRow(cs, tableX, y, tableWidth, rowHeight, "Total", "/ " + totalMarks, true);
    }
    
    // === Helpers ===
    
    private String trimTopic(String topic, int maxLen) {
        if (topic == null) return "";
        if (topic.length() <= maxLen) return topic;
        return topic.substring(0, maxLen - 3) + "...";
    }
    
    private void centerText(PDPageContentStream cs,
                           String text,
                           PDFont font,
                           float fontSize,
                           float pageWidth,
                           float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (pageWidth - textWidth) / 2;
        cs.setFont(font, fontSize);
        cs.setTextMatrix(1, 0, 0, 1, startX, y);
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
        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        float headerWidth = FONT_BOLD.getStringWidth(header) / 1000 * 12;
        cs.newLineAtOffset(x + (width - headerWidth) / 2, y - 18);
        cs.showText(header);
        cs.endText();
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