package com.ppgenarator.processor.markscheme;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenarator.config.Configuration;

public class MarkSchemeProcessor {
    
    private File markSchemeFilePath;
    private static final int MAX_GROUP_QUESTION = 5; // Only group questions 1-5

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
            
            // Find all question tables and their page numbers
            Map<String, List<Integer>> questionPages = findQuestionTables(document);
            
            // Split into questions to group (1-5) and questions to keep separate (6+)
            Map<String, List<Integer>> groupableQuestions = new HashMap<>();
            Map<String, List<Integer>> individualQuestions = new HashMap<>();
            
            for (Map.Entry<String, List<Integer>> entry : questionPages.entrySet()) {
                String questionNumber = entry.getKey();
                int mainNumber = extractMainQuestionNumber(questionNumber);
                
                if (mainNumber <= MAX_GROUP_QUESTION) {
                    groupableQuestions.put(questionNumber, entry.getValue());
                } else {
                    individualQuestions.put(questionNumber, entry.getValue());
                }
            }
            
            // Process questions 1-5 (grouped)
            Map<Integer, TreeSet<Integer>> groupedQuestions = groupSubquestions(groupableQuestions);
            for (Map.Entry<Integer, TreeSet<Integer>> entry : groupedQuestions.entrySet()) {
                Integer mainQuestionNumber = entry.getKey();
                TreeSet<Integer> allPages = entry.getValue();
                
                if (!allPages.isEmpty()) {
                    File outputFile = new File(markSchemeDir, "question" + mainQuestionNumber + ".pdf");
                    
                    // Convert TreeSet to List for the extraction method
                    List<Integer> pagesList = new ArrayList<>(allPages);
                    
                    // Extract all pages for this question and its subquestions
                    extractPages(document, pagesList, outputFile);
                    
                    System.out.println("Created grouped mark scheme for question " + mainQuestionNumber + 
                                      ": " + outputFile.getName());
                }
            }
            
            // Process question 6 and beyond (individual parts)
            for (Map.Entry<String, List<Integer>> entry : individualQuestions.entrySet()) {
                String questionNumber = entry.getKey();
                List<Integer> pages = entry.getValue();
                
                // Format the question number for the filename (e.g., "6(a)" becomes "question6a.pdf")
                String formattedQuestionNumber = questionNumber.replaceAll("\\(|\\)", "");
                File outputFile = new File(markSchemeDir, "question" + formattedQuestionNumber + ".pdf");
                
                // Extract pages for this specific question part
                extractPages(document, pages, outputFile);
                
                System.out.println("Created individual mark scheme for question " + questionNumber + 
                                  ": " + outputFile.getName());
            }
            
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Map<String, List<Integer>> findQuestionTables(PDDocument document) throws IOException {
        Map<String, List<Integer>> questionPages = new HashMap<>();
        
        // Process each page to find question tables
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            String pageText = getTextFromPage(document, pageNum);
            
            // Look for tables with question numbers
            Pattern tablePattern = Pattern.compile("Question\\s+Number|\\d+\\s*\\([a-z]\\)");
            Matcher tableMatcher = tablePattern.matcher(pageText);
            
            // If we find a table structure
            if (tableMatcher.find()) {
                // Find specific question numbers within this page
                Pattern questionPattern = Pattern.compile("(\\d+)\\s*\\(([a-z])\\)");
                Matcher questionMatcher = questionPattern.matcher(pageText);
                
                while (questionMatcher.find()) {
                    String questionNumber = questionMatcher.group(1) + "(" + questionMatcher.group(2) + ")";
                    
                    // Add this page to the list for this question
                    questionPages.computeIfAbsent(questionNumber, k -> new ArrayList<>()).add(pageNum);
                }
            }
        }
        
        return questionPages;
    }
    
    private int extractMainQuestionNumber(String questionNumber) {
        Pattern mainNumberPattern = Pattern.compile("(\\d+)\\([a-z]\\)");
        Matcher matcher = mainNumberPattern.matcher(questionNumber);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0; // Default case, shouldn't happen with valid question numbers
    }
    
    private Map<Integer, TreeSet<Integer>> groupSubquestions(Map<String, List<Integer>> questionPages) {
        Map<Integer, TreeSet<Integer>> groupedQuestions = new HashMap<>();
        
        // Process each question and group by main question number
        for (Map.Entry<String, List<Integer>> entry : questionPages.entrySet()) {
            String questionNumber = entry.getKey();
            List<Integer> pages = entry.getValue();
            
            int mainNumber = extractMainQuestionNumber(questionNumber);
            if (mainNumber > 0) {
                // Add all pages to the main question's set
                TreeSet<Integer> pagesSet = groupedQuestions.computeIfAbsent(mainNumber, k -> new TreeSet<>());
                pagesSet.addAll(pages);
            }
        }
        
        return groupedQuestions;
    }
    
    private String getTextFromPage(PDDocument document, int pageNum) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNum + 1);
        stripper.setEndPage(pageNum + 1);
        return stripper.getText(document);
    }
    
    private void extractPages(PDDocument sourceDoc, List<Integer> pageNumbers, File outputFile) throws IOException {
        PDDocument newDoc = new PDDocument();
        
        // Copy the specified pages, ensuring no duplicates
        for (int pageNum : pageNumbers) {
            PDPage page = sourceDoc.getPage(pageNum);
            newDoc.addPage(newDoc.importPage(page));
        }
        
        
        if(outputFile.exists()) {
            newDoc.close();
            return;
        }

        // Save the new document
        newDoc.save(outputFile);
        newDoc.close();
    }
}