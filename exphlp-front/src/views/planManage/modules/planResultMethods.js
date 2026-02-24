import { getExeResult, getExeResultDetail } from "@/api/exphlp/algResultMgr";

export const planResultMethods = {
  showExeResults(scope) {
    this.getExeResults(scope);
    this.dialogViewExeResultsVisible = true;
  },

  getExeResults(scope) {
    getExeResult(scope.algId, this.exePlanId).then(res => {
      this.exeResultsTable = res;
      return getExeResultDetail(scope.algId, this.exePlanId).catch((error) => {
        if (error && error.response && error.response.status === 404) {
          return null;
        }
        throw error;
      });
    }).then((detailRes) => {
      if (detailRes && detailRes.data) {
        this.exeResultDetail = detailRes.data;
        return;
      }
      if (this.exeResultsTable && this.exeResultsTable.length > 0) {
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
      } else {
        this.exeResultDetail = {
          status: "MISSING",
          reasonCode: "RESULT_ENDPOINT_404",
          message: "执行结果接口不可用，请升级后端容器",
          metricVersion: "",
          runs: [],
          aggregate: {},
        };
        this.exeResultsTable = [];
      }
    }).catch(() => {
      this.exeResultDetail = {
        status: "MISSING",
        reasonCode: "RESULT_ENDPOINT_404",
        message: "执行结果接口不可用，请升级后端容器",
        metricVersion: "",
        runs: [],
        aggregate: {},
      };
      this.exeResultsTable = [];
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

