<template>
  <div class="app-container">
    <!--添加 批量删除 查询-->
    <el-row>
      <el-col :span="2">
        <el-button type="success" icon="el-icon-plus" @click="openAlgInfoAddForm()">添加</el-button>
      </el-col>
      <el-col :span="2">
        <el-button type="danger" icon="el-icon-delete" @click="deleteBatch()">批量删除</el-button>
      </el-col>
      <el-col :span="20">
        <el-form :inline="true" class="demo-form-inline" align="center">
          <el-form-item>
            <el-input v-model="algName" placeholder="请输入算法名称" clearable/>
          </el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getByAlgName()">查询</el-button>
          <el-button type="default" icon="el-icon-refresh" @click="back()">返回</el-button>
        </el-form>
      </el-col>
    </el-row>

    <!--算法信息表格-->
    <el-table
      :data="algInfo"
      border
      fit
      highlight-current-row
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="60" align="center"></el-table-column>
      <el-table-column prop="algName" label="算法名称" width="250" align="center"></el-table-column>
      <el-table-column prop="serviceName" label="服务名" width="250" align="center"></el-table-column>
      <el-table-column prop="description" label="算法描述" width="300" align="center"></el-table-column>
      <el-table-column label="参数" align="center">
        <template slot-scope="scope">
          <el-button  type="text" size="mini" @click="openDefParasTable(scope.row)">参数列表</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="250">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" icon="el-icon-edit" @click="openAlgInfoUpdateForm(scope.row)">编辑</el-button>
          <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteAlgInfo(scope.row.algId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!--分页-->
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

    <!--算法信息添加对话框-->
    <el-dialog
      title="添加"
      :visible.sync="dialogAlgInfoAddFormVisible"
      width="60%"
      align="center"
      :close-on-click-modal="false"
    >
      <el-form :model="algInfoAddForm" :rules="algInfoFormRules" ref="algInfoAddForm" label-width="80px" :style="{ width: '300px' }">
        <el-form-item label="算法名称" prop="algName">
          <el-input v-model="algInfoAddForm.algName" placeholder="请输入算法名称" ></el-input>
        </el-form-item>
        <el-form-item label="服务名">
          <el-input v-model="algInfoAddForm.algName" placeholder="服务名"></el-input>
        </el-form-item>
        <el-form-item label="算法描述" prop="description">
          <el-input type="textarea" v-model="algInfoAddForm.description" placeholder="请输入算法描述" ></el-input>
        </el-form-item>
        <el-form-item label="算法参数">
          <el-button  type="text" size="mini" @click="openAlgInfoDefParasAddForm()">添加参数</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="algInfoAddForm.defParas"  border fit highlight-current-row v-if="algInfoAddForm.defParas.length != 0">
        <el-table-column property="paraId" label="序号" width="100" align="center"></el-table-column>
        <el-table-column property="paraName" label="参数名" width="100" align="center"></el-table-column>
        <el-table-column property="paraType" label="参数类型" width="100" align="center"></el-table-column>
        <el-table-column property="paraValue" label="参数值" width="100" align="center"></el-table-column>
        <el-table-column property="description" label="描述" align="center"></el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" icon="el-icon-edit" @click="openAlgInfoDefParasUpdateForm(scope.row)">编辑</el-button>
            <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteAlgInfoDefParas(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <br>
      <div>
        <el-button @click="closeAlgInfoAddForm('algInfoAddForm')">取 消</el-button>
        <el-button type="primary" @click="submitAlgInfoAddForm('algInfoAddForm')">确 定</el-button>
      </div>
      <!--参数添加表单-->
      <el-dialog
        width="30%"
        center
        title="添加"
        :visible.sync="dialogAlgInfoDefParasAddFormVisible"
        append-to-body
        :close-on-click-modal="false"
      >
        <el-form :model="defParasForm" :rules="defParasFormRules" ref="defParasForm" label-width="80px">
          <el-form-item label="参数名" prop="paraName">
            <el-input v-model="defParasForm.paraName" placeholder="请输入参数名"></el-input>
          </el-form-item>
          <el-form-item label="参数类别" prop="paraType">
            <el-select v-model="defParasForm.paraType" placeholder="请选择参数类别" style="width: 278px" clearable>
              <el-option v-for="(item, index) in paraType" :key="item.value" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="参数值" prop="paraValue">
            <el-input v-model="defParasForm.paraValue" placeholder="请输入参数值"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="defParasForm.description" placeholder="请输入描述"></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="closeAlgInfoDefParasAddForm('defParasForm')">取 消</el-button>
          <el-button type="primary" @click="submitAlgInfoDefParasAddForm('defParasForm')">确 定</el-button>
        </span>
      </el-dialog>
      <!--参数修改表单-->
      <el-dialog
        width="30%"
        center
        title="编辑"
        :visible.sync="dialogAlgInfoDefParasUpdateFormVisible"
        :close-on-click-modal="false"
        append-to-body>
        <el-form :model="defParasForm" :rules="defParasFormRules" ref="defParasForm" label-width="80px">
          <el-form-item label="序号">
            <el-input v-model="defParasForm.paraId" readonly></el-input>
          </el-form-item>
          <el-form-item label="参数名" prop="paraName">
            <el-input v-model="defParasForm.paraName" placeholder="请输入参数名"></el-input>
          </el-form-item>
          <el-form-item label="参数类别" prop="paraType">
            <el-select v-model="defParasForm.paraType" placeholder="请选择参数类别" style="width: 278px" clearable>
              <el-option v-for="(item, index) in paraType" :key="index" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="参数值" prop="paraValue">
            <el-input v-model="defParasForm.paraValue" placeholder="请输入参数值"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="defParasForm.description" placeholder="请输入描述"></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="closeAlgInfoDefParasUpdateForm('defParasForm')">取 消</el-button>
          <el-button type="primary" @click="submitAlgInfoDefParasUpdateForm('defParasForm')">确 定</el-button>
        </span>
      </el-dialog>
    </el-dialog>

    <!--算法信息编辑对话框-->
    <el-dialog
      title="编辑"
      :visible.sync="dialogAlgInfoUpdateFormVisible"
      width="30%"
      align="center"
      :close-on-click-modal="false"
    >
      <el-form :model="algInfoUpdateForm" :rules="algInfoFormRules" ref="algInfoUpdateForm" label-width="80px">
        <el-form-item label="算法名称">
          <el-input v-model="algInfoUpdateForm.algName" readonly></el-input>
        </el-form-item>
        <el-form-item label="服务名">
          <el-input v-model="algInfoUpdateForm.serviceName" readonly></el-input>
        </el-form-item>
        <el-form-item label="算法描述" prop="description">
          <el-input type="textarea" v-model="algInfoUpdateForm.description" placeholder="请输入算法描述"></el-input>
        </el-form-item>
      </el-form>
      <div>
        <el-button @click="closeAlgInfoUpdateForm('algInfoUpdateForm')">取 消</el-button>
        <el-button type="primary" @click="submitAlgInfoUpdateForm('algInfoUpdateForm')">确 定</el-button>
      </div>
    </el-dialog>

    <!--参数表格对话框-->
    <el-dialog
      title="参数列表"
      :visible.sync="dialogDefParasTableVisible"
      center
      width="60%"
      :close-on-click-modal="false"
    >
      <el-row>
        <el-col :span="2">
          <el-button type="success" icon="el-icon-plus" size="mini" @click="openDefParasAddForm()">添加</el-button>
        </el-col>
      </el-row>
      <br>
      <el-table :data="defParas"  border fit highlight-current-row>
        <el-table-column property="paraId" label="序号" width="100" align="center"></el-table-column>
        <el-table-column property="paraName" label="参数名" width="100" align="center"></el-table-column>
        <el-table-column property="paraType" label="参数类型" width="100" align="center"></el-table-column>
        <el-table-column property="paraValue" label="参数值" width="100" align="center"></el-table-column>
        <el-table-column property="description" label="描述" align="center"></el-table-column>
        <el-table-column label="操作" align="center" width="200">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" icon="el-icon-edit" @click="openDefParasUpdateForm(scope.row)">编辑</el-button>
            <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteDefParas(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 添加对话框-->
      <el-dialog
        width="30%"
        center
        title="添加"
        :visible.sync="dialogDefParasAddFormVisible"
        append-to-body
        :close-on-click-modal="false"
      >
        <el-form :model="defParasForm" :rules="defParasFormRules" ref="defParasForm" label-width="80px">
          <el-form-item label="参数名" prop="paraName">
            <el-input v-model="defParasForm.paraName" placeholder="请输入参数名"></el-input>
          </el-form-item>
          <el-form-item label="参数类别" prop="paraType">
            <el-select v-model="defParasForm.paraType" placeholder="请选择参数类别" style="width: 278px" clearable>
              <el-option v-for="(item, index) in paraType" :key="index" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="参数值" prop="paraValue">
            <el-input v-model="defParasForm.paraValue" placeholder="请输入参数值"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="defParasForm.description" placeholder="请输入描述"></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="closeDefParasAddForm('defParasForm')">取 消</el-button>
          <el-button type="primary" @click="submitDefParasAddForm('defParasForm')">确 定</el-button>
        </span>
      </el-dialog>
      <!-- 编辑对话框-->
      <el-dialog
        width="30%"
        center
        title="编辑"
        :visible.sync="dialogDefParasUpdateFormVisible"
        append-to-body
        :close-on-click-modal="false"
      >
        <el-form :model="defParasForm" :rules="defParasFormRules" ref="defParasForm" label-width="80px">
          <el-form-item label="序号">
            <el-input v-model="defParasForm.paraId" readonly></el-input>
          </el-form-item>
          <el-form-item label="参数名" prop="paraName">
            <el-input v-model="defParasForm.paraName" placeholder="请输入参数名"></el-input>
          </el-form-item>
          <el-form-item label="参数类别" prop="paraType">
            <el-select v-model="defParasForm.paraType" placeholder="请选择参数类别" style="width: 278px" clearable>
              <el-option v-for="(item, index) in paraType" :key="index" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="参数值" prop="paraValue">
            <el-input v-model="defParasForm.paraValue" placeholder="请输入参数值"></el-input>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input type="textarea" v-model="defParasForm.description" placeholder="请输入描述"></el-input>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="closeDefParasUpdateForm('defParasForm')">取 消</el-button>
          <el-button type="primary" @click="submitDefParasUpdateForm('defParasForm')">确 定</el-button>
        </span>
      </el-dialog>
      <br>
      <div align="center">
        <el-button @click="closeDefParasTable()">取 消</el-button>
        <el-button type="primary" @click="submitDefParasTable()">提 交</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>


import {
  addAlg,
  deleteAlgById,
  getAlgs,
  getAlgById,
  getAlgsByName,
  updateAlgById,
  getParaByAlgId,
  countAllAlgInfos,
  countAlgInfosByAlgName,
} from "@/api/vadmin/exphlp/algLibMgr";

export default {

  data() {
    return {
      dialogAlgInfoAddFormVisible:false,
      dialogAlgInfoDefParasAddFormVisible:false,
      dialogAlgInfoDefParasUpdateFormVisible:false,
      dialogAlgInfoUpdateFormVisible:false,
      dialogDefParasTableVisible:false,
      dialogDefParasAddFormVisible: false,
      dialogDefParasUpdateFormVisible: false,
      algName: '',
      multipleSelection: [],
      algInfo:[],
      defParas:[],
      algInfoAddForm:{
        algName:"",
        serviceName:'',
        defParas:[],
        description:"",
      },
      algInfoUpdateForm:{
        algName:"",
        serviceName:'',
        description:"",
      },
      defParasForm:{
        paraId:"",
        paraName:"",
        paraType:"",
        paraValue:"",
        description:"",
      },
      pageHelper: {
        currentPageNum: 1,
        pageSize: 10,
        totalSize: 0,
      },
      paraType: [
        {
          value: "int",
          label: "int",
        },
        {
          value: "double",
          label: "double",
        },
        {
          value: "String",
          label: "String",
        },
        {
          value: "boolean",
          label: "boolean",
        },
        {
          value: "Object",
          label: "Object",
        },
      ],
      algInfoFormRules: {
        algName: [
          { required: true, message: '请输入算法名称', trigger: 'blur' },
        ],
        serviceName:[
          { required: true, message: '请输入服务名', trigger: 'blur' },
        ],
        description: [
          { required: true, message: '请输入算法描述', trigger: 'blur' }
        ],
      },
      defParasFormRules: {
        paraName: [
          { required: true, message: '请输入参数名', trigger: 'blur' },
        ],
        paraType: [
          { required: true, message: '请选择参数类别', trigger: 'change' },
        ],
        paraValue: [
          { required: true, message: '请输入参数值', trigger: 'blur' },
        ],
        description: [
          { required: true, message: '请输入算法描述', trigger: 'blur' }
        ],
      },
    };
  },
  created() {
    this.getAlgInfos();
    this.countAllAlgInfos();
    const sourcePage = this.$route.query.source;
    if (sourcePage === 'planManage') {
      this.openAlgInfoAddForm();
    }
  },
  methods: {
    getAlgInfos(){
      getAlgs(this.pageHelper.currentPageNum,this.pageHelper.pageSize).then(res => {
        this.algInfo = res;
        this.countAllAlgInfos();
      });
    },
    back(){
      getAlgs(1,10).then(res => {
        this.algInfo = res;
        this.countAllAlgInfos();
      });
      this.algName = "";
      this.pageHelper.currentPageNum =1;
      this.pageHelper.pageSize =10;
    },
    getByAlgName(){
      getAlgsByName(this.algName,this.pageHelper.currentPageNum,this.pageHelper.pageSize).then(res => {
        this.algInfo = res;
        this.countAlgInfosByAlgName(this.algName);
      });
    },
    //
    openAlgInfoAddForm(){
      this.algInfoAddForm = {
        algName:"",
        serviceName: '',
        defParas:[],
        description:"",
      };
      this.dialogAlgInfoAddFormVisible = true;
    },
    closeAlgInfoAddForm(formName){
      this.$refs[formName].resetFields();
      this.dialogAlgInfoAddFormVisible = false;
    },
    submitAlgInfoAddForm(formName){
      this.$refs[formName].validate((valid) => {
        if (valid) {
          getAlgsByName(this.algInfoAddForm.algName,1,1).then(res => {
            if (res.length){
              this.$message({type: "warning", message: "该算法已存在，请更换算法名",});
            }
            else{
              this.algInfoAddForm.serviceName = this.algInfoAddForm.algName;
              addAlg(this.algInfoAddForm).then(res => {
                this.$message({type: "success", message: "添加成功!",});
                this.getAlgInfos();
                this.dialogAlgInfoAddFormVisible = false;
              });
            }
          });
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    openAlgInfoDefParasAddForm(){
      this.defParasForm = {};
      this.dialogAlgInfoDefParasAddFormVisible = true;
    },
    closeAlgInfoDefParasAddForm(formName){
      this.$refs[formName].resetFields();
      this.dialogAlgInfoDefParasAddFormVisible = false;
    },
    submitAlgInfoDefParasAddForm(formName){
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.defParasForm.paraId = this.algInfoAddForm.defParas.length + 1;
          this.algInfoAddForm.defParas.push(this.defParasForm);
          this.dialogAlgInfoDefParasAddFormVisible = false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    openAlgInfoDefParasUpdateForm(row){
      this.defParasForm = row;
      this.dialogAlgInfoDefParasUpdateFormVisible = true;
    },
    closeAlgInfoDefParasUpdateForm(formName){
      this.$refs[formName].resetFields();
      this.dialogAlgInfoDefParasUpdateFormVisible =false;
    },
    submitAlgInfoDefParasUpdateForm(formName){
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.dialogAlgInfoDefParasUpdateFormVisible =false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    deleteAlgInfoDefParas(row){
      this.algInfoAddForm.defParas.splice(row.paraId - 1,1);
      for(var i = 0; i < this.algInfoAddForm.defParas.length; i++){
        this.algInfoAddForm.defParas[i].paraId = i + 1;
      }
    },
    //
    openAlgInfoUpdateForm(row){
      this.algInfoUpdateForm = row;
      this.dialogAlgInfoUpdateFormVisible = true;
    },
    closeAlgInfoUpdateForm(formName){
      this.$refs[formName].resetFields();
      this.dialogAlgInfoUpdateFormVisible = false;
    },
    submitAlgInfoUpdateForm(formName){
      this.$refs[formName].validate((valid) => {
        if (valid) {
          updateAlgById(this.algInfoUpdateForm).then(res => {
            this.$message({type: "success", message: "修改成功!",});
            this.dialogAlgInfoUpdateFormVisible = false;
          });
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    deleteAlgInfo(algId){
      this.$confirm("此操作将永久删除算法信息记录, 是否继续?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          deleteAlgById(algId).then((res) => {
            this.$message({type: "success", message: "删除成功",});
            this.getAlgInfos();
          });
        })
        .catch(() => {
          this.$message({type: "info", message: "取消删除",});
        });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    deleteBatch() {
      if (this.multipleSelection.length == 0) {
        this.$message({
          type: "warning",
          message: "当前未选中任何算法信息!",
        });
      } else {
        this.$confirm("此操作将永久删除问题算法信息, 是否继续?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        })
          .then(() => {
            for (var i = 0; i < this.multipleSelection.length-1; i++) {
              deleteAlgById(this.multipleSelection[i].algId);
            }
            deleteAlgById(this.multipleSelection[this.multipleSelection.length-1].algId).then((res) => {
              this.$message({type: "success", message: "删除成功",});
              this.getAlgInfos();
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
    //
    openDefParasTable(algInfo){
      this.dialogDefParasTableVisible = true;
      this.defParas = algInfo.defParas;
      this.algInfoAddForm = algInfo;
    },
    closeDefParasTable(){
      this.dialogDefParasTableVisible = false;
    },
    submitDefParasTable(){
      this.algInfoAddForm.defParas = this.defParas;
      updateAlgById(this.algInfoAddForm).then(res => {
        this.$message({type: "success", message: "修改成功!",});
      });
      this.dialogDefParasTableVisible =false;
    },
    //
    openDefParasAddForm(){
      this.defParasForm = {};
      this.dialogDefParasAddFormVisible = true;
    },
    closeDefParasAddForm(formName){
      this.$refs[formName].resetFields();
      this.dialogDefParasAddFormVisible = false;
    },
    submitDefParasAddForm(formName){
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if(this.defParas == null){
            this.defParas = []
          }
          this.defParasForm.paraId = this.defParas.length + 1;
          this.defParas.push(this.defParasForm);
          this.dialogDefParasAddFormVisible = false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    openDefParasUpdateForm(scope){
      this.defParasForm = scope;
      this.dialogDefParasUpdateFormVisible = true;
    },
    closeDefParasUpdateForm(formName){
      this.$refs[formName].resetFields();
      this.dialogDefParasUpdateFormVisible =false;
    },
    submitDefParasUpdateForm(formName){

      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.dialogDefParasUpdateFormVisible =false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    //
    deleteDefParas(row){
      this.defParas.splice(row.paraId - 1,1);
      for(var i = 0; i < this.defParas.length; i++){
        this.defParas[i].paraId = i + 1;
      }
    },
    //
    handleSizeChange(val) {
      this.pageHelper.pageSize = val
      if (this.algName != ""){
        this.getByAlgName();
      }else {
        this.getAlgInfos();
      }
    },
    handleCurrentChange(val) {
      this.pageHelper.currentPageNum = val
      if (this.algName != ""){
        this.getByAlgName();
      }else {
        this.getAlgInfos();
      }
    },
    //
    countAllAlgInfos(){
      countAllAlgInfos().then(res => {
        this.pageHelper.totalSize = res;
      });
    },
    countAlgInfosByAlgName(algName){
      countAlgInfosByAlgName(algName).then(res => {
        this.pageHelper.totalSize = res;
      });
    },

  }
};
</script>

<style lang="less" scoped>
</style>
