<template>
  <div class="app-container">
    <!-- 添加 批量删除 查询-->
    <el-row>
      <el-col :span="2">
        <el-button type="success" icon="el-icon-plus" @click="addForm()">添加</el-button>
      </el-col>
      <el-col :span="2">
        <el-button type="danger" icon="el-icon-delete" @click="deleteBatch()">批量删除</el-button>
      </el-col>
      <el-col :span="20">
        <el-form :inline="true" class="demo-form-inline" align="center">
          <el-form-item>
            <el-input v-model="userName" placeholder="请输入姓名" clearable />
          </el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getUserByRegexName()">查询</el-button>
          <el-button type="default" icon="el-icon-refresh" @click="back()">返回</el-button>
        </el-form>
      </el-col>
    </el-row>
    <!-- 表格 -->
    <el-table
      :data="tableData"
      border
      fit
      highlight-current-row
      @selection-change="handleSelectionChange"
    >
      <!-- 多选框 -->
      <el-table-column type="selection" width="60" align="center"></el-table-column>
      <el-table-column prop="userName" label="姓名" width="210" align="center"></el-table-column>
      <el-table-column prop="email" label="email" width="150" align="center"></el-table-column>
      <el-table-column prop="wechat" label="wechat" width="180" align="center"></el-table-column>
      <el-table-column prop="mobile" label="手机号" width="160" align="center"></el-table-column>
      <el-table-column prop="qq" label="QQ号" width="140" align="center"></el-table-column>
      <el-table-column label="操作" align="center" width="250">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" icon="el-icon-edit" @click="updateForm(scope.row)">编辑</el-button>
          <el-button type="warning" size="mini" icon="el-icon-key" @click="resetPassword(scope.row)">重置密码</el-button>
          <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteForm(scope.row.userId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <br>
    <el-pagination
      align="center"
      :current-page="pageHelper.currentPageNum"
      :page-sizes="[10, 20, 30, 40, 50]"
      :page-size="pageHelper.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pageHelper.totalSize"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    >
    </el-pagination>
    <!-- 对话框-->
    <el-dialog
      :title.sync="title"
      :visible.sync="dialogVisible"
      width="30%"
      align="center"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入姓名" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="email" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="微信号" prop="wechat">
          <el-input v-model="form.wechat" placeholder="请输入微信号" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="QQ号" prop="qq">
          <el-input v-model="form.qq" placeholder="请输入QQ号" :readonly="!canEdit"></el-input>
        </el-form-item>

      </el-form>

      <div v-if="title!='查看'">
        <el-button @click="cancel()">取 消</el-button>
        <el-button type="primary" @click="submit()">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import {
  getUserList,
  deleteUserById,
  addUser,
  getUserByRegexName,
  updateUserById,
  countAllUsers,
  countUserByUserName,
  resetUserPassword
} from "@/api/vadmin/exphlp/platMgr";

export default {

  data() {
    return {
      tableData: [],
      multipleSelection: [],
      dialogVisible: false,
      canEdit: false,
      title: "",
      userName: "",
      form: {
        userId: "",
        userName: "",
        email: "",
        wechat: "",
        mobile: "",
        qq: ""
      },
      pageHelper: {
        currentPageNum: 1,
        pageSize: 10,
        totalSize: 0
      },
      rules: {
        userName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        email: [
          {
            pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
            message: "请输入正确的邮箱地址",
            trigger: "blur"
          }
        ],
        mobile: [
          {
            pattern: /^1\d{10}$/,
            message: "请输入11位手机号",
            trigger: "blur"
          }
        ],
        qq: [
          {
            pattern: /^[1-9]\d{4,11}$/,
            message: "请输入正确QQ号",
            trigger: "blur"
          }
        ]
      }
    };
  },
  created() {
    this.listUsers();
    this.countAllUsers();
  },
  methods: {
    listUsers() {
      getUserList(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countAllUsers();
      });
    },
    back() {
      getUserList(1, 10).then(res => {
        this.tableData = res;
        this.countAllUsers();
      });
      this.userName = "";
      this.pageHelper.currentPageNum = 1;
      this.pageHelper.pageSize = 10;
    },
    getUserByRegexName() {
      getUserByRegexName(this.userName, this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countUserByUserName(this.userName);
      });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    addForm() {
      this.form = {
        userId: "",
        userName: "",
        email: "",
        wechat: "",
        mobile: "",
        qq: ""
      };
      this.dialogVisible = true;
      this.canEdit = true;
      this.title = "添加";
    },
    getForm(row) {
      this.form = row;
      this.dialogVisible = true;
      this.canEdit = false;
      this.title = "查看";
    },
    updateForm(row) {
      this.form = row;
      this.dialogVisible = true;
      this.canEdit = true;
      this.title = "编辑";
    },
    deleteForm(userId) {
      this.$confirm("此操作将永久删除用户, 是否继续?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          deleteUserById(userId).then((res) => {
            this.$message({ type: "success", message: "删除成功" });
            this.listUsers();
          });
        })
        .catch(() => {
          this.$message({ type: "info", message: "取消删除" });
        });
    },
    deleteBatch() {
      if (this.multipleSelection.length == 0) {
        this.$message({
          type: "warning",
          message: "当前未选中任何用户!"
        });
      } else {
        this.$confirm("此操作将永久删除用户, 是否继续?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        })
          .then(() => {
            for (var i = 0; i < this.multipleSelection.length - 1; i++) {
              deleteUserById(this.multipleSelection[i].userId);
            }
            deleteUserById(this.multipleSelection[this.multipleSelection.length - 1].userId).then((res) => {
              this.$message({ type: "success", message: "删除成功" });
              this.listUsers();
            });
          })
          .catch(() => {
            this.$message({
              type: "info",
              message: "取消删除"
            });
          });
      }
    },
    cancel() {
      this.dialogVisible = false;
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return;
        }
        if (this.title == "编辑") {
          updateUserById(this.form).then(() => {
            this.$message({ type: "success", message: "修改成功!" });
            this.listUsers();
          });
          this.dialogVisible = false;
          return;
        }
        if (this.title == "添加") {
          this.$prompt("请输入初始密码", "新增用户", {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            inputType: "password",
            inputPattern: /^.{6,50}$/,
            inputErrorMessage: "密码长度需在6到50之间"
          }).then(({ value }) => {
            const payload = Object.assign({}, this.form, { password: value });
            return addUser(payload);
          }).then(() => {
            this.$message({ type: "success", message: "添加成功!" });
            this.listUsers();
          }).catch(() => {});
          this.dialogVisible = false;
        }
      });
    },
    resetPassword(row) {
      this.$prompt(`请输入用户 ${row.userName} 的新密码`, "重置密码", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputType: "password",
        inputPattern: /^.{6,50}$/,
        inputErrorMessage: "密码长度需在6到50之间"
      }).then(({ value }) => {
        return resetUserPassword(row.userId, value);
      }).then(() => {
        this.$message({ type: "success", message: "密码已重置" });
      }).catch(() => {});
    },
    handleSizeChange(val) {
      this.pageHelper.pageSize = val;
      if (this.userName != "") {
        this.getUserByRegexName();
      } else {
        this.listUsers();
      }
    },
    handleCurrentChange(val) {
      this.pageHelper.currentPageNum = val;
      if (this.userName != "") {
        this.getUserByRegexName();
      } else {
        this.listUsers();
      }
    },
    countAllUsers() {
      countAllUsers().then(res => {
        this.pageHelper.totalSize = res;
      });
    },
    countUserByUserName(userName) {
      countUserByUserName(userName).then(res => {
        this.pageHelper.totalSize = res;
      });
    }

  }
};
</script>

<style lang="less" scoped>
</style>
