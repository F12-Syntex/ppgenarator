package com.ppgenerator.types;

public enum Qualification {
    GCSE("gcse"),
    A_LEVEL("alevel"),
    AS("as"),
    IGCSE("igcse"),
    DIPLOMA("diploma"),
    BTEC("btec"),
    IB("ib");
    
    private final String code;
    
    Qualification(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static Qualification fromString(String text) {
        for (Qualification qual : Qualification.values()) {
            if (qual.code.equalsIgnoreCase(text)) {
                return qual;
            }
        }
        
        // Handle special cases
        if (text.equalsIgnoreCase("a-level") || text.equalsIgnoreCase("a level") || 
            text.equalsIgnoreCase("gce")) {
            return A_LEVEL;
        } else if (text.equalsIgnoreCase("as-level") || text.equalsIgnoreCase("as level")) {
            return AS;
        }
        
        throw new IllegalArgumentException("Unknown qualification: " + text);
    }
}