<template>
  <div class="app-container">
    <el-alert
      class="page-tip"
      :title="isAdmin ? '平台管理：管理员可维护全部账号，普通用户可查看并维护个人资料。' : '平台管理：可查看全部账号；仅可编辑与重置自己的账号信息。'"
      type="info"
      :closable="false"
      show-icon
    />
    <div class="toolbar-row">
      <div class="toolbar-left">
        <el-button v-if="isAdmin" type="success" icon="el-icon-plus" @click="addForm()">添加</el-button>
        <el-button v-if="isAdmin" type="danger" icon="el-icon-delete" @click="deleteBatch()">批量删除</el-button>
        <el-button type="default" icon="el-icon-document" @click="openManualDialog">查看操作手册</el-button>
      </div>
      <el-form :inline="true" class="toolbar-right">
        <el-input v-model="userName" placeholder="请输入姓名" clearable class="toolbar-search-input" />
        <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getUserByRegexName()">查询</el-button>
        <el-button type="default" icon="el-icon-refresh" @click="back()">刷新</el-button>
      </el-form>
    </div>
    <!-- 表格 -->
    <el-table
      :data="tableData"
      v-loading="tableLoading"
      border
      fit
      highlight-current-row
      class="platform-table"
      @selection-change="handleSelectionChange"
    >
      <!-- 多选框 -->
      <el-table-column type="selection" width="60" align="center"></el-table-column>
      <el-table-column prop="userName" label="姓名" min-width="140" align="center"></el-table-column>
      <el-table-column prop="email" label="email" min-width="220" align="center"></el-table-column>
      <el-table-column prop="wechat" label="wechat" min-width="180" align="center"></el-table-column>
      <el-table-column prop="mobile" label="手机号" min-width="140" align="center"></el-table-column>
      <el-table-column prop="qq" label="QQ号" min-width="120" align="center"></el-table-column>
      <el-table-column label="操作" align="center" width="320">
        <template slot-scope="scope">
          <div class="op-buttons">
            <el-button type="primary" size="mini" icon="el-icon-edit" :disabled="!canEditRow(scope.row)" @click="updateForm(scope.row)">编辑</el-button>
            <el-button type="warning" size="mini" icon="el-icon-key" :disabled="!canResetRow(scope.row)" @click="resetPassword(scope.row)">重置密码</el-button>
            <el-button v-if="isAdmin" type="danger" size="mini" icon="el-icon-delete" @click="deleteForm(scope.row.userId)">删除</el-button>
          </div>
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
        <el-button type="primary" :loading="submitLoading" @click="submit()">确 定</el-button>
      </div>
    </el-dialog>
    <manual-doc-dialog
      :visible.sync="manualDialogVisible"
      :page-key="manualPageKey"
    />
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
} from "@/api/exphlp/platMgr";
import ManualDocDialog from "@/components/ManualDocDialog";
import { mapGetters } from "vuex";

export default {
  components: { ManualDocDialog },
  computed: {
    ...mapGetters([
      "roles",
      "name"
    ]),
    isAdmin() {
      return Array.isArray(this.roles) && this.roles.includes("ROLE_ADMIN");
    },
    currentUserName() {
      return (this.name || "").toString().trim();
    }
  },

  data() {
    return {
      tableData: [],
      multipleSelection: [],
      dialogVisible: false,
      canEdit: false,
      tableLoading: false,
      submitLoading: false,
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
      },
      manualDialogVisible: false,
      manualPageKey: "platform"
    };
  },
  created() {
    this.listUsers();
    this.countAllUsers();
  },
  methods: {
    canEditRow(row) {
      return this.isAdmin || this.isSelfRow(row);
    },
    canResetRow(row) {
      return this.isAdmin || this.isSelfRow(row);
    },
    isSelfRow(row) {
      if (!row || !row.userName) {
        return false;
      }
      return row.userName === this.currentUserName;
    },
    openManualDialog() {
      this.manualPageKey = "platform";
      this.manualDialogVisible = true;
    },
    listUsers() {
      this.tableLoading = true;
      getUserList(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countAllUsers();
      }).finally(() => {
        this.tableLoading = false;
      });
    },
    back() {
      this.tableLoading = true;
      getUserList(1, 10).then(res => {
        this.tableData = res;
        this.countAllUsers();
      }).finally(() => {
        this.tableLoading = false;
      });
      this.userName = "";
      this.pageHelper.currentPageNum = 1;
      this.pageHelper.pageSize = 10;
    },
    getUserByRegexName() {
      this.tableLoading = true;
      getUserByRegexName(this.userName, this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countUserByUserName(this.userName);
      }).finally(() => {
        this.tableLoading = false;
      });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    addForm() {
      if (!this.isAdmin) {
        this.$message({ type: "warning", message: "仅管理员可新增用户" });
        return;
      }
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
      if (!this.canEditRow(row)) {
        this.$message({ type: "warning", message: "仅可编辑自己的账号信息" });
        return;
      }
      this.form = Object.assign({}, row, {
        _originUserName: row && row.userName ? row.userName : ""
      });
      this.dialogVisible = true;
      this.canEdit = true;
      this.title = "编辑";
    },
    deleteForm(userId) {
      if (!this.isAdmin) {
        this.$message({ type: "warning", message: "仅管理员可删除用户" });
        return;
      }
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
      if (!this.isAdmin) {
        this.$message({ type: "warning", message: "仅管理员可批量删除用户" });
        return;
      }
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
            const tasks = this.multipleSelection.map(item => deleteUserById(item.userId));
            Promise.all(tasks).then(() => {
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
      if (this.submitLoading) {
        return;
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return;
        }
        if (this.title == "编辑") {
          this.submitLoading = true;
          const payload = Object.assign({}, this.form);
          const commitUpdate = (nextPayload) => updateUserById(nextPayload).then(() => {
            this.$message({ type: "success", message: "修改成功!" });
            this.dialogVisible = false;
            this.listUsers();
          });
          payload.userName = (payload.userName || "").trim();
          if (!payload.userName && payload._originUserName) {
            payload.userName = payload._originUserName;
          }
          commitUpdate(payload).catch((error) => {
            this.$message({ type: "error", message: (error && error.message) || "修改失败" });
          }).finally(() => {
            this.submitLoading = false;
          });
          return;
        }
        if (this.title == "添加") {
          this.submitLoading = true;
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
          }).catch(() => {}).finally(() => {
            this.submitLoading = false;
          });
          this.dialogVisible = false;
        }
      });
    },
    resetPassword(row) {
      if (!this.canResetRow(row)) {
        this.$message({ type: "warning", message: "仅可重置自己的密码" });
        return;
      }
      this.$prompt(`请输入用户 ${row.userName} 的新密码`, "重置密码", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputType: "password",
        inputPattern: /^.{6,50}$/,
        inputErrorMessage: "密码长度需在6到50之间"
      }).then(({ value }) => {
        return resetUserPassword(row.userId, value, row.userName);
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
.app-container {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.page-tip {
  margin-bottom: 12px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-left: auto;
}

.toolbar-search-input {
  width: 300px;
}

.op-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.platform-table /deep/ .el-table th {
  background: #f8fafc;
  color: #4a5568;
  font-weight: 600;
}

.platform-table /deep/ .el-table td,
.platform-table /deep/ .el-table th {
  padding: 9px 0;
}

@media (max-width: 1200px) {
  .toolbar-row {
    flex-wrap: wrap;
  }

  .toolbar-right {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
