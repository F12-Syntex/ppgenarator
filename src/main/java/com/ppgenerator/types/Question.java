package com.ppgenerator.types;

import java.io.File;

import lombok.Data;

@Data
public class Question {

    private String questionNumber;
    private String questionText;

    private File question;
    private File markScheme;

    private String year;
    private ExamBoard board;
    private Qualification qualification;
    private String paperIdentifier;

    public boolean isSection2Question() {
        return this.questionNumber.startsWith("question6")
                || this.questionNumber.startsWith("question1")
                || this.questionNumber.startsWith("question2");
    }

    public int getMarks() {

        if (!this.question.getParentFile().getName().contains("3")) {
            switch (questionNumber) {
                case "question1":
                case "question2":
                case "question3":
                case "question4":
                case "question5":
                    if (this.qualification == Qualification.A_LEVEL) {
                        return 5;
                    }
                    return 4;
            }
        }

        // Handle question 6 and other essay questions
        int[] marks = { 2, 4, 5, 6, 8, 10, 12, 15, 20, 25 };

        for (int mark : marks) {
            if (questionText != null && questionText.contains("(" + String.valueOf(mark) + ")")) {
                return mark;
            }
        }

        return Integer.MAX_VALUE;
    }

    private String[] topics;
}
