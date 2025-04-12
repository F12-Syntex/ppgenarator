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
import org.apache.pdfbox.multipdf.Splitter;

import com.ppgenarator.config.Configuration;

public class SectionAProcessor {
    private File sectionAFile;

    public SectionAProcessor(File sectionAFile) {
        this.sectionAFile = sectionAFile;
    }

    public void process() {
        try {
            PDDocument document = PDDocument.load(sectionAFile);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Create output directory
            File outputDir = new File(Configuration.OUTPUT_DIRECTORY,
                    sectionAFile.getParentFile().getParentFile().getName());
            outputDir.mkdirs();

            // Find all questions
            List<Question> questions = findQuestions(text, document);

            // Sort questions by number to ensure proper order
            questions.sort((q1, q2) -> Integer.compare(q1.number, q2.number));

            // Process each question
            for (Question question : questions) {
                PDDocument questionDoc = new PDDocument();
                
                // Copy pages for this question
                for (int pageNum = question.startPage; pageNum <= question.endPage; pageNum++) {
                    PDPage page = document.getPage(pageNum);
                    questionDoc.addPage(page);
                }

                // Save question document
                File questionFile = new File(outputDir, String.format("question_%d.pdf", question.number));

                //if already exists then skip
                if (questionFile.exists()) {
                    questionDoc.close();
                    continue;
                }

                questionDoc.save(questionFile);
                questionDoc.close();
            }

            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Question {
        int number;
        int startPage;
        int endPage;
        int startPosition;
        int endPosition;

        Question(int number, int startPage, int endPage, int startPosition, int endPosition) {
            this.number = number;
            this.startPage = startPage;
            this.endPage = endPage;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
    }

    private List<Question> findQuestions(String fullText, PDDocument document) throws IOException {
        List<Question> questions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();

        // Find all "Total for Question X = Y marks" occurrences
        Pattern totalPattern = Pattern.compile("Total for Question (\\d+) = \\d+ marks");
        Matcher totalMatcher = totalPattern.matcher(fullText);
        
        // Store all end positions first
        List<QuestionEnd> questionEnds = new ArrayList<>();
        while (totalMatcher.find()) {
            int questionNumber = Integer.parseInt(totalMatcher.group(1));
            int endPosition = totalMatcher.end();
            int endPage = findPageForText(document, totalMatcher.group(0));
            questionEnds.add(new QuestionEnd(questionNumber, endPosition, endPage));
        }

        // Sort question ends by position
        questionEnds.sort((e1, e2) -> Integer.compare(e1.position, e2.position));

        // Process each question
        for (int i = 0; i < questionEnds.size(); i++) {
            QuestionEnd end = questionEnds.get(i);
            int searchStartPos = (i > 0) ? questionEnds.get(i-1).position : 0;
            
            // Find start of this question
            Pattern startPattern = Pattern.compile("(?m)^\\s*" + end.questionNumber + "\\s");
            Matcher startMatcher = startPattern.matcher(fullText);
            startMatcher.region(searchStartPos, end.position);

            if (startMatcher.find()) {
                int startPosition = startMatcher.start();
                int startPage = findPageForText(document, 
                    fullText.substring(startPosition, Math.min(startPosition + 20, fullText.length())));

                if (startPage != -1) {
                    questions.add(new Question(end.questionNumber, startPage, end.page, 
                        startPosition, end.position));
                }
            }
        }

        return questions;
    }

    private static class QuestionEnd {
        int questionNumber;
        int position;
        int page;

        QuestionEnd(int questionNumber, int position, int page) {
            this.questionNumber = questionNumber;
            this.position = position;
            this.page = page;
        }
    }

    private int findPageForText(PDDocument document, String searchText) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);

            if (pageText.contains(searchText)) {
                return i;
            }
        }

        return -1;
    }
}