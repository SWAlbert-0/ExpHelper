package fjnu.edu.algruntime.service;

import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import org.springframework.web.multipart.MultipartFile;

public interface AlgBuildTaskService {
    AlgBuildTask createUploadTask(AlgInfo algInfo, MultipartFile file, String traceId);

    AlgBuildTask triggerBuild(String taskId, String traceId);

    AlgBuildTask getTask(String taskId);

    AlgBuildTask getLatestTaskByAlgId(String algId);

    void updateContainerName(String taskId, String containerName);

    String tailLog(String taskId, int tail);
}
