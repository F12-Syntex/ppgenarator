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
    private static final int BATCH_SIZE = 3; // Reduced batch size for better accuracy
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
    
    // Map for concept relationships to handle broader economic concepts
    private final Map<String, List<String>> conceptRelationships = new HashMap<>();
    
    // Phrases that should be ignored when determining the topic
    private final List<String> ignorePhrases = Arrays.asList(
        "using the data from the extract",
        "using information from the extract",
        "refer to the extract",
        "based on the extract",
        "from the information provided",
        "using the information",
        "using figure",
        "using table",
        "using diagram",
        "using the graph",
        "according to the data",
        "from the data shown"
    );
    
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
        
        // Initialize concept relationships
        initializeConceptRelationships();
    }
    
    /**
     * Initialize the topic keywords map for better topic matching
     */
    private void initializeTopicKeywords() {
        // Theme 1
        addKeywords("scarcity and choice", "scarcity", "choice", "opportunity cost", "economic problem", "limited resources", "unlimited wants", "resource allocation", "economics is about", "basic economic problem");
        addKeywords("production possibility frontiers", "ppf", "production possibility", "frontier", "transformation curve", "opportunity cost", "trade-off", "attainable", "unattainable", "efficient production", "productive efficiency");
        addKeywords("specialization and trade", "specialization", "specialisation", "comparative advantage", "absolute advantage", "division of labor", "trade", "trading", "specializing", "gains from trade", "mutually beneficial");
        addKeywords("demand and supply", "demand curve", "supply curve", "equilibrium", "market equilibrium", "price mechanism", "quantity demanded", "quantity supplied", "market forces", "shifts in demand", "shifts in supply");
        addKeywords("price determination", "price", "equilibrium price", "market clearing", "surplus", "shortage", "price floor", "price ceiling", "rationing", "allocating", "market price");
        addKeywords("price mechanism", "price signal", "signaling function", "rationing function", "incentive function", "allocation of resources", "invisible hand", "market mechanism", "resource allocation");
        addKeywords("consumer and producer surplus", "consumer surplus", "producer surplus", "total surplus", "economic welfare", "deadweight loss", "welfare loss", "welfare gain", "economic efficiency", "consumer benefit", "producer benefit");
        addKeywords("elasticity", "elastic", "inelastic", "price elasticity", "income elasticity", "cross elasticity", "ped", "yed", "xed", "elasticity coefficient", "elastic supply", "elastic demand", "unitary elasticity", "responsiveness");
        addKeywords("alternative market structures", "market structure", "competition", "monopoly", "oligopoly", "monopolistic competition", "perfect competition", "competitive markets", "concentration ratio", "market power", "barriers to entry");
        addKeywords("market failure", "market failure", "inefficient allocation", "socially optimal", "social optimum", "merit goods", "demerit goods", "private cost", "social cost", "inefficiency", "welfare loss", "market solution");
        addKeywords("externalities", "externality", "social cost", "social benefit", "negative externality", "positive externality", "spillover", "external costs", "external benefits", "third party", "pollution", "marginal external cost");
        addKeywords("public goods", "public good", "free-rider", "non-rival", "non-excludable", "market provision", "collective provision", "private provision", "non-rivalrous", "non-excludability", "government provision", "lighthouse");
        addKeywords("information gaps", "information", "asymmetric information", "imperfect information", "adverse selection", "moral hazard", "incomplete information", "information failure", "hidden knowledge", "hidden action");
        addKeywords("government intervention", "government", "intervention", "regulation", "subsidy", "tax", "price control", "quota", "buffer stock", "legislation", "state provision", "government failure", "policy");
        
        // Theme 2
        addKeywords("economic growth", "economic growth", "gdp", "real gdp", "long run", "actual growth", "potential growth", "business cycle", "trend growth", "gdp per capita", "living standards", "sustainable growth", "productivity");
        addKeywords("inflation", "inflation", "cpi", "rpi", "price level", "deflation", "hyperinflation", "cost-push", "demand-pull", "wage inflation", "stagflation", "disinflation", "price stability", "indexation");
        addKeywords("employment and unemployment", "unemployment", "employment", "jobless", "labor force", "participation rate", "natural rate", "frictional unemployment", "structural unemployment", "cyclical unemployment", "full employment");
        addKeywords("balance of payments", "balance of payments", "current account", "financial account", "capital account", "deficit", "surplus", "trade deficit", "trade surplus", "import", "export", "net exports", "invisible trade");
        addKeywords("circular flow of income", "circular flow", "leakages", "injections", "withdrawals", "national income", "income flow", "spending flow", "circular flow diagram", "savings", "investment", "taxation", "government spending");
        addKeywords("aggregate demand", "aggregate demand", "ad", "consumption", "investment", "government spending", "net exports", "ad curve", "shifts in ad", "components of ad", "wealth effect", "interest rate effect", "multiplier effect");
        addKeywords("aggregate supply", "aggregate supply", "as", "sras", "lras", "production", "potential output", "aggregate supply curve", "productive capacity", "shifts in as", "output gap", "supply shock", "productive potential");
        addKeywords("national income", "national income", "gdp", "gnp", "nominal", "real", "output", "value added", "final goods", "intermediate goods", "national output", "income method", "expenditure method", "production method");
        addKeywords("economic cycle", "economic cycle", "business cycle", "boom", "recession", "slump", "recovery", "peak", "trough", "expansion", "contraction", "upturn", "downturn", "trade cycle", "fluctuations");
        addKeywords("monetary policy", "monetary policy", "interest rate", "money supply", "central bank", "quantitative easing", "bank rate", "discount rate", "liquidity", "monetary transmission", "inflation targeting", "repo rate");
        addKeywords("fiscal policy", "fiscal policy", "government spending", "taxation", "budget", "deficit", "surplus", "public finances", "fiscal stimulus", "austerity", "automatic stabilizers", "discretionary policy", "fiscal stance");
        addKeywords("supply-side policies", "supply-side", "productivity", "competitiveness", "deregulation", "privatization", "tax incentives", "labor market", "skills", "training", "infrastructure", "incentives", "enterprise");
        
        // Theme 3
        addKeywords("business growth", "business growth", "merger", "acquisition", "organic growth", "integration", "horizontal integration", "vertical integration", "conglomerate", "takeover", "economies of scale", "diversification");
        addKeywords("business objectives", "business objective", "profit maximization", "revenue maximization", "sales", "market share", "growth", "survival", "shareholder value", "stakeholder interests", "satisficing", "social responsibility");
        addKeywords("revenue", "revenue", "total revenue", "average revenue", "marginal revenue", "sales", "income", "turnover", "demand curve", "ar", "mr", "price elasticity", "pricing strategy");
        addKeywords("costs", "cost", "fixed cost", "variable cost", "total cost", "average cost", "marginal cost", "sunk cost", "opportunity cost", "accounting cost", "economic cost", "explicit cost", "implicit cost");
        addKeywords("economies of scale", "economies of scale", "diseconomies", "increasing returns", "long run average cost", "lrac", "minimum efficient scale", "internal economies", "external economies", "returns to scale");
        addKeywords("profit", "profit", "loss", "profit maximization", "normal profit", "supernormal profit", "economic profit", "accounting profit", "profitability", "profit margin", "profit motive", "return on capital");
        addKeywords("market structures", "market structure", "competition", "competitive", "concentration ratio", "herfindahl index", "barriers to entry", "market power", "market concentration", "competitive behavior", "strategic behavior");
        addKeywords("perfect competition", "perfect competition", "price taker", "homogeneous", "many firms", "free entry", "free exit", "perfect information", "normal profit", "allocative efficiency", "productive efficiency");
        addKeywords("monopolistic competition", "monopolistic competition", "product differentiation", "brand", "advertising", "unique selling point", "non-price competition", "brand loyalty", "entry", "exit", "short run profit", "long run");
        addKeywords("oligopoly", "oligopoly", "interdependence", "few sellers", "collusion", "cartel", "price war", "non-price competition", "game theory", "prisoners dilemma", "price leadership", "barriers to entry", "strategic behavior");
        addKeywords("monopoly", "monopoly", "price maker", "barriers to entry", "single seller", "price discrimination", "monopoly power", "market power", "deadweight loss", "inefficiency", "natural monopoly", "legal monopoly");
        addKeywords("price discrimination", "price discrimination", "first degree", "second degree", "third degree", "price targeting", "market segmentation", "price differentiation", "consumer surplus", "price elasticity", "market power");
        addKeywords("contestable markets", "contestable market", "barrier to entry", "barrier to exit", "sunk cost", "hit and run", "entry threat", "potential competition", "perfectly contestable", "entry deterrence", "limit pricing");
        addKeywords("labor market", "labor market", "labour market", "wage", "employment", "monopsony", "supply of labor", "demand for labor", "derived demand", "marginal revenue product", "marginal cost of labor", "wage rate");
        addKeywords("wage determination", "wage", "determination", "supply of labor", "demand for labor", "equilibrium wage", "minimum wage", "collective bargaining", "trade unions", "wage differentials", "labor productivity");
        addKeywords("labor market failure", "labor market failure", "minimum wage", "discrimination", "immobility", "geographic immobility", "occupational immobility", "information asymmetry", "monopsony", "exploitation", "wage inequality");
        
        // Theme 4
        addKeywords("international economics", "international", "global", "trade", "foreign", "world economy", "globalization", "international trade", "capital flows", "migration", "transnational", "economic integration");
        addKeywords("absolute and comparative advantage", "absolute advantage", "comparative advantage", "opportunity cost", "specialization", "trade gains", "ricardian model", "production possibility", "relative efficiency", "terms of trade");
        addKeywords("terms of trade", "terms of trade", "exchange ratio", "export prices", "import prices", "favorable terms", "unfavorable terms", "commodity terms", "income terms", "single factorial terms", "double factorial terms");
        addKeywords("trading blocs", "trading bloc", "regional", "free trade", "eu", "nafta", "asean", "customs union", "common market", "economic union", "free trade area", "preferential trade", "trade creation", "trade diversion");
        addKeywords("world trade organization", "wto", "world trade organization", "trade liberalization", "gatt", "uruguay round", "doha round", "trade dispute", "most favored nation", "national treatment", "protectionism");
        addKeywords("exchange rates", "exchange rate", "currency", "appreciation", "depreciation", "devaluation", "revaluation", "floating", "fixed", "managed float", "purchasing power parity", "currency market", "forex");
        addKeywords("international competitiveness", "competitiveness", "international", "productivity", "unit labor cost", "export performance", "import penetration", "comparative advantage", "exchange rate", "non-price factors");
        addKeywords("poverty and inequality", "poverty", "inequality", "income distribution", "wealth distribution", "gini coefficient", "lorenz curve", "relative poverty", "absolute poverty", "poverty line", "redistribution", "social mobility");
        addKeywords("developing economies", "developing", "development", "third world", "less developed", "emerging", "developing countries", "newly industrialized", "underdeveloped", "global south", "economic development", "industrialization");
        addKeywords("financial markets", "financial market", "stock market", "bond market", "forex", "securities", "capital market", "money market", "primary market", "secondary market", "financial intermediation", "speculation");
        addKeywords("economic development", "development", "growth", "hdl", "standard of living", "quality of life", "human development", "sustainable development", "millennium goals", "development gap", "development strategies");
        addKeywords("sustainability", "sustainable", "sustainability", "environment", "green", "renewable", "future generations", "sustainable development", "climate change", "carbon emissions", "resource depletion", "circular economy");
    }
    
    /**
     * Initialize concept relationships to understand broader economic relationships
     */
    private void initializeConceptRelationships() {
        // Map broader economic concepts to relevant topics
        
        // Market dynamics
        addRelatedConcepts("market", "demand and supply", "price determination", "price mechanism", "market structures");
        
        // Price-related concepts
        addRelatedConcepts("price", "demand and supply", "price determination", "price mechanism", "elasticity", "price discrimination");
        
        // Government-related concepts
        addRelatedConcepts("government", "government intervention", "fiscal policy", "monetary policy", "supply-side policies", "public goods");
        
        // International concepts
        addRelatedConcepts("international", "international economics", "absolute and comparative advantage", "balance of payments", "exchange rates", "trading blocs");
        
        // Growth and development
        addRelatedConcepts("growth", "economic growth", "business growth", "economic development", "developing economies");
        
        // Cost and revenue concepts
        addRelatedConcepts("cost", "costs", "profit", "revenue", "economies of scale", "externalities");
        
        // Market structure concepts
        addRelatedConcepts("competition", "market structures", "perfect competition", "monopolistic competition", "oligopoly", "monopoly", "contestable markets");
        
        // Labor market concepts
        addRelatedConcepts("labor", "labor market", "wage determination", "labor market failure", "employment and unemployment");
        
        // Macro policies
        addRelatedConcepts("policy", "fiscal policy", "monetary policy", "supply-side policies", "government intervention");
        
        // Social issues
        addRelatedConcepts("inequality", "poverty and inequality", "economic development", "sustainability");
    }
    
    /**
     * Helper method to add keywords for a topic
     */
    private void addKeywords(String topic, String... keywords) {
        topicKeywords.put(topic, Arrays.asList(keywords));
    }
    
    /**
     * Helper method to add related concepts
     */
    private void addRelatedConcepts(String concept, String... relatedTopics) {
        conceptRelationships.put(concept, Arrays.asList(relatedTopics));
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
            // Enhanced batch prompt with clearer instructions and context awareness
            StringBuilder batchPrompt = new StringBuilder();
            batchPrompt.append("You are an expert A-level Economics examiner who specializes in categorizing exam questions based on their core economic concept.\n\n");
            batchPrompt.append("I'll provide you with ").append(questionsNeedingTopics.size()).append(" economics exam questions.\n");
            batchPrompt.append("For each question, determine the PRIMARY economic concept being tested, not just mentioned in passing.\n\n");
            
            batchPrompt.append("IMPORTANT INSTRUCTIONS:\n");
            batchPrompt.append("1. IGNORE phrases like 'using the data from the extract' or 'refer to the figure' - these are just exam instructions\n");
            batchPrompt.append("2. Focus on the SPECIFIC economic concept or theory being tested, not the context or data source\n");
            batchPrompt.append("3. Consider what knowledge a student would need to answer the question successfully\n");
            batchPrompt.append("4. Select EXACTLY ONE topic from this list (no variations or combinations):\n");
            batchPrompt.append(String.join(", ", topics)).append("\n\n");
            
            batchPrompt.append("EXAMPLES:\n");
            batchPrompt.append("- If a question asks 'Using Figure 1, explain how price elasticity affects this firm's revenue' → Topic is 'elasticity'\n");
            batchPrompt.append("- If a question asks 'Using the extract, discuss government intervention in this market' → Topic is 'government intervention'\n");
            batchPrompt.append("- If a question requires calculation of consumer and producer surplus → Topic is 'consumer and producer surplus'\n\n");
            
            for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                Question question = questionsNeedingTopics.get(i);
                batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
                batchPrompt.append(question.getQuestionText()).append("\n\n");
            }
            
            batchPrompt.append("For each question, respond with ONLY the question number and the single most appropriate topic from the list.\n");
            batchPrompt.append("Format: 'Question 1: [topic]'\n");
            batchPrompt.append("Remember: Focus on the economic concept being tested, not the context or data source mentioned.");

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.2); // Lower temperature for more consistent results
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
            jsonPrompt.append("You are an expert in A-level Economics. Analyze these economics questions and identify the primary economic concept being tested in each.\n\n");
            
            jsonPrompt.append("IMPORTANT: Focus on the core economic concept being tested, NOT the context or data references like 'using the extract'.\n\n");
            
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                jsonPrompt.append("QUESTION ").append(i + 1).append(":\n");
                jsonPrompt.append(question.getQuestionText()).append("\n\n");
            }
            
            jsonPrompt.append("Respond with a JSON array where each element contains 'questionNumber' and 'topic'.\n");
            jsonPrompt.append("Select topics ONLY from this list: ").append(String.join(", ", topics)).append("\n\n");
            jsonPrompt.append("Example format: [{\"questionNumber\": 1, \"topic\": \"demand and supply\"}, {\"questionNumber\": 2, \"topic\": \"elasticity\"}]");
            
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.2);
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
     * Identify topic for a single question with significantly improved handling
     * @param question The question to identify topic for
     */
    private void identifyTopicForSingleQuestion(Question question) {
        System.out.println("Processing individual question: " + question.getQuestionNumber());
        
        // First, clean the question text by removing misleading phrases
        String cleanedText = removeIgnorePhrases(question.getQuestionText());
        
        // First try AI-based approach with improved prompting
        try {
            String prompt = String.format(
                    "You are an expert A-level Economics examiner. Analyze this economics exam question to identify its primary economic concept.\n\n" +
                    "QUESTION:\n%s\n\n" +
                    "INSTRUCTIONS:\n" +
                    "1. IGNORE contextual elements like 'refer to the extract' or 'using the data' - focus on the economic concept being tested\n" +
                    "2. Consider what specific economic knowledge a student needs to correctly answer this question\n" +
                    "3. Select EXACTLY ONE topic from this list:\n%s\n\n" +
                    "Examples:\n" +
                    "- Question about price changes affecting revenue → Topic: 'elasticity'\n" +
                    "- Question about government taxes to fix negative externalities → Topic: 'externalities'\n" +
                    "- Question about monopoly pricing strategies → Topic: 'monopoly'\n\n" +
                    "Respond with ONLY the topic name, exactly as it appears in the list.",
                    cleanedText, String.join(", ", topics));

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1); // Very low temperature for consistency
            String response = openAI.query(prompt);
            
            // Clean and validate the response
            String topic = validateTopic(response);
            
            // Perform an additional validation step by checking if the topic is appropriate
            String doubleCheckedTopic = performTopicCrossCheck(cleanedText, topic);
            
            question.setTopics(new String[] { doubleCheckedTopic });
            System.out.println("Assigned topic by AI: " + doubleCheckedTopic);
            
        } catch (Exception e) {
            System.err.println("Error identifying topic for question " + question.getQuestionNumber() + ": " + e.getMessage());
            
            // Try keyword-based approach as backup
            String keywordTopic = findTopicByKeywords(cleanedText);
            if (keywordTopic != null) {
                question.setTopics(new String[] { keywordTopic });
                System.out.println("Assigned topic by keywords: " + keywordTopic);
                return;
            }
            
            // Last resort - assign based on question content patterns
            String fallbackTopic = determineFallbackTopic(cleanedText);
            question.setTopics(new String[] { fallbackTopic });
            System.out.println("Assigned fallback topic: " + fallbackTopic);
        }
    }
    
    /**
     * Perform a cross-check of the assigned topic against the question content
     * This helps catch misclassifications based on superficial features
     */
    private String performTopicCrossCheck(String questionText, String initialTopic) {
        try {
            // Get the core economic concepts from the question
            String conceptPrompt = String.format(
                    "Analyze this economics exam question and list the key economic concepts it tests (maximum 3):\n\n%s\n\n" +
                    "Respond with just a comma-separated list of core economic concepts - no explanation needed.",
                    questionText);
            
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String conceptResponse = openAI.query(conceptPrompt);
            
            // Clean up the response
            String[] concepts = conceptResponse.toLowerCase()
                    .replaceAll("[\"'.]", "")
                    .split("\\s*,\\s*");
            
            // Check if any of these concepts strongly suggest a different topic
            for (String concept : concepts) {
                // Check for direct topic matches
                for (String topic : topics) {
                    if (concept.contains(topic) || topic.contains(concept)) {
                        // If this concept directly matches a topic other than our initial topic
                        if (!topic.equals(initialTopic) && (
                                concept.length() >= 5 || // Avoid matching short words
                                topic.startsWith(concept + " ") || // Match prefix words
                                topic.endsWith(" " + concept))) { // Match suffix words
                            // Verify the match with a second check
                            String verificationPrompt = String.format(
                                    "Based on this economics exam question:\n\n%s\n\n" +
                                    "Which topic is more appropriate: '%s' or '%s'? Respond with just the topic name.",
                                    questionText, initialTopic, topic);
                            
                            String verificationResponse = openAI.query(verificationPrompt).toLowerCase().trim();
                            
                            if (verificationResponse.contains(topic)) {
                                System.out.println("Topic cross-check changed classification from '" + 
                                                   initialTopic + "' to '" + topic + "'");
                                return topic;
                            }
                        }
                    }
                }
                
                // Check related concept mappings
                if (conceptRelationships.containsKey(concept)) {
                    List<String> relatedTopics = conceptRelationships.get(concept);
                    
                    // If our initial topic is not in the related topics, check if we should change
                    if (!relatedTopics.contains(initialTopic)) {
                        for (String relatedTopic : relatedTopics) {
                            // Build a prompt to determine the better topic
                            String verificationPrompt = String.format(
                                    "Based on this economics exam question:\n\n%s\n\n" +
                                    "Which topic is more appropriate: '%s' or '%s'? Respond with just the topic name.",
                                    questionText, initialTopic, relatedTopic);
                            
                            String verificationResponse = openAI.query(verificationPrompt).toLowerCase().trim();
                            
                            if (verificationResponse.contains(relatedTopic)) {
                                System.out.println("Topic cross-check changed classification from '" + 
                                                   initialTopic + "' to '" + relatedTopic + "'");
                                return relatedTopic;
                            }
                        }
                    }
                }
            }
            
            // If we get here, the initial topic was probably correct
            return initialTopic;
            
        } catch (Exception e) {
            System.err.println("Error in topic cross-check: " + e.getMessage());
            return initialTopic; // Return initial topic if cross-check fails
        }
    }
    
    /**
     * Remove phrases that could mislead topic identification
     */
    private String removeIgnorePhrases(String questionText) {
        if (questionText == null) {
            return "";
        }
        
        String cleanedText = questionText;
        for (String phrase : ignorePhrases) {
            // Create a pattern that handles case insensitivity and allows some variation
            Pattern pattern = Pattern.compile(phrase, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleanedText);
            
            // Replace with a neutral phrase that maintains readability
            cleanedText = matcher.replaceAll("for this question");
        }
        
        return cleanedText;
    }
    
    /**
     * Find a topic based on keywords in the question text with improved scoring
     */
    private String findTopicByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return null;
        }
        
        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();
        
        // Score each topic based on keyword matches with weighted scoring
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();
            
            double score = 0;
            for (String keyword : keywords) {
                String cleanKeyword = keyword.toLowerCase();
                // Exact match gets full score
                if (questionText.contains(" " + cleanKeyword + " ") || 
                    questionText.startsWith(cleanKeyword + " ") || 
                    questionText.endsWith(" " + cleanKeyword)) {
                    
                    // Weighting based on keyword specificity (longer keywords are more specific)
                    double weight = 0.5 + (Math.min(cleanKeyword.length(), 15) / 10.0); 
                    
                    // Give more weight to keywords that appear multiple times
                    int occurrences = countOccurrences(questionText, cleanKeyword);
                    score += weight * Math.min(occurrences, 3); // Cap at 3 to avoid over-counting
                    
                    // Bonus for keywords in important parts of the question
                    // Important parts are usually at the beginning or end of a question
                    if (questionText.length() > 100) {
                        String firstThird = questionText.substring(0, questionText.length() / 3);
                        String lastThird = questionText.substring(2 * questionText.length() / 3);
                        
                        if (firstThird.contains(cleanKeyword)) {
                            score += 0.5; // Bonus for appearing early in the question
                        }
                        if (lastThird.contains(cleanKeyword)) {
                            score += 1.0; // Larger bonus for appearing late (often the main task)
                        }
                    }
                }
            }
            
            // Direct mention of the topic itself gets a big bonus
            if (questionText.contains(topic.toLowerCase())) {
                score += 3.0;
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
     * Count occurrences of a keyword in a text
     */
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
    /**
     * Determine a fallback topic based on content analysis with improved matching
     */
    private String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "demand and supply"; // Default topic
        }
        
        questionText = questionText.toLowerCase();
        
        // Map of themes with their constituent topics and detection patterns
        Map<String, Map<String, List<String>>> thematicTopics = new HashMap<>();
        
        // Theme 1: Markets and Market Failure
        Map<String, List<String>> theme1 = new HashMap<>();
        theme1.put("demand and supply", Arrays.asList("demand", "supply", "equilibrium", "market", "price"));
        theme1.put("elasticity", Arrays.asList("elastic", "elasticity", "ped", "yed", "xed", "responsiveness"));
        theme1.put("market failure", Arrays.asList("failure", "inefficient", "welfare", "deadweight"));
        theme1.put("externalities", Arrays.asList("externality", "social cost", "social benefit", "third party"));
        theme1.put("government intervention", Arrays.asList("government", "policy", "tax", "subsidy", "regulation"));
        thematicTopics.put("markets", theme1);
        
        // Theme 2: Macroeconomics
        Map<String, List<String>> theme2 = new HashMap<>();
        theme2.put("economic growth", Arrays.asList("growth", "gdp", "output", "productivity"));
        theme2.put("inflation", Arrays.asList("inflation", "price level", "cpi", "deflation"));
        theme2.put("employment and unemployment", Arrays.asList("unemployment", "employment", "jobless", "labor market"));
        theme2.put("aggregate demand", Arrays.asList("aggregate demand", "ad", "consumption", "investment"));
        theme2.put("aggregate supply", Arrays.asList("aggregate supply", "as", "sras", "lras", "production"));
        theme2.put("fiscal policy", Arrays.asList("fiscal", "government spending", "taxation", "budget"));
        theme2.put("monetary policy", Arrays.asList("monetary", "interest rate", "money supply", "central bank"));
        thematicTopics.put("macro", theme2);
        
        // Theme 3: Business and Markets
        Map<String, List<String>> theme3 = new HashMap<>();
        theme3.put("market structures", Arrays.asList("market structure", "competition", "competitive"));
        theme3.put("perfect competition", Arrays.asList("perfect competition", "price taker", "homogeneous"));
        theme3.put("monopoly", Arrays.asList("monopoly", "price maker", "barriers to entry", "single seller"));
        theme3.put("oligopoly", Arrays.asList("oligopoly", "interdependence", "few sellers", "collusion"));
        theme3.put("costs", Arrays.asList("cost", "fixed", "variable", "total cost", "average cost", "marginal cost"));
        theme3.put("revenue", Arrays.asList("revenue", "total revenue", "average revenue", "marginal revenue"));
        theme3.put("profit", Arrays.asList("profit", "loss", "maximization", "normal profit", "supernormal profit"));
        thematicTopics.put("business", theme3);
        
        // Theme 4: International
        Map<String, List<String>> theme4 = new HashMap<>();
        theme4.put("international economics", Arrays.asList("international", "trade", "global", "foreign"));
        theme4.put("exchange rates", Arrays.asList("exchange rate", "currency", "appreciation", "depreciation"));
        theme4.put("balance of payments", Arrays.asList("balance of payments", "current account", "deficit", "surplus"));
        theme4.put("absolute and comparative advantage", Arrays.asList("advantage", "comparative", "absolute", "specialization"));
        thematicTopics.put("international", theme4);
        
        // First, try to identify the main theme of the question
        String mainTheme = null;
        int bestThemeScore = 0;
        
        for (Map.Entry<String, Map<String, List<String>>> themeEntry : thematicTopics.entrySet()) {
            String theme = themeEntry.getKey();
            int themeScore = 0;
            
            // Count pattern matches for all topics in this theme
            for (Map.Entry<String, List<String>> topicEntry : themeEntry.getValue().entrySet()) {
                for (String pattern : topicEntry.getValue()) {
                    if (questionText.contains(pattern)) {
                        themeScore++;
                    }
                }
            }
            
            if (themeScore > bestThemeScore) {
                bestThemeScore = themeScore;
                mainTheme = theme;
            }
        }
        
        // If we identified a theme, look for the best topic within that theme
        if (mainTheme != null && bestThemeScore > 0) {
            Map<String, List<String>> themeTopics = thematicTopics.get(mainTheme);
            String bestTopic = null;
            int bestTopicScore = 0;
            
            for (Map.Entry<String, List<String>> topicEntry : themeTopics.entrySet()) {
                String topic = topicEntry.getKey();
                int topicScore = 0;
                
                for (String pattern : topicEntry.getValue()) {
                    if (questionText.contains(pattern)) {
                        topicScore++;
                    }
                }
                
                if (topicScore > bestTopicScore) {
                    bestTopicScore = topicScore;
                    bestTopic = topic;
                }
            }
            
            if (bestTopic != null) {
                return bestTopic;
            }
        }
        
        // If theme-based approach fails, try the specific pattern matching approach
        
        // Market-related topics
        if (questionText.contains("demand") && questionText.contains("supply")) {
            return "demand and supply";
        }
        
        if (questionText.contains("elastic") || questionText.contains("ped") || 
            questionText.contains("responsiveness") || questionText.contains("percentage change")) {
            return "elasticity";
        }
        
        if ((questionText.contains("monopoly") || questionText.contains("monopolist")) && 
            !questionText.contains("oligopoly") && !questionText.contains("competition")) {
            return "monopoly";
        }
        
        if (questionText.contains("oligopoly") || questionText.contains("collusion") || 
            questionText.contains("cartel") || questionText.contains("interdependence")) {
            return "oligopoly";
        }
        
        if (questionText.contains("perfect competition") || 
            (questionText.contains("perfect") && questionText.contains("competition"))) {
            return "perfect competition";
        }
        
        // Macroeconomic topics
        if (questionText.contains("inflation") || questionText.contains("price level") || 
            questionText.contains("cpi") || questionText.contains("rpi")) {
            return "inflation";
        }
        
        if (questionText.contains("fiscal policy") || 
            (questionText.contains("fiscal") && questionText.contains("policy")) ||
            (questionText.contains("government") && questionText.contains("spending") && questionText.contains("taxation"))) {
            return "fiscal policy";
        }
        
        if (questionText.contains("monetary policy") || 
            (questionText.contains("monetary") && questionText.contains("policy")) ||
            (questionText.contains("interest rate") && questionText.contains("central bank"))) {
            return "monetary policy";
        }
        
        if (questionText.contains("unemployment") || 
            (questionText.contains("employment") && !questionText.contains("full employment"))) {
            return "employment and unemployment";
        }
        
        if (questionText.contains("economic growth") || 
            (questionText.contains("growth") && questionText.contains("gdp"))) {
            return "economic growth";
        }
        
        // Business topics
        if ((questionText.contains("cost") || questionText.contains("costs")) && 
            !questionText.contains("opportunity cost") && 
            !questionText.contains("social cost")) {
            return "costs";
        }
        
        if (questionText.contains("revenue") && !questionText.contains("marginal revenue product")) {
            return "revenue";
        }
        
        if (questionText.contains("profit") && !questionText.contains("non-profit")) {
            return "profit";
        }
        
        // International topics
        if (questionText.contains("exchange rate") || 
            (questionText.contains("exchange") && questionText.contains("rate"))) {
            return "exchange rates";
        }
        
        if (questionText.contains("comparative advantage") || 
            (questionText.contains("comparative") && questionText.contains("advantage"))) {
            return "absolute and comparative advantage";
        }
        
        if (questionText.contains("international trade") || 
            (questionText.contains("international") && questionText.contains("trade"))) {
            return "international economics";
        }
        
        // If no specific pattern matches, return a common topic as default
        return "demand and supply";
    }
    
    /**
     * Validate and clean a topic response with improved matching
     */
    private String validateTopic(String topicResponse) {
        if (topicResponse == null) {
            return "demand and supply"; // Default topic
        }
        
        // Clean up the response
        String cleanedTopic = topicResponse.trim()
                .replaceAll("[\"'.]", "")
                .replaceAll("(?i)Topic:?\\s*", "")
                .replaceAll("(?i)The topic is:?\\s*", "")
                .replaceAll("(?i)The main topic is:?\\s*", "")
                .replaceAll("(?i)The primary topic is:?\\s*", "")
                .toLowerCase();
        
        // Check if the topic is in our list
        for (String validTopic : topics) {
            if (cleanedTopic.equals(validTopic)) {
                return validTopic;
            }
        }
        
        // Try to find partial matches - more thorough matching
        for (String validTopic : topics) {
            // Check if the valid topic contains the suggested topic
            if (validTopic.contains(cleanedTopic)) {
                return validTopic;
            }
            
            // Check if the suggested topic contains the valid topic 
            // but only if the valid topic is substantial (not just a few letters)
            if (validTopic.length() > 4 && cleanedTopic.contains(validTopic)) {
                return validTopic;
            }
            
            // Check for component words match (e.g. "supply demand" should match "demand and supply")
            String[] validParts = validTopic.split("\\s+");
            String[] suggestedParts = cleanedTopic.split("\\s+");
            
            if (validParts.length > 1 && suggestedParts.length > 0) {
                boolean allPartsFound = true;
                for (String part : validParts) {
                    if (part.length() <= 2) continue; // Skip short words like "and", "of", etc.
                    
                    boolean partFound = false;
                    for (String suggestedPart : suggestedParts) {
                        if (suggestedPart.contains(part) || part.contains(suggestedPart)) {
                            partFound = true;
                            break;
                        }
                    }
                    
                    if (!partFound) {
                        allPartsFound = false;
                        break;
                    }
                }
                
                if (allPartsFound) {
                    return validTopic;
                }
            }
        }
        
        // If we still don't have a match, use improved similarity matching
        return findSimilarTopic(cleanedTopic);
    }
    
    /**
     * Find the most similar topic using a combination of similarity metrics
     */
    private String findSimilarTopic(String suggestedTopic) {
        double bestSimilarity = 0.0;
        String bestMatch = "demand and supply"; // Default fallback
        
        // Check for direct word matches first
        Set<String> suggestedWords = new HashSet<>(Arrays.asList(suggestedTopic.split("\\s+")));
        
        for (String validTopic : topics) {
            // Calculate Jaccard similarity
            double jaccardSim = calculateJaccardSimilarity(suggestedTopic, validTopic);
            
            // Calculate word overlap
            Set<String> topicWords = new HashSet<>(Arrays.asList(validTopic.split("\\s+")));
            int commonWords = 0;
            for (String word : suggestedWords) {
                if (word.length() <= 2) continue; // Skip short words
                if (topicWords.contains(word)) {
                    commonWords++;
                }
            }
            
            // Calculate normalized word overlap
            double wordOverlap = suggestedWords.size() > 0 ? 
                    (double) commonWords / Math.max(suggestedWords.size(), topicWords.size()) : 0;
            
            // Calculate substring similarity
            double substringScore = 0;
            if (validTopic.contains(suggestedTopic)) {
                substringScore = (double) suggestedTopic.length() / validTopic.length();
            } else if (suggestedTopic.contains(validTopic)) {
                substringScore = (double) validTopic.length() / suggestedTopic.length();
            }
            
            // Combined similarity score (weighted)
            double combinedScore = (jaccardSim * 0.4) + (wordOverlap * 0.4) + (substringScore * 0.2);
            
            if (combinedScore > bestSimilarity) {
                bestSimilarity = combinedScore;
                bestMatch = validTopic;
            }
        }
        
        // If we don't have a good match, try keyword-based approach
        if (bestSimilarity < 0.15) {
            // Try to find matches in topic keywords
            for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (suggestedTopic.contains(keyword) && keyword.length() > 3) {
                        return entry.getKey();
                    }
                }
            }
            
            // Check conceptual relationships
            for (Map.Entry<String, List<String>> entry : conceptRelationships.entrySet()) {
                if (suggestedTopic.contains(entry.getKey()) && entry.getKey().length() > 3) {
                    // Return the first related topic as a fallback
                    if (!entry.getValue().isEmpty()) {
                        return entry.getValue().get(0);
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
                
                // If we have a heavily skewed distribution, re-examine those questions
                if (percentage > 40) {
                    reexamineSuspiciousTopicAssignments(questions, entry.getKey());
                }
            }
        }
    }
    
    /**
     * Re-examine suspicious topic assignments that might be incorrect
     */
    private void reexamineSuspiciousTopicAssignments(List<Question> questions, String suspiciousTopic) {
        System.out.println("Re-examining questions assigned to topic: " + suspiciousTopic);
        
        // Get all questions with this topic
        List<Question> topicQuestions = questions.stream()
                .filter(q -> q.getTopics() != null && q.getTopics().length > 0 && 
                       suspiciousTopic.equals(q.getTopics()[0]))
                .collect(Collectors.toList());
        
        // Use a single API call to get better classification for all these questions
        try {
            StringBuilder reconsiderPrompt = new StringBuilder();
            reconsiderPrompt.append("I need to verify the topic classification of these economics exam questions. ");
            reconsiderPrompt.append("They were all initially classified as '").append(suspiciousTopic).append("', but this seems suspicious.\n\n");
            reconsiderPrompt.append("For each question, determine if '").append(suspiciousTopic).append("' is truly the most appropriate topic ");
            reconsiderPrompt.append("or if another topic from this list would be more appropriate:\n");
            reconsiderPrompt.append(String.join(", ", topics)).append("\n\n");
            
            for (int i = 0; i < topicQuestions.size(); i++) {
                Question question = topicQuestions.get(i);
                reconsiderPrompt.append("QUESTION ").append(i + 1).append(":\n");
                reconsiderPrompt.append(question.getQuestionText()).append("\n\n");
            }
            
            reconsiderPrompt.append("For each question, respond in this format:\n");
            reconsiderPrompt.append("Question 1: [topic] - [brief reason]\n");
            reconsiderPrompt.append("Question 2: [topic] - [brief reason]\n");
            reconsiderPrompt.append("...\n\n");
            reconsiderPrompt.append("IMPORTANT: Choose the most specific and accurate topic for each question, ignoring phrases like 'using the data from the extract'.");
            
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.3);
            String response = openAI.query(reconsiderPrompt.toString());
            
            // Extract topic recommendations
            Map<Integer, String> revisedTopics = new HashMap<>();
            Pattern pattern = Pattern.compile("Question (\\d+):\\s*([\\w\\s]+?)(?:\\s*-|$)");
            Matcher matcher = pattern.matcher(response);
            
            while (matcher.find()) {
                try {
                    int questionNum = Integer.parseInt(matcher.group(1));
                    String topic = matcher.group(2).trim().toLowerCase();
                    
                    if (questionNum > 0 && questionNum <= topicQuestions.size()) {
                        revisedTopics.put(questionNum, topic);
                    }
                } catch (NumberFormatException e) {
                    // Skip this match
                }
            }
            
            // Apply revisions
            int changedCount = 0;
            for (int i = 0; i < topicQuestions.size(); i++) {
                Question question = topicQuestions.get(i);
                
                if (revisedTopics.containsKey(i + 1)) {
                    String newTopic = validateTopic(revisedTopics.get(i + 1));
                    
                    // Only update if the topic has changed
                    if (!newTopic.equals(suspiciousTopic)) {
                        System.out.println("Changed topic for question " + question.getQuestionNumber() + 
                                           " from '" + suspiciousTopic + "' to '" + newTopic + "'");
                        question.setTopics(new String[] { newTopic });
                        changedCount++;
                    }
                }
            }
            
            System.out.println("Re-examination complete. Changed " + changedCount + " out of " + topicQuestions.size() + " topic assignments.");
            
        } catch (Exception e) {
            System.err.println("Error during topic re-examination: " + e.getMessage());
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