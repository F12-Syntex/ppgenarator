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

            // Check if we met the target marks
            int actualMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
            if (actualMarks < targetMarksPerMock) {
                System.out.println("Warning: Short questions mock " + (shortMocksCreated + 1) +
                        " only has " + actualMarks + " marks (target: " + targetMarksPerMock + ")");
            }

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

            // Check if we met the target marks
            int actualMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
            if (actualMarks < targetMarksPerMock) {
                System.out.println("Warning: Mixed mock " + mockTestNumber +
                        " only has " + actualMarks + " marks (target: " + targetMarksPerMock + ")");
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

        // First pass: try to get as close as possible to target marks
        for (int i = availableQuestions.size() - 1; i >= 0; i--) {
            Question question = availableQuestions.get(i);
            if (totalMarks + question.getMarks() <= targetMarks + 5) { // Allow small overage
                selected.add(question);
                totalMarks += question.getMarks();
                availableQuestions.remove(i);

                if (totalMarks >= targetMarks) {
                    break; // We've met the target
                }
            }
        }

        // Second pass: if we're still under target, add more questions regardless of
        // overage
        if (totalMarks < targetMarks && !availableQuestions.isEmpty()) {
            System.out.println("Second pass: current marks " + totalMarks + ", need " + targetMarks);

            for (int i = availableQuestions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
                Question question = availableQuestions.get(i);
                selected.add(question);
                totalMarks += question.getMarks();
                availableQuestions.remove(i);

                System.out.println("Added question with " + question.getMarks() +
                        " marks, total now: " + totalMarks);
            }
        }

        System.out.println("Selected " + selected.size() + " questions with " + totalMarks + " marks");
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

        // Fill with Q1-5 questions
        for (int i = q1To5Questions.size() - 1; i >= 0; i--) {
            Question question = q1To5Questions.get(i);

            if (totalMarks < targetMarks || (totalMarks + question.getMarks() <= targetMarks + 5)) {
                selected.add(question);
                totalMarks += question.getMarks();
                q1To5Questions.remove(i);

                System.out.println("Added Q1-5 question: " + question.getQuestionNumber() +
                        " (" + question.getMarks() + " marks), total: " + totalMarks + "/" + targetMarks);

                if (totalMarks >= targetMarks) {
                    break;
                }
            }
        }

        // If we still haven't met target and have no Q6 question, try to add one
        if (totalMarks < targetMarks && !hasQ6Question && !q6Questions.isEmpty()) {
            Question q6Question = q6Questions.remove(q6Questions.size() - 1);
            selected.add(q6Question);
            totalMarks += q6Question.getMarks();
            usedQ6Paper = QuestionUtils.getPaperIdentifier(q6Question);

            System.out.println("Added Q6 question (second attempt) from paper: " + usedQ6Paper +
                    ", final marks: " + totalMarks);
        }

        // Final attempt: if still under target, add more Q6 questions BUT ONLY from the
        // same paper
        if (totalMarks < targetMarks && usedQ6Paper != null) {
            System.out.println("Final attempt: looking for more Q6 questions from paper: " + usedQ6Paper);

            for (int i = q6Questions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
                Question question = q6Questions.get(i);

                // Only add Q6 questions from the same paper
                if (QuestionUtils.getPaperIdentifier(question).equals(usedQ6Paper)) {
                    selected.add(question);
                    totalMarks += question.getMarks();
                    q6Questions.remove(i);

                    System.out.println("Final attempt: added Q6 question from same paper with " +
                            question.getMarks() + " marks, total: " + totalMarks);
                }
            }
        }

        // Continue with Q1-5 questions if still needed
        if (totalMarks < targetMarks) {
            for (int i = q1To5Questions.size() - 1; i >= 0 && totalMarks < targetMarks; i--) {
                Question question = q1To5Questions.get(i);
                selected.add(question);
                totalMarks += question.getMarks();
                q1To5Questions.remove(i);

                System.out.println("Final attempt: added Q1-5 question with " + question.getMarks() +
                        " marks, total: " + totalMarks);
            }
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

        // Warning if under target
        if (totalMarks < targetMarksPerMock) {
            System.out.println("WARNING: " + mockName + " has only " + totalMarks +
                    " marks (target: " + targetMarksPerMock + ")");
        }

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