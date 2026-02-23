package fjnu.edu.exePlanMgr.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class ExePlanLog {
    @Id
    private String logId;
    private String planId;
    private long seq;
    private long ts;
    private String level;
    private String stage;
    private String message;
    private String algId;
    private Integer runIndex;
    private String probInstId;
}

