import { execute, preCheck } from "@/api/exphlp/exePlanMgr";

export const planExecutionMethods = {
  doExePlan(scope) {
    if (scope.exeState !== "未执行") {
      return;
    }
    this.executePlan(scope, false);
  },

  reExecutePlan(scope) {
    if (scope.exeState !== "异常结束") {
      return;
    }
    this.$confirm("重新执行将覆盖当前计划的同名结果展示，是否继续?", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(() => {
      this.executePlan(scope, true);
    }).catch(() => {
      this.$message({ type: "info", message: "已取消重新执行" });
    });
  },

  executePlan(scope, isRetry) {
    preCheck(scope.planId).then(() => {
      return execute(scope.planId);
    }).then(res => {
      const data = res && res.data ? res.data : {};
      if (data.accepted) {
        scope.exeState = "执行中";
        scope.lastError = "";
        this.$message({ type: "success", message: isRetry ? "计划重新执行中" : "计划执行中" });
        if (this.showedExePlan.planId && this.showedExePlan.planId === scope.planId) {
          this.showedExePlan.exeState = "执行中";
          this.showedExePlan.lastError = "";
        }
      } else {
        const reason = data.lastError ? ("，原因: " + data.lastError) : "";
        const msg = ((res && res.msg) ? res.msg : (isRetry ? "计划重新执行失败" : "计划执行失败")) + reason;
        this.$message({ type: "warning", message: msg });
      }
      this.getExePlans();
    }).catch((error) => {
      const check = error && error.response && error.response.data ? error.response.data : null;
      if (check && (check.errorCode === "ALG_SERVICE_NO_INSTANCE" || check.errorCode === "ALG_SERVICE_NAME_EMPTY" || check.errorCode === "NACOS_UNREACHABLE")) {
        this.showPreCheckFailure(check);
        this.getExePlans();
        return;
      }
      this.$message({
        type: "error",
        message: isRetry ? "计划重新执行失败，请稍后重试" : "计划执行失败，请稍后重试",
      });
    });
  },

  // 执行前检查失败时，直接给用户下一步动作，减少阅读外部文档成本。
  showPreCheckFailure(resp) {
    const data = resp && resp.data ? resp.data : {};
    const items = data && data.items ? data.items : [];
    const first = items.length > 0 ? items[0] : null;
    const msg = data && data.message ? data.message : "执行前检查失败";
    const suggestion = first && first.suggestion ? first.suggestion : "";
    const diagnosis = first && first.diagnosis ? first.diagnosis : "";
    const code = resp && resp.errorCode ? resp.errorCode : "";
    const quickStart = (code === "ALG_SERVICE_NO_INSTANCE" || code === "NACOS_UNREACHABLE")
      ? "可先执行：powershell -ExecutionPolicy Bypass -File docs/examples/moo-nsga2-zdt1/scripts/start-alg-with-nacos.ps1"
      : "";
    const detail = [msg, diagnosis, suggestion, quickStart].filter(Boolean).join("；");
    this.$alert(detail, "执行前检查未通过", {
      confirmButtonText: "知道了",
      type: "warning",
    });
  },

  runPreCheckFromLogs() {
    if (!this.showedExePlan || !this.showedExePlan.planId) {
      return;
    }
    preCheck(this.showedExePlan.planId).then(() => {
      this.$message({ type: "success", message: "执行前检查通过" });
    }).catch((error) => {
      const payload = error && error.response && error.response.data ? error.response.data : null;
      if (payload) {
        this.showPreCheckFailure(payload);
        return;
      }
      this.$message({ type: "error", message: "执行前检查失败" });
    });
  },
};

