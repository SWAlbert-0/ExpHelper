package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GenResult {

    private Integer algRunResultId;
    private long outTime;
    private String probInstId;
    private List<EachResult> eachResults;
}
