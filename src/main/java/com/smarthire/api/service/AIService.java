package com.smarthire.api.service;

import com.smarthire.api.dto.ApplicationResponse;
import com.smarthire.api.dto.UpdateCvScoreRequest;
import com.smarthire.api.dto.UpdateInternalNotesRequest;
import com.smarthire.api.model.Application;
import com.smarthire.api.model.JobOffer;
import com.smarthire.api.utils.PdfUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final ApplicationService applicationService;
    private final ChatLanguageModel chatLanguageModel; // Injecté automatiquement par LangChain4j

    /**
     * Analyse toutes les candidatures pour une offre donnée de manière asynchrone.
     */
    @Async
    public void analyzeAllApplications(Long offerId, String userEmail) {
        logger.info("Démarrage de l'analyse IA pour l'offre {}", offerId);

        // 1. Récupérer les candidatures
        List<ApplicationResponse> applications = applicationService.getApplicationsForOffer(offerId, userEmail);

        // 2. Traiter chaque candidature
        for (ApplicationResponse appResp : applications) {
            try {
                // On récupère l'entité complète pour avoir le fichier PDF (byte[])
                Application app = applicationService.getApplicationCv(appResp.id(), userEmail);
                scoreApplication(app, userEmail);
            } catch (Exception e) {
                logger.error("Erreur lors de l'analyse du candidat {}: {}", appResp.id(), e.getMessage());
            }
        }
        logger.info("Fin de l'analyse IA pour l'offre {}", offerId);
    }

    /**
     * Analyse une candidature unique : Extrait le CV -> Appelle Gemini -> Sauvegarde le résultat.
     */
    private void scoreApplication(Application application, String rhEmail) {
        JobOffer offer = application.getJobOffer();

        // 1. Extraire le texte du PDF
        String cvText = PdfUtils.extractTextFromPdf(application.getCvData());
        if (cvText == null || cvText.isEmpty()) {
            logger.warn("CV vide ou illisible pour la candidature {}", application.getId());
            return;
        }

        // 2. Construire le Prompt
        String prompt = buildScoringPrompt(offer, cvText);

        // 3. Appeler Gemini
        String aiResponse = chatLanguageModel.generate(prompt);
        logger.debug("Réponse IA brute : {}", aiResponse);

        // 4. Parser la réponse (JSON)
        Integer score = parseScore(aiResponse);
        String summary = parseSummary(aiResponse);

        // 5. Sauvegarder les résultats via les services existants
        if (score != null) {
            applicationService.updateCvScore(application.getId(), new UpdateCvScoreRequest(score), rhEmail);
        }
        if (summary != null && !summary.isEmpty()) {
            applicationService.updateInternalNotes(application.getId(), new UpdateInternalNotesRequest(summary), rhEmail);
        }
    }

    /**
     * Construit le prompt envoyé à l'IA avec les instructions anti-biais.
     */
    private String buildScoringPrompt(JobOffer offer, String cvText) {
        // Limite la taille du texte pour éviter de dépasser les tokens (sécurité basique)
        String safeCvText = cvText.length() > 20000 ? cvText.substring(0, 20000) : cvText;

        return String.format("""
            Tu es un expert en recrutement impartial.
            
            MISSION :
            Analyse la pertinence du CV ci-dessous par rapport à l'offre d'emploi.
            
            RÈGLES STRICTES (ANTI-DISCRIMINATION) :
            1. IGNORE totalement : nom, prénom, genre, origine, âge, photo, adresse, situation familiale.
            2. BASE-TOI UNIQUEMENT sur : compétences techniques, expérience, soft skills en lien avec le poste.
            3. Sois factuel.
            
            --- OFFRE ---
            Titre : %s
            Description : %s
            
            --- CV CANDIDAT ---
            %s
            
            --- FORMAT DE RÉPONSE ATTENDU (JSON) ---
            Réponds UNIQUEMENT avec ce format JSON, rien d'autre :
            {
              "score": <nombre entier entre 0 et 100>,
              "resume": "<Ton analyse objective en 2 phrases maximum>"
            }
            """,
                offer.getTitle(),
                offer.getDescription(),
                safeCvText
        );
    }

    // --- Méthodes utilitaires pour extraire les infos du JSON (Regex simple pour éviter une lib en plus) ---

    private Integer parseScore(String text) {
        try {
            // Cherche "score": 85
            Pattern pattern = Pattern.compile("\"score\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            logger.error("Impossible de parser le score IA");
        }
        return null;
    }

    private String parseSummary(String text) {
        try {
            // Cherche "resume": "..."
            Pattern pattern = Pattern.compile("\"resume\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logger.error("Impossible de parser le résumé IA");
        }
        return "Analyse effectuée (détails non parsés).";
    }
}