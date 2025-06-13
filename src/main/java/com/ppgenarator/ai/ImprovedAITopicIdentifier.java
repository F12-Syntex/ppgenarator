package com.ppgenarator.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.openai.models.ChatModel;
import com.ppgenarator.concurrent.ParallelProcessor;
import com.ppgenerator.types.Question;

public class ImprovedAITopicIdentifier {

    private static final ChatModel OPENAI_MODEL = ChatModel.GPT_4_1_MINI;
    private final String[] topics;
    private final String qualification;
    private final int paper;
    private final Map<String, String[]> cache = new ConcurrentHashMap<>();

    public ImprovedAITopicIdentifier(String[] topics, String qualification, int paper) {
        this.topics = getRelevantTopicsForQualificationAndPaper(topics, qualification, paper);
        this.qualification = qualification;
        this.paper = paper;
    }

    /**
     * Parallel batch processing with improved accuracy
     */
    public Map<Integer, String[]> identifyTopicsForBatch(List<Question> questions) {
        if (questions.isEmpty()) {
            return new HashMap<>();
        }

        System.out.println("Processing " + questions.size() + " questions in parallel with "
                + Runtime.getRuntime().availableProcessors() + " cores");

        // Process questions in parallel chunks
        int chunkSize = Math.max(1, questions.size() / (Runtime.getRuntime().availableProcessors() * 2));
        List<List<Question>> chunks = partitionList(questions, chunkSize);

        List<CompletableFuture<Map<Integer, String[]>>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(()
                -> processChunk(chunk, questions.indexOf(chunk.get(0))), ParallelProcessor.getExecutor()))
                .collect(Collectors.toList());

        // Combine results
        Map<Integer, String[]> allResults = new HashMap<>();
        for (CompletableFuture<Map<Integer, String[]>> future : futures) {
            allResults.putAll(future.join());
        }

        return allResults;
    }

    private Map<Integer, String[]> processChunk(List<Question> chunk, int startIndex) {
        Map<Integer, String[]> results = new HashMap<>();

        try {
            StringBuilder batchPrompt = createEnhancedBatchPrompt(chunk, startIndex);
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String response = openAI.query(batchPrompt.toString());

            System.out.println("AI Response for chunk starting at " + startIndex + ":\n" + response + "\n");

            Map<Integer, String[]> chunkResults = parseMultipleTopicAssignments(response, chunk.size(), startIndex);

            // Validate and improve results
            for (int i = 0; i < chunk.size(); i++) {
                int questionIndex = startIndex + i + 1;
                Question question = chunk.get(i);

                if (chunkResults.containsKey(questionIndex)) {
                    String[] topics = validateAndImproveTopics(chunkResults.get(questionIndex), question);
                    results.put(questionIndex, topics);
                } else {
                    // Fallback to individual processing
                    String[] topics = identifyTopicsForSingleQuestion(question.getQuestionText());
                    results.put(questionIndex, topics);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in chunk processing: " + e.getMessage());
            // Fallback to individual processing for this chunk
            for (int i = 0; i < chunk.size(); i++) {
                int questionIndex = startIndex + i + 1;
                String[] topics = identifyTopicsForSingleQuestion(chunk.get(i).getQuestionText());
                results.put(questionIndex, topics);
            }
        }

        return results;
    }

    private StringBuilder createEnhancedBatchPrompt(List<Question> questions, int startIndex) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert A-level Economics examiner specializing in the Edexcel specification. ");
        prompt.append("Your task is to categorize exam questions with EXTREME PRECISION.\n\n");

        // Add qualification and paper context
        if (qualification != null && paper > 0) {
            prompt.append("CRITICAL CONTEXT: You are categorizing questions for ")
                    .append(qualification.toUpperCase()).append(" PAPER ").append(paper).append(".\n");
            prompt.append(getPaperContext(qualification, paper)).append("\n\n");
        }

        prompt.append("SPECIFICATION TOPICS FOR THIS PAPER:\n");
        for (String topic : topics) {
            prompt.append("- ").append(topic).append("\n");
        }
        prompt.append("\n");

        prompt.append("ENHANCED CATEGORIZATION RULES:\n");
        prompt.append("1. READ EACH QUESTION WORD BY WORD - ignore instructional phrases like 'using the data'\n");
        prompt.append("2. IDENTIFY THE CORE ECONOMIC CONCEPT being tested\n");
        prompt.append("3. MATCH to the EXACT specification code (e.g., '1.1.2 Positive and normative economic statements')\n");
        prompt.append("4. MAXIMUM 3 topics per question - be selective and precise\n");
        prompt.append("5. ONLY use topics from the provided list for this specific paper\n");
        prompt.append("6. For ambiguous questions, choose the MOST SPECIFIC relevant topic\n\n");

        prompt.append("CRITICAL EXAMPLES FOR ACCURACY:\n");
        prompt.append("- 'State what is meant by a positive economic statement' → '1.1.2 Positive and normative economic statements'\n");
        prompt.append("- 'Calculate the price elasticity of demand' → '1.2.3 Price, income and cross elasticities of demand'\n");
        prompt.append("- 'Explain how market failure occurs' → '1.3.1 Types of market failure'\n");
        prompt.append("- 'Evaluate government intervention policies' → '1.4.1 Government intervention in markets'\n\n");

        // Add specific examples for this paper type
        prompt.append(getPaperSpecificExamples(qualification, paper));

        prompt.append("\nQUESTIONS TO CATEGORIZE:\n");
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            prompt.append("QUESTION ").append(startIndex + i + 1).append(":\n");
            prompt.append(cleanQuestionText(question.getQuestionText())).append("\n\n");
        }

        prompt.append("RESPONSE FORMAT:\n");
        prompt.append("For each question, respond EXACTLY as: 'Question X: [topic codes separated by commas]'\n");
        prompt.append("Example: 'Question 1: 1.1.2 Positive and normative economic statements'\n");
        prompt.append("BE PRECISE. ONLY use the exact topic codes provided. Maximum 3 topics per question.");

        return prompt;
    }

    private String cleanQuestionText(String questionText) {
        if (questionText == null) {
            return "";
        }

        // Remove common instructional phrases that don't indicate topic
        String cleaned = questionText;
        for (String phrase : TopicConstants.IGNORE_PHRASES) {
            cleaned = cleaned.replaceAll("(?i)" + Pattern.quote(phrase), "");
        }

        // Remove excessive whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // Limit length for API efficiency
        if (cleaned.length() > 800) {
            cleaned = cleaned.substring(0, 800) + "...";
        }

        return cleaned;
    }

    private String[] validateAndImproveTopics(String[] suggestedTopics, Question question) {
        List<String> validatedTopics = new ArrayList<>();
        String questionText = question.getQuestionText().toLowerCase();

        for (String topic : suggestedTopics) {
            if (topic == null || topic.trim().isEmpty()) {
                continue;
            }

            // Validate topic exists in our specification
            boolean isValidTopic = Arrays.stream(topics)
                    .anyMatch(validTopic -> validTopic.equalsIgnoreCase(topic.trim()));

            if (isValidTopic) {
                // Additional validation based on question content
                if (isTopicRelevantToQuestion(topic.trim(), questionText)) {
                    validatedTopics.add(topic.trim());
                }
            }
        }

        // If no valid topics found, use keyword-based fallback
        if (validatedTopics.isEmpty()) {
            validatedTopics.addAll(getKeywordBasedTopics(questionText));
        }

        // Ensure we don't exceed maximum topics
        if (validatedTopics.size() > TopicConstants.MAX_TOPICS_PER_QUESTION) {
            validatedTopics = validatedTopics.subList(0, TopicConstants.MAX_TOPICS_PER_QUESTION);
        }

        return validatedTopics.toArray(new String[0]);
    }

    private boolean isTopicRelevantToQuestion(String topic, String questionText) {
        // Enhanced relevance checking
        Map<String, String[]> topicKeywords = getTopicSpecificKeywords();

        if (topicKeywords.containsKey(topic)) {
            String[] keywords = topicKeywords.get(topic);
            for (String keyword : keywords) {
                if (questionText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        // Special validation for commonly confused topics
        if (topic.equals("1.1.2 Positive and normative economic statements")) {
            return questionText.contains("positive") || questionText.contains("normative")
                    || questionText.contains("value judgement") || questionText.contains("opinion")
                    || questionText.contains("fact") || questionText.contains("objective")
                    || questionText.contains("subjective");
        }

        return true; // Default to true if no specific validation
    }

    private List<String> getKeywordBasedTopics(String questionText) {
        List<String> topics = new ArrayList<>();
        Map<String, String[]> topicKeywords = getTopicSpecificKeywords();

        for (Map.Entry<String, String[]> entry : topicKeywords.entrySet()) {
            String topic = entry.getKey();
            String[] keywords = entry.getValue();

            int matchCount = 0;
            for (String keyword : keywords) {
                if (questionText.toLowerCase().contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }

            // Require at least 2 keyword matches for keyword-based assignment
            if (matchCount >= 2) {
                topics.add(topic);
            }
        }

        return topics;
    }

    private Map<String, String[]> getTopicSpecificKeywords() {
        Map<String, String[]> keywords = new HashMap<>();

        // Enhanced keyword mapping
        keywords.put("1.1.2 Positive and normative economic statements",
                new String[]{"positive statement", "normative statement", "value judgement", "objective", "subjective", "fact", "opinion", "should", "ought"});

        keywords.put("1.2.3 Price, income and cross elasticities of demand",
                new String[]{"elasticity", "ped", "yed", "xed", "elastic", "inelastic", "price elasticity", "income elasticity"});

        keywords.put("1.3.1 Types of market failure",
                new String[]{"market failure", "allocative efficiency", "productive efficiency", "externality", "public good", "monopoly power"});

        keywords.put("1.4.1 Government intervention in markets",
                new String[]{"government intervention", "regulation", "tax", "subsidy", "price control", "maximum price", "minimum price"});

        // Add more keyword mappings for all topics...
        return keywords;
    }

    // Helper methods for parallel processing
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }

    private String[] getRelevantTopicsForQualificationAndPaper(String[] allTopics, String qualification, int paper) {
        // Implementation from original class but with parallel processing
        return Arrays.stream(allTopics)
                .parallel()
                .filter(topic -> {
                    String theme = TopicConstants.getThemeFromTopic(topic);
                    return isTopicRelevantForQualificationAndPaper(theme, qualification, paper);
                })
                .toArray(String[]::new);
    }

    private boolean isTopicRelevantForQualificationAndPaper(String theme, String qualification, int paper) {
        if (qualification == null) {
            return true;
        }

        String normalizedQual = qualification.toLowerCase();

        if (normalizedQual.contains("as")) {
            switch (paper) {
                case 1:
                    return "Theme 1".equals(theme);
                case 2:
                    return "Theme 2".equals(theme);
                default:
                    return "Theme 1".equals(theme) || "Theme 2".equals(theme);
            }
        } else if (normalizedQual.contains("a level") || normalizedQual.contains("alevel")) {
            switch (paper) {
                case 1:
                    return "Theme 1".equals(theme) || "Theme 3".equals(theme);
                case 2:
                    return "Theme 2".equals(theme) || "Theme 4".equals(theme);
                case 3:
                    return "Theme 1".equals(theme) || "Theme 2".equals(theme)
                            || "Theme 3".equals(theme) || "Theme 4".equals(theme);
                default:
                    return "Theme 3".equals(theme) || "Theme 4".equals(theme);
            }
        }

        return true;
    }

    public String[] identifyTopicsForSingleQuestion(String cleanedText) {
        // Check cache first
        String cacheKey = cleanedText.hashCode() + "_" + qualification + "_" + paper;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        try {
            String prompt = createEnhancedSingleQuestionPrompt(cleanedText);
            OpenAiService openAI = new OpenAiService(OPENAI_MODEL, 0.1);
            String response = openAI.query(prompt);

            String[] topics = parseTopicsFromResponse(response);
            String[] validatedTopics = validateAndImproveTopics(topics,
                    new Question() {
                {
                    setQuestionText(cleanedText);
                }
            });

            // Cache the result
            cache.put(cacheKey, validatedTopics);

            return validatedTopics;

        } catch (Exception e) {
            System.err.println("Error in single question AI processing: " + e.getMessage());
            return getKeywordBasedTopics(cleanedText).toArray(new String[0]);
        }
    }

    private String createEnhancedSingleQuestionPrompt(String cleanedText) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert A-level Economics examiner. Analyze this question with EXTREME PRECISION.\n\n");

        if (qualification != null && paper > 0) {
            prompt.append("CONTEXT: ").append(qualification.toUpperCase())
                    .append(" PAPER ").append(paper).append("\n");
            prompt.append(getPaperContext(qualification, paper)).append("\n\n");
        }

        prompt.append("QUESTION:\n").append(cleanedText).append("\n\n");

        prompt.append("AVAILABLE TOPICS FOR THIS PAPER:\n");
        for (String topic : topics) {
            prompt.append("- ").append(topic).append("\n");
        }
        prompt.append("\n");

        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Identify the EXACT economic concept being tested\n");
        prompt.append("2. Match to the MOST SPECIFIC topic from the list\n");
        prompt.append("3. Maximum 2 topics\n");
        prompt.append("4. ONLY use topics from the provided list\n");
        prompt.append("5. Be extremely precise - avoid generic assignments\n\n");

        prompt.append("Respond with only the topic codes, separated by commas.");

        return prompt.toString();
    }

    // Additional helper methods...
    private String getPaperContext(String qualification, int paper) {
        // Implementation from original class
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
        // Enhanced examples for better accuracy
        StringBuilder examples = new StringBuilder();
        String normalizedQual = qualification != null ? qualification.toLowerCase() : "";

        if (normalizedQual.contains("as")) {
            switch (paper) {
                case 1:
                    examples.append("CRITICAL AS Paper 1 Examples:\n");
                    examples.append("- 'Define a positive economic statement' → '1.1.2 Positive and normative economic statements'\n");
                    examples.append("- 'Calculate PED using the data' → '1.2.3 Price, income and cross elasticities of demand'\n");
                    examples.append("- 'Explain market failure in this market' → '1.3.1 Types of market failure'\n");
                    break;
                case 2:
                    examples.append("CRITICAL AS Paper 2 Examples:\n");
                    examples.append("- 'Calculate unemployment rate' → '2.1.3 Employment and unemployment'\n");
                    examples.append("- 'Explain economic growth measurement' → '2.1.1 Economic growth'\n");
                    examples.append("- 'Analyze fiscal policy effects' → '2.6.2 Demand-side policies'\n");
                    break;
            }
        }

        return examples.toString();
    }

    private Map<Integer, String[]> parseMultipleTopicAssignments(String response, int expectedCount, int startIndex) {
        Map<Integer, String[]> results = new HashMap<>();

        Pattern pattern = Pattern.compile("(?:Question|QUESTION)\\s+(\\d+)\\s*:\\s*([^\\n\\r]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            try {
                int questionNum = Integer.parseInt(matcher.group(1));
                String topicsString = matcher.group(2).trim();

                if (questionNum > 0 && questionNum <= expectedCount) {
                    String[] topics = parseTopicsList(topicsString);
                    if (topics.length > 0) {
                        results.put(questionNum, topics);
                        System.out.println("Parsed Question " + questionNum + ": " + Arrays.toString(topics));
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse question number from: " + matcher.group(1));
            }
        }

        return results;
    }

    private String[] parseTopicsList(String topicsString) {
        if (topicsString == null || topicsString.trim().isEmpty()) {
            return new String[0];
        }

        // Split by common delimiters
        String[] topics = topicsString.split("\\s*[,;]\\s*|\\s+and\\s+");
        List<String> cleanedTopics = new ArrayList<>();

        for (String topic : topics) {
            String cleaned = topic.trim()
                    .replaceAll("^[\"'.-]+|[\"'.-]+$", "") // Remove quotes and punctuation
                    .replaceAll("(?i)^(the\\s+)?", ""); // Remove "the" prefix

            if (!cleaned.isEmpty() && cleaned.length() > 3) {
                cleanedTopics.add(cleaned);
            }
        }

        return cleanedTopics.toArray(new String[0]);
    }

    private String[] parseTopicsFromResponse(String response) {
        if (response == null) {
            return new String[0];
        }

        String cleaned = response.trim()
                .replaceAll("[\"'.]", "")
                .replaceAll("(?i)Topics?:?\\s*", "")
                .replaceAll("(?i)The topics? (?:are?|is):?\\s*", "");

        return parseTopicsList(cleaned);
    }
}
