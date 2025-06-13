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
        // Handle Paper 3 questions 1 and 2 (which have sub-parts like question 6)
        if (questionNumber.startsWith("question1") || questionNumber.startsWith("question2")) {
            // These are essay-style questions with variable marks
            int[] marks = {2, 4, 5, 6, 8, 10, 12, 15, 20, 25};

            for (int mark : marks) {
                if (questionText != null && questionText.contains("(" + String.valueOf(mark) + ")")) {
                    return mark;
                }
            }

            // Default marks for Paper 3 questions 1 and 2 subparts
            if (questionNumber.contains("a")) {
                return 2;
            }
            if (questionNumber.contains("b")) {
                return 4;
            }
            if (questionNumber.contains("c")) {
                return 6;
            }
            if (questionNumber.contains("d")) {
                return 8;
            }
            if (questionNumber.contains("e")) {
                return 10;
            }

            return 5; // Default
        }

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

        // Handle question 6 and other essay questions
        int[] marks = {2, 4, 5, 6, 8, 10, 12, 15, 20, 25};

        for (int mark : marks) {
            if (questionText != null && questionText.contains("(" + String.valueOf(mark) + ")")) {
                return mark;
            }
        }

        return Integer.MAX_VALUE;
    }

    private String[] topics;
}
