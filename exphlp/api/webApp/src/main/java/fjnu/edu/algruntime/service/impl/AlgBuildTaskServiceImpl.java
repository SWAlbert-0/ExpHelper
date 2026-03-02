package fjnu.edu.algruntime.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fjnu.edu.algruntime.exception.AlgBuildValidationException;
import fjnu.edu.algruntime.entity.AlgBuildTask;
import fjnu.edu.algruntime.service.AlgBuildTaskService;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class AlgBuildTaskServiceImpl implements AlgBuildTaskService {
    private static final String COLLECTION = "algBuildTask";
    private static final DateTimeFormatter VERSION_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Value("${alg.build.work-dir:temp/alg-build}")
    private String buildWorkDir;

    public AlgBuildTaskServiceImpl(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
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
            ValidationResult validation = validateSourcePackage(algInfo, target);
            if (!validation.getErrors().isEmpty()) {
                throw new AlgBuildValidationException(
                        "源码包校验失败: " + validation.getErrors().get(0),
                        "VALIDATE",
                        validation.buildFixHints(),
                        validation.toContractCheck()
                );
            }

            AlgBuildTask task = new AlgBuildTask();
            task.setTaskId(taskId);
            task.setAlgId(algInfo.getAlgId());
            task.setAlgName(algInfo.getAlgName());
            task.setServiceName(algInfo.getServiceName());
            task.setRuntimeType(normalizeRuntime(algInfo.getRuntimeType()));
            task.setVersion(version);
            task.setSourceZipPath(target.toAbsolutePath().toString());
            task.setStatus("PENDING");
            task.setPhase("VALIDATE_PASSED");
            task.setCreatedAt(System.currentTimeMillis());
            task.setTraceId(traceId == null ? "" : traceId);
            task.setLogPath(taskDir.resolve("build.log").toAbsolutePath().toString());
            task.setImageName("exphlp-user-alg-" + safeToken(algInfo.getAlgId()) + ":" + version.toLowerCase(Locale.ROOT));
            task.setContainerName(buildContainerName(algInfo.getServiceName(), algInfo.getAlgId()));
            task.setContractCheck(validation.toContractCheck());
            task.setFixHints(validation.buildFixHints());
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
        updateState(taskId, "RUNNING", "STARTED", "", "BUILD_AND_START", Collections.emptyList(), System.currentTimeMillis(), 0L);
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
    public AlgBuildTask getLatestTaskByAlgId(String algId) {
        if (!StringUtils.hasText(algId)) {
            return null;
        }
        Query q = new Query(Criteria.where("algId").is(algId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(1);
        List<AlgBuildTask> tasks = mongoTemplate.find(q, AlgBuildTask.class, COLLECTION);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    @Override
    public void updateContainerName(String taskId, String containerName) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(containerName)) {
            return;
        }
        Query q = new Query(Criteria.where("_id").is(taskId));
        Update u = new Update().set("containerName", containerName.trim());
        mongoTemplate.updateFirst(q, u, COLLECTION);
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
                updateState(taskId, "SUCCESS", "OK", "", "CHECK_PASSED", Collections.emptyList(), 0L, System.currentTimeMillis());
            } else {
                updateState(taskId, "FAILED", "ALG_BUILD_FAILED", "构建或启动失败，请查看日志",
                        "FAILED", defaultFixHints(task, "ALG_BUILD_FAILED"), 0L, System.currentTimeMillis());
            }
        } catch (Exception ex) {
            try {
                appendLog(logPath, "exception=" + ex.getMessage());
            } catch (Exception ignored) {
            }
            updateState(taskId, "FAILED", "ALG_BUILD_FAILED", ex.getMessage(),
                    "FAILED", defaultFixHints(task, "ALG_BUILD_FAILED"), 0L, System.currentTimeMillis());
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

    private void updateState(String taskId, String status, String errorCode, String errorMessage, String phase,
                             List<String> fixHints, long startedAt, long finishedAt) {
        Query q = new Query(Criteria.where("_id").is(taskId));
        Update u = new Update()
                .set("status", status)
                .set("errorCode", errorCode)
                .set("errorMessage", errorMessage == null ? "" : errorMessage)
                .set("phase", phase == null ? "" : phase);
        if (fixHints != null) {
            u.set("fixHints", fixHints);
        }
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

    private String buildContainerName(String serviceName, String algId) {
        String serviceToken = safeToken(StringUtils.hasText(serviceName) ? serviceName.toLowerCase(Locale.ROOT) : "unknown");
        String idToken = safeToken(algId);
        String shortId = idToken.length() <= 8 ? idToken : idToken.substring(0, 8);
        return "c_alg_" + serviceToken + "_" + shortId;
    }

    private void appendLog(Path path, String line) throws IOException {
        String output = "[" + System.currentTimeMillis() + "] " + line + System.lineSeparator();
        Files.write(path, output.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private ValidationResult validateSourcePackage(AlgInfo algInfo, Path zipPath) throws IOException {
        ValidationResult result = new ValidationResult();
        result.runtimeType = normalizeRuntime(algInfo == null ? null : algInfo.getRuntimeType());
        result.algServiceName = trimOrEmpty(algInfo == null ? null : algInfo.getServiceName());
        if (!Files.exists(zipPath)) {
            result.errors.add("源码包不存在");
            return result;
        }

        try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeEntryName(entry.getName());
                result.entries.add(name);
                if (name.endsWith("exphlp-alg.json") && result.metaEntryName.isEmpty()) {
                    result.metaEntryName = name;
                    byte[] bytes = zipFile.getInputStream(entry).readAllBytes();
                    result.metaNode = objectMapper.readTree(new ByteArrayInputStream(bytes));
                }
            }
        }

        if (result.metaNode == null) {
            result.errors.add("缺少 exphlp-alg.json");
            result.hints.add("请在源码包根目录提供 exphlp-alg.json");
            return result;
        }

        String metaRuntime = trimOrEmpty(readText(result.metaNode, "runtimeType")).toLowerCase(Locale.ROOT);
        String metaServiceName = trimOrEmpty(readText(result.metaNode, "serviceName"));
        result.metaRuntimeType = metaRuntime;
        result.metaServiceName = metaServiceName;
        result.metaPort = readInt(result.metaNode, "port", 18090);
        result.metaEntry = trimOrEmpty(readText(result.metaNode, "entry"));

        if (!metaRuntime.isEmpty() && !metaRuntime.equals(result.runtimeType)) {
            result.errors.add("runtimeType不一致: 算法库=" + result.runtimeType + "，源码包=" + metaRuntime);
            result.hints.add("请统一算法库运行时和 exphlp-alg.json 的 runtimeType");
        }
        if (!metaServiceName.isEmpty() && !result.algServiceName.isEmpty() && !metaServiceName.equals(result.algServiceName)) {
            result.errors.add("serviceName不一致: 算法库=" + result.algServiceName + "，源码包=" + metaServiceName);
            result.hints.add("请统一算法库服务名和 exphlp-alg.json 的 serviceName");
        }

        if ("java".equals(result.runtimeType)) {
            if (!existsAny(result.entries, "pom.xml")) {
                result.errors.add("Java项目缺少 pom.xml");
                result.hints.add("请上传标准 Maven 工程源码包，并包含 pom.xml");
            }
        } else {
            if (!existsAny(result.entries, "requirements.txt") && !existsAny(result.entries, "pyproject.toml")) {
                result.errors.add("Python项目缺少 requirements.txt 或 pyproject.toml");
                result.hints.add("请补充依赖定义文件，确保容器构建可安装依赖");
            }
            if (result.metaEntry.isEmpty()) {
                result.hints.add("建议在 exphlp-alg.json 中配置 entry（例如 main:app）");
            }
        }
        if (result.metaPort <= 0 || result.metaPort > 65535) {
            result.errors.add("端口配置非法: " + result.metaPort);
            result.hints.add("请在 exphlp-alg.json 中将 port 设置为 1-65535 的整数");
        }
        return result;
    }

    private List<String> defaultFixHints(AlgBuildTask task, String reasonCode) {
        List<String> hints = new ArrayList<>();
        if ("ALG_BUILD_FAILED".equals(reasonCode)) {
            hints.add("查看 build.log 末尾日志，定位构建失败阶段");
            hints.add("确认 Docker 可用且网络可访问 Maven/PIP 依赖源");
            hints.add("确认算法服务名与 Nacos 注册名一致");
            if (task != null && "python".equalsIgnoreCase(task.getRuntimeType())) {
                hints.add("Python 工程请确认 requirements.txt/pyproject.toml 可安装");
            }
        }
        return hints;
    }

    private String readText(JsonNode node, String key) {
        if (node == null || key == null || key.isEmpty()) {
            return "";
        }
        JsonNode value = node.get(key);
        return value == null ? "" : value.asText("");
    }

    private int readInt(JsonNode node, String key, int defaultValue) {
        if (node == null || key == null || key.isEmpty()) {
            return defaultValue;
        }
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt(defaultValue);
        }
        try {
            return Integer.parseInt(value.asText(String.valueOf(defaultValue)).trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private boolean existsAny(List<String> entries, String suffix) {
        if (entries == null || suffix == null || suffix.isEmpty()) {
            return false;
        }
        String normalized = suffix.toLowerCase(Locale.ROOT);
        for (String name : entries) {
            if (name != null && name.toLowerCase(Locale.ROOT).endsWith(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeEntryName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("\\", "/");
    }

    private String trimOrEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private static final class ValidationResult {
        private final List<String> entries = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<String> hints = new ArrayList<>();
        private String metaEntryName = "";
        private JsonNode metaNode;
        private String runtimeType = "java";
        private String algServiceName = "";
        private String metaRuntimeType = "";
        private String metaServiceName = "";
        private int metaPort = 18090;
        private String metaEntry = "";

        List<String> getErrors() {
            return errors;
        }

        List<String> buildFixHints() {
            if (hints.isEmpty()) {
                return Collections.singletonList("请检查源码包结构与 exphlp-alg.json 配置");
            }
            return hints;
        }

        Map<String, Object> toContractCheck() {
            Map<String, Object> map = new HashMap<>();
            map.put("metaEntryName", metaEntryName);
            map.put("runtimeType", runtimeType);
            map.put("algServiceName", algServiceName);
            map.put("metaRuntimeType", metaRuntimeType);
            map.put("metaServiceName", metaServiceName);
            map.put("metaPort", metaPort);
            map.put("metaEntry", metaEntry);
            map.put("errors", errors);
            return map;
        }
    }
}
