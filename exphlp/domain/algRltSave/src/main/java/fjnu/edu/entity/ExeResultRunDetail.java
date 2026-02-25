package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ExeResultRunDetail {
    private Integer runIndex;
    private String probInstId;
    private String probInstName;
    private String startTime;
    private String outputTime;
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
    private List<ParetoPoint> paretoPoints;
    private List<EachResult> rawEachResults;
}
