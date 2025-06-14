package com.ppgenarator.ai;

import java.util.ArrayList;
import java.util.List;

public class TopicConstants {

    public static final int BATCH_SIZE = 5;  // Increased to 5 for better context
    public static final int MAX_TOPICS_PER_QUESTION = 5;  // Reduced to 3 for more focused topic assignment
    public static final double KEYWORD_THRESHOLD = 1;  // Increased to 1.5 for stronger primary topic matches 
    public static final double SECONDARY_TOPIC_THRESHOLD = 1;  // Increased to 0.6 for more relevant secondary topics

    // A-level Economics specification topics (comprehensive list)
    public static final String[] DEFAULT_TOPICS = {
        // Theme 1 topics
        "1.1.1 Economics as a social science",
        "1.1.2 Positive and normative economic statements",
        "1.1.3 The economic problem",
        "1.1.4 Production possibility frontiers",
        "1.1.5 Specialisation and the division of labour",
        "1.1.6 Free market economies, mixed economy and command economy",
        "1.2.1 Rational decision making",
        "1.2.2 Demand",
        "1.2.3 Price, income and cross elasticities of demand",
        "1.2.4 Supply",
        "1.2.5 Elasticity of supply",
        "1.2.6 Price determination",
        "1.2.7 Price mechanism",
        "1.2.8 Consumer and producer surplus",
        "1.2.9 Indirect taxes and subsidies",
        "1.2.10 Alternative views of consumer behaviour",
        "1.3.1 Types of market failure",
        "1.3.2 Externalities",
        "1.3.3 Public goods",
        "1.3.4 Information gaps",
        "1.4.1 Government intervention in markets",
        "1.4.2 Government failure",
        // Theme 2 topics
        "2.1.1 Economic growth",
        "2.1.2 Inflation",
        "2.1.3 Employment and unemployment",
        "2.1.4 Balance of payments",
        "2.2.1 The characteristics of AD",
        "2.2.2 Consumption (C)",
        "2.2.3 Investment (I)",
        "2.2.4 Government expenditure (G)",
        "2.2.5 Net trade (X-M)",
        "2.3.1 The characteristics of AS",
        "2.3.2 Short-run AS",
        "2.3.3 Long-run AS",
        "2.4.1 National income",
        "2.4.2 Injections and withdrawals",
        "2.4.3 Equilibrium levels of real national output",
        "2.4.4 The multiplier",
        "2.5.1 Causes of growth",
        "2.5.2 Output gaps",
        "2.5.3 Trade (business) cycle",
        "2.5.4 The impact of economic growth",
        "2.6.1 Possible macroeconomic objectives",
        "2.6.2 Demand-side policies",
        "2.6.3 Supply-side policies",
        "2.6.4 Conflicts and trade-offs between objectives and policies",
        // Theme 3 topics
        "3.1.1 Sizes and types of firms",
        "3.1.2 Business growth",
        "3.1.3 Demergers",
        "3.2.1 Business objectives",
        "3.3.1 Revenue",
        "3.3.2 Costs",
        "3.3.3 Economies and diseconomies of scale",
        "3.3.4 Normal profits, supernormal profits and losses",
        "3.4.1 Efficiency",
        "3.4.2 Perfect competition",
        "3.4.3 Monopolistic competition",
        "3.4.4 Oligopoly",
        "3.4.5 Monopoly",
        "3.4.6 Monopsony",
        "3.4.7 Contestability",
        "3.5.1 Demand for labour",
        "3.5.2 Supply of labour",
        "3.5.3 Wage determination in competitive and non-competitive markets",
        "3.6.1 Government intervention",
        "3.6.2 The impact of government intervention",
        // Theme 4 topics
        "4.1.1 Globalisation",
        "4.1.2 Specialisation and trade",
        "4.1.3 Pattern of trade",
        "4.1.4 Terms of trade",
        "4.1.5 Trading blocs and the World Trade Organisation (WTO)",
        "4.1.6 Restrictions on free trade",
        "4.1.7 Balance of payments",
        "4.1.8 Exchange rates",
        "4.1.9 International competitiveness",
        "4.2.1 Absolute and relative poverty",
        "4.2.2 Inequality",
        "4.3.1 Measures of development",
        "4.3.2 Factors influencing growth and development",
        "4.3.3 Strategies influencing growth and development",
        "4.4.1 Role of financial markets",
        "4.4.2 Market failure in the financial sector",
        "4.4.3 Role of central banks",
        "4.5.1 Public expenditure",
        "4.5.2 Taxation",
        "4.5.3 Public sector finances",
        "4.5.4 Macroeconomic policies in a global context"
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

    // Helper methods to extract hierarchy information
    public static String getThemeFromTopic(String topic) {
        if (topic.startsWith("1.")) {
            return "Theme 1";
        }
        if (topic.startsWith("2.")) {
            return "Theme 2";
        }
        if (topic.startsWith("3.")) {
            return "Theme 3";
        }
        if (topic.startsWith("4.")) {
            return "Theme 4";
        }
        return "Other";
    }

    public static String getMajorTopicFromSubTopic(String topic) {
        if (topic.matches("1\\.1\\.\\d+.*")) {
            return "1.1 Nature of economics";
        }
        if (topic.matches("1\\.2\\.\\d+.*")) {
            return "1.2 How markets work";
        }
        if (topic.matches("1\\.3\\.\\d+.*")) {
            return "1.3 Market failure";
        }
        if (topic.matches("1\\.4\\.\\d+.*")) {
            return "1.4 Government intervention";
        }

        if (topic.matches("2\\.1\\.\\d+.*")) {
            return "2.1 Measures of economic performance";
        }
        if (topic.matches("2\\.2\\.\\d+.*")) {
            return "2.2 Aggregate demand (AD)";
        }
        if (topic.matches("2\\.3\\.\\d+.*")) {
            return "2.3 Aggregate supply (AS)";
        }
        if (topic.matches("2\\.4\\.\\d+.*")) {
            return "2.4 National income";
        }
        if (topic.matches("2\\.5\\.\\d+.*")) {
            return "2.5 Economic growth";
        }
        if (topic.matches("2\\.6\\.\\d+.*")) {
            return "2.6 Macroeconomic objectives and policies";
        }

        if (topic.matches("3\\.1\\.\\d+.*")) {
            return "3.1 Business growth";
        }
        if (topic.matches("3\\.2\\.\\d+.*")) {
            return "3.2 Business objectives";
        }
        if (topic.matches("3\\.3\\.\\d+.*")) {
            return "3.3 Revenues, costs and profits";
        }
        if (topic.matches("3\\.4\\.\\d+.*")) {
            return "3.4 Market structures";
        }
        if (topic.matches("3\\.5\\.\\d+.*")) {
            return "3.5 Labour market";
        }
        if (topic.matches("3\\.6\\.\\d+.*")) {
            return "3.6 Government intervention";
        }

        if (topic.matches("4\\.1\\.\\d+.*")) {
            return "4.1 International economics";
        }
        if (topic.matches("4\\.2\\.\\d+.*")) {
            return "4.2 Poverty and inequality";
        }
        if (topic.matches("4\\.3\\.\\d+.*")) {
            return "4.3 Emerging and developing economies";
        }
        if (topic.matches("4\\.4\\.\\d+.*")) {
            return "4.4 The financial sector";
        }
        if (topic.matches("4\\.5\\.\\d+.*")) {
            return "4.5 Role of the state in the macroeconomy";
        }

        return "Other";
    }

    public static String getSubTopicCode(String topic) {
        if (topic.matches("\\d+\\.\\d+\\.\\d+.*")) {
            return topic.substring(0, topic.indexOf(' ') > 0 ? topic.indexOf(' ') : topic.length());
        }
        return topic;
    }

    public static String getSubTopicName(String topic) {
        if (topic.matches("\\d+\\.\\d+\\.\\d+.*")) {
            int spaceIndex = topic.indexOf(' ');
            return spaceIndex > 0 ? topic.substring(spaceIndex + 1) : topic;
        }
        return topic;
    }

    public static boolean isThemeValidForQualification(String theme, String qualification) {
        switch (qualification.toLowerCase()) {
            case "as level":
            case "as":
                // AS Level only covers Themes 1 and 2
                return "Theme 1".equals(theme) || "Theme 2".equals(theme);
            case "a level":
            case "alevel":
                // A Level covers Themes 3 and 4
                return "Theme 3".equals(theme) || "Theme 4".equals(theme);
            default:
                // For unknown qualifications, include all themes
                return true;
        }
    }

    // Method to get all sub-topics for a theme
    public static String[] getSubTopicsForTheme(int themeNumber) {
        List<String> subTopics = new ArrayList<>();
        String themePrefix = themeNumber + ".";

        for (String topic : DEFAULT_TOPICS) {
            if (topic.startsWith(themePrefix)) {
                subTopics.add(topic);
            }
        }

        return subTopics.toArray(new String[0]);
    }
}
