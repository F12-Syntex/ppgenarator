package com.ppgenarator.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.ppgenarator.ai.OpenAI;
import com.ppgenerator.types.FileInfo;
import com.ppgenerator.types.ProcessingResult;
import com.ppgenerator.types.DocumentType;
import com.ppgenerator.types.ExamBoard;
import com.ppgenerator.types.Qualification;

public class DirectoryFormatter {
    private final File folder;
    private final OpenAI openAI;
    private final File uncategorizedFolder;
    private final List<ProcessingResult> results = new ArrayList<>();
    private final List<FileInfo> processedFiles = new ArrayList<>();
    private int filesProcessed = 0;
    private int filesSkipped = 0;
    private int filesMoved = 0;
    private int filesRenamed = 0;
    
    // Regular expression for correctly formatted filenames
    private static final Pattern CORRECT_FORMAT_PATTERN = Pattern.compile(
            "^[a-z0-9]+_[a-z0-9]+_[a-z0-9]+_\\d{4}(_paper\\d+)?_(?:questionpaper|markscheme|examinerreport)\\.[a-z0-9]+$",
            Pattern.CASE_INSENSITIVE
    );
    
    // Common topics (example list - you can expand this)
    private static final Set<String> COMMON_TOPICS = new HashSet<>(Arrays.asList(
            "businessgrowth", "finance", "marketing", "operations", "hrm", "economics", 
            "management", "accounting", "strategy", "entrepreneurship", "mathematics",
            "physics", "chemistry", "biology", "business"
    ));
    
    // Patterns to identify document types in filenames
    private static final Pattern QUESTION_PAPER_PATTERN = Pattern.compile(
            "(?:question|paper|qp|questions|\\bq\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARK_SCHEME_PATTERN = Pattern.compile(
            "(?:mark|scheme|ms|solution|answers|\\bms\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXAMINER_REPORT_PATTERN = Pattern.compile(
            "(?:examiner|report|er|examreport)", Pattern.CASE_INSENSITIVE);
    
    // Paper number patterns
    private static final Pattern PAPER_NUMBER_PATTERN = Pattern.compile(
            "(?:paper|p)[\\s_-]*(\\d+)|paper[\\s_-]*([a-z0-9]+)", 
            Pattern.CASE_INSENSITIVE);
    
    // Pattern to identify years in filenames (19xx or 20xx)
    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    public DirectoryFormatter(File folder) {
        this.folder = folder;
        this.openAI = new OpenAI();
        this.uncategorizedFolder = new File(folder, "uncategorized");
    }

    public FileInfo[] formatDirectory() {
        if (!folder.exists()) {
            throw new RuntimeException("Directory does not exist: " + folder.getAbsolutePath());
        }

        System.out.println("Starting directory formatting: " + folder.getAbsolutePath());
        long startTime = System.currentTimeMillis();
        
        ensureUncategorizedFolderExists();
        File[] files = getFilesToProcess();
        
        if (files == null || files.length == 0) {
            System.out.println("No files found in directory: " + folder.getAbsolutePath());
            return new FileInfo[0];
        }

        System.out.println("Found " + files.length + " files to process");
        
        // Process each file
        for (File file : files) {
            processFileWithErrorHandling(file);
        }
        
        printSummary(startTime);
        return processedFiles.toArray(new FileInfo[0]);
    }
    
    private void ensureUncategorizedFolderExists() {
        if (!uncategorizedFolder.exists()) {
            boolean created = uncategorizedFolder.mkdir();
            if (!created) {
                throw new RuntimeException("Failed to create uncategorized folder: " + uncategorizedFolder.getAbsolutePath());
            }
        }
    }
    
    private File[] getFilesToProcess() {
        return folder.listFiles(file -> 
            file.isFile() && 
            !file.getName().startsWith(".") && 
            !file.equals(uncategorizedFolder)
        );
    }
    
    private void processFileWithErrorHandling(File file) {
        try {
            processFile(file);
        } catch (Exception e) {
            System.err.println("Error processing file: " + file.getName());
            e.printStackTrace();
            moveToUncategorized(file);
        }
    }
    
    private void processFile(File file) throws IOException, JSONException {
        filesProcessed++;
        
        // Skip if this is already the uncategorized folder
        if (file.getParentFile().equals(uncategorizedFolder)) {
            return;
        }
        
        String originalName = file.getName();
        System.out.println("Processing: " + originalName);
        
        // Check if file is already properly named
        if (isCorrectlyFormatted(originalName)) {
            handleAlreadyFormattedFile(file, originalName);
            return;
        }
        
        // Try to extract information from filename first
        FileInfo fileInfo = extractInfoFromFilename(originalName);
        
        // If we couldn't get all the necessary information, use AI
        if (!fileInfo.isComplete()) {
            fileInfo = enhanceWithAI(file, fileInfo);
        }

        fileInfo.setFile(file);
        
        // Save the FileInfo to our result list
        processedFiles.add(fileInfo);
        
        // If we still don't have enough information, move to uncategorized
        if (!fileInfo.isComplete()) {
            handleIncompleteCategorization(file, originalName);
            return;
        }
        
        renameFile(file, fileInfo, originalName);
    }
    
    private void handleAlreadyFormattedFile(File file, String originalName) {
        System.out.println("File already correctly formatted, skipping: " + originalName);
        filesSkipped++;
        results.add(new ProcessingResult(originalName, originalName, "skipped", "Already correctly formatted"));
        
        // Even for skipped files, we add them to the processed list
        FileInfo fileInfo = extractInfoFromFilename(originalName);
        processedFiles.add(fileInfo);
    }
    
    private void handleIncompleteCategorization(File file, String originalName) {
        System.out.println("Could not categorize: " + originalName);
        results.add(new ProcessingResult(originalName, null, "moved", "Missing required information"));
        moveToUncategorized(file);
    }
    
    private void renameFile(File file, FileInfo fileInfo, String originalName) {
        // Format new filename
        String newName = formatNewFilename(fileInfo);
        
        // Rename the file
        File newFile = new File(folder, newName);
        
        // Make sure we don't overwrite existing files
        if (newFile.exists() && !newFile.equals(file)) {
            newFile = createUniqueFileName(newName);
        }
        
        // Skip if the new filename is the same as the old one
        if (newFile.equals(file)) {
            handleSameNameFile(originalName);
            return;
        }
        
        boolean success = file.renameTo(newFile);
        if (success) {
            System.out.println("Renamed: " + originalName + " -> " + newFile.getName());
            filesRenamed++;
            results.add(new ProcessingResult(originalName, newFile.getName(), "renamed", "Successfully renamed"));
        } else {
            System.err.println("Failed to rename: " + originalName);
            results.add(new ProcessingResult(originalName, null, "error", "Failed to rename file"));
            moveToUncategorized(file);
        }
    }
    
    private File createUniqueFileName(String newName) {
        String baseName = newName.substring(0, newName.lastIndexOf('.'));
        String extension = newName.substring(newName.lastIndexOf('.'));
        String uniqueName = baseName + "_" + System.currentTimeMillis() + extension;
        return new File(folder, uniqueName);
    }
    
    private void handleSameNameFile(String originalName) {
        System.out.println("New filename same as old, skipping: " + originalName);
        filesSkipped++;
        results.add(new ProcessingResult(originalName, originalName, "skipped", "New filename same as old"));
    }
    
    private boolean isCorrectlyFormatted(String filename) {
        // Check if the filename follows our required format
        return CORRECT_FORMAT_PATTERN.matcher(filename).matches();
    }
    
    private FileInfo extractInfoFromFilename(String filename) {
        FileInfo info = new FileInfo();
        String filenameNoExt = filename;
        
        // Extract extension
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            info.setExtension(filename.substring(lastDotIndex));
            filenameNoExt = filename.substring(0, lastDotIndex);
        }
        
        // Enhanced paper number extraction - do this first
        Matcher paperMatcher = PAPER_NUMBER_PATTERN.matcher(filename);
        if (paperMatcher.find()) {
            String paperNum = paperMatcher.group(1);
            if (paperNum == null) {
                paperNum = paperMatcher.group(2);
            }
            if (paperNum != null) {
                info.setPaper(Integer.parseInt(paperNum));
            }
        }
        
        // Document type identification
        if (filename.toLowerCase().contains(" ms ") || filename.toLowerCase().contains("markscheme") || 
            filename.toLowerCase().contains("mark scheme")) {
            info.setDocumentType(DocumentType.MARK_SCHEME);
        } 
        else if (filename.toLowerCase().contains(" qp ") || filename.toLowerCase().contains("questionpaper") || 
                 filename.toLowerCase().contains("question paper")) {
            info.setDocumentType(DocumentType.QUESTION_PAPER);
        }
        else {
            Matcher msMatcher = MARK_SCHEME_PATTERN.matcher(filename);
            Matcher qpMatcher = QUESTION_PAPER_PATTERN.matcher(filename);
            Matcher erMatcher = EXAMINER_REPORT_PATTERN.matcher(filename);
            
            if (msMatcher.find()) {
                info.setDocumentType(DocumentType.MARK_SCHEME);
            } else if (qpMatcher.find()) {
                info.setDocumentType(DocumentType.QUESTION_PAPER);
            } else if (erMatcher.find()) {
                info.setDocumentType(DocumentType.EXAMINER_REPORT);
            }
        }
        
        // Convert to lowercase and replace non-alphanumeric chars with underscores
        String normalized = filenameNoExt.toLowerCase().replaceAll("[^a-z0-9]", "_");
        String[] parts = normalized.split("_");
        
        // Try to extract year (4 digits starting with 19 or 20)
        Matcher yearMatcher = YEAR_PATTERN.matcher(filename);
        if (yearMatcher.find()) {
            info.setYear(Integer.parseInt(yearMatcher.group()));
        }
        
        // Try to identify exam board
        for (String part : parts) {
            try {
                ExamBoard examBoard = ExamBoard.fromString(part);
                if (examBoard != null) {
                    info.setExamBoard(examBoard);
                    break;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a recognized exam board, continue checking
            }
        }
        
        // Try to identify qualification
        for (String part : parts) {
            try {
                Qualification qualification = Qualification.fromString(part);
                if (qualification != null) {
                    info.setQualification(qualification);
                    break;
                }
            } catch (IllegalArgumentException ignored) {
                // Not a recognized qualification, continue checking
            }
        }
        
        // Try to identify topic
        for (String part : parts) {
            if (COMMON_TOPICS.contains(part)) {
                info.setTopic(part);
                break;
            }
        }
        
        // Look for common subjects
        if (info.getTopic() == null) {
            if (filename.toLowerCase().contains("econ")) {
                info.setTopic("economics");
            } else if (filename.toLowerCase().contains("business")) {
                info.setTopic("business");
            } else if (filename.toLowerCase().contains("math") || filename.toLowerCase().contains("maths")) {
                info.setTopic("mathematics");
            } else if (filename.toLowerCase().contains("physics")) {
                info.setTopic("physics");
            } else if (filename.toLowerCase().contains("chemistry")) {
                info.setTopic("chemistry");
            } else if (filename.toLowerCase().contains("biology")) {
                info.setTopic("biology");
            }
        }
        
        // Special case: check for AS vs A-level
        if (filename.toLowerCase().contains("as-level") || filename.toLowerCase().contains("as level")) {
            info.setQualification(Qualification.AS);
        } else if (filename.toLowerCase().contains("a-level") || filename.toLowerCase().contains("a level")) {
            info.setQualification(Qualification.A_LEVEL);
        }
        
        return info;
    }
    
    private FileInfo enhanceWithAI(File file, FileInfo partialInfo) throws IOException, JSONException {
        // Get file content or metadata for AI analysis
        String fileContent = extractFileContent(file);
        
        String prompt = String.format(
                "Analyze this file information and extract the following details in JSON format:\n\n" +
                "Filename: %s\n" +
                "File Content Preview: %s\n\n" +
                "Please extract and return ONLY a JSON object with these fields:\n" +
                "- topic: The main subject area of the document (e.g., economics, business, mathematics)\n" +
                "- qualification: The qualification level (one of: gcse, alevel, as, btec, ib, igcse, diploma)\n" +
                "- examBoard: The examining body (one of: aqa, edexcel, ocr, wjec, cambridge, ib, pearson, eduqas)\n" +
                "- year: The year of the exam as an integer (e.g., 2019)\n" +
                "- paper: The paper number as an integer (e.g., 1, 2, 3)\n" +
                "- documentType: The type of document (one of: questionpaper, markscheme, examinerreport)\n\n" +
                "IMPORTANT: If you see 'MS' it means Mark Scheme, if you see 'QP' it means Question Paper.\n" +
                "If you see 'Paper X', where X is a number, please include this as the paper number in your response.\n" +
                "If you cannot determine a field with confidence, use null for that field.\n" +
                "Return ONLY the JSON object without explanation.\n",
                file.getName(),
                fileContent
        );
        
        String response = openAI.query(prompt);
        
        try {
            // Try to parse the JSON response
            JSONObject jsonResponse = extractJsonFromResponse(response);
            
            // Update the partial info with AI-provided details
            if (partialInfo.getTopic() == null && jsonResponse.has("topic") && !jsonResponse.isNull("topic")) {
                partialInfo.setTopic(jsonResponse.getString("topic").toLowerCase().replaceAll("[^a-z0-9]", ""));
            }
            
            if (partialInfo.getQualification() == null && jsonResponse.has("qualification") && !jsonResponse.isNull("qualification")) {
                try {
                    String qualStr = jsonResponse.getString("qualification").toLowerCase();
                    Qualification qual = Qualification.fromString(qualStr);
                    partialInfo.setQualification(qual);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown qualification: " + jsonResponse.getString("qualification"));
                }
            }
            
            if (partialInfo.getExamBoard() == null && jsonResponse.has("examBoard") && !jsonResponse.isNull("examBoard")) {
                try {
                    String boardStr = jsonResponse.getString("examBoard").toLowerCase();
                    ExamBoard board = ExamBoard.fromString(boardStr);
                    partialInfo.setExamBoard(board);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown exam board: " + jsonResponse.getString("examBoard"));
                }
            }
            
            if (partialInfo.getYear() == 0 && jsonResponse.has("year") && !jsonResponse.isNull("year")) {
                try {
                    int year = jsonResponse.getInt("year");
                    partialInfo.setYear(year);
                } catch (Exception e) {
                    try {
                        // Try parsing as string
                        String yearStr = jsonResponse.getString("year");
                        partialInfo.setYear(Integer.parseInt(yearStr));
                    } catch (Exception ignored) {
                        System.err.println("Could not parse year: " + jsonResponse.get("year"));
                    }
                }
            }
            
            if (partialInfo.getPaper() == 0 && jsonResponse.has("paper") && !jsonResponse.isNull("paper")) {
                try {
                    int paper = jsonResponse.getInt("paper");
                    partialInfo.setPaper(paper);
                } catch (Exception e) {
                    try {
                        // Try parsing as string
                        String paperStr = jsonResponse.getString("paper").replaceAll("[^0-9]", "");
                        if (!paperStr.isEmpty()) {
                            partialInfo.setPaper(Integer.parseInt(paperStr));
                        }
                    } catch (Exception ignored) {
                        System.err.println("Could not parse paper: " + jsonResponse.get("paper"));
                    }
                }
            }
            
            if (partialInfo.getDocumentType() == null && jsonResponse.has("documentType") && !jsonResponse.isNull("documentType")) {
                String docType = jsonResponse.getString("documentType").toLowerCase();
                try {
                    DocumentType type = DocumentType.fromString(docType);
                    partialInfo.setDocumentType(type);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown document type: " + docType);
                }
            }
            
        } catch (JSONException e) {
            System.err.println("Failed to parse AI response as JSON: " + e.getMessage());
            System.err.println("AI response: " + response);
        }
        
        return partialInfo;
    }
    
    private JSONObject extractJsonFromResponse(String response) throws JSONException {
        // Try to extract JSON if it's surrounded by other text
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {
            String jsonStr = response.substring(startIndex, endIndex + 1);
            return new JSONObject(jsonStr);
        } else {
            return new JSONObject(response);
        }
    }
    
    private String extractFileContent(File file) {
        // Extract a preview of file content for AI analysis
        try {
            String extension = "";
            if (file.getName().lastIndexOf('.') > 0) {
                extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
            }
            
            // For text-based files, read the content directly
            if (Arrays.asList("txt", "csv", "md", "json", "xml", "html").contains(extension)) {
                Path path = Paths.get(file.getAbsolutePath());
                byte[] bytes = Files.readAllBytes(path);
                String content = new String(bytes);
                // Return a truncated version to save tokens
                return content.length() > 1000 ? content.substring(0, 1000) + "..." : content;
            }
            
            // For other file types
            return "File type " + extension + " - content extraction not implemented";
            
        } catch (IOException e) {
            return "Error extracting content: " + e.getMessage();
        }
    }
    
    private String formatNewFilename(FileInfo info) {
        StringBuilder sb = new StringBuilder();
        
        // Default values if information is missing
        String topic = info.getTopic() != null ? info.getTopic() : "unknown";
        String qualification = info.getQualification() != null ? info.getQualification().getCode() : "unknown";
        String examBoard = info.getExamBoard() != null ? info.getExamBoard().getCode() : "unknown";
        int year = info.getYear() != 0 ? info.getYear() : 0;
        String documentType = info.getDocumentType() != null ? info.getDocumentType().getCode() : "unknown";
        
        sb.append(topic).append("_")
          .append(qualification).append("_")
          .append(examBoard).append("_")
          .append(year);
        
        // Always include paper number if available
        if (info.getPaper() > 0) {
            sb.append("_paper").append(info.getPaper());
        }
        
        sb.append("_").append(documentType)
          .append(info.getExtension());
        
        return sb.toString();
    }
    
    private void moveToUncategorized(File file) {
        File destination = new File(uncategorizedFolder, file.getName());
        
        // Handle duplicate filenames
        if (destination.exists()) {
            String baseName = file.getName();
            int lastDotIndex = baseName.lastIndexOf('.');
            String nameWithoutExt = lastDotIndex > 0 ? baseName.substring(0, lastDotIndex) : baseName;
            String extension = lastDotIndex > 0 ? baseName.substring(lastDotIndex) : "";
            
            destination = new File(uncategorizedFolder, nameWithoutExt + "_" + System.currentTimeMillis() + extension);
        }
        
        boolean success = file.renameTo(destination);
        if (success) {
            System.out.println("Moved to uncategorized: " + file.getName());
            filesMoved++;
        } else {
            System.err.println("Failed to move to uncategorized: " + file.getName());
        }
    }
    
    private void printSummary(long startTime) {
        long endTime = System.currentTimeMillis();
        double processTime = (endTime - startTime) / 1000.0;
        
        System.out.println("\nDirectory formatting complete:");
        System.out.println("  Total files processed: " + filesProcessed);
        System.out.println("  Files skipped (already correct): " + filesSkipped);
        System.out.println("  Files renamed: " + filesRenamed);
        System.out.println("  Files moved to uncategorized: " + filesMoved);
        System.out.println("  Processing time: " + processTime + " seconds");
    }
}