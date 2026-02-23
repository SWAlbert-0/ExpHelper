package fjnu.edu.exePlanMgr.entity;

import lombok.Data;

@Data
public class ExePlanDeleteResult {
    private long deletedCount;
    private boolean existed;
    private boolean noop;
    private boolean verified;
    private boolean blocked;
}
