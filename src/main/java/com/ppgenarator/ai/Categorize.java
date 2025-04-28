package com.ppgenarator.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ppgenerator.types.Question;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Categorize {

    private File yearDir;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4.1-mini";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String apiKey;
    private final OkHttpClient httpClient;

    String[] topics1 = {
            // "Scarcity",
            // "Resource allocation and economic systems",
            // "Production Possibilities Curve",
            // "Comparative advantage and gains from trade",
            // "Cost-benefit analysis",
            // "Marginal analysis and consumer choice",
            // "Demand and the law of demand",
            // "Supply and the law of supply",
            // "Market equilibrium and disequilibrium",
            // "Changes in equilibrium",
            // "Consumer and producer surplus",
            // "Market interventions",
            // "International trade",
            // "Consumer theory",
            // "Production decisions and economic profit",
            // "Forms of competition",
            // "Factor markets",
            // "Market failure and the role of government",
            // "Economic indicators and the business cycle",
            // "National income and price determination",
            // "Financial sector",
            // "Long-run consequences of stabilization policies",
            // "Open economy: international trade and finance",
            // "Keynesian approaches and IS-LM",
            // "Contemporary macroeconomic issues",
            // "Statistical inference",
            // "Regression",
            // "Generalized least squares",
            // "Instrumental variables",
            // "Simultaneous equations models",
            // "Evaluation of government policies and programs",
            // "Output/GDP (Gross Domestic Product)",
            // "National income",
            // "Unemployment",
            // "Price indices and inflation",
            // "Consumption",
            // "Saving",
            // "Investment",
            // "Energy",
            // "Substitute goods",
            // "Complements",
            // "Economies of scale",
            // "Price elasticity of demand",
            // "Cross elasticity of demand",
            // "Income elasticity of demand",
            // "Price elasticity of supply",
            // "Positive and normative statements",
            // "Opportunity cost",
            // "Specialisation and division of labour",
            // "Positive externalities",
            // "Negative externalities",
            // "Merit goods",
            // "Demerit goods",
            // "Public goods",
            // "Information failure",
            // "Government failure",
            // "Government intervention",
            // "Carbon Tax",
            // "Buffer stocks",
            // "Subsidy",
            // "Carbon Trading",
            // "Nudges",
            // "Specific tax",
            // "Behavioural economics",
            // "Objectives of firms",
            // "Costs",
            // "Diminishing returns",
            // "Allocative Efficiency",
            // "Productive Efficiency",
            // "Dynamic Efficiency",
            // "Pareto efficiency",
            // "Labour markets",
            // "Labour Market Imperfections",
            // "Monopsony",
            // "Monopoly",
            // "Monopolistic competition",
            // "Oligopoly",
            // "Perfect competition",
            // "Competition policy",
            // "Contestable markets",
            // "Price discrimination",
            // "Pricing strategies",
            // "Privatisation",
            // "Isoquant and isocosts",
            // "Adverse selection",
            // "Asymmetric information problem",
            // "Expected Utility Theory",
            // "Marginal utility theory",
            // "Indifference curves and budget lines",
            // "Definition of financial assets: money, stocks, bonds",
            // "Time value of money (present and future value)",
            // "Measures of money supply",
            // "Banks and creation of money",
            // "Money demand",
            // "Money market and the equilibrium nominal interest rate",
            // "Tools of central bank policy",
            // "Quantity theory of money",
            // "Real versus nominal interest rates",
            // "Demand-side effects",
            // "Supply-side effects",
            // "Policy mix",
            // "Government deficits and debt",
            // "Supply of and demand for loanable funds",
            // "Equilibrium real interest rate",
            // "Crowding out",
            // "The Phillips Curve",
            // "Money growth and inflation",
            // "Economic growth",
            // "Balance of payments accounts",
            // "Exchange rates and the foreign exchange market",
            // "Effects of changes in policies and economic conditions on the foreign exchange market",
            // "Changes in the foreign exchange market and net exports",
            // "Real interest rates and international capital flows",
            // "Maths"
    };


    String[] topics = {
        "other", "business growth"
    };

    public Categorize(File yearDir) {
        this.yearDir = yearDir;
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void process() throws JSONException {
        // Check if output file already exists
        File outputFolder = new File(yearDir.getParentFile(), "metadata");
        File outputFile = new File(outputFolder, yearDir.getName() + ".json");

        if (outputFile.exists()) {
            System.out.println("JSON file already exists for " + yearDir.getName() + ". Skipping processing.");
            return;
        }

        List<Question> questions = new ArrayList<>();

        // Find all pdfs
        File[] files = yearDir.listFiles((dir, name) -> name.endsWith(".pdf"));

        for (File file : files) {
            if (!file.getName().contains("question")) {
                continue;
            }

            String name = file.getName().split("question")[1].split("\\.")[0];

            Question question = new Question();
            question.setQuestionNumber(name);
            question.setQuestion(file);
            question.setYear(yearDir.getName());
            question.setBoard("edexcel");

            questions.add(question);
        }

        File markschemeFolder = new File(yearDir, "markscheme");
        for (File file : markschemeFolder.listFiles()) {
            if (!file.getName().contains("question")) {
                continue;
            }

            String name = file.getName().split("question")[1].split("\\.")[0];

            for (Question question : questions) {
                if (question.getQuestionNumber().equals(name)) {
                    question.setMarkScheme(file);
                }
            }
        }

        questions.forEach(this::loadQuestionContent);
        questions.forEach(System.out::println);

        // Create output folder if it doesn't exist
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        try {
            // Create a JSON array to hold all question objects
            JSONArray jsonArray = new JSONArray();

            for (Question question : questions) {
                JSONObject jsonQuestion = new JSONObject();
                jsonQuestion.put("questionNumber", question.getQuestionNumber());
                jsonQuestion.put("year", question.getYear());
                jsonQuestion.put("board", question.getBoard());
                jsonQuestion.put("questionText", question.getQuestionText());

                // Add topics as a JSON array
                if (question.getTopics() != null) {
                    JSONArray topicsArray = new JSONArray();
                    for (String topic : question.getTopics()) {
                        topicsArray.put(topic);
                    }
                    jsonQuestion.put("topics", topicsArray);
                }

                // Add file paths
                if (question.getQuestion() != null) {
                    jsonQuestion.put("questionFile", question.getQuestion().getAbsolutePath());
                }
                if (question.getMarkScheme() != null) {
                    jsonQuestion.put("markSchemeFile", question.getMarkScheme().getAbsolutePath());
                }

                jsonArray.put(jsonQuestion);
            }

            // Write the JSON to the output file
            java.nio.file.Files.write(
                    outputFile.toPath(),
                    jsonArray.toString(2).getBytes(), // Pretty print with indentation of 2
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("Successfully exported questions to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error writing questions to JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadQuestionContent(Question question) {
        System.out.println("Loading question content for: " + question.getQuestionNumber());
        // Read the question PDF file and extract the text
        if (question.getQuestion() != null) {
            String questionContent = extractTextFromPDF(question.getQuestion());
            
            // Enhanced cleaning of the question text
            questionContent = cleanQuestionText(questionContent);
            question.setQuestionText(questionContent);

            // Use OpenAI to identify topics for the question
            String[] topics = identifyTopics(questionContent, question.getBoard());
            question.setTopics(topics);
        }
    }

    private String cleanQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Remove page numbers/identifiers
        text = text.replaceAll("\\*P\\d+A\\d+\\*", "");
        
        // Remove "DO NOT WRITE IN THIS AREA" and variations
        text = text.replaceAll("D\\s*O\\s*N\\s*O\\s*T\\s*W\\s*R\\s*I\\s*T\\s*E\\s*I\\s*N\\s*T\\s*H\\s*I\\s*S\\s*A\\s*R\\s*E\\s*A", "");
        
        // Remove repeated dots (common in exam papers for fill-in spaces)
        text = text.replaceAll("\\.{2,}", " ");
        
        // Replace sequences of dots and spaces with a single space
        text = text.replaceAll("\\. \\.", " ");
        text = text.replaceAll("  \\.", " ");
        
        // Remove repeated "PMT" markings
        text = text.replaceAll("\\bPMT\\b", "");
        
        // Remove unicode placeholder characters
        text = text.replaceAll("\\?+", "");
        
        // Remove page numbers and headers/footers (often appear as isolated numbers or short phrases)
        text = text.replaceAll("(?m)^\\d+$", "");
        
        // Remove exam-specific instructions and references
        text = text.replaceAll("(?i)\\bP\\d+\\b", ""); // Remove page references like P1, P2
        text = text.replaceAll("(?i)turn over", "");
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?i)continue on the next page", "");
        
        // Remove question numbering and marks information
        text = text.replaceAll("\\(Total for Question \\d+:? \\d+ marks?\\)", "");
        text = text.replaceAll("\\(Total for Question \\d+ = \\d+ marks?\\)", "");
        text = text.replaceAll("TOTAL FOR SECTION [A-Z] = \\d+ MARKS", "");
        text = text.replaceAll("\\(\\d+ marks?\\)", "");
        
        // Remove excessive whitespace including new lines
        text = text.replaceAll("\\s+", " ");
        
        // Remove any leading/trailing whitespace
        text = text.trim();
        
        return text;
    }

    private String[] identifyTopics(String questionText, String board) {
        try {
            String prompt = String.format(
                    "Analyze this %s mathematics question and list the main topics it covers. " +
                            "Provide your answer as a JSON array of strings with only the topic names: \n\n%s" +
                            "here are the list of topics %s YOU MAY ONLY USE THE TOPICS IN THE LIST, AND ONLY USE ONE\n",
                    board, questionText, String.join(", ", topics));
    
            OpenAI openAI = new OpenAI(OPENAI_MODEL, 0.3);
            String response = openAI.query(prompt);
            
            // Parse the response
            JSONArray topicsArray = OpenAI.parseJsonArrayResponse(response);
            if (topicsArray != null) {
                return OpenAI.jsonArrayToStringArray(topicsArray);
            } else {
                // Fallback to simple text parsing if JSON parsing fails
                String[] topics = response.split("[\n,]");
                List<String> cleanedTopics = new ArrayList<>();
    
                for (String topic : topics) {
                    String cleaned = topic.trim()
                            .replaceAll("^[0-9]+\\.\\s*", "") // Remove numbering
                            .replaceAll("[\"\\[\\]]", ""); // Remove quotes and brackets
    
                    if (!cleaned.isEmpty() && !cleaned.equalsIgnoreCase("topics:")) {
                        cleanedTopics.add(cleaned);
                    }
                }
    
                return cleanedTopics.toArray(new String[0]);
            }
        } catch (Exception e) {
            System.err.println("Error identifying topics with OpenAI: " + e.getMessage());
            e.printStackTrace();
            return new String[] { "Error" };
        }
    }

    /**
     * Extract text content from a PDF file
     * 
     * @param pdfFile The PDF file to extract text from
     * @return The extracted text
     */
    private String extractTextFromPDF(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF file: " + pdfFile.getName());
            e.printStackTrace();
            return "";
        }
    }
}