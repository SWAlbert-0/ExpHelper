<template>
  <div class="plan-list-header-body">
    <el-alert
      class="plan-list-alert"
      title="推荐优先走“执行向导”：选择问题实例 -> 选择算法 -> 执行检查 -> 提交执行。"
      type="info"
      :closable="false"
      show-icon
    />
    <div class="plan-guide-strip">
      <div class="guide-item"><span class="guide-no">1</span><span>创建计划并选择问题实例</span></div>
      <div class="guide-item"><span class="guide-no">2</span><span>添加算法并核对参数</span></div>
      <div class="guide-item"><span class="guide-no">3</span><span>执行检查通过后再执行</span></div>
    </div>
    <el-form :model="localSearch" :inline="true" class="demo-form-inline plan-search-form" align="center">
      <el-row type="flex" class="plan-search-row">
        <el-col :span="6" class="plan-search-col">
          <el-form-item label="计划名称">
            <el-input
              :value="localSearch.planName"
              placeholder="请输入计划名称"
              clearable
              @input="onFieldChange('planName', $event)"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6" class="plan-search-col">
          <el-form-item label="计划状态">
            <el-select
              :value="localSearch.exeState"
              placeholder="请输入计划状态"
              clearable
              @change="onFieldChange('exeState', $event)"
            >
              <el-option
                v-for="selectItem in options"
                :key="selectItem.value"
                :value="selectItem.value"
                :label="selectItem.label"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6" class="plan-search-col">
          <el-form-item label="开始日期">
            <el-date-picker
              :value="localSearch.exeStartTime"
              type="date"
              placeholder="选择开始日期"
              clearable
              @change="onFieldChange('exeStartTime', $event)"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6" class="plan-search-col">
          <el-form-item label="结束日期">
            <el-date-picker
              :value="localSearch.exeEndTime"
              type="date"
              placeholder="选择结束日期"
              clearable
              @change="onFieldChange('exeEndTime', $event)"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-row class="plan-toolbar-row">
      <el-col :span="12">
        <div class="grid-content bg-purple plan-toolbar-left">
          <el-button type="success" icon="el-icon-plus" @click="$emit('add')">添加</el-button>
          <el-button type="primary" icon="el-icon-magic-stick" @click="$emit('open-wizard')">执行向导</el-button>
          <el-button type="warning" icon="el-icon-data-analysis" @click="$emit('open-readiness')">执行前体检</el-button>
          <el-button type="default" icon="el-icon-document" @click="$emit('open-guide')">查看操作手册</el-button>
          <span class="toolbar-split"></span>
          <el-button type="danger" icon="el-icon-delete" @click="$emit('delete-batch')">批量删除</el-button>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="grid-content plan-toolbar-right">
          <el-button type="primary" icon="el-icon-search" @click="$emit('query')">查询</el-button>
          <el-button type="default" icon="el-icon-refresh" @click="$emit('refresh')">刷新</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "PlanListHeader",
  props: {
    search: {
      type: Object,
      required: true,
    },
    options: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      localSearch: {
        planName: "",
        exeState: "",
        exeStartTime: "",
        exeEndTime: "",
      },
    };
  },
  watch: {
    search: {
      immediate: true,
      deep: true,
      handler(val) {
        this.localSearch = {
          planName: val.planName || "",
          exeState: val.exeState || "",
          exeStartTime: val.exeStartTime || "",
          exeEndTime: val.exeEndTime || "",
        };
      },
    },
  },
  methods: {
    onFieldChange(field, value) {
      this.localSearch = {
        ...this.localSearch,
        [field]: value,
      };
      this.$emit("update:search", { ...this.localSearch });
    },
  },
};
</script>

<style scoped lang="scss">
.plan-list-header-body {
  width: 100%;
}

.plan-list-alert {
  margin-bottom: 12px;
}

.plan-guide-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.guide-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e8eef7;
  border-radius: 6px;
  background: #f9fbff;
  color: #4f5d75;
  font-size: 12px;
}

.guide-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.plan-search-form {
  padding: 8px 10px;
  border: 1px solid #f2f6fc;
  border-radius: 6px;
  background: #fafcff;
}

.plan-search-col {
  padding: 0 8px;
}

.plan-search-col ::v-deep .el-form-item {
  margin-bottom: 10px;
}

.plan-search-col ::v-deep .el-form-item__label {
  width: 78px;
  text-align: right;
  padding-right: 10px;
}

.plan-search-col ::v-deep .el-form-item__content {
  width: calc(100% - 88px);
}

.plan-search-col ::v-deep .el-input,
.plan-search-col ::v-deep .el-select,
.plan-search-col ::v-deep .el-date-editor.el-input {
  width: 100%;
}

.plan-toolbar-row {
  margin-top: 12px;
}

.plan-toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-toolbar-right {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.toolbar-split {
  width: 1px;
  height: 24px;
  background: #e4e7ed;
}

@media (max-width: 1400px) {
  .plan-guide-strip {
    grid-template-columns: 1fr;
  }

  .plan-search-row {
    flex-wrap: wrap;
  }

  .plan-toolbar-row .el-col {
    width: 100% !important;
    max-width: 100%;
  }

  .plan-toolbar-right {
    justify-content: flex-start;
    margin-top: 8px;
  }
}
</style>
