package com.studyplatform.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplatform.ai.dto.RAGChatRequest;
import com.studyplatform.ai.dto.RAGChatResponse;
import com.studyplatform.examprep.ExamPrep;
import com.studyplatform.examprep.ExamPrepRepository;
import com.studyplatform.file.UploadedFile;
import com.studyplatform.file.UploadedFileRepository;
import com.studyplatform.shared.exception.BusinessException;
import com.studyplatform.shared.exception.ResourceNotFoundException;
import com.studyplatform.subject.Subject;
import com.studyplatform.user.User;
import com.studyplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RAGChatService {

    private final VectorStoreService vectorStoreService;
    private final ExamPrepRepository examPrepRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    private void validatePremiumUser(User user) {
        if (!Boolean.TRUE.equals(user.getPremium())) {
            throw new BusinessException("upgrade_required");
        }
    }

    public RAGChatResponse askQuestion(RAGChatRequest request) {
        User user = getAuthenticatedUser();
        validatePremiumUser(user);

        ExamPrep examPrep = examPrepRepository.findByIdAndUserId(request.getExamPrepId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Preparatório de exame não encontrado."));

        Subject subject = examPrep.getSubject();
        List<UploadedFile> userFiles = uploadedFileRepository.findByUserIdAndSubjectId(user.getId(), subject.getId());

        if (userFiles.isEmpty()) {
            throw new BusinessException("Nenhum arquivo encontrado para a matéria deste preparatório de exame.");
        }

        List<String> relevantChunks = vectorStoreService.findMostRelevantChunks(request.getQuestion(), userFiles);

        if (relevantChunks.isEmpty()) {
            return RAGChatResponse.builder()
                    .answer("Desculpe, não consegui encontrar informações relevantes em seus documentos para responder a essa pergunta.")
                    .build();
        }

        String context = String.join("", relevantChunks);
        String prompt = String.format("Com base nas informações do PDF a seguir: \"%s \"\nResponda a seguinte pergunta em português do Brasil: \"%s\"",
                context, request.getQuestion()
        );

        try {
            String answer = callGeminiApi(prompt);
            return RAGChatResponse.builder().answer(answer).build();
        } catch (IOException e) {
            throw new BusinessException("Falha ao se comunicar com a API de IA. Tente novamente mais tarde.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("A resposta foi interrompida.");
        }
    }

    private String callGeminiApi(String prompt) throws IOException, InterruptedException {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("SUA_CHAVE_GEMINI_AQUI")) {
             throw new BusinessException("API Key do Gemini não configurada.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new BusinessException("Erro na resposta da API Gemini: HTTP " + response.statusCode() + " - " + response.body());
        }

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<>() {});
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new BusinessException("Nenhum candidato retornado pelo Gemini.");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new BusinessException("Nenhuma parte de texto retornada pelo Gemini.");
        }

        return (String) parts.get(0).get("text");
    }
}
