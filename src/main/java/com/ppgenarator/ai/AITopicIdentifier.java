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

    public AITopicIdentifier(String[] topics) {
        this.topics = topics;
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
                "You are an expert A-level Economics examiner who specializes in categorizing exam questions based on their economic concepts.\n\n");
        batchPrompt.append("I'll provide you with ").append(questions.size()).append(" economics exam questions.\n");
        batchPrompt.append(
                "For each question, identify ALL relevant economic topics that could be applied or tested, even if they are secondary concepts.\n\n");

        batchPrompt.append("ENHANCED INSTRUCTIONS:\n");
        batchPrompt.append(
                "1. IGNORE phrases like 'using the data from the extract' or 'refer to the figure' - these are just exam instructions\n");
        batchPrompt.append("2. Include the PRIMARY topic that is being directly tested\n");
        batchPrompt.append("3. Include SECONDARY topics that are relevant or could be applied to answer the question\n");
        batchPrompt.append("4. Include topics where knowledge would be helpful even if not directly tested\n");
        batchPrompt.append("5. MAXIMUM 4 topics per question, aim for 2-3 topics for most questions\n");
        batchPrompt.append("6. Be more inclusive - if a topic is reasonably related, include it\n");
        batchPrompt.append("7. Consider cross-connections between topics (e.g., government policies affecting markets)\n");
        batchPrompt.append("8. Select topics ONLY from this list:\n");
        batchPrompt.append(String.join(", ", topics)).append("\n\n");

        batchPrompt.append("ENHANCED EXAMPLES:\n");
        batchPrompt.append(
                "- Question about calculating price elasticity and its impact on revenue → Topics: elasticity, revenue, demand and supply\n");
        batchPrompt.append(
                "- Question about monopoly profit maximization and welfare effects → Topics: monopoly, profit, market failure, consumer and producer surplus\n");
        batchPrompt.append(
                "- Question about government intervention to reduce inequality → Topics: government intervention, poverty and inequality, fiscal policy\n");
        batchPrompt.append(
                "- Question about effects of trade liberalization on developing countries → Topics: trade liberalization, developing economies, international economics, globalisation\n");
        batchPrompt.append(
                "- Question about environmental policies and sustainability → Topics: sustainability, government intervention, externalities, market failure\n");
        batchPrompt.append(
                "- Question about labor market regulations → Topics: labor market, government intervention, wage determination, market failure\n\n");

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
            batchPrompt.append(question.getQuestionText()).append("\n\n");
        }

        batchPrompt.append(
                "For each question, respond with the question number and ALL relevant topics (2-4 topics preferred).\n");
        batchPrompt.append(
                "Format: 'Question 1: topic1, topic2, topic3' (include multiple topics when relevant)\n");
        batchPrompt.append("CRITICAL: Be inclusive and comprehensive. Most questions should have 2-3 topics for better learning coverage.");

        return batchPrompt;
    }

    private String createSingleQuestionPrompt(String cleanedText) {
        return String.format(
                "You are an expert A-level Economics examiner. Analyze this economics exam question to identify ALL relevant economic concepts.\n\n"
                        +
                        "QUESTION:\n%s\n\n" +
                        "ENHANCED INSTRUCTIONS:\n" +
                        "1. IGNORE contextual elements like 'refer to the extract' or 'using the data' - focus on the CORE economic concepts\n"
                        +
                        "2. What is the PRIMARY economic knowledge area a student needs to answer this question?\n" +
                        "3. What SECONDARY topics are relevant or could be applied to answer the question?\n" +
                        "4. Include topics where knowledge would be helpful even if not directly tested\n" +
                        "5. Maximum 4 topics, aim for 2-3 topics for comprehensive coverage\n" +
                        "6. Be inclusive - if a topic is reasonably related, include it\n" +
                        "7. Consider cross-connections between economic concepts\n" +
                        "8. Select ONLY from this list:\n%s\n\n" +
                        "Enhanced Examples:\n" +
                        "- Question asking to calculate PED and discuss revenue implications → Topics: elasticity, revenue, demand and supply\n" +
                        "- Question about monopoly pricing and social welfare → Topics: monopoly, market failure, consumer and producer surplus\n" +
                        "- Question about policies to address globalisation effects → Topics: globalisation, government intervention, fiscal policy, developing economies\n" +
                        "- Question about environmental regulation → Topics: externalities, government intervention, market failure, sustainability\n\n"
                        +
                        "Respond with ALL relevant topics, separated by commas. Be comprehensive and inclusive.",
                cleanedText, String.join(", ", topics));
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