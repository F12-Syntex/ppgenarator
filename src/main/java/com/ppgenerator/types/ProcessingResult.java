package com.ppgenerator.types;

import lombok.Data;

@Data
public class ProcessingResult {
    String originalName;
    String newName;
    String status;
    String message;

    public ProcessingResult(String originalName, String newName, String status, String message) {
        this.originalName = originalName;
        this.newName = newName;
        this.status = status;
        this.message = message;
    }
}