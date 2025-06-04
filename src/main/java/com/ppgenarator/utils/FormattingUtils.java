package com.ppgenarator.utils;

public class FormattingUtils {

    public static String formatQualificationName(String qualification) {
        if (qualification == null) {
            return "Unknown";
        }

        switch (qualification.toUpperCase()) {
            case "A_LEVEL":
                return "A Level";
            case "AS":
                return "AS Level";
            case "GCSE":
                return "GCSE";
            case "IGCSE":
                return "IGCSE";
            case "IB":
                return "IB";
            default:
                String[] words = qualification.replace("_", " ").toLowerCase().split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        sb.append(Character.toUpperCase(word.charAt(0)));
                        if (word.length() > 1) {
                            sb.append(word.substring(1));
                        }
                        sb.append(" ");
                    }
                }
                return sb.toString().trim();
        }
    }

    public static String formatTopicName(String topic) {
        if (topic == null) {
            return "Unknown Topic";
        }

        return capitalizeWords(topic.replace("_", " ").replace("-", " ").toLowerCase());
    }

    public static String formatOriginalQuestionNumber(String questionNumber) {
        if (questionNumber == null) return "";
        
        String cleaned = questionNumber.toLowerCase().replace("question", "");
        return cleaned.trim();
    }

    public static String formatQuestionNumber(String questionNumber) {
        if (questionNumber == null) return "Unknown";
        
        if (questionNumber.toLowerCase().startsWith("question")) {
            String number = questionNumber.substring(8);
            return "Question " + number.toUpperCase();
        }
        
        return questionNumber;
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}