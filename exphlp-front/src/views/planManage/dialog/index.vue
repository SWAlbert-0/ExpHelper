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
    <VueForm
      :form-data="form_data"
      :form-item="form_item"
      :form-handler="form_handler"
    >
    </VueForm>
  </el-dialog>
</template>

<script>
// 组件
import VueForm from "../../../components/form/index.vue";
export default {
  name: "",
  components: {
    VueForm
  },
  props: {
    flagVisible: {
      type: Boolean,
      default: false
    },
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
        { type: "Select", label: "执行状态", prop: "status", options: [] },
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
      options: [
        {
          label: "未执行",
          value: "未执行"
        },
        {
          label: "进行中",
          value: "进行中"
        },
        {
          label: "正常结束",
          value: "正常结束"
        },
        {
          label: "异常结束",
          value: "异常结束"
        }
      ],
      title: ""
    };
  },
  watch: {},
  mounted() {
    const data = this.form_item.filter(item => item.type == "Select");
    console.log(data);
    data[0].options = this.options;
  },
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
    }
  }
};
</script>
<style lang="scss" scoped></style>
