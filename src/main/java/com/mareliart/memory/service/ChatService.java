package com.mareliart.memory.service;

import com.mareliart.memory.model.dto.ChatRequestDTO;
import com.mareliart.memory.model.entities.Message;
import com.mareliart.memory.model.enums.Role;
import com.mareliart.memory.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private final RestTemplate restTemplate;
    private final String ollamaUrl;

    public ChatService(
            MessageRepository messageRepository,
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl
    ) {
        this.messageRepository = messageRepository;
        this.restTemplate = new RestTemplate();
        this.ollamaUrl = ollamaUrl;
    }

    public String chat(ChatRequestDTO request) {
        String userMessage = request.message();

        // 1. Guardar mensaje del usuario
        Message userMsg = new Message(request.sessionId(), Role.USER, userMessage);
        messageRepository.save(userMsg);

        // 2. Recuperar historial reciente (últimos 8 mensajes)
        List<Message> history = messageRepository
                .findBySessionIdOrderByCreatedAtDesc(request.sessionId());

        // Tomar últimos 8 y ordenar cronológicamente (antiguo → reciente)
        int historySize = Math.min(8, history.size());
        List<Message> recentHistory = history.subList(0, historySize);
        Collections.reverse(recentHistory);  // ✅ Más importante: ANTIQUO → RECIENTE

        // 3. Construir payload para Ollama
        String ollamaPayload = buildOllamaPayload(recentHistory, userMessage);

        // 4. Llamar a Ollama
        String assistantResponse = callOllama(ollamaPayload);

        // 5. Guardar respuesta del modelo
        Message assistantMsg = new Message(request.sessionId(), Role.ASSISTANT, assistantResponse);
        messageRepository.save(assistantMsg);

        return assistantResponse;
    }

    private String buildOllamaPayload(List<Message> history, String currentMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\": \"llama3.1:8b\", \"messages\": [");  // Modelo ligero

        // Historial cronológico
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append(",");
            Message msg = history.get(i);
            sb.append("{\"role\":\"").append(msg.getRole().name().toLowerCase())
                    .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
        }

        // Mensaje actual del usuario
        sb.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(currentMessage)).append("\"}");
        sb.append("], \"stream\": false}");

        return sb.toString();
    }

    private String callOllama(String payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ollamaUrl + "/api/chat", entity, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, String> message = (Map<String, String>) body.get("message");

            return message != null ? message.get("content") : "No response from model";
        } catch (Exception e) {
            return "Error conectando con Ollama: " + e.getMessage();
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
