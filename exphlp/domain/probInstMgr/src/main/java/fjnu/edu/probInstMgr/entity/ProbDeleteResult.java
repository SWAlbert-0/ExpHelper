package fjnu.edu.probInstMgr.entity;

import lombok.Data;

@Data
public class ProbDeleteResult {
    private long deletedCount;
    private boolean repaired;
    private boolean noop;
    private boolean verified;
}
