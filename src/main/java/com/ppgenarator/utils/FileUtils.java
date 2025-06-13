package com.ppgenarator.utils;

import java.io.File;
import java.nio.file.Files;

public class FileUtils {
    private static File[] concatenateArrays(File[] firstArray, File[] secondArray) {
        File[] result = new File[firstArray.length + secondArray.length];
        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }

    public static File[] getAllFilesWithExtension(String directoryPath, String extension,
            boolean includeSubdirectories) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            System.out.println("The provided path is not a directory: " + directoryPath);
            return new File[0];
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith("." + extension.toLowerCase()));
        if (files == null) {
            System.out.println("No files found in the directory: " + directoryPath);
            return new File[0];
        }

        if (includeSubdirectories) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] subdirectoryFiles = getAllFilesWithExtension(file.getAbsolutePath(), extension, true);
                    files = concatenateArrays(files, subdirectoryFiles);
                }
            }
        }

        return files;
    }

    public static String sanitizeFileName(String input) {
        // For specification topics, preserve the structure but make it file-system safe
        if (input.matches("\\d+\\.\\d+.*")) {
            // This is a specification topic, preserve the format but make it safe
            return input.replaceAll("[<>:\"/\\\\|?*]", "").trim();
        }
        // For other inputs, use the old method
        return input.replaceAll("[^a-zA-Z0-9_\\s\\.]", "_").toLowerCase();
    }

    public static String getFileMd5Hash(File file) {
        try {
            return org.apache.commons.codec.digest.DigestUtils.md5Hex(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            System.err.println("Error calculating MD5 hash: " + e.getMessage());
            return file.getName();
        }
    }
}