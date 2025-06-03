package com.ppgenarator.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PastPaperDownloader {

    private final File outputFolder;
    private final int maxDepth;
    private final int maxThreads;
    private final Set<String> visitedUrls;
    private final Set<String> downloadedFiles;
    private final List<String> errors;
    private final boolean followLinksOnSameDomain;
    private final String filenamePrefix;

    public PastPaperDownloader(File outputFolder) {
        this(outputFolder, 2, 5, true, "economics_");
    }

    public PastPaperDownloader(File outputFolder, int maxDepth, int maxThreads, boolean followLinksOnSameDomain) {
        this(outputFolder, maxDepth, maxThreads, followLinksOnSameDomain, "");
    }

    public PastPaperDownloader(File outputFolder, int maxDepth, int maxThreads, boolean followLinksOnSameDomain,
            String filenamePrefix) {
        this.outputFolder = outputFolder;
        this.maxDepth = maxDepth;
        this.maxThreads = maxThreads;
        this.followLinksOnSameDomain = followLinksOnSameDomain;
        this.filenamePrefix = filenamePrefix != null ? filenamePrefix : "";
        this.visitedUrls = new HashSet<>();
        this.downloadedFiles = new HashSet<>();
        this.errors = new ArrayList<>();

        // Create output folder if it doesn't exist
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
    }

    /**
     * Download PDFs from a URL and its linked pages
     */
    public void downloadPastPapers(String url) {

        if (url == null || url.isEmpty()) {
            System.err.println("Invalid URL provided.");
            return;
        }

        url = url.trim().replace(" ", "%20");
        downloadPastPapers(url, filenamePrefix);
    }

    /**
     * Download PDFs from a URL and its linked pages with a specific prefix
     */
    public void downloadPastPapers(String url, String prefix) {
        System.out.println("Starting download from URL: " + url);
        System.out.println("Output folder: " + outputFolder.getAbsolutePath());
        if (!prefix.isEmpty()) {
            System.out.println("Using filename prefix: " + prefix);
        }

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

        try {
            // Start crawling from the given URL with depth 0
            crawlForPDFs(url, 0, executor, prefix);

            // Shutdown executor and wait for all tasks to complete
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                System.err.println("Executor did not terminate in the specified time.");
                executor.shutdownNow();
            }

            System.out.println("\nDownload complete!");
            System.out.println("Downloaded " + downloadedFiles.size() + " files");

            if (!errors.isEmpty()) {
                System.out.println("\nErrors encountered (" + errors.size() + "):");
                for (String error : errors) {
                    System.out.println("- " + error);
                }
            }

        } catch (Exception e) {
            System.err.println("Error downloading past papers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Recursively crawl webpages for PDF links
     */
    private void crawlForPDFs(String url, int depth, ExecutorService executor, String prefix) {
        // Skip if we've already visited this URL or exceeded max depth
        if (visitedUrls.contains(url) || depth > maxDepth) {
            return;
        }

        visitedUrls.add(url);

        try {
            System.out.println("Scanning: " + url + " (depth: " + depth + ")");

            // Connect to URL and get HTML
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // Find all PDF links
            Elements pdfLinks = doc.select("a[href$=.pdf]");
            for (Element link : pdfLinks) {
                String pdfUrl = link.absUrl("href");
                if (!downloadedFiles.contains(pdfUrl)) {
                    // Submit download task to executor with the specified prefix
                    executor.submit(() -> downloadPDF(pdfUrl, prefix));
                }
            }

            // Only follow links on the same domain if configured to do so
            if (depth+1 < maxDepth && followLinksOnSameDomain) {
                String baseUrl = new URL(url).getHost();
                Elements pageLinks = doc.select("a[href]");

                for (Element link : pageLinks) {
                    String nextUrl = link.absUrl("href");

                    // Only follow links to the same domain
                    if (!nextUrl.isEmpty() && nextUrl.contains(baseUrl) && !visitedUrls.contains(nextUrl)) {
                        crawlForPDFs(nextUrl, depth + 1, executor, prefix);
                    }
                }
            }

        } catch (IOException e) {
            synchronized (errors) {
                errors.add("Failed to process URL: " + url + " - " + e.getMessage());
            }
        }
    }

    /**
     * Download a PDF file from a URL
     */
    private void downloadPDF(String pdfUrl, String prefix) {
        try {
            
            String baseFilename = extractFilename(pdfUrl);

            // Skip if empty filename or already downloaded
            if (baseFilename.isEmpty() || downloadedFiles.contains(pdfUrl)) {
                return;
            }

            // Add prefix to filename
            String filename = prefix + baseFilename;

            // Make filename unique by adding URL hash if file already exists
            File destination = new File(outputFolder, filename);
            if (destination.exists()) {
                // Add a hash of the URL to make the filename unique
                String fileWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                String extension = filename.substring(filename.lastIndexOf('.'));
                String uniqueId = String.valueOf(Math.abs(pdfUrl.hashCode() % 1000));
                filename = fileWithoutExt + "_" + uniqueId + extension;
                destination = new File(outputFolder, filename);
            }

            // Download the file
            downloadFile(pdfUrl, destination);

            synchronized (downloadedFiles) {
                downloadedFiles.add(pdfUrl);
            }

        } catch (Exception e) {
            synchronized (errors) {
                errors.add("Failed to download PDF: " + pdfUrl + " - " + e.getMessage());
            }
        }
    }

    /**
     * Download a file from a URL to a local file
     */
    private void downloadFile(String fileUrl, File destination) throws IOException {
        System.out.println("Downloading: " + fileUrl + " -> " + destination.getName());

        fileUrl = fileUrl.trim().replace(" ", "%20");
        
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // Check if the response is successful
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }

        // Download the file
        try (InputStream in = connection.getInputStream();
                ReadableByteChannel readableByteChannel = Channels.newChannel(in);
                FileOutputStream fileOutputStream = new FileOutputStream(destination);
                FileChannel fileChannel = fileOutputStream.getChannel()) {

            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }

        System.out.println("Downloaded: " + destination.getName());
    }

    /**
     * Extract filename from URL
     */
    private String extractFilename(String url) {
        try {
            String path = new URL(url).getPath();
            String decoded = java.net.URLDecoder.decode(path, "UTF-8");
            int lastSlashPos = decoded.lastIndexOf('/');

            if (lastSlashPos >= 0 && lastSlashPos < decoded.length() - 1) {
                return decoded.substring(lastSlashPos + 1);
            }

            // If no proper filename found, create one based on URL hash
            return "download_" + Math.abs(url.hashCode()) + ".pdf";

        } catch (Exception e) {
            // If there's any problem, create a filename based on URL hash
            return "download_" + Math.abs(url.hashCode()) + ".pdf";
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: PastPaperDownloader <URL> <output-folder> [max-depth] [max-threads] [prefix]");
            return;
        }

        String url = args[0];
        File outputFolder = new File(args[1]);

        int maxDepth = 2; // Default depth
        int maxThreads = 5; // Default thread count
        String prefix = ""; // Default prefix (empty)

        if (args.length >= 3) {
            maxDepth = Integer.parseInt(args[2]);
        }

        if (args.length >= 4) {
            maxThreads = Integer.parseInt(args[3]);
        }

        if (args.length >= 5) {
            prefix = args[4];
        }

        PastPaperDownloader downloader = new PastPaperDownloader(outputFolder, maxDepth, maxThreads, true, prefix);
        downloader.downloadPastPapers(url);
    }
}