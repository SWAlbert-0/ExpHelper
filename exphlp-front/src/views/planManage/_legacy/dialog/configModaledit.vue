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
    <el-form :inline="true" class="demo-form-inline">
      <el-row>
        <el-col :span="8">
          <el-form-item label="算法名称">
            {{ "冒泡排序" }}
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <!-- 表格数据 -->
    <TabalData ref="table" :config="table_config">
      <!--操作-->
      <template #operation="slotData">
        <el-button @click="editTable(slotData.data)">编辑</el-button>
      </template>
      <template #value="slotData">
        <el-input
          v-if="slotData.data.editable"
          v-model="slotData.data.value"
          style="margin: -5px 0"
          :value="slotData.data.value"
        />

        <template v-else>{{ slotData.data.value }}</template>
      </template>
    </TabalData>
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
      num: "",
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
            type: "slot",
            slotName: "value"
          },
          {
            label: "备注",
            prop: "content",
            type: "content"
          },
          {
            label: "操作",
            type: "slot",
            width: 250,
            slotName: "operation"
          }
        ],
        table_data: [],

        checkbox: false,
        pagination: false
      },
      editData: [
        {
          name: "popuSize",
          type: "int",
          value: "10",
          content: "种群大小",
          editable: false
        },
        {
          name: "neigSize",
          type: "int",
          value: "3",
          content: "邻居数量",
          editable: false
        }
      ],
      editData1: [
        {
          name: "popuSize",
          type: "int",
          value: "10",
          content: "种群大小",
          editable: false
        },
        {
          name: "indvRatio",
          type: "double",
          value: "0.5",
          content: "个体学习率",
          editable: false
        },
        {
          name: "popuRatio",
          type: "double",
          value: "0.5",
          content: "群体学习率",
          editable: false
        }
      ],
      activeName: "first",

      title: ""
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
    edit(value, key) {
      console.log(value);
      this.getKey = "";
      this.dialogVisible = true;
      this.title = "查看参数配置";
      for (const key in value) {
        this.form_data[key] = value[key];
      }
      this.table_config.table_data =
        value.name == "bFOA" ? this.editData : this.editData1;
    },
    detail(key, value) {
      this.dialogVisible = true;
      this.title = "查看详情";
      console.log(key, value);
      this.getKey = key;
      this.getDetail = value;
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
    },
    save() {
      this.dialogVisible = false;
    },
    editTable(record) {
      console.log(record);
      this.oldData = JSON.parse(JSON.stringify(record.name));

      // return false;
      const newData = [...this.table_config.table_data];
      console.log(newData);
      const target = newData.filter(item => record.name === item.name)[0];
      console.log("target", target);
      if (target) {
        target.editable = !target.editable;
        this.table_config.table_data = newData;
      }
    }
  }
};
</script>
<style lang="scss" scoped></style>
