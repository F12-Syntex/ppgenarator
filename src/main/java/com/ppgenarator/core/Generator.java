package com.ppgenarator.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.ppgenarator.ai.Categorize;
import com.ppgenarator.config.Configuration;
import com.ppgenarator.core.topics.TopicCompiler;
import com.ppgenarator.processor.markscheme.MarkSchemeProcessor;
import com.ppgenarator.processor.questions.PastPaperProcessor;
import com.ppgenerator.types.DocumentType;
import com.ppgenerator.types.FileInfo;
import com.ppgenerator.types.Question;

public class Generator {

    public static void main(String[] args) {
        Generator generator = new Generator();
        generator.run();
    }

    public void run() {

        //download past papers
        // downloadPastPapers();

        // Process past papers if needed
        processAllPastPapers();

        // Compile topics from existing processed data (simplified individual topic structure)
        compileTopics();

        // Create comprehensive topic analysis
        createTopicAnalysis();
    }

    /**
     * Downloads past papers from specified URLs
     */
    private void downloadPastPapers() {
        System.out.println("Starting past paper download...");

        File pastpaperFolder = new File(Configuration.PAST_PAPER_DIRECTORY);
        PastPaperDownloader downloader = new PastPaperDownloader(pastpaperFolder);

        List<String> urls = getPastPaperUrls();

        for (String url : urls) {
            System.out.println("Downloading from: " + url);
            downloader.downloadPastPapers(url);
        }

        System.out.println("Past paper download completed.");
    }

    /**
     * Returns list of URLs for past paper downloads
     */
    private List<String> getPastPaperUrls() {
        List<String> urls = new ArrayList<>();
        urls.add("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-1-as/");
        urls.add("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-2-as/");
        // Add more URLs as needed
        urls.add("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-1/");
        urls.add("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-2/");
        urls.add("https://www.physicsandmathstutor.com/past-papers/a-level-economics/edexcel-a-paper-3/");
        return urls;
    }

    /**
     * Complete pipeline for processing all past papers
     */
    private void processAllPastPapers() {
        FileInfo[] files = formatAndGetFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files found to process.");
            return;
        }

        processDocuments(files);
        List<Question> questions = extractAllQuestions(files);
        categorizeQuestions(questions);
    }

    /**
     * Formats directory and returns file information
     */
    private FileInfo[] formatAndGetFiles() {
        File pastpapers = new File(Configuration.PAST_PAPER_DIRECTORY);

        if (!pastpapers.exists()) {
            System.err.println("Past papers directory does not exist: " + Configuration.PAST_PAPER_DIRECTORY);
            return new FileInfo[0];
        }

        DirectoryFormatter directoryFormatter = new DirectoryFormatter(pastpapers);
        return directoryFormatter.formatDirectory();
    }

    /**
     * Processes both question papers and mark schemes
     */
    private void processDocuments(FileInfo[] files) {
        System.out.println("Processing " + files.length + " documents...");

        for (FileInfo file : files) {
            try {
                if (file.getDocumentType() == DocumentType.MARK_SCHEME) {
                    processMarkScheme(file);
                } else if (file.getDocumentType() == DocumentType.QUESTION_PAPER) {
                    processQuestionPaper(file);
                }
            } catch (Exception e) {
                System.err.println("Error processing file: " + file.getFile().getName());
                e.printStackTrace();
            }
        }

        System.out.println("Document processing completed.");
    }

    /**
     * Processes a mark scheme file
     */
    private void processMarkScheme(FileInfo markSchemeFile) {
        System.out.println("Processing mark scheme: " + markSchemeFile.getFile().getName());
        MarkSchemeProcessor markSchemeProcessor = new MarkSchemeProcessor(markSchemeFile);
        markSchemeProcessor.process();
    }

    /**
     * Processes a question paper file
     */
    private void processQuestionPaper(FileInfo questionPaperFile) {
        System.out.println("Processing question paper: " + questionPaperFile.getFile().getName());
        PastPaperProcessor pastPaperProcessor = new PastPaperProcessor(questionPaperFile);
        pastPaperProcessor.process();
    }

    /**
     * Extracts questions from all processed files
     */
    private List<Question> extractAllQuestions(FileInfo[] files) {
        System.out.println("Extracting questions from all files...");

        List<Question> allQuestions = new ArrayList<>();

        for (FileInfo file : files) {
            try {
                List<Question> questionsForFile = file.extractQuestions();
                System.out.println("Questions extracted from " + file.getFile().getName() + ": "
                        + questionsForFile.size());
                allQuestions.addAll(questionsForFile);
            } catch (Exception e) {
                System.err.println("Error extracting questions from: " + file.getFile().getName());
                e.printStackTrace();
            }
        }

        System.out.println("Total questions extracted: " + allQuestions.size());
        return allQuestions;
    }

    /**
     * Categorizes questions using AI
     */
    private void categorizeQuestions(List<Question> questions) {
        if (questions.isEmpty()) {
            System.out.println("No questions to categorize.");
            return;
        }

        System.out.println("Categorizing " + questions.size() + " questions...");

        File output = new File(Configuration.OUTPUT_DIRECTORY);
        Categorize categorize = new Categorize(output);

        try {
            categorize.processQuestions(questions);
            System.out.println("Question categorization completed.");
        } catch (JSONException e) {
            System.err.println("Error during question categorization:");
            e.printStackTrace();
        }
    }

    /**
     * Compiles questions by individual topics (simplified structure)
     */
    private void compileTopics() {
        System.out.println("Compiling questions by individual topics...");

        File output = new File(Configuration.OUTPUT_DIRECTORY);
        File outputDir = new File(Configuration.OUTPUT_DIRECTORY, "topics");

        if (!output.exists()) {
            System.err.println("Output directory does not exist: " + Configuration.OUTPUT_DIRECTORY);
            return;
        }

        TopicCompiler topicCompiler = new TopicCompiler(output, outputDir);
        topicCompiler.compileByTopic();

        System.out.println("Individual topic compilation completed.");
    }

    /**
     * Creates comprehensive topic analysis and reports
     */
    private void createTopicAnalysis() {
        System.out.println("Creating comprehensive topic analysis...");

        File output = new File(Configuration.OUTPUT_DIRECTORY);

        if (!output.exists()) {
            System.err.println("Output directory does not exist: " + Configuration.OUTPUT_DIRECTORY);
            return;
        }

        TopicCompiler topicCompiler = new TopicCompiler(output, output);

        // Generate topic overview
        topicCompiler.generateTopicOverview();

        // Generate analysis reports
        topicCompiler.createTopicAnalysisReport();

        System.out.println("Topic analysis completed.");
    }

    /**
     * Creates directory if it doesn't exist
     */
    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created directory: " + directoryPath);
            } else {
                System.err.println("Failed to create directory: " + directoryPath);
            }
        }
    }
}
