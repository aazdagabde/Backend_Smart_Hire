package com.smarthire.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Async
public class N8nService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${n8n.webhook.application-received}")
    private String webhookUrlApplicationReceived;

    @Value("${n8n.webhook.invite-candidate}")
    private String webhookUrlInviteCandidate;

    /**
     * Déclenche le workflow n8n lorsqu'un candidat postule.
     */
    @Async
    public void triggerApplicationReceived(String candidateName, String candidateEmail, String jobTitle, Long applicationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "APPLICATION_RECEIVED");
        payload.put("candidateName", candidateName);
        payload.put("candidateEmail", candidateEmail);
        payload.put("jobTitle", jobTitle);
        payload.put("applicationId", applicationId);

        sendWebhook(webhookUrlApplicationReceived, payload);
    }

    /**
     * Déclenche le workflow n8n pour inviter un candidat.
     */
    @Async
    public void triggerInviteCandidate(String candidateName, String candidateEmail, String jobTitle, String messageRh, String date) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "INVITE_CANDIDATE");
        payload.put("candidateName", candidateName);
        payload.put("candidateEmail", candidateEmail);
        payload.put("jobTitle", jobTitle);
        payload.put("message", messageRh);
        payload.put("date", date);

        sendWebhook(webhookUrlInviteCandidate, payload);
    }

    @Async
    public void triggerCandidateSelected(String candidateName, String candidateEmail, String jobTitle, Double score) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "CANDIDATE_SELECTED");
        payload.put("candidateName", candidateName);
        payload.put("candidateEmail", candidateEmail);
        payload.put("jobTitle", jobTitle);
        payload.put("score", score != null ? score : "N/A");

        // Utilisation de la méthode générique sendWebhook déjà présente dans votre classe
        //sendWebhook(webhookUrlCandidateSelected, payload);
    }

    private void sendWebhook(String url, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // Envoi de la requête POST vers n8n
            restTemplate.postForObject(url, request, String.class);
            System.out.println("✅ Webhook n8n envoyé avec succès : " + url);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'appel n8n : " + e.getMessage());
            // On loggue juste l'erreur, on ne bloque pas l'application
        }
    }
}