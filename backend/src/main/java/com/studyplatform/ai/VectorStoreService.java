package com.studyplatform.ai;

import com.studyplatform.file.FileAnnotation;
import com.studyplatform.file.FileAnnotationRepository;
import com.studyplatform.file.UploadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final EmbeddingService embeddingService;
    private final FileAnnotationRepository fileAnnotationRepository;

    @Transactional(readOnly = true)
    public List<String> findMostRelevantChunks(String query, List<UploadedFile> files) {
        final int k = 5; // Top 5 most relevant chunks

        List<String> chunks = files.stream()
                .flatMap(file -> fileAnnotationRepository.findByUploadedFileId(file.getId()).stream())
                .map(FileAnnotation::getContent)
                .collect(Collectors.toList());

        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.generateEmbeddings(List.of(query)).get(0);
        List<float[]> chunkEmbeddings = embeddingService.generateEmbeddings(chunks);

        java.util.List<Map.Entry<String, Double>> similarityEntries = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            float[] chunkEmbedding = chunkEmbeddings.get(i);
            double similarity = calculateCosineSimilarity(queryEmbedding, chunkEmbedding);
            similarityEntries.add(new AbstractMap.SimpleEntry<>(chunks.get(i), similarity));
        }

        return similarityEntries.stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must be non-null and of the same length.");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
