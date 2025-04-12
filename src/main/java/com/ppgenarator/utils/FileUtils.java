package com.ppgenarator.utils;

import java.io.File;

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

}
