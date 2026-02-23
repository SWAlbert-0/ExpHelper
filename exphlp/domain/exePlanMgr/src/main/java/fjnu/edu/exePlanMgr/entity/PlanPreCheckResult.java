package fjnu.edu.exePlanMgr.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PlanPreCheckResult {
    private boolean pass;
    private String errorCode;
    private String message;
    private List<PlanPreCheckItem> items;

    public static PlanPreCheckResult passed(List<PlanPreCheckItem> items) {
        PlanPreCheckResult result = new PlanPreCheckResult();
        result.setPass(true);
        result.setItems(items == null ? Collections.emptyList() : items);
        result.setErrorCode("");
        result.setMessage("执行前检查通过");
        return result;
    }

    public static PlanPreCheckResult failed(String errorCode, String message, List<PlanPreCheckItem> items) {
        PlanPreCheckResult result = new PlanPreCheckResult();
        result.setPass(false);
        result.setErrorCode(errorCode == null ? "" : errorCode);
        result.setMessage(message == null ? "执行前检查失败" : message);
        result.setItems(items == null ? Collections.emptyList() : items);
        return result;
    }
}

