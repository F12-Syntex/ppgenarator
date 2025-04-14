package com.ppgenarator.ai;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAI {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final String apiKey;
    private final OkHttpClient httpClient;
    private String model;
    private double temperature;
    
    /**
     * Creates a new OpenAI client with default settings
     */
    public OpenAI() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.model = "gpt-4o-mini";
        this.temperature = 0.3;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Creates a new OpenAI client with custom settings
     * 
     * @param model The OpenAI model to use
     * @param temperature The temperature setting (0-1)
     */
    public OpenAI(String model, double temperature) {
        this();
        this.model = model;
        this.temperature = temperature;
    }
    
    /**
     * Set the model to use for queries
     * 
     * @param model The OpenAI model name
     * @return This OpenAI instance for method chaining
     */
    public OpenAI setModel(String model) {
        this.model = model;
        return this;
    }
    
    /**
     * Set the temperature for queries
     * 
     * @param temperature The temperature value (0-1)
     * @return This OpenAI instance for method chaining
     */
    public OpenAI setTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }
    
    /**
     * Send a query to the OpenAI API and get the response
     * 
     * @param prompt The prompt to send
     * @return The response text from the AI
     * @throws IOException If there's an error with the API request
     * @throws JSONException If there's an error parsing the JSON response
     */
    public String query(String prompt) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messagesArray.put(messageObject);
        
        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", temperature);
        
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error calling OpenAI API: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            // Extract and log token usage
            if (jsonResponse.has("usage")) {
                JSONObject usage = jsonResponse.getJSONObject("usage");
                int promptTokens = usage.getInt("prompt_tokens");
                int completionTokens = usage.getInt("completion_tokens");
                int totalTokens = usage.getInt("total_tokens");
                
                System.out.println("Token usage:");
                System.out.println("  Prompt tokens: " + promptTokens);
                System.out.println("  Completion tokens: " + completionTokens);
                System.out.println("  Total tokens: " + totalTokens);
            }
            
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        }
    }
    
    /**
     * Attempt to parse a response that should contain a JSON array
     * 
     * @param response The response from the OpenAI API
     * @return A JSONArray parsed from the response or null if parsing fails
     */
    public static JSONArray parseJsonArrayResponse(String response) {
        try {
            String contentTrimmed = response.trim();
            int startIndex = contentTrimmed.indexOf('[');
            int endIndex = contentTrimmed.lastIndexOf(']');
            
            if (startIndex != -1 && endIndex != -1) {
                String jsonArrayString = contentTrimmed.substring(startIndex, endIndex + 1);
                return new JSONArray(jsonArrayString);
            }
        } catch (JSONException e) {
            System.err.println("Failed to parse response as JSON array: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Utility method to convert a JSONArray to a String array
     * 
     * @param jsonArray The JSONArray to convert
     * @return A String array containing the elements of the JSONArray
     * @throws JSONException If there's an error accessing elements in the JSONArray
     */
    public static String[] jsonArrayToStringArray(JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            return new String[0];
        }
        
        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.getString(i);
        }
        return result;
    }
}