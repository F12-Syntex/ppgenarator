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

                addKeywords("1.1.1 Economics as a social science", "economics", "social science", "human behaviour",
                                "ceteris paribus", "assumptions", "scientific method", "hypothesis", "theory", "model",
                                "thinking like an economist", "economic methodology", "empirical testing");

                addKeywords("1.1.2 Positive and normative economic statements", "positive", "normative",
                                "value judgement",
                                "objective", "subjective", "fact", "opinion", "should", "ought", "statement",
                                "policy decisions",
                                "economic analysis", "economic prescription");

                addKeywords("1.1.3 The economic problem", "scarcity", "choice", "opportunity cost", "economic problem",
                                "limited resources", "unlimited wants", "resource allocation", "basic economic problem",
                                "finite resources", "choosing", "trade-off", "alternatives", "renewable resources",
                                "non-renewable resources", "economic agents");

                addKeywords("1.1.4 Production possibility frontiers", "ppf", "production possibility", "frontier",
                                "transformation curve", "opportunity cost", "trade-off", "attainable", "unattainable",
                                "efficient production", "productive efficiency", "production boundary",
                                "output combinations",
                                "economic growth", "capital goods", "consumer goods", "shifts in PPF");

                addKeywords("1.1.5 Specialisation and the division of labour", "specialisation", "specialization",
                                "division of labour", "division of labor", "productivity", "efficiency", "expertise",
                                "comparative advantage", "absolute advantage", "trade", "exchange", "adam smith",
                                "functions of money", "medium of exchange", "store of value", "measure of value",
                                "method of deferred payment");

                addKeywords("1.1.6 Free market economies, mixed economy and command economy", "free market",
                                "mixed economy",
                                "command economy", "market economy", "planned economy", "capitalism", "socialism",
                                "private ownership",
                                "public ownership", "government control", "market forces", "adam smith",
                                "friedrich hayek",
                                "karl marx", "invisible hand", "state intervention");

                // Theme 1.2 - How markets work
                addKeywords("1.2.1 Rational decision making", "rational", "decision making", "utility", "maximisation",
                                "marginal utility", "consumer choice", "rational consumer", "rational firm",
                                "profit maximisation",
                                "cost minimisation", "rational economic decision making", "self-interest",
                                "homo economicus");

                addKeywords("1.2.2 Demand", "demand", "demand curve", "law of demand", "individual demand",
                                "market demand",
                                "willingness to pay", "ability to pay", "quantity demanded", "demand schedule",
                                "determinants of demand",
                                "conditions of demand", "shifts in demand", "movements along demand curve",
                                "diminishing marginal utility");

                addKeywords("1.2.3 Price, income and cross elasticities of demand", "price elasticity",
                                "income elasticity",
                                "cross elasticity", "ped", "yed", "xed", "elastic", "inelastic",
                                "elasticity coefficient",
                                "responsiveness", "sensitivity", "percentage change", "substitutes", "complements",
                                "normal goods", "inferior goods", "luxury goods", "total revenue", "relatively elastic",
                                "relatively inelastic", "perfectly elastic", "perfectly inelastic",
                                "unitary elasticity");

                addKeywords("1.2.4 Supply", "supply", "supply curve", "law of supply", "quantity supplied",
                                "supply schedule",
                                "willingness to supply", "ability to supply", "market supply", "individual supply",
                                "determinants of supply", "conditions of supply", "shifts in supply",
                                "movements along supply curve");

                addKeywords("1.2.5 Elasticity of supply", "supply elasticity", "pes", "elastic supply",
                                "inelastic supply",
                                "perfectly elastic", "perfectly inelastic", "time period", "spare capacity", "stocks",
                                "short run", "long run", "price elasticity of supply");

                addKeywords("1.2.6 Price determination", "price determination", "equilibrium price", "market clearing",
                                "surplus", "shortage", "excess demand", "excess supply", "market forces",
                                "price formation",
                                "disequilibrium", "equilibrium quantity", "market equilibrium");

                addKeywords("1.2.7 Price mechanism", "price mechanism", "price signal", "signaling function",
                                "rationing function", "incentive function", "allocation of resources", "invisible hand",
                                "market mechanism", "resource allocation", "market signals", "price system",
                                "adam smith", "local markets", "national markets", "global markets");

                addKeywords("1.2.8 Consumer and producer surplus", "consumer surplus", "producer surplus",
                                "total surplus",
                                "economic welfare", "deadweight loss", "welfare loss", "welfare gain",
                                "economic efficiency",
                                "consumer benefit", "producer benefit", "welfare economics", "surplus",
                                "area under curve");

                addKeywords("1.2.9 Indirect taxes and subsidies", "indirect tax", "subsidy", "ad valorem",
                                "specific tax",
                                "tax incidence", "burden of tax", "government revenue", "market intervention",
                                "excise duty", "vat",
                                "consumer subsidy", "producer subsidy", "elasticity and tax incidence", "tax burden");

                addKeywords("1.2.10 Alternative views of consumer behaviour", "behavioural economics",
                                "bounded rationality",
                                "cognitive bias", "heuristics", "anchoring", "default choice", "choice architecture",
                                "nudge theory",
                                "irrational behaviour", "habitual behaviour", "influence of others",
                                "weakness at computation");

                // Theme 1.3 - Market failure
                addKeywords("1.3.1 Types of market failure", "market failure", "partial market failure",
                                "complete market failure",
                                "allocative efficiency", "productive efficiency", "dynamic efficiency",
                                "social optimum",
                                "misallocation of resources", "welfare loss", "merit goods", "demerit goods",
                                "missing markets");

                addKeywords("1.3.2 Externalities", "externality", "externalities", "social cost", "social benefit",
                                "negative externality", "positive externality", "spillover", "external costs",
                                "external benefits",
                                "third party", "pollution", "marginal external cost", "external effects",
                                "spillover effects",
                                "private costs", "private benefits", "welfare loss area", "welfare gain area",
                                "social optimum position",
                                "market equilibrium position", "marginal social cost", "marginal social benefit");

                addKeywords("1.3.3 Public goods", "public good", "public goods", "free-rider", "non-rival",
                                "non-excludable",
                                "market provision", "collective provision", "private provision", "non-rivalrous",
                                "non-excludability",
                                "government provision", "lighthouse", "public provision", "private goods",
                                "quasi-public goods");

                addKeywords("1.3.4 Information gaps", "information gaps", "asymmetric information",
                                "imperfect information",
                                "adverse selection", "moral hazard", "incomplete information", "information failure",
                                "hidden knowledge", "hidden action", "information asymmetry",
                                "misallocation of resources",
                                "symmetric information", "perfect information");

                // Theme 1.4 - Government intervention
                addKeywords("1.4.1 Government intervention in markets", "government intervention", "regulation",
                                "price controls",
                                "maximum price", "minimum price", "buffer stocks", "tradeable permits",
                                "provision of information",
                                "provision of merit goods", "prohibition of demerit goods", "subsidy",
                                "indirect taxation",
                                "state provision", "laws and regulations");

                addKeywords("1.4.2 Government failure", "government failure", "unintended consequences",
                                "excessive bureaucracy",
                                "lack of information", "conflicting objectives", "short-termism", "regulatory capture",
                                "distortion of price signals", "excessive administrative costs", "information gaps",
                                "net welfare loss", "government intervention failure");

                // Theme 2.1 - Measures of economic performance
                addKeywords("2.1.1 Economic growth", "economic growth", "gdp", "real gdp", "nominal gdp",
                                "gdp per capita",
                                "living standards", "sustainable growth", "productivity", "output growth", "expansion",
                                "recession",
                                "gross national income", "gni", "total and per capita", "value and volume",
                                "purchasing power parities", "ppp", "national happiness", "subjective happiness",
                                "wellbeing");

                addKeywords("2.1.2 Inflation", "inflation", "deflation", "disinflation", "cpi", "rpi", "price level",
                                "hyperinflation", "cost-push inflation", "demand-pull inflation", "wage inflation",
                                "stagflation",
                                "price stability", "indexation", "rising prices", "consumer prices index",
                                "retail prices index",
                                "money supply", "causes of inflation", "effects of inflation");

                addKeywords("2.1.3 Employment and unemployment", "unemployment", "employment", "jobless",
                                "labour force",
                                "participation rate", "natural rate", "frictional unemployment",
                                "structural unemployment",
                                "cyclical unemployment", "seasonal unemployment", "full employment", "job creation",
                                "unemployment rate",
                                "claimant count", "international labour organisation", "labour force survey",
                                "under-employment",
                                "inactivity rate", "real wage inflexibility", "migration", "skills");

                addKeywords("2.1.4 Balance of payments", "balance of payments", "current account", "financial account",
                                "capital account", "trade deficit", "trade surplus", "import", "export", "net exports",
                                "invisible trade", "balance of trade", "bop", "current account deficit",
                                "current account surplus",
                                "international trade", "interconnectedness", "macroeconomic objectives");

                // Theme 2.2 - Aggregate demand
                addKeywords("2.2.1 The characteristics of AD", "aggregate demand", "ad", "total demand", "demand side",
                                "components of ad", "ad curve", "shifts in ad", "movements along ad", "c+i+g+(x-m)",
                                "relative importance");

                addKeywords("2.2.2 Consumption (C)", "consumption", "consumer spending",
                                "marginal propensity to consume",
                                "mpc", "average propensity to consume", "apc", "disposable income", "wealth effect",
                                "confidence",
                                "interest rates", "savings", "wealth effects");

                addKeywords("2.2.3 Investment (I)", "investment", "gross investment", "net investment",
                                "capital formation",
                                "business investment", "accelerator", "marginal efficiency of capital",
                                "animal spirits",
                                "business expectations", "business confidence", "economic growth", "demand for exports",
                                "interest rates", "access to credit", "government regulations");

                addKeywords("2.2.4 Government expenditure (G)", "government expenditure", "government spending",
                                "public spending", "fiscal policy", "budget", "current expenditure",
                                "capital expenditure",
                                "trade cycle", "fiscal policy", "public sector spending");

                addKeywords("2.2.5 Net trade (X-M)", "net exports", "net trade", "exports", "imports", "exchange rate",
                                "competitiveness", "foreign income", "domestic income", "trade balance",
                                "world economy",
                                "protectionism", "non-price factors");

                // Theme 2.3 - Aggregate supply
                addKeywords("2.3.1 The characteristics of AS", "aggregate supply", "as", "total supply", "supply side",
                                "as curve", "shifts in as", "movements along as", "productive capacity", "short-run",
                                "long-run",
                                "relationship between sras and lras");

                addKeywords("2.3.2 Short-run AS", "short-run aggregate supply", "sras", "short run", "sticky wages",
                                "sticky prices", "spare capacity", "output gap", "raw materials costs", "energy costs",
                                "exchange rates", "tax rates");

                addKeywords("2.3.3 Long-run AS", "long-run aggregate supply", "lras", "long run", "potential output",
                                "full employment", "classical", "keynesian", "supply shocks", "technological advances",
                                "relative productivity", "education and skills", "government regulations",
                                "demographic changes", "migration", "competition policy");

                // Theme 2.4 - National income
                addKeywords("2.4.1 National income", "national income", "gdp", "gnp", "gni", "nominal", "real",
                                "value added", "final goods", "intermediate goods", "national output", "circular flow",
                                "income", "wealth");

                addKeywords("2.4.2 Injections and withdrawals", "injections", "withdrawals", "leakages", "investment",
                                "government spending", "exports", "savings", "taxation", "imports", "circular flow");

                addKeywords("2.4.3 Equilibrium levels of real national output", "equilibrium", "national output",
                                "equilibrium level", "ad equals as", "full employment equilibrium",
                                "unemployment equilibrium",
                                "real output", "price level", "ad/as diagrams");

                addKeywords("2.4.4 The multiplier", "multiplier", "multiplier effect", "marginal propensity to consume",
                                "marginal propensity to save", "mpc", "mps", "injection", "withdrawal",
                                "multiplier ratio",
                                "multiplier process", "marginal propensity to tax", "mpt",
                                "marginal propensity to import", "mpm",
                                "1/(1-mpc)", "1/mpw");

                // Theme 2.5 - Economic growth
                addKeywords("2.5.1 Causes of growth", "causes of economic growth", "factors of economic growth",
                                "actual growth", "potential growth", "export-led growth", "international trade",
                                "productivity growth", "capital accumulation", "technological progress",
                                "structural change");

                addKeywords("2.5.2 Output gaps", "output gap", "actual growth rates", "long-term trends",
                                "growth rates",
                                "positive output gap", "negative output gap", "spare capacity", "inflationary gap",
                                "deflationary gap",
                                "measurement difficulties", "potential output");

                addKeywords("2.5.3 Trade (business) cycle", "trade cycle", "business cycle", "boom", "recession",
                                "recovery", "slump", "expansion", "contraction", "peak", "trough",
                                "economic fluctuations",
                                "cyclical unemployment");

                addKeywords("2.5.4 The impact of economic growth", "benefits of growth", "costs of growth",
                                "economic growth impact",
                                "consumers", "firms", "government", "living standards", "future living standards",
                                "sustainability",
                                "environmental impact", "resource depletion", "income distribution", "quality of life");

                // Theme 2.6 - Macroeconomic objectives and policies
                addKeywords("2.6.1 Possible macroeconomic objectives", "economic growth", "low unemployment",
                                "low inflation",
                                "stable inflation", "balance of payments", "current account",
                                "balanced government budget",
                                "environmental protection", "income equality", "macroeconomic goals", "policy targets",
                                "economic stability");

                addKeywords("2.6.2 Demand-side policies", "demand-side policy", "monetary policy", "fiscal policy",
                                "interest rates", "quantitative easing", "money supply", "government spending",
                                "taxation",
                                "budget deficit", "budget surplus", "direct taxation", "indirect taxation",
                                "bank of england",
                                "monetary policy committee", "great depression", "global financial crisis",
                                "stimulus package",
                                "austerity", "keynesian economics", "multiplier effect");

                addKeywords("2.6.3 Supply-side policies", "supply-side policy", "market-based", "interventionist",
                                "incentives", "competition", "labour market reform", "skills", "education", "training",
                                "infrastructure", "productivity", "deregulation", "privatization", "tax cuts",
                                "welfare reform", "union reform", "enterprise zones");

                addKeywords("2.6.4 Conflicts and trade-offs between objectives and policies", "policy conflicts",
                                "trade-offs",
                                "phillips curve", "inflation-unemployment trade-off", "short-run phillips curve",
                                "economic growth vs environment", "equality vs efficiency", "short-term vs long-term",
                                "domestic vs international objectives");

                // Theme 3.1 - Business growth
                addKeywords("3.1.1 Sizes and types of firms", "small firms", "medium firms", "large firms",
                                "multinational", "transnational", "public sector", "private sector",
                                "principal-agent problem",
                                "separation of ownership and control", "profit organization",
                                "not-for-profit organization");

                addKeywords("3.1.2 Business growth", "organic growth", "internal growth", "vertical integration",
                                "forward vertical integration", "backward vertical integration",
                                "horizontal integration",
                                "conglomerate integration", "diversification", "takeover", "merger", "acquisition",
                                "market size", "finance access", "owner objectives", "regulation",
                                "constraints on growth");

                addKeywords("3.1.3 Demergers", "demerger", "divestment", "selling off", "spin-off",
                                "corporate restructuring",
                                "break-up", "anti-trust", "focus on core business", "competition authorities",
                                "workers", "consumers");

                // Theme 3.2 - Business objectives
                addKeywords("3.2.1 Business objectives", "profit maximisation", "revenue maximisation",
                                "sales maximisation",
                                "satisficing", "managerial objectives", "growth maximisation", "utility maximisation",
                                "corporate social responsibility", "stakeholder objectives", "shareholder value",
                                "margin",
                                "mr=mc", "tr=tc", "profit margins", "normal profit", "supernormal profit",
                                "abnormal profit");

                // Theme 3.3 - Revenues, costs and profits
                addKeywords("3.3.1 Revenue", "total revenue", "average revenue", "marginal revenue", "tr", "ar", "mr",
                                "price elasticity", "revenue relationship", "demand curve", "revenue maximization",
                                "revenue function", "sales revenue", "price and quantity");

                addKeywords("3.3.2 Costs", "total cost", "total fixed cost", "total variable cost", "average cost",
                                "average fixed cost", "average variable cost", "marginal cost", "tc", "tfc", "tvc",
                                "ac", "afc", "avc", "mc",
                                "short-run costs", "long-run costs", "diminishing marginal productivity",
                                "diminishing returns",
                                "cost curves", "u-shaped cost curve");

                addKeywords("3.3.3 Economies and diseconomies of scale", "economies of scale", "diseconomies of scale",
                                "minimum efficient scale", "technical economies", "purchasing economies",
                                "managerial economies",
                                "financial economies", "marketing economies", "internal economies",
                                "external economies",
                                "communication problems", "coordination problems", "long-run average cost");

                addKeywords("3.3.4 Normal profits, supernormal profits and losses", "normal profit",
                                "supernormal profit",
                                "abnormal profit", "economic profit", "accounting profit", "losses",
                                "profit maximization",
                                "mr=mc", "shut-down point", "break-even", "loss minimization", "short-run shutdown",
                                "long-run shutdown", "exit industry");

                // Theme 3.4 - Market structures
                addKeywords("3.4.1 Efficiency", "allocative efficiency", "productive efficiency", "dynamic efficiency",
                                "x-inefficiency", "pareto efficiency", "technical efficiency", "economic efficiency",
                                "efficiency in different markets", "perfect competition efficiency",
                                "monopoly efficiency",
                                "efficiency in oligopoly");

                addKeywords("3.4.2 Perfect competition", "perfect competition", "price taker", "homogeneous product",
                                "many buyers and sellers", "perfect information", "free entry and exit",
                                "normal profit",
                                "allocative efficiency", "productive efficiency", "p=mc", "ar=mr", "supply curve",
                                "long-run equilibrium");

                addKeywords("3.4.3 Monopolistic competition", "monopolistic competition", "product differentiation",
                                "many firms", "imperfect information", "supernormal profit", "normal profit",
                                "branding",
                                "advertising", "unique selling point", "barriers to entry", "excess capacity");

                addKeywords("3.4.4 Oligopoly", "oligopoly", "interdependence", "few sellers", "barriers to entry",
                                "concentration ratio", "price leadership", "price wars", "non-price competition",
                                "collusion",
                                "cartel", "game theory", "prisoner's dilemma", "nash equilibrium",
                                "kinked demand curve",
                                "price rigidity", "tacit collusion", "overt collusion", "predatory pricing",
                                "limit pricing",
                                "product differentiation", "brand loyalty", "advertising",
                                "n-firm concentration ratio");

                addKeywords("3.4.5 Monopoly", "monopoly", "single seller", "price maker", "barriers to entry",
                                "economies of scale", "legal barriers", "resource ownership", "profit maximization",
                                "deadweight loss", "price discrimination", "third degree price discrimination",
                                "natural monopoly", "regulatory capture", "consumer exploitation");

                addKeywords("3.4.6 Monopsony", "monopsony", "buyer power", "single buyer", "many sellers",
                                "wage determination", "exploitation", "minimum wage", "trade unions", "labour market",
                                "marginal cost of labour", "average cost of labour");

                addKeywords("3.4.7 Contestability", "contestable markets", "potential competition", "hit and run",
                                "barriers to entry", "barriers to exit", "sunk costs", "credible threat",
                                "limit pricing",
                                "predatory pricing", "competitive behavior", "monopoly behavior",
                                "price close to competitive level");

                // Theme 3.5 - Labour market
                addKeywords("3.5.1 Demand for labour", "demand for labour", "derived demand",
                                "marginal revenue product",
                                "productivity", "output prices", "substitute inputs", "complementary inputs",
                                "mrp=wage",
                                "wage determination", "profit maximization", "marginal cost",
                                "elasticity of demand for labour");

                addKeywords("3.5.2 Supply of labour", "supply of labour", "wage rates", "working conditions",
                                "migration",
                                "net benefits", "participation rate", "opportunity cost", "non-monetary factors",
                                "geographical mobility",
                                "occupational mobility", "market failure", "immobility of labour");

                addKeywords("3.5.3 Wage determination in competitive and non-competitive markets", "wage determination",
                                "labour market equilibrium", "competitive labour market", "monopsony",
                                "bilateral monopoly",
                                "trade unions", "minimum wage", "maximum wage", "public sector pay",
                                "labour market policies",
                                "elasticity of demand for labour", "elasticity of supply of labour",
                                "wage differential",
                                "discrimination", "collective bargaining");

                // Theme 3.6 - Government intervention
                addKeywords("3.6.1 Government intervention", "competition policy", "merger policy",
                                "monopoly regulation",
                                "price regulation", "profit regulation", "quality standards", "performance targets",
                                "competition commission", "competition and markets authority", "promoting competition",
                                "enhancing contestability", "small business promotion", "deregulation",
                                "competitive tendering",
                                "privatisation", "monopsony regulation", "nationalisation", "public ownership",
                                "state control");

                addKeywords("3.6.2 The impact of government intervention", "impact on prices", "impact on profit",
                                "impact on efficiency", "impact on quality", "impact on choice", "regulatory capture",
                                "asymmetric information", "government failure", "limits to intervention",
                                "compliance costs",
                                "administrative costs", "unintended consequences", "regulatory arbitrage");

                // Theme 4.1 - International economics
                addKeywords("4.1.1 Globalisation", "globalisation", "globalization", "global markets", "multinational",
                                "transnational", "capital flows", "labour migration", "trade flows",
                                "technology transfer",
                                "transport costs", "communication costs", "trade liberalization", "trade agreements",
                                "impact on producers", "impact on consumers", "impact on workers",
                                "impact on environment");

                addKeywords("4.1.2 Specialisation and trade", "absolute advantage", "comparative advantage",
                                "opportunity cost", "specialisation", "gains from trade", "terms of trade",
                                "trade creation",
                                "trade diversion", "economic integration", "ricardian model", "heckscher-ohlin",
                                "division of labour", "productivity gains", "consumption gains", "economic welfare");

                addKeywords("4.1.3 Pattern of trade", "pattern of trade", "direction of trade", "volume of trade",
                                "emerging economies", "trading blocs", "bilateral trading agreements", "exchange rates",
                                "resource endowment", "factor abundance", "technology differences", "trade flows",
                                "north-south trade", "south-south trade", "intra-industry trade",
                                "inter-industry trade");

                addKeywords("4.1.4 Terms of trade", "terms of trade", "export prices", "import prices",
                                "commodity prices",
                                "primary product prices", "manufactured goods prices", "improving terms",
                                "deteriorating terms",
                                "prebisch-singer hypothesis", "income elasticity", "price elasticity",
                                "barter terms of trade");

                addKeywords("4.1.5 Trading blocs and the World Trade Organisation (WTO)", "trading bloc",
                                "trade agreement",
                                "free trade area", "customs union", "common market", "monetary union", "economic union",
                                "eurozone", "optimal currency area", "single currency", "wto", "trade liberalization",
                                "most favoured nation", "national treatment", "trade disputes", "doha round");

                addKeywords("4.1.6 Restrictions on free trade", "protectionism", "tariff", "quota", "subsidy",
                                "voluntary export restraint", "technical barriers", "administrative barriers",
                                "non-tariff barriers",
                                "sanitary measures", "dumping", "anti-dumping", "strategic trade", "infant industry",
                                "sunset industry",
                                "employment protection", "consumer protection", "national security");

                addKeywords("4.1.7 Balance of payments", "balance of payments", "current account", "capital account",
                                "financial account", "trade in goods", "trade in services", "income flows",
                                "current transfers",
                                "fdi flows", "portfolio flows", "official reserves", "current account deficit",
                                "current account surplus",
                                "global imbalances", "marshall-lerner", "j-curve", "absorption approach");

                addKeywords("4.1.8 Exchange rates", "exchange rate", "floating exchange rate", "fixed exchange rate",
                                "managed exchange rate", "revaluation", "devaluation", "appreciation", "depreciation",
                                "purchasing power parity", "interest rate parity", "foreign exchange market",
                                "exchange rate intervention",
                                "foreign currency reserves", "competitive devaluation", "currency war",
                                "exchange rate pass-through");

                addKeywords("4.1.9 International competitiveness", "international competitiveness", "unit labour costs",
                                "relative unit labour costs", "relative export prices", "non-price competitiveness",
                                "quality",
                                "innovation", "design", "reliability", "after-sales service", "productivity",
                                "efficiency",
                                "competitiveness indices", "export market share", "world economic forum");

                // Theme 4.2 - Poverty and inequality
                addKeywords("4.2.1 Absolute and relative poverty", "absolute poverty", "relative poverty",
                                "poverty line",
                                "extreme poverty", "dollar-a-day", "minimum standard of living", "social exclusion",
                                "deprivation",
                                "causes of poverty", "poverty reduction", "poverty trap", "cycle of poverty",
                                "measures of absolute poverty",
                                "measures of relative poverty", "poverty threshold", "subsistence level",
                                "poverty alleviation");

                addKeywords("4.2.2 Inequality", "inequality", "income inequality", "wealth inequality", "lorenz curve",
                                "gini coefficient", "quintiles", "deciles", "progressive taxation",
                                "regressive taxation",
                                "redistribution", "welfare state", "social security", "capitalism", "inheritance",
                                "social mobility",
                                "income distribution", "wealth distribution", "economic inequality",
                                "significance of capitalism",
                                "impact of economic change", "development and inequality", "measurement of inequality");

                // Theme 4.3 - Emerging and developing economies
                addKeywords("4.3.1 Measures of development", "development", "human development index", "hdi",
                                "education",
                                "health", "living standards", "life expectancy", "years of schooling", "gdp per capita",
                                "purchasing power parity", "indicators of development", "limitations of hdi",
                                "multidimensional poverty index",
                                "comparing development levels", "development over time", "three dimensions of hdi",
                                "alternative indicators",
                                "development measurement");

                addKeywords("4.3.2 Factors influencing growth and development", "primary product dependency",
                                "commodity prices",
                                "savings gap", "foreign currency gap", "capital flight", "demographic factors", "debt",
                                "banking access", "infrastructure", "education", "skills", "property rights",
                                "harrod-domar model",
                                "institutions", "geography", "climate", "cultural factors", "political stability",
                                "corruption",
                                "economic factors", "non-economic factors", "volatility of commodity prices",
                                "absence of property rights",
                                "access to credit", "education barriers", "population growth", "debt burden");

                addKeywords("4.3.3 Strategies influencing growth and development", "market-oriented strategies",
                                "trade liberalization",
                                "foreign direct investment", "fdi", "subsidy removal", "floating exchange rate",
                                "microfinance",
                                "privatization", "interventionist strategies", "human capital", "protectionism",
                                "managed exchange rate",
                                "infrastructure development", "joint ventures", "buffer stock schemes",
                                "industrialization", "lewis model",
                                "tourism development", "primary industry development", "fairtrade", "aid",
                                "debt relief",
                                "world bank", "imf", "international monetary fund", "ngos",
                                "non-government organisations",
                                "development strategies", "international institutions", "development approaches");

                // Theme 4.4 - The financial sector
                addKeywords("4.4.1 Role of financial markets", "financial markets", "capital markets", "money markets",
                                "stock markets", "bond markets", "foreign exchange markets", "commodities markets",
                                "savings",
                                "investment", "allocate resources", "liquidity", "risk management", "price discovery",
                                "facilitating exchange", "forward markets", "futures markets", "derivatives",
                                "lending function",
                                "business loans", "individual loans", "goods and services exchange", "equity markets",
                                "share markets");

                addKeywords("4.4.2 Market failure in the financial sector", "financial market failure",
                                "asymmetric information",
                                "moral hazard", "adverse selection", "systemic risk", "too big to fail", "speculation",
                                "market bubbles", "financial crisis", "market rigging", "libor",
                                "credit rating agencies",
                                "principal-agent problem", "short-termism", "financial regulation", "externalities",
                                "financial externalities", "financial speculation", "market manipulation");

                addKeywords("4.4.3 Role of central banks", "central bank", "monetary policy", "lender of last resort",
                                "banker to the government", "banker to the banks", "financial stability", "regulation",
                                "supervision",
                                "payment systems", "bank of england", "federal reserve", "european central bank",
                                "reserve requirements", "interest rates", "open market operations",
                                "quantitative easing", "macroprudential policy",
                                "key functions", "implementation of monetary policy", "financial regulation role");

                // Theme 4.5 - Role of the state in the macroeconomy
                addKeywords("4.5.1 Public expenditure", "public expenditure", "government spending",
                                "capital expenditure",
                                "current expenditure", "transfer payments", "public spending", "government consumption",
                                "government investment", "social security", "welfare spending", "health spending",
                                "education spending",
                                "infrastructure investment", "crowding out", "crowding in", "automatic stabilizers",
                                "productivity",
                                "living standards", "changing size of public expenditure",
                                "composition of public expenditure",
                                "public expenditure as proportion of gdp", "impact on productivity",
                                "impact on taxation", "impact on equality");

                addKeywords("4.5.2 Taxation", "taxation", "direct tax", "indirect tax", "progressive tax",
                                "proportional tax",
                                "regressive tax", "income tax", "corporation tax", "value added tax", "excise duty",
                                "incentives to work",
                                "laffer curve", "tax revenue", "income distribution", "redistributive effect",
                                "employment impact",
                                "price level impact", "trade balance impact", "fdi flows", "tax incidence",
                                "tax burden", "tax avoidance",
                                "tax evasion", "economic effects of taxation", "impact on output",
                                "impact on employment");

                addKeywords("4.5.3 Public sector finances", "public sector finances", "fiscal deficit", "national debt",
                                "budget deficit", "public sector borrowing requirement", "psbr", "government borrowing",
                                "automatic stabilizers", "discretionary fiscal policy", "structural deficit",
                                "cyclical deficit",
                                "debt sustainability", "debt-to-gdp ratio", "debt interest payments", "sovereign debt",
                                "balanced budget", "budget surplus", "fiscal sustainability", "size of fiscal deficits",
                                "size of national debts", "significance of deficits", "factors influencing deficits",
                                "factors influencing national debt");

                addKeywords("4.5.4 Macroeconomic policies in a global context", "macroeconomic policy", "fiscal policy",
                                "monetary policy", "exchange rate policy", "supply-side policy", "direct controls",
                                "deficit reduction", "austerity", "expansionary policy", "contractionary policy",
                                "poverty reduction", "inequality reduction", "international competitiveness",
                                "external shocks", "global recession", "global financial crisis", "policy coordination",
                                "transnational companies", "transfer pricing", "tax havens",
                                "international tax agreements",
                                "information problems", "uncertainty", "time lags", "implementation lags",
                                "recognition lags",
                                "policy application problems", "limits to government control",
                                "global companies control",
                                "regulation of transfer pricing");

        }

        private void initializeConceptRelationships() {
                // Economics fundamentals
                addRelatedConcepts("economics", "1.1.1 Economics as a social science",
                                "1.1.2 Positive and normative economic statements",
                                "1.1.3 The economic problem", "2.1.1 Economic growth");

                addRelatedConcepts("scarcity", "1.1.3 The economic problem", "1.1.4 Production possibility frontiers",
                                "1.2.1 Rational decision making", "4.2.1 Absolute and relative poverty");

                addRelatedConcepts("opportunity cost", "1.1.3 The economic problem",
                                "1.1.4 Production possibility frontiers",
                                "4.1.2 Specialisation and trade", "3.5.2 Supply of labour");

                // Market mechanisms
                addRelatedConcepts("market", "1.2.6 Price determination", "1.2.7 Price mechanism",
                                "1.3.1 Types of market failure",
                                "3.4.2 Perfect competition", "3.4.5 Monopoly",
                                "1.1.6 Free market economies, mixed economy and command economy");

                addRelatedConcepts("price", "1.2.6 Price determination",
                                "1.2.3 Price, income and cross elasticities of demand",
                                "1.2.9 Indirect taxes and subsidies", "2.1.2 Inflation", "1.2.7 Price mechanism");

                addRelatedConcepts("demand", "1.2.2 Demand", "1.2.3 Price, income and cross elasticities of demand",
                                "1.2.6 Price determination", "1.2.8 Consumer and producer surplus",
                                "2.2.1 The characteristics of AD");

                addRelatedConcepts("supply", "1.2.4 Supply", "1.2.5 Elasticity of supply", "1.2.6 Price determination",
                                "1.2.8 Consumer and producer surplus", "2.3.1 The characteristics of AS");

                addRelatedConcepts("elasticity", "1.2.3 Price, income and cross elasticities of demand",
                                "1.2.5 Elasticity of supply",
                                "1.2.9 Indirect taxes and subsidies", "3.5.1 Demand for labour",
                                "3.5.3 Wage determination in competitive and non-competitive markets");

                // Efficiency and welfare
                addRelatedConcepts("efficiency", "1.2.8 Consumer and producer surplus", "1.3.1 Types of market failure",
                                "3.4.1 Efficiency", "3.4.2 Perfect competition", "3.4.5 Monopoly");

                addRelatedConcepts("welfare", "1.2.8 Consumer and producer surplus", "1.3.1 Types of market failure",
                                "1.3.2 Externalities", "4.1.2 Specialisation and trade");

                // Market failure
                addRelatedConcepts("externalities", "1.3.2 Externalities", "1.4.1 Government intervention in markets",
                                "2.5.4 The impact of economic growth", "4.4.2 Market failure in the financial sector");

                addRelatedConcepts("public goods", "1.3.3 Public goods", "1.4.1 Government intervention in markets",
                                "4.5.1 Public expenditure");

                addRelatedConcepts("information", "1.3.4 Information gaps", "1.4.1 Government intervention in markets",
                                "3.4.2 Perfect competition", "4.4.2 Market failure in the financial sector");

                // Government intervention
                addRelatedConcepts("government", "1.4.1 Government intervention in markets", "1.4.2 Government failure",
                                "2.2.4 Government expenditure (G)", "2.6.2 Demand-side policies",
                                "4.5.1 Public expenditure");

                addRelatedConcepts("regulation", "1.4.1 Government intervention in markets",
                                "3.6.1 Government intervention",
                                "4.4.3 Role of central banks", "4.5.4 Macroeconomic policies in a global context");

                addRelatedConcepts("tax", "1.2.9 Indirect taxes and subsidies",
                                "1.4.1 Government intervention in markets",
                                "2.6.2 Demand-side policies", "4.5.2 Taxation");

                addRelatedConcepts("subsidy", "1.2.9 Indirect taxes and subsidies",
                                "1.4.1 Government intervention in markets",
                                "4.1.6 Restrictions on free trade",
                                "4.3.3 Strategies influencing growth and development");

                // Macroeconomic measures
                addRelatedConcepts("gdp", "2.1.1 Economic growth", "2.4.1 National income", "2.5.1 Causes of growth",
                                "4.3.1 Measures of development");

                addRelatedConcepts("growth", "2.1.1 Economic growth", "2.5.1 Causes of growth",
                                "2.5.4 The impact of economic growth",
                                "4.3.2 Factors influencing growth and development",
                                "1.1.4 Production possibility frontiers");

                addRelatedConcepts("inflation", "2.1.2 Inflation", "2.6.1 Possible macroeconomic objectives",
                                "2.6.4 Conflicts and trade-offs between objectives and policies",
                                "4.4.3 Role of central banks");

                addRelatedConcepts("unemployment", "2.1.3 Employment and unemployment",
                                "2.6.1 Possible macroeconomic objectives",
                                "2.6.4 Conflicts and trade-offs between objectives and policies",
                                "3.5.3 Wage determination in competitive and non-competitive markets");

                // Aggregate demand components
                addRelatedConcepts("consumption", "2.2.2 Consumption (C)", "2.2.1 The characteristics of AD",
                                "2.4.4 The multiplier", "1.2.2 Demand");

                addRelatedConcepts("investment", "2.2.3 Investment (I)", "2.2.1 The characteristics of AD",
                                "2.4.2 Injections and withdrawals", "4.4.1 Role of financial markets");

                addRelatedConcepts("exports", "2.2.5 Net trade (X-M)", "2.1.4 Balance of payments",
                                "4.1.2 Specialisation and trade", "4.1.9 International competitiveness");

                // Business economics
                addRelatedConcepts("profit", "3.2.1 Business objectives",
                                "3.3.4 Normal profits, supernormal profits and losses",
                                "3.4.2 Perfect competition", "3.4.5 Monopoly");

                addRelatedConcepts("costs", "3.3.2 Costs", "3.3.3 Economies and diseconomies of scale",
                                "3.4.1 Efficiency", "2.1.2 Inflation");

                addRelatedConcepts("revenue", "3.3.1 Revenue", "3.2.1 Business objectives",
                                "1.2.3 Price, income and cross elasticities of demand", "4.5.2 Taxation");

                // Market structures
                addRelatedConcepts("competition", "3.4.2 Perfect competition", "3.4.3 Monopolistic competition",
                                "3.4.7 Contestability", "3.6.1 Government intervention");

                addRelatedConcepts("monopoly", "3.4.5 Monopoly", "1.3.1 Types of market failure",
                                "3.6.1 Government intervention", "3.4.1 Efficiency");

                addRelatedConcepts("oligopoly", "3.4.4 Oligopoly", "3.6.1 Government intervention",
                                "3.1.2 Business growth");

                // Labour markets
                addRelatedConcepts("labour", "3.5.1 Demand for labour", "3.5.2 Supply of labour",
                                "3.5.3 Wage determination in competitive and non-competitive markets",
                                "1.1.5 Specialisation and the division of labour");

                addRelatedConcepts("wages", "3.5.3 Wage determination in competitive and non-competitive markets",
                                "2.1.2 Inflation", "2.1.3 Employment and unemployment", "3.5.1 Demand for labour");

                addRelatedConcepts("productivity", "1.1.5 Specialisation and the division of labour",
                                "3.5.1 Demand for labour",
                                "2.5.1 Causes of growth", "4.1.9 International competitiveness");

                // International economics
                addRelatedConcepts("trade", "4.1.2 Specialisation and trade", "4.1.3 Pattern of trade",
                                "4.1.6 Restrictions on free trade", "2.2.5 Net trade (X-M)",
                                "1.1.5 Specialisation and the division of labour");

                addRelatedConcepts("globalisation", "4.1.1 Globalisation", "4.1.3 Pattern of trade",
                                "3.1.1 Sizes and types of firms", "4.3.2 Factors influencing growth and development");

                addRelatedConcepts("comparative advantage", "4.1.2 Specialisation and trade",
                                "1.1.5 Specialisation and the division of labour",
                                "4.1.3 Pattern of trade", "1.1.3 The economic problem");

                addRelatedConcepts("exchange rate", "4.1.8 Exchange rates", "2.2.5 Net trade (X-M)",
                                "4.1.9 International competitiveness", "4.1.7 Balance of payments");

                addRelatedConcepts("balance of payments", "2.1.4 Balance of payments", "4.1.7 Balance of payments",
                                "2.2.5 Net trade (X-M)", "4.1.8 Exchange rates");

                // Development economics
                addRelatedConcepts("development", "4.3.1 Measures of development",
                                "4.3.2 Factors influencing growth and development",
                                "4.3.3 Strategies influencing growth and development", "2.1.1 Economic growth");

                addRelatedConcepts("poverty", "4.2.1 Absolute and relative poverty",
                                "4.3.2 Factors influencing growth and development",
                                "4.2.2 Inequality", "1.1.3 The economic problem");

                addRelatedConcepts("inequality", "4.2.2 Inequality", "4.2.1 Absolute and relative poverty",
                                "4.5.2 Taxation", "2.6.1 Possible macroeconomic objectives");

                // Financial sector
                addRelatedConcepts("financial markets", "4.4.1 Role of financial markets",
                                "4.4.2 Market failure in the financial sector",
                                "2.2.3 Investment (I)", "4.3.2 Factors influencing growth and development");

                addRelatedConcepts("central bank", "4.4.3 Role of central banks", "2.6.2 Demand-side policies",
                                "2.1.2 Inflation", "4.5.4 Macroeconomic policies in a global context");

                addRelatedConcepts("monetary policy", "2.6.2 Demand-side policies", "4.4.3 Role of central banks",
                                "2.1.2 Inflation", "4.5.4 Macroeconomic policies in a global context");

                // Fiscal policy
                addRelatedConcepts("fiscal policy", "2.6.2 Demand-side policies", "4.5.1 Public expenditure",
                                "4.5.2 Taxation", "4.5.3 Public sector finances");

                addRelatedConcepts("public expenditure", "4.5.1 Public expenditure", "2.2.4 Government expenditure (G)",
                                "4.5.3 Public sector finances", "1.3.3 Public goods");

                addRelatedConcepts("budget", "4.5.3 Public sector finances", "2.6.2 Demand-side policies",
                                "4.5.1 Public expenditure", "4.5.2 Taxation");

                // Multiplier and circular flow
                addRelatedConcepts("multiplier", "2.4.4 The multiplier", "2.2.2 Consumption (C)",
                                "2.6.2 Demand-side policies", "2.4.2 Injections and withdrawals");

                addRelatedConcepts("circular flow", "2.4.1 National income", "2.4.2 Injections and withdrawals",
                                "2.4.4 The multiplier");

                // Cross-cutting policy concepts
                addRelatedConcepts("policy", "2.6.2 Demand-side policies", "2.6.3 Supply-side policies",
                                "4.5.4 Macroeconomic policies in a global context",
                                "1.4.1 Government intervention in markets");

                addRelatedConcepts("objectives", "2.6.1 Possible macroeconomic objectives", "3.2.1 Business objectives",
                                "2.6.4 Conflicts and trade-offs between objectives and policies",
                                "4.5.4 Macroeconomic policies in a global context");

                addRelatedConcepts("trade-offs", "2.6.4 Conflicts and trade-offs between objectives and policies",
                                "1.1.3 The economic problem", "1.1.4 Production possibility frontiers");

                // Innovation and technology
                addRelatedConcepts("technology", "2.5.1 Causes of growth", "4.1.1 Globalisation",
                                "3.3.3 Economies and diseconomies of scale",
                                "4.3.2 Factors influencing growth and development");

                // Environmental connections
                addRelatedConcepts("environment", "2.5.4 The impact of economic growth",
                                "2.6.1 Possible macroeconomic objectives",
                                "1.3.2 Externalities", "4.3.2 Factors influencing growth and development");

                // Rationality and behaviour
                addRelatedConcepts("rational", "1.2.1 Rational decision making",
                                "1.2.10 Alternative views of consumer behaviour",
                                "3.2.1 Business objectives");

                addRelatedConcepts("behavioural", "1.2.10 Alternative views of consumer behaviour",
                                "4.4.2 Market failure in the financial sector");

                // International institutions
                addRelatedConcepts("wto", "4.1.5 Trading blocs and the World Trade Organisation (WTO)",
                                "4.1.6 Restrictions on free trade",
                                "4.3.3 Strategies influencing growth and development");

                addRelatedConcepts("trading blocs", "4.1.5 Trading blocs and the World Trade Organisation (WTO)",
                                "4.1.3 Pattern of trade",
                                "4.1.6 Restrictions on free trade");

                // Business growth and structure
                addRelatedConcepts("merger", "3.1.2 Business growth", "3.6.1 Government intervention",
                                "3.4.4 Oligopoly");

                addRelatedConcepts("economies of scale", "3.3.3 Economies and diseconomies of scale", "3.4.5 Monopoly",
                                "3.1.2 Business growth", "4.1.1 Globalisation");
        }

        private void addKeywords(String topic, String... keywords) {
                topicKeywords.put(topic, Arrays.asList(keywords));
        }

        private void addRelatedConcepts(String concept, String... relatedTopics) {
                conceptRelationships.put(concept, Arrays.asList(relatedTopics));
        }
}