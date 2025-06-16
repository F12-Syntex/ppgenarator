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

public class SingleMockGenerator {

    // Target time periods in minutes
    private static final int[] TARGET_TIMES = {25, 30, 35, 40};
    private static final int TIME_TOLERANCE = 2; // Allow +/- 2 minutes

    private CoverPageCreator coverPageCreator;
    private MockTestPdfCreator mockTestPdfCreator;
    private MarkschemeCreator markschemeCreator;

    public SingleMockGenerator() {
        this.coverPageCreator = new CoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
        this.markschemeCreator = new MarkschemeCreator();
    }

    /**
     * Calculate the estimated time for a question based on marks and type
     * Non-context based: 1 mark = 2 minutes
     * Context based: 1 mark = 2.5 minutes
     */
    private int calculateQuestionTime(Question question) {
        int marks = question.getMarks();
        
        // Check if it's a context-based question (typically Q6 or Paper 3 Q1/Q2)
        if (QuestionUtils.isEssayStyleQuestion(question.getQuestionNumber()) || 
            QuestionUtils.isContextBasedQuestion(question)) {
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
     * Find the best target time that the calculated time can fit into
     */
    private int findBestTargetTime(int calculatedTime) {
        // Find the smallest target time that accommodates the calculated time
        for (int targetTime : TARGET_TIMES) {
            if (calculatedTime <= targetTime + TIME_TOLERANCE) {
                return targetTime;
            }
        }
        // If it exceeds all targets, use the largest target
        return TARGET_TIMES[TARGET_TIMES.length - 1];
    }

    /**
     * Create a single mock test for the given topic, targeting specific time periods
     */
    public void createSingleMock(List<Question> questions, File topicDir, String qualification, String topic) 
            throws IOException {
        
        // Remove duplicates
        List<Question> uniqueQuestions = removeDuplicates(questions);
        
        if (uniqueQuestions.isEmpty()) {
            System.out.println("No questions available for " + topic + " - skipping mock creation");
            return;
        }

        // Calculate total available time
        int totalAvailableTime = calculateTotalTime(uniqueQuestions);
        
        System.out.println("Total available time for " + topic + ": " + totalAvailableTime + " minutes");
        
        // If we have very limited questions, create a mock with all of them
        if (totalAvailableTime <= TARGET_TIMES[0] + TIME_TOLERANCE) {
            System.out.println("Limited questions for " + topic + " - creating mock with all available questions");
            createMockWithQuestions(uniqueQuestions, topicDir, qualification, topic);
            return;
        }

        // Select optimal questions for the best time target
        List<Question> selectedQuestions = selectOptimalQuestions(uniqueQuestions);
        
        if (selectedQuestions.isEmpty()) {
            // Fallback: create mock with all questions if selection fails
            System.out.println("Question selection failed - using all available questions");
            selectedQuestions = uniqueQuestions;
        }

        createMockWithQuestions(selectedQuestions, topicDir, qualification, topic);
    }

    /**
     * Select questions to optimally fit one of the target time periods
     */
    private List<Question> selectOptimalQuestions(List<Question> availableQuestions) {
        List<Question> bestSelection = new ArrayList<>();
        int bestScore = Integer.MAX_VALUE;
        int bestTargetTime = TARGET_TIMES[0];

        // Try to find the best combination for each target time
        for (int targetTime : TARGET_TIMES) {
            List<Question> candidate = selectQuestionsForTarget(new ArrayList<>(availableQuestions), targetTime);
            int candidateTime = calculateTotalTime(candidate);
            
            // Calculate how close we are to the target (prefer being under target)
            int score = Math.abs(candidateTime - targetTime);
            if (candidateTime > targetTime) {
                score += 5; // Penalty for going over
            }
            
            if (score < bestScore && !candidate.isEmpty()) {
                bestScore = score;
                bestSelection = candidate;
                bestTargetTime = targetTime;
            }
        }

        int actualTime = calculateTotalTime(bestSelection);
        System.out.println("Selected " + bestSelection.size() + " questions with " + actualTime + 
                         " minutes (targeting " + bestTargetTime + " minutes)");

        return bestSelection;
    }

    /**
     * Select questions to fit a specific target time
     */
    private List<Question> selectQuestionsForTarget(List<Question> availableQuestions, int targetTime) {
        List<Question> selected = new ArrayList<>();
        int currentTime = 0;
        
        // Separate questions by type for better selection
        List<Question> shortQuestions = new ArrayList<>();
        List<Question> essayQuestions = new ArrayList<>();
        
        for (Question q : availableQuestions) {
            if (QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber())) {
                essayQuestions.add(q);
            } else {
                shortQuestions.add(q);
            }
        }
        
        // Shuffle for variety
        Collections.shuffle(shortQuestions, new Random());
        Collections.shuffle(essayQuestions, new Random());
        
        // Sort by time to optimize selection
        shortQuestions.sort((a, b) -> Integer.compare(calculateQuestionTime(a), calculateQuestionTime(b)));
        essayQuestions.sort((a, b) -> Integer.compare(calculateQuestionTime(a), calculateQuestionTime(b)));
        
        // First, try to add one essay question if it fits
        if (!essayQuestions.isEmpty()) {
            for (Question essayQ : essayQuestions) {
                int essayTime = calculateQuestionTime(essayQ);
                if (essayTime <= targetTime - TIME_TOLERANCE) {
                    selected.add(essayQ);
                    currentTime += essayTime;
                    essayQuestions.remove(essayQ);
                    break;
                }
            }
        }
        
        // Fill remaining time with short questions
        for (int i = 0; i < shortQuestions.size(); i++) {
            Question question = shortQuestions.get(i);
            int questionTime = calculateQuestionTime(question);
            
            if (currentTime + questionTime <= targetTime + TIME_TOLERANCE) {
                selected.add(question);
                currentTime += questionTime;
                shortQuestions.remove(i);
                i--; // Adjust index after removal
                
                // Stop if we're close enough to target
                if (currentTime >= targetTime - TIME_TOLERANCE) {
                    break;
                }
            }
        }
        
        return selected;
    }

    /**
     * Create the actual mock test files
     */
    private void createMockWithQuestions(List<Question> questions, File topicDir, String qualification, String topic) 
            throws IOException {
        
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int actualTime = calculateTotalTime(questions);
        int targetTime = findBestTargetTime(actualTime);
        
        System.out.println("Creating mock for " + topic + ": " + questions.size() + " questions, " + 
                         totalMarks + " marks, " + actualTime + " minutes (using " + targetTime + " minute template)");

        // Create cover page directly in topic directory
        File coverPageFile = coverPageCreator.createCoverPage(1, questions, totalMarks, targetTime, 
                                                            qualification, topic, topicDir);

        // Create mock.pdf directly in topic directory
        createMockPdf(questions, topicDir, coverPageFile);

        // Create markscheme.pdf directly in topic directory
        createMarkscheme(questions, topicDir);
    }

    /**
     * Create the mock.pdf file
     */
    private void createMockPdf(List<Question> questions, File topicDir, File coverPageFile) throws IOException {
        // Use existing MockTestPdfCreator but modify output to be directly in topic directory
        MockTestPdfCreator pdfCreator = new MockTestPdfCreator();
        
        // Temporarily create a subdirectory structure that the existing code expects
        File tempMockDir = new File(topicDir, "temp_mock");
        tempMockDir.mkdirs();
        
        // Create the mock using existing infrastructure
        pdfCreator.createMockTestPdfs(questions, tempMockDir, coverPageFile);
        
        // Move the created mock.pdf to the topic directory
        File createdMock = new File(tempMockDir, "mock.pdf");
        File finalMock = new File(topicDir, "mock.pdf");
        
        if (createdMock.exists()) {
            if (finalMock.exists()) {
                finalMock.delete();
            }
            createdMock.renameTo(finalMock);
            System.out.println("Created mock.pdf: " + finalMock.getAbsolutePath());
        }
        
        // Clean up temporary directory
        if (tempMockDir.exists()) {
            File[] tempFiles = tempMockDir.listFiles();
            if (tempFiles != null) {
                for (File file : tempFiles) {
                    file.delete();
                }
            }
            tempMockDir.delete();
        }
    }

    /**
     * Create the markscheme.pdf file
     */
    private void createMarkscheme(List<Question> questions, File topicDir) throws IOException {
        // Create markscheme directly in topic directory
        File markschemeFile = markschemeCreator.createMockTestMarkscheme(questions, topicDir);
        
        if (markschemeFile != null) {
            // Rename to markscheme.pdf if it has a different name
            File finalMarkscheme = new File(topicDir, "markscheme.pdf");
            if (!markschemeFile.equals(finalMarkscheme)) {
                if (finalMarkscheme.exists()) {
                    finalMarkscheme.delete();
                }
                markschemeFile.renameTo(finalMarkscheme);
            }
            System.out.println("Created markscheme.pdf: " + finalMarkscheme.getAbsolutePath());
        }
    }

    /**
     * Remove duplicate questions based on file hash or question identifier
     */
    private List<Question> removeDuplicates(List<Question> questions) {
        List<Question> uniqueQuestions = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        for (Question question : questions) {
            String identifier = QuestionUtils.getQuestionIdentifier(question);
            if (!seenHashes.contains(identifier)) {
                seenHashes.add(identifier);
                uniqueQuestions.add(question);
            }
        }

        if (questions.size() != uniqueQuestions.size()) {
            System.out.println("Removed " + (questions.size() - uniqueQuestions.size()) + " duplicate questions");
        }

        return uniqueQuestions;
    }
}