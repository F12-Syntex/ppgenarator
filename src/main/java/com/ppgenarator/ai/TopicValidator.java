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
     * Validate and limit topics with strict criteria
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

        // Score topics based on relevance to question
        Map<String, Double> topicRelevanceScores = scoreTopicRelevance(validTopics, questionText);

        // Sort by relevance and limit to top topics
        List<String> finalTopics = topicRelevanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
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
            Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
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

        // Try to find partial matches
        for (String validTopic : validTopics) {
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
     * Determine a fallback topic based on content analysis
     */
    public String determineFallbackTopic(String questionText) {
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
}