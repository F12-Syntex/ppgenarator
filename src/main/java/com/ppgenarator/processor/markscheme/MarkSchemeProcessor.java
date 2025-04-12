package com.ppgenarator.processor.markscheme;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenarator.config.Configuration;

public class MarkSchemeProcessor {

    private File markSchemeFilePath;

    public MarkSchemeProcessor(File markSchemeFilePath) {
        this.markSchemeFilePath = markSchemeFilePath;
    }

    public void process() {
        try {
            PDDocument document = PDDocument.load(markSchemeFilePath);
            
            // Create output directory
            File outputDir = new File(Configuration.OUTPUT_DIRECTORY,
                    markSchemeFilePath.getParentFile().getParentFile().getName());
            outputDir.mkdirs();
            
            // Create a directory for mark scheme
            File markSchemeDir = new File(outputDir, "markscheme");
            markSchemeDir.mkdirs();
            
            // Find all questions in the document
            Map<String, List<Integer>> questionPages = findQuestionPages(document);
            
            // Extract each question to its own PDF
            for (String questionNumber : questionPages.keySet()) {
                List<Integer> pages = questionPages.get(questionNumber);
                
                // Create filename for this question
                String filename = "ms_" + questionNumber.replace("(", "_").replace(")", "") + ".pdf";
                File outputFile = new File(markSchemeDir, filename);
                
                // Extract all pages for this question
                extractPages(document, pages, outputFile);
                
                System.out.println("Created mark scheme file: " + outputFile.getName() + 
                                  " with " + pages.size() + " pages");
            }
            
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Map<String, List<Integer>> findQuestionPages(PDDocument document) throws IOException {
        Map<String, List<Integer>> questionPages = new HashMap<>();
        PDFTextStripper stripper = new PDFTextStripper();
        
        // Process each page to find questions
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            stripper.setStartPage(pageNum + 1);
            stripper.setEndPage(pageNum + 1);
            String pageText = stripper.getText(document);
            
            // Find all question numbers on this page (format: 1(a), 1(b), etc.)
            Pattern questionPattern = Pattern.compile("\\b(\\d+\\([a-z]\\))\\b");
            Matcher matcher = questionPattern.matcher(pageText);
            
            while (matcher.find()) {
                String questionNumber = matcher.group(1);
                
                // Ensure list exists for this question
                if (!questionPages.containsKey(questionNumber)) {
                    questionPages.put(questionNumber, new ArrayList<>());
                }
                
                // Add this page if not already added
                if (!questionPages.get(questionNumber).contains(pageNum)) {
                    questionPages.get(questionNumber).add(pageNum);
                }
            }
            
            // Also check for tables with Question Number column
            if (pageText.contains("Question Number") && pageText.contains("Mark")) {
                // Find which question this table belongs to by looking for text before it
                String[] lines = pageText.split("\\r?\\n");
                String currentQuestion = null;
                
                for (String line : lines) {
                    // Check for a question number
                    Matcher qMatcher = questionPattern.matcher(line);
                    if (qMatcher.find()) {
                        currentQuestion = qMatcher.group(1);
                    }
                    
                    // If we find a table header after finding a question number
                    if (currentQuestion != null && line.contains("Question Number") && line.contains("Mark")) {
                        // Add this page to the question if not already added
                        if (!questionPages.containsKey(currentQuestion)) {
                            questionPages.put(currentQuestion, new ArrayList<>());
                        }
                        
                        if (!questionPages.get(currentQuestion).contains(pageNum)) {
                            questionPages.get(currentQuestion).add(pageNum);
                        }
                        
                        // Reset current question after finding a table
                        currentQuestion = null;
                    }
                }
            }
        }
        
        return questionPages;
    }
    
    private void extractPages(PDDocument document, List<Integer> pages, File outputFile) throws IOException {
        PDDocument newDoc = new PDDocument();
        
        // Sort the pages in ascending order
        pages.sort(Integer::compare);
        
        // Copy each page
        for (int pageNum : pages) {
            PDPage page = document.getPage(pageNum);
            newDoc.addPage(newDoc.importPage(page));
        }
        
        // Save the new document
        newDoc.save(outputFile);
        newDoc.close();
    }
}