<template>
  <el-dialog
    title="执行向导"
    :visible.sync="visibleInner"
    width="760px"
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-alert
      title="最短路径：选择问题实例 -> 选择算法 -> 环境检查 -> 一键执行"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    />
    <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 16px;">
      <el-step title="问题实例" />
      <el-step title="算法选择" />
      <el-step title="环境检查" />
      <el-step title="执行计划" />
    </el-steps>

    <div v-show="step === 0">
      <el-form label-width="110px">
        <el-form-item label="问题实例">
          <el-select v-model="selectedProbId" placeholder="请选择问题实例" style="width: 100%;">
            <el-option v-for="item in probInsts" :key="item.instId" :label="item.instName" :value="item.instId" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-show="step === 1">
      <el-form label-width="110px">
        <el-form-item label="算法">
          <el-select v-model="selectedAlgId" placeholder="请选择算法" style="width: 100%;" @change="onAlgChange">
            <el-option v-for="item in algInfos" :key="item.algId" :label="item.algName" :value="item.algId" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务名">
          <el-input v-model="selectedServiceName" placeholder="将用于执行前检查" />
        </el-form-item>
      </el-form>
    </div>

    <div v-show="step === 2">
      <el-button type="primary" icon="el-icon-refresh" :loading="checkLoading" @click="runWizardCheck">执行检查</el-button>
      <el-alert
        v-if="checkResult"
        :title="checkResult.pass ? '检查通过，可以执行计划' : '检查未通过，请先修复'"
        :type="checkResult.pass ? 'success' : 'error'"
        :closable="false"
        show-icon
        style="margin-top: 12px;"
      />
      <el-table v-if="checkItems.length > 0" :data="checkItems" border style="margin-top: 12px;">
        <el-table-column prop="algName" label="算法" width="160" />
        <el-table-column prop="serviceName" label="服务名" width="180" />
        <el-table-column prop="instanceCount" label="实例数" width="100" />
        <el-table-column prop="diagnosis" label="诊断" />
        <el-table-column prop="suggestion" label="建议" />
      </el-table>
      <el-button
        type="text"
        style="margin-top: 10px;"
        :disabled="!selectedAlgId"
        @click="loadDeployTemplate"
      >查看 Docker 部署模板</el-button>
      <el-input
        v-if="deployTemplate"
        type="textarea"
        :rows="10"
        readonly
        :value="deployTemplate"
        style="margin-top: 8px;"
      />
    </div>

    <div v-show="step === 3">
      <el-form label-width="110px">
        <el-form-item label="计划名称">
          <el-input v-model="planName" />
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input v-model="planDesc" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert
        title="点击“一键创建并执行”后将自动创建计划并立即执行。若失败可在执行计划日志查看异常原因。"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>

    <span slot="footer" class="dialog-footer">
      <el-button @click="onClose">取消</el-button>
      <el-button v-if="step > 0" @click="step = step - 1">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="goNext">下一步</el-button>
      <el-button v-if="step === 3" type="success" :loading="runLoading" @click="createAndRun">一键创建并执行</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addExePlan, execute, getExePlanByName, wizardPrecheck } from "@/api/exphlp/exePlanMgr";
import { generateDeployTemplate } from "@/api/exphlp/algLibMgr";

export default {
  name: "ExecutionWizard",
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
      step: 0,
      selectedProbId: "",
      selectedAlgId: "",
      selectedServiceName: "",
      checkLoading: false,
      checkResult: null,
      checkItems: [],
      deployTemplate: "",
      runLoading: false,
      planName: "",
      planDesc: "",
    };
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        this.visibleInner = v;
        if (v) {
          this.resetWizard();
        }
      },
    },
  },
  methods: {
    resetWizard() {
      const ts = new Date().getTime();
      this.step = 0;
      this.selectedProbId = "";
      this.selectedAlgId = "";
      this.selectedServiceName = "";
      this.checkLoading = false;
      this.checkResult = null;
      this.checkItems = [];
      this.deployTemplate = "";
      this.runLoading = false;
      this.planName = `plan-wizard-${ts}`;
      this.planDesc = "通过执行向导创建";
    },
    onClose() {
      this.visibleInner = false;
      this.$emit("update:visible", false);
      this.$emit("closed");
    },
    onAlgChange(algId) {
      const found = this.algInfos.find((item) => item.algId === algId);
      this.selectedServiceName = found && found.serviceName ? found.serviceName : "";
    },
    goNext() {
      if (this.step === 0 && !this.selectedProbId) {
        this.$message({ type: "warning", message: "请先选择问题实例" });
        return;
      }
      if (this.step === 1 && !this.selectedAlgId) {
        this.$message({ type: "warning", message: "请先选择算法" });
        return;
      }
      if (this.step === 2 && !(this.checkResult && this.checkResult.pass)) {
        this.$message({ type: "warning", message: "请先完成检查并确保通过" });
        return;
      }
      this.step = this.step + 1;
    },
    runWizardCheck() {
      this.checkLoading = true;
      this.checkResult = null;
      this.checkItems = [];
      wizardPrecheck({
        probInstId: this.selectedProbId,
        algId: this.selectedAlgId,
        serviceName: this.selectedServiceName,
      }).then((res) => {
        const data = res && res.data ? res.data : {};
        this.checkResult = data;
        this.checkItems = data.items || [];
      }).catch((error) => {
        if (error && error.response && error.response.status === 404) {
          this.$alert(
            "当前后端版本未包含 /api/ExePlanController/wizardPrecheck 接口。请重建并启动最新 webapp 容器后重试。",
            "接口未就绪",
            { type: "warning", confirmButtonText: "知道了" }
          );
          return;
        }
        const payload = error && error.response && error.response.data ? error.response.data : {};
        const data = payload.data || {};
        this.checkResult = data;
        this.checkItems = data.items || [];
        this.$message({ type: "error", message: payload.msg || "执行前检查失败" });
      }).finally(() => {
        this.checkLoading = false;
      });
    },
    loadDeployTemplate() {
      generateDeployTemplate(this.selectedAlgId).then((res) => {
        const data = res && res.data ? res.data : {};
        const compose = data.composeYaml || "";
        const env = data.envTemplate || "";
        const cmd = data.runCommand || "";
        const verify = data.verifyCommand || "";
        this.deployTemplate = [
          "# docker-compose.algorithm.yml",
          compose,
          "",
          "# .env.algorithm",
          env,
          "",
          "# 启动命令",
          cmd,
          "",
          "# 校验命令",
          verify,
        ].join("\n");
      });
    },
    createAndRun() {
      if (!this.planName || !this.planName.trim()) {
        this.$message({ type: "warning", message: "计划名称不能为空" });
        return;
      }
      const alg = this.algInfos.find((item) => item.algId === this.selectedAlgId);
      if (!alg) {
        this.$message({ type: "warning", message: "算法信息不存在，请重新选择" });
        return;
      }
      const payload = {
        planName: this.planName.trim(),
        probInstIds: [this.selectedProbId],
        userIds: [],
        exeStartTime: 0,
        exeEndTime: 0,
        exeState: 1,
        description: this.planDesc || "通过执行向导创建",
        algRunInfos: [
          {
            algRunInfoId: "1",
            algId: alg.algId,
            algName: alg.algName,
            serviceName: this.selectedServiceName,
            runNum: 1,
            runParas: alg.defParas || [],
          },
        ],
      };
      this.runLoading = true;
      let planId = "";
      addExePlan(payload).then((res) => {
        planId = res;
        if (!planId || typeof planId !== "string") {
          return getExePlanByName(payload.planName).then((plan) => {
            planId = plan && plan.planId ? plan.planId : "";
          });
        }
        return null;
      }).then(() => {
        if (!planId) {
          throw new Error("创建计划失败");
        }
        return execute(planId);
      }).then(() => {
        this.$message({ type: "success", message: "计划已创建并提交执行" });
        this.$emit("refresh");
        this.onClose();
      }).catch((error) => {
        const message = error && error.response && error.response.data && error.response.data.msg
          ? error.response.data.msg : (error && error.message ? error.message : "创建或执行失败");
        this.$message({ type: "error", message });
      }).finally(() => {
        this.runLoading = false;
      });
    },
  },
};
</script>
