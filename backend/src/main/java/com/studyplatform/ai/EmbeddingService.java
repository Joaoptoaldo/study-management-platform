package com.studyplatform.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplatform.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new BusinessException("A lista de textos para gerar embeddings não pode ser vazia.");
        }

        try {
            return callEmbeddingApi(texts);
        } catch (IOException e) {
            throw new BusinessException("Falha ao se comunicar com a API de IA para gerar embeddings. Tente novamente mais tarde.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Geração de embeddings interrompida.");
        }
    }

    private List<float[]> callEmbeddingApi(List<String> texts) throws IOException, InterruptedException {
        if (isMockMode()) {
            throw new BusinessException("API Key do Gemini não configurada para embeddings.");
        }

        List<Map<String, Object>> requests = texts.stream()
                .map(text -> Map.of(
                        "model", "models/text-embedding-004",
                        "content", Map.of("parts", List.of(Map.of("text", text)))
                ))
                .collect(Collectors.toList());

        Map<String, Object> requestBody = Map.of("requests", requests);
        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models:batchEmbedContents?key=" + geminiApiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new BusinessException("Erro na resposta da API Gemini Embeddings: HTTP " + response.statusCode() + " - " + response.body());
        }

        Map<String, List<Map<String, Object>>> responseMap = objectMapper.readValue(response.body(), new TypeReference<>() {});
        List<Map<String, Object>> embeddingsList = responseMap.get("embeddings");

        if (embeddingsList == null || embeddingsList.isEmpty()) {
            throw new BusinessException("Nenhum embedding retornado pelo Gemini.");
        }

        return embeddingsList.stream()
                .map(embeddingMap -> {
                    List<Double> values = (List<Double>) embeddingMap.get("value");
                    float[] floatValues = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        floatValues[i] = values.get(i).floatValue();
                    }
                    return floatValues;
                })
                .collect(Collectors.toList());
    }

    private boolean isMockMode() {
        return geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("SUA_CHAVE_GEMINI_AQUI");
    }
}
