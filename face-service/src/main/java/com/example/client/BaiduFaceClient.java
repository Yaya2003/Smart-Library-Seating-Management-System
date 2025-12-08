package com.example.client;

import com.example.config.BaiduFaceProperties;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class BaiduFaceClient {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String REGISTER_URL = "https://aip.baidubce.com/rest/2.0/face/v3/faceset/user/add";
    private static final String SEARCH_URL = "https://aip.baidubce.com/rest/2.0/face/v3/search";
    private static final MediaType JSON = MediaType.parse("application/json;charset=UTF-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final BaiduFaceProperties props;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;
    private final Object tokenLock = new Object();

    public BaiduFaceClient(BaiduFaceProperties props) {
        this.props = props;
    }

    public FaceRegisterResult register(Long userId, String imageBase64) throws IOException {
        String token = ensureToken();
        HttpUrl url = HttpUrl.parse(REGISTER_URL).newBuilder()
                .addQueryParameter("access_token", token)
                .build();

        JsonObject body = new JsonObject();
        body.addProperty("image", imageBase64);
        body.addProperty("image_type", "BASE64");
        body.addProperty("group_id", props.getGroupId());
        body.addProperty("user_id", String.valueOf(userId));
        body.addProperty("liveness_control", "NORMAL");
        body.addProperty("quality_control", "LOW");

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return FaceRegisterResult.error("http_" + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(respBody, JsonObject.class);
            if (json == null || !json.has("error_code")) {
                return FaceRegisterResult.error("empty_response");
            }
            int code = json.get("error_code").getAsInt();
            String errorMsg = json.has("error_msg") ? json.get("error_msg").getAsString() : "";
            if (code != 0) {
                return FaceRegisterResult.error(code + ":" + errorMsg);
            }
            JsonObject result = json.getAsJsonObject("result");
            String faceToken = result != null && result.has("face_token") ? result.get("face_token").getAsString() : null;
            return FaceRegisterResult.ok(faceToken);
        }
    }

    public FaceSearchResult search(String imageBase64) throws IOException {
        String token = ensureToken();
        HttpUrl url = HttpUrl.parse(SEARCH_URL).newBuilder()
                .addQueryParameter("access_token", token)
                .build();

        JsonObject body = new JsonObject();
        body.addProperty("image", imageBase64);
        body.addProperty("image_type", "BASE64");
        body.addProperty("group_id_list", props.getGroupId());
        body.addProperty("liveness_control", "NORMAL");
        body.addProperty("quality_control", "LOW");

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return FaceSearchResult.error("http_" + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(respBody, JsonObject.class);
            if (json == null || !json.has("error_code")) {
                return FaceSearchResult.error("empty_response");
            }
            int code = json.get("error_code").getAsInt();
            String errorMsg = json.has("error_msg") ? json.get("error_msg").getAsString() : "";
            if (code != 0) {
                return FaceSearchResult.error(code + ":" + errorMsg);
            }
            JsonObject result = json.getAsJsonObject("result");
            if (result == null || !result.has("user_list")) {
                return FaceSearchResult.error("no_result");
            }
            JsonArray userList = result.getAsJsonArray("user_list");
            if (userList.size() == 0) {
                return FaceSearchResult.error("no_user");
            }
            JsonObject top = userList.get(0).getAsJsonObject();
            String userId = top.has("user_id") ? top.get("user_id").getAsString() : null;
            double score = top.has("score") ? top.get("score").getAsDouble() : 0.0;
            return FaceSearchResult.ok(userId, score);
        }
    }

    private String ensureToken() throws IOException {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireAt) {
            return cachedToken;
        }
        synchronized (tokenLock) {
            if (cachedToken != null && now < tokenExpireAt) {
                return cachedToken;
            }
            HttpUrl url = HttpUrl.parse(TOKEN_URL).newBuilder()
                    .addQueryParameter("grant_type", "client_credentials")
                    .addQueryParameter("client_id", props.getApiKey())
                    .addQueryParameter("client_secret", props.getApiSecret())
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("failed_to_fetch_token_http_" + response.code());
                }
                String respBody = response.body() != null ? response.body().string() : "";
                JsonObject json = gson.fromJson(respBody, JsonObject.class);
                if (json == null || !json.has("access_token")) {
                    throw new IOException("empty_token_response");
                }
                String token = json.get("access_token").getAsString();
                long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 0L;
                cachedToken = token;
                tokenExpireAt = now + Math.max((expiresIn - 300) * 1000, 5 * 60 * 1000);
                return token;
            }
        }
    }

    public record FaceRegisterResult(boolean success, String faceToken, String error) {
        public static FaceRegisterResult ok(String faceToken) {
            return new FaceRegisterResult(true, faceToken, null);
        }

        public static FaceRegisterResult error(String err) {
            return new FaceRegisterResult(false, null, err);
        }
    }

    public record FaceSearchResult(boolean success, String userId, double score, String error) {
        public static FaceSearchResult ok(String userId, double score) {
            return new FaceSearchResult(true, userId, score, null);
        }

        public static FaceSearchResult error(String err) {
            return new FaceSearchResult(false, null, 0.0, err);
        }
    }
}
