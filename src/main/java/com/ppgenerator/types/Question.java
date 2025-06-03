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
    private int marks;

    private String[] topics;
}
