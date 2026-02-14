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
    width="30%"
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
        name: ""
      },
      // 表单项y
      form_item: [
        { type: "Input", label: "问题名称", prop: "name", width: "220px" }
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
      // 通过时间戳生成 UUID
      const uuid = Math.round(new Date().getTime()).toString();
      this.getKey.children.push({
        id: uuid,
        label: this.form_data.name
      });
      this.$emit("getVisible");
      console.log(this.getKey);
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
    treeAdd(value) {
      this.dialogVisible = true;
      this.title = "添加问题";
      this.getKey = value;
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
