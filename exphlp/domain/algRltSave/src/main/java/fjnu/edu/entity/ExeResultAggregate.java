package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExeResultAggregate {
    private Integer runCount;
    private Double runtimeMsMean;
    private Double hvMean;
    private Double igdPlusMean;
    private Double gdMean;
    private Double coverageMean;
    private Double spreadDeltaMean;
    private Double spacingMean;
}
