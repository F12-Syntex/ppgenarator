package com.ppgenarator.core.topics;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class SingleMockGenerator {

    // Fixed exam lengths (in minutes)
    private static final int[] TARGET_TIMES = { 60, 90 };
    private static final int TIME_TOLERANCE = 15; // +/- tolerance for fitting

    private CoverPageCreator coverPageCreator;
    private MockTestPdfCreator mockTestPdfCreator;
    private MarkschemeCreator markschemeCreator;

    public SingleMockGenerator() {
        this.coverPageCreator = new CoverPageCreator();
        this.mockTestPdfCreator = new MockTestPdfCreator();
        this.markschemeCreator = new MarkschemeCreator();
    }

    /** Calculate estimated time for a question. */
    private int calculateQuestionTime(Question q) {
        int marks = q.getMarks();
        if (QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber())
                || QuestionUtils.isContextBasedQuestion(q)) {
            return (int) Math.round(marks * 2.5);
        } else {
            return marks * 1;
        }
    }

    private int calculateTotalTime(List<Question> qs) {
        return qs.stream().mapToInt(this::calculateQuestionTime).sum();
    }

    private int calculateTotalMarks(List<Question> qs) {
        return qs.stream().mapToInt(Question::getMarks).sum();
    }

    /**
     * Randomized selector for a target paper length, prioritising short non-essay
     * questions.
     */
    private List<Question> selectRandomForTarget(List<Question> pool, int targetTime) {
        if (pool.isEmpty())
            return Collections.emptyList();

        // Split the pool
        List<Question> shortQs = new ArrayList<>();
        List<Question> longQs = new ArrayList<>();
        for (Question q : pool) {
            boolean isEssay = QuestionUtils.isEssayStyleQuestion(q.getQuestionNumber())
                    || QuestionUtils.isContextBasedQuestion(q);
            if (!isEssay && q.getMarks() <= 10) {
                shortQs.add(q);
            } else {
                longQs.add(q);
            }
        }

        List<Question> best = new ArrayList<>();
        int closestDiff = Integer.MAX_VALUE;
        Random rnd = new Random();

        for (int attempt = 0; attempt < 50; attempt++) {
            // Shuffle each group
            Collections.shuffle(shortQs, rnd);
            Collections.shuffle(longQs, rnd);

            List<Question> selected = new ArrayList<>();
            int timeUsed = 0;

            // First try to fill with short questions
            for (Question q : shortQs) {
                int qt = calculateQuestionTime(q);
                if (timeUsed + qt <= targetTime + TIME_TOLERANCE) {
                    selected.add(q);
                    timeUsed += qt;
                }
                if (timeUsed >= targetTime - TIME_TOLERANCE)
                    break;
            }

            // Fill remaining with long ones if needed
            if (timeUsed < targetTime - TIME_TOLERANCE) {
                for (Question q : longQs) {
                    int qt = calculateQuestionTime(q);
                    if (timeUsed + qt <= targetTime + TIME_TOLERANCE) {
                        selected.add(q);
                        timeUsed += qt;
                    }
                    if (timeUsed >= targetTime - TIME_TOLERANCE)
                        break;
                }
            }

            int diff = Math.abs(timeUsed - targetTime);
            if (diff < closestDiff) {
                closestDiff = diff;
                best = selected;
            }

            if (closestDiff == 0)
                break; // perfect fit found
        }

        return best;
    }

    /** Main: create single mock. */
    public void createSingleMock(List<Question> questions, File topicDir, String qualification, String topic)
            throws IOException {

        List<Question> unique = removeDuplicates(questions);
        if (unique.isEmpty()) {
            System.out.println("No questions in " + topic);
            return;
        }

        //remove all 10+ markers
        unique.removeIf(q -> q.getMarks() > 10);

        int totalTime = calculateTotalTime(unique);
        System.out.println("Available " + topic + ": " + totalTime + " min total");

        // randomly choose whether this mock will be 60 or 90 minutes
        int chosenTarget = (Math.random() < 0.5) ? 60 : 90;

        List<Question> selected = selectRandomForTarget(unique, chosenTarget);
        if (selected.isEmpty()) {
            selected = unique;
            System.out.println("Fallback → using all");
        }

        createMockWithQuestions(selected, topicDir, qualification, topic, chosenTarget);
    }

    /** Write PDFs with randomized selection. */
    private void createMockWithQuestions(List<Question> qs, File topicDir, String qualification, String topic,
            int templateTime)
            throws IOException {

        int marks = calculateTotalMarks(qs);
        int time = calculateTotalTime(qs);

        System.out.println("Build mock: " + qs.size() + " qs, " +
                marks + " marks, " + time + " min (template " +
                templateTime + " min)");

        File cover = coverPageCreator.createCoverPage(
                1,
                qs,
                marks,
                templateTime, // Paper is labelled as exactly 60 or 90
                qualification,
                topic,
                topicDir);

        createMockPdf(qs, topicDir, cover);
        createMarkscheme(qs, topicDir);
    }

    private void createMockPdf(List<Question> qs, File topicDir, File coverPageFile) throws IOException {
        MockTestPdfCreator pdfCreator = new MockTestPdfCreator();
        File temp = new File(topicDir, "temp_mock");
        temp.mkdirs();

        pdfCreator.createMockTestPdfs(qs, temp, coverPageFile);

        File created = new File(temp, "mock.pdf");
        File finalMock = new File(topicDir, "mock.pdf");
        if (created.exists()) {
            if (finalMock.exists())
                finalMock.delete();
            created.renameTo(finalMock);
            System.out.println("Created mock.pdf: " + finalMock.getAbsolutePath());
        }

        for (File f : Objects.requireNonNull(temp.listFiles()))
            f.delete();
        temp.delete();
    }

    private void createMarkscheme(List<Question> qs, File topicDir) throws IOException {
        File ms = markschemeCreator.createMockTestMarkscheme(qs, topicDir);
        if (ms != null) {
            File finalMS = new File(topicDir, "markscheme.pdf");
            if (!ms.equals(finalMS)) {
                if (finalMS.exists())
                    finalMS.delete();
                ms.renameTo(finalMS);
            }
            System.out.println("Created markscheme.pdf: " + finalMS.getAbsolutePath());
        }
    }

    /** Deduplication. */
    private List<Question> removeDuplicates(List<Question> qs) {
        List<Question> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Question q : qs) {
            String id = QuestionUtils.getQuestionIdentifier(q);
            if (seen.add(id)) {
                unique.add(q);
            }
        }
        if (qs.size() != unique.size()) {
            System.out.println("Removed " + (qs.size() - unique.size()) + " duplicates");
        }
        return unique;
    }
}