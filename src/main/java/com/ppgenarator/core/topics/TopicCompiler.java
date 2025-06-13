package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ppgenarator.ai.TopicConstants;
import com.ppgenarator.utils.FileUtils;
import com.ppgenerator.types.Question;

public class TopicCompiler {

    private File metadataDir;
    private File outputDir; // This will point to the 'mocks' folder
    private int targetMarksPerMock = 20;
    private int mockTime = 25;
    private int minimumQ1To5MockTests = 1;
    private boolean createQ1To5OnlyMocks = true;

    private QuestionLoader questionLoader;
    private MockTestGenerator mockTestGenerator;
    private PdfMerger pdfMerger;

    public TopicCompiler(File metadataDir, File parentOutputDir) {
        this.metadataDir = metadataDir;
        // All output goes into the 'mocks' subdirectory of the given output directory
        this.outputDir = new File(parentOutputDir, "mocks");
        if (!this.outputDir.exists()) {
            this.outputDir.mkdirs();
        }

        this.questionLoader = new QuestionLoader();
        this.mockTestGenerator = new MockTestGenerator(targetMarksPerMock, minimumQ1To5MockTests, createQ1To5OnlyMocks, mockTime);
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
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            System.out.println("Loaded " + allQuestions.size() + " questions from JSON files");

            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic
                    = groupQuestionsByQualificationAndTopic(allQuestions);

            processQualificationTopics(questionsByQualificationAndTopic);

            System.out.println("Topic compilation complete. Output directory: " + outputDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error compiling questions by topic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Map<String, List<Question>>> groupQuestionsByQualificationAndTopic(List<Question> allQuestions) {
        Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = new HashMap<>();

        for (Question question : allQuestions) {
            String qualification = question.getQualification() != null
                    ? convertQualificationName(question.getQualification().toString().toLowerCase())
                    : "unknown";

            if (question.getTopics() != null && question.getTopics().length > 0) {
                for (String topic : question.getTopics()) {
                    questionsByQualificationAndTopic
                            .computeIfAbsent(qualification, k -> new HashMap<>())
                            .computeIfAbsent(topic, k -> new ArrayList<>())
                            .add(question);
                }
            }
        }

        return questionsByQualificationAndTopic;
    }

    private String convertQualificationName(String qualification) {
        switch (qualification) {
            case "a_level":
                return "a level";
            case "as":
                return "as level";
            case "gcse":
                return "gcse";
            case "igcse":
                return "igcse";
            default:
                return qualification;
        }
    }

    private void processQualificationTopics(Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic)
            throws IOException {

        for (String qualification : questionsByQualificationAndTopic.keySet()) {
            Map<String, List<Question>> topicMap = questionsByQualificationAndTopic.get(qualification);

            for (String topic : topicMap.keySet()) {
                List<Question> topicQuestions = topicMap.get(topic);

                System.out.println("Processing topic: " + topic
                        + " in qualification: " + qualification
                        + " with " + topicQuestions.size() + " questions");

                // Output all topics inside the 'mocks' directory
                processIndividualTopic(topic, topicQuestions, outputDir, qualification);
            }
        }
    }

    private void processIndividualTopic(String topic, List<Question> topicQuestions, File mocksRootDir, String qualification)
            throws IOException {

        List<Question> uniqueTopicQuestions = removeDuplicateQuestions(topicQuestions);

        if (uniqueTopicQuestions.isEmpty()) {
            System.out.println("No unique questions found for topic: " + topic);
            return;
        }

        // Create directory for this individual topic inside 'mocks'
        String topicDirName = FileUtils.sanitizeFileName(topic);
        File topicDir = new File(mocksRootDir, topicDirName);
        if (!topicDir.exists()) {
            topicDir.mkdirs();
        }

        // Create the main directories
        File allQuestionsDir = new File(topicDir, "all questions");
        File mocksDir = new File(topicDir, "mocks");

        allQuestionsDir.mkdirs();
        mocksDir.mkdirs();

        System.out.println("Creating resources for " + topic + " with " + uniqueTopicQuestions.size() + " unique questions");

        // Create combined questions and markschemes PDF
        pdfMerger.createCombinedQuestionsPdf(uniqueTopicQuestions, allQuestionsDir);

        // Create topic-specific mock tests
        mockTestGenerator.createTopicSpecificMocks(uniqueTopicQuestions, mocksDir, qualification,
                TopicConstants.getSubTopicName(topic), topic);

        System.out.println("Completed processing for " + topic + " (" + qualification + ")");
    }

    private List<Question> removeDuplicateQuestions(List<Question> questions) {
        Map<String, Question> uniqueQuestions = new HashMap<>();

        for (Question question : questions) {
            String identifier = getQuestionIdentifier(question);
            if (!uniqueQuestions.containsKey(identifier)) {
                uniqueQuestions.put(identifier, question);
            }
        }

        List<Question> result = new ArrayList<>(uniqueQuestions.values());
        if (questions.size() != result.size()) {
            System.out.println("Removed " + (questions.size() - result.size()) + " duplicate questions");
        }
        return result;
    }

    private String getQuestionIdentifier(Question question) {
        if (question.getQuestion() != null && question.getQuestion().exists()) {
            return FileUtils.getFileMd5Hash(question.getQuestion());
        }
        return question.getYear() + "_" + question.getBoard().toString() + "_" + question.getQuestionNumber();
    }

    public void createTopicAnalysisReport() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            System.out.println("Loaded " + allQuestions.size() + " questions for analysis report");

            Map<String, List<Question>> questionsByQualification = allQuestions.stream()
                    .collect(Collectors.groupingBy(
                            q -> q.getQualification() != null ? convertQualificationName(q.getQualification().toString().toLowerCase())
                            : "unknown"));

            for (Map.Entry<String, List<Question>> entry : questionsByQualification.entrySet()) {
                String qualification = entry.getKey();
                List<Question> qualificationQuestions = entry.getValue();

                // Write the report directly to the 'mocks' directory
                File qualificationDir = outputDir;

                if (qualificationDir.exists()) {
                    TopicSummaryReportCreator reportCreator = new TopicSummaryReportCreator();
                    reportCreator.createTopicSummaryReport(qualificationQuestions, qualificationDir, qualification);
                    System.out.println("Created analysis report for " + qualification);
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating topic analysis reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void generateAllTopicMocks() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            System.out.println("Loaded " + allQuestions.size() + " questions for comprehensive topic mock generation");

            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic
                    = groupQuestionsByQualificationAndTopic(allQuestions);

            for (Map.Entry<String, Map<String, List<Question>>> qualEntry : questionsByQualificationAndTopic.entrySet()) {
                String qualification = qualEntry.getKey();
                Map<String, List<Question>> topicMap = qualEntry.getValue();

                for (Map.Entry<String, List<Question>> topicEntry : topicMap.entrySet()) {
                    String topic = topicEntry.getKey();
                    List<Question> topicQuestions = topicEntry.getValue();

                    if (!topicQuestions.isEmpty()) {
                        List<Question> uniqueQuestions = removeDuplicateQuestions(topicQuestions);

                        // Create directory structure for individual topics under 'mocks'
                        File topicDir = new File(outputDir, FileUtils.sanitizeFileName(topic));
                        topicDir.mkdirs();

                        File mocksDir = new File(topicDir, "mocks");
                        mocksDir.mkdirs();

                        File allQuestionsDir = new File(topicDir, "all questions");
                        allQuestionsDir.mkdirs();

                        pdfMerger.createCombinedQuestionsPdf(uniqueQuestions, allQuestionsDir);

                        mockTestGenerator.createTopicSpecificMocks(uniqueQuestions, mocksDir,
                                qualification, topic, topic);

                        System.out.println("Generated individual topic resources for " + topic + " (" + qualification
                                + ") with " + uniqueQuestions.size() + " questions");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error generating individual topic resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate a comprehensive overview of all topics
     */
    public void generateTopicOverview() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic
                    = groupQuestionsByQualificationAndTopic(allQuestions);

            // Create overview directory as 'mocks/topic_overview'
            File overviewDir = new File(outputDir, "topic_overview");
            overviewDir.mkdirs();

            for (Map.Entry<String, Map<String, List<Question>>> qualEntry : questionsByQualificationAndTopic.entrySet()) {
                String qualification = qualEntry.getKey();
                Map<String, List<Question>> topicMap = qualEntry.getValue();

                File qualOverviewFile = new File(overviewDir, qualification + "_topic_overview.txt");

                try (java.io.PrintWriter writer = new java.io.PrintWriter(qualOverviewFile)) {
                    writer.println("=== " + qualification.toUpperCase() + " TOPIC OVERVIEW ===");
                    writer.println("Generated: " + java.time.LocalDateTime.now());
                    writer.println("Total topics: " + topicMap.size());
                    writer.println();

                    List<Map.Entry<String, List<Question>>> sortedTopics = topicMap.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .collect(Collectors.toList());

                    String currentTheme = "";
                    for (Map.Entry<String, List<Question>> topicEntry : sortedTopics) {
                        String topic = topicEntry.getKey();
                        List<Question> questions = removeDuplicateQuestions(topicEntry.getValue());

                        String theme = TopicConstants.getThemeFromTopic(topic);
                        if (!theme.equals(currentTheme)) {
                            currentTheme = theme;
                            writer.println();
                            writer.println("--- " + currentTheme + " ---");
                        }

                        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
                        writer.printf("%-50s | %3d questions | %4d total marks%n",
                                topic, questions.size(), totalMarks);
                    }
                }

                System.out.println("Created topic overview for " + qualification + ": " + qualOverviewFile.getName());
            }

        } catch (Exception e) {
            System.err.println("Error generating topic overview: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
