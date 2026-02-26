package fjnu.edu.algruntime.service.impl;

import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.algruntime.service.AlgBuildTaskService;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AlgBuildTaskServiceImpl implements AlgBuildTaskService {
    private static final String COLLECTION = "algBuildTask";
    private static final DateTimeFormatter VERSION_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MongoTemplate mongoTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Value("${alg.build.work-dir:temp/alg-build}")
    private String buildWorkDir;

    public AlgBuildTaskServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public AlgBuildTask createUploadTask(AlgInfo algInfo, MultipartFile file, String traceId) {
        if (algInfo == null || !StringUtils.hasText(algInfo.getAlgId())) {
            throw new IllegalArgumentException("算法不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!originalName.endsWith(".zip")) {
            throw new IllegalArgumentException("仅支持 zip 源码包");
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String version = "v" + LocalDateTime.now().format(VERSION_FMT);
        Path taskDir = taskDir(taskId);
        try {
            Files.createDirectories(taskDir);
            Path target = taskDir.resolve("source.zip");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            AlgBuildTask task = new AlgBuildTask();
            task.setTaskId(taskId);
            task.setAlgId(algInfo.getAlgId());
            task.setAlgName(algInfo.getAlgName());
            task.setServiceName(algInfo.getServiceName());
            task.setRuntimeType(normalizeRuntime(algInfo.getRuntimeType()));
            task.setVersion(version);
            task.setSourceZipPath(target.toAbsolutePath().toString());
            task.setStatus("PENDING");
            task.setCreatedAt(System.currentTimeMillis());
            task.setTraceId(traceId == null ? "" : traceId);
            task.setLogPath(taskDir.resolve("build.log").toAbsolutePath().toString());
            task.setImageName("exphlp-user-alg-" + safeToken(algInfo.getAlgId()) + ":" + version.toLowerCase(Locale.ROOT));
            task.setContainerName("c_user_alg_" + safeToken(algInfo.getAlgId()));
            mongoTemplate.save(task, COLLECTION);
            return task;
        } catch (IOException e) {
            throw new IllegalStateException("保存源码包失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AlgBuildTask triggerBuild(String taskId, String traceId) {
        AlgBuildTask task = getTask(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if ("RUNNING".equalsIgnoreCase(task.getStatus())) {
            return task;
        }
        updateState(taskId, "RUNNING", "STARTED", "", System.currentTimeMillis(), 0L);
        executor.submit(() -> runBuildTask(taskId, traceId));
        return getTask(taskId);
    }

    @Override
    public AlgBuildTask getTask(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        Query q = new Query(Criteria.where("_id").is(taskId));
        return mongoTemplate.findOne(q, AlgBuildTask.class, COLLECTION);
    }

    @Override
    public String tailLog(String taskId, int tail) {
        AlgBuildTask task = getTask(taskId);
        if (task == null || !StringUtils.hasText(task.getLogPath())) {
            return "";
        }
        Path log = Paths.get(task.getLogPath());
        if (!Files.exists(log)) {
            return "";
        }
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            if (tail <= 0 || tail >= lines.size()) {
                return String.join("\n", lines);
            }
            return String.join("\n", lines.subList(lines.size() - tail, lines.size()));
        } catch (IOException e) {
            return "读取日志失败: " + e.getMessage();
        }
    }

    private void runBuildTask(String taskId, String traceId) {
        AlgBuildTask task = getTask(taskId);
        if (task == null) {
            return;
        }
        Path logPath = Paths.get(task.getLogPath());
        try {
            Files.createDirectories(logPath.getParent());
            List<String> cmd = buildScriptCommand(task);
            appendLog(logPath, "traceId=" + (traceId == null ? "" : traceId));
            appendLog(logPath, "command=" + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(Paths.get("").toAbsolutePath().toFile());
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(logPath, line);
                }
            }
            int exit = p.waitFor();
            if (exit == 0) {
                updateState(taskId, "SUCCESS", "OK", "", 0L, System.currentTimeMillis());
            } else {
                updateState(taskId, "FAILED", "ALG_BUILD_FAILED", "构建或启动失败，请查看日志", 0L, System.currentTimeMillis());
            }
        } catch (Exception ex) {
            try {
                appendLog(logPath, "exception=" + ex.getMessage());
            } catch (Exception ignored) {
            }
            updateState(taskId, "FAILED", "ALG_BUILD_FAILED", ex.getMessage(), 0L, System.currentTimeMillis());
        }
    }

    private List<String> buildScriptCommand(AlgBuildTask task) {
        List<String> cmd = new ArrayList<>();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String workRoot = Paths.get(buildWorkDir).toAbsolutePath().toString();
        if (isWindows) {
            cmd.add("powershell");
            cmd.add("-ExecutionPolicy");
            cmd.add("Bypass");
            cmd.add("-File");
            cmd.add("scripts/tasks/build-uploaded-alg.ps1");
            cmd.add("-TaskId");
            cmd.add(task.getTaskId());
            cmd.add("-AlgId");
            cmd.add(task.getAlgId());
            cmd.add("-ServiceName");
            cmd.add(task.getServiceName());
            cmd.add("-RuntimeType");
            cmd.add(task.getRuntimeType());
            cmd.add("-Version");
            cmd.add(task.getVersion());
            cmd.add("-SourceZip");
            cmd.add(task.getSourceZipPath());
            cmd.add("-ImageName");
            cmd.add(task.getImageName());
            cmd.add("-ContainerName");
            cmd.add(task.getContainerName());
            cmd.add("-WorkRoot");
            cmd.add(workRoot);
        } else {
            cmd.add("sh");
            cmd.add("scripts/tasks/build-uploaded-alg.sh");
            cmd.add("--task-id");
            cmd.add(task.getTaskId());
            cmd.add("--alg-id");
            cmd.add(task.getAlgId());
            cmd.add("--service-name");
            cmd.add(task.getServiceName());
            cmd.add("--runtime-type");
            cmd.add(task.getRuntimeType());
            cmd.add("--version");
            cmd.add(task.getVersion());
            cmd.add("--source-zip");
            cmd.add(task.getSourceZipPath());
            cmd.add("--image-name");
            cmd.add(task.getImageName());
            cmd.add("--container-name");
            cmd.add(task.getContainerName());
            cmd.add("--work-root");
            cmd.add(workRoot);
        }
        return cmd;
    }

    private void updateState(String taskId, String status, String errorCode, String errorMessage, long startedAt, long finishedAt) {
        Query q = new Query(Criteria.where("_id").is(taskId));
        Update u = new Update()
                .set("status", status)
                .set("errorCode", errorCode)
                .set("errorMessage", errorMessage == null ? "" : errorMessage);
        if (startedAt > 0) {
            u.set("startedAt", startedAt);
        }
        if (finishedAt > 0) {
            u.set("finishedAt", finishedAt);
        }
        mongoTemplate.updateFirst(q, u, COLLECTION);
    }

    private Path taskDir(String taskId) {
        return Paths.get(buildWorkDir, taskId);
    }

    private String normalizeRuntime(String runtimeType) {
        if (!StringUtils.hasText(runtimeType)) {
            return "java";
        }
        return "python".equalsIgnoreCase(runtimeType.trim()) ? "python" : "java";
    }

    private String safeToken(String text) {
        if (!StringUtils.hasText(text)) {
            return "unknown";
        }
        return text.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void appendLog(Path path, String line) throws IOException {
        String output = "[" + System.currentTimeMillis() + "] " + line + System.lineSeparator();
        Files.write(path, output.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
