package com.mareliart.memory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mareliart.memory.model.dto.ChatRequestDTO;
import com.mareliart.memory.model.entities.Message;
import com.mareliart.memory.model.enums.Role;
import com.mareliart.memory.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ChatService(
            MessageRepository messageRepository,
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.messageRepository = messageRepository;
        this.webClient = webClientBuilder.baseUrl(ollamaUrl).build();
        this.objectMapper = objectMapper;
    }

    // Método original (para compatibilidad si se requiere)
    public String chat(ChatRequestDTO request) {
        // Implementación simplificada o lanzar excepción si ya no se usa
        return "Usa el método con streaming";
    }

    // --- NUEVO MÉTODO CON STREAMING ---
    public Flux<String> chatStream(String sessionId, String userMessage) {
        // 1. Guardar mensaje del usuario (Igual que antes)
        Message userMsg = new Message(sessionId, Role.USER, userMessage);
        messageRepository.save(userMsg);

        // 2. Historial (Igual que antes)
        List<Message> history = messageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        int historySize = Math.min(8, history.size());
        List<Message> recentHistory = history.subList(0, historySize);
        Collections.reverse(recentHistory);

        // 3. Payload para Ollama con "stream": true
        String payload = buildOllamaPayload(recentHistory, userMessage);

        // Variable para acumular la respuesta completa (para la BD)
        StringBuilder fullResponseAccumulator = new StringBuilder();

        // 4. Llamada Reactiva
        return webClient.post()
                .uri("/api/chat")
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class) // Recibimos el stream JSON de Ollama
                .map(this::extractContentFromOllamaChunk) // Extraemos solo el texto
                .filter(content -> !content.isEmpty())    // Ignoramos vacíos
                .doOnNext(fullResponseAccumulator::append) // Acumulamos en memoria
                .doOnComplete(() -> {
                    // 5. ¡MAGIA! Guardamos en BD solo al terminar el stream
                    Message assistantMsg = new Message(sessionId, Role.ASSISTANT, fullResponseAccumulator.toString());
                    messageRepository.save(assistantMsg);
                })
                .doOnError(e -> System.err.println("Error en stream: " + e.getMessage()));
    }

    private String extractContentFromOllamaChunk(String jsonChunk) {
        try {
            JsonNode root = objectMapper.readTree(jsonChunk);
            if (root.has("message") && root.get("message").has("content")) {
                return root.get("message").get("content").asText();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private String buildOllamaPayload(List<Message> history, String currentMessage) {
        StringBuilder sb = new StringBuilder();
        // NOTA: Cambiamos "stream": false a true
        sb.append("{\"model\": \"phi3:3.8b\", \"messages\": [");

        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append(",");
            Message msg = history.get(i);
            sb.append("{\"role\":\"").append(msg.getRole().name().toLowerCase())
                    .append("\",\"content\":\"").append(escapeJson(msg.getContent())).append("\"}");
        }

        sb.append(",{\"role\":\"user\",\"content\":\"").append(escapeJson(currentMessage)).append("\"}");
        sb.append("], \"stream\": true}"); // <--- CAMBIO IMPORTANTE

        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}