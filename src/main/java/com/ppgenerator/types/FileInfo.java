package com.ppgenerator.types;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ppgenarator.config.Configuration;

import lombok.Data;

@Data
public class FileInfo {
    private String topic;
    private Qualification qualification;
    private ExamBoard examBoard;
    private int year;
    private int paper;
    private DocumentType documentType;
    private String extension = "";

    private File file;

    public boolean isComplete() {
        return topic != null && qualification != null &&
                examBoard != null && year > 0 && documentType != null;
    }

    public File getOutputFolder() {
        return new File(Configuration.OUTPUT_DIRECTORY, topic + File.separator + qualification + File.separator
                + examBoard + File.separator + year + File.separator + paper);
    }

    public List<Question> extractQuestions() {
        List<Question> questions = new ArrayList<>();

        File outputFolder = getOutputFolder();
        File markschemeFolder = new File(outputFolder, "markscheme");

        File[] questionFiles = outputFolder.listFiles();
        File[] markschemeFiles = markschemeFolder.listFiles();

        if (questionFiles == null || markschemeFiles == null) {
            System.out.println("No question files found in " + outputFolder.getAbsolutePath());
            return questions;
        }

        for (File questionFile : questionFiles) {
            if (questionFile.isFile() && questionFile.getName().endsWith(".pdf")) {
                String questionNumber = questionFile.getName().replace(".pdf", "");
                File markschemeFile = new File(markschemeFolder, questionNumber + ".pdf");

                if (!markschemeFile.exists()) {
                    // System.out.println("Mark scheme file not found for " +
                    // questionFile.getName());
                    continue;
                }

                Question question = new Question();
                question.setBoard(examBoard);
                question.setQualification(qualification);
                question.setQuestionNumber(questionNumber);
                question.setYear(String.valueOf(year));
                question.setQuestion(questionFile);
                question.setMarkScheme(markschemeFile);
                question.setPaperIdentifier(questionFile.getParentFile().getName());

                questions.add(question);
            }
        }

        return questions;
    }
}