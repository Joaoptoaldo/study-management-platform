package com.studyplatform.ai;

import com.studyplatform.ai.dto.RAGChatRequest;
import com.studyplatform.ai.dto.RAGChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat (RAG)", description = "Endpoints para interação com o tutor de IA via RAG")
public class ChatController {

    private final RAGChatService ragChatService;

    @PostMapping("/ask")
    @Operation(summary = "Faz uma pergunta ao tutor de IA sobre um preparatório de exame")
    public ResponseEntity<RAGChatResponse> askQuestion(@Valid @RequestBody RAGChatRequest request) {
        RAGChatResponse response = ragChatService.askQuestion(request);
        return ResponseEntity.ok(response);
    }
}
