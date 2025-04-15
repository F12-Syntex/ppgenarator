package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ppgenerator.types.ExamBoard;
import com.ppgenerator.types.Question;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

public class Categorize {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int BATCH_SIZE = 10; // Process questions in batches to reduce API calls

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final File outputFolder;
    private boolean dynamicTopics;

    // Primary topics that should be used if specified
    String[] topics = {};

    // Optional topics that the AI should try to include if applicable
    String[] optionalTopics = {"calculation"};

    public Categorize(File outputFolder) {
        this.outputFolder = outputFolder;
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Determine if we should use dynamic topics (when topics array is empty)
        this.dynamicTopics = (topics == null || topics.length == 0);

        // Create output folder if it doesn't exist
        if (!this.outputFolder.exists()) {
            this.outputFolder.mkdirs();
        }
    }

    // Constructor with the ability to set topics and optional topics
    public Categorize(File outputFolder, String[] topics, String[] optionalTopics) {
        this(outputFolder);
        if (topics != null) {
            this.topics = topics;
        }
        if (optionalTopics != null) {
            this.optionalTopics = optionalTopics;
        }
        this.dynamicTopics = (this.topics == null || this.topics.length == 0);
    }

    public void processQuestions(List<Question> questions) throws JSONException {
        if (questions == null || questions.isEmpty()) {
            System.out.println("No questions to process.");
            return;
        }

        System.out.println("Processing " + questions.size() + " questions...");
        System.out.println("Mode: " + (dynamicTopics ? "Dynamic Topics" : "Fixed Topics"));

        if (!dynamicTopics) {
            System.out.println("Using fixed topics: " + String.join(", ", topics));
        }

        if (optionalTopics != null && optionalTopics.length > 0) {
            System.out.println("Optional topics: " + String.join(", ", optionalTopics));
        }

        // Group questions by year
        Map<String, List<Question>> questionsByYear = groupQuestionsByYear(questions);

        // Process each year's questions
        for (Map.Entry<String, List<Question>> entry : questionsByYear.entrySet()) {
            String year = entry.getKey();
            List<Question> yearQuestions = entry.getValue();

            File outputFile = new File(outputFolder, year + ".json");
            if (outputFile.exists()) {
                System.out.println("JSON file already exists for " + year + ". Skipping processing.");
                continue;
            }

            System.out.println("Processing " + yearQuestions.size() + " questions for year " + year);

            // Load question content in batches to optimize API usage
            processQuestionBatches(yearQuestions);

            // Export to JSON
            exportQuestionsToJson(yearQuestions, outputFile);
        }
    }

    private Map<String, List<Question>> groupQuestionsByYear(List<Question> questions) {
        Map<String, List<Question>> questionsByYear = new HashMap<>();

        for (Question question : questions) {
            String year = question.getYear();
            if (!questionsByYear.containsKey(year)) {
                questionsByYear.put(year, new ArrayList<>());
            }
            questionsByYear.get(year).add(question);
        }

        return questionsByYear;
    }

    private void processQuestionBatches(List<Question> questions) {
        // First load all question content
        for (Question question : questions) {
            loadQuestionText(question);
        }

        // If we're using dynamic topics, first get suggestions for a single batch to
        // establish a baseline
        if (dynamicTopics) {
            // Take a sample of questions to get suggested topics
            List<Question> sampleQuestions = new ArrayList<>();
            for (int i = 0; i < Math.min(5, questions.size()); i++) {
                sampleQuestions.add(questions.get(i));
            }
            getSuggestedTopics(sampleQuestions);
        }

        // Then process in batches for topic identification
        for (int i = 0; i < questions.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, questions.size());
            List<Question> batch = questions.subList(i, endIndex);

            System.out.println("Processing batch of " + batch.size() + " questions for topic identification");
            identifyTopicsForBatch(batch);
        }
    }

    private void loadQuestionText(Question question) {
        // Skip if question text is already loaded
        if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
            return;
        }

        System.out.println(
                "Loading question content for: " + question.getQuestionNumber() + " from year " + question.getYear());

        if (question.getQuestion() != null) {
            String questionContent = extractTextFromPDF(question.getQuestion());
            questionContent = cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);
        }
    }

    private Set<String> getSuggestedTopics(List<Question> sampleQuestions) {
        if (!dynamicTopics) {
            return new HashSet<>(Arrays.asList(topics));
        }

        Set<String> suggestedTopics = new HashSet<>();

        // Add optional topics as suggestions
        if (optionalTopics != null && optionalTopics.length > 0) {
            suggestedTopics.addAll(Arrays.asList(optionalTopics));
        }

        try {
            // Prepare a prompt for topic suggestion
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder
                    .append("You are analyzing a set of exam questions to determine appropriate topic categories. ");
            promptBuilder
                    .append("Based on the following sample questions, suggest 5-10 main topics that would be useful ");
            promptBuilder
                    .append("for categorizing these questions and similar ones. Focus on identifying subject areas ");
            promptBuilder.append("that would be meaningful for study purposes.\n\n");

            promptBuilder.append("If these optional topics are relevant, please include them: ");
            promptBuilder.append(String.join(", ", optionalTopics));
            promptBuilder.append("\n\n");

            promptBuilder.append("Sample questions:\n\n");

            for (int i = 0; i < sampleQuestions.size(); i++) {
                Question question = sampleQuestions.get(i);
                promptBuilder.append("Sample Question ").append(i + 1).append(":\n");
                promptBuilder.append(question.getQuestionText()).append("\n\n");
            }

            promptBuilder.append(
                    "Please provide your response ONLY as a JSON array of strings containing the suggested topics. ");
            promptBuilder.append("Example response format: [\"topic1\", \"topic2\", \"topic3\"]\n");

            OpenAI openAI = new OpenAI(OPENAI_MODEL, 0.7); // Use higher temperature for creativity
            String response = openAI.query(promptBuilder.toString());

            JSONArray topicsArray = OpenAI.parseJsonArrayResponse(response);
            if (topicsArray != null) {
                for (int i = 0; i < topicsArray.length(); i++) {
                    suggestedTopics.add(topicsArray.getString(i).toLowerCase());
                }

                System.out.println("AI suggested topics: " + String.join(", ", suggestedTopics));
            }
        } catch (Exception e) {
            System.err.println("Error getting suggested topics: " + e.getMessage());
            e.printStackTrace();
        }

        // Always include "other" as a fallback
        suggestedTopics.add("other");

        return suggestedTopics;
    }

    private void identifyTopicsForBatch(List<Question> questions) {
        // Filter out questions that already have topics
        List<Question> questionsNeedingTopics = questions.stream()
                .filter(q -> q.getTopics() == null || q.getTopics().length == 0)
                .filter(q -> q.getQuestionText() != null && !q.getQuestionText().isEmpty())
                .collect(Collectors.toList());

        if (questionsNeedingTopics.isEmpty()) {
            return;
        }

        try {
            // For small batches, process individually
            if (questionsNeedingTopics.size() <= 3) {
                for (Question question : questionsNeedingTopics) {
                    identifyTopicsForSingleQuestion(question);
                }
                return;
            }

            // For larger batches, combine questions into a single prompt
            StringBuilder batchPrompt = new StringBuilder();
            batchPrompt.append("Analyze each of the following questions and determine the main topic it covers.\n\n");

            if (!dynamicTopics) {
                // Use fixed topics
                batchPrompt.append("TOPICS: ").append(String.join(", ", topics)).append("\n\n");
                batchPrompt.append(
                        "For each question, respond with ONLY the question number and the SINGLE most appropriate topic from the list.\n\n");
            } else {
                // Allow AI to suggest topics, but mention optional topics as guidance
                batchPrompt.append(
                        "You may choose any relevant topic name, but consider these suggested topics if applicable: ");
                batchPrompt.append(String.join(", ", optionalTopics)).append("\n\n");
                batchPrompt.append(
                        "For each question, identify ONE main topic that best describes what the question is testing.\n\n");
            }

            for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                Question question = questionsNeedingTopics.get(i);
                batchPrompt.append("QUESTION ").append(i + 1).append(" (ID: ").append(question.getQuestionNumber())
                        .append("):\n");
                batchPrompt.append(question.getQuestionText()).append("\n\n");
            }

            batchPrompt.append(
                    "Respond with a JSON array where each element is an object with 'id' and 'topic' fields. Example format:");
            batchPrompt.append(
                    "\n[{\"id\": \"1\", \"topic\": \"business growth\"}, {\"id\": \"2\", \"topic\": \"other\"}]");

            OpenAI openAI = new OpenAI(OPENAI_MODEL, dynamicTopics ? 0.5 : 0.3);
            String response = openAI.query(batchPrompt.toString());

            JSONArray responsesArray = OpenAI.parseJsonArrayResponse(response);
            if (responsesArray != null) {
                // Map the response back to the questions
                Map<String, String> topicMap = new HashMap<>();
                for (int i = 0; i < responsesArray.length(); i++) {
                    JSONObject obj = responsesArray.getJSONObject(i);
                    String id = obj.getString("id");
                    String topic = obj.getString("topic").toLowerCase();
                    topicMap.put(id, topic);
                }

                // Assign topics to questions
                for (int i = 0; i < questionsNeedingTopics.size(); i++) {
                    Question question = questionsNeedingTopics.get(i);
                    String id = String.valueOf(i + 1);
                    if (topicMap.containsKey(id)) {
                        String topic = topicMap.get(id);

                        // Validate topic against the list if not using dynamic topics
                        if (!dynamicTopics) {
                            boolean validTopic = false;
                            for (String allowedTopic : topics) {
                                if (topic.equalsIgnoreCase(allowedTopic)) {
                                    validTopic = true;
                                    break;
                                }
                            }

                            if (!validTopic) {
                                topic = "other";
                            }
                        }

                        question.setTopics(new String[] { topic });
                    } else {
                        question.setTopics(new String[] { "other" });
                    }
                }
            } else {
                // Fallback to individual processing
                for (Question question : questionsNeedingTopics) {
                    identifyTopicsForSingleQuestion(question);
                }
            }
        } catch (Exception e) {
            System.err.println("Error identifying topics for batch: " + e.getMessage());
            e.printStackTrace();

            // Fallback to individual processing
            for (Question question : questionsNeedingTopics) {
                identifyTopicsForSingleQuestion(question);
            }
        }
    }

    private void identifyTopicsForSingleQuestion(Question question) {
        try {
            String prompt;

            if (!dynamicTopics) {
                // Use fixed topics
                prompt = String.format(
                        "Analyze this %s question and identify the SINGLE main topic it covers. " +
                                "Choose from ONLY these topics: %s. " +
                                "Provide your answer as a single string with only the topic name: \n\n%s",
                        question.getBoard(), String.join(", ", topics), question.getQuestionText());
            } else {
                // Allow AI to suggest topics but mention optional topics
                prompt = String.format(
                        "Analyze this %s question and identify the SINGLE main topic it covers. " +
                                "You can choose any appropriate topic name, but consider these suggested topics if relevant: %s. "
                                +
                                "Provide your answer as a single string with only the topic name: \n\n%s",
                        question.getBoard(), String.join(", ", optionalTopics), question.getQuestionText());
            }

            OpenAI openAI = new OpenAI(OPENAI_MODEL, dynamicTopics ? 0.5 : 0.3);
            String response = openAI.query(prompt);

            // Clean up the response
            response = response.trim().replaceAll("\"", "").toLowerCase();

            if (!dynamicTopics) {
                // Validate that response is in our topics list
                boolean validTopic = false;
                for (String topic : topics) {
                    if (response.equalsIgnoreCase(topic)) {
                        validTopic = true;
                        break;
                    }
                }

                if (validTopic) {
                    question.setTopics(new String[] { response });
                } else {
                    // Default if not identified
                    question.setTopics(new String[] { "other" });
                }
            } else {
                // When using dynamic topics, accept whatever the AI suggests
                question.setTopics(new String[] { response });
            }
        } catch (Exception e) {
            System.err.println(
                    "Error identifying topic for question " + question.getQuestionNumber() + ": " + e.getMessage());
            e.printStackTrace();
            question.setTopics(new String[] { "other" });
        }
    }

    private String cleanQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove page numbers/identifiers
        text = text.replaceAll("\\*P\\d+A\\d+\\*", "");

        // Remove "DO NOT WRITE IN THIS AREA" and variations
        text = text.replaceAll(
                "D\\s*O\\s*N\\s*O\\s*T\\s*W\\s*R\\s*I\\s*T\\s*E\\s*I\\s*N\\s*T\\s*H\\s*I\\s*S\\s*A\\s*R\\s*E\\s*A", "");

        // Remove repeated dots (common in exam papers for fill-in spaces)
        text = text.replaceAll("\\.{2,}", " ");

        // Replace sequences of dots and spaces with a single space
        text = text.replaceAll("\\. \\.", " ");
        text = text.replaceAll("  \\.", " ");

        // Remove repeated "PMT" markings
        text = text.replaceAll("\\bPMT\\b", "");

        // Remove unicode placeholder characters
        text = text.replaceAll("\\?+", "");

        // Remove page numbers and headers/footers (often appear as isolated numbers or
        // short phrases)
        text = text.replaceAll("(?m)^\\d+$", "");

        // Remove exam-specific instructions and references
        text = text.replaceAll("(?i)\\bP\\d+\\b", ""); // Remove page references like P1, P2
        text = text.replaceAll("(?i)turn over", "");
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?i)continue on the next page", "");

        // Remove question numbering and marks information
        text = text.replaceAll("\\(Total for Question \\d+:? \\d+ marks?\\)", "");
        text = text.replaceAll("\\(Total for Question \\d+ = \\d+ marks?\\)", "");
        text = text.replaceAll("TOTAL FOR SECTION [A-Z] = \\d+ MARKS", "");
        text = text.replaceAll("\\(\\d+ marks?\\)", "");

        // Remove excessive whitespace including new lines
        text = text.replaceAll("\\s+", " ");

        // Remove any leading/trailing whitespace
        text = text.trim();

        return text;
    }

    private void exportQuestionsToJson(List<Question> questions, File outputFile) throws JSONException {
        try {
            // Create a JSON array to hold all question objects
            JSONArray jsonArray = new JSONArray();

            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("questionText", question.getQuestionText());

                // Add topics as a JSON array
                if (question.getTopics() != null) {
                    JSONArray topicsArray = new JSONArray();
                    for (String topic : question.getTopics()) {
                        topicsArray.put(topic);
                    }
                    jsonQuestion.put("topics", topicsArray);
                } else {
                    jsonQuestion.put("topics", new JSONArray());
                }

                // Add file paths
                if (question.getQuestion() != null) {
                    jsonQuestion.put("questionFile", question.getQuestion().getAbsolutePath());
                }
                if (question.getMarkScheme() != null) {
                    jsonQuestion.put("markSchemeFile", question.getMarkScheme().getAbsolutePath());
                }

                jsonArray.put(jsonQuestion);
            }

            // Write the JSON to the output file
            java.nio.file.Files.write(
                    outputFile.toPath(),
                    jsonArray.toString(2).getBytes(), // Pretty print with indentation of 2
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println(
                    "Successfully exported " + questions.size() + " questions to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing questions to JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract text content from a PDF file
     * 
     * @param pdfFile The PDF file to extract text from
     * @return The extracted text
     */
    private String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF file: " + pdfFile.getName());
            e.printStackTrace();
            return "";
        }
    }
}