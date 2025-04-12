package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenerator.types.Question;

public class Categorize {

    private File yearDir;

    public Categorize(File yearDir) {
        this.yearDir = yearDir;
    }


    public void process(){

        List<Question> questions = new ArrayList<>();

        //find all pdfs
        File[] files = yearDir.listFiles((dir, name) -> name.endsWith(".pdf"));

        for(File file : files) {

            if(!file.getName().contains("question")){
                continue;
            }

            String name = file.getName().split("question")[1].split("\\.")[0];

            Question question = new Question();
            question.setQuestionNumber(name);
            question.setQuestion(file);
            question.setYear(yearDir.getName());
            question.setBoard("edexcel");

            questions.add(question);
        }

        File markschemeFolder = new File(yearDir, "markscheme");
        for(File file : markschemeFolder.listFiles()) {

            if(!file.getName().contains("question")){
                continue;
            }

            String name = file.getName().split("question")[1].split("\\.")[0];

            for(Question question : questions) {
                if(question.getQuestionNumber().equals(name)) {
                    question.setMarkScheme(file);
                }
            }
        }

        questions.forEach(this::loadQuestionContent);
        questions.forEach(System.out::println);

    }


    private void loadQuestionContent(Question question){
        // Read the question PDF file and extract the text
        if (question.getQuestion() != null) {
            String questionContent = extractTextFromPDF(question.getQuestion());
            if(questionContent.contains(". .")){
                questionContent = questionContent.replace(". .", "").replace("  .", "").split("\\)")[1].trim();
            }
            question.setQuestionText(questionContent);
        }
    }
    
    /**
     * Extract text content from a PDF file
     * @param pdfFile The PDF file to extract text from
     * @return The extracted text
     */
    private String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF file: " + pdfFile.getName());
            e.printStackTrace();
            return "";
        }
    }
}