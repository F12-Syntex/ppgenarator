package com.ppgenarator.core.topics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ppgenerator.types.ExamBoard;
import com.ppgenerator.types.Qualification;
import com.ppgenerator.types.Question;

public class QuestionLoader {

    public List<Question> loadQuestionsFromJsonFiles(File metadataDir) throws JSONException, IOException {
        List<Question> allQuestions = new ArrayList<>();

        File[] jsonFiles = metadataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in directory: " + metadataDir.getAbsolutePath());
            return allQuestions;
        }

        for (File jsonFile : jsonFiles) {
            allQuestions.addAll(loadQuestionsFromFile(jsonFile));
        }

        return allQuestions;
    }

    private List<Question> loadQuestionsFromFile(File jsonFile) throws JSONException, IOException {
        List<Question> questions = new ArrayList<>();

        try (FileReader reader = new FileReader(jsonFile)) {
            StringBuilder content = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
            }

            JSONArray jsonArray = new JSONArray(new JSONTokener(content.toString()));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonQuestion = jsonArray.getJSONObject(i);
                Question question = parseQuestionFromJson(jsonQuestion);
                questions.add(question);
            }
        }

        return questions;
    }

    private Question parseQuestionFromJson(JSONObject jsonQuestion) throws JSONException {
        Question question = new Question();

        question.setQualification(
                Qualification.fromString(jsonQuestion.optString("qualification", "UNKNOWN")));

        question.setQuestionNumber(jsonQuestion.getString("questionNumber"));
        question.setYear(jsonQuestion.getString("year"));

        try {
            question.setBoard(ExamBoard.valueOf(jsonQuestion.optString("board", "UNKNOWN").toUpperCase()));
        } catch (IllegalArgumentException e) {
            question.setBoard(ExamBoard.UNKNOWN);
        }

        question.setQuestionText(jsonQuestion.optString("questionText", ""));

        // Set file references
        if (jsonQuestion.has("questionFile")) {
            File questionFile = new File(jsonQuestion.getString("questionFile"));
            if (questionFile.exists()) {
                question.setQuestion(questionFile);
            }

            question.setPaperIdentifier(questionFile.getParentFile().getName());
        }

        if (jsonQuestion.has("markSchemeFile")) {
            File markSchemeFile = new File(jsonQuestion.getString("markSchemeFile"));
            if (markSchemeFile.exists()) {
                question.setMarkScheme(markSchemeFile);
            }
        }

        // Set topics
        if (jsonQuestion.has("topics")) {
            JSONArray topicsArray = jsonQuestion.getJSONArray("topics");
            String[] topics = new String[topicsArray.length()];
            for (int j = 0; j < topicsArray.length(); j++) {
                topics[j] = topicsArray.getString(j);
            }
            question.setTopics(topics);
        }

        return question;
    }
}