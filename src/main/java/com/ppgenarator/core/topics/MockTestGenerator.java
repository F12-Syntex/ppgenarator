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

            // Create short questions mocks if enabled
            if (createQ1To5OnlyMocks && !q1To5Questions.isEmpty()) {
                createShortQuestionsMocks(new ArrayList<>(q1To5Questions), mockTestsDir, qualification, topic);
            }

            // Create mixed mock tests with remaining questions
            createMixedMockTests(new ArrayList<>(q1To5Questions), new ArrayList<>(q6Questions),
                    mockTestsDir, qualification, topic);

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

    private void createShortQuestionsMocks(List<Question> q1To5Questions, File mockTestsDir, String qualification,
            String topic) throws IOException {
        System.out.println("Creating short questions mocks...");

        int totalAvailableMarks = q1To5Questions.stream().mapToInt(Question::getMarks).sum();

        if (totalAvailableMarks < targetMarksPerMock) {
            if (!q1To5Questions.isEmpty()) {
                createSingleMock(q1To5Questions, mockTestsDir, qualification, topic, "short questions mock", true);
            }
            return;
        }

        Collections.shuffle(q1To5Questions, new Random());
        int shortMocksCreated = 0;

        while (shortMocksCreated < minimumQ1To5MockTests && !q1To5Questions.isEmpty()) {
            List<Question> selectedQuestions = selectQuestionsForMock(q1To5Questions, targetMarksPerMock);

            if (selectedQuestions.isEmpty())
                break;

            String mockName = shortMocksCreated == 0 ? "short questions mock"
                    : "short questions mock" + (shortMocksCreated + 1);
            createSingleMock(selectedQuestions, mockTestsDir, qualification, topic, mockName, true);
            shortMocksCreated++;
        }

        System.out.println("Created " + shortMocksCreated + " short questions mocks");
    }

    private void createMixedMockTests(List<Question> q1To5Questions, List<Question> q6Questions,
            File mockTestsDir, String qualification, String topic) throws IOException {
        System.out.println("Creating mixed mock tests...");

        Collections.shuffle(q1To5Questions, new Random());
        Collections.shuffle(q6Questions, new Random());

        int mockTestNumber = 1;

        // Continue creating mocks while we have questions
        while (!q1To5Questions.isEmpty() || !q6Questions.isEmpty()) {
            List<Question> selectedQuestions = selectQuestionsForMixedMock(q1To5Questions, q6Questions,
                    targetMarksPerMock);

            if (selectedQuestions.isEmpty()) {
                break;
            }

            String mockName = "mock" + mockTestNumber;
            createSingleMock(selectedQuestions, mockTestsDir, qualification, topic, mockName, false);
            mockTestNumber++;
        }

        System.out.println("Created " + (mockTestNumber - 1) + " mixed mock tests");
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

    private List<Question> selectQuestionsForMixedMock(List<Question> q1To5Questions, List<Question> q6Questions,
            int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int totalMarks = 0;
        boolean hasQ6Question = false;
        String usedQ6Paper = null;

        // First, try to add one Q6 question if available
        if (!q6Questions.isEmpty()) {
            Question q6Question = q6Questions.remove(q6Questions.size() - 1);
            selected.add(q6Question);
            totalMarks += q6Question.getMarks();
            hasQ6Question = true;
            usedQ6Paper = QuestionUtils.getPaperIdentifier(q6Question);

            System.out.println("Added Q6 question from paper: " + usedQ6Paper +
                    ", marks so far: " + totalMarks + "/" + targetMarks);
        }

        // Then fill the rest with Q1-5 questions
        for (int i = q1To5Questions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
            Question question = q1To5Questions.get(i);
            selected.add(question);
            totalMarks += question.getMarks();
            q1To5Questions.remove(i);

            System.out.println("Added Q1-5 question: " + question.getQuestionNumber() +
                    ", marks so far: " + totalMarks + "/" + targetMarks);
        }

        // If we still have space and no Q6 question was added, try to add one
        if (!hasQ6Question && !q6Questions.isEmpty() && totalMarks < targetMarks) {
            Question q6Question = q6Questions.remove(q6Questions.size() - 1);
            selected.add(q6Question);
            totalMarks += q6Question.getMarks();
            usedQ6Paper = QuestionUtils.getPaperIdentifier(q6Question);

            System.out.println("Added Q6 question (second attempt) from paper: " + usedQ6Paper +
                    ", final marks: " + totalMarks);
        }

        return selected;
    }

    private void createSingleMock(List<Question> questions, File mockTestsDir, String qualification,
            String topic, String mockName, boolean isQ1To5Only) throws IOException {
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int estimatedMinutes = totalMarks * 2;

        File mockTestDir = new File(mockTestsDir, mockName);
        mockTestDir.mkdirs();

        // Count Q6 questions for verification
        long q6Count = questions.stream().filter(q -> QuestionUtils.isQuestion6(q.getQuestionNumber())).count();
        System.out.println("Creating " + mockName + " with " + questions.size() + " questions (" +
                q6Count + " Q6 questions, " + totalMarks + " marks)");

        File coverPageFile = isQ1To5Only
                ? coverPageCreator.createQ1To5CoverPage(1, questions, totalMarks, estimatedMinutes,
                        qualification, topic, mockTestDir)
                : coverPageCreator.createCoverPage(1, questions, totalMarks, estimatedMinutes, qualification,
                        topic, mockTestDir);

        mockTestPdfCreator.createMockTestPdfs(questions, mockTestDir, coverPageFile);
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