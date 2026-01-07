package com.mareliart.memory.controller;

import com.mareliart.memory.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/v1/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        return ResponseEntity.ok(Map.of(
                "object", "list",
                "data", List.of(Map.of(
                        "id", "memory-model",
                        "object", "model",
                        "created", System.currentTimeMillis(),
                        "owned_by", "mareliart"
                ))
        ));
    }

    // --- ENDPOINT PRINCIPAL MODIFICADO PARA STREAMING ---
    @PostMapping(value = "/v1/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatOpenAIStream(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        if (messages == null || messages.isEmpty()) {
            return Flux.empty();
        }

        String userMessage = messages.get(messages.size() - 1).get("content");
        String sessionId = "mi-guion"; // O extraer de headers

        // Llamamos al servicio reactivo
        return chatService.chatStream(sessionId, userMessage)
                .map(chunk -> {
                    // Convertimos cada letra al formato especial de OpenAI: "data: {...}"
                    String jsonChunk = "{\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\""
                            + escapeJson(chunk) + "\"},\"finish_reason\":null}]}";
                    return "data: " + jsonChunk + "\n\n";
                })
                .concatWith(Flux.just("data: [DONE]\n\n")); // Señal de fin requerida por OpenAI
    }

    // Método auxiliar para escapar JSON en el controlador
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @GetMapping("/api/chat/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok("Historial para: " + sessionId);
    }
}