<template>
  <div>
    <el-dialog
      :title.sync="title"
      :visible.sync="dialogVisible"
      width="50%"
      @close="$emit('update:show', false)"
    >
      <el-form label-width="80px">
        <el-form-item label="问题名称">
          <el-input
            v-model="form.instName"
            placeholder="请输入问题实例名称"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="问题分类">
          <el-input
            v-model="form.categoryName"
            placeholder="请输入问题实例名称"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="机器名">
          <el-input
            v-model="form.machineName"
            placeholder="请输入保存程序的机器名"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="机器IP">
          <el-input
            v-model="form.machineIp"
            placeholder="请输入保存程序的机器IP"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="路径">
          <el-input
            v-model="form.dirName"
            placeholder="请输入路径"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请输入问题实例描述信息"
            :readonly="!canEdit"
          ></el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          @click="handlerConfirm"
        >确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { updateProbById, addProblem } from "@/api/vadmin/problem";
export default {
  props: ["show", "dialogTitle", "rowData"],
  data() {
    return {
      dialogVisible: this.show,
      title: this.dialogTitle,

      form: {
        instId: "",
        instName: "",
        categoryName: "",
        machineName: "",
        machineIp: "",
        dirName: "",
        description: "",
        row: "",
        col: "",
        input: [[]],
        valueCol: "",
        valueInput: []
      },

      rowInt: 0,
      colInt: 0,
      valueColInt: 0,
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
        this.form.instId = this.rowData["instId"];
        this.form.instName = this.rowData["instName"];
        this.form.categoryName = this.rowData["categoryName"];
        this.form.machineName = this.rowData["machineName"];
        this.form.machineIp = this.rowData["machineIp"];
        this.form.dirName = this.rowData["dirName"];
        this.form.description = this.rowData["description"];
        
        // this.form.row = this.rowData["contraintMatrix"].length;
        // this.form.col = this.rowData["contraintMatrix"][0].length;
        // this.rowInt = this.rowData["contraintMatrix"].length;
        // this.colInt = this.rowData["contraintMatrix"][0].length;
        // this.form.input = this.getInput(this.rowInt, this.colInt);
        // this.form.valueCol = this.rowData["valueVector"].length;
        // this.valueColInt = this.rowData["valueVector"].length;
        // this.form.valueInput = this.getValueInput(this.valueColInt);
      }
    }
  },
  methods: {
    leave() {
      if (this.form.col != "" && this.form.row != "") {
        this.rowInt = parseInt(this.form.row);
        this.colInt = parseInt(this.form.col);
        this.form.input = this.getInput(this.rowInt, this.colInt);
      }
      if (this.form.valueCol != "") {
        this.valueColInt = parseInt(this.form.valueCol);
        this.form.valueInput = this.getValueInput(this.valueColInt);
      }
    },

    // 约束矩阵值
    // getInput(row, col) {
    //   const input = [[]];
    //   for (let a = 0; a < row; a++) {
    //     input[a] = [];
    //     if (a >= this.rowData["contraintMatrix"].length) {
    //       for (let b = 0; b < col; b++) {
    //         input[a][b] = undefined;
    //       }
    //     } else {
    //       for (let b = 0; b < col; b++) {
    //         input[a][b] = this.rowData["contraintMatrix"][a][b];
    //       }
    //     }
    //   }
    //   return input;
    // },

    // 价值矩阵值
    // getValueInput(col) {
    //   const valueInput = [];
    //   for (let a = 0; a < col; a++) {
    //     valueInput[`${a}`] = this.rowData["valueVector"][a];
    //   }
    //   return valueInput;
    // },

    handlerConfirm() {
      this.title = this.dialogTitle;
      if (this.title == "编辑") {
        console.log(this.form);
        this.updateProById(this.form);
      } else if (this.title == "新增") {
        addProblem(this.form);
      }
      this.dialogVisible = false;
    },
    updateProById(problem) {
      updateProbById(problem).then(result => {
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.matrix ::v-deep .el-input__inner {
  border-radius: 0;
}
</style>
