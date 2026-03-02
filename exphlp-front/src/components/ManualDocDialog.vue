<template>
  <el-dialog
    :title="manual.title || '操作手册'"
    :visible.sync="innerVisible"
    width="68%"
    top="6vh"
    :close-on-click-modal="false"
    @close="onClose"
  >
    <div class="manual-summary">{{ manual.summary }}</div>
    <div class="manual-content-wrap">
      <div
        v-for="(section, idx) in manual.sections"
        :key="`${pageKey}-${idx}`"
        class="manual-section-card"
      >
        <div class="section-title">{{ idx + 1 }}. {{ section.heading }}</div>
        <ol v-if="section.steps && section.steps.length" class="section-step-list">
          <li v-for="(step, stepIdx) in section.steps" :key="stepIdx">{{ step }}</li>
        </ol>
        <div v-if="section.tips && section.tips.length" class="section-block tip-block">
          <div class="block-title">操作提示</div>
          <ul>
            <li v-for="(tip, tipIdx) in section.tips" :key="tipIdx">{{ tip }}</li>
          </ul>
        </div>
        <div v-if="section.warnings && section.warnings.length" class="section-block warning-block">
          <div class="block-title">注意事项</div>
          <ul>
            <li v-for="(warning, warningIdx) in section.warnings" :key="warningIdx">{{ warning }}</li>
          </ul>
        </div>
      </div>
      <el-empty v-if="!manual.sections || manual.sections.length === 0" description="暂无页面说明" />
    </div>
  </el-dialog>
</template>

<script>
import { getPageManual } from "@/manuals/pageManuals";

export default {
  name: "ManualDocDialog",
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    pageKey: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      innerVisible: false,
      manual: { title: "", summary: "", sections: [] },
    };
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.innerVisible = val;
        if (val) {
          this.loadManual();
        }
      }
    },
    innerVisible(val) {
      if (!val && this.visible) {
        this.$emit("update:visible", false);
      }
    },
    pageKey() {
      if (this.innerVisible) {
        this.loadManual();
      }
    }
  },
  methods: {
    onClose() {
      this.$emit("update:visible", false);
    },
    loadManual() {
      this.manual = getPageManual(this.pageKey);
    },
  },
};
</script>

<style lang="less" scoped>
.manual-summary {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f4f8ff;
  color: #355a8a;
}
.manual-content-wrap {
  max-height: 62vh;
  overflow: auto;
  padding-right: 4px;
}
.manual-section-card {
  border: 1px solid #e7edf5;
  border-left: 4px solid #409eff;
  border-radius: 8px;
  padding: 12px 12px 12px 14px;
  margin-bottom: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2e3a53;
  margin-bottom: 10px;
}
.section-step-list {
  margin: 0;
  padding-left: 18px;
  color: #4d5b72;
  line-height: 1.8;
}
.section-block {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 6px;
}
.section-block ul {
  margin: 0;
  padding-left: 16px;
  line-height: 1.7;
}
.block-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}
.tip-block {
  background: #f0f9eb;
  color: #4e6c35;
}
.warning-block {
  background: #fdf6ec;
  color: #a57a2c;
}
</style>
