package com.nix.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseWithData<T> {
    private String message;
    private boolean success;
    private T data;

    public ApiResponseWithData() {
    }

    public ApiResponseWithData(String message, boolean success) {
        this(message, success, null);
    }

    public ApiResponseWithData(String message, boolean success, T data) {
        this.message = message;
        this.success = success;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
