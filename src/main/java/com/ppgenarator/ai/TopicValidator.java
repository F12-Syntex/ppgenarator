package com.ppgenarator.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopicValidator {
    private final String[] validTopics;
    private final TopicKeywordManager keywordManager;

    public TopicValidator(String[] validTopics, TopicKeywordManager keywordManager) {
        this.validTopics = validTopics;
        this.keywordManager = keywordManager;
    }

    /**
     * Validate and limit topics with very inclusive criteria
     */
    public String[] validateAndLimitTopics(String[] suggestedTopics, String questionText) {
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

        // Score topics based on relevance to question with very lenient thresholds
        Map<String, Double> topicRelevanceScores = scoreTopicRelevance(validTopics, questionText);

        // Very inclusive filtering - much lower threshold for inclusion
        List<String> finalTopics = topicRelevanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
                .filter(entry -> entry.getValue() > 0.1) // Reduced from 0.3 to 0.1 (very inclusive)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // If we still have room and there are more valid topics, include them all
        if (finalTopics.size() < TopicConstants.MAX_TOPICS_PER_QUESTION && validTopics.size() > finalTopics.size()) {
            for (String topic : validTopics) {
                if (!finalTopics.contains(topic) && finalTopics.size() < TopicConstants.MAX_TOPICS_PER_QUESTION) {
                    finalTopics.add(topic);
                    System.out.println("Added additional topic for comprehensive coverage: " + topic);
                }
            }
        }

        // If we still don't have enough topics, try to find more related ones
        if (finalTopics.size() < 2 && questionText != null) {
            List<String> additionalTopics = findAdditionalRelevantTopics(questionText, finalTopics);
            for (String additionalTopic : additionalTopics) {
                if (finalTopics.size() < TopicConstants.MAX_TOPICS_PER_QUESTION) {
                    finalTopics.add(additionalTopic);
                    System.out.println("Added related topic for better coverage: " + additionalTopic);
                }
            }
        }

        // Ensure we have at least one topic
        if (finalTopics.isEmpty()) {
            return new String[] { validTopics.get(0) };
        }

        return finalTopics.toArray(new String[0]);
    }

    /**
     * Find additional relevant topics by analyzing the question text more broadly
     */
    private List<String> findAdditionalRelevantTopics(String questionText, List<String> existingTopics) {
        List<String> additionalTopics = new ArrayList<>();
        String lowerQuestionText = questionText.toLowerCase();

        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();

        for (String topic : validTopics) {
            if (existingTopics.contains(topic)) {
                continue; // Skip already included topics
            }

            double relevanceScore = 0;

            // Check for keyword matches with very lenient criteria
            if (topicKeywords.containsKey(topic)) {
                List<String> keywords = topicKeywords.get(topic);
                for (String keyword : keywords) {
                    if (containsKeywordVeryFlexibly(lowerQuestionText, keyword.toLowerCase())) {
                        relevanceScore += 0.5; // Lower weight for broad inclusion
                    }
                }
            }

            // Include if there's any reasonable connection
            if (relevanceScore > 0.3) { // Very low threshold
                additionalTopics.add(topic);
            }
        }

        return additionalTopics;
    }

    /**
     * Score topics based on their relevance to the question text with very lenient scoring
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

            // Check for any part of the topic title
            String[] topicWords = topic.toLowerCase().split("\\s+");
            for (String word : topicWords) {
                if (word.length() > 3 && lowerQuestionText.contains(word)) {
                    score += 1.0;
                }
            }

            // Check for keyword relevance with very lenient matching
            Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
            if (topicKeywords.containsKey(topic)) {
                List<String> keywords = topicKeywords.get(topic);
                int matchingKeywords = 0;
                for (String keyword : keywords) {
                    if (containsKeywordVeryFlexibly(lowerQuestionText, keyword.toLowerCase())) {
                        matchingKeywords++;
                        // Give points for any keyword match
                        score += 0.5;
                    }
                }

                // Bonus for multiple keyword matches (but lower threshold)
                if (matchingKeywords > 0) {
                    score += 0.5;
                }
                if (matchingKeywords > 2) {
                    score += 1.0;
                }
            }

            // Check for related concept connections with broad interpretation
            Map<String, List<String>> conceptRelationships = keywordManager.getConceptRelationships();
            for (Map.Entry<String, List<String>> conceptEntry : conceptRelationships.entrySet()) {
                String concept = conceptEntry.getKey();
                if (lowerQuestionText.contains(concept.toLowerCase()) && 
                    conceptEntry.getValue().contains(topic)) {
                    score += 1.0; // Bonus for related concepts
                }
            }

            scores.put(topic, score);
        }

        return scores;
    }

    /**
     * Very flexible keyword matching with multiple variations
     */
    private boolean containsKeywordVeryFlexibly(String text, String keyword) {
        // Original flexible matching
        boolean basicMatch = text.contains(" " + keyword + " ") ||
               text.startsWith(keyword + " ") ||
               text.endsWith(" " + keyword) ||
               text.contains(" " + keyword + ",") ||
               text.contains(" " + keyword + ".") ||
               text.contains(" " + keyword + ";") ||
               text.contains("(" + keyword + ")") ||
               text.contains(keyword + "'s") ||
               text.contains(keyword + "s ") || // Plural forms
               text.contains(keyword + "ing") || // Gerund forms
               text.contains(keyword + "ed") || // Past tense forms
               text.contains(keyword + "-") || // Hyphenated forms
               text.contains("-" + keyword); // Reverse hyphenated forms

        if (basicMatch) return true;

        // Additional very flexible matching
        // Partial word matching for longer keywords
        if (keyword.length() > 6) {
            String[] keywordParts = keyword.split("\\s+");
            for (String part : keywordParts) {
                if (part.length() > 4 && text.contains(part)) {
                    return true;
                }
            }
        }

        // Synonym and related term matching
        if (keyword.equals("demand") && (text.contains("consumer") || text.contains("buyer") || text.contains("purchase"))) return true;
        if (keyword.equals("supply") && (text.contains("producer") || text.contains("seller") || text.contains("provision"))) return true;
        if (keyword.equals("price") && (text.contains("cost") || text.contains("charge") || text.contains("fee"))) return true;
        if (keyword.equals("market") && (text.contains("industry") || text.contains("sector") || text.contains("trade"))) return true;
        if (keyword.equals("government") && (text.contains("state") || text.contains("public sector") || text.contains("authority"))) return true;
        if (keyword.equals("profit") && (text.contains("revenue") || text.contains("earnings") || text.contains("income"))) return true;
        if (keyword.equals("growth") && (text.contains("expansion") || text.contains("increase") || text.contains("development"))) return true;
        if (keyword.equals("unemployment") && (text.contains("jobless") || text.contains("employment") || text.contains("jobs"))) return true;
        if (keyword.equals("inflation") && (text.contains("prices") || text.contains("price level") || text.contains("cost of living"))) return true;

        return false;
    }

    /**
     * Validate a single topic with very inclusive matching
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

        // Check if the topic is in our list (exact match first)
        for (String validTopic : validTopics) {
            if (cleanedTopic.equalsIgnoreCase(validTopic)) {
                return validTopic;
            }
        }

        // Try to find partial matches with very flexible criteria
        for (String validTopic : validTopics) {
            // Check if the valid topic contains the suggested topic
            if (validTopic.toLowerCase().contains(cleanedTopic.toLowerCase()) && cleanedTopic.length() > 2) {
                return validTopic;
            }

            // Check if the suggested topic contains the valid topic
            if (validTopic.length() > 2 && cleanedTopic.toLowerCase().contains(validTopic.toLowerCase())) {
                return validTopic;
            }

            // Check for word-by-word match with very lenient criteria
            String[] validParts = validTopic.toLowerCase().split("\\s+");
            String[] suggestedParts = cleanedTopic.toLowerCase().split("\\s+");

            if (validParts.length > 1 && suggestedParts.length > 0) {
                int matchingWords = 0;
                for (String validPart : validParts) {
                    if (validPart.length() <= 2) continue; // Skip short words

                    for (String suggestedPart : suggestedParts) {
                        if (validPart.equals(suggestedPart) || 
                            (validPart.length() > 2 && suggestedPart.contains(validPart)) ||
                            (suggestedPart.length() > 2 && validPart.contains(suggestedPart))) {
                            matchingWords++;
                            break;
                        }
                    }
                }

                // Very lenient matching criteria - just need some word overlap
                if (matchingWords >= 1 && matchingWords >= Math.max(1, validParts.length - 3)) {
                    return validTopic;
                }
            }
        }

        return null; // No match found
    }

    /**
     * Determine a fallback topic based on content analysis with comprehensive coverage
     */
    public String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "1.2.2 Demand"; // Default topic
        }

        questionText = questionText.toLowerCase();

        // Enhanced fallback logic with many more topics
        
        // Check for specific economic concepts
        if (questionText.contains("globalisation") || questionText.contains("globalization") ||
                questionText.contains("global") || questionText.contains("multinational")) {
            return "4.1.1 Globalisation";
        }

        if (questionText.contains("trade") || questionText.contains("import") || questionText.contains("export")) {
            return "4.1.2 Specialisation and trade";
        }

        if (questionText.contains("fiscal policy") || questionText.contains("government spending") ||
                questionText.contains("taxation") || questionText.contains("budget")) {
            return "2.6.2 Demand-side policies";
        }

        if (questionText.contains("monetary policy") || questionText.contains("interest rate") ||
                questionText.contains("central bank") || questionText.contains("money supply")) {
            return "2.6.2 Demand-side policies";
        }

        if (questionText.contains("externality") || questionText.contains("externalities") ||
                questionText.contains("pollution") || questionText.contains("environment")) {
            return "1.3.2 Externalities";
        }

        if (questionText.contains("monopoly") || questionText.contains("market power")) {
            return "3.4.5 Monopoly";
        }

        if (questionText.contains("oligopoly") || questionText.contains("few firms")) {
            return "3.4.4 Oligopoly";
        }

        if (questionText.contains("perfect competition") || questionText.contains("competitive market")) {
            return "3.4.2 Perfect competition";
        }

        if (questionText.contains("elasticity") || questionText.contains("elastic") || questionText.contains("responsive")) {
            return "1.2.3 Price, income and cross elasticities of demand";
        }

        if (questionText.contains("supply") && !questionText.contains("demand")) {
            return "1.2.4 Supply";
        }

        if (questionText.contains("price") && (questionText.contains("determination") || questionText.contains("equilibrium"))) {
            return "1.2.6 Price determination";
        }

        if (questionText.contains("consumer surplus") || questionText.contains("producer surplus")) {
            return "1.2.8 Consumer and producer surplus";
        }

        if (questionText.contains("unemployment") || questionText.contains("employment") || questionText.contains("jobless")) {
            return "2.1.3 Employment and unemployment";
        }

        if (questionText.contains("inflation") || questionText.contains("price level") || questionText.contains("deflation")) {
            return "2.1.2 Inflation";
        }

        if (questionText.contains("growth") || questionText.contains("gdp")) {
            return "2.1.1 Economic growth";
        }

        if (questionText.contains("costs") || questionText.contains("revenue") || questionText.contains("profit")) {
            return "3.3.1 Revenue";
        }

        if (questionText.contains("labour") || questionText.contains("labor") || questionText.contains("wage")) {
            return "3.5.1 Demand for labour";
        }

        if (questionText.contains("development") || questionText.contains("developing")) {
            return "4.3.1 Measures of development";
        }

        if (questionText.contains("poverty") || questionText.contains("inequality")) {
            return "4.2.1 Absolute and relative poverty";
        }

        // If no specific pattern matches, return a common topic as default
        return "1.2.2 Demand";
    }
}