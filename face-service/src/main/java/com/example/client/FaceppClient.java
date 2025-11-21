package com.example.client;

import com.example.config.FaceppProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class FaceppClient {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final FaceppProperties props;

    public FaceppClient(FaceppProperties props) {
        this.props = props;
    }

    public FaceppCompareResult compare(String imageA, String imageB) throws IOException {
        FormBody body = new FormBody.Builder()
                .add("api_key", props.getApiKey())
                .add("api_secret", props.getApiSecret())
                .add("image_base64_1", imageA)
                .add("image_base64_2", imageB)
                .build();

        Request request = new Request.Builder()
                .url(props.getCompareUrl())
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return FaceppCompareResult.error("http_" + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            JsonObject json = gson.fromJson(respBody, JsonObject.class);
            if (json == null || !json.has("confidence")) {
                return FaceppCompareResult.error("empty_response");
            }
            double confidence = json.get("confidence").getAsDouble();
            return FaceppCompareResult.ok(confidence);
        }
    }

    public record FaceppCompareResult(boolean success, double score, String error) {
        public static FaceppCompareResult ok(double score) {
            return new FaceppCompareResult(true, score, null);
        }

        public static FaceppCompareResult error(String err) {
            return new FaceppCompareResult(false, 0.0, err);
        }
    }
}
