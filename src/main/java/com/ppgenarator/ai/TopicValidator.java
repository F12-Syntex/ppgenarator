package com.ppgenarator.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicValidator {
    private final String[] validTopics;
    private final TopicKeywordManager keywordManager;
    
    // Track over-used topics for better balance
    private static final Set<String> COMMONLY_OVERUSED_TOPICS = new HashSet<>();
    static {
        COMMONLY_OVERUSED_TOPICS.add("1.1.3 The economic problem");
        COMMONLY_OVERUSED_TOPICS.add("1.2.6 Price determination");
        COMMONLY_OVERUSED_TOPICS.add("1.2.2 Demand");
    }

    public TopicValidator(String[] validTopics, TopicKeywordManager keywordManager) {
        this.validTopics = validTopics;
        this.keywordManager = keywordManager;
    }

    /**
     * Validate and limit topics with improved consistency and quality control
     */
    public String[] validateAndLimitTopics(String[] suggestedTopics, String questionText) {
        if (suggestedTopics == null) {
            return new String[] { determineFallbackTopic(questionText) };
        }

        // Extract question characteristics for better validation
        int estimatedMarks = estimateMarksFromText(questionText);
        boolean isCalculationQuestion = TopicConstants.TopicAssignmentRules.isComputationalQuestion(questionText);
        boolean requiresDiagram = TopicConstants.TopicAssignmentRules.requiresDiagramAnalysis(questionText);

        // Determine appropriate number of topics
        int maxTopics = TopicConstants.TopicAssignmentRules.getMaxTopicsForQuestion(estimatedMarks, questionText);

        System.out.println("Question analysis - Estimated marks: " + estimatedMarks + 
                          ", Calculation: " + isCalculationQuestion + 
                          ", Diagram: " + requiresDiagram + 
                          ", Max topics: " + maxTopics);

        // First validate all topics
        List<String> validatedTopics = new ArrayList<>();
        for (String suggestedTopic : suggestedTopics) {
            if (suggestedTopic == null || suggestedTopic.trim().isEmpty()) {
                continue;
            }

            String validatedTopic = validateSingleTopic(suggestedTopic);
            if (validatedTopic != null && !validatedTopics.contains(validatedTopic)) {
                validatedTopics.add(validatedTopic);
                System.out.println("Validated topic: '" + suggestedTopic + "' → '" + validatedTopic + "'");
            }
        }

        // If no valid topics, return fallback
        if (validatedTopics.isEmpty()) {
            return new String[] { determineFallbackTopic(questionText) };
        }

        // Apply quality filters
        List<String> filteredTopics = applyQualityFilters(validatedTopics, questionText, isCalculationQuestion);

        // Score and rank topics by relevance
        Map<String, Double> topicScores = scoreTopicRelevance(filteredTopics, questionText, estimatedMarks);

        // Select final topics based on scores and limits
        List<String> finalTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxTopics)
                .filter(entry -> entry.getValue() > getMinimumScoreThreshold(isCalculationQuestion))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Ensure we have at least one high-quality topic
        if (finalTopics.isEmpty() && !filteredTopics.isEmpty()) {
            finalTopics.add(filteredTopics.get(0));
        }

        // Final validation and consistency check
        String[] result = finalTopics.toArray(new String[0]);
        result = ensureTopicConsistency(result, questionText);

        System.out.println("Final topic assignment: " + String.join(", ", result));
        return result;
    }

    /**
     * Apply quality filters to remove inappropriate topics
     */
    private List<String> applyQualityFilters(List<String> topics, String questionText, boolean isCalculationQuestion) {
        List<String> filtered = new ArrayList<>();
        String lowerText = questionText.toLowerCase();

        for (String topic : topics) {
            boolean shouldInclude = true;

            // Filter "The economic problem" unless explicitly relevant
            if (topic.equals("1.1.3 The economic problem")) {
                shouldInclude = TopicConstants.TopicAssignmentRules.shouldIncludeEconomicProblem(questionText, 5);
                if (!shouldInclude) {
                    System.out.println("Filtered out '1.1.3 The economic problem' - not explicitly relevant");
                }
            }

            // Filter out vague connections for calculation questions
            if (isCalculationQuestion && !isDirectlyRelevantToCalculation(topic, lowerText)) {
                shouldInclude = false;
                System.out.println("Filtered out '" + topic + "' - not directly relevant to calculation");
            }

            // Filter out government intervention unless explicitly mentioned
            if (topic.contains("Government intervention") && 
                !lowerText.contains("government") && !lowerText.contains("policy") && 
                !lowerText.contains("regulation") && !lowerText.contains("intervention")) {
                shouldInclude = false;
                System.out.println("Filtered out government intervention topic - not explicitly mentioned");
            }

            // Filter out market failure unless explicitly mentioned
            if (topic.contains("market failure") && 
                !lowerText.contains("market failure") && !lowerText.contains("externality") && 
                !lowerText.contains("public good") && !lowerText.contains("information gap")) {
                shouldInclude = false;
                System.out.println("Filtered out market failure topic - not explicitly mentioned");
            }

            if (shouldInclude) {
                filtered.add(topic);
            }
        }

        return filtered;
    }

    /**
     * Check if topic is directly relevant to calculation questions
     */
    private boolean isDirectlyRelevantToCalculation(String topic, String lowerText) {
        // For calculation questions, be very strict about relevance
        if (lowerText.contains("elasticity") && topic.contains("elasticities")) return true;
        if (lowerText.contains("revenue") && topic.contains("Revenue")) return true;
        if (lowerText.contains("cost") && topic.contains("Costs")) return true;
        if (lowerText.contains("profit") && topic.contains("profit")) return true;
        if (lowerText.contains("tax") && topic.contains("taxes")) return true;
        if (lowerText.contains("gdp") && topic.contains("Economic growth")) return true;
        if (lowerText.contains("unemployment") && topic.contains("unemployment")) return true;
        if (lowerText.contains("inflation") && topic.contains("Inflation")) return true;
        
        // Supply and demand are often relevant to calculations
        if ((lowerText.contains("supply") || lowerText.contains("demand")) && 
            (topic.contains("Supply") || topic.contains("Demand"))) return true;
            
        return false;
    }

    /**
     * Score topics based on relevance with improved algorithms
     */
    private Map<String, Double> scoreTopicRelevance(List<String> topics, String questionText, int estimatedMarks) {
        Map<String, Double> scores = new HashMap<>();
        String lowerText = questionText.toLowerCase();

        for (String topic : topics) {
            double score = 0.0;

            // Base score for topic validation
            score += 1.0;

            // Direct mention gets highest score
            String topicLower = topic.toLowerCase();
            if (lowerText.contains(topicLower)) {
                score += 10.0;
            }

            // Check for key words from topic title
            String[] topicWords = extractKeyWordsFromTopic(topic);
            int matchingWords = 0;
            for (String word : topicWords) {
                if (word.length() > 3 && lowerText.contains(word.toLowerCase())) {
                    matchingWords++;
                    score += 2.0;
                }
            }

            // Bonus for multiple word matches
            if (matchingWords > 1) {
                score += 3.0;
            }

            // Check keyword relevance using keyword manager
            score += getKeywordRelevanceScore(topic, lowerText);

            // Penalty for commonly overused topics unless strongly relevant
            if (COMMONLY_OVERUSED_TOPICS.contains(topic) && score < 8.0) {
                score *= 0.7;
                System.out.println("Applied penalty to commonly overused topic: " + topic);
            }

            // Bonus for topics that match question complexity
            if (estimatedMarks >= 10 && isComplexTopic(topic)) {
                score += 1.5;
            }

            scores.put(topic, score);
        }

        return scores;
    }

    /**
     * Extract key words from topic title for matching
     */
    private String[] extractKeyWordsFromTopic(String topic) {
        // Extract the topic name part (after the code)
        String topicName = topic.replaceFirst("^\\d+\\.\\d+\\.\\d+\\s+", "");
        
        // Split and filter meaningful words
        return topicName.toLowerCase()
                .replaceAll("[(),]", "")
                .split("\\s+");
    }

    /**
     * Get keyword relevance score using keyword manager
     */
    private double getKeywordRelevanceScore(String topic, String questionText) {
        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
        
        if (!topicKeywords.containsKey(topic)) {
            return 0.0;
        }

        double score = 0.0;
        List<String> keywords = topicKeywords.get(topic);
        
        for (String keyword : keywords) {
            if (containsKeywordReasonably(questionText, keyword.toLowerCase())) {
                score += 1.0;
            }
        }

        return Math.min(score, 5.0); // Cap the keyword score
    }

    /**
     * More reasonable keyword matching (less aggressive than before)
     */
    private boolean containsKeywordReasonably(String text, String keyword) {
        return text.contains(" " + keyword + " ") ||
               text.startsWith(keyword + " ") ||
               text.endsWith(" " + keyword) ||
               text.contains(" " + keyword + ",") ||
               text.contains(" " + keyword + ".") ||
               text.contains("(" + keyword + ")") ||
               (keyword.length() > 5 && text.contains(keyword));
    }

    /**
     * Check if topic is considered complex/advanced
     */
    private boolean isComplexTopic(String topic) {
        return topic.contains("Oligopoly") ||
               topic.contains("Monopoly") ||
               topic.contains("game theory") ||
               topic.contains("macroeconomic") ||
               topic.contains("policies") ||
               topic.contains("development") ||
               topic.contains("globalisation");
    }

    /**
     * Get minimum score threshold based on question type
     */
    private double getMinimumScoreThreshold(boolean isCalculationQuestion) {
        return isCalculationQuestion ? 3.0 : 2.0;
    }

    /**
     * Ensure topic consistency and logical combinations
     */
    private String[] ensureTopicConsistency(String[] topics, String questionText) {
        List<String> consistent = new ArrayList<>();
        String lowerText = questionText.toLowerCase();

        // Add topics in order of logical dependency
        // 1. First add fundamental concepts
        for (String topic : topics) {
            if (isFundamentalTopic(topic)) {
                consistent.add(topic);
            }
        }

        // 2. Then add specific application topics
        for (String topic : topics) {
            if (!isFundamentalTopic(topic) && !consistent.contains(topic)) {
                consistent.add(topic);
            }
        }

        // 3. Apply logical consistency rules
        return applyConsistencyRules(consistent.toArray(new String[0]), lowerText);
    }

    /**
     * Check if topic is fundamental/foundational
     */
    private boolean isFundamentalTopic(String topic) {
        return topic.contains("Demand") ||
               topic.contains("Supply") ||
               topic.contains("Price determination") ||
               topic.contains("Economics as a social science");
    }

    /**
     * Apply consistency rules to prevent illogical combinations
     */
    private String[] applyConsistencyRules(String[] topics, String lowerText) {
        List<String> finalTopics = new ArrayList<>();
        
        for (String topic : topics) {
            boolean shouldInclude = true;
            
            // If we have specific elasticity topic, don't need general demand
            if (topic.equals("1.2.2 Demand") && hasSpecificDemandTopic(topics)) {
                shouldInclude = false;
            }
            
            // If we have specific supply topic, don't need general supply in some cases
            if (topic.equals("1.2.4 Supply") && hasSpecificSupplyTopic(topics) && topics.length > 2) {
                shouldInclude = false;
            }
            
            if (shouldInclude) {
                finalTopics.add(topic);
            }
        }
        
        return finalTopics.toArray(new String[0]);
    }

    /**
     * Check if we have more specific demand-related topics
     */
    private boolean hasSpecificDemandTopic(String[] topics) {
        for (String topic : topics) {
            if (topic.contains("elasticities of demand") || 
                topic.contains("Consumer and producer surplus")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if we have more specific supply-related topics
     */
    private boolean hasSpecificSupplyTopic(String[] topics) {
        for (String topic : topics) {
            if (topic.contains("Elasticity of supply")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimate marks from question text for better validation
     */
    private int estimateMarksFromText(String questionText) {
        // Look for explicit mark indicators
        if (questionText.contains("(1)") || questionText.contains("1 mark")) return 1;
        if (questionText.contains("(2)") || questionText.contains("2 marks")) return 2;
        if (questionText.contains("(3)") || questionText.contains("3 marks")) return 3;
        if (questionText.contains("(4)") || questionText.contains("4 marks")) return 4;
        if (questionText.contains("(5)") || questionText.contains("5 marks")) return 5;
        if (questionText.contains("(8)") || questionText.contains("8 marks")) return 8;
        if (questionText.contains("(10)") || questionText.contains("10 marks")) return 10;
        if (questionText.contains("(12)") || questionText.contains("12 marks")) return 12;
        if (questionText.contains("(15)") || questionText.contains("15 marks")) return 15;
        if (questionText.contains("(20)") || questionText.contains("20 marks")) return 20;
        if (questionText.contains("(25)") || questionText.contains("25 marks")) return 25;

        // Estimate based on question type
        if (TopicConstants.TopicAssignmentRules.isComputationalQuestion(questionText)) {
            return 3; // Most calculations are 2-4 marks
        }
        if (questionText.toLowerCase().contains("explain one")) {
            return 3;
        }
        if (questionText.toLowerCase().contains("discuss") || 
            questionText.toLowerCase().contains("evaluate") ||
            questionText.toLowerCase().contains("assess")) {
            return 12; // Essay questions
        }

        return 5; // Default estimate
    }

    /**
     * Validate a single topic with stricter matching
     */
    public String validateSingleTopic(String topicResponse) {
        if (topicResponse == null) {
            return null;
        }

        // Clean up the response
        String cleanedTopic = topicResponse.trim()
                .replaceAll("[\"'.-]", "")
                .replaceAll("(?i)Topic:?\\s*", "")
                .replaceAll("(?i)The topic is:?\\s*", "")
                .replaceAll("(?i)The main topic is:?\\s*", "")
                .replaceAll("(?i)The primary topic is:?\\s*", "");

        // Check for exact match first
        for (String validTopic : validTopics) {
            if (cleanedTopic.equalsIgnoreCase(validTopic)) {
                return validTopic;
            }
        }

        // Try more lenient matching but still strict
        for (String validTopic : validTopics) {
            // Must match the topic code exactly
            String validCode = validTopic.split(" ")[0]; // e.g., "1.2.3"
            if (cleanedTopic.startsWith(validCode) && cleanedTopic.length() > validCode.length()) {
                return validTopic;
            }

            // Check for substantial word overlap
            if (hasSubstantialWordOverlap(cleanedTopic, validTopic)) {
                return validTopic;
            }
        }

        return null; // No match found
    }

    /**
     * Check for substantial word overlap between topics
     */
    private boolean hasSubstantialWordOverlap(String suggested, String valid) {
        String[] suggestedWords = suggested.toLowerCase().split("\\s+");
        String[] validWords = valid.toLowerCase().split("\\s+");
        
        int matches = 0;
        int significantWords = 0;
        
        for (String validWord : validWords) {
            if (validWord.length() > 3) { // Skip short words like "and", "of"
                significantWords++;
                for (String suggestedWord : suggestedWords) {
                    if (validWord.equals(suggestedWord)) {
                        matches++;
                        break;
                    }
                }
            }
        }
        
        // Need at least 50% overlap of significant words
        return significantWords > 0 && (double) matches / significantWords >= 0.5;
    }

    /**
     * Determine a fallback topic with improved logic
     */
    public String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "1.2.2 Demand"; // Safe default
        }

        String lowerText = questionText.toLowerCase();

        // More specific fallback logic
        if (lowerText.contains("calculate") || lowerText.contains("work out")) {
            if (lowerText.contains("elasticity")) return "1.2.3 Price, income and cross elasticities of demand";
            if (lowerText.contains("revenue") || lowerText.contains("profit")) return "3.3.1 Revenue";
            if (lowerText.contains("cost")) return "3.3.2 Costs";
            if (lowerText.contains("gdp")) return "2.1.1 Economic growth";
            if (lowerText.contains("unemployment")) return "2.1.3 Employment and unemployment";
        }

        if (lowerText.contains("draw") || lowerText.contains("diagram")) {
            if (lowerText.contains("supply") && lowerText.contains("demand")) return "1.2.6 Price determination";
            if (lowerText.contains("market")) return "1.2.6 Price determination";
        }

        // Check for key economic terms
        if (lowerText.contains("supply") && !lowerText.contains("demand")) return "1.2.4 Supply";
        if (lowerText.contains("price") && lowerText.contains("determine")) return "1.2.6 Price determination";
        if (lowerText.contains("elasticity")) return "1.2.3 Price, income and cross elasticities of demand";
        if (lowerText.contains("government") && lowerText.contains("intervention")) return "1.4.1 Government intervention in markets";
        if (lowerText.contains("market failure")) return "1.3.1 Types of market failure";
        if (lowerText.contains("externality")) return "1.3.2 Externalities";
        if (lowerText.contains("monopoly")) return "3.4.5 Monopoly";
        if (lowerText.contains("competition")) return "3.4.2 Perfect competition";
        if (lowerText.contains("unemployment")) return "2.1.3 Employment and unemployment";
        if (lowerText.contains("inflation")) return "2.1.2 Inflation";
        if (lowerText.contains("growth")) return "2.1.1 Economic growth";

        // Default to demand as most common topic
        return "1.2.2 Demand";
    }
}