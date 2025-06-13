package com.ppgenarator.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopicMatcher {
    private final TopicKeywordManager keywordManager;
    private final TextProcessor textProcessor;

    public TopicMatcher(TopicKeywordManager keywordManager, TextProcessor textProcessor) {
        this.keywordManager = keywordManager;
        this.textProcessor = textProcessor;
    }

    /**
     * Find topics based on keywords with enhanced scoring and broader coverage
     */
    public String[] findStrictTopicsByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new String[0];
        }

        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();
        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
        Map<String, List<String>> conceptRelationships = keywordManager.getConceptRelationships();

        // Score each topic based on keyword matches with enhanced criteria
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = 0;
            int significantMatches = 0;

            for (String keyword : keywords) {
                String cleanKeyword = keyword.toLowerCase();
                if (containsKeyword(questionText, cleanKeyword)) {
                    double weight = 1.0 + (Math.min(cleanKeyword.length(), 15) / 10.0);
                    score += weight;

                    // Count significant matches (longer keywords)
                    if (cleanKeyword.length() > 4) { // Reduced from 5
                        significantMatches++;
                    }
                }
            }

            // Check for related concepts with enhanced scoring
            for (Map.Entry<String, List<String>> conceptEntry : conceptRelationships.entrySet()) {
                String concept = conceptEntry.getKey();
                if (questionText.contains(concept.toLowerCase()) && 
                    conceptEntry.getValue().contains(topic)) {
                    score += 1.5; // Bonus for related concepts
                    significantMatches++;
                }
            }

            // Direct mention bonus (reduced to balance with other factors)
            if (questionText.contains(topic.toLowerCase())) {
                score += 3.0; // Reduced from 5.0
                significantMatches += 1;
            }

            // More lenient inclusion criteria
            if (score >= TopicConstants.KEYWORD_THRESHOLD || significantMatches > 0) {
                topicScores.put(topic, score);
            }
        }

        // Return more topics with better coverage
        List<String> matchingTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("Enhanced keyword matching found " + matchingTopics.size() + " topics: " + matchingTopics);

        return matchingTopics.toArray(new String[0]);
    }

    /**
     * Enhanced flexible keyword matching
     */
    private boolean containsKeyword(String text, String keyword) {
        // More flexible keyword matching
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
               text.contains(keyword + "ed") || // Past tense forms
               text.contains(keyword + "-") || // Hyphenated forms
               text.contains("-" + keyword); // Reverse hyphenated forms
    }
}