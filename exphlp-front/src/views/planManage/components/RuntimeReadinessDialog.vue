<template>
  <el-dialog
    class="runtime-readiness-dialog"
    title="执行前一键体检"
    :visible.sync="visibleInner"
    width="820px"
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-alert
      title="建议先完成体检，再执行计划。体检覆盖：后端版本、通知配置、算法服务连通性。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    />

    <div class="check-panel">
      <div class="panel-title">检查参数</div>
      <el-form :inline="true" label-width="80px" class="check-form">
      <el-form-item label="问题实例">
        <el-select v-model="selectedProbId" placeholder="可选" class="field-select" clearable>
          <el-option
            v-for="item in probInsts"
            :key="item.instId"
            :label="item.instName"
            :value="item.instId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="算法">
        <el-select v-model="selectedAlgId" placeholder="请选择算法" class="field-select" @change="onAlgChange">
          <el-option
            v-for="item in algInfos"
            :key="item.algId"
            :label="item.algName"
            :value="item.algId"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-refresh" :loading="loading" @click="runCheck">执行体检</el-button>
      </el-form-item>
      </el-form>
    </div>

    <div class="check-panel">
      <div class="panel-title">体检结果</div>
      <el-table :data="items" border class="result-table">
      <el-table-column prop="name" label="检查项" width="180" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'PASS' ? 'success' : (scope.row.status === 'WARN' ? 'warning' : 'danger')">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="结果" min-width="260" show-overflow-tooltip />
      <el-table-column prop="suggestion" label="修复建议" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.action" type="text" @click="goFix(scope.row.action)">去处理</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      </el-table>
    </div>

    <div class="summary-area">
      <el-alert
        :title="summaryText"
        :type="summaryType"
        :closable="false"
        show-icon
      />
    </div>

    <span slot="footer" class="dialog-footer">
      <el-button @click="onClose">关闭</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { wizardPrecheck } from "@/api/exphlp/exePlanMgr";
import { getNotifyProfile } from "@/api/exphlp/notification";
import { getHealthz } from "@/api/auth";

export default {
  name: "RuntimeReadinessDialog",
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    probInsts: {
      type: Array,
      default: () => [],
    },
    algInfos: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      visibleInner: false,
      loading: false,
      selectedProbId: "",
      selectedAlgId: "",
      selectedServiceName: "",
      items: [],
    };
  },
  computed: {
    summaryType() {
      if (!this.items.length) return "info";
      const hasFail = this.items.some((item) => item.status === "FAIL");
      const hasWarn = this.items.some((item) => item.status === "WARN");
      if (hasFail) return "error";
      if (hasWarn) return "warning";
      return "success";
    },
    summaryText() {
      if (!this.items.length) return "点击“执行体检”开始检查";
      const pass = this.items.filter((item) => item.status === "PASS").length;
      const warn = this.items.filter((item) => item.status === "WARN").length;
      const fail = this.items.filter((item) => item.status === "FAIL").length;
      return `体检完成：通过 ${pass} 项，警告 ${warn} 项，失败 ${fail} 项`;
    },
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        this.visibleInner = v;
        if (v) {
          this.reset();
        }
      },
    },
  },
  methods: {
    reset() {
      this.loading = false;
      this.items = [];
      this.selectedAlgId = "";
      this.selectedServiceName = "";
      this.selectedProbId = this.probInsts.length ? this.probInsts[0].instId : "";
    },
    onClose() {
      this.visibleInner = false;
      this.$emit("update:visible", false);
    },
    onAlgChange(algId) {
      const found = this.algInfos.find((item) => item.algId === algId);
      this.selectedServiceName = found && found.serviceName ? found.serviceName : "";
    },
    addCheck(item) {
      this.items.push(item);
    },
    runCheck() {
      if (!this.selectedAlgId) {
        this.$message({ type: "warning", message: "请先选择算法" });
        return;
      }
      this.loading = true;
      this.items = [];
      Promise.allSettled([
        getHealthz(),
        getNotifyProfile(),
        wizardPrecheck({
          probInstId: this.selectedProbId,
          algId: this.selectedAlgId,
          serviceName: this.selectedServiceName,
        }),
      ]).then(([healthR, notifyR, precheckR]) => {
        if (healthR.status === "fulfilled") {
          const data = (healthR.value && healthR.value.data) || {};
          this.addCheck({
            name: "后端版本",
            status: "PASS",
            message: `healthz ok, version=${data.artifactVersion || "unknown"}, build=${data.buildTime || "unknown"}`,
            suggestion: "确认 buildTime 与最新重建时间一致",
            action: "",
          });
        } else {
          this.addCheck({
            name: "后端版本",
            status: "FAIL",
            message: "无法访问 /api/auth/healthz",
            suggestion: "重建并启动 webapp/front 容器",
            action: "deploy",
          });
        }

        if (notifyR.status === "fulfilled") {
          const data = (notifyR.value && notifyR.value.data) || {};
          const email = (data.email || "").trim();
          const enabled = data.emailEnabled !== false && data.eventPlanDoneEnabled !== false;
          if (email && enabled) {
            this.addCheck({
              name: "通知配置",
              status: "PASS",
              message: `通知邮箱=${email}`,
              suggestion: "可直接接收执行结束通知",
              action: "",
            });
          } else {
            this.addCheck({
              name: "通知配置",
              status: "WARN",
              message: "通知邮箱为空或通知开关关闭",
              suggestion: "在个人中心->通知设置中补齐邮箱并开启通知",
              action: "notify",
            });
          }
        } else {
          this.addCheck({
            name: "通知配置",
            status: "WARN",
            message: "获取通知配置失败",
            suggestion: "检查登录状态后重试，必要时去通知设置页保存一次",
            action: "notify",
          });
        }

        if (precheckR.status === "fulfilled") {
          const data = (precheckR.value && precheckR.value.data) || {};
          const pass = data.pass === true;
          const badItem = (data.items || []).find((item) => item.instanceCount <= 0);
          if (pass) {
            this.addCheck({
              name: "算法服务连通性",
              status: "PASS",
              message: "执行前检查通过，Nacos中存在可用实例",
              suggestion: "可直接进入执行向导或点击执行",
              action: "",
            });
          } else {
            this.addCheck({
              name: "算法服务连通性",
              status: "FAIL",
              message: badItem ? `服务 ${badItem.serviceName} 无可用实例` : "执行前检查未通过",
              suggestion: "启动算法服务并确保服务名与Nacos注册名一致",
              action: "algorithm",
            });
          }
        } else {
          this.addCheck({
            name: "算法服务连通性",
            status: "FAIL",
            message: "调用执行前检查接口失败",
            suggestion: "确认后端版本已更新，或使用执行向导再次检查",
            action: "wizard",
          });
        }
      }).finally(() => {
        this.loading = false;
      });
    },
    goFix(action) {
      const map = {
        notify: "/profile/index",
        algorithm: "/algorithmConfig/index",
      };
      if (action === "deploy") {
        this.$alert(
          "请在项目根目录执行：\npowershell -ExecutionPolicy Bypass -File scripts/rebuild-webapp-front.ps1",
          "部署修复建议",
          { confirmButtonText: "知道了", type: "warning" }
        );
        return;
      }
      if (action === "wizard") {
        this.$emit("open-wizard");
        return;
      }
      const path = map[action];
      if (path) {
        this.onClose();
        this.$router.push(path);
      }
    },
  },
};
</script>

<style scoped lang="scss">
.runtime-readiness-dialog ::v-deep .el-dialog__body {
  padding-top: 12px;
}

.check-panel {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  margin-top: 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.check-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.field-select {
  width: 260px;
}

.result-table {
  margin-top: 4px;
}

.runtime-readiness-dialog ::v-deep .el-table td,
.runtime-readiness-dialog ::v-deep .el-table th {
  padding: 8px 0;
}

.summary-area {
  margin-top: 12px;
}

@media (max-width: 900px) {
  .field-select {
    width: 220px;
  }
}
</style>
