package com.ppgenarator.core;

import java.io.File;

import com.ppgenarator.config.Configuration;

public class Generator {
    public static void main(String[] args) {

        File[] pastpapers = new File(Configuration.PAST_PAPER_DIRECTORY).listFiles();

        if (pastpapers == null) {
            System.out.println("No past papers found in the directory: " + Configuration.PAST_PAPER_DIRECTORY);
            return;
        }

        for (File pastpaper : pastpapers) {
            if (pastpaper.isFile() && pastpaper.getName().endsWith(".pdf")) {
                System.out.println("Processing file: " + pastpaper.getName());
            } else {
                System.out.println("Skipping non-PDF file: " + pastpaper.getName());
            }
        }


    }
}
