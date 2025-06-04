package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class MockTestGenerator {

    private int targetMarksPerMock;
    private int minimumQ1To5MockTests;
    private boolean createQ1To5OnlyMocks;

    private CoverPageCreator coverPageCreator;
    private MockTestPdfCreator mockTestPdfCreator;

    public MockTestGenerator(int targetMarksPerMock, int minimumQ1To5MockTests, boolean createQ1To5OnlyMocks) {
        this.targetMarksPerMock = targetMarksPerMock;
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
        this.coverPageCreator = new CoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
    }

    public void createMockTests(List<Question> questions, File mockTestsDir, String qualification, String topic) {
        try {
            // Remove duplicates
            List<Question> uniqueQuestions = removeDuplicates(questions);

            // Separate questions into Q1-5 and Q6
            List<Question> q1To5Questions = new ArrayList<>();
            List<Question> q6Questions = new ArrayList<>();

            for (Question question : uniqueQuestions) {
                if (QuestionUtils.isQuestion6(question.getQuestionNumber())) {
                    q6Questions.add(question);
                } else {
                    q1To5Questions.add(question);
                }
            }

            System.out.println("Found " + q1To5Questions.size() + " Q1-5 questions and " +
                    q6Questions.size() + " Q6 questions");

            int mockTestNumber = 1;

            // Create Q1-5 only mock tests if enabled
            if (createQ1To5OnlyMocks && !q1To5Questions.isEmpty()) {
                mockTestNumber = createQ1To5OnlyMocks(q1To5Questions, mockTestsDir, qualification, topic,
                        mockTestNumber);
            }

            // Create mixed mock tests
            createMixedMockTests(q1To5Questions, q6Questions, mockTestsDir, qualification, topic, mockTestNumber);

        } catch (Exception e) {
            System.err.println("Error creating mock tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Question> removeDuplicates(List<Question> questions) {
        List<Question> uniqueQuestions = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        for (Question question : questions) {
            String identifier = QuestionUtils.getQuestionIdentifier(question);
            if (!seenHashes.contains(identifier)) {
                seenHashes.add(identifier);
                uniqueQuestions.add(question);
            } else {
                System.out.println(
                        "Removing duplicate question: " + question.getYear() + "_" + question.getQuestionNumber());
            }
        }

        return uniqueQuestions;
    }

    private int createQ1To5OnlyMocks(List<Question> q1To5Questions, File mockTestsDir, String qualification,
            String topic, int startingMockNumber) throws IOException {
        System.out.println("Creating Q1-5 only mock tests...");

        int totalAvailableMarks = q1To5Questions.stream().mapToInt(Question::getMarks).sum();

        if (totalAvailableMarks < targetMarksPerMock) {
            if (!q1To5Questions.isEmpty()) {
                createSingleMock(q1To5Questions, mockTestsDir, qualification, topic, startingMockNumber, true);
                return startingMockNumber + 1;
            }
            return startingMockNumber;
        }

        List<Question> availableQ1To5 = new ArrayList<>(q1To5Questions);
        Collections.shuffle(availableQ1To5, new Random());

        int mockTestNumber = startingMockNumber;
        int q1To5MocksCreated = 0;

        while (q1To5MocksCreated < minimumQ1To5MockTests && !availableQ1To5.isEmpty()) {
            List<Question> selectedQuestions = selectQuestionsForMock(availableQ1To5, targetMarksPerMock);

            if (selectedQuestions.isEmpty())
                break;

            createMockTest(selectedQuestions, mockTestsDir, qualification, topic, mockTestNumber, true);
            mockTestNumber++;
            q1To5MocksCreated++;
        }

        System.out.println("Created " + q1To5MocksCreated + " Q1-5 only mock tests");
        return mockTestNumber;
    }

    private void createMixedMockTests(List<Question> remainingQ1To5, List<Question> q6Questions,
            File mockTestsDir, String qualification, String topic, int startingMockNumber) throws IOException {
        System.out.println("Creating mixed mock tests...");

        List<Question> allRemainingQuestions = new ArrayList<>();
        allRemainingQuestions.addAll(remainingQ1To5);
        allRemainingQuestions.addAll(q6Questions);

        Collections.shuffle(allRemainingQuestions, new Random());

        int mockTestNumber = startingMockNumber;

        while (!allRemainingQuestions.isEmpty()) {
            List<Question> selectedQuestions = selectQuestionsForMixedMock(allRemainingQuestions, targetMarksPerMock);

            if (selectedQuestions.isEmpty())
                break;

            createMockTest(selectedQuestions, mockTestsDir, qualification, topic, mockTestNumber, false);
            mockTestNumber++;
        }
    }

    private List<Question> selectQuestionsForMock(List<Question> availableQuestions, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int totalMarks = 0;

        for (int i = availableQuestions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
            Question question = availableQuestions.get(i);
            selected.add(question);
            totalMarks += question.getMarks();
            availableQuestions.remove(i);
        }

        return selected;
    }

    private List<Question> selectQuestionsForMixedMock(List<Question> availableQuestions, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        Set<String> usedPapers = new HashSet<>();
        int totalMarks = 0;

        for (int i = availableQuestions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
            Question question = availableQuestions.get(i);

            if (QuestionUtils.isQuestion6(question.getQuestionNumber())) {
                String paperIdentifier = QuestionUtils.getPaperIdentifier(question);
                if (usedPapers.contains(paperIdentifier)) {
                    continue;
                }
                usedPapers.add(paperIdentifier);
            }

            selected.add(question);
            totalMarks += question.getMarks();
            availableQuestions.remove(i);
        }

        return selected;
    }

    private void createSingleMock(List<Question> questions, File mockTestsDir, String qualification,
            String topic, int mockNumber, boolean isQ1To5Only) throws IOException {
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int estimatedMinutes = totalMarks * 2;

        String dirName = isQ1To5Only ? "mock" + mockNumber + "_q1-5_only" : "mock" + mockNumber;
        File mockTestDir = new File(mockTestsDir, dirName);
        mockTestDir.mkdirs();

        File coverPageFile = isQ1To5Only
                ? coverPageCreator.createQ1To5CoverPage(mockNumber, questions, totalMarks, estimatedMinutes,
                        qualification, topic, mockTestDir)
                : coverPageCreator.createCoverPage(mockNumber, questions, totalMarks, estimatedMinutes, qualification,
                        topic, mockTestDir);

        mockTestPdfCreator.createMockTestPdfs(questions, mockTestDir, coverPageFile);
    }

    private void createMockTest(List<Question> questions, File mockTestsDir, String qualification,
            String topic, int mockNumber, boolean isQ1To5Only) throws IOException {
        createSingleMock(questions, mockTestsDir, qualification, topic, mockNumber, isQ1To5Only);
    }

    // Setters
    public void setTargetMarksPerMock(int targetMarksPerMock) {
        this.targetMarksPerMock = targetMarksPerMock;
    }

    public void setMinimumQ1To5MockTests(int minimumQ1To5MockTests) {
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
    }

    public void setCreateQ1To5OnlyMocks(boolean createQ1To5OnlyMocks) {
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
    }
}