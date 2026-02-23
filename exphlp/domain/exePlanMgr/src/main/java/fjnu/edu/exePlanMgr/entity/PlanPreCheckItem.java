package fjnu.edu.exePlanMgr.entity;

import lombok.Data;

@Data
public class PlanPreCheckItem {
    private String algId;
    private String algName;
    private String serviceName;
    private int instanceCount;
    private boolean reachable;
    private String errorCode;
    private String diagnosis;
    private String suggestion;
}

