package com.ppgenarator.ai;

import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class OpenAiService {

    private final OpenAIClient client;
    private ChatModel model;
    private double temperature;

    /**
     * Creates a new OpenAI client with default settings
     */
    public OpenAiService() {
        this.client = OpenAIOkHttpClient.fromEnv();
        this.model = ChatModel.GPT_4_1_MINI;
        this.temperature = 0.3;
    }

    /**
     * Creates a new OpenAI client with custom settings
     * 
     * @param model       The OpenAI model to use
     * @param temperature The temperature setting (0-1)
     */
    public OpenAiService(ChatModel model, double temperature) {
        this();
        this.model = model;
        this.temperature = temperature;
    }

    /**
     * Creates a new OpenAI client with a custom client
     * 
     * @param client The pre-configured OpenAI client
     */
    public OpenAiService(OpenAIClient client) {
        this.client = client;
        this.model = ChatModel.GPT_4_1_MINI;
        this.temperature = 0.3;
    }

    /**
     * Set the model to use for queries
     * 
     * @param model The OpenAI model
     * @return This OpenAI instance for method chaining
     */
    public OpenAiService setModel(ChatModel model) {
        this.model = model;
        return this;
    }

    /**
     * Set the temperature for queries
     * 
     * @param temperature The temperature value (0-1)
     * @return This OpenAI instance for method chaining
     */
    public OpenAiService setTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Send a query to the OpenAI API and get the response
     * 
     * @param prompt The prompt to send
     * @return The response text from the AI
     * @throws RuntimeException If there's an error with the API request
     */
    public String query(String prompt) {
        try {
            // Create parameters for the ChatCompletion request
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model(model)
                    .temperature(temperature)
                    .build();

            // Execute the API call
            ChatCompletion completion = client.chat().completions().create(params);

            // Extract and log token usage
            if (completion.usage() != null) {
                System.out.println("Token usage:");
                System.out.println("  Prompt tokens: " + completion.usage().get().promptTokens());
                System.out.println("  Completion tokens: " + completion.usage().get().completionTokens());
                System.out.println("  Total tokens: " + completion.usage().get().totalTokens());
            }

            // Extract the response content
            Optional<String> responseContent = completion.choices().get(0).message().content();

            if (!responseContent.isPresent()) {
                throw new RuntimeException("Empty response from OpenAI API");
            }

            return responseContent.get();
        } catch (Exception e) {
            throw new RuntimeException("Error while querying OpenAI API", e);
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