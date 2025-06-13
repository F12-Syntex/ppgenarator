package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ppgenerator.types.Question;

public class Categorize {
    // Instance variables
    private final File outputFolder;
    private final String[] topics;
    private final boolean useDynamicTopics;

    // Component dependencies
    private final TopicKeywordManager keywordManager;
    private final TextProcessor textProcessor;
    private final TopicValidator topicValidator;
    private final TopicMatcher topicMatcher;

    // Tracking for quality control
    private final Map<String, Integer> topicDistribution = new HashMap<>();

    /**
     * Constructor with default topics
     */
    public Categorize(File outputFolder) {
        this(outputFolder, TopicConstants.DEFAULT_TOPICS, null);
    }

    /**
     * Constructor with custom topics
     */
    public Categorize(File outputFolder, String[] topics, String[] optionalTopics) {
        this.outputFolder = outputFolder;
        this.topics = topics;
        this.useDynamicTopics = (topics == null || topics.length == 0);

        // Create output folder if it doesn't exist
        if (!this.outputFolder.exists()) {
            this.outputFolder.mkdirs();
        }

        // Initialize components
        this.keywordManager = new TopicKeywordManager();
        this.textProcessor = new TextProcessor();
        this.topicValidator = new TopicValidator(topics, keywordManager);
        this.topicMatcher = new TopicMatcher(keywordManager, textProcessor);
    }

    /**
     * Process a list of questions to categorize them by topic with enhanced coverage
     */
    public void processQuestions(List<Question> questions) throws JSONException {
        if (questions == null || questions.isEmpty()) {
            System.out.println("No questions to process.");
            return;
        }

        System.out.println("Processing " + questions.size() + " questions with enhanced topic coverage...");
        System.out.println("Mode: " + (useDynamicTopics ? "Dynamic Topics" : "Enhanced Fixed Topics"));
        System.out.println("Topics: " + String.join(", ", topics));
        System.out.println("Maximum topics per question: " + TopicConstants.MAX_TOPICS_PER_QUESTION);

        // Group questions by year
        Map<String, List<Question>> questionsByYear = groupQuestionsByYear(questions);

        // Process each year's questions
        for (Map.Entry<String, List<Question>> entry : questionsByYear.entrySet()) {
            String year = entry.getKey();
            List<Question> yearQuestions = entry.getValue();

            File outputFile = new File(outputFolder, year + ".json");
            if (outputFile.exists()) {
                System.out.println("JSON file already exists for " + year + ". Skipping processing.");
                continue;
            }

            System.out.println("Processing " + yearQuestions.size() + " questions for year " + year);
            processQuestionBatches(yearQuestions);

            // Enhanced quality check for topic distribution
            analyzeTopicDistribution(yearQuestions);

            exportQuestionsToJson(yearQuestions, outputFile);
        }

        // Print comprehensive topic distribution
        printTopicDistribution();
    }

    /**
     * Group questions by their year
     */
    private Map<String, List<Question>> groupQuestionsByYear(List<Question> questions) {
        Map<String, List<Question>> questionsByYear = new HashMap<>();

        for (Question question : questions) {
            String year = question.getYear();
            if (!questionsByYear.containsKey(year)) {
                questionsByYear.put(year, new ArrayList<>());
            }
            questionsByYear.get(year).add(question);
        }

        return questionsByYear;
    }

    /**
     * Process questions in batches to optimize API usage
     */
    private void processQuestionBatches(List<Question> questions) {
        // First load all question content
        for (Question question : questions) {
            loadQuestionText(question);
        }

        // Group questions by qualification and paper for targeted categorization
        Map<String, Map<Integer, List<Question>>> questionsByQualificationAndPaper = 
            groupQuestionsByQualificationAndPaper(questions);

        // Process each qualification and paper group separately
        for (Map.Entry<String, Map<Integer, List<Question>>> qualEntry : questionsByQualificationAndPaper.entrySet()) {
            String qualification = qualEntry.getKey();
            Map<Integer, List<Question>> paperGroups = qualEntry.getValue();

            for (Map.Entry<Integer, List<Question>> paperEntry : paperGroups.entrySet()) {
                int paper = paperEntry.getKey();
                List<Question> paperQuestions = paperEntry.getValue();

                System.out.println("Processing " + paperQuestions.size() + " questions for " + 
                                 qualification + " Paper " + paper);

                // Process in batches for topic identification with qualification and paper-specific context
                for (int i = 0; i < paperQuestions.size(); i += TopicConstants.BATCH_SIZE) {
                    int endIndex = Math.min(i + TopicConstants.BATCH_SIZE, paperQuestions.size());
                    List<Question> batch = paperQuestions.subList(i, endIndex);

                    System.out.println("Processing batch of " + batch.size() + " questions for " + 
                                     qualification + " Paper " + paper + " with enhanced topic identification");
                    identifyTopicsForBatch(batch, qualification, paper);

                    // Add a small delay to avoid rate limiting
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Group questions by qualification and paper
     */
    private Map<String, Map<Integer, List<Question>>> groupQuestionsByQualificationAndPaper(List<Question> questions) {
        Map<String, Map<Integer, List<Question>>> questionsByQualificationAndPaper = new HashMap<>();

        for (Question question : questions) {
            String qualification = question.getQualification() != null 
                ? question.getQualification().toString().toLowerCase() 
                : "unknown";
            
            // Normalize qualification names
            switch (qualification) {
                case "a_level":
                    qualification = "a level";
                    break;
                case "as":
                    qualification = "as level";
                    break;
            }

            // Extract paper number from the question
            int paper = extractPaperNumber(question);

            questionsByQualificationAndPaper
                .computeIfAbsent(qualification, k -> new HashMap<>())
                .computeIfAbsent(paper, k -> new ArrayList<>())
                .add(question);
        }

        return questionsByQualificationAndPaper;
    }

    /**
     * Extract paper number from question
     */
    private int extractPaperNumber(Question question) {
        // Try to get paper number from the question's paper identifier
        String paperIdentifier = question.getPaperIdentifier();
        if (paperIdentifier != null) {
            // Look for paper number in the identifier
            if (paperIdentifier.toLowerCase().contains("paper1") || paperIdentifier.toLowerCase().contains("paper_1")) {
                return 1;
            } else if (paperIdentifier.toLowerCase().contains("paper2") || paperIdentifier.toLowerCase().contains("paper_2")) {
                return 2;
            } else if (paperIdentifier.toLowerCase().contains("paper3") || paperIdentifier.toLowerCase().contains("paper_3")) {
                return 3;
            }
        }

        // Try to extract from file path if available
        if (question.getQuestion() != null) {
            String path = question.getQuestion().getAbsolutePath().toLowerCase();
            if (path.contains("paper1") || path.contains("paper_1")) {
                return 1;
            } else if (path.contains("paper2") || path.contains("paper_2")) {
                return 2;
            } else if (path.contains("paper3") || path.contains("paper_3")) {
                return 3;
            }
        }

        // Default to paper 1 if we can't determine
        return 1;
    }

    /**
     * Load text content from question PDF
     */
    private void loadQuestionText(Question question) {
        // Skip if question text is already loaded
        if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
            return;
        }

        System.out.println(
                "Loading question content for: " + question.getQuestionNumber() + " from year " + question.getYear());

        if (question.getQuestion() != null) {
            String questionContent = textProcessor.extractTextFromPDF(question.getQuestion());
            questionContent = textProcessor.cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);
        }
    }

    /**
     * Identify topics for a batch of questions with enhanced coverage and qualification/paper context
     */
    private void identifyTopicsForBatch(List<Question> questions, String qualification, int paper) {
        // Filter out questions that already have topics
        List<Question> questionsNeedingTopics = questions.stream()
                .filter(q -> q.getTopics() == null || q.getTopics().length == 0)
                .filter(q -> q.getQuestionText() != null && !q.getQuestionText().isEmpty())
                .collect(Collectors.toList());

        if (questionsNeedingTopics.isEmpty()) {
            return;
        }

        // Create qualification and paper-specific AI topic identifier
        AITopicIdentifier aiTopicIdentifier = new AITopicIdentifier(topics, qualification, paper);

        // Try AI-based batch processing first
        Map<Integer, String[]> topicAssignments = aiTopicIdentifier.identifyTopicsForBatch(questionsNeedingTopics);

        // If batch processing failed, process individually
        if (topicAssignments.isEmpty()) {
            System.out.println("Batch parsing failed for " + qualification + " Paper " + paper + ". Processing individually...");
            for (Question question : questionsNeedingTopics) {
                identifyTopicsForSingleQuestion(question, qualification, paper);
            }
            return;
        }

        // Assign topics to questions with enhanced validation
        for (int i = 0; i < questionsNeedingTopics.size(); i++) {
            Question question = questionsNeedingTopics.get(i);

            if (topicAssignments.containsKey(i + 1)) {
                String[] assignedTopics = topicAssignments.get(i + 1);
                String[] validatedTopics = topicValidator.validateAndLimitTopics(
                        assignedTopics,
                        textProcessor.removeIgnorePhrases(question.getQuestionText()));

                question.setTopics(validatedTopics);
                System.out.println("Assigned " + validatedTopics.length + " topics " + Arrays.toString(validatedTopics) +
                        " to " + qualification + " Paper " + paper + " question " + question.getQuestionNumber());
            } else {
                // If we couldn't get topics from batch processing, try individual processing
                identifyTopicsForSingleQuestion(question, qualification, paper);
            }
        }
    }

    /**
     * Identify topics for a single question with enhanced coverage and qualification/paper context
     */
    private void identifyTopicsForSingleQuestion(Question question, String qualification, int paper) {
        System.out.println("Processing individual " + qualification + " Paper " + paper + " question: " + question.getQuestionNumber());

        // Clean the question text by removing misleading phrases
        String cleanedText = textProcessor.removeIgnorePhrases(question.getQuestionText());

        // Create qualification and paper-specific AI topic identifier
        AITopicIdentifier aiTopicIdentifier = new AITopicIdentifier(topics, qualification, paper);

        // Try AI-based approach
        String[] suggestedTopics = aiTopicIdentifier.identifyTopicsForSingleQuestion(cleanedText);
        String[] validatedTopics = topicValidator.validateAndLimitTopics(suggestedTopics, cleanedText);

        if (validatedTopics.length > 0) {
            question.setTopics(validatedTopics);
            System.out.println("Assigned " + validatedTopics.length + " topics: " + Arrays.toString(validatedTopics));
            return;
        }

        // Try keyword-based approach as backup
        String[] keywordTopics = topicMatcher.findStrictTopicsByKeywords(cleanedText);
        if (keywordTopics.length > 0) {
            question.setTopics(keywordTopics);
            System.out.println("Assigned " + keywordTopics.length + " topics by keywords: " + Arrays.toString(keywordTopics));
            return;
        }

        // Last resort - assign fallback topic
        String fallbackTopic = topicValidator.determineFallbackTopic(cleanedText);
        question.setTopics(new String[] { fallbackTopic });
        System.out.println("Assigned fallback topic: " + fallbackTopic);
    }

    /**
     * Export questions to a JSON file
     */
    private void exportQuestionsToJson(List<Question> questions, File outputFile) throws JSONException {
        try {
            JSONArray jsonArray = new JSONArray();

            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("questionText", question.getQuestionText());
                jsonQuestion.put("qualification", question.getQualification());
                jsonQuestion.put("marks", question.getMarks());

                // Add topics as a JSON array
                JSONArray topicsArray = new JSONArray();
                if (question.getTopics() != null) {
                    for (String topic : question.getTopics()) {
                        topicsArray.put(topic);
                    }
                }
                jsonQuestion.put("topics", topicsArray);

                // Add file paths
                if (question.getQuestion() != null) {
                    jsonQuestion.put("questionFile", question.getQuestion().getAbsolutePath());
                }
                if (question.getMarkScheme() != null) {
                    jsonQuestion.put("markSchemeFile", question.getMarkScheme().getAbsolutePath());
                }

                jsonArray.put(jsonQuestion);
            }

            // Write the JSON to the output file
            java.nio.file.Files.write(
                    outputFile.toPath(),
                    jsonArray.toString(2).getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println(
                    "Successfully exported " + questions.size() + " questions to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing questions to JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enhanced analysis of topic distribution
     */
    private void analyzeTopicDistribution(List<Question> questions) {
        Map<String, Integer> yearDistribution = new HashMap<>();
        Map<Integer, Integer> topicCountDistribution = new HashMap<>();
        int questionsWithMultipleTopics = 0;

        for (Question question : questions) {
            if (question.getTopics() != null && question.getTopics().length > 0) {
                int topicCount = question.getTopics().length;
                topicCountDistribution.put(topicCount, topicCountDistribution.getOrDefault(topicCount, 0) + 1);
                
                if (question.getTopics().length > 1) {
                    questionsWithMultipleTopics++;
                }

                // Count each topic separately
                for (String topic : question.getTopics()) {
                    yearDistribution.put(topic, yearDistribution.getOrDefault(topic, 0) + 1);
                    topicDistribution.put(topic, topicDistribution.getOrDefault(topic, 0) + 1);
                }
            }
        }

        int totalQuestions = questions.size();
        double multipleTopicPercentage = (double) questionsWithMultipleTopics / totalQuestions * 100;

        System.out.println("\n=== ENHANCED TOPIC DISTRIBUTION ANALYSIS ===");
        System.out.println("Questions with multiple topics: " + questionsWithMultipleTopics +
                " (" + String.format("%.1f%%", multipleTopicPercentage) + ")");
        
        System.out.println("\nTopic count distribution:");
        for (Map.Entry<Integer, Integer> entry : topicCountDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            System.out.println("  " + entry.getKey() + " topics: " + entry.getValue() + 
                    " questions (" + String.format("%.1f%%", percentage) + ")");
        }

        // Only warn if a topic appears in more than 80% of questions (very high threshold)
        for (Map.Entry<String, Integer> entry : yearDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            if (percentage > 80) {
                System.out.println("NOTE: Topic '" + entry.getKey() + "' appears in " +
                        String.format("%.1f%%", percentage) + " of questions - consider if this is appropriate.");
            }
        }

        // Show topics with good coverage
        System.out.println("\nTopics with good coverage (>5 questions):");
        yearDistribution.entrySet().stream()
                .filter(entry -> entry.getValue() > 5)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    double percentage = (double) entry.getValue() / totalQuestions * 100;
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue() + 
                            " questions (" + String.format("%.1f%%", percentage) + ")");
                });
    }

    /**
     * Print comprehensive topic distribution
     */
    private void printTopicDistribution() {
        System.out.println("\n=== COMPREHENSIVE TOPIC DISTRIBUTION ===");
        System.out.println("(Enhanced categorization with up to " + TopicConstants.MAX_TOPICS_PER_QUESTION + " topics per question)");

        // Sort topics by frequency
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(topicDistribution.entrySet());
        sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int totalTopicAssignments = sortedEntries.stream().mapToInt(Map.Entry::getValue).sum();

        System.out.println("\nAll topics (sorted by frequency):");
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            double percentage = (double) entry.getValue() / totalTopicAssignments * 100;
            System.out.printf("%-35s: %3d assignments (%.1f%% of total assignments)\n",
                    entry.getKey(), entry.getValue(), percentage);
        }

        // Show topics that might need attention
        System.out.println("\nTopics with few assignments (might need review):");
        sortedEntries.stream()
                .filter(entry -> entry.getValue() < 3)
                .forEach(entry -> 
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " assignments"));

        System.out.println("\nTotal topic assignments: " + totalTopicAssignments);
        System.out.println("Average topics per question: " + 
                String.format("%.2f", (double) totalTopicAssignments / 
                (totalTopicAssignments > 0 ? sortedEntries.size() : 1)));
        System.out.println("==============================================");
    }
}