package com.ppgenerator.types;

import java.io.File;

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
}