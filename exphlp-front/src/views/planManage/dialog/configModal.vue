<template>
  <!--dialog 弹窗
    子组件接收父组件的数据，是通过属性接收
  -->
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    class="cars-dialog-center"
    :close-on-click-modal="false"
    append-to-body
    width="40%"
    @close="close"
    @opened="opened"
  >
    <!-- <el-descriptions :column="4">
      <el-descriptions-item label="计划名称">计划示例1</el-descriptions-item>
      <el-descriptions-item label="问题名称">问题示例1</el-descriptions-item>
      <el-descriptions-item label="算法名称">冒泡排序</el-descriptions-item>
      <el-descriptions-item label="运行次数（次）">10</el-descriptions-item>
    </el-descriptions> -->
    <el-form layout="inline">
      <el-row :gutter="10">
        <el-col :md="6">
          <el-form-item label="计划名称:"> 计划示例1 </el-form-item>
        </el-col>
        <el-col :md="6">
          <el-form-item label="问题名称:"> 问题示例1 </el-form-item>
        </el-col>
        <el-col :md="6">
          <el-form-item label="算法名称:"> bFOA </el-form-item>
        </el-col>
        <el-col :md="6">
          <el-form-item label="运行次数（次）:"> 20 </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-tabs v-model="activeName" @tab-click="handleClick">
      <el-tab-pane label="执行配置" name="first">
        <!-- 表格数据 -->
        <TabalData ref="table" :config="table_config">
          <!--操作-->
          <template #operation="slotData">
            <el-button
              @click="showDialog('detail', slotData.data)"
            >查看</el-button>
          </template>
        </TabalData>
      </el-tab-pane>
      <el-tab-pane label="运行结果" name="second">
        <el-button style="float: right; margin-bottom: 5px">导出</el-button>
        <!-- 表格数据 -->
        <TabalData ref="table" :config="table_config1">
          <!--操作-->
          <template #operation="slotData">
            <el-button
              @click="showDialog('detail', slotData.data)"
            >查看</el-button>
          </template>
        </TabalData>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script>
// 组件
import TabalData from "../../../components/tableData";
import configModal from "../dialog/configModal.vue";
export default {
  name: "",
  components: {
    TabalData,
    configModal
  },
  props: {
    data: {
      type: Object,
      defult: () => {}
    }
  },
  data() {
    return {
      // 弹窗显示/关闭标记
      dialogVisible: false,
      outerVisible: false,
      getKey: "",
      getDetail: [],
      form_data: {
        name: "",
        startTime: "",
        endTime: ""
      },
      // 表单项y
      form_item: [
        { type: "Input", label: "计划名称", prop: "name", width: "220px" },
        { type: "date", label: "开始时间", prop: "startTime" },
        { type: "date", label: "结束时间", prop: "endTime" }
      ],

      // 表单按钮
      form_handler: [
        {
          label: "保存",
          key: "submit",
          type: "primary",
          handler: () => this.submit()
        },
        {
          label: "重置",
          handler: () => this.reset()
        }
      ],
      table_config: {
        thead: [
          {
            label: "序号",
            prop: "index",
            type: "index",
            width: 50
          },
          {
            label: "参数名称",
            prop: "name",
            type: "name",
            width: 100
          },
          {
            label: "类型",
            prop: "type",
            type: "type"
          },
          {
            label: "值",
            prop: "value",
            type: "value"
          },
          {
            label: "备注",
            prop: "remark",
            type: "remark"
          }
          // {
          //   label: "操作",
          //   type: "slot",
          //   width: 140,
          //   slotName: "operation"
          // }
        ],
        table_data: [
          {
            name: "popuSize",
            type: "int",
            value: "10",
            remark: "种群大小"
          },
          {
            name: "neigSize",
            type: "int",
            value: "3",
            remark: "邻居数量"
          }
        ],
        checkbox: false,
        pagination: false
      },

      table_config1: {
        thead: [
          {
            label: "运行次数",
            prop: "num",
            type: "num",
            width: 100
          },
          {
            label: "代数",
            prop: "algebra",
            type: "algebra"
          },
          {
            label: "约束值",
            prop: "limit",
            type: "limit"
          },
          {
            label: "目标值",
            prop: "targetValue",
            type: "targetValue"
          },
          {
            label: "耗时（分）",
            prop: "time",
            type: "time"
          }
          // {
          //   label: "操作",
          //   type: "slot",
          //   width: 140,
          //   slotName: "operation"
          // }
        ],
        table_data: [
          {
            num: "1",
            algebra: "1",
            limit: "20",
            targetValue: "50",
            time: "10"
          },
          {
            num: "1",
            algebra: "2",
            limit: "30",
            targetValue: "42",
            time: "20"
          },
          {
            num: "1",
            algebra: "3",
            limit: "24",
            targetValue: "46",
            time: "30"
          },
          {
            num: "2",
            algebra: "1",
            limit: "24",
            targetValue: "46",
            time: "30"
          }
        ],
        checkbox: false,
        pagination: false
      },
      activeName: "first",

      title: "",
      editData: [
        {
          name: "popuSize",
          type: "int",
          value: "10",
          remark: "种群大小",
          editable: false
        },
        {
          name: "neigSize",
          type: "int",
          value: "3",
          remark: "邻居数量",
          editable: false
        }
      ],
      editData1: [
        {
          name: "popuSize",
          type: "int",
          value: "10",
          remark: "种群大小",
          editable: false
        },
        {
          name: "indvRatio",
          type: "double",
          value: "0.5",
          remark: "个体学习率",
          editable: false
        },
        {
          name: "popuRatio",
          type: "double",
          value: "0.5",
          remark: "群体学习率",
          editable: false
        }
      ]
    };
  },
  watch: {},
  mounted() {},
  methods: {
    opened() {},

    /** 获取详情 */
    getDetailed() {},
    /** 提交 */
    submit() {
      this.dialogVisible = false;
    },

    /** 添加 */
    add(value) {
      this.getKey = "";
      this.reset();
      console.log("22222");
      this.dialogVisible = true;
      this.title = "添加";
    },
    /** 修改 */
    edit(value) {
      console.log(value);
      this.getKey = "";
      this.dialogVisible = true;
      this.title = "修改";
      for (const key in value) {
        this.form_data[key] = value[key];
      }
    },
    detail(key, value) {
      this.dialogVisible = true;
      this.title = "查看详情";
      console.log("122111221");
      console.log(key, value);
      this.getKey = key;
      this.getDetail = value;
      this.table_config.table_data =
        value.id == 1 ? this.editData : this.editData1;
    },
    /** 重置表单 */
    reset() {
      for (const key in this.form_data) {
        this.form_data[key] = "";
      }
    },

    close() {
      this.reset();
      // 关闭窗口
      this.dialogVisible = false;
    },
    addParams() {
      this.outerVisible = true;
    },
    handleClick(tab, event) {
      console.log(tab, event);
    }
  }
};
</script>
<style lang="scss" scoped></style>
