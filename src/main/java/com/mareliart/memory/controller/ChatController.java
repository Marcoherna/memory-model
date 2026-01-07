package com.mareliart.memory.controller;

import com.mareliart.memory.model.dto.ChatRequestDTO;
import com.mareliart.memory.model.dto.ChatResponseDTO;
import com.mareliart.memory.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/api/chat")  // ← Tu API original
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        String answer = chatService.chat(request);
        return ResponseEntity.ok(new ChatResponseDTO(answer, request.sessionId()));
    }

    @GetMapping("/api/chat/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok("Historial para: " + sessionId);
    }

    @PostMapping("/v1/chat/completions")  // ← OpenAI compatible
    public ResponseEntity<Map<String, Object>> chatOpenAI(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String userMessage = messages.get(messages.size() - 1).get("content");
        String sessionId = "mi-guion";  // ← Fijo por ahora, luego lo lees de headers/context

        ChatRequestDTO chatRequest = new ChatRequestDTO(sessionId, userMessage);
        String answer = chatService.chat(chatRequest);

        Map<String, Object> response = Map.of(
                "id", "chatcmpl-" + System.currentTimeMillis(),
                "object", "chat.completion",
                "choices", List.of(Map.of(
                        "message", Map.of("role", "assistant", "content", answer),
                        "finish_reason", "stop"
                ))
        );
        return ResponseEntity.ok(response);
    }

    // En ChatController.java
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

}
