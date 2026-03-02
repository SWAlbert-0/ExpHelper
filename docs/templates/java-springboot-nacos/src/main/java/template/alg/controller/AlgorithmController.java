package template.alg.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/myAlg")
public class AlgorithmController {

    @GetMapping("/")
    public ResponseEntity<?> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("service", "java-template");
        return ResponseEntity.ok(data);
    }

    @PostMapping("/")
    public ResponseEntity<?> run(@RequestBody(required = false) Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("reasonCode", "OK");
        result.put("message", "模板算法执行成功，请替换为你的真实算法实现");
        result.put("inputEcho", input == null ? new HashMap<>() : input);
        return ResponseEntity.ok(result);
    }
}
