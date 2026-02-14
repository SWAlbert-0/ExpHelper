package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
public class PlanExeResult {

    @MongoId
    private String planExeResultId;
    private String planId;
    private String algId;
    private String algName;
    private int runNum;
    private long startTime;
    private long outputTime;
    private List<GenResult> genResults;
}
