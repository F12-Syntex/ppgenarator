package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openai.models.ChatModel;
import com.ppgenerator.types.Question;

public class Categorize {
    // Constants
    private static final int BATCH_SIZE = 5; // Reduced batch size for better accuracy
    private static final ChatModel OPENAI_MODEL = ChatModel.GPT_4_1_MINI;
    
    // A-level Edexcel Economics topics
    private static final String[] DEFAULT_TOPICS = {
        // Theme 1: Introduction to markets and market failure
        "scarcity and choice", "production possibility frontiers", "specialization and trade",
        "demand and supply", "price determination", "price mechanism", "consumer and producer surplus",
        "elasticity", "alternative market structures", "market failure", "externalities",
        "public goods", "information gaps", "government intervention",
        
        // Theme 2: The UK economy
        "economic growth", "inflation", "employment and unemployment", "balance of payments",
        "circular flow of income", "aggregate demand", "aggregate supply", "national income",
        "economic cycle", "monetary policy", "fiscal policy", "supply-side policies",
        
        // Theme 3: Business behavior and the labor market
        "business growth", "business objectives", "revenue", "costs", "economies of scale",
        "profit", "market structures", "perfect competition", "monopolistic competition",
        "oligopoly", "monopoly", "price discrimination", "contestable markets", "labor market",
        "wage determination", "labor market failure",
        
        // Theme 4: Global perspective
        "international economics", "absolute and comparative advantage", "terms of trade",
        "trading blocs", "world trade organization", "balance of payments", "exchange rates",
        "international competitiveness", "poverty and inequality", "developing economies",
        "financial markets", "economic development", "sustainability",
    };

    // Map for keyword-based topic identification
    private final Map<String, List<String>> topicKeywords = new HashMap<>();
    
    // Instance variables
    private final File outputFolder;
    private final String[] topics;
    private final boolean useDynamicTopics;
    
    // Tracking for quality control
    private final Map<String, Integer> topicDistribution = new HashMap<>();

    /**
     * Constructor with default topics
     * @param outputFolder The folder to save categorized question files
     */
    public Categorize(File outputFolder) {
        this(outputFolder, DEFAULT_TOPICS, null);
    }

    /**
     * Constructor with custom topics
     * @param outputFolder The folder to save categorized question files
     * @param topics Custom topics to use for categorization
     * @param optionalTopics Additional topics to consider (ignored in this implementation)
     */
    public Categorize(File outputFolder, String[] topics, String[] optionalTopics) {
        this.outputFolder = outputFolder;
        this.topics = topics;
        this.useDynamicTopics = (topics == null || topics.length == 0);
        
        // Create output folder if it doesn't exist
        if (!this.outputFolder.exists()) {
            this.outputFolder.mkdirs();
        }
        
        // Initialize topic keywords for better matching
        initializeTopicKeywords();
    }
    
    /**
     * Initialize the topic keywords map for better topic matching
     */
    private void initializeTopicKeywords() {
        // Theme 1
        addKeywords("scarcity and choice", "scarcity", "choice", "opportunity cost", "economic problem", "limited resources");
        addKeywords("production possibility frontiers", "ppf", "production possibility", "frontier", "transformation curve", "opportunity cost");
        addKeywords("specialization and trade", "specialization", "specialisation", "comparative advantage", "division of labor", "trade");
        addKeywords("demand and supply", "demand", "supply", "equilibrium", "market", "price mechanism");
        addKeywords("price determination", "price", "equilibrium", "market clearing", "surplus", "shortage");
        addKeywords("price mechanism", "price", "signal", "mechanism", "rationing", "allocation");
        addKeywords("consumer and producer surplus", "consumer surplus", "producer surplus", "welfare", "deadweight loss");
        addKeywords("elasticity", "elastic", "inelastic", "price elasticity", "income elasticity", "cross elasticity", "ped", "yed", "xed");
        addKeywords("alternative market structures", "market structure", "competition", "monopoly", "oligopoly", "monopolistic");
        addKeywords("market failure", "market failure", "inefficient", "externality", "social cost", "merit goods");
        addKeywords("externalities", "externality", "social cost", "social benefit", "negative externality", "positive externality", "spillover");
        addKeywords("public goods", "public good", "free-rider", "non-rival", "non-excludable", "market provision");
        addKeywords("information gaps", "information", "asymmetric information", "imperfect information");
        addKeywords("government intervention", "government", "intervention", "regulation", "subsidy", "tax", "price control");
        
        // Theme 2
        addKeywords("economic growth", "economic growth", "gdp", "real gdp", "long run", "actual growth", "potential growth");
        addKeywords("inflation", "inflation", "cpi", "price level", "deflation", "hyperinflation", "cost-push", "demand-pull");
        addKeywords("employment and unemployment", "unemployment", "employment", "jobless", "labor force", "participation rate");
        addKeywords("balance of payments", "balance of payments", "current account", "financial account", "capital account", "deficit", "surplus");
        addKeywords("circular flow of income", "circular flow", "leakages", "injections", "withdrawals", "national income");
        addKeywords("aggregate demand", "aggregate demand", "ad", "consumption", "investment", "government spending", "net exports");
        addKeywords("aggregate supply", "aggregate supply", "as", "sras", "lras", "production", "potential output");
        addKeywords("national income", "national income", "gdp", "gnp", "nominal", "real", "output");
        addKeywords("economic cycle", "economic cycle", "business cycle", "boom", "recession", "slump", "recovery");
        addKeywords("monetary policy", "monetary policy", "interest rate", "money supply", "central bank", "quantitative easing");
        addKeywords("fiscal policy", "fiscal policy", "government spending", "taxation", "budget", "deficit", "surplus");
        addKeywords("supply-side policies", "supply-side", "productivity", "competitiveness", "deregulation", "privatization");
        
        // Theme 3
        addKeywords("business growth", "business growth", "merger", "acquisition", "organic growth", "integration");
        addKeywords("business objectives", "business objective", "profit maximization", "revenue maximization", "sales");
        addKeywords("revenue", "revenue", "total revenue", "average revenue", "marginal revenue", "sales");
        addKeywords("costs", "cost", "fixed cost", "variable cost", "total cost", "average cost", "marginal cost");
        addKeywords("economies of scale", "economies of scale", "diseconomies", "increasing returns", "long run average cost");
        addKeywords("profit", "profit", "loss", "profit maximization", "normal profit", "supernormal profit");
        addKeywords("market structures", "market structure", "competition", "competitive", "concentration ratio");
        addKeywords("perfect competition", "perfect competition", "price taker", "homogeneous", "many firms");
        addKeywords("monopolistic competition", "monopolistic competition", "product differentiation", "brand");
        addKeywords("oligopoly", "oligopoly", "interdependence", "few sellers", "collusion", "cartel", "price war");
        addKeywords("monopoly", "monopoly", "price maker", "barriers to entry", "single seller", "price discrimination");
        addKeywords("price discrimination", "price discrimination", "first degree", "second degree", "third degree");
        addKeywords("contestable markets", "contestable market", "barrier to entry", "barrier to exit", "sunk cost");
        addKeywords("labor market", "labor market", "labour market", "wage", "employment", "monopsony");
        addKeywords("wage determination", "wage", "determination", "supply of labor", "demand for labor", "equilibrium wage");
        addKeywords("labor market failure", "labor market failure", "minimum wage", "discrimination", "immobility");
        
        // Theme 4
        addKeywords("international economics", "international", "global", "trade", "foreign", "world economy");
        addKeywords("absolute and comparative advantage", "absolute advantage", "comparative advantage", "opportunity cost", "specialization");
        addKeywords("terms of trade", "terms of trade", "exchange ratio", "export prices", "import prices");
        addKeywords("trading blocs", "trading bloc", "regional", "free trade", "eu", "nafta", "asean", "customs union");
        addKeywords("world trade organization", "wto", "world trade organization", "trade liberalization", "gatt");
        addKeywords("exchange rates", "exchange rate", "currency", "appreciation", "depreciation", "devaluation", "revaluation");
        addKeywords("international competitiveness", "competitiveness", "international", "productivity", "unit labor cost");
        addKeywords("poverty and inequality", "poverty", "inequality", "income distribution", "wealth distribution", "gini coefficient");
        addKeywords("developing economies", "developing", "development", "third world", "less developed", "emerging");
        addKeywords("financial markets", "financial market", "stock market", "bond market", "forex", "securities");
        addKeywords("economic development", "development", "growth", "hdl", "standard of living", "quality of life");
        addKeywords("sustainability", "sustainable", "sustainability", "environment", "green", "renewable", "future generations");
    }

    /**
     * Helper method to add keywords for a topic
     */
    private void addKeywords(String topic, String... keywords) {
        topicKeywords.put(topic, Arrays.asList(keywords));
    }

    /**
     * Process a list of questions to categorize them by topic
     * @param questions The list of questions to process
     * @throws JSONException If there's an error processing JSON
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
     * @param questions The list of questions to group
     * @return A map of year to list of questions
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
     * @param questions The list of questions to process
     */
    private void processQuestionBatches(List<Question> questions) {
        // First load all question content
        for (Question question : questions) {
            loadQuestionText(question);
        }

        // Process in batches for topic identification
        for (int i = 0; i < questions.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, questions.size());
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
     * @param question The question to load text for
     */
    private void loadQuestionText(Question question) {
        // Skip if question text is already loaded
        if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
            return;
        }

        System.out.println("Loading question content for: " + question.getQuestionNumber() + " from year " + question.getYear());

        if (question.getQuestion() != null) {
            String questionContent = extractTextFromPDF(question.getQuestion());
            questionContent = cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);
        }
    }

    /**
     * Identify topics for a batch of questions with improved handling
     * @param questions The batch of questions to process
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

        try {
            // Improved batch prompt with clearer instructions and formatting
            StringBuilder batchPrompt = new StringBuilder();
            batchPrompt.append("You are an expert in A-level Economics who specializes in categorizing exam questions.\n\n");
            batchPrompt.append("Below are ").append(questionsNeedingTopics.size()).append(" economics exam questions.\n");
            batchPrompt.append("For each question, determine the SINGLE most relevant topic from this list:\n");
            batchPrompt.append(String.join(", ", topics)).append("\n\n");
            
            batchPrompt.append("IMPORTANT INSTRUCTIONS:\n");
            batchPrompt.append("1. Analyze each question carefully for economic concepts and terminology\n");
            batchPrompt.append("2. Select EXACTLY ONE topic from the provided list - no other topics are allowed\n");
            batchPrompt.append("3. Respond in the format \"Question X: [topic]\"\n");
            batchPrompt.append("4. Consider what economic knowledge is required to answer the question\n");
            batchPrompt.append("5. Focus on the primary concept being tested, not just mentioned\n\n");
            
            for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                Question question = questionsNeedingTopics.get(i);
                batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
                batchPrompt.append(question.getQuestionText()).append("\n\n");
            }
            
            // Add final reminder and explicit output format
            batchPrompt.append("REMINDER: For each question, select EXACTLY ONE topic from the provided list.\n");
            batchPrompt.append("Format your response as a simple list:\n");
            batchPrompt.append("Question 1: [topic]\nQuestion 2: [topic]\n...\n\n");
            batchPrompt.append("Use the EXACT topic names from the list provided above.");

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.3);
            String response = openAI.query(batchPrompt.toString());
            
            // Process the response using regex to extract question-topic pairs
            Map<Integer, String> topicAssignments = parseTopicAssignments(response, questionsNeedingTopics.size());
            
            // If we couldn't parse any topics, try a JSON format request
            if (topicAssignments.isEmpty()) {
                System.out.println("Standard format parsing failed. Trying JSON format...");
                topicAssignments = requestJsonFormatResponse(questionsNeedingTopics);
            }
            
            // Assign topics to questions
            for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                Question question = questionsNeedingTopics.get(i);
                
                if (topicAssignments.containsKey(i + 1)) {
                    String topic = validateTopic(topicAssignments.get(i + 1));
                    question.setTopics(new String[] { topic });
                    System.out.println("Assigned topic '" + topic + "' to question " + question.getQuestionNumber());
                } else {
                    // If we couldn't get a topic from batch processing, try individual processing
                    identifyTopicForSingleQuestion(question);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in batch processing: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to individual processing
            for (Question question : questionsNeedingTopics) {
                identifyTopicForSingleQuestion(question);
            }
        }
    }
    
    /**
     * Parse topic assignments from the AI response using regex
     */
    private Map<Integer, String> parseTopicAssignments(String response, int expectedCount) {
        Map<Integer, String> results = new HashMap<>();
        
        // Pattern to match "Question X: topic"
        Pattern pattern = Pattern.compile("(?:Question|QUESTION)\\s+(\\d+)\\s*:\\s*([\\w\\s]+)");
        Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            try {
                int questionNum = Integer.parseInt(matcher.group(1));
                String topic = matcher.group(2).trim().toLowerCase();
                
                if (questionNum > 0 && questionNum <= expectedCount) {
                    results.put(questionNum, topic);
                }
            } catch (NumberFormatException e) {
                // Skip this match
            }
        }
        
        return results;
    }
    
    /**
     * Try to get a JSON formatted response as fallback
     */
    private Map<Integer, String> requestJsonFormatResponse(List<Question> questions) {
        Map<Integer, String> results = new HashMap<>();
        
        try {
            StringBuilder jsonPrompt = new StringBuilder();
            jsonPrompt.append("Analyze these economics questions and identify the main topic for each.\n\n");
            
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                jsonPrompt.append("QUESTION ").append(i + 1).append(":\n");
                jsonPrompt.append(question.getQuestionText()).append("\n\n");
            }
            
            jsonPrompt.append("Respond with a JSON array where each element contains 'questionNumber' and 'topic'.\n");
            jsonPrompt.append("Select topics ONLY from this list: ").append(String.join(", ", topics)).append("\n\n");
            jsonPrompt.append("Example format: [{\"questionNumber\": 1, \"topic\": \"demand and supply\"}, {\"questionNumber\": 2, \"topic\": \"elasticity\"}]");
            
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.3);
            String response = openAI.query(jsonPrompt.toString());
            
            // Extract JSON array from response
            JSONArray jsonArray = extractJsonArrayFromResponse(response);
            
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int questionNum = obj.getInt("questionNumber");
                    String topic = obj.getString("topic").toLowerCase().trim();
                    
                    results.put(questionNum, topic);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in JSON format request: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Extract a JSON array from a potentially text-wrapped response
     */
    private JSONArray extractJsonArrayFromResponse(String response) {
        try {
            // First try to parse the whole response as JSON
            return new JSONArray(response);
        } catch (JSONException e) {
            // Try to extract JSON array using regex
            Pattern pattern = Pattern.compile("\\[\\s*\\{.*\\}\\s*\\]", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                try {
                    return new JSONArray(matcher.group(0));
                } catch (JSONException ex) {
                    System.err.println("Found JSON-like text but couldn't parse: " + ex.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Identify topic for a single question with robust handling
     * @param question The question to identify topic for
     */
    private void identifyTopicForSingleQuestion(Question question) {
        System.out.println("Processing individual question: " + question.getQuestionNumber());
        
        // First try keyword-based approach
        String keywordTopic = findTopicByKeywords(question.getQuestionText());
        if (keywordTopic != null) {
            question.setTopics(new String[] { keywordTopic });
            System.out.println("Assigned topic by keywords: " + keywordTopic);
            return;
        }
        
        // If keywords don't work, try AI-based approach
        try {
            String prompt = String.format(
                    "As an economics expert, analyze this exam question and identify the SINGLE most relevant topic it tests.\n\n" +
                    "QUESTION:\n%s\n\n" +
                    "Choose EXACTLY ONE topic from this list:\n%s\n\n" +
                    "Respond with only the topic name, exactly as it appears in the list.",
                    question.getQuestionText(), String.join(", ", topics));

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1); // Lower temperature for more consistent results
            String response = openAI.query(prompt);
            
            // Clean and validate the response
            String topic = validateTopic(response);
            question.setTopics(new String[] { topic });
            System.out.println("Assigned topic by AI: " + topic);
            
        } catch (Exception e) {
            System.err.println("Error identifying topic for question " + question.getQuestionNumber() + ": " + e.getMessage());
            
            // Last resort - assign based on question content patterns
            String fallbackTopic = determineFallbackTopic(question.getQuestionText());
            question.setTopics(new String[] { fallbackTopic });
            System.out.println("Assigned fallback topic: " + fallbackTopic);
        }
    }
    
    /**
     * Find a topic based on keywords in the question text
     */
    private String findTopicByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return null;
        }
        
        questionText = questionText.toLowerCase();
        Map<String, Integer> topicScores = new HashMap<>();
        
        // Score each topic based on keyword matches
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int score = 0;
            for (String keyword : keywords) {
                if (questionText.contains(keyword.toLowerCase())) {
                    score++;
                    
                    // Give extra points for key phrases that are strong indicators
                    if (questionText.contains(topic.toLowerCase())) {
                        score += 2;
                    }
                }
            }
            
            if (score > 0) {
                topicScores.put(topic, score);
            }
        }
        
        // Find the topic with the highest score
        if (!topicScores.isEmpty()) {
            return topicScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Determine a fallback topic based on content analysis
     */
    private String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "demand and supply"; // Default topic
        }
        
        questionText = questionText.toLowerCase();
        
        // Check for key economics concepts in priority order
        if (questionText.contains("demand") || questionText.contains("supply") || 
            questionText.contains("equilibrium") || questionText.contains("market")) {
            return "demand and supply";
        }
        
        if (questionText.contains("elastic") || questionText.contains("ped") || 
            questionText.contains("yed") || questionText.contains("xed")) {
            return "elasticity";
        }
        
        if (questionText.contains("monopoly") || questionText.contains("oligopoly") ||
            questionText.contains("perfect competition") || questionText.contains("market structure")) {
            return "market structures";
        }
        
        if (questionText.contains("inflation") || questionText.contains("price level") ||
            questionText.contains("cpi") || questionText.contains("deflation")) {
            return "inflation";
        }
        
        if (questionText.contains("government") || questionText.contains("policy") ||
            questionText.contains("intervention") || questionText.contains("regulation")) {
            return "government intervention";
        }
        
        if (questionText.contains("international") || questionText.contains("trade") ||
            questionText.contains("export") || questionText.contains("import") ||
            questionText.contains("global")) {
            return "international economics";
        }
        
        if (questionText.contains("growth") || questionText.contains("gdp") ||
            questionText.contains("output") || questionText.contains("national income")) {
            return "economic growth";
        }
        
        if (questionText.contains("externality") || questionText.contains("social cost") ||
            questionText.contains("social benefit") || questionText.contains("spillover")) {
            return "externalities";
        }
        
        if (questionText.contains("cost") || questionText.contains("revenue") ||
            questionText.contains("profit") || questionText.contains("loss")) {
            return "costs";
        }
        
        // If no specific pattern matches, return a common topic as default
        return "demand and supply";
    }
    
    /**
     * Validate and clean a topic response
     */
    private String validateTopic(String topicResponse) {
        if (topicResponse == null) {
            return "demand and supply"; // Default topic
        }
        
        // Clean up the response
        String cleanedTopic = topicResponse.trim()
                .replaceAll("[\"'.]", "")
                .replaceAll("Topic:?\\s*", "")
                .replaceAll("The topic is:?\\s*", "")
                .toLowerCase();
        
        // Check if the topic is in our list
        for (String validTopic : topics) {
            if (cleanedTopic.equals(validTopic)) {
                return validTopic;
            }
        }
        
        // Try to find partial matches
        for (String validTopic : topics) {
            if (cleanedTopic.contains(validTopic) || validTopic.contains(cleanedTopic)) {
                return validTopic;
            }
        }
        
        // If we still don't have a match, use similarity matching
        return findSimilarTopic(cleanedTopic);
    }
    
    /**
     * Find the most similar topic using Jaccard similarity
     */
    private String findSimilarTopic(String suggestedTopic) {
        double bestSimilarity = 0.0;
        String bestMatch = "demand and supply"; // Default fallback
        
        for (String validTopic : topics) {
            double similarity = calculateJaccardSimilarity(suggestedTopic, validTopic);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = validTopic;
            }
        }
        
        // If we don't have a good match, try keyword-based approach
        if (bestSimilarity < 0.2) {
            for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (suggestedTopic.contains(keyword)) {
                        return entry.getKey();
                    }
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Calculate Jaccard similarity between two strings
     */
    private double calculateJaccardSimilarity(String s1, String s2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(s1.toLowerCase().split("\\W+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.toLowerCase().split("\\W+")));
        
        set1.remove(""); // Remove empty strings
        set2.remove("");
        
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }

    /**
     * Clean question text by removing unnecessary content
     * @param text The text to clean
     * @return Cleaned text
     */
    private String cleanQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove page numbers/identifiers
        text = text.replaceAll("\\*P\\d+A\\d+\\*", "");

        // Remove "DO NOT WRITE IN THIS AREA" and variations
        text = text.replaceAll(
                "D\\s*O\\s*N\\s*O\\s*T\\s*W\\s*R\\s*I\\s*T\\s*E\\s*I\\s*N\\s*T\\s*H\\s*I\\s*S\\s*A\\s*R\\s*E\\s*A", "");

        // Remove repeated dots (common in exam papers for fill-in spaces)
        text = text.replaceAll("\\.{2,}", " ");
        text = text.replaceAll("\\. \\.", " ");
        text = text.replaceAll("  \\.", " ");

        // Remove repeated "PMT" markings
        text = text.replaceAll("\\bPMT\\b", "");

        // Remove unicode placeholder characters
        text = text.replaceAll("\\?+", "");

        // Remove page numbers and headers/footers
        text = text.replaceAll("(?m)^\\d+$", "");
        text = text.replaceAll("(?i)\\bP\\d+\\b", "");
        text = text.replaceAll("(?i)turn over", "");
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?i)continue on the next page", "");

        // Remove question numbering and marks information
        text = text.replaceAll("\\(Total for Question \\d+:? \\d+ marks?\\)", "");
        text = text.replaceAll("\\(Total for Question \\d+ = \\d+ marks?\\)", "");
        text = text.replaceAll("TOTAL FOR SECTION [A-Z] = \\d+ MARKS", "");
        text = text.replaceAll("\\(\\d+ marks?\\)", "");

        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ");
        
        return text.trim();
    }

    /**
     * Export questions to a JSON file
     * @param questions The questions to export
     * @param outputFile The file to export to
     * @throws JSONException If there's an error processing JSON
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

            System.out.println("Successfully exported " + questions.size() + " questions to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing questions to JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract text content from a PDF file
     * @param pdfFile The PDF file to extract text from
     * @return The extracted text
     */
    private String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF file: " + pdfFile.getName());
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Analyze the distribution of topics in questions
     */
    private void analyzeTopicDistribution(List<Question> questions) {
        Map<String, Integer> yearDistribution = new HashMap<>();
        
        for (Question question : questions) {
            if (question.getTopics() != null && question.getTopics().length > 0) {
                String topic = question.getTopics()[0];
                
                // Update year distribution
                yearDistribution.put(topic, yearDistribution.getOrDefault(topic, 0) + 1);
                
                // Update overall distribution
                topicDistribution.put(topic, topicDistribution.getOrDefault(topic, 0) + 1);
            }
        }
        
        // Check for suspicious patterns (too many of the same topic)
        int totalQuestions = questions.size();
        for (Map.Entry<String, Integer> entry : yearDistribution.entrySet()) {
            double percentage = (double) entry.getValue() / totalQuestions * 100;
            if (percentage > 30) {
                System.out.println("WARNING: Topic '" + entry.getKey() + "' appears in " + 
                        String.format("%.1f%%", percentage) + " of questions for this year. This may indicate categorization issues.");
            }
        }
    }
    
    /**
     * Print overall topic distribution
     */
    private void printTopicDistribution() {
        System.out.println("\n--- TOPIC DISTRIBUTION ---");
        
        // Sort topics by frequency
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(topicDistribution.entrySet());
        sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        int total = sortedEntries.stream().mapToInt(Map.Entry::getValue).sum();
        
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            double percentage = (double) entry.getValue() / total * 100;
            System.out.printf("%-30s: %3d (%.1f%%)\n", entry.getKey(), entry.getValue(), percentage);
        }
        
        System.out.println("-------------------------");
    }
}