package com.ppgenarator.ai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessor {

    /**
     * Extract text content from a PDF file
     * 
     * @param pdfFile The PDF file to extract text from
     * @return The extracted text
     */
    public String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF file: " + pdfFile.getName());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Clean question text by removing unnecessary content
     * 
     * @param text The text to clean
     * @return Cleaned text
     */
    public String cleanQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove page numbers/identifiers
        text = text.replaceAll("\\*P\\d+A\\d+\\*", "");

        // Remove "DO NOT WRITE IN THIS AREA" and variations
        text = text.replaceAll(
                "D\\s*O\\s*N\\s*O\\s*T\\s*W\\s*R\\s*I\\s*T\\s*E\\s*I\\s*N\\s*T\\s*H\\s*I\\s*S\\s*A\\s*R\\s*E\\s*A", "");

        // Remove repeated dots (common in exam papers for fill-in spaces)
        text = text.replaceAll("\\.{2,}", " ");
        text = text.replaceAll("\\. \\.", " ");
        text = text.replaceAll("  \\.", " ");

        // Remove repeated "PMT" markings
        text = text.replaceAll("\\bPMT\\b", "");

        // Remove unicode placeholder characters
        text = text.replaceAll("\\?+", "");

        // Remove page numbers and headers/footers
        text = text.replaceAll("(?m)^\\d+$", "");
        text = text.replaceAll("(?i)\\bP\\d+\\b", "");
        text = text.replaceAll("(?i)turn over", "");
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?i)continue on the next page", "");

        // Remove question numbering and marks information
        text = text.replaceAll("\\(Total for Question \\d+:? \\d+ marks?\\)", "");
        text = text.replaceAll("\\(Total for Question \\d+ = \\d+ marks?\\)", "");
        text = text.replaceAll("TOTAL FOR SECTION [A-Z] = \\d+ MARKS", "");
        text = text.replaceAll("\\(\\d+ marks?\\)", "");

        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }

    /**
     * Remove phrases that could mislead topic identification
     */
    public String removeIgnorePhrases(String questionText) {
        if (questionText == null) {
            return "";
        }

        String cleanedText = questionText;
        for (String phrase : TopicConstants.IGNORE_PHRASES) {
            // Create a pattern that handles case insensitivity and allows some variation
            Pattern pattern = Pattern.compile(phrase, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleanedText);

            // Replace with a neutral phrase that maintains readability
            cleanedText = matcher.replaceAll("for this question");
        }

        return cleanedText;
    }

    /**
     * Count occurrences of a keyword in a text
     */
    public int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}