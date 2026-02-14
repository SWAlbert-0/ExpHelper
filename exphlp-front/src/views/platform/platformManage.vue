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
            <el-input v-model="userName" placeholder="请输入姓名" clearable/>
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
      <el-table-column prop="password" label="密码" width="200" align="center"></el-table-column>
      <el-table-column prop="role" label="角色" width="150" align="center"></el-table-column>
      <el-table-column prop="email" label="email" width="150" align="center"></el-table-column>
      <el-table-column prop="wechat" label="wechat" align="center"></el-table-column>
      <el-table-column label="操作" align="center" width="250">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" icon="el-icon-edit" @click="updateForm(scope.row)">编辑</el-button>
          <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteForm(scope.row.userId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <br>
    <el-pagination
      align="center"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      :current-page="pageHelper.currentPageNum"
      :page-sizes="[10, 20, 30, 40, 50]"
      :page-size="pageHelper.pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pageHelper.totalSize">
    </el-pagination>
    <!-- 对话框-->
    <el-dialog
      :title.sync="title"
      :visible.sync="dialogVisible"
      width="30%"
      align="center"
    >
      <el-form :model="form" ref="form" label-width="80px">
        <el-form-item label="姓名" >
          <el-input v-model="form.userName" placeholder="请输入姓名" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="请输入密码" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-input v-model="form.role" placeholder="请输入角色" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="微信号">
          <el-input v-model="form.wechat" placeholder="请输入微信号" :readonly="!canEdit"></el-input>
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
  countUserByUserName
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
        password: "",
        role: 0,
        email: "",
        wechat: ""
      },
      pageHelper: {
        currentPageNum: 1,
        pageSize: 10,
        totalSize: 0
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
      this.form = {};
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
              deleteUserById(this.multipleSelection[i].instId);
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
      if (this.title == "编辑") {
        updateUserById(this.form).then(res => {
          this.$message({ type: "success", message: "修改成功!" });
        });
      } else if (this.title == "添加") {
        addUser(this.form).then(res => {
          this.$message({ type: "success", message: "添加成功!" });
          this.listUsers();
        });
      }
      this.dialogVisible = false;
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
