package com.ppgenarator.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.ppgenarator.ai.Categorize;
import com.ppgenarator.config.Configuration;
import com.ppgenarator.processor.markscheme.MarkSchemeProcessor;
import com.ppgenarator.processor.questions.PastPaperProcessor;
import com.ppgenerator.types.DocumentType;
import com.ppgenerator.types.FileInfo;
import com.ppgenerator.types.Question;

public class Generator {
    public static void main(String[] args) {

        // downloadPastPapers();

        // File pastpapers = new File(Configuration.PAST_PAPER_DIRECTORY);
        // DirectoryFormatter directoryFomatter = new DirectoryFormatter(pastpapers);

        // FileInfo[] files = directoryFomatter.formatDirectory();

        // for (FileInfo file : files) {
        //     if (file.getDocumentType() == DocumentType.MARK_SCHEME) {
        //         MarkSchemeProcessor markSchemeProcessor = new MarkSchemeProcessor(file);
        //         markSchemeProcessor.process();
        //     }

        //     if (file.getDocumentType() == DocumentType.QUESTION_PAPER) {
        //         PastPaperProcessor pastPaperProcessor = new PastPaperProcessor(file);
        //         pastPaperProcessor.process();
        //     }
        // }

        // List<Question> questions = new ArrayList<>();

        // for (FileInfo file : files) {
        //     List<Question> questionsForFile = file.extractQuestions();
        //     System.out.println("Questions for file: " + file.getFile().getName() + " : "
        //             + questions.size());
        //     questions.addAll(questionsForFile);
        // }

        File output = new File(Configuration.OUTPUT_DIRECTORY);
        // Categorize categorize = new Categorize(output);

        // try {
        //     categorize.processQuestions(questions);
        // } catch (JSONException e) {
        //     e.printStackTrace();
        // }

        File outputDir = new File(Configuration.OUTPUT_DIRECTORY, "topics");

        TopicCompiler topicCompiler = new TopicCompiler(output, outputDir);
        topicCompiler.compileByTopic();

    }

    private static void downloadPastPapers() {
        File pastpaperFolder = new File(Configuration.PAST_PAPER_DIRECTORY);
        PastPaperDownloader downloader = new PastPaperDownloader(pastpaperFolder);
        downloader.downloadPastPapers(
                "https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-1-as/");
        downloader.downloadPastPapers(
                "https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-2-as/");
        // downloader.downloadPastPapers(
        // "https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-1/");
        // downloader.downloadPastPapers("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-2/");
        System.exit(0);
    }

    private static void processQuestionPaper(FileInfo questions) {
        System.out.println("Processing question paper: " + questions);
    }

    private static void processMarkScheme(FileInfo markschemes) {
        System.out.println("Processing mark scheme: " + markschemes);
    }

}