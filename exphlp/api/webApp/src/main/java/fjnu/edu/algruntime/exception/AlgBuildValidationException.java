package fjnu.edu.algruntime.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AlgBuildValidationException extends IllegalArgumentException {
    private final String phase;
    private final List<String> fixHints;
    private final Map<String, Object> contractCheck;

    public AlgBuildValidationException(String message, String phase, List<String> fixHints, Map<String, Object> contractCheck) {
        super(message);
        this.phase = phase == null ? "VALIDATE" : phase;
        this.fixHints = fixHints == null ? Collections.emptyList() : fixHints;
        this.contractCheck = contractCheck == null ? Collections.emptyMap() : contractCheck;
    }

    public String getPhase() {
        return phase;
    }

    public List<String> getFixHints() {
        return fixHints;
    }

    public Map<String, Object> getContractCheck() {
        return contractCheck;
    }
}

