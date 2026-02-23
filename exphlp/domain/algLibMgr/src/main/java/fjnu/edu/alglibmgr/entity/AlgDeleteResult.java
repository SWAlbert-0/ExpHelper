package fjnu.edu.alglibmgr.entity;

import lombok.Data;

@Data
public class AlgDeleteResult {
    private long deletedCount;
    private boolean repaired;
    private boolean noop;
    private boolean verified;
}
