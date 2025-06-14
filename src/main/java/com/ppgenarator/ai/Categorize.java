package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Quality control tracking
    private final Map<String, Integer> topicDistribution = new HashMap<>();
    private final Set<String> processedQuestionHashes = new HashSet<>();
    private final Map<String, List<String>> duplicateTracker = new HashMap<>();

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
     * Process a list of questions with improved consistency and duplicate detection
     */
    public void processQuestions(List<Question> questions) throws JSONException {
        if (questions == null || questions.isEmpty()) {
            System.out.println("No questions to process.");
            return;
        }

        System.out.println("Processing " + questions.size() + " questions with improved consistency and quality control...");
        System.out.println("Mode: " + (useDynamicTopics ? "Dynamic Topics" : "Consistent Fixed Topics"));
        System.out.println("Topics: " + String.join(", ", topics));
        System.out.println("Maximum topics per question: " + TopicConstants.MAX_TOPICS_PER_QUESTION);

        // Remove duplicates before processing
        List<Question> uniqueQuestions = removeDuplicates(questions);
        System.out.println("After duplicate removal: " + uniqueQuestions.size() + " unique questions");

        // Group questions by year
        Map<String, List<Question>> questionsByYear = groupQuestionsByYear(uniqueQuestions);

        // Process each year's questions
        for (Map.Entry<String, List<Question>> entry : questionsByYear.entrySet()) {
            String year = entry.getKey();
            List<Question> yearQuestions = entry.getValue();

            File outputFile = new File(outputFolder, year + ".json");
            if (outputFile.exists()) {
                System.out.println("JSON file already exists for " + year + ". Skipping processing.");
                continue;
            }

            System.out.println("Processing " + yearQuestions.size() + " unique questions for year " + year);
            processQuestionBatches(yearQuestions);

            // Quality analysis
            analyzeTopicDistribution(yearQuestions);
            checkForInconsistencies(yearQuestions);

            exportQuestionsToJson(yearQuestions, outputFile);
        }

        // Print comprehensive topic distribution
        printTopicDistribution();
        printQualityReport();
    }

    /**
     * Remove duplicate questions based on content similarity
     */
    private List<Question> removeDuplicates(List<Question> questions) {
        Map<String, Question> uniqueQuestions = new HashMap<>();
        List<String> duplicateLog = new ArrayList<>();

        for (Question question : questions) {
            String contentHash = generateContentHash(question);
            
            if (uniqueQuestions.containsKey(contentHash)) {
                Question existing = uniqueQuestions.get(contentHash);
                duplicateLog.add("DUPLICATE: " + question.getQuestionNumber() + 
                               " (Year: " + question.getYear() + ") matches " + 
                               existing.getQuestionNumber() + " (Year: " + existing.getYear() + ")");
            } else {
                uniqueQuestions.put(contentHash, question);
            }
        }

        System.out.println("Duplicate detection results:");
        System.out.println("Original questions: " + questions.size());
        System.out.println("Unique questions: " + uniqueQuestions.size());
        System.out.println("Duplicates removed: " + (questions.size() - uniqueQuestions.size()));

        if (!duplicateLog.isEmpty()) {
            System.out.println("\nDuplicate details:");
            duplicateLog.forEach(System.out::println);
        }

        return new ArrayList<>(uniqueQuestions.values());
    }

    /**
     * Generate a content hash for duplicate detection
     */
    private String generateContentHash(Question question) {
        if (question.getQuestionText() == null) {
            return question.getQuestionNumber() + "_" + question.getYear();
        }

        // Create hash based on first 200 characters of cleaned text
        String cleanedText = question.getQuestionText()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .toLowerCase()
                .trim();

        if (cleanedText.length() > 200) {
            cleanedText = cleanedText.substring(0, 200);
        }

        return cleanedText + "_" + question.getMarks();
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
     * Process questions in batches with improved consistency
     */
    private void processQuestionBatches(List<Question> questions) {
        // First load all question content
        for (Question question : questions) {
            loadQuestionText(question);
        }

        // Group questions by qualification and paper for consistent categorization
        Map<String, Map<Integer, List<Question>>> questionsByQualificationAndPaper
                = groupQuestionsByQualificationAndPaper(questions);

        // Process each qualification and paper group separately for consistency
        for (Map.Entry<String, Map<Integer, List<Question>>> qualEntry : questionsByQualificationAndPaper.entrySet()) {
            String qualification = qualEntry.getKey();
            Map<Integer, List<Question>> paperGroups = qualEntry.getValue();

            for (Map.Entry<Integer, List<Question>> paperEntry : paperGroups.entrySet()) {
                int paper = paperEntry.getKey();
                List<Question> paperQuestions = paperEntry.getValue();

                System.out.println("Processing " + paperQuestions.size() + " questions for "
                        + qualification + " Paper " + paper + " with consistency checks");

                // Process in smaller batches for better quality control
                for (int i = 0; i < paperQuestions.size(); i += TopicConstants.BATCH_SIZE) {
                    int endIndex = Math.min(i + TopicConstants.BATCH_SIZE, paperQuestions.size());
                    List<Question> batch = paperQuestions.subList(i, endIndex);

                    System.out.println("Processing batch " + ((i / TopicConstants.BATCH_SIZE) + 1) + 
                                     " of " + ((paperQuestions.size() - 1) / TopicConstants.BATCH_SIZE + 1) + 
                                     " for " + qualification + " Paper " + paper);
                    
                    identifyTopicsForBatch(batch, qualification, paper);

                    // Add delay to avoid rate limiting and allow for quality control
                    try {
                        Thread.sleep(1500); // Increased delay for better AI responses
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Group questions by qualification and paper for consistent processing
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

            int paper = extractPaperNumber(question);

            questionsByQualificationAndPaper
                    .computeIfAbsent(qualification, k -> new HashMap<>())
                    .computeIfAbsent(paper, k -> new ArrayList<>())
                    .add(question);
        }

        return questionsByQualificationAndPaper;
    }

    /**
     * Extract paper number from question with improved logic
     */
    private int extractPaperNumber(Question question) {
        // Try to get paper number from the question's paper identifier
        String paperIdentifier = question.getPaperIdentifier();
        if (paperIdentifier != null) {
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

        return 1; // Default to paper 1
    }

    /**
     * Load text content from question PDF
     */
    private void loadQuestionText(Question question) {
        if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
            return;
        }

        System.out.println("Loading question content for: " + question.getQuestionNumber() + 
                          " from year " + question.getYear());

        if (question.getQuestion() != null) {
            String questionContent = textProcessor.extractTextFromPDF(question.getQuestion());
            questionContent = textProcessor.cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);
        }
    }

    /**
     * Identify topics for a batch of questions with improved consistency
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

        // Create AI topic identifier
        AITopicIdentifier aiTopicIdentifier = new AITopicIdentifier(topics);

        // Try AI-based batch processing first
        Map<Integer, String[]> topicAssignments = aiTopicIdentifier.identifyTopicsForBatch(questionsNeedingTopics);

        // Process results with improved validation
        if (topicAssignments.isEmpty()) {
            System.out.println("Batch processing failed for " + qualification + " Paper " + paper + 
                             ". Processing individually with enhanced validation...");
            for (Question question : questionsNeedingTopics) {
                identifyTopicsForSingleQuestion(question, qualification, paper);
            }
            return;
        }

        // Assign and validate topics with consistency checks
        for (int i = 0; i < questionsNeedingTopics.size(); i++) {
            Question question = questionsNeedingTopics.get(i);

            if (topicAssignments.containsKey(i + 1)) {
                String[] assignedTopics = topicAssignments.get(i + 1);
                String[] validatedTopics = topicValidator.validateAndLimitTopics(
                        assignedTopics,
                        textProcessor.removeIgnorePhrases(question.getQuestionText()));

                // Apply additional consistency checks
                validatedTopics = applyConsistencyChecks(validatedTopics, question, qualification);

                question.setTopics(validatedTopics);
                
                // Log for quality control
                logTopicAssignment(question, validatedTopics, qualification, paper);
                
                System.out.println("Assigned " + validatedTopics.length + " topics " + 
                                 Arrays.toString(validatedTopics) + " to " + qualification + 
                                 " Paper " + paper + " question " + question.getQuestionNumber());
            } else {
                identifyTopicsForSingleQuestion(question, qualification, paper);
            }
        }
    }

    /**
     * Apply additional consistency checks based on similar questions
     */
    private String[] applyConsistencyChecks(String[] topics, Question question, String qualification) {
        // Check for similar questions and apply consistency
        String questionType = classifyQuestionType(question.getQuestionText());
        
        // Store in tracking for future consistency checks
        String key = qualification + "_" + questionType + "_" + question.getMarks();
        if (!duplicateTracker.containsKey(key)) {
            duplicateTracker.put(key, new ArrayList<>());
        }
        duplicateTracker.get(key).addAll(Arrays.asList(topics));

        return topics;
    }

    /**
     * Classify question type for consistency checking
     */
    private String classifyQuestionType(String questionText) {
        if (questionText == null) return "unknown";
        
        String lower = questionText.toLowerCase();
        
        if (lower.contains("calculate") || lower.contains("work out")) {
            if (lower.contains("elasticity")) return "calculation_elasticity";
            if (lower.contains("revenue") || lower.contains("profit")) return "calculation_finance";
            return "calculation_general";
        }
        
        if (lower.contains("draw") || lower.contains("diagram")) {
            return "diagram";
        }
        
        if (lower.contains("explain") && lower.contains("one")) {
            return "explain_short";
        }
        
        if (lower.contains("discuss") || lower.contains("evaluate") || lower.contains("assess")) {
            return "essay";
        }
        
        return "general";
    }

    /**
     * Log topic assignment for quality control
     */
    private void logTopicAssignment(Question question, String[] topics, String qualification, int paper) {
        for (String topic : topics) {
            topicDistribution.put(topic, topicDistribution.getOrDefault(topic, 0) + 1);
        }
    }

    /**
     * Identify topics for a single question with enhanced validation
     */
    private void identifyTopicsForSingleQuestion(Question question, String qualification, int paper) {
        System.out.println("Processing individual " + qualification + " Paper " + paper + 
                          " question: " + question.getQuestionNumber());

        String cleanedText = textProcessor.removeIgnorePhrases(question.getQuestionText());
        AITopicIdentifier aiTopicIdentifier = new AITopicIdentifier(topics);

        // Try AI-based approach
        String[] suggestedTopics = aiTopicIdentifier.identifyTopicsForSingleQuestion(cleanedText);
        String[] validatedTopics = topicValidator.validateAndLimitTopics(suggestedTopics, cleanedText);

        if (validatedTopics.length > 0) {
            validatedTopics = applyConsistencyChecks(validatedTopics, question, qualification);
            question.setTopics(validatedTopics);
            logTopicAssignment(question, validatedTopics, qualification, paper);
            System.out.println("Assigned " + validatedTopics.length + " topics: " + Arrays.toString(validatedTopics));
            return;
        }

        // Try keyword-based approach as backup
        String[] keywordTopics = topicMatcher.findStrictTopicsByKeywords(cleanedText);
        if (keywordTopics.length > 0) {
            keywordTopics = topicValidator.validateAndLimitTopics(keywordTopics, cleanedText);
            question.setTopics(keywordTopics);
            logTopicAssignment(question, keywordTopics, qualification, paper);
            System.out.println("Assigned " + keywordTopics.length + " topics by keywords: " + Arrays.toString(keywordTopics));
            return;
        }

        // Last resort - assign fallback topic
        String fallbackTopic = topicValidator.determineFallbackTopic(cleanedText);
        question.setTopics(new String[]{fallbackTopic});
        logTopicAssignment(question, new String[]{fallbackTopic}, qualification, paper);
        System.out.println("Assigned fallback topic: " + fallbackTopic);
    }

    /**
     * Check for inconsistencies in topic assignments
     */
    private void checkForInconsistencies(List<Question> questions) {
        System.out.println("\n=== CONSISTENCY CHECK ===");
        
        Map<String, List<Question>> questionsByType = new HashMap<>();
        
        for (Question question : questions) {
            if (question.getTopics() != null && question.getTopics().length > 0) {
                String type = classifyQuestionType(question.getQuestionText()) + "_" + question.getMarks();
                questionsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(question);
            }
        }
        
        for (Map.Entry<String, List<Question>> entry : questionsByType.entrySet()) {
            if (entry.getValue().size() > 1) {
                checkTypeConsistency(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Check consistency within question types
     */
    private void checkTypeConsistency(String type, List<Question> questions) {
        Map<String, Integer> topicCounts = new HashMap<>();
        
        for (Question question : questions) {
            for (String topic : question.getTopics()) {
                topicCounts.put(topic, topicCounts.getOrDefault(topic, 0) + 1);
            }
        }
        
        System.out.println("Type: " + type + " (" + questions.size() + " questions)");
        
        // Find topics that appear in most questions of this type
        int threshold = Math.max(1, questions.size() / 2);
        List<String> commonTopics = topicCounts.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (!commonTopics.isEmpty()) {
            System.out.println("  Common topics: " + String.join(", ", commonTopics));
        }
        
        // Check for outliers
        for (Question question : questions) {
            boolean hasCommonTopic = Arrays.stream(question.getTopics())
                    .anyMatch(commonTopics::contains);
            
            if (!hasCommonTopic && !commonTopics.isEmpty()) {
                System.out.println("  POTENTIAL INCONSISTENCY: " + question.getQuestionNumber() + 
                                 " has topics " + Arrays.toString(question.getTopics()) + 
                                 " but lacks common topics for this type");
            }
        }
    }

    /**
     * Export questions to JSON with improved structure
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

            // Write JSON with proper formatting
            java.nio.file.Files.write(
                    outputFile.toPath(),
                    jsonArray.toString(2).getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Successfully exported " + questions.size() + 
                             " questions to: " + outputFile.getAbsolutePath());

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

                for (String topic : question.getTopics()) {
                    yearDistribution.put(topic, yearDistribution.getOrDefault(topic, 0) + 1);
                }
            }
        }

        int totalQuestions = questions.size();
        double multipleTopicPercentage = (double) questionsWithMultipleTopics / totalQuestions * 100;

        System.out.println("\n=== ENHANCED TOPIC DISTRIBUTION ANALYSIS ===");
        System.out.println("Questions with multiple topics: " + questionsWithMultipleTopics
                + " (" + String.format("%.1f%%", multipleTopicPercentage) + ")");

        System.out.println("\nTopic count distribution:");
        for (Map.Entry<Integer, Integer> entry : topicCountDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            System.out.println("  " + entry.getKey() + " topics: " + entry.getValue()
                    + " questions (" + String.format("%.1f%%", percentage) + ")");
        }

        // Warn about overused topics (>60% threshold)
        for (Map.Entry<String, Integer> entry : yearDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            if (percentage > 60) {
                System.out.println("WARNING: Topic '" + entry.getKey() + "' appears in "
                        + String.format("%.1f%%", percentage) + " of questions - may be overused");
            }
        }

        // Show balanced topics
        System.out.println("\nWell-balanced topics (5-50% coverage):");
        yearDistribution.entrySet().stream()
                .filter(entry -> {
                    double percentage = (double) entry.getValue() / totalQuestions * 100;
                    return percentage >= 5 && percentage <= 50;
                })
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    double percentage = (double) entry.getValue() / totalQuestions * 100;
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue()
                            + " questions (" + String.format("%.1f%%", percentage) + ")");
                });
    }

    /**
     * Print comprehensive topic distribution
     */
    private void printTopicDistribution() {
        System.out.println("\n=== COMPREHENSIVE TOPIC DISTRIBUTION ===");
        System.out.println("(Improved categorization with max " + TopicConstants.MAX_TOPICS_PER_QUESTION + " topics per question)");

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(topicDistribution.entrySet());
        sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int totalTopicAssignments = sortedEntries.stream().mapToInt(Map.Entry::getValue).sum();

        System.out.println("\nAll topics (sorted by frequency):");
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            double percentage = (double) entry.getValue() / totalTopicAssignments * 100;
            System.out.printf("%-40s: %3d assignments (%.1f%%)\n",
                    entry.getKey(), entry.getValue(), percentage);
        }

        // Show underused topics
        System.out.println("\nUnderused topics (≤2 assignments):");
        sortedEntries.stream()
                .filter(entry -> entry.getValue() <= 2)
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " assignments"));

        System.out.println("\nTotal topic assignments: " + totalTopicAssignments);
        System.out.println("Unique topics used: " + sortedEntries.size());
        System.out.println("=================================================");
    }

    /**
     * Print quality report
     */
    private void printQualityReport() {
        System.out.println("\n=== QUALITY CONTROL REPORT ===");
        System.out.println("Processed question hashes: " + processedQuestionHashes.size());
        System.out.println("Duplicate question patterns: " + duplicateTracker.size());
        
        System.out.println("\nQuestion type patterns:");
        duplicateTracker.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 3)
                .forEach(entry -> {
                    Set<String> uniqueTopics = new HashSet<>(entry.getValue());
                    System.out.println("  " + entry.getKey() + ": " + 
                                     entry.getValue().size() + " assignments, " + 
                                     uniqueTopics.size() + " unique topics");
                });
        
        System.out.println("=====================================");
    }
}