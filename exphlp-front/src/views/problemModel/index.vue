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
            <el-input v-model="instName" placeholder="请输入实例名" clearable/>
          </el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getByInstName()">查询</el-button>
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
      <el-table-column prop="instId" label="实例ID" width="210" align="center"></el-table-column>
      <el-table-column prop="instName" label="实例名称" width="250" align="center"></el-table-column>
      <el-table-column prop="description" label="实例描述" align="center"></el-table-column>
      <el-table-column label="操作" align="center" width="350">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" icon="el-icon-view" @click="getForm(scope.row)">查看</el-button>
          <el-button type="primary" size="mini" icon="el-icon-edit" @click="updateForm(scope.row)">编辑</el-button>
          <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteForm(scope.row.instId)">删除</el-button>
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
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="form" label-width="80px">
        <el-form-item label="实例ID" v-if="title!='添加'">
          <el-input v-model="form.instId" readonly></el-input>
        </el-form-item>
        <el-form-item label="实例名称" prop="instName">
          <el-input v-model="form.instName" placeholder="请输入实例名称" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="类别名称" prop="categoryName">
          <el-autocomplete
            class="inline-input"
            v-model="form.categoryName"
            :fetch-suggestions="querySearch"
            placeholder="请输入类别名称"
            :readonly="!canEdit"
            style="width: 340px"
          ></el-autocomplete>
        </el-form-item>
        <el-form-item label="目录名称" prop="dirName">
          <el-input v-model="form.dirName" placeholder="请输入目录名称" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="机器名称" prop="machineName">
          <el-input v-model="form.machineName" placeholder="请输入机器名称" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="IP地址" prop="machineIp">
          <el-input v-model="form.machineIp" placeholder="请输入IP地址" :readonly="!canEdit"></el-input>
        </el-form-item>
        <el-form-item label="实例描述" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="请输入实例描述" :readonly="!canEdit"></el-input>
        </el-form-item>

      </el-form>

      <div v-if="title!='查看'">
        <el-button @click="cancel('form')">取 消</el-button>
        <el-button type="primary" @click="submit('form')">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import {
  getProbInstList,
  getProbInstByInstName,
  addProbInst,
  updateProbInstById,
  deleteProbInstById,
  countAllProbInsts,
  countProbInstsByInstName
} from "@/api/vadmin/exphlp/probInstMgr";

export default {

  data() {
    return {
      tableData: [],
      multipleSelection: [],
      dialogVisible:false,
      canEdit: false,
      title: "",
      instName: '',
      form: {
        instId:"",
        instName: "",
        categoryName:"",
        dirName:"",
        machineName:"",
        machineIp:"",
        description: "",
      },
      pageHelper: {
        currentPageNum: 1,
        pageSize: 10,
        totalSize: 0,
      },
      formRules: {
        instName: [
          { required: true, message: '请输入实例名称', trigger: 'blur' },
        ],
        categoryName: [
          { required: true, message: '请输入类别名称', trigger: ('blur','change')},
        ],
        dirName: [
          { required: true, message: '请输入目录名称', trigger: 'blur' },
        ],
        machineName: [
          { required: true, message: '请输入机器名称', trigger: 'blur' },
        ],
        machineIp: [
          { required: true, message: '请输入IP地址', trigger: 'blur' },
        ],
        description: [
          { required: true, message: '请选择实例描述', trigger: 'blur' }
        ],
      },
      categoryNames:[
        {
          label: '类别1',
          value:'类别1'
        },
        {
          label: '类别2',
          value:'类别2'
        },
        {
          label: '类别3',
          value:'类别3'
        },
        {
          label: '类别4',
          value:'类别4'
        },
        {
          label: '类别5',
          value:'类别5'
        },
        {
          label: '类别6',
          value:'类别6'
        },
        {
          label: '类别7',
          value:'类别7'
        },
        {
          label: '类别8',
          value:'类别8'
        },
        {
          label: '类别9',
          value:'类别9'
        },
        {
          label: '类别10',
          value:'类别10'
        }
      ],
    };
  },
  created() {
    this.listProbInsts();
    this.countAllProbInsts();
    const sourcePage = this.$route.query.source;
    if (sourcePage === 'planManage') {
      this.addForm();
    }
  },
  methods: {
    listProbInsts() {
      getProbInstList(this.pageHelper.currentPageNum,this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countAllProbInsts();
      });
    },
    back(){
      getProbInstList(1,10).then(res => {
        this.tableData = res;
        this.countAllProbInsts();
      });
      this.instName = "";
      this.pageHelper.currentPageNum =1;
      this.pageHelper.pageSize =10;
    },
    getByInstName(){
      getProbInstByInstName(this.instName,this.pageHelper.currentPageNum,this.pageHelper.pageSize).then(res => {
        this.tableData = res;
        this.countProbInstsByInstName(this.instName);
      });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    addForm(){
      this.form = {
        instName: "",
        categoryName:"",
        dirName:"",
        machineName:"",
        machineIp:"",
        description: "",
      };
      this.form.machineIp = "localhost";
      this.dialogVisible = true;
      this.canEdit = true;
      this.title = '添加';
    },
    getForm(row){
      this.form = row;
      this.dialogVisible = true;
      this.canEdit = false;
      this.title = "查看";
    },
    updateForm(row){
      this.form = row;
      this.dialogVisible = true;
      this.canEdit = true;
      this.title = "编辑";
    },
    deleteForm(instId){
      this.$confirm("此操作将永久删除问题实例记录, 是否继续?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          deleteProbInstById(instId).then((res) => {
            this.$message({type: "success", message: "删除成功",});
            this.listProbInsts();
          });
        })
        .catch(() => {
          this.$message({type: "info", message: "取消删除",});
        });
    },
    deleteBatch() {
      if (this.multipleSelection.length == 0) {
        this.$message({
          type: "warning",
          message: "当前未选中任何问题实例!",
        });
      } else {
        this.$confirm("此操作将永久删除问题实例记录, 是否继续?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        })
          .then(() => {
            for (var i = 0; i < this.multipleSelection.length-1; i++) {
              deleteProbInstById(this.multipleSelection[i].instId);
            }
            deleteProbInstById(this.multipleSelection[this.multipleSelection.length-1].instId).then((res) => {
              this.$message({type: "success", message: "删除成功",});
              this.listProbInsts();
            });
          })
          .catch(() => {
            this.$message({
              type: "info",
              message: "取消删除",
            });
          });
      }
    },
    cancel(formName){
      this.$refs[formName].clearValidate();
      this.dialogVisible = false;
    },
    submit(formName){
      if (this.title == "编辑") {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            updateProbInstById(this.form).then(res => {
              this.$message({type: "success", message: "修改成功!",});
              this.dialogVisible = false;
            });
          } else {
            this.$message({type: "warning", message: "请完整填写信息",});
          }
        });
      } else if (this.title == "添加") {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            addProbInst(this.form).then(res => {
              this.$message({type: "success", message: "添加成功!",});
              this.dialogVisible = false;
              this.listProbInsts();
            });
          } else {
            this.$message({type: "warning", message: "请完整填写信息",});
          }
        });
      }
    },
    handleSizeChange(val) {
      this.pageHelper.pageSize = val
      if (this.instName != ""){
        this.getByInstName();
      }else {
        this.listProbInsts();
      }
    },
    handleCurrentChange(val) {
      this.pageHelper.currentPageNum = val
      if (this.instName != ""){
        this.getByInstName();
      }else {
        this.listProbInsts();
      }
    },
    countAllProbInsts(){
      countAllProbInsts().then(res => {
        this.pageHelper.totalSize = res;
      });
    },
    countProbInstsByInstName(instName){
      countProbInstsByInstName(instName).then(res => {
        this.pageHelper.totalSize = res;
      });
    },
    querySearch(queryString, cb) {
      var categoryNames = this.categoryNames;
      var results = queryString ? categoryNames.filter(this.createFilter(queryString)) : categoryNames;
      cb(results);
    },
    createFilter(queryString) {
      return (categoryName) => {
        return (categoryName.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0);
      };
    }
  }
};
</script>

<style lang="less" scoped>
</style>
