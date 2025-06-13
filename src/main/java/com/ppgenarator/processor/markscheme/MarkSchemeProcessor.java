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

import com.ppgenerator.types.FileInfo;

public class MarkSchemeProcessor {

    private FileInfo file;
    private static final int MAX_GROUP_QUESTION = 5;

    public MarkSchemeProcessor(FileInfo file) {
        this.file = file;
    }

    public void process() {
        try {
            PDDocument document = PDDocument.load(file.getFile());

            // Create output directory
            File outputDir = file.getOutputFolder();
            outputDir.mkdirs();

            // Create a directory for mark scheme
            File markSchemeDir = new File(outputDir, "markscheme");
            markSchemeDir.mkdirs();

            // Check if this is Paper 3
            boolean isPaper3 = isPaper3(document);
            
            if (isPaper3) {
                System.out.println("Processing Paper 3 markscheme with sub-questions");
                processPaper3MarkschemeDetailed(document, markSchemeDir);
            } else {
                System.out.println("Processing standard markscheme");
                processStandardMarkscheme(document, markSchemeDir);
            }

            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPaper3(PDDocument document) throws IOException {
        // Check if this is Paper 3 by looking for indicators in the text
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(3, document.getNumberOfPages())); // Check first few pages
        String text = stripper.getText(document);
        
        // Check for Paper 3 indicators
        return text.toLowerCase().contains("paper 3") || 
               text.toLowerCase().contains("paper 03") ||
               text.toLowerCase().contains("paper three") ||
               file.getPaper() == 3;
    }

    private void processPaper3MarkschemeDetailed(PDDocument document, File markSchemeDir) throws IOException {
        // Find sub-question sections with clear boundaries
        Map<String, SubQuestionSection> subQuestionSections = findSubQuestionSections(document);

        // Create PDFs for each sub-question
        for (Map.Entry<String, SubQuestionSection> entry : subQuestionSections.entrySet()) {
            String subQuestionKey = entry.getKey(); // e.g., "1a", "1b", "2a", "2b"
            SubQuestionSection section = entry.getValue();

            if (section.startPage <= section.endPage && section.startPage >= 0) {
                List<Integer> pages = new ArrayList<>();
                for (int i = section.startPage; i <= section.endPage; i++) {
                    pages.add(i);
                }
                
                File outputFile = new File(markSchemeDir, "question" + subQuestionKey + ".pdf");
                extractPages(document, pages, outputFile);
                System.out.println("Created Paper 3 sub-question mark scheme for " + subQuestionKey +
                        ": " + outputFile.getName() + " (pages " + (section.startPage + 1) + "-" + (section.endPage + 1) + ")");
            }
        }

        // Also create grouped markschemes for backward compatibility
        createGroupedMarkschemes(subQuestionSections, document, markSchemeDir);
    }

    private static class SubQuestionSection {
        int startPage = -1;
        int endPage = -1;
        boolean hasContent = false;
        
        SubQuestionSection(int startPage) {
            this.startPage = startPage;
            this.endPage = startPage; // Initially, end page is same as start page
            this.hasContent = true;
        }
    }

    private Map<String, SubQuestionSection> findSubQuestionSections(PDDocument document) throws IOException {
        Map<String, SubQuestionSection> sections = new HashMap<>();
        List<SubQuestionMarker> markers = new ArrayList<>();
        
        // First pass: Find all sub-question markers and their pages
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            String pageText = getTextFromPage(document, pageNum);

            // Skip guidance pages
            if (shouldSkipPage(pageText)) {
                continue;
            }

            // Look for sub-question markers on this page
            Pattern subQuestionPattern = Pattern.compile("([12])\\s*\\(([a-e])\\)");
            Matcher matcher = subQuestionPattern.matcher(pageText);
            
            while (matcher.find()) {
                String questionNum = matcher.group(1);
                String subLetter = matcher.group(2);
                String key = questionNum + subLetter;
                
                // Only record if we haven't seen this sub-question before
                if (!sections.containsKey(key)) {
                    markers.add(new SubQuestionMarker(key, pageNum, matcher.start()));
                    System.out.println("Found sub-question " + key + " starting on page " + (pageNum + 1));
                }
            }
        }

        // Sort markers by page and position
        markers.sort((a, b) -> {
            if (a.pageNum != b.pageNum) {
                return Integer.compare(a.pageNum, b.pageNum);
            }
            return Integer.compare(a.positionInPage, b.positionInPage);
        });

        // Second pass: Determine section boundaries
        for (int i = 0; i < markers.size(); i++) {
            SubQuestionMarker current = markers.get(i);
            SubQuestionMarker next = (i + 1 < markers.size()) ? markers.get(i + 1) : null;
            
            SubQuestionSection section = new SubQuestionSection(current.pageNum);
            
            if (next != null) {
                // End this section just before the next sub-question starts
                section.endPage = next.pageNum - 1;
                
                // If the next sub-question is on the same page, we need to be more careful
                if (next.pageNum == current.pageNum) {
                    // Both sub-questions are on the same page - this section gets only this page
                    section.endPage = current.pageNum;
                } else {
                    // Extend to include any continuation pages
                    section.endPage = findSectionEndPage(document, current.pageNum, next.pageNum - 1);
                }
            } else {
                // This is the last sub-question, extend to end of document or until we find non-content
                section.endPage = findSectionEndPage(document, current.pageNum, document.getNumberOfPages() - 1);
            }
            
            sections.put(current.subQuestion, section);
            System.out.println("Sub-question " + current.subQuestion + " spans pages " + 
                             (section.startPage + 1) + " to " + (section.endPage + 1));
        }

        return sections;
    }

    private static class SubQuestionMarker {
        String subQuestion;
        int pageNum;
        int positionInPage;
        
        SubQuestionMarker(String subQuestion, int pageNum, int positionInPage) {
            this.subQuestion = subQuestion;
            this.pageNum = pageNum;
            this.positionInPage = positionInPage;
        }
    }

    private int findSectionEndPage(PDDocument document, int startPage, int maxEndPage) throws IOException {
        int endPage = startPage;
        
        // Look for content continuation
        for (int pageNum = startPage + 1; pageNum <= maxEndPage; pageNum++) {
            String pageText = getTextFromPage(document, pageNum);
            
            // If the page has substantial content related to mark schemes, include it
            if (hasMarkSchemeContent(pageText)) {
                endPage = pageNum;
            } else if (pageText.trim().length() < 50) {
                // Very short page, likely a page break - stop here
                break;
            }
        }
        
        return endPage;
    }

    private boolean hasMarkSchemeContent(String pageText) {
        if (pageText.trim().length() < 30) {
            return false;
        }
        
        String lowerText = pageText.toLowerCase();
        
        // Check for mark scheme indicators
        return lowerText.contains("knowledge") ||
               lowerText.contains("application") ||
               lowerText.contains("analysis") ||
               lowerText.contains("evaluation") ||
               lowerText.contains("level") ||
               lowerText.contains("mark") ||
               lowerText.contains("descriptor") ||
               lowerText.contains("indicative content") ||
               lowerText.contains("continued") ||
               // Also include pages with bullet points or economic content
               pageText.contains("•") ||
               pageText.contains("marks") ||
               pageText.contains("(1)") ||
               pageText.contains("(2)");
    }

    private boolean shouldSkipPage(String pageText) {
        if (pageText.trim().length() < 50) {
            return true;
        }

        String lowerText = pageText.toLowerCase();
        return lowerText.contains("general marking guidance") ||
               lowerText.contains("marking guidance") ||
               lowerText.contains("edexcel and btec qualifications") ||
               lowerText.contains("pearson education") ||
               lowerText.contains("publications code") ||
               (lowerText.contains("pearson") && !lowerText.contains("question"));
    }

    private void createGroupedMarkschemes(Map<String, SubQuestionSection> sections, 
                                        PDDocument document, File markSchemeDir) throws IOException {
        
        // Group sections by main question number
        Map<String, List<Integer>> groupedPages = new HashMap<>();
        groupedPages.put("1", new ArrayList<>());
        groupedPages.put("2", new ArrayList<>());
        
        for (Map.Entry<String, SubQuestionSection> entry : sections.entrySet()) {
            String subKey = entry.getKey(); // e.g., "1a", "1b", "2a"
            String mainQuestion = subKey.substring(0, 1); // Extract "1" or "2"
            SubQuestionSection section = entry.getValue();
            
            for (int i = section.startPage; i <= section.endPage; i++) {
                if (!groupedPages.get(mainQuestion).contains(i)) {
                    groupedPages.get(mainQuestion).add(i);
                }
            }
        }
        
        // Create grouped PDFs
        for (Map.Entry<String, List<Integer>> entry : groupedPages.entrySet()) {
            String questionNum = entry.getKey();
            List<Integer> pages = entry.getValue();
            
            if (!pages.isEmpty()) {
                // Sort pages
                pages.sort(Integer::compareTo);
                
                File outputFile = new File(markSchemeDir, "question" + questionNum + ".pdf");
                extractPages(document, pages, outputFile);
                System.out.println("Created grouped mark scheme for question " + questionNum +
                        ": " + outputFile.getName() + " with " + pages.size() + " pages");
            }
        }
    }

    private void processStandardMarkscheme(PDDocument document, File markSchemeDir) throws IOException {
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
                List<Integer> pagesList = new ArrayList<>(allPages);
                extractPages(document, pagesList, outputFile);
                System.out.println("Created grouped mark scheme for question " + mainQuestionNumber +
                        ": " + outputFile.getName());
            }
        }

        // Process question 6 and beyond (individual parts)
        for (Map.Entry<String, List<Integer>> entry : individualQuestions.entrySet()) {
            String questionNumber = entry.getKey();
            List<Integer> pages = entry.getValue();

            String formattedQuestionNumber = questionNumber.replaceAll("\\(|\\)", "");
            File outputFile = new File(markSchemeDir, "question" + formattedQuestionNumber + ".pdf");
            extractPages(document, pages, outputFile);
            System.out.println("Created individual mark scheme for question " + questionNumber +
                    ": " + outputFile.getName());
        }
    }

    private Map<String, List<Integer>> findQuestionTables(PDDocument document) throws IOException {
        Map<String, List<Integer>> questionPages = new HashMap<>();

        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            String pageText = getTextFromPage(document, pageNum);

            if (pageText.trim().length() < 10) {
                continue;
            }

            Pattern tablePattern = Pattern.compile("Question\\s+Number|\\d+\\s*\\([a-z]\\)");
            Matcher tableMatcher = tablePattern.matcher(pageText);

            if (tableMatcher.find()) {
                Pattern questionPattern = Pattern.compile("(\\d+)\\s*\\(([a-z])\\)");
                Matcher questionMatcher = questionPattern.matcher(pageText);

                while (questionMatcher.find()) {
                    String questionNumber = questionMatcher.group(1) + "(" + questionMatcher.group(2) + ")";
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
        return 0;
    }

    private Map<Integer, TreeSet<Integer>> groupSubquestions(Map<String, List<Integer>> questionPages) {
        Map<Integer, TreeSet<Integer>> groupedQuestions = new HashMap<>();

        for (Map.Entry<String, List<Integer>> entry : questionPages.entrySet()) {
            String questionNumber = entry.getKey();
            List<Integer> pages = entry.getValue();

            int mainNumber = extractMainQuestionNumber(questionNumber);
            if (mainNumber > 0) {
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

        TreeSet<Integer> uniquePages = new TreeSet<>(pageNumbers);
        
        for (int pageNum : uniquePages) {
            if (pageNum >= 0 && pageNum < sourceDoc.getNumberOfPages()) {
                PDPage page = sourceDoc.getPage(pageNum);
                newDoc.addPage(newDoc.importPage(page));
            } else {
                System.out.println("Skipping invalid page number: " + pageNum);
            }
        }

        if (outputFile.exists()) {
            System.out.println("File already exists, skipping: " + outputFile.getName());
            newDoc.close();
            return;
        }

        if (newDoc.getNumberOfPages() > 0) {
            newDoc.save(outputFile);
            System.out.println("Saved " + outputFile.getName() + " with " + newDoc.getNumberOfPages() + " pages");
        } else {
            System.out.println("No valid pages to save for " + outputFile.getName());
        }
        
        newDoc.close();
    }
}