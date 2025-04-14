package com.ppgenarator.core;

import java.io.File;

import com.ppgenarator.config.Configuration;
import com.ppgenerator.types.FileInfo;

public class Generator {
    public static void main(String[] args) {

        File pastpapers = new File(Configuration.PAST_PAPER_DIRECTORY);
        DirectoryFormatter directoryFomatter = new DirectoryFormatter(pastpapers);

        FileInfo[] files = directoryFomatter.formatDirectory();

        for (FileInfo file : files) {
            System.out.println(file);
        }

    }
}