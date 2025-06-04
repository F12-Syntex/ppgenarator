package com.ppgenarator.core.topics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.ppgenarator.utils.FormattingUtils;
import com.ppgenarator.utils.QuestionUtils;
import com.ppgenerator.types.Question;

public class TopicSummaryReportCreator {

    private static final Color[] PIE_COLORS = {
            new Color(255, 99, 132), // Red
            new Color(54, 162, 235), // Blue
            new Color(255, 205, 86), // Yellow
            new Color(75, 192, 192), // Teal
            new Color(153, 102, 255), // Purple
            new Color(255, 159, 64), // Orange
            new Color(199, 199, 199), // Gray
            new Color(83, 102, 147), // Dark Blue
            new Color(255, 99, 255), // Pink
            new Color(124, 179, 66), // Green
            new Color(189, 195, 199), // Light Gray
            new Color(155, 89, 182), // Violet
            new Color(52, 152, 219), // Light Blue
            new Color(230, 126, 34), // Dark Orange
            new Color(46, 204, 113) // Light Green
    };

    public void createTopicSummaryReport(List<Question> allQuestions, File topicsDir, String qualification) {
        try {
            File reportFile = new File(topicsDir.getParentFile(), "topics_analysis_report.pdf");

            try (PDDocument document = new PDDocument()) {
                // Create cover page
                createCoverPage(document, qualification, allQuestions.size());

                // Create overview statistics page
                createOverviewPage(document, allQuestions, qualification);

                // Create topic distribution page
                createTopicDistributionPage(document, allQuestions);

                // Create year distribution page
                createYearDistributionPage(document, allQuestions);

                // Create exam board distribution page
                createExamBoardDistributionPage(document, allQuestions);

                // Create question types analysis page
                createQuestionTypesPage(document, allQuestions);

                // Create marks distribution page
                createMarksDistributionPage(document, allQuestions);

                // Create detailed topic breakdown pages
                createDetailedTopicPages(document, allQuestions);

                document.save(reportFile);
                System.out.println("Created topics analysis report: " + reportFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("Error creating topic summary report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createCoverPage(PDDocument document, String qualification, int totalQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;

            // Draw border
            contentStream.setLineWidth(3);
            contentStream.addRect(margin, margin, pageWidth - 2 * margin, pageHeight - 2 * margin);
            contentStream.stroke();

            // Title
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 36);
            String title = "TOPICS ANALYSIS REPORT";
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 36;
            contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, pageHeight - 150);
            contentStream.showText(title);
            contentStream.endText();

            // Subtitle
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 24);
            String subtitle = FormattingUtils.formatQualificationName(qualification);
            float subtitleWidth = PDType1Font.HELVETICA.getStringWidth(subtitle) / 1000 * 24;
            contentStream.newLineAtOffset((pageWidth - subtitleWidth) / 2, pageHeight - 200);
            contentStream.showText(subtitle);
            contentStream.endText();

            // Statistics box
            float boxWidth = 400;
            float boxHeight = 200;
            float boxX = (pageWidth - boxWidth) / 2;
            float boxY = pageHeight - 450;

            contentStream.setLineWidth(2);
            contentStream.addRect(boxX, boxY, boxWidth, boxHeight);
            contentStream.stroke();

            // Statistics content
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(boxX + 20, boxY + 150);
            contentStream.showText("Report Summary");
            contentStream.endText();

            String[] stats = {
                    "Total Questions: " + totalQuestions,
                    "Generated: " + java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "Analysis Type: Comprehensive Topic Breakdown"
            };

            float statsY = boxY + 110;
            for (String stat : stats) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.newLineAtOffset(boxX + 30, statsY);
                contentStream.showText(stat);
                contentStream.endText();
                statsY -= 25;
            }
        }
    }

    private void createOverviewPage(PDDocument document, List<Question> allQuestions, String qualification)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "OVERVIEW STATISTICS", pageWidth, yPos);
            yPos -= 40;

            // Calculate statistics
            Map<String, Integer> topicCounts = getTopicCounts(allQuestions);
            Map<String, Integer> yearCounts = getYearCounts(allQuestions);
            Map<String, Integer> boardCounts = getBoardCounts(allQuestions);

            int totalMarks = allQuestions.stream().mapToInt(Question::getMarks).sum();
            int q1to5Count = (int) allQuestions.stream().filter(q -> !QuestionUtils.isQuestion6(q.getQuestionNumber()))
                    .count();
            int q6Count = (int) allQuestions.stream().filter(q -> QuestionUtils.isQuestion6(q.getQuestionNumber()))
                    .count();

            // Create statistics boxes
            String[][] stats = {
                    { "Total Questions", String.valueOf(allQuestions.size()) },
                    { "Total Marks", String.valueOf(totalMarks) },
                    { "Q1-5 Questions", String.valueOf(q1to5Count) },
                    { "Q6 Questions", String.valueOf(q6Count) },
                    { "Topics Covered", String.valueOf(topicCounts.size()) },
                    { "Years Covered", String.valueOf(yearCounts.size()) },
                    { "Exam Boards", String.valueOf(boardCounts.size()) },
                    { "Avg Marks/Question", String.format("%.1f", (double) totalMarks / allQuestions.size()) }
            };

            yPos = createStatisticsGrid(contentStream, stats, margin, pageWidth, yPos);

            // Top 5 topics
            yPos -= 60;
            yPos = createTopTopicsSection(contentStream, topicCounts, margin, yPos);
        }
    }

    private void createTopicDistributionPage(PDDocument document, List<Question> allQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "TOPIC DISTRIBUTION", pageWidth, yPos);
            yPos -= 40;

            // Get topic data
            Map<String, Integer> topicCounts = getTopicCounts(allQuestions);

            // Create pie chart
            BufferedImage pieChart = createPieChart(topicCounts, "Questions by Topic", 400, 400);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    bufferedImageToByteArray(pieChart, "PNG"), "pie_chart");

            // Add pie chart to PDF
            float imageSize = 300;
            contentStream.drawImage(pdImage, (pageWidth - imageSize) / 2, yPos - imageSize, imageSize, imageSize);
            yPos -= imageSize + 40;

            // Add legend
            yPos = createLegend(contentStream, topicCounts, margin, pageWidth, yPos);
        }
    }

    private void createYearDistributionPage(PDDocument document, List<Question> allQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "YEAR DISTRIBUTION", pageWidth, yPos);
            yPos -= 40;

            // Get year data
            Map<String, Integer> yearCounts = getYearCounts(allQuestions);

            // Create bar chart for years
            BufferedImage barChart = createBarChart(yearCounts, "Questions by Year", 500, 300);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    bufferedImageToByteArray(barChart, "PNG"), "bar_chart");

            // Add chart to PDF
            float chartWidth = 450;
            float chartHeight = 270;
            contentStream.drawImage(pdImage, (pageWidth - chartWidth) / 2, yPos - chartHeight, chartWidth, chartHeight);
            yPos -= chartHeight + 40;

            // Add year statistics table
            yPos = createYearStatsTable(contentStream, yearCounts, margin, pageWidth, yPos);
        }
    }

    private void createExamBoardDistributionPage(PDDocument document, List<Question> allQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "EXAM BOARD DISTRIBUTION", pageWidth, yPos);
            yPos -= 40;

            // Get board data
            Map<String, Integer> boardCounts = getBoardCounts(allQuestions);

            // Create pie chart
            BufferedImage pieChart = createPieChart(boardCounts, "Questions by Exam Board", 350, 350);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    bufferedImageToByteArray(pieChart, "PNG"), "board_pie");

            // Add pie chart
            float imageSize = 280;
            contentStream.drawImage(pdImage, (pageWidth - imageSize) / 2, yPos - imageSize, imageSize, imageSize);
            yPos -= imageSize + 40;

            // Add board statistics
            yPos = createBoardStatsTable(contentStream, boardCounts, allQuestions, margin, pageWidth, yPos);
        }
    }

    private void createQuestionTypesPage(PDDocument document, List<Question> allQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "QUESTION TYPES ANALYSIS", pageWidth, yPos);
            yPos -= 40;

            // Analyze question types
            Map<String, Integer> questionTypes = new HashMap<>();
            for (Question q : allQuestions) {
                if (QuestionUtils.isQuestion6(q.getQuestionNumber())) {
                    questionTypes.put("Question 6 (Essays)", questionTypes.getOrDefault("Question 6 (Essays)", 0) + 1);
                } else {
                    questionTypes.put("Questions 1-5", questionTypes.getOrDefault("Questions 1-5", 0) + 1);
                }
            }

            // Create pie chart
            BufferedImage pieChart = createPieChart(questionTypes, "Question Types Distribution", 300, 300);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    bufferedImageToByteArray(pieChart, "PNG"), "types_pie");

            float imageSize = 250;
            contentStream.drawImage(pdImage, (pageWidth - imageSize) / 2, yPos - imageSize, imageSize, imageSize);
            yPos -= imageSize + 60;

            // Add detailed question number analysis
            yPos = createQuestionNumberAnalysis(contentStream, allQuestions, margin, pageWidth, yPos);
        }
    }

    private void createMarksDistributionPage(PDDocument document, List<Question> allQuestions) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "MARKS DISTRIBUTION", pageWidth, yPos);
            yPos -= 40;

            // Analyze marks distribution
            Map<String, Integer> marksDistribution = new HashMap<>();
            for (Question q : allQuestions) {
                String marksRange = getMarksRange(q.getMarks());
                marksDistribution.put(marksRange, marksDistribution.getOrDefault(marksRange, 0) + 1);
            }

            // Create bar chart
            BufferedImage barChart = createBarChart(marksDistribution, "Questions by Marks Range", 450, 250);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                    bufferedImageToByteArray(barChart, "PNG"), "marks_bar");

            float chartWidth = 400;
            float chartHeight = 200;
            contentStream.drawImage(pdImage, (pageWidth - chartWidth) / 2, yPos - chartHeight, chartWidth, chartHeight);
            yPos -= chartHeight + 40;

            // Add marks statistics
            yPos = createMarksStatsTable(contentStream, allQuestions, margin, pageWidth, yPos);
        }
    }

    private void createDetailedTopicPages(PDDocument document, List<Question> allQuestions) throws IOException {
        Map<String, List<Question>> questionsByTopic = groupQuestionsByTopic(allQuestions);

        for (Map.Entry<String, List<Question>> entry : questionsByTopic.entrySet()) {
            if (entry.getValue().size() >= 5) { // Only create detailed pages for topics with 5+ questions
                createDetailedTopicPage(document, entry.getKey(), entry.getValue());
            }
        }
    }

    private void createDetailedTopicPage(PDDocument document, String topic, List<Question> questions)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            float yPos = pageHeight - margin;

            // Page title
            yPos = addPageTitle(contentStream, "TOPIC: " + FormattingUtils.formatTopicName(topic).toUpperCase(),
                    pageWidth, yPos);
            yPos -= 40;

            // Topic statistics
            int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
            Map<String, Integer> yearCounts = questions.stream().collect(
                    Collectors.groupingBy(Question::getYear,
                            Collectors.summingInt(q -> 1)));

            String[][] topicStats = {
                    { "Total Questions", String.valueOf(questions.size()) },
                    { "Total Marks", String.valueOf(totalMarks) },
                    { "Years Covered", String.valueOf(yearCounts.size()) },
                    { "Avg Marks/Question", String.format("%.1f", (double) totalMarks / questions.size()) }
            };

            yPos = createStatisticsGrid(contentStream, topicStats, margin, pageWidth, yPos);
            yPos -= 40;

            // Year distribution for this topic
            if (yearCounts.size() > 1) {
                BufferedImage yearChart = createBarChart(yearCounts, "Questions by Year - " + topic, 400, 200);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document,
                        bufferedImageToByteArray(yearChart, "PNG"), "topic_year_chart");

                float chartWidth = 350;
                float chartHeight = 175;
                contentStream.drawImage(pdImage, (pageWidth - chartWidth) / 2, yPos - chartHeight, chartWidth,
                        chartHeight);
                yPos -= chartHeight + 30;
            }

            // Questions list
            yPos = createTopicQuestionsList(contentStream, questions, margin, pageWidth, yPos);
        }
    }

    // Utility methods for creating charts and tables

    private BufferedImage createPieChart(Map<String, Integer> data, String title, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Calculate pie chart
        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0)
            return image;

        int centerX = width / 2;
        int centerY = height / 2 + 20;
        int radius = Math.min(width, height) / 3;

        double startAngle = 0;
        int colorIndex = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double percentage = (double) entry.getValue() / total;
            double arcAngle = percentage * 360;

            g2d.setColor(PIE_COLORS[colorIndex % PIE_COLORS.length]);
            g2d.fill(new Arc2D.Double(centerX - radius, centerY - radius,
                    radius * 2, radius * 2, startAngle, arcAngle, Arc2D.PIE));

            startAngle += arcAngle;
            colorIndex++;
        }

        // Add title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 25);

        g2d.dispose();
        return image;
    }

    private BufferedImage createBarChart(Map<String, Integer> data, String title, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        if (data.isEmpty())
            return image;

        // Chart area
        int marginX = 60;
        int marginY = 40;
        int chartWidth = width - 2 * marginX;
        int chartHeight = height - 2 * marginY;

        // Find max value
        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        // Draw bars
        int barWidth = chartWidth / data.size() - 10;
        int x = marginX + 5;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int barHeight = (int) ((double) entry.getValue() / maxValue * chartHeight);

            g2d.setColor(PIE_COLORS[0]);
            g2d.fillRect(x, height - marginY - barHeight, barWidth, barHeight);

            // Draw value on top of bar
            g2d.setColor(Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
            String value = entry.getValue().toString();
            int valueWidth = g2d.getFontMetrics().stringWidth(value);
            g2d.drawString(value, x + (barWidth - valueWidth) / 2, height - marginY - barHeight - 5);

            // Draw label below bar
            String label = entry.getKey();
            if (label.length() > 8)
                label = label.substring(0, 8) + "...";
            int labelWidth = g2d.getFontMetrics().stringWidth(label);
            g2d.drawString(label, x + (barWidth - labelWidth) / 2, height - marginY + 15);

            x += barWidth + 10;
        }

        // Add title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 20);

        g2d.dispose();
        return image;
    }

    // Helper methods for data processing and PDF creation

    private Map<String, Integer> getTopicCounts(List<Question> questions) {
        Map<String, Integer> topicCounts = new HashMap<>();
        for (Question q : questions) {
            if (q.getTopics() != null) {
                for (String topic : q.getTopics()) {
                    topicCounts.put(topic, topicCounts.getOrDefault(topic, 0) + 1);
                }
            }
        }
        return topicCounts;
    }

    private Map<String, Integer> getYearCounts(List<Question> questions) {
        return questions.stream().collect(
                Collectors.groupingBy(Question::getYear,
                        Collectors.summingInt(q -> 1)));
    }

    private Map<String, Integer> getBoardCounts(List<Question> questions) {
        return questions.stream().collect(
                Collectors.groupingBy(q -> q.getBoard().toString(),
                        Collectors.summingInt(q -> 1)));
    }

    private Map<String, List<Question>> groupQuestionsByTopic(List<Question> questions) {
        Map<String, List<Question>> topicGroups = new HashMap<>();
        for (Question q : questions) {
            if (q.getTopics() != null) {
                for (String topic : q.getTopics()) {
                    topicGroups.computeIfAbsent(topic, k -> new ArrayList<>()).add(q);
                }
            }
        }
        return topicGroups;
    }

    private String getMarksRange(int marks) {
        if (marks <= 2)
            return "1-2 marks";
        if (marks <= 4)
            return "3-4 marks";
        if (marks <= 6)
            return "5-6 marks";
        if (marks <= 10)
            return "7-10 marks";
        return "11+ marks";
    }

    private byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    private float addPageTitle(PDPageContentStream contentStream, String title, float pageWidth, float yPos)
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 20;
        contentStream.newLineAtOffset((pageWidth - titleWidth) / 2, yPos);
        contentStream.showText(title);
        contentStream.endText();

        // Underline
        contentStream.setLineWidth(1);
        contentStream.moveTo((pageWidth - titleWidth) / 2, yPos - 5);
        contentStream.lineTo((pageWidth + titleWidth) / 2, yPos - 5);
        contentStream.stroke();

        return yPos - 30;
    }

    private float createStatisticsGrid(PDPageContentStream contentStream, String[][] stats,
            float margin, float pageWidth, float yPos) throws IOException {
        int cols = 4;
        int rows = (stats.length + cols - 1) / cols;
        float boxWidth = (pageWidth - 2 * margin - 30) / cols;
        float boxHeight = 60;

        for (int i = 0; i < stats.length; i++) {
            int row = i / cols;
            int col = i % cols;

            float x = margin + col * (boxWidth + 10);
            float y = yPos - row * (boxHeight + 10);

            // Draw box
            contentStream.setLineWidth(1);
            contentStream.addRect(x, y - boxHeight, boxWidth, boxHeight);
            contentStream.stroke();

            // Add label
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(x + 5, y - 20);
            contentStream.showText(stats[i][0]);
            contentStream.endText();

            // Add value
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            float valueWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(stats[i][1]) / 1000 * 16;
            contentStream.newLineAtOffset(x + (boxWidth - valueWidth) / 2, y - 45);
            contentStream.showText(stats[i][1]);
            contentStream.endText();
        }

        return yPos - rows * (boxHeight + 10);
    }

    private float createLegend(PDPageContentStream contentStream, Map<String, Integer> data,
            float margin, float pageWidth, float yPos) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.newLineAtOffset(margin, yPos);
        contentStream.showText("Legend:");
        contentStream.endText();

        yPos -= 20;
        int colorIndex = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            // Use asterisk instead of square symbol
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(margin, yPos);
            contentStream.showText("* " + FormattingUtils.formatTopicName(entry.getKey()) +
                    " (" + entry.getValue() + " questions)");
            contentStream.endText();

            yPos -= 18;
            colorIndex++;

            if (yPos < 100)
                break; // Prevent overflow
        }

        return yPos;
    }

    private float createTopTopicsSection(PDPageContentStream contentStream, Map<String, Integer> topicCounts,
            float margin, float yPos) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.newLineAtOffset(margin, yPos);
        contentStream.showText("Top 5 Topics by Question Count:");
        contentStream.endText();

        yPos -= 25;

        // Sort topics by count and take top 5
        List<Map.Entry<String, Integer>> sortedTopics = topicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        for (int i = 0; i < sortedTopics.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTopics.get(i);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(margin + 20, yPos);
            contentStream.showText((i + 1) + ". " + FormattingUtils.formatTopicName(entry.getKey()) +
                    " - " + entry.getValue() + " questions");
            contentStream.endText();
            yPos -= 18;
        }

        return yPos;
    }

    private float createYearStatsTable(PDPageContentStream contentStream, Map<String, Integer> yearCounts,
            float margin, float pageWidth, float yPos) throws IOException {
        return createSimpleTable(contentStream, "Years", yearCounts, margin, pageWidth, yPos);
    }

    private float createBoardStatsTable(PDPageContentStream contentStream, Map<String, Integer> boardCounts,
            List<Question> allQuestions, float margin, float pageWidth, float yPos) throws IOException {
        return createSimpleTable(contentStream, "Exam Boards", boardCounts, margin, pageWidth, yPos);
    }

    private float createMarksStatsTable(PDPageContentStream contentStream, List<Question> allQuestions,
            float margin, float pageWidth, float yPos) throws IOException {
        int totalMarks = allQuestions.stream().mapToInt(Question::getMarks).sum();
        int minMarks = allQuestions.stream().mapToInt(Question::getMarks).min().orElse(0);
        int maxMarks = allQuestions.stream().mapToInt(Question::getMarks).max().orElse(0);
        double avgMarks = (double) totalMarks / allQuestions.size();

        String[][] marksStats = {
                { "Total Marks", String.valueOf(totalMarks) },
                { "Average Marks", String.format("%.1f", avgMarks) },
                { "Minimum Marks", String.valueOf(minMarks) },
                { "Maximum Marks", String.valueOf(maxMarks) }
        };

        return createStatisticsGrid(contentStream, marksStats, margin, pageWidth, yPos);
    }

    private float createQuestionNumberAnalysis(PDPageContentStream contentStream, List<Question> allQuestions,
            float margin, float pageWidth, float yPos) throws IOException {
        Map<String, Integer> questionNumbers = new HashMap<>();
        for (Question q : allQuestions) {
            String qNum = q.getQuestionNumber();
            if (qNum.startsWith("6")) {
                questionNumbers.put("Q6", questionNumbers.getOrDefault("Q6", 0) + 1);
            } else {
                questionNumbers.put("Q" + qNum.charAt(0), questionNumbers.getOrDefault("Q" + qNum.charAt(0), 0) + 1);
            }
        }

        return createSimpleTable(contentStream, "Question Numbers", questionNumbers, margin, pageWidth, yPos);
    }

    private float createTopicQuestionsList(PDPageContentStream contentStream, List<Question> questions,
            float margin, float pageWidth, float yPos) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(margin, yPos);
        contentStream.showText("Questions in this topic:");
        contentStream.endText();

        yPos -= 20;

        for (int i = 0; i < Math.min(questions.size(), 15); i++) { // Limit to 15 questions
            Question q = questions.get(i);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(margin + 10, yPos);
            contentStream
                    .showText(q.getYear() + " Q" + FormattingUtils.formatOriginalQuestionNumber(q.getQuestionNumber()) +
                            " (" + q.getMarks() + " marks) - " + q.getBoard());
            contentStream.endText();
            yPos -= 15;

            if (yPos < 100)
                break; // Prevent overflow
        }

        if (questions.size() > 15) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            contentStream.newLineAtOffset(margin + 10, yPos);
            contentStream.showText("... and " + (questions.size() - 15) + " more questions");
            contentStream.endText();
        }

        return yPos;
    }

    private float createSimpleTable(PDPageContentStream contentStream, String title, Map<String, Integer> data,
            float margin, float pageWidth, float yPos) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(margin, yPos);
        contentStream.showText(title + ":");
        contentStream.endText();

        yPos -= 20;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(margin + 20, yPos);
            contentStream.showText(entry.getKey() + ": " + entry.getValue() + " questions");
            contentStream.endText();
            yPos -= 15;

            if (yPos < 100)
                break;
        }

        return yPos;
    }
}