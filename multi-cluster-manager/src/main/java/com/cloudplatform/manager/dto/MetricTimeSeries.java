package com.cloudplatform.manager.dto;

import lombok.Data;
import java.util.List;

@Data
public class MetricTimeSeries {
    private List<String> timestamps;
    private List<Double> values;
}
