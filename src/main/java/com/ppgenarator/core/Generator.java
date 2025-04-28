package com.ppgenarator.core;

import java.io.File;
import java.util.List;

import com.ppgenarator.config.Configuration;
import com.ppgenerator.types.DocumentType;
import com.ppgenerator.types.FileInfo;

public class Generator {
    public static void main(String[] args) {

        File pastpapers = new File(Configuration.PAST_PAPER_DIRECTORY);
        DirectoryFormatter directoryFomatter = new DirectoryFormatter(pastpapers);

        FileInfo[] files = directoryFomatter.formatDirectory();

        for(FileInfo file : files){
            if(file.getDocumentType() == DocumentType.QUESTION_PAPER){
                processQuestionPaper(file);
            }
            if(file.getDocumentType() == DocumentType.MARK_SCHEME){
                processMarkScheme(file);
            }
        }

    }

    private static void processQuestionPaper(FileInfo questions) {
        System.out.println("Processing question paper: " + questions);
    }

    private static void processMarkScheme(FileInfo markschemes) {
        System.out.println("Processing mark scheme: " + markschemes);
    }

}