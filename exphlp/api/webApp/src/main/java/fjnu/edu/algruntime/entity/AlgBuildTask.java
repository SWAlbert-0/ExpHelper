package fjnu.edu.algruntime.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
public class AlgBuildTask {
    @MongoId
    private String taskId;
    private String algId;
    private String algName;
    private String serviceName;
    private String runtimeType;
    private String version;
    private String sourceZipPath;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String logPath;
    private String imageName;
    private String containerName;
    private long createdAt;
    private long startedAt;
    private long finishedAt;
    private String traceId;
}

