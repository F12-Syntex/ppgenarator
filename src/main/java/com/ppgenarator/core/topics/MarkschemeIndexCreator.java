package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class MarkschemeIndexCreator {

    public File createMarkschemeIndexPage(List<Question> questions, File markschemesDir) throws IOException {
        File indexPageFile = new File(markschemesDir, "index_page.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createIndexContent(contentStream, page, questions);
            }

            document.save(indexPageFile);
        }

        return indexPageFile;
    }

    private void createIndexContent(PDPageContentStream contentStream, PDPage page, List<Question> questions)
            throws IOException {
        float margin = 50;
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float yPosition = pageHeight - margin;

        // Create header
        yPosition = createIndexHeader(contentStream, pageWidth, yPosition, margin);

        // Create table
        createIndexTable(contentStream, questions, margin, pageWidth, yPosition);

        // Create footer
        createIndexFooter(contentStream, pageWidth, margin);
    }

    private float createIndexHeader(PDPageContentStream contentStream, float pageWidth, float yPosition, float margin)
            throws IOException {

        // Main title
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 26);
        String title = "MARKSCHEME INDEX";
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 26;
        contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
        contentStream.showText(title);
        contentStream.endText();

        yPosition -= 35;

        // Subtitle
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 12);
        String subtitle = "Quick Navigation Guide to Question Markschemes";
        float subtitleWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(subtitle) / 1000 * 12;
        contentStream.newLineAtOffset((pageWidth - subtitleWidth) / 2, yPosition);
        contentStream.showText(subtitle);
        contentStream.endText();

        yPosition -= 25;

        // Draw decorative line under the header
        contentStream.setLineWidth(3);
        contentStream.setStrokingColor(new PDColor(new float[] { 0.7f, 0.7f, 0.7f }, PDDeviceRGB.INSTANCE));
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(pageWidth - margin, yPosition);
        contentStream.stroke();

        // Reset stroke color
        contentStream.setStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));

        return yPosition - 30;
    }

    private void createIndexTable(PDPageContentStream contentStream, List<Question> questions,
            float margin, float pageWidth, float yPosition) throws IOException {

        // Table configuration
        float tableWidth = pageWidth - 2 * margin;
        float[] columnWidths = { 120, 80, 80, 80, 80 }; // Question, Year, Board, Marks, Page
        String[] headers = { "Question", "Year", "Board", "Marks", "Page" };

        // Calculate table height based on content
        int maxRows = Math.min(questions.size(), 25); // Limit to prevent overflow
        float rowHeight = 18;
        float headerHeight = 25;
        float tableHeight = headerHeight + (maxRows * rowHeight) + 10;

        // Draw table border
        contentStream.setLineWidth(2);
        contentStream.addRect(margin, yPosition - tableHeight, tableWidth, tableHeight);
        contentStream.stroke();

        // Create table header
        yPosition = createTableHeader(contentStream, headers, columnWidths, margin, yPosition, tableWidth,
                headerHeight);

        // Create table content
        createTableContent(contentStream, questions, columnWidths, margin, yPosition, rowHeight, maxRows);
    }

    private float createTableHeader(PDPageContentStream contentStream, String[] headers, float[] columnWidths,
            float margin, float yPosition, float tableWidth, float headerHeight)
            throws IOException {

        float headerY = yPosition - headerHeight + 5;

        // Header background
        contentStream.setNonStrokingColor(new PDColor(new float[] { 0.85f, 0.85f, 0.85f }, PDDeviceRGB.INSTANCE));
        contentStream.addRect(margin, headerY - 5, tableWidth, headerHeight);
        contentStream.fill();

        // Reset to black for text and borders
        contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));

        // Draw vertical lines for columns
        float currentX = margin;
        for (int i = 0; i <= columnWidths.length; i++) {
            contentStream.setLineWidth(1);
            contentStream.moveTo(currentX, yPosition);
            contentStream.lineTo(currentX, yPosition - headerHeight);
            contentStream.stroke();

            if (i < columnWidths.length) {
                currentX += columnWidths[i];
            }
        }

        // Draw horizontal line under header
        contentStream.moveTo(margin, headerY - 5);
        contentStream.lineTo(margin + tableWidth, headerY - 5);
        contentStream.stroke();

        // Add header text
        currentX = margin;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);

            // Center text in column
            float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(headers[i]) / 1000 * 11;
            float textX = currentX + (columnWidths[i] - textWidth) / 2;

            contentStream.newLineAtOffset(textX, headerY);
            contentStream.showText(headers[i]);
            contentStream.endText();

            currentX += columnWidths[i];
        }

        return yPosition - headerHeight;
    }

    private void createTableContent(PDPageContentStream contentStream, List<Question> questions,
            float[] columnWidths, float margin, float yPosition,
            float rowHeight, int maxRows) throws IOException {

        int currentPage = 2; // Start after index page
        Set<String> processedMarkschemes = new HashSet<>();
        float currentY = yPosition;
        int rowCount = 0;

        for (Question question : questions) {
            if (rowCount >= maxRows)
                break;

            if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                String markschemeHash = FileUtils.getFileMd5Hash(question.getMarkScheme());

                // Skip duplicates in the index
                if (processedMarkschemes.contains(markschemeHash)) {
                    continue;
                }
                processedMarkschemes.add(markschemeHash);

                // Create table row
                createTableRow(contentStream, question, columnWidths, margin, currentY, currentPage);

                // Draw horizontal line after each row
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(margin, currentY - rowHeight + 5);
                contentStream.lineTo(
                        margin + columnWidths[0] + columnWidths[1] + columnWidths[2] + columnWidths[3]
                                + columnWidths[4],
                        currentY - rowHeight + 5);
                contentStream.stroke();

                currentY -= rowHeight;
                rowCount++;

                // Calculate pages for this markscheme
                try (PDDocument markschemeDoc = PDDocument.load(question.getMarkScheme())) {
                    int validPages = countValidPages(markschemeDoc);
                    currentPage += validPages;
                }
            }
        }

        // Draw remaining vertical lines for the table
        float currentX = margin;
        for (int i = 0; i <= columnWidths.length; i++) {
            contentStream.setLineWidth(1);
            contentStream.moveTo(currentX, yPosition);
            contentStream.lineTo(currentX, currentY);
            contentStream.stroke();

            if (i < columnWidths.length) {
                currentX += columnWidths[i];
            }
        }
    }

    private void createTableRow(PDPageContentStream contentStream, Question question, float[] columnWidths,
            float margin, float yPosition, int currentPage) throws IOException {

        String[] rowData = {
                FormattingUtils.formatQuestionNumber(question.getQuestionNumber()),
                question.getYear(),
                question.getBoard().toString(),
                String.valueOf(question.getMarks()),
                String.valueOf(currentPage)
        };

        float currentX = margin;
        for (int i = 0; i < rowData.length; i++) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);

            // Center text in column for numbers, left-align for text
            float textWidth = PDType1Font.HELVETICA.getStringWidth(rowData[i]) / 1000 * 10;
            float textX;

            if (i == 2 || i == 3 || i == 4) { // Board, Marks, Page - center these
                textX = currentX + (columnWidths[i] - textWidth) / 2;
            } else { // Question and Year - left align with padding
                textX = currentX + 8;
            }

            contentStream.newLineAtOffset(textX, yPosition - 12);
            contentStream.showText(rowData[i]);
            contentStream.endText();

            currentX += columnWidths[i];
        }
    }

    private void createIndexFooter(PDPageContentStream contentStream, float pageWidth, float margin)
            throws IOException {
        float footerY = margin + 40;

        // Instructions box
        float boxWidth = pageWidth - 2 * margin - 40;
        float boxHeight = 35;
        float boxX = margin + 20;

        // Background for instructions
        contentStream.setNonStrokingColor(new PDColor(new float[] { 0.95f, 0.95f, 0.95f }, PDDeviceRGB.INSTANCE));
        contentStream.addRect(boxX, footerY - boxHeight, boxWidth, boxHeight);
        contentStream.fill();

        // Border for instructions
        contentStream.setStrokingColor(new PDColor(new float[] { 0.8f, 0.8f, 0.8f }, PDDeviceRGB.INSTANCE));
        contentStream.setLineWidth(1);
        contentStream.addRect(boxX, footerY - boxHeight, boxWidth, boxHeight);
        contentStream.stroke();

        // Reset colors
        contentStream.setNonStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));
        contentStream.setStrokingColor(new PDColor(new float[] { 0f, 0f, 0f }, PDDeviceRGB.INSTANCE));

        // Footer text
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        String footerTitle = "How to Use This Index:";
        contentStream.newLineAtOffset(boxX + 10, footerY - 15);
        contentStream.showText(footerTitle);
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        String instructions = "Use the page numbers in the rightmost column to quickly navigate to specific question markschemes.";
        contentStream.newLineAtOffset(boxX + 10, footerY - 28);
        contentStream.showText(instructions);
        contentStream.endText();
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

        return validPages + 1; // +1 for the header page we add
    }
}