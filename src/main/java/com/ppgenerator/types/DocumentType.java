package com.ppgenerator.types;

public enum DocumentType {
    QUESTION_PAPER("questionpaper", "qp"),
    MARK_SCHEME("markscheme", "ms"),
    EXAMINER_REPORT("examinerreport", "er");
    
    private final String code;
    private final String shortCode;
    
    DocumentType(String code, String shortCode) {
        this.code = code;
        this.shortCode = shortCode;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getShortCode() {
        return shortCode;
    }
    
    public static DocumentType fromString(String text) {
        for (DocumentType type : DocumentType.values()) {
            if (type.code.equalsIgnoreCase(text) || 
                type.shortCode.equalsIgnoreCase(text) ||
                text.toLowerCase().contains(type.code)) {
                return type;
            }
        }
        
        // Special cases
        if (text.toLowerCase().contains("mark") || text.toLowerCase().equals("ms")) {
            return MARK_SCHEME;
        } else if (text.toLowerCase().contains("question") || text.toLowerCase().equals("qp")) {
            return QUESTION_PAPER;
        } else if (text.toLowerCase().contains("report")) {
            return EXAMINER_REPORT;
        }
        
        throw new IllegalArgumentException("Unknown document type: " + text);
    }
}