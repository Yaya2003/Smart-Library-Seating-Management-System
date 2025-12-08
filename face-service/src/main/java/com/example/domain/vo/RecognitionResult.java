package com.example.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResult {
    private boolean matched;
    private Long userId;
    private double score;
    private String message;
}
