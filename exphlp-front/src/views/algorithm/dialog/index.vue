<template>
  <!--dialog 弹窗
    子组件接收父组件的数据，是通过属性接收
  -->
  <el-dialog
    :title.sync="title"
    :visible.sync="dialogVisible"
    class="cars-dialog-center"
    :close-on-click-modal="false"
    append-to-body
    width="40%"
    :modal="false"
    @close="$emit('update:show', false)"
  >
    <div v-if="getKey != 'detail'">
      <VueForm
        :form-data="form_data"
        :form-item="form_item"
        :form-handler="form_handler"
        :form-table="table_config"
      >
      </VueForm>
      <el-dialog
        title="添加参数"
        :visible.sync="outerVisible"
        width="30%"
        :modal="false"
      >
        <VueForm
          :form-data="form_data1"
          :form-item="form_item1"
          :form-handler="form_handler1"
        >
        </VueForm>
      </el-dialog>
    </div>
    <div v-else-if="getKey == '添加'">
      <VueForm :form-data="form_data3" :form-item="form_item3"> </VueForm>
      <!-- 表格数据 -->
      <TabalData ref="table" :config="table_config2"> </TabalData>
    </div>
  </el-dialog>
</template>

<script>

import VueForm from "../../../components/form/index.vue";
import TabalData from "../../../components/tableData";
import { getParaByAlgId } from "@/api/vadmin/alg";
export default {
  name: "",
  components: {
    VueForm,
    TabalData
  },
  props: ["show", "dialogTitle", "rowData"],
  data() {
    return {
      // 弹窗显示/关闭标记
      dialogVisible: this.show,
      outerVisible: this.show,
      getKey: "",
      getDetail: [],
      title: this.dialogTitle,
      form_data: this.rowData,
      form_data3: {
        name: "",
        content: ""
      },
      form_data1: {
        name: "",
        type: "",
        value: ""
      },
      form_item: [
        { type: "Input", label: "算法名称", prop: "algName" },
        { type: "textArea", label: "算法描述", prop: "description", rows: "5" },
        {
          label: "参数",
          type: "button",
          type1: "text",
          handler: () => this.addParams()
        }
      ],
      form_item1: [
        { type: "Input", label: "参数名称", prop: "defParaName" },
        { type: "Select", label: "类型", prop: "defParaType", options: [] },
        { type: "Input", label: "值", prop: "value" }
      ],
      form_item3: [
        { type: "Input", label: "算法名称", prop: "name", disabled: true },
        {
          type: "textArea",
          label: "算法描述",
          prop: "content",
          rows: "5",
          disabled: true
        }
      ],
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
      form_handler1: [
        {
          label: "保存",
          key: "submit",
          type: "primary",
          handler: () => this.submit1()
        },
        {
          label: "重置",
          handler: () => this.reset1()
        }
      ],
      option: [
        {
          value: "int",
          label: "int"
        },
        {
          value: "string",
          label: "string"
        },
        {
          value: "number",
          label: "number"
        },
        {
          value: "boolean",
          label: "boolean"
        },
        {
          value: "float",
          label: "float"
        },
        {
          value: "Object",
          label: "Object"
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
            prop: "defParaName",
            width: 100
          },
          {
            label: "类型",
            prop: "defParaType",
            width: 150
          },
          {
            label: "值",
            prop: "value",
            slotName: "value",
            width: 150
          },
          {
            label: "操作",
            type: "slot",
            width: 150,
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
      ],
      table_config2: {
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
            type: "remark",
            width: 150
          }
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
            type: "string",
            value: "3",
            remark: "邻居数量"
          }
        ],
        checkbox: false,
        pagination: false
      }
    };
  },
  watch: {
    show() {
      this.dialogVisible = this.show;
    },
    dialogTitle() {
      this.title = this.dialogTitle;
    },
    rowData() {
      this.form_data = this.rowData;
      this.getDefPara(this.rowData.algId);
    }
  },
  mounted() {
    const data = this.form_item1.filter(item => item.type == "Select");
    data[0].options = this.option;
  },
  methods: {
    getDefPara(algId) {
      if (this.dialogTitle != "添加") {
        getParaByAlgId(algId).then(res => {
          this.table_config.table_data = res.data;
        });
      }
    },
    opened() {
      //   this.getBrandLogo();
      //   this.getDetailed();
    },

    /** 获取详情 */
    getDetailed() {
      //   this.form_data = this.data;
      //   this.logo_current = this.data.imgUrl;
      //   this.form_data.imgUrl = this.data.imgUrl;
    },
    /** 提交 */
    submit() {
      //   this.data.id ? this.edit() : this.add();
    },
    /** 提交 */
    submit1() {
      const data = JSON.parse(JSON.stringify(this.form_data1));
      this.table_config.table_data.push(data);
      //   this.data.id ? this.edit() : this.add();
      this.outerVisible = false;
      this.reset1();
    },
    /** 添加 */
    add(value) {
      this.getKey = "";
      this.reset();
      this.dialogVisible = true;
      this.title = "添加";
    },
    /** 修改 */
    edit(value) {
      this.getKey = "";
      this.dialogVisible = true;
      this.title = "修改";
      for (const key in value) {
        this.form_data[key] = value[key];
      }
      this.table_config.table_data =
        value.id == 1 ? this.editData : this.editData1;
    },
    detail(key, value) {
      this.dialogVisible = true;
      this.title = "查看详情";
      for (const key in value) {
        this.form_data3[key] = value[key];
      }
      this.getKey = key;
      this.getDetail = value;
    },
    /** 重置表单 */
    reset() {
      for (const key in this.form_data) {
        this.form_data[key] = "";
      }

      this.table_config.table_data = [];
    },
    reset1() {
      for (const key in this.form_data1) {
        this.form_data1[key] = "";
      }
    },
    close() {
      this.table_config.table_data = [];
      this.reset();
      this.dialogVisible = false;
    },
    addParams() {
      this.outerVisible = true;
    }
  }
};
</script>
<style lang="scss" scoped></style>
