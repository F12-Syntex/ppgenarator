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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ppgenerator.types.ExamBoard;
import com.ppgenerator.types.Qualification;
import com.ppgenerator.types.Question;

public class TopicCompiler {

    private File metadataDir;
    private File outputDir;
    private int targetMarksPerMock = 25; // Default target marks per mock test

    public TopicCompiler(File metadataDir, File outputDir) {
        this.metadataDir = metadataDir;
        this.outputDir = outputDir;

        // Create output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    public void setTargetMarksPerMock(int targetMarksPerMock) {
        this.targetMarksPerMock = targetMarksPerMock;
    }

    public void compileByTopic() {
        try {
            // Map to store questions by qualification and topic
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = new HashMap<>();

            // Load all questions from JSON files
            List<Question> allQuestions = loadQuestionsFromJsonFiles();
            System.out.println("Loaded " + allQuestions.size() + " questions from JSON files");

            // Group questions by qualification and topic
            for (Question question : allQuestions) {
                String qualification = question.getQualification() != null
                        ? question.getQualification().toString().toLowerCase()
                        : "unknown";

                if (!questionsByQualificationAndTopic.containsKey(qualification)) {
                    questionsByQualificationAndTopic.put(qualification, new HashMap<>());
                }

                if (question.getTopics() != null && question.getTopics().length > 0) {
                    for (String topic : question.getTopics()) {
                        Map<String, List<Question>> topicMap = questionsByQualificationAndTopic.get(qualification);
                        if (!topicMap.containsKey(topic)) {
                            topicMap.put(topic, new ArrayList<>());
                        }
                        topicMap.get(topic).add(question);
                    }
                }
            }

            // Process each qualification and topic
            for (String qualification : questionsByQualificationAndTopic.keySet()) {
                Map<String, List<Question>> topicMap = questionsByQualificationAndTopic.get(qualification);

                // Create qualification directory
                File qualificationDir = new File(outputDir, qualification);
                if (!qualificationDir.exists()) {
                    qualificationDir.mkdirs();
                }

                // Create topics directory
                File topicsDir = new File(qualificationDir, "topics");
                if (!topicsDir.exists()) {
                    topicsDir.mkdirs();
                }

                for (String topic : topicMap.keySet()) {
                    List<Question> topicQuestions = topicMap.get(topic);
                    System.out.println("Processing topic: " + topic + " in qualification: " + qualification +
                            " with " + topicQuestions.size() + " questions");

                    // Create directory for this topic
                    File topicDir = new File(topicsDir, sanitizeFileName(topic));
                    if (!topicDir.exists()) {
                        topicDir.mkdirs();
                    }

                    // Create questions and markscheme directories
                    File questionsDir = new File(topicDir, "questions");
                    File markschemesDir = new File(topicDir, "markschemes");
                    File mockTestsDir = new File(topicDir, "mock_tests");
                    questionsDir.mkdirs();
                    markschemesDir.mkdirs();
                    mockTestsDir.mkdirs();

                    // Create merged PDFs (without duplicates)
                    createMergedPdf(topicQuestions, questionsDir, markschemesDir);

                    // Copy individual files
                    copyIndividualFiles(topicQuestions, questionsDir, markschemesDir);

                    // Create mock tests
                    createMockTests(topicQuestions, mockTestsDir, 5);

                    // Save metadata about this topic
                    saveTopicMetadata(topic, topicQuestions, topicDir);
                }
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
                    // question.setQualification(
                    question.setQualification(
                            Qualification.fromString(jsonQuestion.optString("qualification", "UNKNOWN")));

                    question.setMarks(jsonQuestion.optInt("marks", 1)); // Default to 1 mark if not specified

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

    // ---------------------------
    // Merge PDF creation (fix: ensure only unique, real files are merged, fresh
    // PDFMergerUtility each time)
    // ---------------------------
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

            // Sets to track unique question and markscheme content
            Set<String> questionMd5Hashes = new HashSet<>();
            Set<String> markschemeMd5Hashes = new HashSet<>();

            for (Question question : questions) {
                if (question.getQuestion() != null && question.getQuestion().exists()) {
                    String md5Hash = getFileMd5Hash(question.getQuestion());

                    if (!questionMd5Hashes.contains(md5Hash)) {
                        questionMd5Hashes.add(md5Hash);

                        PDDocument document = PDDocument.load(question.getQuestion());
                        File tempQuestionFile = File.createTempFile("temp_question_", ".pdf");
                        document.save(tempQuestionFile);
                        document.close();

                        questionsMerger.addSource(tempQuestionFile);
                        tempQuestionFiles.add(tempQuestionFile);
                        hasQuestions = true;
                    } else {
                        System.out.println("Skipping duplicate question: " + question.getYear() + "_" +
                                question.getQuestionNumber());
                    }
                }

                if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                    String md5Hash = getFileMd5Hash(question.getMarkScheme());

                    if (!markschemeMd5Hashes.contains(md5Hash)) {
                        markschemeMd5Hashes.add(md5Hash);

                        PDDocument document = PDDocument.load(question.getMarkScheme());
                        File tempMarkSchemeFile = File.createTempFile("temp_ms_", ".pdf");
                        document.save(tempMarkSchemeFile);
                        document.close();

                        markschemesMerger.addSource(tempMarkSchemeFile);
                        tempMarkSchemeFiles.add(tempMarkSchemeFile);
                        hasMarkschemes = true;
                    } else {
                        System.out.println("Skipping duplicate markscheme: " + question.getYear() + "_" +
                                question.getQuestionNumber());
                    }
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

    private String getFileMd5Hash(File file) {
        try {
            return org.apache.commons.codec.digest.DigestUtils.md5Hex(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            System.err.println("Error calculating MD5 hash: " + e.getMessage());
            return file.getName(); // Fallback to filename if we can't calculate hash
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

    // ------------------------------------------
    // MOCK TEST CREATION -- ENSURE NO DUPLICATES
    // ------------------------------------------
    private void createMockTests(List<Question> questions, File mockTestsDir, int numberOfTests) {
        Random random = new Random();

        for (int i = 1; i <= numberOfTests; i++) {
            try {
                File mockTestDir = new File(mockTestsDir, "mock" + i);
                mockTestDir.mkdirs();

                // Shuffle and select unique questions
                List<Question> availableQuestions = new ArrayList<>(questions);
                Collections.shuffle(availableQuestions, random);

                List<Question> selectedQuestions = new ArrayList<>();
                int totalMarks = 0;
                for (Question question : availableQuestions) {
                    if (totalMarks + question.getMarks() <= targetMarksPerMock) {
                        selectedQuestions.add(question);
                        totalMarks += question.getMarks();
                    }
                }
                // Never add a question twice, even if we don't reach the target

                System.out.println("Mock test " + i + " created with " + selectedQuestions.size() +
                        " unique questions and " + totalMarks + " total marks");

                // Prepare PDF mergers
                PDFMergerUtility questionsMerger = new PDFMergerUtility();
                questionsMerger.setDestinationFileName(new File(mockTestDir, "mock.pdf").getAbsolutePath());

                PDFMergerUtility markschemesMerger = new PDFMergerUtility();
                markschemesMerger
                        .setDestinationFileName(new File(mockTestDir, "mock_markscheme.pdf").getAbsolutePath());

                List<File> tempQuestionFiles = new ArrayList<>();
                List<File> tempMarkSchemeFiles = new ArrayList<>();

                boolean hasQuestions = false;
                boolean hasMarkschemes = false;

                JSONObject metadata = new JSONObject();
                metadata.put("mockTestId", i);
                metadata.put("questionCount", selectedQuestions.size());
                metadata.put("totalMarks", totalMarks);
                JSONArray questionsArray = new JSONArray();

                for (Question question : selectedQuestions) {
                    JSONObject jsonQuestion = new JSONObject();
                    jsonQuestion.put("year", question.getYear());
                    jsonQuestion.put("questionNumber", question.getQuestionNumber());
                    jsonQuestion.put("board", question.getBoard());
                    jsonQuestion.put("marks", question.getMarks());

                    if (question.getQuestion() != null && question.getQuestion().exists()) {
                        PDDocument document = PDDocument.load(question.getQuestion());
                        File tempQuestionFile = File.createTempFile("temp_mock_q_", ".pdf");
                        document.save(tempQuestionFile);
                        document.close();

                        questionsMerger.addSource(tempQuestionFile);
                        tempQuestionFiles.add(tempQuestionFile);
                        hasQuestions = true;

                        jsonQuestion.put("questionFile", "mock.pdf");
                    }

                    if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                        PDDocument document = PDDocument.load(question.getMarkScheme());
                        File tempMarkSchemeFile = File.createTempFile("temp_mock_ms_", ".pdf");
                        document.save(tempMarkSchemeFile);
                        document.close();

                        markschemesMerger.addSource(tempMarkSchemeFile);
                        tempMarkSchemeFiles.add(tempMarkSchemeFile);
                        hasMarkschemes = true;

                        jsonQuestion.put("markSchemeFile", "mock_markscheme.pdf");
                    }

                    questionsArray.put(jsonQuestion);
                }

                metadata.put("questions", questionsArray);

                // Merge the PDFs
                if (hasQuestions) {
                    questionsMerger.mergeDocuments(null);
                }
                if (hasMarkschemes) {
                    markschemesMerger.mergeDocuments(null);
                }

                // Clean up temporary files
                for (File tempFile : tempQuestionFiles) {
                    tempFile.delete();
                }
                for (File tempFile : tempMarkSchemeFiles) {
                    tempFile.delete();
                }

                // Write metadata
                File metadataFile = new File(mockTestDir, "metadata.json");
                Files.write(
                        metadataFile.toPath(),
                        metadata.toString(2).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                generateMockTestHtml(i, selectedQuestions, totalMarks, mockTestDir);

            } catch (Exception e) {
                System.err.println("Error creating mock test " + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void saveTopicMetadata(String topic, List<Question> questions, File topicDir) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put("topic", topic);
            metadata.put("questionCount", questions.size());

            // Calculate total marks
            int totalMarks = 0;
            for (Question question : questions) {
                totalMarks += question.getMarks();
            }
            metadata.put("totalMarks", totalMarks);

            JSONArray questionsArray = new JSONArray();
            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("qualification", question.getQualification());
                jsonQuestion.put("questionText", question.getQuestionText());
                jsonQuestion.put("marks", question.getMarks());

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
                .append("        .mock-tests { margin: 20px 0; padding: 15px; background: #f8f8f8; border-radius: 5px; }\n")
                .append("        .marks { font-weight: bold; color: #d9534f; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <h1>").append(topic).append(" - Question Compilation</h1>\n");

        // Calculate total marks
        int totalMarks = 0;
        for (Question question : questions) {
            totalMarks += question.getMarks();
        }

        html.append("    <p>Total questions: ").append(questions.size())
                .append(" (Total marks: ").append(totalMarks).append(")</p>\n")
                .append("    <div class=\"merged-links\">\n")
                .append("        <h2>Merged Files:</h2>\n")
                .append("        <a href=\"questions/questions.pdf\" target=\"_blank\">All Questions PDF</a> | \n")
                .append("        <a href=\"markschemes/markscheme.pdf\" target=\"_blank\">All Mark Schemes PDF</a>\n")
                .append("    </div>\n");

        html.append("    <div class=\"mock-tests\">\n")
                .append("        <h2>Mock Tests:</h2>\n")
                .append("        <ul>\n");

        for (int i = 1; i <= 5; i++) {
            html.append("            <li><a href=\"mock_tests/mock")
                    .append(i).append("/index.html\" target=\"_blank\">Mock Test ").append(i)
                    .append(" (Target: ").append(targetMarksPerMock).append(" marks)</a></li>\n");
        }

        html.append("        </ul>\n")
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
                    .append(question.getQuestionNumber())
                    .append(" <span class=\"marks\">(").append(question.getMarks()).append(" marks)</span></h3>\n");

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

    private void generateMockTestHtml(int mockTestId, List<Question> questions, int totalMarks, File mockTestDir)
            throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>Mock Test ").append(mockTestId).append("</title>\n")
                .append("    <style>\n")
                .append("        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }\n")
                .append("        h1 { color: #333; }\n")
                .append("        .mock-info { margin-bottom: 20px; padding: 15px; background: #f0f0f0; border-radius: 5px; }\n")
                .append("        .question-list { margin-top: 20px; }\n")
                .append("        .question-item { margin-bottom: 10px; }\n")
                .append("        .links { margin-top: 20px; }\n")
                .append("        .links a { display: inline-block; margin-right: 10px; padding: 8px 15px; background: #4CAF50; color: white; text-decoration: none; border-radius: 4px; }\n")
                .append("        .links a:hover { background: #45a049; }\n")
                .append("        .marks { font-weight: bold; color: #d9534f; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <h1>Mock Test ").append(mockTestId).append("</h1>\n")
                .append("    <div class=\"mock-info\">\n")
                .append("        <p>This mock test contains ").append(questions.size())
                .append(" questions with a total of ").append(totalMarks)
                .append(" marks (Target: ").append(targetMarksPerMock).append(" marks).</p>\n")
                .append("    </div>\n")
                .append("    <div class=\"links\">\n")
                .append("        <a href=\"mock.pdf\" target=\"_blank\">View Questions</a>\n")
                .append("        <a href=\"mock_markscheme.pdf\" target=\"_blank\">View Mark Scheme</a>\n")
                .append("    </div>\n")
                .append("    <div class=\"question-list\">\n")
                .append("        <h2>Questions included:</h2>\n")
                .append("        <ul>\n");

        for (Question question : questions) {
            html.append("            <li class=\"question-item\">")
                    .append(question.getYear()).append(" - Question ").append(question.getQuestionNumber())
                    .append(" <span class=\"marks\">(").append(question.getMarks()).append(" marks)</span>");

            if (question.getQuestionText() != null && !question.getQuestionText().isEmpty()) {
                String preview = question.getQuestionText();
                if (preview.length() > 100) {
                    preview = preview.substring(0, 100) + "...";
                }
                html.append(" - ").append(escapeHtml(preview));
            }

            html.append("</li>\n");
        }

        html.append("        </ul>\n")
                .append("    </div>\n")
                .append("</body>\n")
                .append("</html>");

        Files.write(new File(mockTestDir, "index.html").toPath(), html.toString().getBytes());
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
            System.out
                    .println(
                            "Usage: TopicCompiler <metadata-directory> <output-directory> [target-marks-per-mock]");
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

        // Set target marks per mock test if provided
        if (args.length > 2) {
            try {
                int targetMarksPerMock = Integer.parseInt(args[2]);
                compiler.setTargetMarksPerMock(targetMarksPerMock);
                System.out.println("Setting target marks per mock test to: " + targetMarksPerMock);
            } catch (NumberFormatException e) {
                System.err.println("Invalid target marks per mock test: " + args[2]);
            }
        }

        compiler.compileByTopic();
    }
}