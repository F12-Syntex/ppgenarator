package com.ppgenerator.types;

public enum ExamBoard {
    AQA("aqa"),
    EDEXCEL("edexcel"),
    OCR("ocr"),
    WJEC("wjec"),
    CAMBRIDGE("cambridge"),
    IB("ib"),
    PEARSON("pearson"),
    EDUQAS("eduqas"),
    UNKNOWN("unknown");
    
    private final String code;
    
    ExamBoard(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static ExamBoard fromString(String text) {
        for (ExamBoard board : ExamBoard.values()) {
            if (board.code.equalsIgnoreCase(text)) {
                return board;
            }
        }
        
        // Handle special cases
        if (text.equalsIgnoreCase("pearson edexcel") || text.equalsIgnoreCase("pearsonedexcel")) {
            return EDEXCEL;
        } else if (text.equalsIgnoreCase("caie") || text.equalsIgnoreCase("cie")) {
            return CAMBRIDGE;
        }
        
        throw new IllegalArgumentException("Unknown exam board: " + text);
    }
}