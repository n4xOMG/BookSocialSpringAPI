package com.nix.service;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.nix.enums.NsfwLevel;
import com.nix.response.ImageSafetyAssessment;
import com.nix.response.NsfwPredictionResponse;

@Service
public class NsfwDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(NsfwDetectionService.class);

    private final RestTemplate restTemplate;
    private final String predictUrl;
    private final boolean enabled;

    public NsfwDetectionService(RestTemplate restTemplate,
            @Value("${nsfw.detector.url}") String predictUrl,
            @Value("${nsfw.detector.enabled:true}") boolean enabled) {
        this.restTemplate = restTemplate;
        this.predictUrl = predictUrl;
        this.enabled = enabled;
    }

    public ImageSafetyAssessment analyse(MultipartFile file) {
        if (!enabled) {
            logger.debug("NSFW detector disabled. Defaulting to SAFE.");
            return ImageSafetyAssessment.safeFallback();
        }

        try {
            ResponseEntity<NsfwPredictionResponse> response = restTemplate.exchange(
                    predictUrl,
                    HttpMethod.POST,
                    buildRequest(file),
                    NsfwPredictionResponse.class);

            NsfwPredictionResponse prediction = response.getBody();
            if (prediction == null) {
                logger.warn("Empty response from NSFW detector");
                return ImageSafetyAssessment.safeFallback();
            }

            if (prediction.getError() != null) {
                logger.warn("NSFW detector error for file {}: {}", prediction.getFilename(), prediction.getError());
                return ImageSafetyAssessment.safeFallback();
            }

            return mapToAssessment(prediction);
        } catch (Exception ex) {
            logger.error("Failed to call NSFW detector", ex);
            return ImageSafetyAssessment.safeFallback();
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildRequest(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new MultipartInputStreamFileResource(file));

        return new HttpEntity<>(body, headers);
    }

    private ImageSafetyAssessment mapToAssessment(NsfwPredictionResponse prediction) {
        ImageSafetyAssessment assessment = new ImageSafetyAssessment();
        NsfwLevel level = resolveLevel(prediction.getPredictedClass());
        assessment.setLevel(level);
        assessment.setConfidence(prediction.getConfidence());
        assessment.setScores(prediction.getAllScores());
        assessment.setBlurRequired(level.requiresBlur());
        assessment.setPredictedClass(prediction.getPredictedClass());
        return assessment;
    }

    private NsfwLevel resolveLevel(String predictedClass) {
        if (predictedClass == null) {
            return NsfwLevel.UNKNOWN;
        }

        String normalized = normalizeClassName(predictedClass);
        return switch (normalized) {
            case "SAFE" -> NsfwLevel.SAFE;
            case "MILD" -> NsfwLevel.MILD;
            case "EXPLICIT" -> NsfwLevel.EXPLICIT;
            default -> NsfwLevel.UNKNOWN;
        };
    }

    private String normalizeClassName(String rawClass) {
        String normalized = rawClass.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("NSFW")) {
            normalized = normalized.substring(4).trim();
        }
        if (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        return normalized.replace('_', ' ').trim();
    }

    /**
     * Adapter for MultipartFile so RestTemplate can stream the file content.
     */
    private static class MultipartInputStreamFileResource extends org.springframework.core.io.InputStreamResource {

        private final String filename;
        private final long contentLength;

        MultipartInputStreamFileResource(MultipartFile file) throws IOException {
            super(file.getInputStream());
            this.filename = file.getOriginalFilename();
            this.contentLength = file.getSize();
        }

        @Override
        public String getFilename() {
            return filename != null ? filename : "uploaded_image";
        }

        @Override
        public long contentLength() {
            return contentLength;
        }
    }
}
