package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ppgenarator.ai.TopicConstants;
import com.ppgenarator.utils.FileUtils;
import com.ppgenerator.types.Qualification;
import com.ppgenerator.types.Question;

public class TopicCompiler {

    private File metadataDir;
    private File outputDir;
    public QuestionLoader questionLoader;
    private SingleMockGenerator singleMockGenerator;
    private PdfMerger pdfMerger;
    private UnitMockGenerator unitMockGenerator;

    public TopicCompiler(File metadataDir, File parentOutputDir) {
        this.metadataDir = metadataDir;
        this.outputDir = parentOutputDir;
        if (!this.outputDir.exists()) {
            this.outputDir.mkdirs();
        }

        this.questionLoader = new QuestionLoader();
        this.singleMockGenerator = new SingleMockGenerator();
        this.pdfMerger = new PdfMerger();
        this.unitMockGenerator = new UnitMockGenerator();
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

    /**
     * New unit mocks logic - creates special unit tests with one topic from each major section
     */
    public void compileByUnit() throws Exception {
        List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
        
        // Group questions by their topics for theme-based selection
        Map<String, List<Question>> questionsByTopic = new HashMap<>();
        for (Question q : allQuestions) {
            if (q.getTopics() != null) {
                for (String topic : q.getTopics()) {
                    questionsByTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(q);
                }
            }
        }

        // Create unit mocks for each theme
        for (int themeNum = 1; themeNum <= 4; themeNum++) {
            createThemeUnitMocks(themeNum, questionsByTopic);
        }
    }

    private void createThemeUnitMocks(int themeNum, Map<String, List<Question>> questionsByTopic) throws IOException {
        String themeName = "Theme " + themeNum;
        File unitBaseDir = new File(outputDir, "unit mocks" + File.separator + "theme" + themeNum);
        unitBaseDir.mkdirs();

        System.out.println("\n=== Creating Unit Mocks for " + themeName + " ===");

        // Create 3 different mocks for this theme
        for (int mockNum = 1; mockNum <= 3; mockNum++) {
            File mockDir = new File(unitBaseDir, "mock" + mockNum);
            mockDir.mkdirs();

            // Generate special unit mock with topic selection
            unitMockGenerator.createSpecialUnitMock(themeNum, questionsByTopic, mockDir, mockNum);
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

                processIndividualTopic(topic, topicQuestions, outputDir, qualification);
            }
        }
    }

    private void processIndividualTopic(String topic, List<Question> topicQuestions, File outputDir,
            String qualification) throws IOException {
        List<Question> uniqueTopicQuestions = removeDuplicateQuestions(topicQuestions);
        if (uniqueTopicQuestions.isEmpty())
            return;

        String topicDirName = FileUtils.sanitizeFileName(topic);
        File topicDir = new File(outputDir, topicDirName);
        if (!topicDir.exists())
            topicDir.mkdirs();

        pdfMerger.createCombinedQuestionsPdf(uniqueTopicQuestions, topicDir);

        singleMockGenerator.createSingleMock(uniqueTopicQuestions, topicDir, qualification, topic);
    }

    private List<Question> removeDuplicateQuestions(List<Question> questions) {
        Map<String, Question> uniqueQuestions = new HashMap<>();
        for (Question question : questions) {
            String identifier = getQuestionIdentifier(question);
            if (!uniqueQuestions.containsKey(identifier)) {
                uniqueQuestions.put(identifier, question);
            }
        }
        return new ArrayList<>(uniqueQuestions.values());
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

    public void generateTopicOverview() {
        try {
            List<Question> allQuestions = questionLoader.loadQuestionsFromJsonFiles(metadataDir);
            Map<String, Map<String, List<Question>>> questionsByQualificationAndTopic = groupQuestionsByQualificationAndTopic(
                    allQuestions);

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