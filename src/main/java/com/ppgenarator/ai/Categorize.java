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
    private static final int MAX_TOPICS_PER_QUESTION = 3;
    private static final double KEYWORD_THRESHOLD = 2.0; // Increased threshold for stricter matching
    private static final double SECONDARY_TOPIC_THRESHOLD = 0.7; // Threshold for secondary topics relative to primary

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
            "financial markets", "economic development", "sustainability", "globalisation",
            "protectionism", "trade liberalization"
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
            "from the data shown",
            "with reference to extract");

    // Instance variables
    private final File outputFolder;
    private final String[] topics;
    private final boolean useDynamicTopics;

    // Tracking for quality control
    private final Map<String, Integer> topicDistribution = new HashMap<>();

    /**
     * Constructor with default topics
     * 
     * @param outputFolder The folder to save categorized question files
     */
    public Categorize(File outputFolder) {
        this(outputFolder, DEFAULT_TOPICS, null);
    }

    /**
     * Constructor with custom topics
     * 
     * @param outputFolder   The folder to save categorized question files
     * @param topics         Custom topics to use for categorization
     * @param optionalTopics Additional topics to consider (ignored in this
     *                       implementation)
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
        addKeywords("scarcity and choice", "scarcity", "choice", "opportunity cost", "economic problem",
                "limited resources", "unlimited wants", "resource allocation", "economics is about",
                "basic economic problem");
        addKeywords("production possibility frontiers", "ppf", "production possibility", "frontier",
                "transformation curve", "opportunity cost", "trade-off", "attainable", "unattainable",
                "efficient production", "productive efficiency");
        addKeywords("specialization and trade", "specialization", "specialisation", "comparative advantage",
                "absolute advantage", "division of labor", "trade", "trading", "specializing", "gains from trade",
                "mutually beneficial");
        addKeywords("demand and supply", "demand curve", "supply curve", "equilibrium", "market equilibrium",
                "price mechanism", "quantity demanded", "quantity supplied", "market forces", "shifts in demand",
                "shifts in supply");
        addKeywords("price determination", "price", "equilibrium price", "market clearing", "surplus", "shortage",
                "price floor", "price ceiling", "rationing", "allocating", "market price");
        addKeywords("price mechanism", "price signal", "signaling function", "rationing function", "incentive function",
                "allocation of resources", "invisible hand", "market mechanism", "resource allocation");
        addKeywords("consumer and producer surplus", "consumer surplus", "producer surplus", "total surplus",
                "economic welfare", "deadweight loss", "welfare loss", "welfare gain", "economic efficiency",
                "consumer benefit", "producer benefit");
        addKeywords("elasticity", "elastic", "inelastic", "price elasticity", "income elasticity", "cross elasticity",
                "ped", "yed", "xed", "elasticity coefficient", "elastic supply", "elastic demand", "unitary elasticity",
                "responsiveness");
        addKeywords("alternative market structures", "market structure", "competition", "monopoly", "oligopoly",
                "monopolistic competition", "perfect competition", "competitive markets", "concentration ratio",
                "market power", "barriers to entry");
        addKeywords("market failure", "market failure", "inefficient allocation", "socially optimal", "social optimum",
                "merit goods", "demerit goods", "private cost", "social cost", "inefficiency", "welfare loss",
                "market solution");
        addKeywords("externalities", "externality", "social cost", "social benefit", "negative externality",
                "positive externality", "spillover", "external costs", "external benefits", "third party", "pollution",
                "marginal external cost");
        addKeywords("public goods", "public good", "free-rider", "non-rival", "non-excludable", "market provision",
                "collective provision", "private provision", "non-rivalrous", "non-excludability",
                "government provision", "lighthouse");
        addKeywords("information gaps", "information", "asymmetric information", "imperfect information",
                "adverse selection", "moral hazard", "incomplete information", "information failure",
                "hidden knowledge", "hidden action");
        addKeywords("government intervention", "government", "intervention", "regulation", "subsidy", "tax",
                "price control", "quota", "buffer stock", "legislation", "state provision", "government failure",
                "policy");

        // Theme 2
        addKeywords("economic growth", "economic growth", "gdp", "real gdp", "long run", "actual growth",
                "potential growth", "business cycle", "trend growth", "gdp per capita", "living standards",
                "sustainable growth", "productivity");
        addKeywords("inflation", "inflation", "cpi", "rpi", "price level", "deflation", "hyperinflation", "cost-push",
                "demand-pull", "wage inflation", "stagflation", "disinflation", "price stability", "indexation");
        addKeywords("employment and unemployment", "unemployment", "employment", "jobless", "labor force",
                "participation rate", "natural rate", "frictional unemployment", "structural unemployment",
                "cyclical unemployment", "full employment");
        addKeywords("balance of payments", "balance of payments", "current account", "financial account",
                "capital account", "deficit", "surplus", "trade deficit", "trade surplus", "import", "export",
                "net exports", "invisible trade");
        addKeywords("circular flow of income", "circular flow", "leakages", "injections", "withdrawals",
                "national income", "income flow", "spending flow", "circular flow diagram", "savings", "investment",
                "taxation", "government spending");
        addKeywords("aggregate demand", "aggregate demand", "ad", "consumption", "investment", "government spending",
                "net exports", "ad curve", "shifts in ad", "components of ad", "wealth effect", "interest rate effect",
                "multiplier effect");
        addKeywords("aggregate supply", "aggregate supply", "as", "sras", "lras", "production", "potential output",
                "aggregate supply curve", "productive capacity", "shifts in as", "output gap", "supply shock",
                "productive potential");
        addKeywords("national income", "national income", "gdp", "gnp", "nominal", "real", "output", "value added",
                "final goods", "intermediate goods", "national output", "income method", "expenditure method",
                "production method");
        addKeywords("economic cycle", "economic cycle", "business cycle", "boom", "recession", "slump", "recovery",
                "peak", "trough", "expansion", "contraction", "upturn", "downturn", "trade cycle", "fluctuations");
        addKeywords("monetary policy", "monetary policy", "interest rate", "money supply", "central bank",
                "quantitative easing", "bank rate", "discount rate", "liquidity", "monetary transmission",
                "inflation targeting", "repo rate");
        addKeywords("fiscal policy", "fiscal policy", "government spending", "taxation", "budget", "deficit", "surplus",
                "public finances", "fiscal stimulus", "austerity", "automatic stabilizers", "discretionary policy",
                "fiscal stance", "macroeconomic policy");
        addKeywords("supply-side policies", "supply-side", "productivity", "competitiveness", "deregulation",
                "privatization", "tax incentives", "labor market", "skills", "training", "infrastructure", "incentives",
                "enterprise");

        // Theme 3
        addKeywords("business growth", "business growth", "merger", "acquisition", "organic growth", "integration",
                "horizontal integration", "vertical integration", "conglomerate", "takeover", "economies of scale",
                "diversification");
        addKeywords("business objectives", "business objective", "profit maximization", "revenue maximization", "sales",
                "market share", "growth", "survival", "shareholder value", "stakeholder interests", "satisficing",
                "social responsibility");
        addKeywords("revenue", "revenue", "total revenue", "average revenue", "marginal revenue", "sales", "income",
                "turnover", "demand curve", "ar", "mr", "price elasticity", "pricing strategy");
        addKeywords("costs", "cost", "fixed cost", "variable cost", "total cost", "average cost", "marginal cost",
                "sunk cost", "opportunity cost", "accounting cost", "economic cost", "explicit cost", "implicit cost");
        addKeywords("economies of scale", "economies of scale", "diseconomies", "increasing returns",
                "long run average cost", "lrac", "minimum efficient scale", "internal economies", "external economies",
                "returns to scale");
        addKeywords("profit", "profit", "loss", "profit maximization", "normal profit", "supernormal profit",
                "economic profit", "accounting profit", "profitability", "profit margin", "profit motive",
                "return on capital");
        addKeywords("market structures", "market structure", "competition", "competitive", "concentration ratio",
                "herfindahl index", "barriers to entry", "market power", "market concentration", "competitive behavior",
                "strategic behavior");
        addKeywords("perfect competition", "perfect competition", "price taker", "homogeneous", "many firms",
                "free entry", "free exit", "perfect information", "normal profit", "allocative efficiency",
                "productive efficiency");
        addKeywords("monopolistic competition", "monopolistic competition", "product differentiation", "brand",
                "advertising", "unique selling point", "non-price competition", "brand loyalty", "entry", "exit",
                "short run profit", "long run");
        addKeywords("oligopoly", "oligopoly", "interdependence", "few sellers", "collusion", "cartel", "price war",
                "non-price competition", "game theory", "prisoners dilemma", "price leadership", "barriers to entry",
                "strategic behavior");
        addKeywords("monopoly", "monopoly", "price maker", "barriers to entry", "single seller", "price discrimination",
                "monopoly power", "market power", "deadweight loss", "inefficiency", "natural monopoly",
                "legal monopoly");
        addKeywords("price discrimination", "price discrimination", "first degree", "second degree", "third degree",
                "price targeting", "market segmentation", "price differentiation", "consumer surplus",
                "price elasticity", "market power");
        addKeywords("contestable markets", "contestable market", "barrier to entry", "barrier to exit", "sunk cost",
                "hit and run", "entry threat", "potential competition", "perfectly contestable", "entry deterrence",
                "limit pricing");
        addKeywords("labor market", "labor market", "labour market", "wage", "employment", "monopsony",
                "supply of labor", "demand for labor", "derived demand", "marginal revenue product",
                "marginal cost of labor", "wage rate");
        addKeywords("wage determination", "wage", "determination", "supply of labor", "demand for labor",
                "equilibrium wage", "minimum wage", "collective bargaining", "trade unions", "wage differentials",
                "labor productivity");
        addKeywords("labor market failure", "labor market failure", "minimum wage", "discrimination", "immobility",
                "geographic immobility", "occupational immobility", "information asymmetry", "monopsony",
                "exploitation", "wage inequality");

        // Theme 4
        addKeywords("international economics", "international", "global", "trade", "foreign", "world economy",
                "globalization", "globalisation", "international trade", "capital flows", "migration", "transnational",
                "economic integration");
        addKeywords("absolute and comparative advantage", "absolute advantage", "comparative advantage",
                "opportunity cost", "specialization", "trade gains", "ricardian model", "production possibility",
                "relative efficiency", "terms of trade");
        addKeywords("terms of trade", "terms of trade", "exchange ratio", "export prices", "import prices",
                "favorable terms", "unfavorable terms", "commodity terms", "income terms", "single factorial terms",
                "double factorial terms");
        addKeywords("trading blocs", "trading bloc", "regional", "free trade", "eu", "nafta", "asean", "customs union",
                "common market", "economic union", "free trade area", "preferential trade", "trade creation",
                "trade diversion");
        addKeywords("world trade organization", "wto", "world trade organization", "trade liberalization", "gatt",
                "uruguay round", "doha round", "trade dispute", "most favored nation", "national treatment",
                "protectionism");
        addKeywords("exchange rates", "exchange rate", "currency", "appreciation", "depreciation", "devaluation",
                "revaluation", "floating", "fixed", "managed float", "purchasing power parity", "currency market",
                "forex");
        addKeywords("international competitiveness", "competitiveness", "international", "productivity",
                "unit labor cost", "export performance", "import penetration", "comparative advantage", "exchange rate",
                "non-price factors");
        addKeywords("poverty and inequality", "poverty", "inequality", "income distribution", "wealth distribution",
                "gini coefficient", "lorenz curve", "relative poverty", "absolute poverty", "poverty line",
                "redistribution", "social mobility");
        addKeywords("developing economies", "developing", "development", "third world", "less developed", "emerging",
                "developing countries", "newly industrialized", "underdeveloped", "global south",
                "economic development", "industrialization");
        addKeywords("financial markets", "financial market", "stock market", "bond market", "forex", "securities",
                "capital market", "money market", "primary market", "secondary market", "financial intermediation",
                "speculation");
        addKeywords("economic development", "development", "growth", "hdl", "standard of living", "quality of life",
                "human development", "sustainable development", "millennium goals", "development gap",
                "development strategies");
        addKeywords("sustainability", "sustainable", "sustainability", "environment", "green", "renewable",
                "future generations", "sustainable development", "climate change", "carbon emissions",
                "resource depletion", "circular economy");
        addKeywords("globalisation", "globalisation", "globalization", "global", "multinational", "tnc",
                "global economy", "global trade", "global integration", "global market", "global supply chain",
                "global competition", "negative effects of globalisation", "globalisation effects");
        addKeywords("protectionism", "protectionism", "tariff", "quota", "trade barrier", "import restriction",
                "trade protection", "domestic industry protection", "trade war", "anti-dumping", "safeguard measures");
        addKeywords("trade liberalization", "trade liberalization", "free trade", "trade liberalisation",
                "removal of barriers", "trade openness", "trade reform", "trade deregulation");
    }

    /**
     * Initialize concept relationships to understand broader economic relationships
     */
    private void initializeConceptRelationships() {
        // Map broader economic concepts to relevant topics

        // Market dynamics
        addRelatedConcepts("market", "demand and supply", "price determination", "price mechanism",
                "market structures");

        // Price-related concepts
        addRelatedConcepts("price", "demand and supply", "price determination", "price mechanism", "elasticity",
                "price discrimination");

        // Government-related concepts
        addRelatedConcepts("government", "government intervention", "fiscal policy", "monetary policy",
                "supply-side policies", "public goods");

        // International concepts
        addRelatedConcepts("international", "international economics", "absolute and comparative advantage",
                "balance of payments", "exchange rates", "trading blocs", "globalisation");

        // Growth and development
        addRelatedConcepts("growth", "economic growth", "business growth", "economic development",
                "developing economies");

        // Cost and revenue concepts
        addRelatedConcepts("cost", "costs", "profit", "revenue", "economies of scale", "externalities");

        // Market structure concepts
        addRelatedConcepts("competition", "market structures", "perfect competition", "monopolistic competition",
                "oligopoly", "monopoly", "contestable markets");

        // Labor market concepts
        addRelatedConcepts("labor", "labor market", "wage determination", "labor market failure",
                "employment and unemployment");

        // Macro policies
        addRelatedConcepts("policy", "fiscal policy", "monetary policy", "supply-side policies",
                "government intervention");
        addRelatedConcepts("macroeconomic", "fiscal policy", "monetary policy", "supply-side policies",
                "aggregate demand", "aggregate supply");

        // Social issues
        addRelatedConcepts("inequality", "poverty and inequality", "economic development", "sustainability");

        // Globalisation concepts
        addRelatedConcepts("globalisation", "international economics", "protectionism", "trade liberalization",
                "poverty and inequality", "developing economies");
        addRelatedConcepts("global", "globalisation", "international economics", "multinational");
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
     * 
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
     * 
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
     * 
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
     * 
     * @param question The question to load text for
     */
    private void loadQuestionText(Question question) {
        // Skip if question text is already loaded
        if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
            return;
        }

        System.out.println(
                "Loading question content for: " + question.getQuestionNumber() + " from year " + question.getYear());

        if (question.getQuestion() != null) {
            String questionContent = extractTextFromPDF(question.getQuestion());
            questionContent = cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);
        }
    }

    /**
     * Identify topics for a batch of questions with strict topic limitation
     * 
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
            // Enhanced batch prompt for strict topic allocation
            StringBuilder batchPrompt = new StringBuilder();
            batchPrompt.append(
                    "You are an expert A-level Economics examiner who specializes in categorizing exam questions based on their CORE economic concepts.\n\n");
            batchPrompt.append("I'll provide you with ").append(questionsNeedingTopics.size())
                    .append(" economics exam questions.\n");
            batchPrompt.append(
                    "For each question, determine the PRIMARY economic concept being tested, and ONLY add secondary topics if they are absolutely essential.\n\n");

            batchPrompt.append("STRICT INSTRUCTIONS:\n");
            batchPrompt.append(
                    "1. IGNORE phrases like 'using the data from the extract' or 'refer to the figure' - these are just exam instructions\n");
            batchPrompt.append(
                    "2. Focus on the MAIN economic concept being tested - what is the question primarily about?\n");
            batchPrompt.append("3. Only add a second topic if the question equally tests TWO distinct concepts\n");
            batchPrompt.append("4. MAXIMUM 3 topics per question, but aim for 1-2 topics in most cases\n");
            batchPrompt
                    .append("5. Be VERY selective - only include topics that are central to answering the question\n");
            batchPrompt.append("6. Select topics ONLY from this list:\n");
            batchPrompt.append(String.join(", ", topics)).append("\n\n");

            batchPrompt.append("EXAMPLES:\n");
            batchPrompt.append(
                    "- Question about macroeconomic policies to reduce negative effects of globalisation → Topics: globalisation, fiscal policy (TWO topics because it's about policies addressing globalisation)\n");
            batchPrompt.append(
                    "- Question about calculating price elasticity of demand → Topics: elasticity (ONE topic - it's purely about elasticity)\n");
            batchPrompt.append(
                    "- Question about monopoly profit maximization → Topics: monopoly (ONE topic - profit is part of monopoly theory)\n");
            batchPrompt.append(
                    "- Question about government intervention to correct market failure → Topics: market failure, government intervention (TWO topics because both are central)\n\n");

            for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                Question question = questionsNeedingTopics.get(i);
                batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
                batchPrompt.append(question.getQuestionText()).append("\n\n");
            }

            batchPrompt.append(
                    "For each question, respond with the question number and the most relevant topics (1-3 maximum, prefer fewer).\n");
            batchPrompt.append(
                    "Format: 'Question 1: topic1' OR 'Question 1: topic1, topic2' (only if both are equally important)\n");
            batchPrompt.append("CRITICAL: Be strict and selective. Most questions should have only 1-2 topics.");

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1); // Very low temperature for consistency
            String response = openAI.query(batchPrompt.toString());

            System.out.println("AI Response for batch:\n" + response + "\n");

            // Process the response to extract topics per question
            Map<Integer, String[]> topicAssignments = parseMultipleTopicAssignments(response,
                    questionsNeedingTopics.size());

            // If we couldn't parse any topics, try individual processing
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
                    String[] validatedTopics = validateAndLimitTopics(assignedTopics,
                            removeIgnorePhrases(question.getQuestionText()));

                    question.setTopics(validatedTopics);
                    System.out.println("Assigned topics " + Arrays.toString(validatedTopics) +
                            " to question " + question.getQuestionNumber());
                } else {
                    // If we couldn't get topics from batch processing, try individual processing
                    identifyTopicsForSingleQuestion(question);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in batch processing: " + e.getMessage());
            e.printStackTrace();

            // Fallback to individual processing
            for (Question question : questionsNeedingTopics) {
                identifyTopicsForSingleQuestion(question);
            }
        }
    }

    /**
     * Parse multiple topic assignments from the AI response
     */
    private Map<Integer, String[]> parseMultipleTopicAssignments(String response, int expectedCount) {
        Map<Integer, String[]> results = new HashMap<>();

        // Pattern to match "Question X: topic1, topic2, topic3"
        Pattern pattern = Pattern.compile("(?:Question|QUESTION)\\s+(\\d+)\\s*:\\s*([^\\n\\r]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            try {
                int questionNum = Integer.parseInt(matcher.group(1));
                String topicsString = matcher.group(2).trim();

                System.out.println("Parsing - Question " + questionNum + ": " + topicsString);

                if (questionNum > 0 && questionNum <= expectedCount) {
                    // Split topics by comma and clean them
                    String[] topics = topicsString.split("\\s*[,;]\\s*|\\s+and\\s+");
                    List<String> cleanedTopics = new ArrayList<>();

                    for (String topic : topics) {
                        String cleanedTopic = topic.toLowerCase().trim()
                                .replaceAll("[\"'.-]", "")
                                .replaceAll("(?i)^(the\\s+)?", ""); // Remove "the" at the beginning

                        if (!cleanedTopic.isEmpty() && cleanedTopic.length() > 2) {
                            cleanedTopics.add(cleanedTopic);
                        }
                    }

                    if (!cleanedTopics.isEmpty()) {
                        // Limit to maximum topics
                        if (cleanedTopics.size() > MAX_TOPICS_PER_QUESTION) {
                            cleanedTopics = cleanedTopics.subList(0, MAX_TOPICS_PER_QUESTION);
                        }
                        results.put(questionNum, cleanedTopics.toArray(new String[0]));
                        System.out.println(
                                "Successfully parsed " + cleanedTopics.size() + " topics for question " + questionNum);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse question number from: " + matcher.group(1));
            }
        }

        System.out.println("Total questions parsed: " + results.size() + " out of " + expectedCount);
        return results;
    }

    /**
     * Identify topics for a single question with strict limitation
     * 
     * @param question The question to identify topics for
     */
    private void identifyTopicsForSingleQuestion(Question question) {
        System.out.println("Processing individual question: " + question.getQuestionNumber());

        // First, clean the question text by removing misleading phrases
        String cleanedText = removeIgnorePhrases(question.getQuestionText());

        // Try AI-based approach with strict prompting
        try {
            String prompt = String.format(
                    "You are an expert A-level Economics examiner. Analyze this economics exam question to identify the PRIMARY economic concept being tested.\n\n"
                            +
                            "QUESTION:\n%s\n\n" +
                            "INSTRUCTIONS:\n" +
                            "1. IGNORE contextual elements like 'refer to the extract' or 'using the data' - focus on the CORE economic concept\n"
                            +
                            "2. What is the MAIN economic knowledge area a student needs to answer this question?\n" +
                            "3. Only include secondary topics if they are absolutely essential to answering the question\n"
                            +
                            "4. Maximum 3 topics, but prefer 1-2 topics\n" +
                            "5. Select ONLY from this list:\n%s\n\n" +
                            "Examples:\n" +
                            "- Question asking to calculate PED → Topic: elasticity\n" +
                            "- Question about policies to address globalisation → Topics: globalisation, fiscal policy\n"
                            +
                            "- Question about monopoly pricing → Topic: monopoly\n" +
                            "- Question about market failure and government response → Topics: market failure, government intervention\n\n"
                            +
                            "Respond with the most relevant topic(s), separated by commas if multiple. Be strict and selective.",
                    cleanedText, String.join(", ", topics));

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String response = openAI.query(prompt);

            System.out.println("Single question AI response: " + response);

            // Parse topics from response
            String[] suggestedTopics = parseTopicsFromResponse(response);
            String[] validatedTopics = validateAndLimitTopics(suggestedTopics, cleanedText);

            question.setTopics(validatedTopics);
            System.out.println("Assigned topics: " + Arrays.toString(validatedTopics));

        } catch (Exception e) {
            System.err.println(
                    "Error identifying topics for question " + question.getQuestionNumber() + ": " + e.getMessage());

            // Try keyword-based approach as backup with strict scoring
            String[] keywordTopics = findStrictTopicsByKeywords(cleanedText);
            if (keywordTopics.length > 0) {
                question.setTopics(keywordTopics);
                System.out.println("Assigned topics by keywords: " + Arrays.toString(keywordTopics));
                return;
            }

            // Last resort - assign based on question content patterns
            String fallbackTopic = determineFallbackTopic(cleanedText);
            question.setTopics(new String[] { fallbackTopic });
            System.out.println("Assigned fallback topic: " + fallbackTopic);
        }
    }

    /**
     * Parse topics from AI response that may contain multiple topics
     */
    private String[] parseTopicsFromResponse(String response) {
        if (response == null) {
            return new String[0];
        }

        // Clean up the response
        String cleanedResponse = response.trim()
                .replaceAll("[\"'.]", "")
                .replaceAll("(?i)Topics?:?\\s*", "")
                .replaceAll("(?i)The topics? (?:are?|is):?\\s*", "")
                .replaceAll("(?i)The relevant topics? (?:are?|is):?\\s*", "")
                .replaceAll("(?i)Primary topics?:?\\s*", "");

        // Split by common separators
        String[] topics = cleanedResponse.split("\\s*[,;]\\s*|\\s+and\\s+");

        List<String> cleanedTopics = new ArrayList<>();

        // Clean each topic
        for (String topic : topics) {
            String cleanedTopic = topic.toLowerCase().trim()
                    .replaceAll("(?i)^(the\\s+)?", ""); // Remove "the" at the beginning

            if (!cleanedTopic.isEmpty() && cleanedTopic.length() > 2) {
                cleanedTopics.add(cleanedTopic);
            }
        }

        return cleanedTopics.toArray(new String[0]);
    }

    /**
     * Validate and limit topics with strict criteria
     */
    private String[] validateAndLimitTopics(String[] suggestedTopics, String questionText) {
        if (suggestedTopics == null) {
            return new String[] { determineFallbackTopic(questionText) };
        }

        // First validate all topics
        List<String> validTopics = new ArrayList<>();
        for (String suggestedTopic : suggestedTopics) {
            if (suggestedTopic == null || suggestedTopic.trim().isEmpty()) {
                continue;
            }

            String validatedTopic = validateSingleTopic(suggestedTopic);
            if (validatedTopic != null && !validatedTopic.isEmpty()) {
                validTopics.add(validatedTopic);
                System.out.println("Validated topic: '" + suggestedTopic + "' → '" + validatedTopic + "'");
            }
        }

        // If no valid topics, return fallback
        if (validTopics.isEmpty()) {
            return new String[] { determineFallbackTopic(questionText) };
        }

        // Score topics based on relevance to question
        Map<String, Double> topicRelevanceScores = scoreTopicRelevance(validTopics, questionText);

        // Sort by relevance and limit to top topics
        List<String> finalTopics = topicRelevanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_TOPICS_PER_QUESTION)
                .filter(entry -> entry.getValue() > 0.5) // Only include topics with decent relevance
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Ensure we have at least one topic
        if (finalTopics.isEmpty()) {
            return new String[] { validTopics.get(0) }; // Return the first valid topic
        }

        return finalTopics.toArray(new String[0]);
    }

    /**
     * Score topics based on their relevance to the question text
     */
    private Map<String, Double> scoreTopicRelevance(List<String> topics, String questionText) {
        Map<String, Double> scores = new HashMap<>();
        String lowerQuestionText = questionText.toLowerCase();

        for (String topic : topics) {
            double score = 0.0;

            // Direct mention of topic gets high score
            if (lowerQuestionText.contains(topic.toLowerCase())) {
                score += 5.0;
            }

            // Check for keyword relevance
            if (topicKeywords.containsKey(topic)) {
                List<String> keywords = topicKeywords.get(topic);
                int matchingKeywords = 0;
                for (String keyword : keywords) {
                    if (lowerQuestionText.contains(keyword.toLowerCase())) {
                        matchingKeywords++;
                        // More specific keywords get higher weight
                        score += (keyword.length() > 6) ? 1.5 : 1.0;
                    }
                }

                // Bonus for multiple keyword matches
                if (matchingKeywords > 2) {
                    score += 1.0;
                }
            }

            scores.put(topic, score);
        }

        return scores;
    }

    /**
     * Validate a single topic with improved matching
     */
    private String validateSingleTopic(String topicResponse) {
        if (topicResponse == null) {
            return null;
        }

        // Clean up the response
        String cleanedTopic = topicResponse.trim()
                .replaceAll("[\"'.-]", "")
                .replaceAll("(?i)Topic:?\\s*", "")
                .replaceAll("(?i)The topic is:?\\s*", "")
                .replaceAll("(?i)The main topic is:?\\s*", "")
                .replaceAll("(?i)The primary topic is:?\\s*", "")
                .toLowerCase();

        // Check if the topic is in our list (exact match)
        for (String validTopic : topics) {
            if (cleanedTopic.equals(validTopic)) {
                return validTopic;
            }
        }

        // Try to find partial matches
        for (String validTopic : topics) {
            // Check if the valid topic contains the suggested topic
            if (validTopic.contains(cleanedTopic) && cleanedTopic.length() > 4) {
                return validTopic;
            }

            // Check if the suggested topic contains the valid topic
            if (validTopic.length() > 4 && cleanedTopic.contains(validTopic)) {
                return validTopic;
            }

            // Check for word-by-word match
            String[] validParts = validTopic.split("\\s+");
            String[] suggestedParts = cleanedTopic.split("\\s+");

            if (validParts.length > 1 && suggestedParts.length > 0) {
                int matchingWords = 0;
                for (String validPart : validParts) {
                    if (validPart.length() <= 2)
                        continue; // Skip short words

                    for (String suggestedPart : suggestedParts) {
                        if (validPart.equals(suggestedPart)) {
                            matchingWords++;
                            break;
                        }
                    }
                }

                // If most words match, consider it a match
                if (matchingWords >= Math.max(1, validParts.length - 1)) {
                    return validTopic;
                }
            }
        }

        return null; // No match found
    }

    /**
     * Find topics based on keywords with strict scoring
     */
    private String[] findStrictTopicsByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new String[0];
        }

        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();

        // Score each topic based on keyword matches with stricter criteria
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = 0;
            int significantMatches = 0;

            for (String keyword : keywords) {
                String cleanKeyword = keyword.toLowerCase();
                if (questionText.contains(" " + cleanKeyword + " ") ||
                        questionText.startsWith(cleanKeyword + " ") ||
                        questionText.endsWith(" " + cleanKeyword) ||
                        questionText.contains(" " + cleanKeyword + ",") ||
                        questionText.contains(" " + cleanKeyword + ".")) {

                    double weight = 1.0 + (Math.min(cleanKeyword.length(), 15) / 10.0);
                    score += weight;

                    // Count significant matches (longer keywords)
                    if (cleanKeyword.length() > 5) {
                        significantMatches++;
                    }
                }
            }

            // Direct mention bonus
            if (questionText.contains(topic.toLowerCase())) {
                score += 5.0;
                significantMatches += 2;
            }

            // Only include topics with strong evidence
            if (score >= KEYWORD_THRESHOLD && significantMatches > 0) {
                topicScores.put(topic, score);
            }
        }

        // Return only the top scoring topics
        List<String> matchingTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_TOPICS_PER_QUESTION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("Strict keyword matching found " + matchingTopics.size() + " topics: " + matchingTopics);

        return matchingTopics.toArray(new String[0]);
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
     * Determine a fallback topic based on content analysis
     */
    private String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "demand and supply"; // Default topic
        }

        questionText = questionText.toLowerCase();

        // Check for globalisation-related terms
        if (questionText.contains("globalisation") || questionText.contains("globalization") ||
                questionText.contains("negative effects of globalisation")) {
            return "globalisation";
        }

        // Check for fiscal policy terms
        if (questionText.contains("fiscal policy") ||
                (questionText.contains("government") && questionText.contains("spending")) ||
                (questionText.contains("taxation") && questionText.contains("policy"))) {
            return "fiscal policy";
        }

        // Check for monetary policy terms
        if (questionText.contains("monetary policy") ||
                questionText.contains("interest rate") || questionText.contains("central bank")) {
            return "monetary policy";
        }

        // Check for market failure
        if (questionText.contains("market") && questionText.contains("failure")) {
            return "market failure";
        }

        // Check for externalities
        if (questionText.contains("externality") || questionText.contains("externalities")) {
            return "externalities";
        }

        // If no specific pattern matches, return a common topic as default
        return "demand and supply";
    }

    /**
     * Clean question text by removing unnecessary content
     * 
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
     * 
     * @param questions  The questions to export
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

            System.out.println(
                    "Successfully exported " + questions.size() + " questions to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing questions to JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract text content from a PDF file
     * 
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