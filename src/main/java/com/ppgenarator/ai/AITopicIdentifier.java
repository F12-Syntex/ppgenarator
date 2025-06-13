package com.ppgenarator.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openai.models.ChatModel;
import com.ppgenerator.types.Question;

public class AITopicIdentifier {

    private static final ChatModel OPENAI_MODEL = ChatModel.GPT_4_1_MINI;
    private final String[] topics;
    private final String qualification;
    private final int paper;

    public AITopicIdentifier(String[] topics) {
        this.topics = topics;
        this.qualification = null;
        this.paper = 0;
    }

    public AITopicIdentifier(String[] topics, String qualification) {
        this.topics = getRelevantTopicsForQualification(topics, qualification, 0);
        this.qualification = qualification;
        this.paper = 0;
    }

    public AITopicIdentifier(String[] topics, String qualification, int paper) {
        this.topics = getRelevantTopicsForQualificationAndPaper(topics, qualification, paper);
        this.qualification = qualification;
        this.paper = paper;
    }

    /**
     * Filter topics based on qualification level and paper number
     */
    private String[] getRelevantTopicsForQualificationAndPaper(String[] allTopics, String qualification, int paper) {
        if (qualification == null) {
            return allTopics;
        }

        List<String> relevantTopics = new ArrayList<>();
        
        for (String topic : allTopics) {
            String theme = TopicConstants.getThemeFromTopic(topic);
            
            if (isTopicRelevantForQualificationAndPaper(theme, qualification, paper)) {
                relevantTopics.add(topic);
            }
        }
        
        System.out.println("Filtered topics for " + qualification + " Paper " + paper + ": " + 
                         relevantTopics.size() + " out of " + allTopics.length + " total topics");
        
        if (relevantTopics.isEmpty()) {
            System.out.println("WARNING: No relevant topics found for " + qualification + " Paper " + paper + 
                             ". Using fallback filtering.");
            return getRelevantTopicsForQualification(allTopics, qualification, paper);
        }
        
        return relevantTopics.toArray(new String[0]);
    }

    /**
     * Determine if a theme is relevant for a specific qualification and paper
     */
    private boolean isTopicRelevantForQualificationAndPaper(String theme, String qualification, int paper) {
        String normalizedQual = qualification.toLowerCase();
        
        if (normalizedQual.contains("as")) {
            // AS Level paper-specific filtering
            switch (paper) {
                case 1:
                    return "Theme 1".equals(theme);
                case 2:
                    return "Theme 2".equals(theme);
                default:
                    // Fallback: AS covers Themes 1 and 2
                    return "Theme 1".equals(theme) || "Theme 2".equals(theme);
            }
        } else if (normalizedQual.contains("a level") || normalizedQual.contains("alevel") || normalizedQual.contains("a_level")) {
            // A Level paper-specific filtering
            switch (paper) {
                case 1:
                    return "Theme 1".equals(theme) || "Theme 3".equals(theme);
                case 2:
                    return "Theme 2".equals(theme) || "Theme 4".equals(theme);
                case 3:
                    return "Theme 1".equals(theme) || "Theme 2".equals(theme) || 
                           "Theme 3".equals(theme) || "Theme 4".equals(theme);
                default:
                    // Fallback: A Level covers Themes 3 and 4 primarily
                    return "Theme 3".equals(theme) || "Theme 4".equals(theme);
            }
        }
        
        // For unknown qualifications, include all themes
        return true;
    }

    /**
     * Fallback method for qualification-only filtering (backward compatibility)
     */
    private String[] getRelevantTopicsForQualification(String[] allTopics, String qualification, int paper) {
        if (qualification == null) {
            return allTopics;
        }

        List<String> relevantTopics = new ArrayList<>();
        
        for (String topic : allTopics) {
            String theme = TopicConstants.getThemeFromTopic(topic);
            
            switch (qualification.toLowerCase()) {
                case "as":
                case "as level":
                case "as_level":
                    // AS Level covers Themes 1 and 2
                    if ("Theme 1".equals(theme) || "Theme 2".equals(theme)) {
                        relevantTopics.add(topic);
                    }
                    break;
                case "a level":
                case "a_level":
                case "alevel":
                    // A Level covers Themes 3 and 4 primarily, but can include 1 and 2 for Paper 3
                    if ("Theme 3".equals(theme) || "Theme 4".equals(theme) ||
                        (paper == 3 && ("Theme 1".equals(theme) || "Theme 2".equals(theme)))) {
                        relevantTopics.add(topic);
                    }
                    break;
                default:
                    relevantTopics.add(topic);
                    break;
            }
        }
        
        return relevantTopics.toArray(new String[0]);
    }

    /**
     * Identify topics for a batch of questions with enhanced topic coverage
     */
    public Map<Integer, String[]> identifyTopicsForBatch(List<Question> questions) {
        if (questions.isEmpty()) {
            return new HashMap<>();
        }

        try {
            StringBuilder batchPrompt = createBatchPrompt(questions);

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String response = openAI.query(batchPrompt.toString());

            System.out.println("AI Response for batch:\n" + response + "\n");

            return parseMultipleTopicAssignments(response, questions.size());

        } catch (Exception e) {
            System.err.println("Error in batch AI processing: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Identify topics for a single question with enhanced coverage
     */
    public String[] identifyTopicsForSingleQuestion(String cleanedText) {
        try {
            String prompt = createSingleQuestionPrompt(cleanedText);

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String response = openAI.query(prompt);

            System.out.println("Single question AI response: " + response);

            return parseTopicsFromResponse(response);

        } catch (Exception e) {
            System.err.println("Error in single question AI processing: " + e.getMessage());
            return new String[0];
        }
    }

    private StringBuilder createBatchPrompt(List<Question> questions) {
        StringBuilder batchPrompt = new StringBuilder();
        batchPrompt.append(
                "You are an expert A-level Economics examiner who specializes in categorizing exam questions based on the official Edexcel A-level Economics specification.\n\n");
        
        // Add qualification and paper context
        if (qualification != null && paper > 0) {
            batchPrompt.append("IMPORTANT: You are categorizing questions for ").append(qualification.toUpperCase())
                      .append(" PAPER ").append(paper).append(".\n");
            
            String paperContext = getPaperContext(qualification, paper);
            batchPrompt.append(paperContext).append("\n");
        } else if (qualification != null) {
            batchPrompt.append("IMPORTANT: You are categorizing questions for ").append(qualification.toUpperCase()).append(" level.\n");
            if (qualification.toLowerCase().contains("as")) {
                batchPrompt.append("AS Level covers ONLY Themes 1 and 2 of the specification.\n");
            } else if (qualification.toLowerCase().contains("a level") || qualification.toLowerCase().contains("alevel")) {
                batchPrompt.append("A Level covers ONLY Themes 3 and 4 of the specification (plus Themes 1-2 for Paper 3).\n");
            }
            batchPrompt.append("\n");
        }
        
        batchPrompt.append("I'll provide you with ").append(questions.size()).append(" economics exam questions.\n");
        batchPrompt.append(
                "For each question, identify ALL relevant economic topics that could be applied or tested. BE COMPREHENSIVE AND INCLUSIVE - if a topic is even reasonably related, include it.\n\n");

        batchPrompt.append("SPECIFICATION STRUCTURE:\n");
        batchPrompt.append("Use ONLY these relevant specification topics for this paper:\n");
        for (String topic : topics) {
            batchPrompt.append("- ").append(topic).append("\n");
        }
        batchPrompt.append("\n");

        batchPrompt.append("ENHANCED INSTRUCTIONS FOR COMPREHENSIVE COVERAGE:\n");
        batchPrompt.append(
                "1. IGNORE phrases like 'using the data from the extract' or 'refer to the figure' - these are just exam instructions\n");
        batchPrompt.append("2. Include the PRIMARY topic that is being directly tested\n");
        batchPrompt.append("3. Include ALL SECONDARY topics that are relevant or could be applied to answer the question\n");
        batchPrompt.append("4. Include topics where knowledge would be helpful for understanding, even if not directly tested\n");
        batchPrompt.append("5. Use the EXACT specification codes (e.g., '1.2.3 Price, income and cross elasticities of demand')\n");
        batchPrompt.append("6. MAXIMUM 6 topics per question - aim for 3-5 topics for comprehensive coverage\n");
        batchPrompt.append("7. Be VERY inclusive - if a topic is reasonably related, include it\n");
        batchPrompt.append("8. Consider all cross-connections between topics and related concepts\n");
        batchPrompt.append("9. Think about what students need to know to fully understand and answer the question\n");
        batchPrompt.append("10. Include foundational topics that underpin the main concepts being tested\n");
        batchPrompt.append("11. ONLY use topics from the provided list for this specific paper\n\n");

        // Add paper-specific examples
        batchPrompt.append(getPaperSpecificExamples(qualification, paper));

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
            batchPrompt.append(question.getQuestionText()).append("\n\n");
        }

        batchPrompt.append(
                "For each question, respond with the question number and ALL relevant specification topics (be comprehensive!).\n");
        batchPrompt.append(
                "Format: 'Question 1: 1.2.3 Price, income and cross elasticities of demand, 1.2.2 Demand'\n");
        batchPrompt.append("CRITICAL: Use exact specification codes and be VERY COMPREHENSIVE in coverage. ONLY use topics from the provided list for this specific paper. Include all topics that would help a student understand and answer the question effectively.");

        return batchPrompt;
    }

    private String createSingleQuestionPrompt(String cleanedText) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert A-level Economics examiner. Analyze this economics exam question to identify ALL relevant economic concepts.\n\n");
        
        // Add qualification and paper context
        if (qualification != null && paper > 0) {
            prompt.append("IMPORTANT: You are categorizing a question for ").append(qualification.toUpperCase())
                  .append(" PAPER ").append(paper).append(".\n");
            
            String paperContext = getPaperContext(qualification, paper);
            prompt.append(paperContext).append("\n");
        } else if (qualification != null) {
            prompt.append("IMPORTANT: You are categorizing a question for ").append(qualification.toUpperCase()).append(" level.\n");
            if (qualification.toLowerCase().contains("as")) {
                prompt.append("AS Level covers ONLY Themes 1 and 2 of the specification.\n");
            } else if (qualification.toLowerCase().contains("a level") || qualification.toLowerCase().contains("alevel")) {
                prompt.append("A Level covers ONLY Themes 3 and 4 of the specification (plus Themes 1-2 for Paper 3).\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("QUESTION:\n").append(cleanedText).append("\n\n");
        
        prompt.append("ENHANCED INSTRUCTIONS:\n");
        prompt.append("1. IGNORE contextual elements like 'refer to the extract' or 'using the data' - focus on the CORE economic concepts\n");
        prompt.append("2. What is the PRIMARY economic knowledge area a student needs to answer this question?\n");
        prompt.append("3. What SECONDARY topics are relevant or could be applied to answer the question?\n");
        prompt.append("4. Include topics where knowledge would be helpful even if not directly tested\n");
        prompt.append("5. Maximum 4 topics, aim for 2-3 topics for comprehensive coverage\n");
        prompt.append("6. Be inclusive - if a topic is reasonably related, include it\n");
        prompt.append("7. Consider cross-connections between economic concepts\n");
        prompt.append("8. ONLY select from this list for this specific paper:\n");
        
        for (String topic : topics) {
            prompt.append("- ").append(topic).append("\n");
        }
        prompt.append("\n");

        // Add paper-specific examples
        prompt.append(getPaperSpecificExamples(qualification, paper));
        
        prompt.append("\nRespond with ALL relevant topics, separated by commas. Be comprehensive and inclusive, but ONLY use topics from the provided list for this specific paper.");

        return prompt.toString();
    }

    private String getPaperContext(String qualification, int paper) {
        String normalizedQual = qualification.toLowerCase();
        
        if (normalizedQual.contains("as")) {
            switch (paper) {
                case 1:
                    return "AS Paper 1 covers ONLY Theme 1 (Nature of economics, How markets work, Market failure, Government intervention).";
                case 2:
                    return "AS Paper 2 covers ONLY Theme 2 (Economic performance, Aggregate demand/supply, National income, Economic growth, Macroeconomic policies).";
                default:
                    return "AS Level covers Themes 1 and 2 of the specification.";
            }
        } else if (normalizedQual.contains("a level") || normalizedQual.contains("alevel")) {
            switch (paper) {
                case 1:
                    return "A Level Paper 1 covers ONLY Themes 1 and 3 (Microeconomics: Markets, businesses, and distribution of income).";
                case 2:
                    return "A Level Paper 2 covers ONLY Themes 2 and 4 (Macroeconomics: National and international economy).";
                case 3:
                    return "A Level Paper 3 covers ALL Themes 1, 2, 3, and 4 (Microeconomics and Macroeconomics synoptic assessment).";
                default:
                    return "A Level covers Themes 3 and 4 primarily, with Themes 1-2 for Paper 3.";
            }
        }
        
        return "Standard economics specification coverage.";
    }

    private String getPaperSpecificExamples(String qualification, int paper) {
        StringBuilder examples = new StringBuilder();
        String normalizedQual = qualification != null ? qualification.toLowerCase() : "";
        
        if (normalizedQual.contains("as")) {
            switch (paper) {
                case 1:
                    examples.append("Enhanced Examples (AS Paper 1 - Theme 1 only):\n");
                    examples.append("- Question asking to calculate PED and discuss revenue implications → Topics: 1.2.3 Price, income and cross elasticities of demand, 1.2.2 Demand, 1.2.6 Price determination\n");
                    examples.append("- Question about market failure and externalities → Topics: 1.3.2 Externalities, 1.3.1 Types of market failure, 1.4.1 Government intervention in markets\n");
                    examples.append("- Question about supply and demand equilibrium → Topics: 1.2.2 Demand, 1.2.4 Supply, 1.2.6 Price determination, 1.2.7 Price mechanism\n");
                    break;
                case 2:
                    examples.append("Enhanced Examples (AS Paper 2 - Theme 2 only):\n");
                    examples.append("- Question about unemployment and government policies → Topics: 2.1.3 Employment and unemployment, 2.6.2 Demand-side policies, 2.6.3 Supply-side policies\n");
                    examples.append("- Question about economic growth and output gaps → Topics: 2.1.1 Economic growth, 2.5.2 Output gaps, 2.5.1 Causes of growth\n");
                    examples.append("- Question about inflation and monetary policy → Topics: 2.1.2 Inflation, 2.6.2 Demand-side policies, 2.6.1 Possible macroeconomic objectives\n");
                    break;
                default:
                    examples.append("Enhanced Examples (AS Level - Themes 1&2):\n");
                    examples.append("- Question asking to calculate PED and discuss revenue implications → Topics: 1.2.3 Price, income and cross elasticities of demand, 1.2.2 Demand\n");
                    examples.append("- Question about government policies to address unemployment → Topics: 2.1.3 Employment and unemployment, 2.6.2 Demand-side policies\n");
            }
        } else if (normalizedQual.contains("a level") || normalizedQual.contains("alevel")) {
            switch (paper) {
                case 1:
                    examples.append("Enhanced Examples (A Level Paper 1 - Themes 1&3 only):\n");
                    examples.append("- Question about monopoly pricing and market efficiency → Topics: 3.4.5 Monopoly, 3.4.1 Efficiency, 1.2.8 Consumer and producer surplus\n");
                    examples.append("- Question about business objectives and profit maximization → Topics: 3.2.1 Business objectives, 3.3.4 Normal profits, supernormal profits and losses\n");
                    examples.append("- Question about labour market wage determination → Topics: 3.5.3 Wage determination in competitive and non-competitive markets, 3.5.1 Demand for labour\n");
                    break;
                case 2:
                    examples.append("Enhanced Examples (A Level Paper 2 - Themes 2&4 only):\n");
                    examples.append("- Question about globalisation and trade policies → Topics: 4.1.1 Globalisation, 4.1.6 Restrictions on free trade, 4.1.9 International competitiveness\n");
                    examples.append("- Question about development strategies and poverty → Topics: 4.3.3 Strategies influencing growth and development, 4.2.1 Absolute and relative poverty\n");
                    examples.append("- Question about fiscal policy and economic growth → Topics: 2.6.2 Demand-side policies, 2.1.1 Economic growth, 4.5.1 Public expenditure\n");
                    break;
                case 3:
                    examples.append("Enhanced Examples (A Level Paper 3 - All Themes 1,2,3,4):\n");
                    examples.append("- Question linking microeconomic market failure to macroeconomic policy → Topics: 1.3.1 Types of market failure, 2.6.2 Demand-side policies, 3.6.1 Government intervention\n");
                    examples.append("- Question about international trade and domestic market structures → Topics: 4.1.2 Specialisation and trade, 3.4.2 Perfect competition, 3.4.5 Monopoly\n");
                    examples.append("- Question connecting business decisions to economic development → Topics: 3.2.1 Business objectives, 4.3.2 Factors influencing growth and development\n");
                    break;
                default:
                    examples.append("Enhanced Examples (A Level - Themes 3&4):\n");
                    examples.append("- Question about monopoly pricing and social welfare → Topics: 3.4.5 Monopoly, 3.4.1 Efficiency, 3.6.1 Government intervention\n");
                    examples.append("- Question about globalisation effects → Topics: 4.1.1 Globalisation, 4.1.6 Restrictions on free trade\n");
            }
        } else {
            examples.append("Enhanced Examples:\n");
            examples.append("- Question asking to calculate PED and discuss revenue implications → Topics: elasticity, demand and supply\n");
            examples.append("- Question about market structures and efficiency → Topics: market structures, efficiency, government intervention\n");
        }
        
        return examples.toString();
    }

    /**
     * Parse multiple topic assignments from the AI response with enhanced handling
     */
    private Map<Integer, String[]> parseMultipleTopicAssignments(String response, int expectedCount) {
        Map<Integer, String[]> results = new HashMap<>();

        Pattern pattern = Pattern.compile("(?:Question|QUESTION)\\s+(\\d+)\\s*:\\s*([^\\n\\r]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            try {
                int questionNum = Integer.parseInt(matcher.group(1));
                String topicsString = matcher.group(2).trim();

                System.out.println("Parsing - Question " + questionNum + ": " + topicsString);

                if (questionNum > 0 && questionNum <= expectedCount) {
                    String[] topics = topicsString.split("\\s*[,;]\\s*|\\s+and\\s+");
                    List<String> cleanedTopics = new ArrayList<>();

                    for (String topic : topics) {
                        String cleanedTopic = topic.toLowerCase().trim()
                                .replaceAll("[\"'.-]", "")
                                .replaceAll("(?i)^(the\\s+)?", "");

                        if (!cleanedTopic.isEmpty() && cleanedTopic.length() > 2) {
                            cleanedTopics.add(cleanedTopic);
                        }
                    }

                    if (!cleanedTopics.isEmpty()) {
                        if (cleanedTopics.size() > TopicConstants.MAX_TOPICS_PER_QUESTION) {
                            cleanedTopics = cleanedTopics.subList(0, TopicConstants.MAX_TOPICS_PER_QUESTION);
                        }
                        results.put(questionNum, cleanedTopics.toArray(new String[0]));
                        System.out.println(
                                "Successfully parsed " + cleanedTopics.size() + " topics for question " + questionNum);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse question number from: " + matcher.group(1));
            }
        }

        System.out.println("Total questions parsed: " + results.size() + " out of " + expectedCount);
        return results;
    }

    /**
     * Parse topics from AI response that may contain multiple topics
     */
    private String[] parseTopicsFromResponse(String response) {
        if (response == null) {
            return new String[0];
        }

        String cleanedResponse = response.trim()
                .replaceAll("[\"'.]", "")
                .replaceAll("(?i)Topics?:?\\s*", "")
                .replaceAll("(?i)The topics? (?:are?|is):?\\s*", "")
                .replaceAll("(?i)The relevant topics? (?:are?|is):?\\s*", "")
                .replaceAll("(?i)Primary topics?:?\\s*", "")
                .replaceAll("(?i)All relevant topics?:?\\s*", "");

        String[] topics = cleanedResponse.split("\\s*[,;]\\s*|\\s+and\\s+");

        List<String> cleanedTopics = new ArrayList<>();

        for (String topic : topics) {
            String cleanedTopic = topic.toLowerCase().trim()
                    .replaceAll("(?i)^(the\\s+)?", "");

            if (!cleanedTopic.isEmpty() && cleanedTopic.length() > 2) {
                cleanedTopics.add(cleanedTopic);
            }
        }

        return cleanedTopics.toArray(new String[0]);
    }
}