package com.nix.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageUploadResponse {

    private String url;
    private String relativePath;
    private ImageSafetyAssessment safety;

    public ImageUploadResponse() {
    }

    public ImageUploadResponse(String url, String relativePath, ImageSafetyAssessment safety) {
        this.url = url;
        this.relativePath = relativePath;
        this.safety = safety;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public ImageSafetyAssessment getSafety() {
        return safety;
    }

    public void setSafety(ImageSafetyAssessment safety) {
        this.safety = safety;
    }
}
