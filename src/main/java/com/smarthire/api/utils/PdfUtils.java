package com.smarthire.api.utils;

import org.apache.pdfbox.Loader; // <--- C'est l'import important qui change tout !
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utilitaire pour gérer les fichiers PDF.
 * Version corrigée pour PDFBox 3.0.x
 */
public class PdfUtils {

    private static final Logger logger = LoggerFactory.getLogger(PdfUtils.class);

    /**
     * Extrait le texte brut d'un fichier PDF sous forme de tableau d'octets.
     *
     * @param pdfData Les données binaires du fichier PDF.
     * @return Le texte extrait, ou une chaîne vide en cas d'erreur.
     */
    public static String extractTextFromPdf(byte[] pdfData) {
        if (pdfData == null || pdfData.length == 0) {
            logger.warn("Tentative d'extraction de texte sur des données PDF vides.");
            return "";
        }

        // CORRECTION ICI : On utilise Loader.loadPDF(byte[]) directement
        // Plus besoin de ByteArrayInputStream
        try (PDDocument document = Loader.loadPDF(pdfData)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);

            if (text == null) {
                return "";
            }

            return text.trim();

        } catch (IOException e) {
            logger.error("Erreur lors de l'extraction du texte du PDF : {}", e.getMessage());
            return "";
        }
    }
}