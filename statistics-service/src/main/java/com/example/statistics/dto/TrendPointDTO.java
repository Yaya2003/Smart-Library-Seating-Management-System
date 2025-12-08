package com.example.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointDTO {

    /**
     * 维度名称，如时间点/时间段名称
     */
    private String label;

    /**
     * 数值
     */
    private long value;
}

