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
     * Find topics based on keywords with strict scoring
     */
    public String[] findStrictTopicsByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new String[0];
        }

        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();
        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();

        // Score each topic based on keyword matches with stricter criteria
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = 0;
            int significantMatches = 0;

            for (String keyword : keywords) {
                String cleanKeyword = keyword.toLowerCase();
                if (questionText.contains(" " + cleanKeyword + " ") ||
                        questionText.startsWith(cleanKeyword + " ") ||
                        questionText.endsWith(" " + cleanKeyword) ||
                        questionText.contains(" " + cleanKeyword + ",") ||
                        questionText.contains(" " + cleanKeyword + ".")) {

                    double weight = 1.0 + (Math.min(cleanKeyword.length(), 15) / 10.0);
                    score += weight;

                    // Count significant matches (longer keywords)
                    if (cleanKeyword.length() > 5) {
                        significantMatches++;
                    }
                }
            }

            // Direct mention bonus
            if (questionText.contains(topic.toLowerCase())) {
                score += 5.0;
                significantMatches += 2;
            }

            // Only include topics with strong evidence
            if (score >= TopicConstants.KEYWORD_THRESHOLD && significantMatches > 0) {
                topicScores.put(topic, score);
            }
        }

        // Return only the top scoring topics
        List<String> matchingTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("Strict keyword matching found " + matchingTopics.size() + " topics: " + matchingTopics);

        return matchingTopics.toArray(new String[0]);
    }
}