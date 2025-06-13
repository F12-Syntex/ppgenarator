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
     * Find topics based on keywords with very lenient scoring and comprehensive
     * coverage
     */
    public String[] findStrictTopicsByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new String[0];
        }

        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();
        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
        Map<String, List<String>> conceptRelationships = keywordManager.getConceptRelationships();

        // Score each topic based on keyword matches with very lenient criteria
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = 0;
            int significantMatches = 0;

            for (String keyword : keywords) {
                String cleanKeyword = keyword.toLowerCase();
                if (containsKeywordVeryFlexibly(questionText, cleanKeyword)) {
                    double weight = 0.5 + (Math.min(cleanKeyword.length(), 15) / 20.0); // Lower base weight
                    score += weight;

                    // Count significant matches (longer keywords)
                    if (cleanKeyword.length() > 3) { // Reduced from 4
                        significantMatches++;
                    }
                }
            }

            // Check for related concepts with enhanced scoring
            for (Map.Entry<String, List<String>> conceptEntry : conceptRelationships.entrySet()) {
                String concept = conceptEntry.getKey();
                if (questionText.contains(concept.toLowerCase())
                        && conceptEntry.getValue().contains(topic)) {
                    score += 1.0; // Bonus for related concepts
                    significantMatches++;
                }
            }

            // Direct mention bonus (moderate bonus)
            if (questionText.contains(topic.toLowerCase())) {
                score += 2.0; // Reduced from 3.0
                significantMatches += 1;
            }

            // Very lenient inclusion criteria
            if (score >= TopicConstants.KEYWORD_THRESHOLD || significantMatches > 0) {
                topicScores.put(topic, score);
            }
        }

        // Return more topics with much better coverage
        List<String> matchingTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TopicConstants.MAX_TOPICS_PER_QUESTION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("Enhanced comprehensive keyword matching found " + matchingTopics.size() + " topics: " + matchingTopics);

        return matchingTopics.toArray(new String[0]);
    }

    /**
     * Very flexible keyword matching with extensive variations
     */
    private boolean containsKeywordVeryFlexibly(String text, String keyword) {
        // All the original flexible matching plus more
        return text.contains(" " + keyword + " ")
                || text.startsWith(keyword + " ")
                || text.endsWith(" " + keyword)
                || text.contains(" " + keyword + ",")
                || text.contains(" " + keyword + ".")
                || text.contains(" " + keyword + ";")
                || text.contains("(" + keyword + ")")
                || text.contains(keyword + "'s")
                || text.contains(keyword + "s ")
                || // Plural forms
                text.contains(keyword + "ing")
                || // Gerund forms
                text.contains(keyword + "ed")
                || // Past tense forms
                text.contains(keyword + "-")
                || // Hyphenated forms
                text.contains("-" + keyword)
                || // Reverse hyphenated forms
                text.contains(keyword + "ion")
                || // -tion endings
                text.contains(keyword + "ment")
                || // -ment endings
                text.contains(keyword + "ness")
                || // -ness endings
                text.contains(keyword + "ity")
                || // -ity endings
                // Partial matching for longer keywords
                (keyword.length() > 6 && text.contains(keyword.substring(0, keyword.length() - 2)))
                || // Root word matching
                (keyword.length() > 5 && text.contains(keyword.substring(0, keyword.length() - 1)));
    }
}
