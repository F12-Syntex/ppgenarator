package com.ppgenarator.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ppgenarator.config.Configuration;
import com.ppgenerator.types.Question;

public class QuestionGeneration {

    private static final int MOCK_COUNT = 5; // Number of mock exams to generate
    private static final int QUESTIONS_PER_MOCK = 7; // Number of questions per mock exam
    private Random random = new Random();

    public void process() {
        List<Question> allQuestions = new ArrayList<>();
        
        File metadata = new File(Configuration.OUTPUT_DIRECTORY, "metadata");
        File[] years = metadata.listFiles();
        
        if (years != null) {
            for (File yearFile : years) {
                try {
                    // Each file in the metadata directory contains JSON data
                    if (yearFile.isFile() && yearFile.getName().endsWith(".json")) {
                        FileReader reader = new FileReader(yearFile);
                        List<Question> questions = new ArrayList<>();
                        
                        // Parse the JSON array into Question objects
                        List<QuestionJson> jsonQuestions = new Gson().fromJson(reader, 
                            new TypeToken<List<QuestionJson>>(){}.getType());
                        
                        for (QuestionJson jsonQ : jsonQuestions) {
                            Question q = new Question();
                            q.setQuestionNumber(jsonQ.questionNumber);
                            q.setQuestionText(jsonQ.questionText);
                            q.setYear(jsonQ.year);
                            q.setBoard(jsonQ.board);
                            q.setTopics(jsonQ.topics);
                            
                            if (jsonQ.questionFile != null) {
                                q.setQuestion(new File(jsonQ.questionFile));
                            }
                            if (jsonQ.markSchemeFile != null) {
                                q.setMarkScheme(new File(jsonQ.markSchemeFile));
                            }
                            
                            questions.add(q);
                        }
                        
                        allQuestions.addAll(questions);
                        reader.close();
                        
                        System.out.println("Loaded " + questions.size() + " questions from " + yearFile.getName());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing file: " + yearFile.getName());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Total questions loaded: " + allQuestions.size());
        
        // Group questions by topic
        Map<String, List<Question>> questionsByTopic = new HashMap<>();
        
        // Filter out questions 6
        List<Question> questionsExcluding6 = allQuestions.stream()
            .filter(q -> !q.getQuestionNumber().contains("6"))
            .collect(Collectors.toList());
        
        System.out.println("Questions excluding '6': " + questionsExcluding6.size());
        
        // Iterate through filtered questions and organize them by topic
        for (Question question : questionsExcluding6) {
            if (question.getTopics() != null) {
                for (String topic : question.getTopics()) {
                    // Normalize topic name for folder creation
                    String normalizedTopic = normalizeTopic(topic);
                    
                    // Add question to the appropriate topic list
                    if (!questionsByTopic.containsKey(normalizedTopic)) {
                        questionsByTopic.put(normalizedTopic, new ArrayList<>());
                    }
                    questionsByTopic.get(normalizedTopic).add(question);
                }
            }
        }
        
        // Create topic folders and generate PDFs
        generateTopicContent(questionsByTopic);
        
        // Generate mock exams
        generateMockExams(questionsExcluding6);
    }
    
    private String normalizeTopic(String topic) {
        // Replace spaces with underscores and remove special characters
        return topic.trim().replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }
    
    private void generateTopicContent(Map<String, List<Question>> questionsByTopic) {
        File outputDir = new File(Configuration.OUTPUT_DIRECTORY, "generation");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        for (Map.Entry<String, List<Question>> entry : questionsByTopic.entrySet()) {
            String topic = entry.getKey();
            List<Question> questions = entry.getValue();
            
            // Create topic directory
            File topicDir = new File(outputDir, topic);
            if (!topicDir.exists()) {
                topicDir.mkdirs();
            }
            
            // Create mark scheme directory
            File msDir = new File(topicDir, "ms");
            if (!msDir.exists()) {
                msDir.mkdirs();
            }
            
            // Create destination file for questions
            File questionsOutputFile = new File(topicDir, "allquestions.pdf");
            
            List<File> questionFiles = new ArrayList<>();
            
            // Track which extract files have already been added to avoid duplicates
            Set<String> addedExtracts = new HashSet<>();
            
            // Collect all valid PDF files and copy mark schemes
            int markSchemesCopied = 0;
            for (Question question : questions) {
                if (question.getQuestion() != null && question.getQuestion().exists()) {
                    questionFiles.add(question.getQuestion());
                    
                    // Check if this is question 6 and add context if available and not already added
                    if (question.getQuestionNumber().contains("6")) {
                        File questionDir = question.getQuestion().getParentFile();
                        File contextFile = new File(questionDir, "extract.pdf");
                        
                        // Use the absolute path as a unique identifier for the extract
                        String extractPath = contextFile.getAbsolutePath();
                        
                        if (contextFile.exists() && !addedExtracts.contains(extractPath)) {
                            questionFiles.add(contextFile);
                            addedExtracts.add(extractPath);
                            System.out.println("Added context file for question 6 from: " + extractPath);
                        } else if (!contextFile.exists()) {
                            System.out.println("Context file for question 6 not found at: " + extractPath);
                        } else {
                            System.out.println("Skipped duplicate context file: " + extractPath);
                        }
                    }
                }
                
                // Copy individual mark scheme files to the ms directory
                if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                    try {
                        // Create a filename that includes year and question number for better organization
                        String msFilename = question.getYear() + "_Q" + question.getQuestionNumber() + ".pdf";
                        File destinationFile = new File(msDir, msFilename);
                        
                        // Copy the mark scheme file
                        Files.copy(
                            question.getMarkScheme().toPath(), 
                            destinationFile.toPath(), 
                            StandardCopyOption.REPLACE_EXISTING
                        );
                        markSchemesCopied++;
                    } catch (IOException e) {
                        System.err.println("Error copying mark scheme for question: " + 
                                          question.getYear() + " " + question.getQuestionNumber());
                        e.printStackTrace();
                    }
                }
            }
            
            // Merge question PDFs
            if (!questionFiles.isEmpty()) {
                try {
                    mergePDFs(questionFiles, questionsOutputFile);
                    System.out.println("Created questions PDF for topic: " + topic + 
                                      " with " + questionFiles.size() + " documents");
                } catch (IOException e) {
                    System.err.println("Error generating questions PDF for topic: " + topic);
                    e.printStackTrace();
                }
            }
            
            System.out.println("Copied " + markSchemesCopied + " mark scheme files to ms folder for topic: " + topic);
        }
    }
    
    private void generateMockExams(List<Question> questions) {
        // Create mocks directory
        File mocksDir = new File(Configuration.OUTPUT_DIRECTORY, "mocks");
        if (!mocksDir.exists()) {
            mocksDir.mkdirs();
        }
        
        // Filter out questions that don't have both question and mark scheme files
        List<Question> validQuestions = questions.stream()
            .filter(q -> q.getQuestion() != null && q.getQuestion().exists() && 
                         q.getMarkScheme() != null && q.getMarkScheme().exists())
            .collect(Collectors.toList());
        
        System.out.println("Valid questions for mock exams: " + validQuestions.size());
        
        // Generate mock exams
        for (int mockIndex = 1; mockIndex <= MOCK_COUNT; mockIndex++) {
            String mockName = "Mock_" + mockIndex;
            File mockDir = new File(mocksDir, mockName);
            if (!mockDir.exists()) {
                mockDir.mkdirs();
            }
            
            // Create mark scheme directory for this mock
            File msDir = new File(mockDir, "ms");
            if (!msDir.exists()) {
                msDir.mkdirs();
            }
            
            // Shuffle the questions for random selection
            Collections.shuffle(validQuestions, random);
            
            // Take first QUESTIONS_PER_MOCK questions
            List<Question> mockQuestions = validQuestions.stream()
                .limit(QUESTIONS_PER_MOCK)
                .collect(Collectors.toList());
            
            // Create the mock exam PDF
            List<File> mockQuestionFiles = new ArrayList<>();
            for (Question question : mockQuestions) {
                mockQuestionFiles.add(question.getQuestion());
                
                // Copy mark scheme to the mock's ms folder
                try {
                    // Create a filename that includes year and question number
                    String msFilename = question.getYear() + "_Q" + question.getQuestionNumber() + ".pdf";
                    File destinationFile = new File(msDir, msFilename);
                    
                    // Copy the mark scheme file
                    Files.copy(
                        question.getMarkScheme().toPath(),
                        destinationFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    System.err.println("Error copying mark scheme for mock exam: " + mockName);
                    e.printStackTrace();
                }
            }
            
            // Merge all question PDFs for this mock
            File mockPdf = new File(mockDir, mockName + ".pdf");
            try {
                mergePDFs(mockQuestionFiles, mockPdf);
                System.out.println("Created mock exam: " + mockName + " with " + mockQuestions.size() + " questions");
            } catch (IOException e) {
                System.err.println("Error generating mock exam PDF: " + mockName);
                e.printStackTrace();
            }
            
            // Create a consolidated mark scheme PDF
            List<File> msFiles = mockQuestions.stream()
                .map(q -> new File(msDir, q.getYear() + "_Q" + q.getQuestionNumber() + ".pdf"))
                .filter(File::exists)
                .collect(Collectors.toList());
            
            if (!msFiles.isEmpty()) {
                try {
                    File consolidatedMs = new File(mockDir, mockName + "_ms.pdf");
                    mergePDFs(msFiles, consolidatedMs);
                    System.out.println("Created consolidated mark scheme for: " + mockName);
                } catch (IOException e) {
                    System.err.println("Error generating consolidated mark scheme for: " + mockName);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void mergePDFs(List<File> sourceFiles, File destinationFile) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(destinationFile.getAbsolutePath());
        
        // Open all PDFs first to ensure they're valid
        List<PDDocument> documents = new ArrayList<>();
        
        try {
            for (File file : sourceFiles) {
                PDDocument doc = PDDocument.load(file);
                documents.add(doc);
                merger.addSource(file);
            }
            
            // Perform the merge with non-sequential mode
            merger.mergeDocuments(null);
        } finally {
            // Close all opened documents
            for (PDDocument doc : documents) {
                try {
                    doc.close();
                } catch (IOException e) {
                    // Log but continue closing other documents
                    System.err.println("Error closing PDF: " + e.getMessage());
                }
            }
        }
    }
    
    // This inner class matches the JSON structure you provided
    private static class QuestionJson {
        private String markSchemeFile;
        private String year;
        private String questionFile;
        private String[] topics;
        private String board;
        private String questionText;
        private String questionNumber;
    }
}