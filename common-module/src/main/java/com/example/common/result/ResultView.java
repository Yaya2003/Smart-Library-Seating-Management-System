package com.example.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ResultView<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResultView<T> success(T data) {
        return new ResultView<>(HttpStatus.OK.value(), "success", data);
    }

    public static <T> ResultView<T> error(int code, String message) {
        return new ResultView<>(code, message, null);
    }
}
