package com.ppgenarator.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ppgenerator.types.ExamBoard;
import com.ppgenerator.types.Question;

public class TopicCompiler {

    private File metadataDir;
    private File outputDir;

    public TopicCompiler(File metadataDir, File outputDir) {
        this.metadataDir = metadataDir;
        this.outputDir = outputDir;

        // Create output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    public void compileByTopic() {
        try {
            // Map to store questions by topic
            Map<String, List<Question>> questionsByTopic = new HashMap<>();

            // Load all questions from JSON files
            List<Question> allQuestions = loadQuestionsFromJsonFiles();
            System.out.println("Loaded " + allQuestions.size() + " questions from JSON files");

            // Group questions by topic
            for (Question question : allQuestions) {
                if (question.getTopics() != null && question.getTopics().length > 0) {
                    for (String topic : question.getTopics()) {
                        if (!questionsByTopic.containsKey(topic)) {
                            questionsByTopic.put(topic, new ArrayList<>());
                        }
                        questionsByTopic.get(topic).add(question);
                    }
                }
            }

            // Process each topic
            for (String topic : questionsByTopic.keySet()) {
                List<Question> topicQuestions = questionsByTopic.get(topic);
                System.out.println("Processing topic: " + topic + " with " + topicQuestions.size() + " questions");

                // Create directory for this topic
                File topicDir = new File(outputDir, sanitizeFileName(topic));
                if (!topicDir.exists()) {
                    topicDir.mkdirs();
                }

                // Create questions and markscheme directories
                File questionsDir = new File(topicDir, "questions");
                File markschemesDir = new File(topicDir, "markschemes");
                questionsDir.mkdirs();
                markschemesDir.mkdirs();

                // Create merged PDFs
                createMergedPdf(topicQuestions, questionsDir, markschemesDir);

                // Copy individual files
                copyIndividualFiles(topicQuestions, questionsDir, markschemesDir);

                // Save metadata about this topic
                saveTopicMetadata(topic, topicQuestions, topicDir);
            }

            System.out.println("Topic compilation complete. Output directory: " + outputDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error compiling questions by topic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Question> loadQuestionsFromJsonFiles() throws JSONException, IOException {
        List<Question> allQuestions = new ArrayList<>();

        File[] jsonFiles = metadataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in directory: " + metadataDir.getAbsolutePath());
            return allQuestions;
        }

        for (File jsonFile : jsonFiles) {
            try (FileReader reader = new FileReader(jsonFile)) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        content.append(line);
                    }
                }
                JSONArray jsonArray = new JSONArray(new JSONTokener(content.toString()));

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonQuestion = jsonArray.getJSONObject(i);
                    Question question = new Question();

                    // Set basic properties
                    question.setQuestionNumber(jsonQuestion.getString("questionNumber"));
                    question.setYear(jsonQuestion.getString("year"));
                    try {
                        question.setBoard(ExamBoard.valueOf(jsonQuestion.optString("board", "UNKNOWN").toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        question.setBoard(ExamBoard.UNKNOWN);
                    }
                    
                    question.setQuestionText(jsonQuestion.optString("questionText", ""));

                    // Set file references
                    if (jsonQuestion.has("questionFile")) {
                        File questionFile = new File(jsonQuestion.getString("questionFile"));
                        if (questionFile.exists()) {
                            question.setQuestion(questionFile);
                        }
                    }

                    if (jsonQuestion.has("markSchemeFile")) {
                        File markSchemeFile = new File(jsonQuestion.getString("markSchemeFile"));
                        if (markSchemeFile.exists()) {
                            question.setMarkScheme(markSchemeFile);
                        }
                    }

                    // Set topics
                    if (jsonQuestion.has("topics")) {
                        JSONArray topicsArray = jsonQuestion.getJSONArray("topics");
                        String[] topics = new String[topicsArray.length()];
                        for (int j = 0; j < topicsArray.length(); j++) {
                            topics[j] = topicsArray.getString(j);
                        }
                        question.setTopics(topics);
                    }

                    allQuestions.add(question);
                }
            }
        }

        return allQuestions;
    }

    private void createMergedPdf(List<Question> questions, File questionsDir, File markschemesDir) {
        try {
            // Sort questions by year and question number for consistent ordering
            questions.sort((q1, q2) -> {
                int yearCompare = q1.getYear().compareTo(q2.getYear());
                if (yearCompare != 0) {
                    return yearCompare;
                }
                return q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
            });
            
            // Create merged questions PDF
            PDFMergerUtility questionsMerger = new PDFMergerUtility();
            questionsMerger.setDestinationFileName(new File(questionsDir, "questions.pdf").getAbsolutePath());
            boolean hasQuestions = false;

            // Create merged markschemes PDF
            PDFMergerUtility markschemesMerger = new PDFMergerUtility();
            markschemesMerger.setDestinationFileName(new File(markschemesDir, "markscheme.pdf").getAbsolutePath());
            boolean hasMarkschemes = false;

            // Lists to keep track of temporary files so we can delete them later
            List<File> tempQuestionFiles = new ArrayList<>();
            List<File> tempMarkSchemeFiles = new ArrayList<>();

            for (Question question : questions) {
                if (question.getQuestion() != null && question.getQuestion().exists()) {
                    // Create a temporary document to handle it as a file source
                    PDDocument document = PDDocument.load(question.getQuestion());
                    // Create a temporary file
                    File tempQuestionFile = File.createTempFile("temp_question_", ".pdf");
                    document.save(tempQuestionFile);
                    document.close();

                    questionsMerger.addSource(tempQuestionFile);
                    tempQuestionFiles.add(tempQuestionFile);
                    hasQuestions = true;
                }

                if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                    // Create a temporary document to handle it as a file source
                    PDDocument document = PDDocument.load(question.getMarkScheme());
                    // Create a temporary file
                    File tempMarkSchemeFile = File.createTempFile("temp_ms_", ".pdf");
                    document.save(tempMarkSchemeFile);
                    document.close();

                    markschemesMerger.addSource(tempMarkSchemeFile);
                    tempMarkSchemeFiles.add(tempMarkSchemeFile);
                    hasMarkschemes = true;
                }
            }

            // Merge the PDFs if there are any
            if (hasQuestions) {
                questionsMerger.mergeDocuments(null);
                System.out.println("Created merged questions PDF: " + questionsMerger.getDestinationFileName());
            }

            if (hasMarkschemes) {
                markschemesMerger.mergeDocuments(null);
                System.out.println("Created merged markschemes PDF: " + markschemesMerger.getDestinationFileName());
            }

            // Clean up temporary files
            for (File tempFile : tempQuestionFiles) {
                tempFile.delete();
            }
            for (File tempFile : tempMarkSchemeFiles) {
                tempFile.delete();
            }

        } catch (IOException e) {
            System.err.println("Error creating merged PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void copyIndividualFiles(List<Question> questions, File questionsDir, File markschemesDir) {
        for (Question question : questions) {
            try {
                String filenamePrefix = question.getYear() + "_" + question.getQuestionNumber();

                if (question.getQuestion() != null && question.getQuestion().exists()) {
                    Path target = Paths.get(questionsDir.getAbsolutePath(), filenamePrefix + "_question.pdf");
                    Files.copy(question.getQuestion().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                }

                if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                    Path target = Paths.get(markschemesDir.getAbsolutePath(), filenamePrefix + "_markscheme.pdf");
                    Files.copy(question.getMarkScheme().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println(
                        "Error copying file for question " + question.getQuestionNumber() + ": " + e.getMessage());
            }
        }
    }

    private void saveTopicMetadata(String topic, List<Question> questions, File topicDir) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put("topic", topic);
            metadata.put("questionCount", questions.size());

            JSONArray questionsArray = new JSONArray();
            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("questionText", question.getQuestionText());

                if (question.getQuestion() != null) {
                    jsonQuestion.put("questionFile",
                            question.getYear() + "_" + question.getQuestionNumber() + "_question.pdf");
                }

                if (question.getMarkScheme() != null) {
                    jsonQuestion.put("markSchemeFile",
                            question.getYear() + "_" + question.getQuestionNumber() + "_markscheme.pdf");
                }

                questionsArray.put(jsonQuestion);
            }

            metadata.put("questions", questionsArray);

            // Write the JSON to a file
            File metadataFile = new File(topicDir, "metadata.json");
            Files.write(
                    metadataFile.toPath(),
                    metadata.toString(2).getBytes(), // Pretty print with indentation of 2
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create a simple HTML index file for easier browsing
            generateHtmlIndex(topic, questions, topicDir);

        } catch (JSONException | IOException e) {
            System.err.println("Error saving metadata for topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateHtmlIndex(String topic, List<Question> questions, File topicDir) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>").append(topic).append(" - Question Compilation</title>\n")
                .append("    <style>\n")
                .append("        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }\n")
                .append("        h1 { color: #333; }\n")
                .append("        .question { margin-bottom: 20px; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }\n")
                .append("        .question h3 { margin-top: 0; }\n")
                .append("        .question-text { margin: 10px 0; font-style: italic; color: #555; }\n")
                .append("        .links { margin-top: 10px; }\n")
                .append("        .links a { display: inline-block; margin-right: 10px; padding: 5px 10px; background: #f0f0f0; text-decoration: none; color: #333; border-radius: 3px; }\n")
                .append("        .links a:hover { background: #e0e0e0; }\n")
                .append("        .merged-links { margin: 20px 0; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <h1>").append(topic).append(" - Question Compilation</h1>\n")
                .append("    <p>Total questions: ").append(questions.size()).append("</p>\n")
                .append("    <div class=\"merged-links\">\n")
                .append("        <h2>Merged Files:</h2>\n")
                .append("        <a href=\"questions/questions.pdf\" target=\"_blank\">All Questions PDF</a> | \n")
                .append("        <a href=\"markschemes/markscheme.pdf\" target=\"_blank\">All Mark Schemes PDF</a>\n")
                .append("    </div>\n");

        // Sort questions by year and question number
        questions.sort((q1, q2) -> {
            int yearCompare = q1.getYear().compareTo(q2.getYear());
            if (yearCompare != 0) {
                return yearCompare;
            }
            return q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
        });

        html.append("    <h2>Individual Questions:</h2>\n");

        for (Question question : questions) {
            html.append("    <div class=\"question\">\n")
                    .append("        <h3>").append(question.getYear()).append(" - Question ")
                    .append(question.getQuestionNumber()).append("</h3>\n");

            if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
                // Limit question text preview to 200 characters
                String preview = question.getQuestionText();
                if (preview.length() > 200) {
                    preview = preview.substring(0, 200) + "...";
                }
                html.append("        <div class=\"question-text\">").append(escapeHtml(preview)).append("</div>\n");
            }

            html.append("        <div class=\"links\">\n");

            if (question.getQuestion() != null) {
                String questionPath = "questions/" + question.getYear() + "_" + question.getQuestionNumber()
                        + "_question.pdf";
                html.append("            <a href=\"").append(questionPath)
                        .append("\" target=\"_blank\">View Question</a>\n");
            }

            if (question.getMarkScheme() != null) {
                String markSchemePath = "markschemes/" + question.getYear() + "_" + question.getQuestionNumber()
                        + "_markscheme.pdf";
                html.append("            <a href=\"").append(markSchemePath)
                        .append("\" target=\"_blank\">View Mark Scheme</a>\n");
            }

            html.append("        </div>\n")
                    .append("    </div>\n");
        }

        html.append("</body>\n")
                .append("</html>");

        Files.write(new File(topicDir, "index.html").toPath(), html.toString().getBytes());
    }

    private String sanitizeFileName(String input) {
        // Replace any character that isn't a letter, number, or underscore with
        // underscore
        return input.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: TopicCompiler <metadata-directory> <output-directory>");
            return;
        }

        File metadataDir = new File(args[0]);
        File outputDir = new File(args[1]);

        if (!metadataDir.exists() || !metadataDir.isDirectory()) {
            System.err.println(
                    "Metadata directory does not exist or is not a directory: " + metadataDir.getAbsolutePath());
            return;
        }

        TopicCompiler compiler = new TopicCompiler(metadataDir, outputDir);
        compiler.compileByTopic();
    }
}