package com.ppgenarator.utils;

import java.io.File;

import com.ppgenerator.types.Question;

public class QuestionUtils {

    public static boolean isQuestion6(String questionNumber) {
        return questionNumber != null && questionNumber.toLowerCase().startsWith("question6");
    }

    public static boolean isPaper3Question1or2(String questionNumber) {
        return questionNumber != null &&
                (questionNumber.toLowerCase().startsWith("question1") ||
                        questionNumber.toLowerCase().startsWith("question2"));
    }

    public static boolean isEssayStyleQuestion(String questionNumber) {
        return isQuestion6(questionNumber) || isPaper3Question1or2(questionNumber);
    }

    public static String getPaperIdentifier(Question question) {
        if (question.getQuestion() == null || !question.getQuestion().exists()) {
            return question.getYear() + "_" + question.getBoard().toString();
        }

        try {
            File questionFile = question.getQuestion();
            File paperDir = questionFile.getParentFile();
            File yearDir = paperDir != null ? paperDir.getParentFile() : null;
            File boardDir = yearDir != null ? yearDir.getParentFile() : null;

            if (paperDir != null && yearDir != null && boardDir != null) {
                return question.getYear() + "_" + question.getBoard().toString() + "_" + paperDir.getName();
            }
        } catch (Exception e) {
            System.err.println("Error getting paper identifier: " + e.getMessage());
        }

        return question.getYear() + "_" + question.getBoard().toString();
    }

    public static String getQuestionIdentifier(Question question) {
        if (question.getQuestion() != null && question.getQuestion().exists()) {
            return FileUtils.getFileMd5Hash(question.getQuestion());
        }
        return question.getYear() + "_" + question.getBoard().toString() + "_" + question.getQuestionNumber();
    }

    public static String getFormattedMonth(Question question) {
        if (question.getQuestion() != null) {
            String path = question.getQuestion().getAbsolutePath().toLowerCase();

            if (path.contains("june") || path.contains("summer") || path.contains("may")) {
                return "June";
            } else if (path.contains("october") || path.contains("autumn") || path.contains("fall")) {
                return "October";
            } else if (path.contains("january") || path.contains("winter")) {
                return "January";
            } else if (path.contains("march") || path.contains("spring")) {
                return "March";
            }
        }

        return "June"; // Default
    }

    // Add this method to your existing QuestionUtils class

    /**
     * Determines if a question is context-based (requires 2.5 minutes per mark)
     * Context-based questions typically include:
     * - Question 6 (essay questions)
     * - Paper 3 Questions 1 and 2
     * - Questions that have associated extract files
     */
    public static boolean isContextBasedQuestion(Question question) {
        if (question == null) {
            return false;
        }

        String questionNumber = question.getQuestionNumber();

        // Essay-style questions are always context-based
        if (isEssayStyleQuestion(questionNumber)) {
            return true;
        }

        // Paper 3 Questions 1 and 2 are context-based
        if (isPaper3Question1or2(questionNumber)) {
            return true;
        }

        // Check if question has an associated extract file (indicates context-based)
        if (question.getQuestion() != null && question.getQuestion().exists()) {
            File questionFile = question.getQuestion();
            File paperDir = questionFile.getParentFile();

            if (paperDir != null) {
                // Look for extract files
                File extractFile = new File(paperDir, "extract.pdf");
                File extract1File = new File(paperDir, "extract1.pdf");
                File extract2File = new File(paperDir, "extract2.pdf");

                if (extractFile.exists() || extract1File.exists() || extract2File.exists()) {
                    return true;
                }
            }
        }

        // High mark questions (10+ marks) are often context-based
        if (question.getMarks() >= 10) {
            return true;
        }

        return false;
    }
}