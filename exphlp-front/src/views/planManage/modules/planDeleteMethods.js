import { deleteExePlanById } from "@/api/exphlp/exePlanMgr";

export const planDeleteMethods = {
  deleteExePlan(scope) {
    if (typeof this.canDeletePlan === "function" && !this.canDeletePlan(scope)) {
      this.$message({ type: "warning", message: "当前账号仅可删除自己创建的计划" });
      return;
    }
    if (scope.exeState === "执行中") {
      this.$message({ type: "warning", message: "计划执行中，不能删除" });
      return;
    }
    this.$confirm("此操作将永久删除执行计划记录, 是否继续?", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(() => {
      deleteExePlanById(scope.planId).then((res) => {
        const data = res && res.data ? res.data : {};
        if (data.blocked) {
          this.$message({ type: "warning", message: "删除失败，计划执行中" });
          this.getExePlans();
          return;
        }
        if (data.deletedCount > 0) {
          this.$message({ type: "success", message: `删除成功（planId=${scope.planId}）` });
        } else if (data.noop) {
          this.$message({ type: "success", message: `记录已不存在，列表已同步（planId=${scope.planId}）` });
        } else {
          this.$message({ type: "warning", message: "删除未生效，请刷新后重试" });
        }
        this.getExePlans();
      });
    }).catch(() => {
      this.$message({ type: "info", message: "取消删除" });
    });
  },

  deleteBatchExePlan() {
    if (this.multipleSelection1.length === 0) {
      this.$message({
        type: "warning",
        message: "当前未选中任何执行计划",
      });
      return;
    }
    let selected = this.multipleSelection1;
    if (typeof this.canDeletePlan === "function") {
      selected = this.multipleSelection1.filter((item) => this.canDeletePlan(item));
      if (selected.length === 0) {
        this.$message({ type: "warning", message: "仅可批量删除自己创建的计划" });
        return;
      }
    }
    const hasRunningPlan = selected.some((item) => item.exeState === "执行中");
    if (hasRunningPlan) {
      this.$message({ type: "warning", message: "存在执行中的执行计划，请重新选择" });
      return;
    }
    this.$confirm("此操作将永久删除执行计划记录, 是否继续?", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }).then(() => {
      const deleteTasks = selected.map((item) => deleteExePlanById(item.planId));
      Promise.allSettled(deleteTasks).then((results) => {
        let deletedCount = 0;
        let noopCount = 0;
        let blockedCount = 0;
        let failedCount = 0;
        for (let i = 0; i < results.length; i++) {
          const result = results[i];
          if (result.status !== "fulfilled") {
            failedCount += 1;
            continue;
          }
          const data = result.value && result.value.data ? result.value.data : {};
          if (data.blocked) {
            blockedCount += 1;
          } else if (Number(data.deletedCount) > 0) {
            deletedCount += 1;
          } else if (data.noop) {
            noopCount += 1;
          } else {
            failedCount += 1;
          }
        }
        // 批量删除反馈统一统计口径，避免“只弹窗不落地”的误判。
        if (failedCount === 0 && blockedCount === 0) {
          this.$message({ type: "success", message: `删除完成：成功删除 ${deletedCount} 条，同步不存在 ${noopCount} 条` });
        } else {
          this.$message({ type: "warning", message: `批量删除完成：删除 ${deletedCount} 条，同步不存在 ${noopCount} 条，执行中阻止 ${blockedCount} 条，失败 ${failedCount} 条` });
        }
        this.getExePlans();
      });
    }).catch(() => {
      this.$message({
        type: "info",
        message: "取消删除",
      });
    });
  },
};
