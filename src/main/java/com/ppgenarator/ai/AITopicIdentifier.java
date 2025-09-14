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

    private static final ChatModel OPENAI_MODEL = ChatModel.GPT_5_NANO_2025_08_07;
    private final String[] topics;

    public AITopicIdentifier(String[] topics) {
        this.topics = topics;
    }

    /**
     * Identify topics for a batch of questions with improved consistency
     */
    public Map<Integer, String[]> identifyTopicsForBatch(List<Question> questions) {
        if (questions.isEmpty()) {
            return new HashMap<>();
        }

        try {
            StringBuilder batchPrompt = createImprovedBatchPrompt(questions);

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 1); // Slightly higher temperature for consistency
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
     * Identify topics for a single question with improved focus
     */
    public String[] identifyTopicsForSingleQuestion(String cleanedText) {
        try {
            String prompt = createImprovedSingleQuestionPrompt(cleanedText);

            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.2);
            String response = openAI.query(prompt);

            System.out.println("Single question AI response: " + response);

            return parseTopicsFromResponse(response);

        } catch (Exception e) {
            System.err.println("Error in single question AI processing: " + e.getMessage());
            return new String[0];
        }
    }

    private StringBuilder createImprovedBatchPrompt(List<Question> questions) {
        StringBuilder batchPrompt = new StringBuilder();
        batchPrompt.append(
                "You are an expert A-level Economics examiner who categorizes exam questions with CONSISTENCY and PRECISION.\n\n");

        batchPrompt.append("I'll provide you with ").append(questions.size()).append(" economics exam questions.\n");
        batchPrompt.append("For each question, identify the MOST RELEVANT topics based on what students actually need to know to answer the question.\n\n");

        batchPrompt.append("CRITICAL RULES FOR CONSISTENCY:\n");
        batchPrompt.append("1. IGNORE exam instructions like 'using the data from the extract', 'refer to the figure', 'show your working'\n");
        batchPrompt.append("2. Focus on the CORE ECONOMIC CONCEPTS being tested\n");
        batchPrompt.append("3. For calculation questions (2-4 marks): Usually 1-2 topics maximum\n");
        batchPrompt.append("4. For short explanations (5-8 marks): Usually 2-3 topics\n");
        batchPrompt.append("5. For essays (12+ marks): Maximum 4 topics\n");
        batchPrompt.append("6. DO NOT include '1.1.3 The economic problem' unless the question is specifically about scarcity, opportunity cost, or resource allocation\n");
        batchPrompt.append("7. Be SELECTIVE - only include topics that are directly relevant\n");
        batchPrompt.append("8. Ensure similar questions get similar topic assignments\n\n");

        batchPrompt.append("SPECIFICATION TOPICS (use EXACT codes):\n");
        for (String topic : topics) {
            batchPrompt.append("- ").append(topic).append("\n");
        }
        batchPrompt.append("\n");

        batchPrompt.append("EXAMPLES OF GOOD CATEGORIZATION:\n");
        batchPrompt.append("- 'Calculate price elasticity of demand' (2 marks) → '1.2.3 Price, income and cross elasticities of demand' ONLY\n");
        batchPrompt.append("- 'Explain factors affecting supply' (4 marks) → '1.2.4 Supply, 1.2.6 Price determination'\n");
        batchPrompt.append("- 'Assess government intervention to reduce market failure' (12 marks) → '1.3.1 Types of market failure, 1.4.1 Government intervention in markets, 1.4.2 Government failure'\n");
        batchPrompt.append("- 'Draw supply and demand diagram' (3 marks) → '1.2.2 Demand, 1.2.4 Supply, 1.2.6 Price determination'\n\n");

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            batchPrompt.append("QUESTION ").append(i + 1).append(" (").append(question.getMarks()).append(" marks):\n");
            batchPrompt.append(question.getQuestionText()).append("\n\n");
        }

        batchPrompt.append("RESPONSE FORMAT:\n");
        batchPrompt.append("Question 1: [topic1], [topic2] (if needed)\n");
        batchPrompt.append("Question 2: [topic1]\n");
        batchPrompt.append("etc.\n\n");
        
        batchPrompt.append("Remember: Be CONSISTENT, SELECTIVE, and PRECISE. Use exact specification codes. Focus on what students need to know.");

        return batchPrompt;
    }

    private String createImprovedSingleQuestionPrompt(String cleanedText) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert A-level Economics examiner. Categorize this exam question with PRECISION and CONSISTENCY.\n\n");

        prompt.append("QUESTION:\n").append(cleanedText).append("\n\n");

        prompt.append("CATEGORIZATION RULES:\n");
        prompt.append("1. IGNORE exam instructions ('refer to extract', 'show working', etc.)\n");
        prompt.append("2. Focus on CORE ECONOMIC CONCEPTS being tested\n");
        prompt.append("3. Be SELECTIVE - only essential topics\n");
        prompt.append("4. Maximum 3 topics for most questions\n");
        prompt.append("5. Only include '1.1.3 The economic problem' if specifically about scarcity/opportunity cost\n");
        prompt.append("6. Use EXACT specification codes from this list:\n\n");

        // Group topics by theme for better organization
        prompt.append("THEME 1 (Microeconomics fundamentals):\n");
        for (String topic : topics) {
            if (topic.startsWith("1.")) {
                prompt.append("- ").append(topic).append("\n");
            }
        }
        
        prompt.append("\nTHEME 2 (Macroeconomics):\n");
        for (String topic : topics) {
            if (topic.startsWith("2.")) {
                prompt.append("- ").append(topic).append("\n");
            }
        }
        
        prompt.append("\nTHEME 3 (Business economics):\n");
        for (String topic : topics) {
            if (topic.startsWith("3.")) {
                prompt.append("- ").append(topic).append("\n");
            }
        }
        
        prompt.append("\nTHEME 4 (Global economics):\n");
        for (String topic : topics) {
            if (topic.startsWith("4.")) {
                prompt.append("- ").append(topic).append("\n");
            }
        }

        prompt.append("\nRespond with the most relevant topics, separated by commas. Be precise and consistent.");

        return prompt.toString();
    }

    /**
     * Parse multiple topic assignments with improved validation
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
                    String[] topics = parseTopicsString(topicsString);
                    
                    if (topics.length > 0) {
                        // Limit topics based on realistic expectations
                        if (topics.length > TopicConstants.MAX_TOPICS_PER_QUESTION) {
                            String[] limitedTopics = new String[TopicConstants.MAX_TOPICS_PER_QUESTION];
                            System.arraycopy(topics, 0, limitedTopics, 0, TopicConstants.MAX_TOPICS_PER_QUESTION);
                            topics = limitedTopics;
                        }
                        
                        results.put(questionNum, topics);
                        System.out.println("Successfully parsed " + topics.length + " topics for question " + questionNum);
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
     * Parse topics string with better cleaning
     */
    private String[] parseTopicsString(String topicsString) {
        if (topicsString == null || topicsString.trim().isEmpty()) {
            return new String[0];
        }
        
        // Clean the topics string
        String cleaned = topicsString
                .replaceAll("[\"'()]", "")
                .replaceAll("(?i)\\b(topics?:?|the topics? (?:are?|is):?)\\s*", "")
                .trim();
        
        // Split by comma, semicolon, or "and"
        String[] rawTopics = cleaned.split("\\s*[,;]\\s*|\\s+and\\s+");
        
        List<String> validTopics = new ArrayList<>();
        
        for (String rawTopic : rawTopics) {
            String topic = rawTopic.trim();
            if (!topic.isEmpty() && topic.length() > 5) { // Must be substantial
                // Validate it looks like a proper topic code
                if (topic.matches("^\\d+\\.\\d+\\.\\d+\\s+.+")) {
                    validTopics.add(topic);
                }
            }
        }
        
        return validTopics.toArray(new String[0]);
    }

    /**
     * Parse topics from AI response with improved validation
     */
    private String[] parseTopicsFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new String[0];
        }

        // Clean the response
        String cleanedResponse = response.trim()
                .replaceAll("[\"'()]", "")
                .replaceAll("(?i)\\b(topics?:?|the topics? (?:are?|is):?|relevant topics?:?)\\s*", "")
                .trim();

        return parseTopicsString(cleanedResponse);
    }
}