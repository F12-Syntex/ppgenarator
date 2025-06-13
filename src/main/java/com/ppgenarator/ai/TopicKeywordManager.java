package com.ppgenarator.ai;

import java.util.*;

public class TopicKeywordManager {
    private final Map<String, List<String>> topicKeywords = new HashMap<>();
    private final Map<String, List<String>> conceptRelationships = new HashMap<>();

    public TopicKeywordManager() {
        initializeTopicKeywords();
        initializeConceptRelationships();
    }

    public Map<String, List<String>> getTopicKeywords() {
        return topicKeywords;
    }

    public Map<String, List<String>> getConceptRelationships() {
        return conceptRelationships;
    }

    private void initializeTopicKeywords() {
        // Theme 1.1 - Nature of economics
        addKeywords("1.1.1 Economics as a social science", "economics", "social science", "human behaviour", 
                "ceteris paribus", "assumptions", "scientific method", "hypothesis", "theory", "model");
        
        addKeywords("1.1.2 Positive and normative economic statements", "positive", "normative", "value judgement", 
                "objective", "subjective", "fact", "opinion", "should", "ought", "statement");
        
        addKeywords("1.1.3 The economic problem", "scarcity", "choice", "opportunity cost", "economic problem",
                "limited resources", "unlimited wants", "resource allocation", "basic economic problem", 
                "finite resources", "choosing", "trade-off", "alternatives");
        
        addKeywords("1.1.4 Production possibility frontiers", "ppf", "production possibility", "frontier",
                "transformation curve", "opportunity cost", "trade-off", "attainable", "unattainable",
                "efficient production", "productive efficiency", "production boundary", "output combinations");
        
        addKeywords("1.1.5 Specialisation and the division of labour", "specialisation", "specialization", 
                "division of labour", "division of labor", "productivity", "efficiency", "expertise", 
                "comparative advantage", "absolute advantage", "trade", "exchange");
        
        addKeywords("1.1.6 Free market economies, mixed economy and command economy", "free market", "mixed economy", 
                "command economy", "market economy", "planned economy", "capitalism", "socialism", "private ownership", 
                "public ownership", "government control", "market forces");

        // Theme 1.2 - How markets work
        addKeywords("1.2.1 Rational decision making", "rational", "decision making", "utility", "maximisation", 
                "marginal utility", "consumer choice", "rational consumer", "rational firm", "profit maximisation", 
                "cost minimisation");
        
        addKeywords("1.2.2 Demand", "demand", "demand curve", "law of demand", "individual demand", "market demand", 
                "willingness to pay", "ability to pay", "quantity demanded", "demand schedule");
        
        addKeywords("1.2.3 Price, income and cross elasticities of demand", "price elasticity", "income elasticity", 
                "cross elasticity", "ped", "yed", "xed", "elastic", "inelastic", "elasticity coefficient", 
                "responsiveness", "sensitivity", "percentage change", "substitutes", "complements");
        
        addKeywords("1.2.4 Supply", "supply", "supply curve", "law of supply", "quantity supplied", "supply schedule", 
                "willingness to supply", "ability to supply", "market supply", "individual supply");
        
        addKeywords("1.2.5 Elasticity of supply", "supply elasticity", "pes", "elastic supply", "inelastic supply", 
                "perfectly elastic", "perfectly inelastic", "time period", "spare capacity", "stocks");
        
        addKeywords("1.2.6 Price determination", "price determination", "equilibrium price", "market clearing", 
                "surplus", "shortage", "excess demand", "excess supply", "market forces", "price formation");
        
        addKeywords("1.2.7 Price mechanism", "price mechanism", "price signal", "signaling function", 
                "rationing function", "incentive function", "allocation of resources", "invisible hand", 
                "market mechanism", "resource allocation", "market signals", "price system");
        
        addKeywords("1.2.8 Consumer and producer surplus", "consumer surplus", "producer surplus", "total surplus",
                "economic welfare", "deadweight loss", "welfare loss", "welfare gain", "economic efficiency",
                "consumer benefit", "producer benefit", "welfare economics", "surplus");
        
        addKeywords("1.2.9 Indirect taxes and subsidies", "indirect tax", "subsidy", "ad valorem", "specific tax", 
                "tax incidence", "burden of tax", "government revenue", "market intervention", "excise duty", "vat");
        
        addKeywords("1.2.10 Alternative views of consumer behaviour", "behavioural economics", "bounded rationality", 
                "cognitive bias", "heuristics", "anchoring", "default choice", "choice architecture", "nudge theory");

        // Theme 1.3 - Market failure
        addKeywords("1.3.1 Types of market failure", "market failure", "partial market failure", "complete market failure", 
                "allocative efficiency", "productive efficiency", "dynamic efficiency", "social optimum");
        
        addKeywords("1.3.2 Externalities", "externality", "externalities", "social cost", "social benefit", 
                "negative externality", "positive externality", "spillover", "external costs", "external benefits", 
                "third party", "pollution", "marginal external cost", "external effects", "spillover effects");
        
        addKeywords("1.3.3 Public goods", "public good", "public goods", "free-rider", "non-rival", "non-excludable", 
                "market provision", "collective provision", "private provision", "non-rivalrous", "non-excludability",
                "government provision", "lighthouse", "public provision");
        
        addKeywords("1.3.4 Information gaps", "information gaps", "asymmetric information", "imperfect information",
                "adverse selection", "moral hazard", "incomplete information", "information failure",
                "hidden knowledge", "hidden action", "information asymmetry");

        // Theme 1.4 - Government intervention
        addKeywords("1.4.1 Government intervention in markets", "government intervention", "regulation", "price controls", 
                "maximum price", "minimum price", "buffer stocks", "tradeable permits", "provision of information", 
                "provision of merit goods", "prohibition of demerit goods");
        
        addKeywords("1.4.2 Government failure", "government failure", "unintended consequences", "excessive bureaucracy", 
                "lack of information", "conflicting objectives", "short-termism", "regulatory capture");

        // Theme 2.1 - Measures of economic performance
        addKeywords("2.1.1 Economic growth", "economic growth", "gdp", "real gdp", "nominal gdp", "gdp per capita", 
                "living standards", "sustainable growth", "productivity", "output growth", "expansion", "recession");
        
        addKeywords("2.1.2 Inflation", "inflation", "deflation", "disinflation", "cpi", "rpi", "price level", 
                "hyperinflation", "cost-push inflation", "demand-pull inflation", "wage inflation", "stagflation", 
                "price stability", "indexation", "rising prices");
        
        addKeywords("2.1.3 Employment and unemployment", "unemployment", "employment", "jobless", "labour force",
                "participation rate", "natural rate", "frictional unemployment", "structural unemployment",
                "cyclical unemployment", "seasonal unemployment", "full employment", "job creation", "unemployment rate");
        
        addKeywords("2.1.4 Balance of payments", "balance of payments", "current account", "financial account",
                "capital account", "trade deficit", "trade surplus", "import", "export", "net exports", 
                "invisible trade", "balance of trade", "bop");

        // Theme 2.2 - Aggregate demand
        addKeywords("2.2.1 The characteristics of AD", "aggregate demand", "ad", "total demand", "demand side", 
                "components of ad", "ad curve", "shifts in ad", "movements along ad");
        
        addKeywords("2.2.2 Consumption (C)", "consumption", "consumer spending", "marginal propensity to consume", 
                "mpc", "average propensity to consume", "apc", "disposable income", "wealth effect", "confidence");
        
        addKeywords("2.2.3 Investment (I)", "investment", "gross investment", "net investment", "capital formation", 
                "business investment", "accelerator", "marginal efficiency of capital", "animal spirits");
        
        addKeywords("2.2.4 Government expenditure (G)", "government expenditure", "government spending", 
                "public spending", "fiscal policy", "budget", "current expenditure", "capital expenditure");
        
        addKeywords("2.2.5 Net trade (X-M)", "net exports", "net trade", "exports", "imports", "exchange rate", 
                "competitiveness", "foreign income", "domestic income", "trade balance");

        // Theme 2.3 - Aggregate supply
        addKeywords("2.3.1 The characteristics of AS", "aggregate supply", "as", "total supply", "supply side", 
                "as curve", "shifts in as", "movements along as", "productive capacity");
        
        addKeywords("2.3.2 Short-run AS", "short-run aggregate supply", "sras", "short run", "sticky wages", 
                "sticky prices", "spare capacity", "output gap");
        
        addKeywords("2.3.3 Long-run AS", "long-run aggregate supply", "lras", "long run", "potential output", 
                "full employment", "classical", "keynesian", "supply shocks");

        // Continue with remaining themes...
        // I'll add the rest of the themes following the same pattern
        
        // Theme 2.4 - National income
        addKeywords("2.4.1 National income", "national income", "gdp", "gnp", "gni", "nominal", "real", 
                "value added", "final goods", "intermediate goods", "national output", "circular flow");
        
        addKeywords("2.4.2 Injections and withdrawals", "injections", "withdrawals", "leakages", "investment", 
                "government spending", "exports", "savings", "taxation", "imports", "circular flow");
        
        addKeywords("2.4.3 Equilibrium levels of real national output", "equilibrium", "national output", 
                "equilibrium level", "ad equals as", "full employment equilibrium", "unemployment equilibrium");
        
        addKeywords("2.4.4 The multiplier", "multiplier", "multiplier effect", "marginal propensity to consume", 
                "marginal propensity to save", "mpc", "mps", "injection", "withdrawal");

        // Add more themes as needed...
        // This is a comprehensive example showing the pattern
    }

    private void initializeConceptRelationships() {
        // Enhanced relationships for better cross-topic categorization
        
        // Market dynamics with broader connections
        addRelatedConcepts("market", "1.2.6 Price determination", "1.2.7 Price mechanism", 
                "1.3.1 Types of market failure", "3.4.2 Perfect competition", "3.4.5 Monopoly");

        // Price-related concepts
        addRelatedConcepts("price", "1.2.6 Price determination", "1.2.3 Price, income and cross elasticities of demand", 
                "1.2.9 Indirect taxes and subsidies", "2.1.2 Inflation");

        // Government-related concepts
        addRelatedConcepts("government", "1.4.1 Government intervention in markets", "2.2.4 Government expenditure (G)", 
                "2.6.2 Demand-side policies", "4.5.1 Public expenditure");

        // International concepts
        addRelatedConcepts("international", "4.1.2 Specialisation and trade", "4.1.8 Exchange rates", 
                "4.1.9 International competitiveness", "2.1.4 Balance of payments");

        // Add more relationships as needed...
    }

    private void addKeywords(String topic, String... keywords) {
        topicKeywords.put(topic, Arrays.asList(keywords));
    }

    private void addRelatedConcepts(String concept, String... relatedTopics) {
        conceptRelationships.put(concept, Arrays.asList(relatedTopics));
    }
}