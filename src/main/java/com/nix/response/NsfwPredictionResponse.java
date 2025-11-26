package com.nix.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NsfwPredictionResponse {

    private String filename;

    @JsonProperty("predicted_class")
    private String predictedClass;

    @JsonProperty("predicted_index")
    private Integer predictedIndex;

    @JsonProperty("is_nsfw")
    private Boolean nsfw;

    private Double confidence;

    @JsonProperty("all_scores")
    private Map<String, Double> allScores;

    private String status;

    private String error;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    public Integer getPredictedIndex() {
        return predictedIndex;
    }

    public void setPredictedIndex(Integer predictedIndex) {
        this.predictedIndex = predictedIndex;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Double> getAllScores() {
        return allScores;
    }

    public void setAllScores(Map<String, Double> allScores) {
        this.allScores = allScores;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
