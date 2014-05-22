package com.example.floudcloud.app.operation;

public abstract class RemoteOperation implements Operation {
    private String apiKey;
    private String path;
    private String uri;

    public RemoteOperation(String apiKey, String uri, String path) {
        this.apiKey = apiKey;
        this.uri = uri;
        this.path = path;
    }


    public String getApiKey() {
        return apiKey;
    }

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }
}
