import { exportPlanLogs, getPlanLogs } from "@/api/exphlp/exePlanMgr";
import { listNotifications, resendByExecution, resendNotification } from "@/api/exphlp/notification";

export const planLogMethods = {
  openPlanLogsDialog() {
    this.dialogPlanLogsVisible = true;
    this.planLogs = [];
    this.planLogAfterSeq = 0;
    this.planLogExecutionId = this.showedExePlan.executionId || "";
    this.planLogScope = "latest";
    this.notificationLogs = [];
    this.notificationTotal = 0;
    this.fetchPlanLogs(true);
    this.fetchNotificationLogs();
    if (this.showedExePlan.exeState === "执行中") {
      this.startPlanLogPolling();
    }
  },

  closePlanLogsDialog(done) {
    this.stopPlanLogPolling();
    this.dialogPlanLogsVisible = false;
    this.notificationLogs = [];
    this.notificationTotal = 0;
    if (done) {
      done();
    }
  },

  startPlanLogPolling() {
    this.stopPlanLogPolling();
    this.planLogTimer = setInterval(() => {
      this.fetchPlanLogs(false);
    }, 2000);
  },

  stopPlanLogPolling() {
    if (this.planLogTimer) {
      clearInterval(this.planLogTimer);
      this.planLogTimer = null;
    }
  },

  fetchPlanLogs(reset) {
    if (!this.exePlanId) {
      return;
    }
    const afterSeq = reset ? 0 : this.planLogAfterSeq;
    getPlanLogs(this.exePlanId, afterSeq, 200, this.planLogExecutionId, this.planLogScope).then(res => {
      const data = res && res.data ? res.data : {};
      const items = data.items || [];
      if (data.executionId !== undefined && data.executionId !== null) {
        this.planLogExecutionId = data.executionId;
      }
      if (reset) {
        this.planLogs = items;
      } else if (items.length > 0) {
        this.planLogs = this.planLogs.concat(items);
      }
      if (typeof data.nextSeq === "number") {
        this.planLogAfterSeq = data.nextSeq;
      } else if (items.length > 0) {
        this.planLogAfterSeq = items[items.length - 1].seq;
      }
      if (typeof data.planState === "number") {
        const stateIndex = data.planState - 1;
        if (stateIndex >= 0 && stateIndex < this.options.length) {
          this.showedExePlan.exeState = this.options[stateIndex].value;
        }
      }
      if (data.lastError !== undefined) {
        this.showedExePlan.lastError = data.lastError || "";
      }
      if (this.showedExePlan.exeState !== "执行中") {
        this.stopPlanLogPolling();
      }
      this.fetchNotificationLogs();
    });
  },

  switchPlanLogScope() {
    this.planLogs = [];
    this.planLogAfterSeq = 0;
    this.fetchPlanLogs(true);
    if (this.showedExePlan.exeState === "执行中" && this.planLogScope === "latest") {
      this.startPlanLogPolling();
    } else {
      this.stopPlanLogPolling();
    }
  },

  exportPlanLogsAs(format) {
    if (!this.exePlanId) {
      return;
    }
    exportPlanLogs(this.exePlanId, this.planLogExecutionId, this.planLogScope, 10000).then(res => {
      const data = res && res.data ? res.data : {};
      const items = data.items || [];
      const scope = data.scope || this.planLogScope;
      const executionId = data.executionId || this.planLogExecutionId || "all";
      if (format === "json") {
        const content = JSON.stringify(items, null, 2);
        this.downloadTextFile(`plan-logs-${this.exePlanId}-${scope}-${executionId}.json`, content, "application/json;charset=utf-8");
        return;
      }
      const header = ["seq", "time", "level", "stage", "executionId", "algId", "runIndex", "probInstId", "message", "details"];
      const lines = [header.join(",")];
      for (let i = 0; i < items.length; i++) {
        const row = items[i] || {};
        const values = [
          row.seq,
          this.formatTimestampToDateTime(row.ts),
          row.level,
          row.stage,
          row.executionId,
          row.algId,
          row.runIndex,
          row.probInstId,
          row.message,
          row.details,
        ].map((v) => {
          const text = v === undefined || v === null ? "" : String(v);
          return `"${text.replace(/"/g, '""')}"`;
        });
        lines.push(values.join(","));
      }
      this.downloadTextFile(`plan-logs-${this.exePlanId}-${scope}-${executionId}.csv`, lines.join("\n"), "text/csv;charset=utf-8");
    });
  },

  fetchNotificationLogs() {
    if (!this.exePlanId) {
      return;
    }
    this.notificationLoading = true;
    listNotifications({
      planId: this.exePlanId,
      executionId: this.planLogExecutionId || "",
      pageNum: 1,
      pageSize: 100,
    }).then((res) => {
      const data = (res && res.data) || {};
      this.notificationLogs = data.items || [];
      this.notificationTotal = data.total || 0;
    }).finally(() => {
      this.notificationLoading = false;
    });
  },

  resendOneNotification(row) {
    if (!row || !row.notificationId) {
      return;
    }
    resendNotification(row.notificationId).then(() => {
      this.$message({ type: "success", message: "补发任务已创建" });
      this.fetchNotificationLogs();
      this.fetchPlanLogs(true);
    });
  },

  resendFailedByExecution() {
    if (!this.exePlanId || !this.planLogExecutionId) {
      this.$message({ type: "warning", message: "缺少执行批次，无法补发" });
      return;
    }
    resendByExecution(this.exePlanId, this.planLogExecutionId).then((res) => {
      const count = res && res.data ? (res.data.createdCount || 0) : 0;
      this.$message({ type: "success", message: `补发任务创建完成，共${count}条` });
      this.fetchNotificationLogs();
      this.fetchPlanLogs(true);
    });
  },

  downloadTextFile(filename, content, mimeType) {
    const blob = new Blob([content], { type: mimeType || "text/plain;charset=utf-8" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};
