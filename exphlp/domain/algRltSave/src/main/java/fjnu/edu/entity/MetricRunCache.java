package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MetricRunCache {
    private Integer runIndex;
    private String probInstId;
    private Long outputTime;
    private Long runtimeMs;
    private Integer paretoSize;
    private Double hv;
    private Double igdPlus;
    private Double gd;
    private Double coverage;
    private Double spreadDelta;
    private Double spacing;
    private String metricStatus;
    private String reasonCode;
}
