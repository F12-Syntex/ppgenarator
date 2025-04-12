package com.ppgenarator.core;

import java.io.File;

import com.ppgenarator.processor.markscheme.MarkSchemeProcessor;
import com.ppgenarator.processor.questions.PastPaperProcessor;

public class PPProcess {

    private File questionPaper;
    private File markScheme;

    public PPProcess(File questionPaper, File markScheme) {
        this.questionPaper = questionPaper;
        this.markScheme = markScheme;
    }

    public void process() {

        String year = questionPaper.getParentFile().getParentFile().getName();

        System.out.println("Processing year " + year);
        PastPaperProcessor pastPaperProcessor = new PastPaperProcessor(questionPaper);
        MarkSchemeProcessor markSchemeProcessor = new MarkSchemeProcessor(markScheme);

        pastPaperProcessor.process();
        markSchemeProcessor.process();
        System.out.println("Finished processing year " + year);
        System.out.println("CLOSING FROM PPProcess.java");
        System.exit(0);
    }
}
