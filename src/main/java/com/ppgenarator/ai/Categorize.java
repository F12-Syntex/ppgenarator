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
    private final AITopicIdentifier aiTopicIdentifier;

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
        this.aiTopicIdentifier = new AITopicIdentifier(topics);
    }

    /**
     * Process a list of questions to categorize them by topic
     */
    public void processQuestions(List<Question> questions) throws JSONException {
        if (questions == null || questions.isEmpty()) {
            System.out.println("No questions to process.");
            return;
        }

        System.out.println("Processing " + questions.size() + " questions...");
        System.out.println("Mode: " + (useDynamicTopics ? "Dynamic Topics" : "Fixed Topics"));
        System.out.println("Topics: " + String.join(", ", topics));

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

            // Quality check for topic distribution
            analyzeTopicDistribution(yearQuestions);

            exportQuestionsToJson(yearQuestions, outputFile);
        }

        // Print topic distribution
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

        // Process in batches for topic identification
        for (int i = 0; i < questions.size(); i += TopicConstants.BATCH_SIZE) {
            int endIndex = Math.min(i + TopicConstants.BATCH_SIZE, questions.size());
            List<Question> batch = questions.subList(i, endIndex);

            System.out.println("Processing batch of " + batch.size() + " questions for topic identification");
            identifyTopicsForBatch(batch);

            // Add a small delay to avoid rate limiting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
     * Identify topics for a batch of questions
     */
    private void identifyTopicsForBatch(List<Question> questions) {
        // Filter out questions that already have topics
        List<Question> questionsNeedingTopics = questions.stream()
                .filter(q -> q.getTopics() == null || q.getTopics().length == 0)
                .filter(q -> q.getQuestionText() != null && !q.getQuestionText().isEmpty())
                .collect(Collectors.toList());

        if (questionsNeedingTopics.isEmpty()) {
            return;
        }

        // Try AI-based batch processing first
        Map<Integer, String[]> topicAssignments = aiTopicIdentifier.identifyTopicsForBatch(questionsNeedingTopics);

        // If batch processing failed, process individually
        if (topicAssignments.isEmpty()) {
            System.out.println("Batch parsing failed. Processing individually...");
            for (Question question : questionsNeedingTopics) {
                identifyTopicsForSingleQuestion(question);
            }
            return;
        }

        // Assign topics to questions with strict validation
        for (int i = 0; i < questionsNeedingTopics.size(); i++) {
            Question question = questionsNeedingTopics.get(i);

            if (topicAssignments.containsKey(i + 1)) {
                String[] assignedTopics = topicAssignments.get(i + 1);
                String[] validatedTopics = topicValidator.validateAndLimitTopics(
                        assignedTopics,
                        textProcessor.removeIgnorePhrases(question.getQuestionText()));

                question.setTopics(validatedTopics);
                System.out.println("Assigned topics " + Arrays.toString(validatedTopics) +
                        " to question " + question.getQuestionNumber());
            } else {
                // If we couldn't get topics from batch processing, try individual processing
                identifyTopicsForSingleQuestion(question);
            }
        }
    }

    /**
     * Identify topics for a single question
     */
    private void identifyTopicsForSingleQuestion(Question question) {
        System.out.println("Processing individual question: " + question.getQuestionNumber());

        // Clean the question text by removing misleading phrases
        String cleanedText = textProcessor.removeIgnorePhrases(question.getQuestionText());

        // Try AI-based approach
        String[] suggestedTopics = aiTopicIdentifier.identifyTopicsForSingleQuestion(cleanedText);
        String[] validatedTopics = topicValidator.validateAndLimitTopics(suggestedTopics, cleanedText);

        if (validatedTopics.length > 0) {
            question.setTopics(validatedTopics);
            System.out.println("Assigned topics: " + Arrays.toString(validatedTopics));
            return;
        }

        // Try keyword-based approach as backup
        String[] keywordTopics = topicMatcher.findStrictTopicsByKeywords(cleanedText);
        if (keywordTopics.length > 0) {
            question.setTopics(keywordTopics);
            System.out.println("Assigned topics by keywords: " + Arrays.toString(keywordTopics));
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
     * Analyze the distribution of topics
     */
    private void analyzeTopicDistribution(List<Question> questions) {
        Map<String, Integer> yearDistribution = new HashMap<>();
        int questionsWithMultipleTopics = 0;

        for (Question question : questions) {
            if (question.getTopics() != null && question.getTopics().length > 0) {
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

        System.out.println("Questions with multiple topics: " + questionsWithMultipleTopics +
                " (" + String.format("%.1f%%", multipleTopicPercentage) + ")");

        // Check for any topics that appear too frequently
        for (Map.Entry<String, Integer> entry : yearDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            if (percentage > 70) { // Very high threshold since we're being strict
                System.out.println("WARNING: Topic '" + entry.getKey() + "' appears in " +
                        String.format("%.1f%%", percentage) + " of questions for this year.");
            }
        }
    }

    /**
     * Print overall topic distribution
     */
    private void printTopicDistribution() {
        System.out.println("\n--- TOPIC DISTRIBUTION (Strict categorization, max 3 topics) ---");

        // Sort topics by frequency
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(topicDistribution.entrySet());
        sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int totalTopicAssignments = sortedEntries.stream().mapToInt(Map.Entry::getValue).sum();

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            double percentage = (double) entry.getValue() / totalTopicAssignments * 100;
            System.out.printf("%-30s: %3d (%.1f%% of topic assignments)\n",
                    entry.getKey(), entry.getValue(), percentage);
        }

        System.out.println("Total topic assignments: " + totalTopicAssignments);
        System.out.println("-------------------------");
    }
}