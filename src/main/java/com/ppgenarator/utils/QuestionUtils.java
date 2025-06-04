package com.ppgenarator.utils;

import java.io.File;

import com.ppgenerator.types.Question;

public class QuestionUtils {

    public static boolean isQuestion6(String questionNumber) {
        return questionNumber != null && questionNumber.toLowerCase().startsWith("question6");
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
}