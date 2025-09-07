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
    private File outputDir;
    private int targetMarksPerMock = 20;
    private int mockTime = 25;
    private int minimumQ1To5MockTests = 0; // Disabled for single mock structure
    private boolean createQ1To5OnlyMocks = false; // Disabled for single mock structure

    private QuestionLoader questionLoader;
    private SingleMockGenerator singleMockGenerator;
    private PdfMerger pdfMerger;

    public TopicCompiler(File metadataDir, File parentOutputDir) {
        this.metadataDir = metadataDir;
        this.outputDir = parentOutputDir;
        if (!this.outputDir.exists()) {
            this.outputDir.mkdirs();
        }

        this.questionLoader = new QuestionLoader();
        this.singleMockGenerator = new SingleMockGenerator();
        this.pdfMerger = new PdfMerger();
    }

    public void setTargetMarksPerMock(int targetMarksPerMock) {
        this.targetMarksPerMock = targetMarksPerMock;
    }

    public void compileByTopic() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            System.out.println("Loaded " + allQuestions.size() + " questions from JSON files");

            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = groupQuestionsByQualificationAndTopic(
                    allQuestions);

            processQualificationTopics(questionsByQualificationAndTopic);

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

                processIndividualTopic(topic, topicQuestions, outputDir, qualification);
            }
        }
    }

    public void compileByUnit() throws Exception {
        List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
        Map<String, List<Question>> questionsByUnit = groupQuestionsByUnit(allQuestions);

        for (Map.Entry<String, List<Question>> entry : questionsByUnit.entrySet()) {
            String unit = entry.getKey();
            List<Question> questions = removeDuplicateQuestions(entry.getValue());

            if (questions.isEmpty())
                continue;

            // Create directory for this unit
            String unitDirName = unit.toLowerCase().replace(" ", "_");
            File unitDir = new File(outputDir, unitDirName);
            unitDir.mkdirs();

            System.out.println("Creating unit mock for " + unit + " with " + questions.size() + " questions");

            // Create merged PDF and mocks
            pdfMerger.createCombinedQuestionsPdf(questions, unitDir);
            singleMockGenerator.createSingleMock(questions, unitDir, "a level", unit);
        }
    }

    private Map<String, List<Question>> groupQuestionsByUnit(List<Question> allQuestions) {
        Map<String, List<Question>> unitQuestions = new HashMap<>();

        for (Question question : allQuestions) {
            if (question.getTopics() == null || question.getTopics().length == 0)
                continue;

            for (String topic : question.getTopics()) {
                String theme = TopicConstants.getThemeFromTopic(topic);
                unitQuestions.computeIfAbsent(theme, k -> new ArrayList<>()).add(question);
            }
        }
        return unitQuestions;
    }

    private void processIndividualTopic(String topic, List<Question> topicQuestions, File outputDir,
            String qualification) throws IOException {

        List<Question> uniqueTopicQuestions = removeDuplicateQuestions(topicQuestions);

        if (uniqueTopicQuestions.isEmpty()) {
            System.out.println("No unique questions found for topic: " + topic);
            return;
        }

        // Create directory for this topic directly in output directory
        String topicDirName = FileUtils.sanitizeFileName(topic);
        File topicDir = new File(outputDir, topicDirName);
        if (!topicDir.exists()) {
            topicDir.mkdirs();
        }

        System.out.println(
                "Creating single mock for " + topic + " with " + uniqueTopicQuestions.size() + " unique questions");

        // Create all questions with markscheme PDF
        pdfMerger.createCombinedQuestionsPdf(uniqueTopicQuestions, topicDir);

        // Create single mock test targeting specific time periods
        singleMockGenerator.createSingleMock(uniqueTopicQuestions, topicDir, qualification, topic);

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
                            q -> q.getQualification() != null
                                    ? convertQualificationName(q.getQualification().toString().toLowerCase())
                                    : "unknown"));

            for (Map.Entry<String, List<Question>> entry : questionsByQualification.entrySet()) {
                String qualification = entry.getKey();
                List<Question> qualificationQuestions = entry.getValue();

                if (outputDir.exists()) {
                    TopicSummaryReportCreator reportCreator = new TopicSummaryReportCreator();
                    reportCreator.createTopicSummaryReport(qualificationQuestions, outputDir, qualification);
                    System.out.println("Created analysis report for " + qualification);
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating topic analysis reports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate a comprehensive overview of all topics
     */
    public void generateTopicOverview() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = groupQuestionsByQualificationAndTopic(
                    allQuestions);

            // Create overview directory
            File overviewDir = new File(outputDir, "topic_overview");
            overviewDir.mkdirs();

            for (Map.Entry<String, Map<String, List<Question>>> qualEntry : questionsByQualificationAndTopic
                    .entrySet()) {
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