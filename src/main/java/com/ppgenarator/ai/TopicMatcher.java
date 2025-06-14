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
     * Find topics based on keywords with improved precision and consistency
     */
    public String[] findStrictTopicsByKeywords(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return new String[0];
        }

        questionText = questionText.toLowerCase();
        Map<String, Double> topicScores = new HashMap<>();
        Map<String, List<String>> topicKeywords = keywordManager.getTopicKeywords();
        Map<String, List<String>> conceptRelationships = keywordManager.getConceptRelationships();

        // Analyze question characteristics for better scoring
        boolean isCalculationQuestion = TopicConstants.TopicAssignmentRules.isComputationalQuestion(questionText);
        boolean requiresDiagram = TopicConstants.TopicAssignmentRules.requiresDiagramAnalysis(questionText);

        System.out.println("Keyword matching - Calculation: " + isCalculationQuestion + ", Diagram: " + requiresDiagram);

        // Score each topic based on keyword matches with improved precision
        for (Map.Entry<String, List<String>> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = calculateTopicScore(questionText, keywords, topic, isCalculationQuestion);

            // Enhanced scoring for related concepts
            for (Map.Entry<String, List<String>> conceptEntry : conceptRelationships.entrySet()) {
                String concept = conceptEntry.getKey();
                if (containsKeywordPrecisely(questionText, concept.toLowerCase())
                        && conceptEntry.getValue().contains(topic)) {
                    score += 2.0; // Bonus for related concepts
                }
            }

            // Direct topic mention gets significant bonus
            if (containsTopicDirectly(questionText, topic)) {
                score += 5.0;
            }

            // Apply quality threshold - more selective than before
            double threshold = isCalculationQuestion ? TopicConstants.KEYWORD_THRESHOLD + 1.0 : TopicConstants.KEYWORD_THRESHOLD;
            if (score >= threshold) {
                topicScores.put(topic, score);
            }
        }

        // Return fewer, higher-quality topics
        int maxTopics = isCalculationQuestion ? 2 : 3; // More restrictive
        List<String> matchingTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxTopics)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("Improved keyword matching found " + matchingTopics.size() + " topics: " + matchingTopics);

        return matchingTopics.toArray(new String[0]);
    }

    /**
     * Calculate topic score with improved precision
     */
    private double calculateTopicScore(String questionText, List<String> keywords, String topic, boolean isCalculationQuestion) {
        double score = 0;
        int exactMatches = 0;
        int partialMatches = 0;
        int significantMatches = 0;

        for (String keyword : keywords) {
            String cleanKeyword = keyword.toLowerCase().trim();
            
            if (cleanKeyword.length() < 3) continue; // Skip very short keywords
            
            MatchType matchType = getKeywordMatchType(questionText, cleanKeyword);
            
            switch (matchType) {
                case EXACT:
                    exactMatches++;
                    score += 3.0; // High score for exact matches
                    if (cleanKeyword.length() > 6) significantMatches++;
                    break;
                case PARTIAL:
                    partialMatches++;
                    score += 1.5; // Lower score for partial matches
                    break;
                case RELATED:
                    score += 0.5; // Minimal score for related terms
                    break;
                case NONE:
                default:
                    // No score
                    break;
            }
        }

        // Bonus scoring for multiple matches
        if (exactMatches > 1) {
            score += 2.0;
        }
        if (significantMatches > 0) {
            score += 1.0;
        }

        // Apply penalties for weak connections
        if (exactMatches == 0 && partialMatches < 2) {
            score *= 0.3; // Heavy penalty for weak connections
        }

        // Special handling for calculation questions
        if (isCalculationQuestion) {
            if (isCalculationRelevantTopic(topic)) {
                score += 1.0; // Bonus for calculation-relevant topics
            } else {
                score *= 0.5; // Penalty for non-calculation topics
            }
        }

        return score;
    }

    /**
     * Determine the type of keyword match
     */
    private MatchType getKeywordMatchType(String text, String keyword) {
        // Exact word boundary matches
        if (text.matches(".*\\b" + keyword + "\\b.*")) {
            return MatchType.EXACT;
        }
        
        // Exact matches with punctuation
        if (text.contains(" " + keyword + " ") ||
            text.contains(" " + keyword + ",") ||
            text.contains(" " + keyword + ".") ||
            text.contains("(" + keyword + ")") ||
            text.startsWith(keyword + " ") ||
            text.endsWith(" " + keyword)) {
            return MatchType.EXACT;
        }
        
        // Partial matches (plurals, tenses)
        if (keyword.length() > 4 && (
            text.contains(keyword + "s ") ||
            text.contains(keyword + "ing") ||
            text.contains(keyword + "ed") ||
            text.contains(keyword + "'s"))) {
            return MatchType.PARTIAL;
        }
        
        // Related term matching
        if (hasRelatedTermMatch(text, keyword)) {
            return MatchType.RELATED;
        }
        
        return MatchType.NONE;
    }

    /**
     * Check for related term matches
     */
    private boolean hasRelatedTermMatch(String text, String keyword) {
        // Only include very clear related terms
        switch (keyword) {
            case "demand":
                return text.contains("consumer") || text.contains("buyer") || text.contains("purchase");
            case "supply":
                return text.contains("producer") || text.contains("seller") || text.contains("provision");
            case "price":
                return text.contains("cost") && text.contains("pricing");
            case "market":
                return text.contains("industry") && text.contains("competition");
            case "government":
                return text.contains("state") || text.contains("public sector");
            case "profit":
                return text.contains("earnings") && text.contains("revenue");
            case "unemployment":
                return text.contains("jobless") || text.contains("employment rate");
            case "inflation":
                return text.contains("price level") || text.contains("cost of living");
            default:
                return false;
        }
    }

    /**
     * Check if topic is mentioned directly in the text
     */
    private boolean containsTopicDirectly(String text, String topic) {
        String topicLower = topic.toLowerCase();
        
        // Check for direct topic name mention
        if (text.contains(topicLower)) {
            return true;
        }
        
        // Check for key phrases from the topic
        String topicName = topic.replaceFirst("^\\d+\\.\\d+\\.\\d+\\s+", "");
        String[] keyPhrases = topicName.toLowerCase().split("\\s+and\\s+|,\\s*");
        
        for (String phrase : keyPhrases) {
            phrase = phrase.trim();
            if (phrase.length() > 3 && text.contains(phrase)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * More precise keyword matching
     */
    private boolean containsKeywordPrecisely(String text, String keyword) {
        return text.matches(".*\\b" + keyword.toLowerCase() + "\\b.*") ||
               text.contains(" " + keyword + " ") ||
               text.contains(" " + keyword + ",") ||
               text.contains(" " + keyword + ".") ||
               text.startsWith(keyword + " ") ||
               text.endsWith(" " + keyword);
    }

    /**
     * Check if topic is relevant for calculation questions
     */
    private boolean isCalculationRelevantTopic(String topic) {
        return topic.contains("elasticities") ||
               topic.contains("Revenue") ||
               topic.contains("Costs") ||
               topic.contains("profits") ||
               topic.contains("Economic growth") ||
               topic.contains("Inflation") ||
               topic.contains("unemployment") ||
               topic.contains("Supply") ||
               topic.contains("Demand") ||
               topic.contains("taxes") ||
               topic.contains("multiplier");
    }

    /**
     * Enum for match types
     */
    private enum MatchType {
        EXACT,    // Exact word boundary match
        PARTIAL,  // Partial match (plurals, tenses)
        RELATED,  // Related term match
        NONE      // No match
    }
}