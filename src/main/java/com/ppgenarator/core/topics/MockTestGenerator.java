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
    private int mockTime;
    private int maxMocksPerTopic = 1; // Maximum number of mocks per topic/major topic
    
    // Fixed time periods for mocks (in minutes)
    private static final int[] TARGET_TIMES = {25, 30, 35};

    private CoverPageCreator coverPageCreator;
    private MockTestPdfCreator mockTestPdfCreator;

    public MockTestGenerator(int targetMarksPerMock, int minimumQ1To5MockTests, boolean createQ1To5OnlyMocks,
            int mockTime) {
        this.targetMarksPerMock = targetMarksPerMock;
        this.minimumQ1To5MockTests = minimumQ1To5MockTests;
        this.createQ1To5OnlyMocks = createQ1To5OnlyMocks;
        this.mockTime = mockTime;
        this.coverPageCreator = new CoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
    }

    /**
     * Calculate the estimated time for a question based on marks
     * 1-14 marks: 1 mark = 2 minutes
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

    /**
     * Calculate the total estimated time for a list of questions
     */
    private int calculateTotalTime(List<Question> questions) {
        int totalMinutes = 0;
        for (Question question : questions) {
            totalMinutes += calculateQuestionTime(question);
        }
        return totalMinutes;
    }

    /**
     * Find the best target time from our fixed options
     */
    private int findBestTargetTime(int calculatedTime) {
        // Find the smallest target time that is >= calculatedTime
        for (int targetTime : TARGET_TIMES) {
            if (targetTime >= calculatedTime) {
                return targetTime;
            }
        }
        // If calculated time is greater than our largest target, use the largest
        return TARGET_TIMES[TARGET_TIMES.length - 1];
    }

    public void createMockTests(List<Question> questions, File mockTestsDir, String qualification, String topic) {
        try {
            // Remove duplicates
            List<Question> uniqueQuestions = removeDuplicates(questions);

            // Check if we have enough questions to create meaningful mocks
            if (uniqueQuestions.isEmpty()) {
                System.out.println("No questions available for " + topic + " - skipping mock creation");
                return;
            }

            int totalAvailableMarks = uniqueQuestions.stream().mapToInt(Question::getMarks).sum();
            int totalAvailableTime = calculateTotalTime(uniqueQuestions);
            
            // If we don't have enough content for even one mock, create a single mock with all available questions
            if (totalAvailableTime < TARGET_TIMES[0]) {
                System.out.println("Limited questions for " + topic + " (" + totalAvailableTime + " minutes) - creating single short mock");
                createSingleMock(uniqueQuestions, mockTestsDir, qualification, topic, "mock", false);
                return;
            }

            // Calculate how many mocks we can realistically create based on time
            int possibleMocksByTime = totalAvailableTime / TARGET_TIMES[0]; // Use shortest time as baseline
            int possibleMocks = Math.min(maxMocksPerTopic, possibleMocksByTime);
            
            // Ensure we create at least 1 mock if we have questions
            possibleMocks = Math.max(1, possibleMocks);
            
            System.out.println("Creating " + possibleMocks + " mocks for " + topic + " (max " + maxMocksPerTopic + 
                             ", available time: " + totalAvailableTime + " minutes)");

            // Separate questions into Q1-5 and essay-style questions (Q6, Paper3 Q1/Q2)
            List<Question> q1To5Questions = new ArrayList<>();
            List<Question> essayQuestions = new ArrayList<>();

            for (Question question : uniqueQuestions) {
                if (QuestionUtils.isEssayStyleQuestion(question.getQuestionNumber())) {
                    essayQuestions.add(question);
                } else {
                    q1To5Questions.add(question);
                }
            }

            System.out.println("Found " + q1To5Questions.size() + " Q1-5 questions and "
                    + essayQuestions.size() + " essay-style questions (Q6, Paper3 Q1/Q2)");

            // Create short questions mocks if enabled (limited to max mocks)
            if (createQ1To5OnlyMocks && !q1To5Questions.isEmpty()) {
                int shortMocksToCreate = Math.min(possibleMocks, minimumQ1To5MockTests);
                createLimitedShortQuestionsMocks(new ArrayList<>(q1To5Questions), mockTestsDir, qualification, topic, shortMocksToCreate);
            }

            // Create mixed mock tests with remaining questions (limited to max mocks)
            createLimitedMixedMockTests(new ArrayList<>(q1To5Questions), new ArrayList<>(essayQuestions),
                    mockTestsDir, qualification, topic, possibleMocks);

        } catch (Exception e) {
            System.err.println("Error creating mock tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createTopicSpecificMocks(List<Question> questions, File mockTestsDir, String qualification, 
                                       String topic, String subTopic) {
        try {
            // Remove duplicates
            List<Question> uniqueQuestions = removeDuplicates(questions);

            if (uniqueQuestions.isEmpty()) {
                System.out.println("No questions available for " + subTopic + " - skipping mock creation");
                return;
            }

            int totalAvailableTime = calculateTotalTime(uniqueQuestions);
            
            // Create at least one mock per topic, even if it's short
            if (totalAvailableTime < TARGET_TIMES[0]) {
                System.out.println("Limited questions for " + subTopic + " (" + totalAvailableTime + 
                                 " minutes) - creating single focused mock");
                createSingleMock(uniqueQuestions, mockTestsDir, qualification, subTopic, "topic mock", false);
                return;
            }

            // For topics with sufficient content, create 1-2 focused mocks
            int possibleMocks = Math.min(2, totalAvailableTime / TARGET_TIMES[0]);
            possibleMocks = Math.max(1, possibleMocks);
            
            System.out.println("Creating " + possibleMocks + " topic-specific mocks for " + subTopic + 
                             " (available time: " + totalAvailableTime + " minutes)");

            // Separate questions into Q1-5 and essay-style questions
            List<Question> q1To5Questions = new ArrayList<>();
            List<Question> essayQuestions = new ArrayList<>();

            for (Question question : uniqueQuestions) {
                if (QuestionUtils.isEssayStyleQuestion(question.getQuestionNumber())) {
                    essayQuestions.add(question);
                } else {
                    q1To5Questions.add(question);
                }
            }

            // Create focused mocks for this specific topic
            createTopicFocusedMocks(q1To5Questions, essayQuestions, mockTestsDir, qualification, 
                                  subTopic, possibleMocks);

        } catch (Exception e) {
            System.err.println("Error creating topic-specific mock tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTopicFocusedMocks(List<Question> q1To5Questions, List<Question> essayQuestions,
                                       File mockTestsDir, String qualification, String subTopic, 
                                       int maxMocks) throws IOException {
        Collections.shuffle(q1To5Questions, new Random());
        Collections.shuffle(essayQuestions, new Random());

        int mockNumber = 1;

        while (mockNumber <= maxMocks && (!q1To5Questions.isEmpty() || !essayQuestions.isEmpty())) {
            List<Question> selectedQuestions = selectQuestionsForMixedMockByTime(
                new ArrayList<>(q1To5Questions), new ArrayList<>(essayQuestions), TARGET_TIMES);

            if (selectedQuestions.isEmpty()) {
                break;
            }

            // Calculate actual time and find appropriate target
            int actualTime = calculateTotalTime(selectedQuestions);
            int targetTime = findBestTargetTime(actualTime);
            int actualMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
            
            System.out.println("Topic mock " + mockNumber + " for " + subTopic + ": " + 
                             actualMarks + " marks, " + actualTime + " minutes (targeting " + 
                             targetTime + " minutes)");

            String mockName = "topic mock" + (mockNumber > 1 ? " " + mockNumber : "");
            createSingleMockWithFixedTime(selectedQuestions, mockTestsDir, qualification, 
                                        subTopic, mockName, false, targetTime);
            
            // Remove used questions from the pools
            q1To5Questions.removeAll(selectedQuestions);
            essayQuestions.removeAll(selectedQuestions);
            
            mockNumber++;
        }

        System.out.println("Created " + (mockNumber - 1) + " topic-focused mocks for " + subTopic);
    }

    private void createLimitedShortQuestionsMocks(List<Question> q1To5Questions, File mockTestsDir, String qualification,
            String topic, int maxMocks) throws IOException {
        System.out.println("Creating limited short questions mocks (max " + maxMocks + ")...");

        int totalAvailableTime = calculateTotalTime(q1To5Questions);

        if (totalAvailableTime < TARGET_TIMES[0]) {
            if (!q1To5Questions.isEmpty()) {
                createSingleMock(q1To5Questions, mockTestsDir, qualification, topic, "short questions mock", true);
            }
            return;
        }

        Collections.shuffle(q1To5Questions, new Random());
        int shortMocksCreated = 0;

        while (shortMocksCreated < maxMocks && shortMocksCreated < minimumQ1To5MockTests && !q1To5Questions.isEmpty()) {
            List<Question> selectedQuestions = selectQuestionsForTimeTarget(q1To5Questions, TARGET_TIMES);

            if (selectedQuestions.isEmpty()) {
                break;
            }

            // Calculate actual time and find appropriate target
            int actualTime = calculateTotalTime(selectedQuestions);
            int targetTime = findBestTargetTime(actualTime);
            int actualMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
            
            System.out.println("Short questions mock " + (shortMocksCreated + 1) + ": " + 
                             actualMarks + " marks, " + actualTime + " minutes (targeting " + targetTime + " minutes)");

            String mockName = shortMocksCreated == 0 ? "short questions mock"
                    : "short questions mock" + (shortMocksCreated + 1);
            createSingleMockWithFixedTime(selectedQuestions, mockTestsDir, qualification, topic, mockName, true, targetTime);
            shortMocksCreated++;
        }

        System.out.println("Created " + shortMocksCreated + " short questions mocks (limited to " + maxMocks + ")");
    }

    private void createLimitedMixedMockTests(List<Question> q1To5Questions, List<Question> essayQuestions,
            File mockTestsDir, String qualification, String topic, int maxMocks) throws IOException {
        System.out.println("Creating limited mixed mock tests (max " + maxMocks + ")...");

        Collections.shuffle(q1To5Questions, new Random());
        Collections.shuffle(essayQuestions, new Random());

        int mockTestNumber = 1;

        // Continue creating mocks while we have questions and haven't hit the limit
        while (mockTestNumber <= maxMocks && (!q1To5Questions.isEmpty() || !essayQuestions.isEmpty())) {
            List<Question> selectedQuestions = selectQuestionsForMixedMockByTime(q1To5Questions, essayQuestions, TARGET_TIMES);

            if (selectedQuestions.isEmpty()) {
                break;
            }

            // Calculate actual time and find appropriate target
            int actualTime = calculateTotalTime(selectedQuestions);
            int targetTime = findBestTargetTime(actualTime);
            int actualMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
            
            System.out.println("Mixed mock " + mockTestNumber + ": " + 
                             actualMarks + " marks, " + actualTime + " minutes (targeting " + targetTime + " minutes)");

            String mockName = "mock" + mockTestNumber;
            createSingleMockWithFixedTime(selectedQuestions, mockTestsDir, qualification, topic, mockName, false, targetTime);
            mockTestNumber++;
        }

        System.out.println("Created " + (mockTestNumber - 1) + " mixed mock tests (limited to " + maxMocks + ")");
    }

    /**
     * Select questions to fit within one of the target time periods
     */
    private List<Question> selectQuestionsForTimeTarget(List<Question> availableQuestions, int[] targetTimes) {
        List<Question> selected = new ArrayList<>();
        int currentTime = 0;

        // Sort by time to optimize selection
        availableQuestions.sort((a, b) -> Integer.compare(calculateQuestionTime(a), calculateQuestionTime(b)));

        // Try to fill up to the largest target time, but accept any of the target times
        int maxTargetTime = targetTimes[targetTimes.length - 1];

        for (int i = 0; i < availableQuestions.size(); i++) {
            Question question = availableQuestions.get(i);
            int questionTime = calculateQuestionTime(question);
            
            // Check if adding this question would still keep us within reasonable bounds
            if (currentTime + questionTime <= maxTargetTime + 5) { // Allow small overage
                selected.add(question);
                currentTime += questionTime;
                availableQuestions.remove(i);
                i--; // Adjust index after removal
                
                // Check if we've hit one of our target times
                for (int targetTime : targetTimes) {
                    if (currentTime >= targetTime - 2 && currentTime <= targetTime + 5) {
                        System.out.println("Hit target time range for " + targetTime + " minutes with " + currentTime + " minutes");
                        return selected;
                    }
                }
            }
        }

        // If we didn't hit a target exactly, but have questions, return what we have
        if (!selected.isEmpty()) {
            System.out.println("Selected " + selected.size() + " questions with " + currentTime + " minutes (no exact target hit)");
        }
        
        return selected;
    }

    /**
     * Select questions for mixed mock targeting specific time periods
     */
    private List<Question> selectQuestionsForMixedMockByTime(List<Question> q1To5Questions, List<Question> essayQuestions, int[] targetTimes) {
        List<Question> selected = new ArrayList<>();
        int currentTime = 0;
        boolean hasEssayQuestion = false;
        String usedEssayPaper = null;
        int maxTargetTime = targetTimes[targetTimes.length - 1];

        // First, try to add one essay question if available and it fits
        if (!essayQuestions.isEmpty()) {
            Question essayQuestion = essayQuestions.get(essayQuestions.size() - 1);
            int essayTime = calculateQuestionTime(essayQuestion);
            
            // Only add essay if it doesn't exceed our largest target time
            if (essayTime <= maxTargetTime) {
                essayQuestions.remove(essayQuestions.size() - 1);
                selected.add(essayQuestion);
                currentTime += essayTime;
                hasEssayQuestion = true;
                usedEssayPaper = QuestionUtils.getPaperIdentifier(essayQuestion);
                
                System.out.println("Added essay question: " + essayTime + " minutes, total: " + currentTime);
            }
        }

        // Fill with Q1-5 questions
        q1To5Questions.sort((a, b) -> Integer.compare(calculateQuestionTime(a), calculateQuestionTime(b)));
        
        for (int i = 0; i < q1To5Questions.size(); i++) {
            Question question = q1To5Questions.get(i);
            int questionTime = calculateQuestionTime(question);
            
            if (currentTime + questionTime <= maxTargetTime + 5) {
                selected.add(question);
                currentTime += questionTime;
                q1To5Questions.remove(i);
                i--;
                
                // Check if we've hit one of our target times
                for (int targetTime : targetTimes) {
                    if (currentTime >= targetTime - 2 && currentTime <= targetTime + 5) {
                        System.out.println("Mixed mock hit target time range for " + targetTime + " minutes with " + currentTime + " minutes");
                        return selected;
                    }
                }
            }
        }

        return selected;
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

    private void createSingleMock(List<Question> questions, File mockTestsDir, String qualification,
            String topic, String mockName, boolean isQ1To5Only) throws IOException {
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int calculatedTime = calculateTotalTime(questions);
        int targetTime = findBestTargetTime(calculatedTime);

        createSingleMockWithFixedTime(questions, mockTestsDir, qualification, topic, mockName, isQ1To5Only, targetTime);
    }

    private void createSingleMockWithFixedTime(List<Question> questions, File mockTestsDir, String qualification,
            String topic, String mockName, boolean isQ1To5Only, int fixedTime) throws IOException {
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int calculatedTime = calculateTotalTime(questions);

        File mockTestDir = new File(mockTestsDir, mockName);
        mockTestDir.mkdirs();

        // Count essay questions for verification
        long essayCount = questions.stream().filter(q -> QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber())).count();
        System.out.println("Creating " + mockName + " with " + questions.size() + " questions (" +
                essayCount + " essay questions, " + totalMarks + " marks, " + calculatedTime + 
                " calculated minutes → " + fixedTime + " minutes fixed)");

        File coverPageFile = isQ1To5Only
                ? coverPageCreator.createQ1To5CoverPage(1, questions, totalMarks, fixedTime,
                        qualification, topic, mockTestDir)
                : coverPageCreator.createCoverPage(1, questions, totalMarks, fixedTime, qualification,
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

    public void setMaxMocksPerTopic(int maxMocksPerTopic) {
        this.maxMocksPerTopic = maxMocksPerTopic;
    }
}