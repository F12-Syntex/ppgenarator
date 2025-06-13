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
     * Validate and limit topics with more inclusive criteria
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

        // Score topics based on relevance to question with more lenient thresholds
        Map<String, Double> topicRelevanceScores = scoreTopicRelevance(validTopics, questionText);

        // More inclusive filtering - lower threshold for inclusion
        List<String> finalTopics = topicRelevanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
                .filter(entry -> entry.getValue() > 0.3) // Reduced from 0.5
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // If we still have room and there are more valid topics, include them
        if (finalTopics.size() < TopicConstants.MAX_TOPICS_PER_QUESTION && validTopics.size() > finalTopics.size()) {
            for (String topic : validTopics) {
                if (!finalTopics.contains(topic) && finalTopics.size() < TopicConstants.MAX_TOPICS_PER_QUESTION) {
                    finalTopics.add(topic);
                    System.out.println("Added additional topic for better coverage: " + topic);
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
     * Score topics based on their relevance to the question text with enhanced scoring
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

            // Check for keyword relevance with enhanced matching
            Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
            if (topicKeywords.containsKey(topic)) {
                List<String> keywords = topicKeywords.get(topic);
                int matchingKeywords = 0;
                for (String keyword : keywords) {
                    if (containsKeywordFlexibly(lowerQuestionText, keyword.toLowerCase())) {
                        matchingKeywords++;
                        // More specific keywords get higher weight
                        score += (keyword.length() > 6) ? 1.5 : 1.0;
                    }
                }

                // Bonus for multiple keyword matches
                if (matchingKeywords > 1) {
                    score += 1.0;
                }
                if (matchingKeywords > 3) {
                    score += 2.0;
                }
            }

            // Check for related concept connections
            Map<String, List<String>> conceptRelationships = keywordManager.getConceptRelationships();
            for (Map.Entry<String, List<String>> conceptEntry : conceptRelationships.entrySet()) {
                String concept = conceptEntry.getKey();
                if (lowerQuestionText.contains(concept.toLowerCase()) && 
                    conceptEntry.getValue().contains(topic)) {
                    score += 1.5; // Bonus for related concepts
                }
            }

            scores.put(topic, score);
        }

        return scores;
    }

    /**
     * Enhanced flexible keyword matching
     */
    private boolean containsKeywordFlexibly(String text, String keyword) {
        return text.contains(" " + keyword + " ") ||
               text.startsWith(keyword + " ") ||
               text.endsWith(" " + keyword) ||
               text.contains(" " + keyword + ",") ||
               text.contains(" " + keyword + ".") ||
               text.contains(" " + keyword + ";") ||
               text.contains("(" + keyword + ")") ||
               text.contains(keyword + "'s") ||
               text.contains(keyword + "s ") || // Plural forms
               text.contains(keyword + "ing") || // Gerund forms
               text.contains(keyword + "ed"); // Past tense forms
    }

    /**
     * Validate a single topic with improved matching
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
                .replaceAll("(?i)The primary topic is:?\\s*", "")
                .toLowerCase();

        // Check if the topic is in our list (exact match)
        for (String validTopic : validTopics) {
            if (cleanedTopic.equals(validTopic)) {
                return validTopic;
            }
        }

        // Try to find partial matches with more flexible criteria
        for (String validTopic : validTopics) {
            // Check if the valid topic contains the suggested topic
            if (validTopic.contains(cleanedTopic) && cleanedTopic.length() > 3) { // Reduced from 4
                return validTopic;
            }

            // Check if the suggested topic contains the valid topic
            if (validTopic.length() > 3 && cleanedTopic.contains(validTopic)) { // Reduced from 4
                return validTopic;
            }

            // Check for word-by-word match with more lenient criteria
            String[] validParts = validTopic.split("\\s+");
            String[] suggestedParts = cleanedTopic.split("\\s+");

            if (validParts.length > 1 && suggestedParts.length > 0) {
                int matchingWords = 0;
                for (String validPart : validParts) {
                    if (validPart.length() <= 2)
                        continue; // Skip short words

                    for (String suggestedPart : suggestedParts) {
                        if (validPart.equals(suggestedPart) || 
                            (validPart.length() > 3 && suggestedPart.contains(validPart)) ||
                            (suggestedPart.length() > 3 && validPart.contains(suggestedPart))) {
                            matchingWords++;
                            break;
                        }
                    }
                }

                // More lenient matching criteria
                if (matchingWords >= Math.max(1, validParts.length - 2)) { // More flexible
                    return validTopic;
                }
            }
        }

        return null; // No match found
    }

    /**
     * Determine a fallback topic based on content analysis with broader coverage
     */
    public String determineFallbackTopic(String questionText) {
        if (questionText == null) {
            return "demand and supply"; // Default topic
        }

        questionText = questionText.toLowerCase();

        // Enhanced fallback logic with more topics
        
        // Check for globalisation-related terms
        if (questionText.contains("globalisation") || questionText.contains("globalization") ||
                questionText.contains("global") || questionText.contains("international trade") ||
                questionText.contains("multinational") || questionText.contains("world economy")) {
            return "globalisation";
        }

        // Check for international economics
        if (questionText.contains("international") || questionText.contains("trade") ||
                questionText.contains("import") || questionText.contains("export")) {
            return "international economics";
        }

        // Check for fiscal policy terms
        if (questionText.contains("fiscal policy") || questionText.contains("government spending") ||
                questionText.contains("taxation") || questionText.contains("budget")) {
            return "fiscal policy";
        }

        // Check for monetary policy terms
        if (questionText.contains("monetary policy") || questionText.contains("interest rate") ||
                questionText.contains("central bank") || questionText.contains("money supply")) {
            return "monetary policy";
        }

        // Check for market failure
        if (questionText.contains("market failure") || questionText.contains("externality") ||
                questionText.contains("public good") || questionText.contains("merit good")) {
            return "market failure";
        }

        // Check for externalities
        if (questionText.contains("externality") || questionText.contains("externalities") ||
                questionText.contains("pollution") || questionText.contains("environment")) {
            return "externalities";
        }

        // Check for competition and market structures
        if (questionText.contains("monopoly") || questionText.contains("oligopoly") ||
                questionText.contains("competition") || questionText.contains("market structure")) {
            return "market structures";
        }

        // Check for development economics
        if (questionText.contains("developing") || questionText.contains("development") ||
                questionText.contains("poverty") || questionText.contains("inequality")) {
            return "developing economies";
        }

        // Check for labor market
        if (questionText.contains("labor") || questionText.contains("labour") ||
                questionText.contains("wage") || questionText.contains("employment")) {
            return "labor market";
        }

        // Check for economic growth
        if (questionText.contains("growth") || questionText.contains("gdp") ||
                questionText.contains("productivity")) {
            return "economic growth";
        }

        // If no specific pattern matches, return a common topic as default
        return "demand and supply";
    }
}