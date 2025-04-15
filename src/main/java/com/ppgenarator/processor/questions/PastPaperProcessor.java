package com.ppgenarator.processor.questions;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

import com.ppgenerator.types.FileInfo;

public class PastPaperProcessor {

    private FileInfo pastpaper;

    public PastPaperProcessor(FileInfo pastpaper) {
        this.pastpaper = pastpaper;
    }

    public void process() {
        if (!pastpaper.getExtension().equals(".pdf")) {
            System.out.println("Not a pdf file: " + pastpaper.getFile().getAbsolutePath());
            return;
        }

        try {
            PDDocument document = PDDocument.load(pastpaper.getFile());
            if (document.isEncrypted()) {
                System.out.println("Document is encrypted: " + pastpaper.getFile().getAbsolutePath());
                return;
            }

            File sectionAFile = new File(pastpaper.getOutputFolder(), "sectionA.pdf");
            File sectionBFile = new File(pastpaper.getOutputFolder(), "sectionB.pdf");
            File sectionCFile = new File(pastpaper.getOutputFolder(), "sectionC.pdf");

            if (!(sectionAFile.exists() && sectionBFile.exists() && sectionCFile.exists())) {
                this.processDocument(document);
            }

            SectionAProcessor sectionAProcessor = new SectionAProcessor(sectionAFile, pastpaper.getOutputFolder());
            SectionBProcessor sectionBProcessor = new SectionBProcessor(sectionBFile, pastpaper.getOutputFolder());

            sectionAProcessor.process();
            sectionBProcessor.process();

        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void processDocument(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);

        // Find section markers
        int sectionBStart = findSectionStart(text, "SECTION B");
        int sectionCStart = findSectionStart(text, "SECTION C");

        // Create output directory
        String outputDir = pastpaper.getOutputFolder().getAbsolutePath();

        // Create Section A
        PDDocument sectionA = new PDDocument();
        int startPage = 2;
        int endPage = (sectionBStart != -1) ? getPageForPosition(document, text, sectionBStart) - 1
                : document.getNumberOfPages();
        copyPages(document, sectionA, startPage - 1, endPage - 1);
        sectionA.save(new File(outputDir, "sectionA.pdf"));
        sectionA.close();

        // Create Section B if it exists
        if (sectionBStart != -1) {
            PDDocument sectionB = new PDDocument();
            startPage = getPageForPosition(document, text, sectionBStart);
            endPage = (sectionCStart != -1) ? getPageForPosition(document, text, sectionCStart) - 1
                    : document.getNumberOfPages();
            copyPages(document, sectionB, startPage - 1, endPage - 1);
            sectionB.save(new File(outputDir, "sectionB.pdf"));
            sectionB.close();

            // Create Section C if it exists
            if (sectionCStart != -1) {
                PDDocument sectionC = new PDDocument();
                startPage = getPageForPosition(document, text, sectionCStart);
                endPage = document.getNumberOfPages();
                copyPages(document, sectionC, startPage - 1, endPage - 1);
                sectionC.save(new File(outputDir, "sectionC.pdf"));
                sectionC.close();
            }
        }

        document.close();
    }

    private int findSectionStart(String text, String sectionMarker) {
        int index = text.indexOf(sectionMarker);
        return index;
    }

    private int getPageForPosition(PDDocument document, String fullText, int position) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        String textSoFar = "";
        int currentPage = 1;

        while (currentPage <= document.getNumberOfPages()) {
            stripper.setStartPage(1);
            stripper.setEndPage(currentPage);
            textSoFar = stripper.getText(document);
            if (textSoFar.length() > position) {
                return currentPage;
            }
            currentPage++;
        }

        return document.getNumberOfPages();
    }

    private void copyPages(PDDocument sourceDoc, PDDocument targetDoc, int startPage, int endPage) throws IOException {
        for (int i = startPage; i <= endPage; i++) {
            PDPage page = sourceDoc.getPage(i);
            targetDoc.importPage(page);
        }
    }
}