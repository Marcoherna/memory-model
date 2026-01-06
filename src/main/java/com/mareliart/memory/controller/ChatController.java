package com.mareliart.memory.controller;

import com.mareliart.memory.model.dto.ChatRequestDTO;
import com.mareliart.memory.model.dto.ChatResponseDTO;
import com.mareliart.memory.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {

        String answer = chatService.chat(request);
        return ResponseEntity.ok(new ChatResponseDTO(answer, request.sessionId()));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        // Aquí iría endpoint para recuperar historial (opcional)
        return ResponseEntity.ok("Historial para: " + sessionId);
    }
}
