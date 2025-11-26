package com.nix.response;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nix.enums.NsfwLevel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageSafetyAssessment {

    private NsfwLevel level;
    private Double confidence;
    private Map<String, Double> scores;
    private boolean blurRequired;
    private String predictedClass;

    public static ImageSafetyAssessment safeFallback() {
        ImageSafetyAssessment assessment = new ImageSafetyAssessment();
        assessment.setLevel(NsfwLevel.SAFE);
        assessment.setConfidence(1.0);
        assessment.setScores(Collections.emptyMap());
        assessment.setBlurRequired(false);
        assessment.setPredictedClass("safe");
        return assessment;
    }

    public NsfwLevel getLevel() {
        return level;
    }

    public void setLevel(NsfwLevel level) {
        this.level = level;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    public boolean isBlurRequired() {
        return blurRequired;
    }

    public void setBlurRequired(boolean blurRequired) {
        this.blurRequired = blurRequired;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }
}
