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
     * Identify topics for a batch of questions with strict topic limitation
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
     * Identify topics for a single question with strict limitation
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
                "You are an expert A-level Economics examiner who specializes in categorizing exam questions based on their CORE economic concepts.\n\n");
        batchPrompt.append("I'll provide you with ").append(questions.size()).append(" economics exam questions.\n");
        batchPrompt.append(
                "For each question, determine the PRIMARY economic concept being tested, and ONLY add secondary topics if they are absolutely essential.\n\n");

        batchPrompt.append("STRICT INSTRUCTIONS:\n");
        batchPrompt.append(
                "1. IGNORE phrases like 'using the data from the extract' or 'refer to the figure' - these are just exam instructions\n");
        batchPrompt
                .append("2. Focus on the MAIN economic concept being tested - what is the question primarily about?\n");
        batchPrompt.append("3. Only add a second topic if the question equally tests TWO distinct concepts\n");
        batchPrompt.append("4. MAXIMUM 3 topics per question, but aim for 1-2 topics in most cases\n");
        batchPrompt.append("5. Be VERY selective - only include topics that are central to answering the question\n");
        batchPrompt.append("6. Select topics ONLY from this list:\n");
        batchPrompt.append(String.join(", ", topics)).append("\n\n");

        batchPrompt.append("EXAMPLES:\n");
        batchPrompt.append(
                "- Question about macroeconomic policies to reduce negative effects of globalisation → Topics: globalisation, fiscal policy (TWO topics because it's about policies addressing globalisation)\n");
        batchPrompt.append(
                "- Question about calculating price elasticity of demand → Topics: elasticity (ONE topic - it's purely about elasticity)\n");
        batchPrompt.append(
                "- Question about monopoly profit maximization → Topics: monopoly (ONE topic - profit is part of monopoly theory)\n");
        batchPrompt.append(
                "- Question about government intervention to correct market failure → Topics: market failure, government intervention (TWO topics because both are central)\n\n");

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            batchPrompt.append("QUESTION ").append(i + 1).append(":\n");
            batchPrompt.append(question.getQuestionText()).append("\n\n");
        }

        batchPrompt.append(
                "For each question, respond with the question number and the most relevant topics (1-3 maximum, prefer fewer).\n");
        batchPrompt.append(
                "Format: 'Question 1: topic1' OR 'Question 1: topic1, topic2' (only if both are equally important)\n");
        batchPrompt.append("CRITICAL: Be strict and selective. Most questions should have only 1-2 topics.");

        return batchPrompt;
    }

    private String createSingleQuestionPrompt(String cleanedText) {
        return String.format(
                "You are an expert A-level Economics examiner. Analyze this economics exam question to identify the PRIMARY economic concept being tested.\n\n"
                        +
                        "QUESTION:\n%s\n\n" +
                        "INSTRUCTIONS:\n" +
                        "1. IGNORE contextual elements like 'refer to the extract' or 'using the data' - focus on the CORE economic concept\n"
                        +
                        "2. What is the MAIN economic knowledge area a student needs to answer this question?\n" +
                        "3. Only include secondary topics if they are absolutely essential to answering the question\n"
                        +
                        "4. Maximum 3 topics, but prefer 1-2 topics\n" +
                        "5. Select ONLY from this list:\n%s\n\n" +
                        "Examples:\n" +
                        "- Question asking to calculate PED → Topic: elasticity\n" +
                        "- Question about policies to address globalisation → Topics: globalisation, fiscal policy\n" +
                        "- Question about monopoly pricing → Topic: monopoly\n" +
                        "- Question about market failure and government response → Topics: market failure, government intervention\n\n"
                        +
                        "Respond with the most relevant topic(s), separated by commas if multiple. Be strict and selective.",
                cleanedText, String.join(", ", topics));
    }

    /**
     * Parse multiple topic assignments from the AI response
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
                .replaceAll("(?i)Primary topics?:?\\s*", "");

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