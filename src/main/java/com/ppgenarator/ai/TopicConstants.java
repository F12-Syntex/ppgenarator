package com.ppgenarator.ai;

public class TopicConstants {
    // Constants
    public static final int BATCH_SIZE = 3;
    public static final int MAX_TOPICS_PER_QUESTION = 3;
    public static final double KEYWORD_THRESHOLD = 2.0;
    public static final double SECONDARY_TOPIC_THRESHOLD = 0.7;

    // A-level Edexcel Economics topics
    public static final String[] DEFAULT_TOPICS = {
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

    // Phrases that should be ignored when determining the topic
    public static final String[] IGNORE_PHRASES = {
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
            "with reference to extract"
    };
}