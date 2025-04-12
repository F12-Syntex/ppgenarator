package com.ppgenarator.core;

import java.io.File;

import com.ppgenarator.config.Configuration;

public class Generator {
    public static void main(String[] args) {

        File[] years = new File(Configuration.PAST_PAPER_DIRECTORY).listFiles();

        if (years == null) {
            System.out.println("No past papers found in the directory: " + Configuration.PAST_PAPER_DIRECTORY);
            return;
        }

        for(File year : years) {
            YearProcessor yearProcessor = new YearProcessor(year);
            yearProcessor.process();
        }


    }
}
