package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ppgenarator.utils.FileUtils;
import com.ppgenerator.types.Question;

public class TopicCompiler {

    private File metadataDir;
    private File outputDir;
    private int targetMarksPerMock = 15;
    private int minimumQ1To5MockTests = 1;
    private boolean createQ1To5OnlyMocks = true;

    private QuestionLoader questionLoader;
    private MockTestGenerator mockTestGenerator;
    private PdfMerger pdfMerger;

    public TopicCompiler(File metadataDir, File outputDir) {
        this.metadataDir = metadataDir;
        this.outputDir = outputDir;

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Initialize services
        this.questionLoader = new QuestionLoader();
        this.mockTestGenerator = new MockTestGenerator(targetMarksPerMock, minimumQ1To5MockTests, createQ1To5OnlyMocks);
        this.pdfMerger = new PdfMerger();
    }

    public void setTargetMarksPerMock(int targetMarksPerMock) {
        this.targetMarksPerMock = targetMarksPerMock;
        this.mockTestGenerator.setTargetMarksPerMock(targetMarksPerMock);
    }

    public void setMinimumQ1To5MockTests(int minimumQ1To5MockTests) {
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
        this.mockTestGenerator.setMinimumQ1To5MockTests(minimumQ1To5MockTests);
    }

    public void setCreateQ1To5OnlyMocks(boolean createQ1To5OnlyMocks) {
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
        this.mockTestGenerator.setCreateQ1To5OnlyMocks(createQ1To5OnlyMocks);
    }

    public void compileByTopic() {
        try {
            // Load all questions from JSON files
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            System.out.println("Loaded " + allQuestions.size() + " questions from JSON files");

            // Group questions by qualification and topic
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = groupQuestionsByQualificationAndTopic(
                    allQuestions);

            // Process each qualification and topic
            processQualificationsAndTopics(questionsByQualificationAndTopic);

            System.out.println("Topic compilation complete. Output directory: " + outputDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error compiling questions by topic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Map<String, List<Question>>> groupQuestionsByQualificationAndTopic(
            List<Question> allQuestions) {
        Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = new HashMap<>();

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

        return questionsByQualificationAndTopic;
    }

    private void processQualificationsAndTopics(
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic)
            throws IOException {
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

                processTopic(topic, topicQuestions, topicsDir, qualification);
            }
        }
    }

    private void processTopic(String topic, List<Question> topicQuestions, File topicsDir, String qualification)
            throws IOException {
        // Create directory for this topic
        File topicDir = new File(topicsDir, FileUtils.sanitizeFileName(topic));
        if (!topicDir.exists()) {
            topicDir.mkdirs();
        }

        // Create sub-directories
        File questionsDir = new File(topicDir, "questions");
        File markschemesDir = new File(topicDir, "markschemes");
        File mockTestsDir = new File(topicDir, "mock_tests");
        questionsDir.mkdirs();
        markschemesDir.mkdirs();
        mockTestsDir.mkdirs();

        // Create merged PDFs
        pdfMerger.createMergedPdfs(topicQuestions, questionsDir, markschemesDir);

        // Create mock tests
        mockTestGenerator.createMockTests(topicQuestions, mockTestsDir, qualification, topic);
    }
}