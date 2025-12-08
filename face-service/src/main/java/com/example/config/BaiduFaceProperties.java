package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "baidu.face")
public class BaiduFaceProperties {

    /**
     * API Key from Baidu Cloud console.
     */
    private String apiKey;

    /**
     * Secret Key from Baidu Cloud console.
     */
    private String apiSecret;

    /**
     * Face set group id to store/search faces.
     */
    private String groupId = "library_users";

    /**
     * Score threshold to treat a match as success.
     */
    private double matchThreshold = 80.0;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public double getMatchThreshold() {
        return matchThreshold;
    }

    public void setMatchThreshold(double matchThreshold) {
        this.matchThreshold = matchThreshold;
    }
}
