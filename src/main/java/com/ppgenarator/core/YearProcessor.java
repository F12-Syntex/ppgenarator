package com.ppgenarator.core;

import java.io.File;

public class YearProcessor {

    private File yearDirectory;

    public YearProcessor(File year) {
        this.yearDirectory = year;
    }


    public void process(){
        File qp = new File(yearDirectory, "qp");
        File ms = new File(yearDirectory, "ms");

        if(!qp.exists() || !ms.exists()) {
            System.out.println("QP or MS not found in the directory: " + yearDirectory.getAbsolutePath());
            return;
        }

        if(!qp.isDirectory() || !ms.isDirectory()) {
            System.out.println("QP or MS is not a directory: " + yearDirectory.getAbsolutePath());
            return;
        }

        File[] qpFiles = qp.listFiles();
        File[] msFiles = ms.listFiles();

        if(qpFiles == null || msFiles == null) {
            System.out.println("QP or MS files not found in the directory: " + yearDirectory.getAbsolutePath());
            return;
        }

        File questionPaper = qpFiles[0];
        File markScheme = msFiles[0];

        PPProcess ppProcess = new PPProcess(questionPaper, markScheme);
        ppProcess.process();
    }

}
