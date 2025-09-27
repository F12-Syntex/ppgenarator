package com.ppgenarator.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NotesDownloader 3.0:
 * - Crawl PMT subject themes and organize files.
 * - Toggle to include parent header categories in folder structure.
 * - Deletes empty folders.
 */
public class NotesDownloader {

    // === CONFIG ===
    private static final boolean USE_HEADER_CATEGORIES = false; // toggle: false = flat (default), true = nested
    
    // Totals
    private static AtomicLong totalBytes = new AtomicLong(0);
    private static AtomicLong totalFiles = new AtomicLong(0);

    public static void main(String[] args) {
        String baseUrl = "https://www.physicsandmathstutor.com/economics-revision/a-level-edexcel-a/";
        String root = "downloads";

        processBase(baseUrl, root);

        System.out.println("\n📊 Final Summary");
        System.out.println("   Files downloaded: " + totalFiles.get());
        System.out.println("   Data used: " + humanReadable(totalBytes.get()));
    }

    /** Step 1: find all theme pages from a subject base page */
    private static void processBase(String baseUrl, String rootDir) {
        try {
            Document baseDoc = Jsoup.connect(baseUrl).get();
            Elements links = baseDoc.select("a[href]");

            String subjectFolder = clean(baseUrl.replace("https://www.physicsandmathstutor.com/", "").replace("/", "-"));
            File subjectDir = new File(rootDir, subjectFolder);
            subjectDir.mkdirs();
            System.out.println("📚 Subject folder: " + subjectDir.getAbsolutePath());

            for (Element link : links) {
                String subUrl = link.absUrl("href");
                if (subUrl.startsWith(baseUrl) && subUrl.contains("theme-")) {
                    String themeName = extractThemeName(subUrl);
                    File themeDir = new File(subjectDir, themeName);
                    themeDir.mkdirs();

                    System.out.println("\n🔗 Theme: " + themeName);
                    processTheme(subUrl, themeDir);

                    removeEmpty(themeDir);
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error reading base: " + e.getMessage());
        }
    }

    /** Step 2: process a Theme page and build folders according to headers */
    private static void processTheme(String themeUrl, File themeDir) {
        try {
            Document doc = Jsoup.connect(themeUrl).get();
            Elements headers = doc.select("h4, h5, strong"); // both big + small headers

            File currentParent = themeDir;

            for (Element header : headers) {
                String headerName = sanitizeName(header.text());
                if (headerName.isBlank()) continue;

                // If toggle is ON, update parent folder when we see a big section (h4)
                if (USE_HEADER_CATEGORIES && header.tagName().equals("h4")) {
                    currentParent = new File(themeDir, headerName);
                    currentParent.mkdirs();
                }

                // Subsections always get a folder, either nested or flat
                File sectionDir = USE_HEADER_CATEGORIES && header.tagName().equals("h5")
                        ? new File(currentParent, headerName)
                        : new File(themeDir, headerName);

                sectionDir.mkdirs();
                System.out.println("   📂 Section: " + sectionDir.getName());

                Element next = header.nextElementSibling();
                while (next != null && !next.tagName().matches("h4|h5|strong")) {
                    Elements links = next.select("a[href]");
                    for (Element a : links) {
                        String fileUrl = a.absUrl("href");
                        String linkText = sanitizeName(a.text());

                        if (fileUrl.matches(".*\\.(pdf|docx|pptx|xlsx)$")) {
                            String fileName = sanitizeFile(fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
                            File outFile = new File(sectionDir, fileName);
                            if (!outFile.exists()) {
                                System.out.println("      ⏬ " + fileName);
                                long bytes = download(fileUrl, outFile);
                                if (bytes > 0) {
                                    totalFiles.incrementAndGet();
                                    totalBytes.addAndGet(bytes);
                                    System.out.println("         ✅ " + humanReadable(bytes));
                                } else {
                                    System.out.println("         ❌ Failed");
                                }
                            } else {
                                System.out.println("      ✅ Exists: " + fileName);
                            }
                        } else if (fileUrl.startsWith("https://")) {
                            // Save as .url file
                            String fileName = sanitizeFile(linkText) + ".url";
                            File outFile = new File(sectionDir, fileName);
                            if (!outFile.exists()) {
                                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                    fos.write(("[InternetShortcut]\nURL=" + fileUrl).getBytes());
                                }
                                System.out.println("      🔗 Shortcut saved: " + fileName);
                            }
                        }
                    }
                    next = next.nextElementSibling();
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error reading theme: " + e.getMessage());
        }
    }

    /** Download one file */
    private static long download(String fileUrl, File outFile) {
        try {
            URI uri = new URI(fileUrl.replace(" ", "%20"));
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();

            try (InputStream in = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outFile)) {

                byte[] buf = new byte[8192];
                int read;
                long total = 0;
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                    total += read;
                }
                return total;
            }
        } catch (Exception e) {
            System.err.println("      ❌ Download err: " + fileUrl + " - " + e.getMessage());
            return 0;
        }
    }

    /** Recursively delete empty folders */
    private static void removeEmpty(File dir) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) removeEmpty(f);
        }
        files = dir.listFiles();
        if (files != null && files.length == 0) {
            if (dir.delete()) {
                System.out.println("   🗑️ Removed empty: " + dir.getAbsolutePath());
            }
        }
    }

    // === Helpers ===

    private static String humanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return new DecimalFormat("#.##").format(kb) + " KB";
        double mb = kb / 1024.0;
        if (mb < 1024) return new DecimalFormat("#.##").format(mb) + " MB";
        double gb = mb / 1024.0;
        return new DecimalFormat("#.##").format(gb) + " GB";
    }

    private static String sanitizeName(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\- .]", "").trim().replace(" ", "-");
    }

    private static String sanitizeFile(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String clean(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-]", "_");
    }

    private static String extractThemeName(String url) {
        Pattern p = Pattern.compile("theme-(\\d+)/?");
        Matcher m = p.matcher(url);
        if (m.find()) return "Theme-" + m.group(1);
        return url.replaceAll(".*/([^/]+)/?$", "$1");
    }
}