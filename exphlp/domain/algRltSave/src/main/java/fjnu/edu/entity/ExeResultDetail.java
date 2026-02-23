package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ExeResultDetail {
    private String status;
    private String reasonCode;
    private String message;
    private String planId;
    private String algId;
    private String metricVersion;
    private List<ExeResultRunDetail> runs = new ArrayList<>();
    private ExeResultAggregate aggregate;
}

