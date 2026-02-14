<template>
  <div>
    <el-dialog
      :title.sync="title"
      :visible.sync="dialogVisible"
      width="30%"
      @close="$emit('update:show', false)"
    >
      <el-form label-width="80px">
        <!-- <el-form-item label="学号">
          <el-input
            v-model="form.sid"
            placeholder="请输入学号"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item> -->

        <el-form-item label="姓名">
          <el-input
            v-model="form.name"
            placeholder="请输入姓名"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="email">
          <el-input
            v-model="form.email"
            placeholder="请输入email"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="微信号">
          <el-input
            v-model="form.wnum"
            placeholder="请输入微信号"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          type="primary" @click="dialogVisible = false"
        >确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  props: ["show", "dialogTitle", "rowData"],
  data() {
    return {
      dialogVisible: this.show,
      title: this.dialogTitle,

      form: {
        name: "",
        password: "",
        email: "",
        wnum: ""
      },

      canEdit: false,
      itemData: ""
    };
  },
  watch: {
    show() {
      this.dialogVisible = this.show;
    },
    dialogTitle() {
      this.title = this.dialogTitle;
      this.canEdit = this.title != "查看";
    },
    rowData() {
      this.itemData = this.rowData;
      if (this.title != "新增") {
        for (const key in this.itemData) {
          this.form[key] = this.itemData[key];
        }
      }
    }
  },
  methods: {}
};
</script>

<style lang="scss" scoped>
.matrix ::v-deep .el-input__inner {
  border-radius: 0;
}
</style>
