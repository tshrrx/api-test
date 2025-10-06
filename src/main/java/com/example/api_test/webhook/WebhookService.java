package com.example.api_test.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    private static final String NAME = "Tushar Hirekhan";
    private static final String REG_NO = "112215190";
    private static final String EMAIL = "112215190@cse.iiitp.ac.in";

    public WebhookService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void executeOnStartup() {
        logger.info("Application started. Initiating webhook flow...");

        try {
            WebhookResponse webhookResponse = generateWebhook();
            logger.info("Webhook generated successfully");
            logger.info("Webhook URL: {}", webhookResponse.getWebhook());
            logger.info("Access Token (first 50 chars): {}",
                    webhookResponse.getAccessToken().substring(0, Math.min(50, webhookResponse.getAccessToken().length())));

            String sqlQuery = getSqlQuery();
            logger.info("SQL Query prepared: {}", sqlQuery);

            submitSolution(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), sqlQuery);
            logger.info("Solution submitted successfully!");

        } catch (Exception e) {
            logger.error("Error in webhook flow: ", e);
            if (e.getMessage() != null) {
                logger.error("Error message: {}", e.getMessage());
            }
        }
    }

    private WebhookResponse generateWebhook() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", NAME);
        requestBody.put("regNo", REG_NO);
        requestBody.put("email", EMAIL);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        logger.info("Sending request to generate webhook...");
        logger.info("Request body: {}", objectMapper.writeValueAsString(requestBody));

        ResponseEntity<String> response = restTemplate.exchange(
                GENERATE_WEBHOOK_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        logger.info("Generate webhook response status: {}", response.getStatusCode());
        logger.info("Generate webhook response body: {}", response.getBody());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        String webhook = jsonNode.get("webhook").asText();
        String accessToken = jsonNode.get("accessToken").asText();

        return new WebhookResponse(webhook, accessToken);
    }

    private String getSqlQuery() {
        return Solution();
    }


    private String Solution() {
        return "SELECT\r\n" + //
                        "    e1.EMP_ID,\r\n" + //
                        "    e1.FIRST_NAME,\r\n" + //
                        "    e1.\"LAST NAME\" AS LAST_NAME,\r\n" + //
                        "    d.DEPARTMENT_NAME,\r\n" + //
                        "    COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT\r\n" + //
                        "FROM\r\n" + //
                        "    EMPLOYEE e1\r\n" + //
                        "JOIN\r\n" + //
                        "    DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID\r\n" + //
                        "LEFT JOIN\r\n" + //
                        "    EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e1.DOB < e2.DOB\r\n" + //
                        "GROUP BY\r\n" + //
                        "    e1.EMP_ID,\r\n" + //
                        "    e1.FIRST_NAME,\r\n" + //
                        "    e1.\"LAST NAME\",\r\n" + //
                        "    d.DEPARTMENT_NAME\r\n" + //
                        "ORDER BY\r\n" + //
                        "    e1.EMP_ID DESC;";
    }

    private void submitSolution(String webhookUrl, String accessToken, String sqlQuery) throws Exception {
        String cleanQuery = sqlQuery.trim().replaceAll("\\s+", " ");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Authorization", accessToken.trim());

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("finalQuery", cleanQuery);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        logger.info("=== SUBMISSION REQUEST DETAILS ===");
        logger.info("Webhook URL: {}", webhookUrl);
        logger.info("Authorization header: Bearer {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
        logger.info("Request body: {}", objectMapper.writeValueAsString(requestBody));
        logger.info("Headers: {}", headers);
        logger.info("-------------------------------------");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            logger.info("Submission response status: {}", response.getStatusCode());
            logger.info("Submission response body: {}", response.getBody());
        } catch (Exception e) {
            logger.error("Failed to submit solution");
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            throw e;
        }
    }

    private static class WebhookResponse {
        private final String webhook;
        private final String accessToken;

        public WebhookResponse(String webhook, String accessToken) {
            this.webhook = webhook;
            this.accessToken = accessToken;
        }

        public String getWebhook() {
            return webhook;
        }

        public String getAccessToken() {
            return accessToken;
        }
    }
}