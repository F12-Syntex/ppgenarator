package com.ppgenarator.core;

import java.io.File;

import org.json.JSONException;

import com.ppgenarator.ai.Categorize;
import com.ppgenarator.config.Configuration;
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


        File output = new File(Configuration.OUTPUT_DIRECTORY, year);

        System.out.println("Preparing meta data for year " + year);
        Categorize categorize = new Categorize(output);

        try {
            categorize.process();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        System.out.println("Finished processing year " + year);
    }
}
