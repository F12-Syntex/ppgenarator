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
        // Theme 1 - Enhanced with more keywords and variations
        addKeywords("scarcity and choice", "scarcity", "choice", "opportunity cost", "economic problem",
                "limited resources", "unlimited wants", "resource allocation", "economics is about",
                "basic economic problem", "finite resources", "choosing", "trade-off", "alternatives");
        
        addKeywords("production possibility frontiers", "ppf", "production possibility", "frontier",
                "transformation curve", "opportunity cost", "trade-off", "attainable", "unattainable",
                "efficient production", "productive efficiency", "production boundary", "output combinations");
        
        addKeywords("specialization and trade", "specialization", "specialisation", "comparative advantage",
                "absolute advantage", "division of labor", "trade", "trading", "specializing", "gains from trade",
                "mutually beneficial", "export", "import", "exchange", "international trade");
        
        addKeywords("demand and supply", "demand curve", "supply curve", "equilibrium", "market equilibrium",
                "price mechanism", "quantity demanded", "quantity supplied", "market forces", "shifts in demand",
                "shifts in supply", "demand schedule", "supply schedule", "market clearing");
        
        addKeywords("price determination", "price", "equilibrium price", "market clearing", "surplus", "shortage",
                "price floor", "price ceiling", "rationing", "allocating", "market price", "pricing",
                "price formation", "price discovery");
        
        addKeywords("price mechanism", "price signal", "signaling function", "rationing function", "incentive function",
                "allocation of resources", "invisible hand", "market mechanism", "resource allocation",
                "market signals", "price system");
        
        addKeywords("consumer and producer surplus", "consumer surplus", "producer surplus", "total surplus",
                "economic welfare", "deadweight loss", "welfare loss", "welfare gain", "economic efficiency",
                "consumer benefit", "producer benefit", "welfare economics", "surplus");
        
        addKeywords("elasticity", "elastic", "inelastic", "price elasticity", "income elasticity", "cross elasticity",
                "ped", "yed", "xed", "elasticity coefficient", "elastic supply", "elastic demand", "unitary elasticity",
                "responsiveness", "sensitivity", "percentage change");
        
        addKeywords("alternative market structures", "market structure", "competition", "monopoly", "oligopoly",
                "monopolistic competition", "perfect competition", "competitive markets", "concentration ratio",
                "market power", "barriers to entry", "market organization");
        
        addKeywords("market failure", "market failure", "inefficient allocation", "socially optimal", "social optimum",
                "merit goods", "demerit goods", "private cost", "social cost", "inefficiency", "welfare loss",
                "market solution", "failure", "imperfect market");
        
        addKeywords("externalities", "externality", "social cost", "social benefit", "negative externality",
                "positive externality", "spillover", "external costs", "external benefits", "third party", "pollution",
                "marginal external cost", "external effects", "spillover effects");
        
        addKeywords("public goods", "public good", "free-rider", "non-rival", "non-excludable", "market provision",
                "collective provision", "private provision", "non-rivalrous", "non-excludability",
                "government provision", "lighthouse", "public provision");
        
        addKeywords("information gaps", "information", "asymmetric information", "imperfect information",
                "adverse selection", "moral hazard", "incomplete information", "information failure",
                "hidden knowledge", "hidden action", "information asymmetry");
        
        addKeywords("government intervention", "government", "intervention", "regulation", "subsidy", "tax",
                "price control", "quota", "buffer stock", "legislation", "state provision", "government failure",
                "policy", "government action", "state intervention");

        // Theme 2 - Enhanced
        addKeywords("economic growth", "economic growth", "gdp", "real gdp", "long run", "actual growth",
                "potential growth", "business cycle", "trend growth", "gdp per capita", "living standards",
                "sustainable growth", "productivity", "output growth", "expansion");
        
        addKeywords("inflation", "inflation", "cpi", "rpi", "price level", "deflation", "hyperinflation", "cost-push",
                "demand-pull", "wage inflation", "stagflation", "disinflation", "price stability", "indexation",
                "rising prices", "price increase");
        
        addKeywords("employment and unemployment", "unemployment", "employment", "jobless", "labor force",
                "participation rate", "natural rate", "frictional unemployment", "structural unemployment",
                "cyclical unemployment", "full employment", "job creation", "unemployment rate");
        
        addKeywords("balance of payments", "balance of payments", "current account", "financial account",
                "capital account", "deficit", "surplus", "trade deficit", "trade surplus", "import", "export",
                "net exports", "invisible trade", "balance of trade");
        
        addKeywords("circular flow of income", "circular flow", "leakages", "injections", "withdrawals",
                "national income", "income flow", "spending flow", "circular flow diagram", "savings", "investment",
                "taxation", "government spending", "economic flow");
        
        addKeywords("aggregate demand", "aggregate demand", "ad", "consumption", "investment", "government spending",
                "net exports", "ad curve", "shifts in ad", "components of ad", "wealth effect", "interest rate effect",
                "multiplier effect", "total demand");
        
        addKeywords("aggregate supply", "aggregate supply", "as", "sras", "lras", "production", "potential output",
                "aggregate supply curve", "productive capacity", "shifts in as", "output gap", "supply shock",
                "productive potential", "total supply");
        
        addKeywords("national income", "national income", "gdp", "gnp", "nominal", "real", "output", "value added",
                "final goods", "intermediate goods", "national output", "income method", "expenditure method",
                "production method");
        
        addKeywords("economic cycle", "economic cycle", "business cycle", "boom", "recession", "slump", "recovery",
                "peak", "trough", "expansion", "contraction", "upturn", "downturn", "trade cycle", "fluctuations",
                "cyclical");
        
        addKeywords("monetary policy", "monetary policy", "interest rate", "money supply", "central bank",
                "quantitative easing", "bank rate", "discount rate", "liquidity", "monetary transmission",
                "inflation targeting", "repo rate", "monetary control");
        
        addKeywords("fiscal policy", "fiscal policy", "government spending", "taxation", "budget", "deficit", "surplus",
                "public finances", "fiscal stimulus", "austerity", "automatic stabilizers", "discretionary policy",
                "fiscal stance", "macroeconomic policy", "public spending");
        
        addKeywords("supply-side policies", "supply-side", "productivity", "competitiveness", "deregulation",
                "privatization", "tax incentives", "labor market", "skills", "training", "infrastructure", "incentives",
                "enterprise", "supply side");

        // Theme 3 - Enhanced
        addKeywords("business growth", "business growth", "merger", "acquisition", "organic growth", "integration",
                "horizontal integration", "vertical integration", "conglomerate", "takeover", "economies of scale",
                "diversification", "expansion", "growth strategy");
        
        addKeywords("business objectives", "business objective", "profit maximization", "revenue maximization", "sales",
                "market share", "growth", "survival", "shareholder value", "stakeholder interests", "satisficing",
                "social responsibility", "objectives", "aims");
        
        addKeywords("revenue", "revenue", "total revenue", "average revenue", "marginal revenue", "sales", "income",
                "turnover", "demand curve", "ar", "mr", "price elasticity", "pricing strategy", "sales revenue");
        
        addKeywords("costs", "cost", "fixed cost", "variable cost", "total cost", "average cost", "marginal cost",
                "sunk cost", "opportunity cost", "accounting cost", "economic cost", "explicit cost", "implicit cost",
                "production costs");
        
        addKeywords("economies of scale", "economies of scale", "diseconomies", "increasing returns",
                "long run average cost", "lrac", "minimum efficient scale", "internal economies", "external economies",
                "returns to scale", "scale economies");
        
        addKeywords("profit", "profit", "loss", "profit maximization", "normal profit", "supernormal profit",
                "economic profit", "accounting profit", "profitability", "profit margin", "profit motive",
                "return on capital", "profit making");
        
        addKeywords("market structures", "market structure", "competition", "competitive", "concentration ratio",
                "herfindahl index", "barriers to entry", "market power", "market concentration", "competitive behavior",
                "strategic behavior", "market organization");
        
        addKeywords("perfect competition", "perfect competition", "price taker", "homogeneous", "many firms",
                "free entry", "free exit", "perfect information", "normal profit", "allocative efficiency",
                "productive efficiency", "perfectly competitive");
        
        addKeywords("monopolistic competition", "monopolistic competition", "product differentiation", "brand",
                "advertising", "unique selling point", "non-price competition", "brand loyalty", "entry", "exit",
                "short run profit", "long run", "differentiated products");
        
        addKeywords("oligopoly", "oligopoly", "interdependence", "few sellers", "collusion", "cartel", "price war",
                "non-price competition", "game theory", "prisoners dilemma", "price leadership", "barriers to entry",
                "strategic behavior", "oligopolistic");
        
        addKeywords("monopoly", "monopoly", "price maker", "barriers to entry", "single seller", "price discrimination",
                "monopoly power", "market power", "deadweight loss", "inefficiency", "natural monopoly",
                "legal monopoly", "monopolist");
        
        addKeywords("price discrimination", "price discrimination", "first degree", "second degree", "third degree",
                "price targeting", "market segmentation", "price differentiation", "consumer surplus",
                "price elasticity", "market power", "discriminatory pricing");
        
        addKeywords("contestable markets", "contestable market", "barrier to entry", "barrier to exit", "sunk cost",
                "hit and run", "entry threat", "potential competition", "perfectly contestable", "entry deterrence",
                "limit pricing", "contestability");
        
        addKeywords("labor market", "labor market", "labour market", "wage", "employment", "monopsony",
                "supply of labor", "demand for labor", "derived demand", "marginal revenue product",
                "marginal cost of labor", "wage rate", "labour", "workforce");
        
        addKeywords("wage determination", "wage", "determination", "supply of labor", "demand for labor",
                "equilibrium wage", "minimum wage", "collective bargaining", "trade unions", "wage differentials",
                "labor productivity", "wage setting");
        
        addKeywords("labor market failure", "labor market failure", "minimum wage", "discrimination", "immobility",
                "geographic immobility", "occupational immobility", "information asymmetry", "monopsony",
                "exploitation", "wage inequality", "labour market failure");

        // Theme 4 - Enhanced with more comprehensive coverage
        addKeywords("international economics", "international", "global", "trade", "foreign", "world economy",
                "globalization", "globalisation", "international trade", "capital flows", "migration", "transnational",
                "economic integration", "global economy", "world trade");
        
        addKeywords("absolute and comparative advantage", "absolute advantage", "comparative advantage",
                "opportunity cost", "specialization", "trade gains", "ricardian model", "production possibility",
                "relative efficiency", "terms of trade", "competitive advantage");
        
        addKeywords("terms of trade", "terms of trade", "exchange ratio", "export prices", "import prices",
                "favorable terms", "unfavorable terms", "commodity terms", "income terms", "single factorial terms",
                "double factorial terms", "trade terms");
        
        addKeywords("trading blocs", "trading bloc", "regional", "free trade", "eu", "nafta", "asean", "customs union",
                "common market", "economic union", "free trade area", "preferential trade", "trade creation",
                "trade diversion", "economic integration", "regional trade");
        
        addKeywords("world trade organization", "wto", "world trade organization", "trade liberalization", "gatt",
                "uruguay round", "doha round", "trade dispute", "most favored nation", "national treatment",
                "protectionism", "multilateral trade");
        
        addKeywords("exchange rates", "exchange rate", "currency", "appreciation", "depreciation", "devaluation",
                "revaluation", "floating", "fixed", "managed float", "purchasing power parity", "currency market",
                "forex", "foreign exchange");
        
        addKeywords("international competitiveness", "competitiveness", "international", "productivity",
                "unit labor cost", "export performance", "import penetration", "comparative advantage", "exchange rate",
                "non-price factors", "competitive advantage");
        
        addKeywords("poverty and inequality", "poverty", "inequality", "income distribution", "wealth distribution",
                "gini coefficient", "lorenz curve", "relative poverty", "absolute poverty", "poverty line",
                "redistribution", "social mobility", "income inequality");
        
        addKeywords("developing economies", "developing", "development", "third world", "less developed", "emerging",
                "developing countries", "newly industrialized", "underdeveloped", "global south",
                "economic development", "industrialization", "emerging markets");
        
        addKeywords("financial markets", "financial market", "stock market", "bond market", "forex", "securities",
                "capital market", "money market", "primary market", "secondary market", "financial intermediation",
                "speculation", "financial system");
        
        addKeywords("economic development", "development", "growth", "hdi", "standard of living", "quality of life",
                "human development", "sustainable development", "millennium goals", "development gap",
                "development strategies", "development economics");
        
        addKeywords("sustainability", "sustainable", "sustainability", "environment", "green", "renewable",
                "future generations", "sustainable development", "climate change", "carbon emissions",
                "resource depletion", "circular economy", "environmental sustainability");
        
        addKeywords("globalisation", "globalisation", "globalization", "global", "multinational", "tnc",
                "global economy", "global trade", "global integration", "global market", "global supply chain",
                "global competition", "negative effects of globalisation", "globalisation effects", "worldwide");
        
        addKeywords("protectionism", "protectionism", "tariff", "quota", "trade barrier", "import restriction",
                "trade protection", "domestic industry protection", "trade war", "anti-dumping", "safeguard measures",
                "protective measures");
        
        addKeywords("trade liberalization", "trade liberalization", "free trade", "trade liberalisation",
                "removal of barriers", "trade openness", "trade reform", "trade deregulation", "open trade");
    }

    private void initializeConceptRelationships() {
        // Enhanced relationships for better cross-topic categorization
        
        // Market dynamics with broader connections
        addRelatedConcepts("market", "demand and supply", "price determination", "price mechanism",
                "market structures", "market failure", "perfect competition", "monopoly", "oligopoly",
                "monopolistic competition", "consumer and producer surplus");

        // Price-related concepts with extensive connections
        addRelatedConcepts("price", "demand and supply", "price determination", "price mechanism", "elasticity",
                "price discrimination", "inflation", "consumer and producer surplus", "market structures");

        // Government-related concepts with comprehensive coverage
        addRelatedConcepts("government", "government intervention", "fiscal policy", "monetary policy",
                "supply-side policies", "public goods", "market failure", "externalities", "regulation",
                "poverty and inequality", "economic growth");

        // International concepts with full integration
        addRelatedConcepts("international", "international economics", "absolute and comparative advantage",
                "balance of payments", "exchange rates", "trading blocs", "globalisation", "trade liberalization",
                "protectionism", "developing economies", "world trade organization");

        // Growth and development with cross-connections
        addRelatedConcepts("growth", "economic growth", "business growth", "economic development",
                "developing economies", "sustainability", "supply-side policies", "productivity");

        // Cost and revenue concepts
        addRelatedConcepts("cost", "costs", "profit", "revenue", "economies of scale", "externalities",
                "business objectives", "market structures");

        // Market structure concepts with full integration
        addRelatedConcepts("competition", "market structures", "perfect competition", "monopolistic competition",
                "oligopoly", "monopoly", "contestable markets", "market failure", "efficiency");

        // Labor market concepts
        addRelatedConcepts("labor", "labor market", "wage determination", "labor market failure",
                "employment and unemployment", "government intervention", "market failure");
        addRelatedConcepts("labour", "labor market", "wage determination", "labor market failure",
                "employment and unemployment", "government intervention", "market failure");

        // Policy interconnections
        addRelatedConcepts("policy", "fiscal policy", "monetary policy", "supply-side policies",
                "government intervention", "economic growth", "inflation", "unemployment");
        addRelatedConcepts("macroeconomic", "fiscal policy", "monetary policy", "supply-side policies",
                "aggregate demand", "aggregate supply", "economic growth", "inflation");

        // Social and environmental issues
        addRelatedConcepts("inequality", "poverty and inequality", "economic development", "sustainability",
                "government intervention", "fiscal policy", "developing economies", "globalisation");
        addRelatedConcepts("environment", "sustainability", "externalities", "market failure", 
                "government intervention", "developing economies");
        addRelatedConcepts("pollution", "externalities", "market failure", "government intervention", 
                "sustainability");

        // Globalisation comprehensive connections
        addRelatedConcepts("globalisation", "international economics", "protectionism", "trade liberalization",
                "poverty and inequality", "developing economies", "multinational", "global", "trade");
        addRelatedConcepts("global", "globalisation", "international economics", "multinational", "trade",
                "developing economies");

        // Trade connections
        addRelatedConcepts("trade", "international economics", "globalisation", "protectionism", 
                "trade liberalization", "absolute and comparative advantage", "balance of payments",
                "trading blocs");

        // Development connections
        addRelatedConcepts("development", "economic development", "developing economies", "poverty and inequality",
                "sustainability", "economic growth", "globalisation");
        addRelatedConcepts("developing", "developing economies", "economic development", "poverty and inequality",
                "globalisation", "international economics");

        // Efficiency and welfare connections
        addRelatedConcepts("efficiency", "market failure", "externalities", "public goods",
                "consumer and producer surplus", "perfect competition", "government intervention");
        addRelatedConcepts("welfare", "consumer and producer surplus", "market failure", "externalities",
                "government intervention", "poverty and inequality");

        // Financial and monetary connections
        addRelatedConcepts("monetary", "monetary policy", "inflation", "exchange rates", "financial markets",
                "economic growth");
        addRelatedConcepts("fiscal", "fiscal policy", "government intervention", "economic growth",
                "aggregate demand", "poverty and inequality");

        // Business and firm connections
        addRelatedConcepts("business", "business growth", "business objectives", "market structures",
                "profit", "revenue", "costs", "economies of scale");
        addRelatedConcepts("firm", "market structures", "profit", "revenue", "costs", "business objectives",
                "perfect competition", "monopoly");
    }

    private void addKeywords(String topic, String... keywords) {
        topicKeywords.put(topic, Arrays.asList(keywords));
    }

    private void addRelatedConcepts(String concept, String... relatedTopics) {
        conceptRelationships.put(concept, Arrays.asList(relatedTopics));
    }
}