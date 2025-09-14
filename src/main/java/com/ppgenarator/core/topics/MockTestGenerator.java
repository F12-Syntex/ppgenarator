package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.*;
import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class MockTestGenerator {

    private int targetMarksPerMock;
    private int minimumQ1To5MockTests;
    private boolean createQ1To5OnlyMocks;
    private int mockTime;
    private int maxMocksPerTopic = 1; // Maximum number of mocks per topic/major topic

    private CoverPageCreator coverPageCreator;
    private MockTestPdfCreator mockTestPdfCreator;

    public MockTestGenerator(int targetMarksPerMock, int minimumQ1To5MockTests,
            boolean createQ1To5OnlyMocks, int mockTime) {
        this.targetMarksPerMock = targetMarksPerMock;
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
        this.mockTime = mockTime;
        this.coverPageCreator = new CoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
    }

    /**
     * Calculate the estimated time for a question based on marks
     * 1–14 marks: 1 mark = 2 minutes
     * 15+ marks: 1 mark = 2.5 minutes
     */
    private int calculateQuestionTime(Question question) {
        int marks = question.getMarks();
        if (marks >= 15) {
            return (int) Math.round(marks * 2.5);
        } else {
            return marks * 2;
        }
    }

    private int calculateTotalTime(List<Question> questions) {
        return questions.stream().mapToInt(this::calculateQuestionTime).sum();
    }

    /**
     * Create mocks for a given topic pool
     */
    public void createMockTests(List<Question> questions, File mockTestsDir,
            String qualification, String topic) {
        try {
            List<Question> uniqueQuestions = removeDuplicates(questions);

            if (uniqueQuestions.isEmpty()) {
                System.out.println("No questions available for " + topic);
                return;
            }

            int totalMarks = uniqueQuestions.stream().mapToInt(Question::getMarks).sum();
            System.out.println(topic + " - total pool = " + uniqueQuestions.size() +
                    " questions, " + totalMarks + " marks");

            int possibleMocks = Math.min(maxMocksPerTopic,
                    Math.max(1, totalMarks / targetMarksPerMock));

            for (int i = 1; i <= possibleMocks; i++) {
                List<Question> poolCopy = new ArrayList<>(uniqueQuestions);
                List<Question> selected = selectBalancedQuestions(poolCopy, targetMarksPerMock);

                if (selected.isEmpty())
                    break;

                int actualMarks = selected.stream().mapToInt(Question::getMarks).sum();
                int fixedTime = calculateTotalTime(selected);

                String mockName = "mock" + i;
                System.out.println("Creating " + mockName + " for " + topic +
                        " with " + selected.size() + " questions → " +
                        actualMarks + " marks, " + fixedTime + " minutes");

                createSingleMockWithFixedTime(selected, mockTestsDir, qualification,
                        topic, mockName, false, fixedTime);

                // remove used from pool
                uniqueQuestions.removeAll(selected);
            }

        } catch (Exception e) {
            System.err.println("Error creating mocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Greedy selection towards ~targetMarksPerMock
     */
    private List<Question> selectQuestionsForMarksTarget(List<Question> availableQuestions, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int currentMarks = 0;
        final int tolerance = 10; // +/- tolerance

        // Shuffle for variety
        Collections.shuffle(availableQuestions, new Random());

        // Sort questions lowest → highest marks (build base then essays)
        availableQuestions.sort(Comparator.comparingInt(Question::getMarks));

        for (int i = 0; i < availableQuestions.size(); i++) {
            Question q = availableQuestions.get(i);
            if (currentMarks + q.getMarks() <= targetMarks + tolerance) {
                selected.add(q);
                currentMarks += q.getMarks();
                availableQuestions.remove(i);
                i--;
            }
            if (currentMarks >= targetMarks - tolerance)
                break;
        }

        System.out.println(">> Selected " + selected.size() + " Qs for ~" +
                currentMarks + " marks (target=" + targetMarks + ")");
        return selected;
    }

    private List<Question> removeDuplicates(List<Question> questions) {
        List<Question> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Question q : questions) {
            String id = QuestionUtils.getQuestionIdentifier(q);
            if (seen.add(id)) {
                unique.add(q);
            }
        }
        return unique;
    }

    private void createSingleMockWithFixedTime(List<Question> questions, File mockTestsDir,
            String qualification, String topic,
            String mockName, boolean isQ1To5Only,
            int fixedTime) throws IOException {
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int calculatedTime = calculateTotalTime(questions);

        File mockTestDir = new File(mockTestsDir, mockName);
        mockTestDir.mkdirs();

        long essayCount = questions.stream()
                .filter(q -> QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber()))
                .count();

        System.out.println(" → " + mockName + ": " + totalMarks + " marks, " +
                calculatedTime + " mins (" + essayCount + " essays)");

        File coverPageFile = coverPageCreator.createCoverPage(1, questions,
                totalMarks, fixedTime, qualification, topic, mockTestDir);

        mockTestPdfCreator.createMockTestPdfs(questions, mockTestDir, coverPageFile);
    }

    /**
     * Selects ~targetMarks worth of questions ensuring exactly ONE higher-mark
     * question (>=10 marks).
     */
    private List<Question> selectBalancedQuestions(List<Question> pool, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int currentMarks = 0;
        final int tolerance = 10;

        // Split pool into essays vs shorts
        List<Question> essays = new ArrayList<>();
        List<Question> shorts = new ArrayList<>();
        for (Question q : pool) {
            if (QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber()) || q.getMarks() >= 10) {
                essays.add(q);
            } else {
                shorts.add(q);
            }
        }
        Collections.shuffle(essays, new Random());
        Collections.shuffle(shorts, new Random());

        // 1. Add EXACTLY ONE essay if available
        if (!essays.isEmpty()) {
            Question chosenEssay = essays.remove(0);
            selected.add(chosenEssay);
            currentMarks += chosenEssay.getMarks();
        }

        // 2. Fill remainder with only short questions
        Iterator<Question> it = shorts.iterator();
        while (it.hasNext() && currentMarks < targetMarks - tolerance) {
            Question q = it.next();
            selected.add(q);
            currentMarks += q.getMarks();
            it.remove();
        }

        // Log result
        System.out.println(">> Balanced selection: " + selected.size() +
                " questions → " + currentMarks + " marks (1 essay + " +
                (selected.size() - 1) + " shorts)");

        pool.removeAll(selected);
        return selected;
    }

    private List<Question> selectBalancedQuestionsForMarks(List<Question> pool, int targetMarks) {
        List<Question> selected = new ArrayList<>();
        int currentMarks = 0;
        final int tolerance = 10;

        // Separate pools
        List<Question> essays = new ArrayList<>();
        List<Question> shorts = new ArrayList<>();
        for (Question q : pool) {
            if (QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber()) || q.getMarks() >= 10) {
                essays.add(q);
            } else {
                shorts.add(q);
            }
        }
        Collections.shuffle(essays, new Random());
        Collections.shuffle(shorts, new Random());

        // 1. Ensure at least one essay if possible
        if (!essays.isEmpty()) {
            Question e = essays.remove(0);
            selected.add(e);
            currentMarks += e.getMarks();
        }

        // 2. Fill with shorts until near target
        Iterator<Question> it = shorts.iterator();
        while (it.hasNext() && currentMarks < targetMarks - tolerance) {
            Question q = it.next();
            selected.add(q);
            currentMarks += q.getMarks();
            it.remove();
        }

        // 3. Optionally add another essay if under target
        if (currentMarks < targetMarks - 15 && !essays.isEmpty()) {
            Question e2 = essays.remove(0);
            selected.add(e2);
            currentMarks += e2.getMarks();
        }

        System.out.println(">> Balanced selection: " + selected.size() +
                " questions → " + currentMarks + " marks (target " + targetMarks + ")");
        pool.removeAll(selected);
        return selected;
    }

    // setters
    public void setTargetMarksPerMock(int targetMarksPerMock) {
        this.targetMarksPerMock = targetMarksPerMock;
    }

    public void setMinimumQ1To5MockTests(int minimumQ1To5MockTests) {
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
    }

    public void setCreateQ1To5OnlyMocks(boolean createQ1To5OnlyMocks) {
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
    }

    public void setMaxMocksPerTopic(int maxMocksPerTopic) {
        this.maxMocksPerTopic = maxMocksPerTopic;
    }
}