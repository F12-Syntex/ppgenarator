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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
    private int targetMarksPerMock = 15;

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

                    // Create mock tests (generate as many as possible without overlap)
                    createNonOverlappingMockTests(topicQuestions, mockTestsDir, qualification, topic);

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

                    question.setQualification(
                            Qualification.fromString(jsonQuestion.optString("qualification", "UNKNOWN")));

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
            return file.getName();
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

    private boolean isQuestion6(String questionNumber) {
        return questionNumber != null && questionNumber.toLowerCase().startsWith("question6");
    }

    private String getPaperIdentifier(Question question) {
        // Create a unique identifier for the paper (year + board + specific paper info)
        if (question.getQuestion() == null || !question.getQuestion().exists()) {
            return question.getYear() + "_" + question.getBoard().toString();
        }
        
        try {
            // Get the path to find the paper directory
            File questionFile = question.getQuestion();
            File paperDir = questionFile.getParentFile(); // The paper directory (e.g., "1")
            File yearDir = paperDir != null ? paperDir.getParentFile() : null; // The year directory
            File boardDir = yearDir != null ? yearDir.getParentFile() : null; // The board directory
            
            if (paperDir != null && yearDir != null && boardDir != null) {
                return question.getYear() + "_" + question.getBoard().toString() + "_" + paperDir.getName();
            }
        } catch (Exception e) {
            System.err.println("Error getting paper identifier: " + e.getMessage());
        }
        
        return question.getYear() + "_" + question.getBoard().toString();
    }

    private void createNonOverlappingMockTests(List<Question> questions, File mockTestsDir, String qualification, String topic) {
        try {
            // Shuffle questions for random distribution
            List<Question> availableQuestions = new ArrayList<>(questions);
            Collections.shuffle(availableQuestions, new Random());

            int mockTestNumber = 1;
            int questionsUsed = 0;

            while (!availableQuestions.isEmpty()) {
                // Select questions for this mock test without exceeding target marks
                List<Question> selectedQuestions = new ArrayList<>();
                Set<String> usedPapers = new HashSet<>(); // Track papers already used for Question 6
                int totalMarks = 0;
                
                for (int i = availableQuestions.size() - 1; i >= 0; i--) {
                    Question question = availableQuestions.get(i);
                    
                    // Check if this is Question 6 and if we already have a Question 6 from this paper
                    if (isQuestion6(question.getQuestionNumber())) {
                        String paperIdentifier = getPaperIdentifier(question);
                        if (usedPapers.contains(paperIdentifier)) {
                            continue; // Skip this question as we already have a Question 6 from this paper
                        }
                        
                        // Check if adding this would fit within marks limit
                        if (totalMarks + question.getMarks() <= targetMarksPerMock) {
                            selectedQuestions.add(question);
                            totalMarks += question.getMarks();
                            usedPapers.add(paperIdentifier);
                            availableQuestions.remove(i);
                            questionsUsed++;
                        }
                    } else {
                        // For non-Question 6, just check marks limit
                        if (totalMarks + question.getMarks() <= targetMarksPerMock) {
                            selectedQuestions.add(question);
                            totalMarks += question.getMarks();
                            availableQuestions.remove(i);
                            questionsUsed++;
                        }
                    }
                }

                // If no questions were selected (all remaining questions exceed target), take the smallest one
                if (selectedQuestions.isEmpty() && !availableQuestions.isEmpty()) {
                    Question smallestQuestion = availableQuestions.stream()
                            .min((q1, q2) -> Integer.compare(q1.getMarks(), q2.getMarks()))
                            .orElse(null);
                    
                    if (smallestQuestion != null) {
                        selectedQuestions.add(smallestQuestion);
                        totalMarks = smallestQuestion.getMarks();
                        availableQuestions.remove(smallestQuestion);
                        questionsUsed++;
                    }
                }

                if (selectedQuestions.isEmpty()) {
                    break;
                }

                // Calculate estimated time (2 minutes per mark)
                int estimatedMinutes = totalMarks * 2;

                System.out.println("Mock test " + mockTestNumber + " created with " + selectedQuestions.size() +
                        " questions, " + totalMarks + " marks, estimated time: " + estimatedMinutes + " minutes");

                // Create mock test directory
                File mockTestDir = new File(mockTestsDir, "mock" + mockTestNumber);
                mockTestDir.mkdirs();

                // Create cover page
                File coverPageFile = createCoverPage(mockTestNumber, selectedQuestions, totalMarks, 
                        estimatedMinutes, qualification, topic, mockTestDir);

                // Create the mock test PDFs with cover page
                createMockTestPdfs(selectedQuestions, mockTestDir, coverPageFile);

                // Save metadata for this mock test
                saveMockTestMetadata(mockTestNumber, selectedQuestions, totalMarks, estimatedMinutes, mockTestDir);

                mockTestNumber++;
            }

            System.out.println("Created " + (mockTestNumber - 1) + " mock tests using " + questionsUsed + 
                    " out of " + questions.size() + " questions");

        } catch (Exception e) {
            System.err.println("Error creating mock tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Set<File> findRequiredExtracts(List<Question> questions) {
        Set<File> extractFiles = new HashSet<>();
        
        for (Question question : questions) {
            // Check if this is a question 6 variant (6, 6a, 6b, 6c, etc.)
            if (isQuestion6(question.getQuestionNumber())) {
                File extractFile = findExtractFile(question);
                if (extractFile != null && extractFile.exists()) {
                    extractFiles.add(extractFile);
                }
            }
        }
        
        return extractFiles;
    }

    private File findExtractFile(Question question) {
        if (question.getQuestion() == null || !question.getQuestion().exists()) {
            return null;
        }
        
        try {
            // Get the path to the question file
            String questionPath = question.getQuestion().getAbsolutePath();
            
            // Parse the path to find the extract location
            // Expected format: ...\subject\level\board\year\paper\questionX.pdf
            // Extract should be: ...\subject\level\board\year\paper\extract.pdf
            
            File questionFile = question.getQuestion();
            File paperDir = questionFile.getParentFile(); // The paper directory (e.g., "1")
            
            if (paperDir != null) {
                File extractFile = new File(paperDir, "extract.pdf");
                if (extractFile.exists()) {
                    return extractFile;
                }
            }
            
            System.out.println("Extract file not found for question: " + questionPath);
            return null;
            
        } catch (Exception e) {
            System.err.println("Error finding extract file for question " + question.getQuestionNumber() + ": " + e.getMessage());
            return null;
        }
    }

    private File createCoverPage(int mockTestNumber, List<Question> questions, int totalMarks, 
            int estimatedMinutes, String qualification, String topic, File mockTestDir) throws IOException {
        
        File coverPageFile = new File(mockTestDir, "cover_page.pdf");
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float pageWidth = page.getMediaBox().getWidth();
                float yPosition = page.getMediaBox().getHeight() - margin;
                
                // Header - Best Tutors
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 28);
                float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("BEST TUTORS") / 1000 * 28;
                contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPosition);
                contentStream.showText("BEST TUTORS");
                contentStream.endText();
                yPosition -= 35;
                
                // Subtitle
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 14);
                String subtitle = "Name and reference";
                float subtitleWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(subtitle) / 1000 * 14;
                contentStream.newLineAtOffset((pageWidth - subtitleWidth) / 2, yPosition);
                contentStream.showText(subtitle);
                contentStream.endText();
                yPosition -= 40;
                
                // Decorative line
                contentStream.setLineWidth(2);
                contentStream.moveTo(margin + 50, yPosition);
                contentStream.lineTo(pageWidth - margin - 50, yPosition);
                contentStream.stroke();
                yPosition -= 30;
                
                // Mock Test Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
                String mockTitle = "Mock Test " + mockTestNumber;
                float mockTitleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(mockTitle) / 1000 * 24;
                contentStream.newLineAtOffset((pageWidth - mockTitleWidth) / 2, yPosition);
                contentStream.showText(mockTitle);
                contentStream.endText();
                yPosition -= 50;
                
                // Topic and Qualification in a box
                float boxWidth = pageWidth - 2 * margin;
                float boxHeight = 80;
                contentStream.setLineWidth(1);
                contentStream.addRect(margin, yPosition - boxHeight, boxWidth, boxHeight);
                contentStream.stroke();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(margin + 20, yPosition - 25);
                contentStream.showText("Subject: " + topic);
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.newLineAtOffset(margin + 20, yPosition - 50);
                contentStream.showText("Qualification: " + qualification.toUpperCase());
                contentStream.endText();
                
                yPosition -= boxHeight + 30;
                
                // Test Information Box
                float infoBoxHeight = 120;
                contentStream.setLineWidth(1);
                contentStream.addRect(margin, yPosition - infoBoxHeight, boxWidth, infoBoxHeight);
                contentStream.stroke();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(margin + 20, yPosition - 25);
                contentStream.showText("Test Information");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(margin + 40, yPosition - 50);
                contentStream.showText("Number of Questions: " + questions.size());
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(margin + 40, yPosition - 70);
                contentStream.showText("Total Marks: " + totalMarks);
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(margin + 40, yPosition - 90);
                contentStream.showText("Estimated Time: " + estimatedMinutes + " minutes");
                contentStream.endText();
                
                yPosition -= infoBoxHeight + 30;
                
                // Instructions
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Instructions:");
                contentStream.endText();
                yPosition -= 25;
                
                String[] instructions = {
                    "• Answer ALL questions in the spaces provided",
                    "• Show all your working clearly and logically",
                    "• Give your final answers to an appropriate degree of accuracy",
                    "• Check your work carefully before submitting",
                    "• The mark allocation for each question is shown in brackets [ ]"
                };
                
                for (String instruction : instructions) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 11);
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    contentStream.showText(instruction);
                    contentStream.endText();
                    yPosition -= 18;
                }
                
                yPosition -= 20;
                
                // Question Breakdown
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Question Overview:");
                contentStream.endText();
                yPosition -= 20;
                
                // Sort questions by year and question number
                List<Question> sortedQuestions = new ArrayList<>(questions);
                sortedQuestions.sort((q1, q2) -> {
                    int yearCompare = q1.getYear().compareTo(q2.getYear());
                    if (yearCompare != 0) {
                        return yearCompare;
                    }
                    return q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
                });
                
                for (int i = 0; i < sortedQuestions.size() && yPosition > 80; i++) {
                    Question question = sortedQuestions.get(i);
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    String questionInfo = String.format("Q%d: %s %s [%d marks]", 
                            (i + 1), question.getYear(), question.getQuestionNumber(), question.getMarks());
                    contentStream.showText(questionInfo);
                    contentStream.endText();
                    yPosition -= 15;
                }
                
                // Footer
                yPosition = margin + 20;
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                String footer = "Good luck with your test!";
                float footerWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(footer) / 1000 * 10;
                contentStream.newLineAtOffset((pageWidth - footerWidth) / 2, yPosition);
                contentStream.showText(footer);
                contentStream.endText();
            }
            
            document.save(coverPageFile);
        }
        
        return coverPageFile;
    }

    private void createMockTestPdfs(List<Question> questions, File mockTestDir, File coverPageFile) {
        try {
            // Create questions PDF with cover page
            PDFMergerUtility questionsMerger = new PDFMergerUtility();
            questionsMerger.setDestinationFileName(new File(mockTestDir, "mock.pdf").getAbsolutePath());
            
            // Add cover page first
            questionsMerger.addSource(coverPageFile);
            
            // Check if we need to include any extracts
            Set<File> extractFiles = findRequiredExtracts(questions);
            
            // Add extracts before questions if any are found
            List<File> tempExtractFiles = new ArrayList<>();
            for (File extractFile : extractFiles) {
                if (extractFile.exists()) {
                    PDDocument document = PDDocument.load(extractFile);
                    File tempExtractFile = File.createTempFile("temp_mock_extract_", ".pdf");
                    document.save(tempExtractFile);
                    document.close();
                    
                    questionsMerger.addSource(tempExtractFile);
                    tempExtractFiles.add(tempExtractFile);
                    System.out.println("Added extract: " + extractFile.getName());
                }
            }
            
            // Create markschemes PDF
            PDFMergerUtility markschemesMerger = new PDFMergerUtility();
            markschemesMerger.setDestinationFileName(new File(mockTestDir, "mock_markscheme.pdf").getAbsolutePath());

            List<File> tempQuestionFiles = new ArrayList<>();
            List<File> tempMarkSchemeFiles = new ArrayList<>();

            boolean hasQuestions = false;
            boolean hasMarkschemes = false;

            for (Question question : questions) {
                if (question.getQuestion() != null && question.getQuestion().exists()) {
                    PDDocument document = PDDocument.load(question.getQuestion());
                    File tempQuestionFile = File.createTempFile("temp_mock_q_", ".pdf");
                    document.save(tempQuestionFile);
                    document.close();

                    questionsMerger.addSource(tempQuestionFile);
                    tempQuestionFiles.add(tempQuestionFile);
                    hasQuestions = true;
                }

                if (question.getMarkScheme() != null && question.getMarkScheme().exists()) {
                    PDDocument document = PDDocument.load(question.getMarkScheme());
                    File tempMarkSchemeFile = File.createTempFile("temp_mock_ms_", ".pdf");
                    document.save(tempMarkSchemeFile);
                    document.close();

                    markschemesMerger.addSource(tempMarkSchemeFile);
                    tempMarkSchemeFiles.add(tempMarkSchemeFile);
                    hasMarkschemes = true;
                }
            }

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
            for (File tempFile : tempExtractFiles) {
                tempFile.delete();
            }

            // Delete the temporary cover page file
            coverPageFile.delete();

        } catch (IOException e) {
            System.err.println("Error creating mock test PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveMockTestMetadata(int mockTestId, List<Question> questions, int totalMarks, 
            int estimatedMinutes, File mockTestDir) {
        try {
            JSONObject metadata = new JSONObject();
            metadata.put("mockTestId", mockTestId);
            metadata.put("questionCount", questions.size());
            metadata.put("totalMarks", totalMarks);
            metadata.put("estimatedMinutes", estimatedMinutes);

            // Check for extracts
            Set<File> extractFiles = findRequiredExtracts(questions);
            JSONArray extractsArray = new JSONArray();
            for (File extractFile : extractFiles) {
                if (extractFile.exists()) {
                    extractsArray.put(extractFile.getName());
                }
            }
            metadata.put("extracts", extractsArray);
            metadata.put("hasExtracts", !extractFiles.isEmpty());

            JSONArray questionsArray = new JSONArray();
            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("marks", question.getMarks());
                jsonQuestion.put("questionText", question.getQuestionText());
                
                // Mark if this question requires an extract
                boolean requiresExtract = isQuestion6(question.getQuestionNumber());
                jsonQuestion.put("requiresExtract", requiresExtract);
                
                questionsArray.put(jsonQuestion);
            }

            metadata.put("questions", questionsArray);

            File metadataFile = new File(mockTestDir, "metadata.json");
            Files.write(
                    metadataFile.toPath(),
                    metadata.toString(2).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (Exception e) {
            System.err.println("Error saving mock test metadata: " + e.getMessage());
            e.printStackTrace();
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

            File metadataFile = new File(topicDir, "metadata.json");
            Files.write(
                    metadataFile.toPath(),
                    metadata.toString(2).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (JSONException | IOException e) {
            System.err.println("Error saving metadata for topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
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