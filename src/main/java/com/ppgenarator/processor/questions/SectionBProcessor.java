package com.ppgenarator.processor.questions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

public class SectionBProcessor {
    private File sectionFile;
    private File outputDir;
    private String questionNumber; // "1", "2", or "6"

    public SectionBProcessor(File sectionFile, File outputDir) {
        this(sectionFile, outputDir, "6"); // Default to question 6
    }

    public SectionBProcessor(File sectionFile, File outputDir, String questionNumber) {
        this.sectionFile = sectionFile;
        this.outputDir = outputDir;
        this.questionNumber = questionNumber;
    }

    public void process() {
        try {
            PDDocument document = PDDocument.load(sectionFile);
            
            outputDir.mkdirs();

            // Find the page where the main question starts
            int questionStartPage = findQuestionStartPage(document, questionNumber);
            
            if (questionStartPage == -1) {
                System.err.println("Could not find Question " + questionNumber + " in the document");
                document.close();
                return;
            }
            
            System.out.println("Found Question " + questionNumber + " starting at page " + (questionStartPage + 1));
            
            // Extract context pages before the question as extract
            if (questionStartPage > 0) {
                extractContextPdf(document, questionStartPage, outputDir, questionNumber);
            }
            
            // Find all subquestions starting from the page AFTER the main question page
            List<Subquestion> subquestions = findSubquestions(document, questionStartPage + 1, questionNumber);
            
            System.out.println("Found " + subquestions.size() + " subquestions for Question " + questionNumber);
            
            // Process each subquestion
            extractSubquestionPdfs(document, subquestions, outputDir, questionNumber);

            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractContextPdf(PDDocument document, int questionStartPage, File outputDir, String questionNum) throws IOException {
        if (questionStartPage <= 0) return; // No context pages to extract
        
        PDDocument extractDoc = new PDDocument();
        for (int i = 0; i < questionStartPage; i++) {
            extractDoc.addPage(document.getPage(i));
        }
        
        // Create different extract filenames for Paper 3 questions 1 and 2
        String extractFileName;
        if ("1".equals(questionNum)) {
            extractFileName = "extract1.pdf";
        } else if ("2".equals(questionNum)) {
            extractFileName = "extract2.pdf";
        } else {
            extractFileName = "extract.pdf"; // Default for question 6
        }
        
        File extractFile = new File(outputDir, extractFileName);

        //if already exists then skip
        if (extractFile.exists()) {
            extractDoc.close();
            return;
        }

        extractDoc.save(extractFile);
        extractDoc.close();
        System.out.println("Created " + extractFileName + " with " + questionStartPage + " pages for question " + questionNum);
    }
    
    private void extractSubquestionPdfs(PDDocument document, List<Subquestion> subquestions, File outputDir, String questionNumber) throws IOException {
        for (int i = 0; i < subquestions.size(); i++) {
            Subquestion subquestion = subquestions.get(i);
            PDDocument subquestionDoc = new PDDocument();
            
            // Copy pages for this subquestion
            for (int pageNum = subquestion.startPage; pageNum <= subquestion.endPage; pageNum++) {
                PDPage page = document.getPage(pageNum);
                subquestionDoc.addPage(page);
            }

            // Save subquestion document
            File subquestionFile = new File(outputDir, String.format("question%s%s.pdf", questionNumber, subquestion.letter));

            //if already exists then skip
            if (subquestionFile.exists()) {
                subquestionDoc.close();
                continue;
            }

            subquestionDoc.save(subquestionFile);
            subquestionDoc.close();
            System.out.println("Created question" + questionNumber + subquestion.letter + ".pdf with pages " 
                + (subquestion.startPage + 1) + " to " + (subquestion.endPage + 1));
        }
    }

    private int findQuestionStartPage(PDDocument document, String questionNum) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);
            
            // Check if page contains a line starting with the question number followed by a space or other character
            for (String line : pageText.split("\\r?\\n")) {
                line = line.trim();
                if (line.matches("^" + questionNum + "\\s.*")) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    private static class Subquestion {
        String letter;
        int startPage;
        int endPage;
        
        Subquestion(String letter, int startPage, int endPage) {
            this.letter = letter;
            this.startPage = startPage;
            this.endPage = endPage;
        }
    }
    
    private List<Subquestion> findSubquestions(PDDocument document, int startPage, String questionNumber) throws IOException {
        List<Subquestion> subquestions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        
        // Keep track of the current subquestion we're processing
        String currentLetter = null;
        int currentStartPage = -1;
        
        System.out.println("Looking for subquestions for Question " + questionNumber + " starting from page " + (startPage + 1));
        
        // Check each page starting from the page after the main question
        for (int i = startPage; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);
            
            // For Question 2 in Paper 3, we don't need to look for next main question
            // since it's the last question in the paper
            boolean isNewMainQuestion = false;
            if (!"2".equals(questionNumber)) {
                // Define the next question number to look for
                String nextQuestionPattern = getNextQuestionPattern(questionNumber);
                
                // Look for start of next main question
                for (String line : pageText.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.matches(nextQuestionPattern)) {
                        isNewMainQuestion = true;
                        System.out.println("Found next main question at page " + (i + 1) + ": " + line);
                        break;
                    }
                }
            }
            
            if (isNewMainQuestion) {
                // End the current subquestion if we were tracking one
                if (currentStartPage != -1) {
                    subquestions.add(new Subquestion(currentLetter, currentStartPage, i - 1));
                    System.out.println("Ended subquestion " + currentLetter + " at page " + i);
                }
                break; // Exit the loop as we've found the next main question
            }
            
            // Check if this page starts a new subquestion (contains "(a)", "(b)", etc.)
            String subquestionLetter = null;
            Pattern pattern = Pattern.compile("\\(([a-z])\\)");
            Matcher matcher = pattern.matcher(pageText);
            if (matcher.find()) {
                subquestionLetter = matcher.group(1);
                System.out.println("Found subquestion (" + subquestionLetter + ") at page " + (i + 1));
            }
            
            if (subquestionLetter != null) {
                // This page starts a new subquestion
                
                // End the previous subquestion if we were tracking one
                if (currentStartPage != -1) {
                    subquestions.add(new Subquestion(currentLetter, currentStartPage, i - 1));
                    System.out.println("Ended previous subquestion " + currentLetter + " at page " + i);
                }
                
                // Start tracking the new subquestion
                currentLetter = subquestionLetter;
                currentStartPage = i;
                System.out.println("Started tracking subquestion " + currentLetter + " from page " + (i + 1));
            }
        }
        
        // Handle the last subquestion if we were still tracking one
        if (currentStartPage != -1) {
            subquestions.add(new Subquestion(currentLetter, currentStartPage, document.getNumberOfPages() - 1));
            System.out.println("Ended final subquestion " + currentLetter + " at last page " + document.getNumberOfPages());
        }
        
        return subquestions;
    }
    
    private String getNextQuestionPattern(String currentQuestionNumber) {
        switch (currentQuestionNumber) {
            case "1":
                return "^2\\s.*";
            case "2":
                // For Paper 3, after question 2, there are no more questions
                // This should never be called due to the check in findSubquestions
                return "^NONEXISTENT_PATTERN$";
            case "6":
                return "^[7-9]\\s.*|^[1-9][0-9]\\s.*";
            default:
                // For other question numbers, look for the next sequential number
                try {
                    int nextNum = Integer.parseInt(currentQuestionNumber) + 1;
                    return "^" + nextNum + "\\s.*";
                } catch (NumberFormatException e) {
                    return "^[7-9]\\s.*|^[1-9][0-9]\\s.*";
                }
        }
    }
}