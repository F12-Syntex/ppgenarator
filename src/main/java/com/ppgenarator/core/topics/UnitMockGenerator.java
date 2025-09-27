package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class UnitMockGenerator {
    
    private static final int TARGET_TIME = 75; // minutes
    private static final int TIME_TOLERANCE = 15;
    private static final int QUESTIONS_PER_SECTION = 2;
    private static final Random RANDOM = new Random();
    
    private final UnitMockCoverPageCreator coverPageCreator;
    private final MockTestPdfCreator mockTestPdfCreator;
    private final MarkschemeCreator markschemeCreator;
    
    public UnitMockGenerator() {
        this.coverPageCreator = new UnitMockCoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
        this.markschemeCreator = new MarkschemeCreator();
    }
    
    public void createSpecialUnitMock(int themeNum,
                                      Map<String, List<Question>> questionsByTopic,
                                      File mockDir,
                                      int mockNum) throws IOException {
        
        System.out.println("\n=== Creating Mock " + mockNum + " for Theme " + themeNum + " ===");
        
        List<String> majorSections = getMajorSectionsForTheme(themeNum);
        List<Question> allSelected = new ArrayList<>();
        
        for (String majorSection : majorSections) {
            List<String> sectionTopics = getTopicsInSection(themeNum, majorSection, questionsByTopic.keySet());
            
            if (sectionTopics.isEmpty()) {
                System.out.println("No valid topics found for " + majorSection);
                continue;
            }
            
            // randomly pick a topic
            String selectedTopic = sectionTopics.get(RANDOM.nextInt(sectionTopics.size()));
            List<Question> topicQuestions = questionsByTopic.get(selectedTopic);
            
            if (topicQuestions != null && !topicQuestions.isEmpty()) {
                // 🔍 DEBUG: show raw questions for this topic
                System.out.println("\n--- Raw Questions for Topic: " + selectedTopic + " ---");
                for (Question q : topicQuestions) {
                    printQuestionDebug(q);
                }
                
                // Apply filters
                for (Iterator<Question> it = topicQuestions.iterator(); it.hasNext();) {
                    Question q = it.next();
                    
                    // must have topics
                    if (q.getTopics() == null || q.getTopics().length == 0) {
                        it.remove();
                        continue;
                    }
                    
                    // must match this theme’s tags
                    String[] filtered = Arrays.stream(q.getTopics())
                                              .filter(t -> t.startsWith(themeNum + "."))
                                              .toArray(String[]::new);
                    if (filtered.length == 0) {
                        System.out.println("⚠ Removing " + q.getQuestionNumber() 
                            + " – wrong theme tags " + Arrays.toString(q.getTopics()));
                        it.remove();
                        continue;
                    }
                    q.setTopics(filtered);
                    
                    // ⛔ exclude 10+ markers
                    if (q.getMarks() >= 10) {
                        System.out.println("⚠ Removing " + q.getQuestionNumber() 
                            + " – marks too high (" + q.getMarks() + ")");
                        it.remove();
                        continue;
                    }
                    
                    // ⛔ enforce paperIdentifier rules
                    String paperId = q.getPaperIdentifier() != null ? q.getPaperIdentifier() : "";
                    switch (themeNum) {
                        case 1:
                            if (!"1".equals(paperId)) {
                                System.out.println("⚠ Removing " + q.getQuestionNumber() 
                                    + " – Theme 1 requires paper 1 but found " + paperId);
                                it.remove();
                            }
                            break;
                        case 2:
                            if (!"2".equals(paperId)) {
                                System.out.println("⚠ Removing " + q.getQuestionNumber() 
                                    + " – Theme 2 requires paper 2 but found " + paperId);
                                it.remove();
                            }
                            break;
                        case 3:
                            if (!( "1".equals(paperId) || "3".equals(paperId) )) {
                                System.out.println("⚠ Removing " + q.getQuestionNumber() 
                                    + " – Theme 3 requires paper 1 or 3 but found " + paperId);
                                it.remove();
                            }
                            break;
                        case 4:
                            if (!( "2".equals(paperId) || "3".equals(paperId) )) {
                                System.out.println("⚠ Removing " + q.getQuestionNumber() 
                                    + " – Theme 4 requires paper 2 or 3 but found " + paperId);
                                it.remove();
                            }
                            break;
                    }
                }
                
                if (topicQuestions.isEmpty()) {
                    System.out.println("No valid questions left in " + selectedTopic + " after filters");
                    continue;
                }
                
                topicQuestions = removeDuplicates(topicQuestions);
                List<Question> picked = selectExactlyTwo(topicQuestions);
                allSelected.addAll(picked);
                
                System.out.println("Section " + majorSection + " → picked " + picked.size() + " question(s)");
            }
        }
        
        allSelected = removeDuplicates(allSelected);
        if (allSelected.isEmpty()) {
            System.out.println("No valid questions selected for Theme " + themeNum + " Mock " + mockNum);
            return;
        }
        
        int totalMarks = allSelected.stream().mapToInt(Question::getMarks).sum();
        int totalTime = calculateTotalTime(allSelected);
        
        System.out.println("Final mock: " + allSelected.size() + " questions, "
                + totalMarks + " marks, " + totalTime + " minutes");
        
        File coverPage = coverPageCreator.createUnitMockCoverPage(
                mockNum, allSelected, totalMarks, TARGET_TIME, themeNum, mockDir);
        
        mockTestPdfCreator.createMockTestPdfs(allSelected, mockDir, coverPage);
        markschemeCreator.createMockTestMarkscheme(allSelected, mockDir);
    }
    
    // === Debug Printer ===
    private void printQuestionDebug(Question q) {
        System.out.println("  QuestionNumber: " + q.getQuestionNumber());
        System.out.println("  QuestionText: " + (q.getQuestionText() != null 
                ? q.getQuestionText().substring(0, Math.min(80, q.getQuestionText().length())) + "..." 
                : "null"));
        System.out.println("  QuestionFile: " + (q.getQuestion() != null ? q.getQuestion().getAbsolutePath() : "null"));
        System.out.println("  MarkSchemeFile: " + (q.getMarkScheme() != null ? q.getMarkScheme().getAbsolutePath() : "null"));
        System.out.println("  Year: " + q.getYear());
        System.out.println("  Board: " + q.getBoard());
        System.out.println("  Qualification: " + q.getQualification());
        System.out.println("  PaperIdentifier: " + q.getPaperIdentifier());
        System.out.println("  Marks: " + q.getMarks());
        System.out.println("  Topics: " + Arrays.toString(q.getTopics()));
        System.out.println("-------------------------------------------------");
    }
    
    // === Helpers ===
    
    private List<Question> selectExactlyTwo(List<Question> topicQs) {
        Collections.shuffle(topicQs, RANDOM);
        return topicQs.stream().limit(QUESTIONS_PER_SECTION).collect(Collectors.toList());
    }
    
    private List<String> getMajorSectionsForTheme(int themeNum) {
        switch (themeNum) {
            case 1: return Arrays.asList("1.1","1.2","1.3","1.4");
            case 2: return Arrays.asList("2.1","2.2","2.3","2.4","2.5","2.6");
            case 3: return Arrays.asList("3.1","3.2","3.3","3.4","3.5","3.6");
            case 4: return Arrays.asList("4.1","4.2","4.3","4.4","4.5");
            default: return Collections.emptyList();
        }
    }
    
    private List<String> getTopicsInSection(int themeNum, String majorSection, Set<String> allTopics) {
        String regex = "^" + themeNum + "\\.\\d+\\.\\d+\\s+.+";
        return allTopics.stream()
                        .filter(t -> t.startsWith(majorSection + "."))
                        .filter(t -> t.matches(regex))
                        .collect(Collectors.toList());
    }
    
    private int calculateTotalTime(List<Question> questions) {
        return questions.stream().mapToInt(this::calculateQuestionTime).sum();
    }
    
    private int calculateQuestionTime(Question q) {
        int marks = q.getMarks();
        if (QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber()) 
            || QuestionUtils.isContextBasedQuestion(q)) {
            return (int) Math.round(marks * 2.5);
        } else {
            return marks * 2;
        }
    }
    
    private List<Question> removeDuplicates(List<Question> questions) {
        Map<String, Question> unique = new LinkedHashMap<>();
        for (Question q : questions) {
            String id = QuestionUtils.getQuestionIdentifier(q);
            unique.putIfAbsent(id, q);
        }
        return new ArrayList<>(unique.values());
    }
}