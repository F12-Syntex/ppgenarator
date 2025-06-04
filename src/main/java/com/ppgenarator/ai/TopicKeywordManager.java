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

    private void initializeConceptRelationships() {
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

    private void addKeywords(String topic, String... keywords) {
        topicKeywords.put(topic, Arrays.asList(keywords));
    }

    private void addRelatedConcepts(String concept, String... relatedTopics) {
        conceptRelationships.put(concept, Arrays.asList(relatedTopics));
    }
}