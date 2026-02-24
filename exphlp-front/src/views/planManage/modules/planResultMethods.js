import { getExeResult, getExeResultDetail } from "@/api/exphlp/algResultMgr";

export const planResultMethods = {
  showExeResults(scope) {
    this.getExeResults(scope);
    this.dialogViewExeResultsVisible = true;
  },

  getExeResults(scope) {
    const planId = this.exePlanId;
    const algId = scope.algId;
    const algName = scope.algName;
    getExeResult(planId, algId, algName).then(res => {
      this.exeResultsTable = Array.isArray(res) ? res : [];
      return getExeResultDetail(planId, algId).catch((error) => {
        if (error && error.response && error.response.status === 404) {
          return null;
        }
        throw error;
      });
    }).then((detailRes) => {
      if (detailRes && detailRes.data) {
        const detail = detailRes.data || {};
        const runs = Array.isArray(detail.runs) ? detail.runs.map((item, idx) => {
          return {
            ...item,
            runIndex: item && item.runIndex ? item.runIndex : (idx + 1),
          };
        }) : [];
        this.exeResultDetail = {
          ...detail,
          runs,
        };
        this.exeResultsTable = runs;
        if ((detail.status === "SUCCESS" || detail.status === "PARTIAL") && runs.length === 0) {
          this.$message({
            type: "warning",
            message: "结果已生成，但没有可展示的 run 指标明细，请检查算法输出字段",
          });
        }
        return;
      }
      if (this.exeResultsTable.length > 0) {
        this.exeResultDetail = {
          status: "DONE",
          reasonCode: "LEGACY_RESULT_ENDPOINT",
          message: "当前后端未提供详细结果接口，已降级展示基础结果，建议升级 webapp 镜像",
          metricVersion: "-",
          runs: this.exeResultsTable,
          aggregate: { runCount: this.exeResultsTable.length },
        };
        this.$message({
          type: "warning",
          message: "执行结果接口版本较旧，已自动降级展示基础结果",
        });
        return;
      }
      this.exeResultDetail = {
        status: "MISSING",
        reasonCode: "RESULT_ENDPOINT_404",
        message: "执行结果接口不可用，请升级后端容器",
        metricVersion: "",
        runs: [],
        aggregate: {},
      };
      this.exeResultsTable = [];
    }).catch((error) => {
      const status = error && error.response ? error.response.status : "";
      const data = error && error.response ? error.response.data : null;
      const traceId = data && data.traceId ? data.traceId : "";
      const msg = data && data.msg ? data.msg : "执行结果查询失败";
      this.exeResultDetail = {
        status: "MISSING",
        reasonCode: data && data.errorCode ? data.errorCode : "RESULT_QUERY_FAILED",
        message: traceId ? `${msg}（traceId: ${traceId}）` : msg,
        metricVersion: "",
        runs: [],
        aggregate: {},
      };
      this.exeResultsTable = [];
      if (status === 404) {
        this.exeResultDetail.reasonCode = "RESULT_ENDPOINT_404";
      }
    });
  },

  formatMetric(value) {
    if (value === undefined || value === null || value === "") {
      return "-";
    }
    const number = Number(value);
    if (Number.isNaN(number)) {
      return String(value);
    }
    return number.toFixed(6);
  },
};
