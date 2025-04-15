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

import com.ppgenarator.config.Configuration;

public class SectionBProcessor {
    private File sectionBFile;
    private File outputDir;

    public SectionBProcessor(File sectionBFile, File outputDir) {
        this.sectionBFile = sectionBFile;
        this.outputDir = outputDir;
    }

    public void process() {
        try {
            PDDocument document = PDDocument.load(sectionBFile);
            
            outputDir.mkdirs();

            // Find the page where Question 6 starts (a page containing a line starting with "6")
            int question6StartPage = findQuestion6StartPage(document);
            
            if (question6StartPage == -1) {
                System.err.println("Could not find Question 6 in the document");
                document.close();
                return;
            }
            
            // Extract all pages before Question 6 to extract.pdf (the context)
            extractContextPdf(document, question6StartPage, outputDir);
            
            // Find all subquestions starting from the page AFTER Question 6 page
            List<Subquestion> subquestions = findSubquestions(document, question6StartPage + 1);
            
            // Process each subquestion
            extractSubquestionPdfs(document, subquestions, outputDir);

            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractContextPdf(PDDocument document, int question6StartPage, File outputDir) throws IOException {
        PDDocument extractDoc = new PDDocument();
        for (int i = 0; i < question6StartPage; i++) {
            extractDoc.addPage(document.getPage(i));
        }
        File extractFile = new File(outputDir, "extract.pdf");

        //if already exists then skip
        if (extractFile.exists()) {
            extractDoc.close();
            return;
        }

        extractDoc.save(extractFile);
        extractDoc.close();
        System.out.println("Created extract.pdf with " + question6StartPage + " pages");
    }
    
    private void extractSubquestionPdfs(PDDocument document, List<Subquestion> subquestions, File outputDir) throws IOException {
        for (int i = 0; i < subquestions.size(); i++) {
            Subquestion subquestion = subquestions.get(i);
            PDDocument subquestionDoc = new PDDocument();
            
            // Copy pages for this subquestion
            for (int pageNum = subquestion.startPage; pageNum <= subquestion.endPage; pageNum++) {
                PDPage page = document.getPage(pageNum);
                subquestionDoc.addPage(page);
            }

            // Save subquestion document
            File subquestionFile = new File(outputDir, String.format("question6%s.pdf", subquestion.letter));


            //if already exists then skip
            if (subquestionFile.exists()) {
                subquestionDoc.close();
                continue;
            }

            subquestionDoc.save(subquestionFile);
            subquestionDoc.close();
            System.out.println("Created question6" + subquestion.letter + ".pdf with pages " 
                + (subquestion.startPage + 1) + " to " + (subquestion.endPage + 1));
        }
    }

    private int findQuestion6StartPage(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);
            
            // Check if page contains a line starting with "6" followed by a space or other character
            for (String line : pageText.split("\\r?\\n")) {
                line = line.trim();
                if (line.matches("^6\\s.*")) {
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
    
    private List<Subquestion> findSubquestions(PDDocument document, int startPage) throws IOException {
        List<Subquestion> subquestions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        
        // Keep track of the current subquestion we're processing
        String currentLetter = null;
        int currentStartPage = -1;
        
        // Check each page starting from the page after Question 6
        for (int i = startPage; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);
            
            // Look for start of next main question (7, 8, etc.)
            boolean isNewMainQuestion = false;
            for (String line : pageText.split("\\r?\\n")) {
                line = line.trim();
                if (line.matches("^[7-9]\\s.*") || line.matches("^[1-9][0-9]\\s.*")) {
                    isNewMainQuestion = true;
                    break;
                }
            }
            
            if (isNewMainQuestion) {
                // End the current subquestion if we were tracking one
                if (currentStartPage != -1) {
                    subquestions.add(new Subquestion(currentLetter, currentStartPage, i - 1));
                }
                break; // Exit the loop as we've found the next main question
            }
            
            // Check if this page starts a new subquestion (contains "(a)", "(b)", etc.)
            String subquestionLetter = null;
            Pattern pattern = Pattern.compile("\\(([a-z])\\)");
            Matcher matcher = pattern.matcher(pageText);
            if (matcher.find()) {
                subquestionLetter = matcher.group(1);
            }
            
            if (subquestionLetter != null) {
                // This page starts a new subquestion
                
                // End the previous subquestion if we were tracking one
                if (currentStartPage != -1) {
                    subquestions.add(new Subquestion(currentLetter, currentStartPage, i - 1));
                }
                
                // Start tracking the new subquestion
                currentLetter = subquestionLetter;
                currentStartPage = i;
            }
        }
        
        // Handle the last subquestion if we were still tracking one
        if (currentStartPage != -1) {
            subquestions.add(new Subquestion(currentLetter, currentStartPage, document.getNumberOfPages() - 1));
        }
        
        return subquestions;
    }
}