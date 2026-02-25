<template>
  <div class="app-container">
    <el-alert
      title="执行建议：算法服务名必须与 Nacos 注册名一致。完成后可到“执行计划管理”使用执行向导自动检查并执行。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px;"
    />
    <!--添加 批量删除 查询-->
    <el-row>
      <el-col :span="2">
        <el-button type="success" icon="el-icon-plus" @click="openAlgInfoAddForm()">添加</el-button>
      </el-col>
      <el-col :span="2">
        <el-button type="primary" icon="el-icon-upload2" @click="openImportDialog()">JSON导入</el-button>
      </el-col>
      <el-col :span="2">
        <el-button type="danger" icon="el-icon-delete" :loading="batchDeleteLoading" @click="deleteBatch()">批量删除</el-button>
      </el-col>
      <el-col :span="18">
        <el-form :inline="true" class="demo-form-inline" align="center">
          <el-form-item>
            <el-input v-model="algName" placeholder="请输入算法名称" clearable/>
          </el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getByAlgName()">查询</el-button>
          <el-button type="default" icon="el-icon-refresh" @click="back()">刷新</el-button>
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
          <el-button
            type="danger"
            size="mini"
            icon="el-icon-delete"
            :loading="!!deletingAlgIds[scope.row.algId]"
            :disabled="!!deletingAlgIds[scope.row.algId]"
            @click="deleteAlgInfo(scope.row.algId)"
          >删除</el-button>
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
        <el-form-item label="服务名" prop="serviceName">
          <el-input v-model="algInfoAddForm.serviceName" placeholder="服务名"></el-input>
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
        <el-form-item label="服务名" prop="serviceName">
          <el-input v-model="algInfoUpdateForm.serviceName"></el-input>
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
    <el-dialog
      title="算法 JSON 导入"
      :visible.sync="dialogImportVisible"
      width="50%"
      align="center"
      :close-on-click-modal="false"
    >
      <el-alert
        title="支持拖拽 .json 文件或粘贴JSON。系统会自动兼容 algorithmName/service 等旧字段。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px;"
      />
      <div
        class="import-dropzone"
        @click="triggerImportFileSelect"
        @dragover.prevent
        @drop.prevent="handleImportFileDrop"
      >
        <i class="el-icon-upload2"></i>
        <span>{{ importFileName ? `已选择: ${importFileName}` : "拖拽 JSON 文件到此处，或点击选择文件" }}</span>
      </div>
      <input
        ref="importFileInput"
        type="file"
        accept=".json,application/json"
        class="import-file-input"
        @change="handleImportFileChange"
      />
      <el-alert
        v-if="importSummary.total > 0"
        :title="`解析 ${importSummary.total} 项，标准化 ${importSummary.normalized} 项，兼容映射 ${importSummary.legacyMappedCount} 项`"
        :type="importSummary.errors.length > 0 ? 'error' : 'success'"
        :closable="false"
        show-icon
        style="margin: 10px 0;"
      />
      <el-alert
        v-if="importSummary.warnings.length > 0"
        :title="`告警：${importSummary.warnings[0]}`"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 10px;"
      />
      <el-input
        type="textarea"
        :rows="14"
        v-model="importJsonText"
        placeholder='示例：[{"algName":"nsga2-zdt1-ls","serviceName":"nsga2-zdt1-ls","description":"demo","defParas":[{"paraName":"nVars","paraType":"int","paraValue":"100","description":"变量维度"}]}]'
      ></el-input>
      <div style="margin-top: 12px; text-align: right;">
        <el-button @click="dialogImportVisible=false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImportJson">开始导入</el-button>
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
  importAlgsJson,
} from "@/api/exphlp/algLibMgr";
import { normalizeAlgorithmImportJson } from "@/utils/jsonImportNormalizer";

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
      dialogImportVisible: false,
      importJsonText: "",
      importLoading: false,
      importFileName: "",
      importSummary: {
        total: 0,
        normalized: 0,
        legacyMappedCount: 0,
        warnings: [],
        errors: [],
      },
      algName: '',
      multipleSelection: [],
      batchDeleteLoading: false,
      deletingAlgIds: {},
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
          value: "long",
          label: "long",
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
    refreshCurrentList() {
      if (this.algName != "") {
        this.getByAlgName();
      } else {
        this.getAlgInfos();
      }
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
    openImportDialog() {
      this.importJsonText = "";
      this.importFileName = "";
      this.importSummary = {
        total: 0,
        normalized: 0,
        legacyMappedCount: 0,
        warnings: [],
        errors: [],
      };
      this.dialogImportVisible = true;
    },
    triggerImportFileSelect() {
      if (!this.$refs.importFileInput) {
        return;
      }
      this.$refs.importFileInput.click();
    },
    handleImportFileDrop(event) {
      const file = event && event.dataTransfer && event.dataTransfer.files
        ? event.dataTransfer.files[0]
        : null;
      this.readImportFile(file);
    },
    handleImportFileChange(event) {
      const file = event && event.target && event.target.files ? event.target.files[0] : null;
      this.readImportFile(file);
      if (this.$refs.importFileInput) {
        this.$refs.importFileInput.value = "";
      }
    },
    readImportFile(file) {
      if (!file) {
        return;
      }
      if (!/\.json$/i.test(file.name)) {
        this.$message({ type: "warning", message: "仅支持 .json 文件" });
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.applyImportJsonText(reader.result || "", file.name);
      };
      reader.onerror = () => {
        this.$message({ type: "error", message: "读取文件失败，请重试" });
      };
      reader.readAsText(file, "utf-8");
    },
    applyImportJsonText(rawText, fileName) {
      try {
        const normalized = normalizeAlgorithmImportJson(rawText);
        this.importJsonText = normalized.jsonText;
        this.importSummary = normalized.summary;
        this.importFileName = fileName || "";
        if (normalized.summary.errors.length > 0) {
          this.$message({ type: "warning", message: `文件解析完成，但存在 ${normalized.summary.errors.length} 条错误` });
          return;
        }
        this.$message({ type: "success", message: "JSON 已标准化，可直接导入" });
      } catch (error) {
        this.importSummary = {
          total: 0,
          normalized: 0,
          legacyMappedCount: 0,
          warnings: [],
          errors: [error.message],
        };
        this.$message({ type: "error", message: error.message || "JSON解析失败" });
      }
    },
    submitImportJson() {
      if (this.importLoading) {
        return;
      }
      if (!this.importJsonText || !this.importJsonText.trim()) {
        this.$message({ type: "warning", message: "请先拖拽文件或粘贴JSON内容" });
        return;
      }
      let importText = this.importJsonText;
      try {
        const normalized = normalizeAlgorithmImportJson(this.importJsonText);
        this.importJsonText = normalized.jsonText;
        this.importSummary = normalized.summary;
        importText = normalized.jsonText;
      } catch (error) {
        this.$message({ type: "error", message: error.message || "JSON解析失败" });
        return;
      }
      if (this.importSummary.errors.length > 0) {
        this.$message({ type: "error", message: `存在 ${this.importSummary.errors.length} 条错误，请修正后再导入` });
        return;
      }
      this.importLoading = true;
      importAlgsJson(importText).then((res) => {
        const data = res && res.data ? res.data : {};
        const total = Number(data.total || 0);
        const success = Number(data.success || 0);
        const skipped = Number(data.skipped || 0);
        const failed = Number(data.failed || 0);
        this.$message({
          type: failed > 0 ? "warning" : "success",
          message: `导入完成：总计 ${total}，成功 ${success}，跳过 ${skipped}，失败 ${failed}`,
        });
        this.dialogImportVisible = false;
        this.refreshCurrentList();
      }).catch((error) => {
        const msg = error && error.response && error.response.data
          ? (error.response.data.msg || error.response.data.message || "导入失败")
          : "导入失败";
        this.$message({ type: "error", message: msg });
      }).finally(() => {
        this.importLoading = false;
      });
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
          this.$set(this.deletingAlgIds, algId, true);
          deleteAlgById(algId).then((res) => {
            const state = this.extractDeleteState(res, algId);
            if (state.deletedCount > 0) {
              const repairedText = state.repaired ? "，已自动修复历史数据" : "";
              this.$message({type: "success", message: `删除成功（algId=${algId}${repairedText}）`,});
            } else if (state.noop) {
              this.$message({type: "info", message: `记录已不存在（algId=${algId}），列表已同步`,});
            } else {
              this.$message({type: "error", message: `删除失败（algId=${algId}）：后端未确认删除`,});
            }
          }).catch((error) => {
            this.$message({type: "error", message: this.extractDeleteErrorMessage(error, algId),});
          }).finally(() => {
            this.$delete(this.deletingAlgIds, algId);
            this.refreshCurrentList();
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
            this.batchDeleteLoading = true;
            const deleteTasks = this.multipleSelection.map((item) => deleteAlgById(item.algId));
            Promise.allSettled(deleteTasks).then((results) => {
              let deletedCount = 0;
              let noopCount = 0;
              let blockedCount = 0;
              let repairedCount = 0;
              const blockedAlgIds = [];
              const failedAlgIds = [];
              for (let i = 0; i < results.length; i++) {
                const current = results[i];
                const currentAlgId = this.multipleSelection[i] && this.multipleSelection[i].algId ? this.multipleSelection[i].algId : "";
                if (current.status === "fulfilled") {
                  const state = this.extractDeleteState(current.value, currentAlgId);
                  if (state.deletedCount > 0) {
                    deletedCount += 1;
                    if (state.repaired) {
                      repairedCount += 1;
                    }
                  } else if (state.noop) {
                    noopCount += 1;
                  } else {
                    failedAlgIds.push(currentAlgId);
                  }
                } else {
                  if (this.isDeleteBlockedError(current.reason)) {
                    blockedCount += 1;
                    blockedAlgIds.push(currentAlgId);
                  } else {
                    failedAlgIds.push(currentAlgId);
                  }
                }
              }

              const totalSuccess = deletedCount + noopCount;
              if (blockedCount === 0 && failedAlgIds.length === 0) {
                const repairedText = repairedCount > 0 ? `，自动修复 ${repairedCount} 条` : "";
                const noopText = noopCount > 0 ? `，已同步 ${noopCount} 条不存在记录` : "";
                this.$message({type: "success", message: `删除完成：成功删除 ${deletedCount} 条${repairedText}${noopText}`,});
              } else if (totalSuccess === 0 && blockedCount > 0 && failedAlgIds.length === 0) {
                this.$message({type: "warning", message: `删除被阻止：${this.formatFailedIds(blockedAlgIds)} 仍被执行计划引用`,});
              } else {
                const repairedText = repairedCount > 0 ? `，自动修复 ${repairedCount} 条` : "";
                const noopText = noopCount > 0 ? `，同步不存在 ${noopCount} 条` : "";
                const blockedText = blockedCount > 0 ? `，阻止 ${blockedCount} 条（${this.formatFailedIds(blockedAlgIds)}）` : "";
                const failedText = failedAlgIds.length > 0 ? `，失败 ${failedAlgIds.length} 条（${this.formatFailedIds(failedAlgIds)}）` : "";
                this.$message({type: "warning", message: `批量删除完成：删除 ${deletedCount} 条${repairedText}${noopText}${blockedText}${failedText}`,});
              }
            }).finally(() => {
              this.batchDeleteLoading = false;
              this.refreshCurrentList();
            });
          })
          .catch(() => {
            this.batchDeleteLoading = false;
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
    extractDeletedCount(res) {
      if (!res || !res.data) {
        return 0;
      }
      const raw = res.data.deletedCount;
      if (typeof raw === "number") {
        return raw;
      }
      const parsed = Number(raw);
      return Number.isNaN(parsed) ? 0 : parsed;
    },
    isRepairedDelete(res) {
      return !!(res && res.data && res.data.repaired === true);
    },
    isNoopDelete(res) {
      return !!(res && res.data && res.data.noop === true);
    },
    extractDeleteState(res, algId) {
      const deletedCount = this.extractDeletedCount(res);
      const repaired = this.isRepairedDelete(res);
      const verified = !(res && res.data && res.data.verified === false);
      const noop = this.isNoopDelete(res) && verified;
      return { algId: algId, deletedCount: deletedCount, repaired: repaired, noop: noop, verified: verified };
    },
    extractBackendErrorCode(error) {
      return error && error.response && error.response.data ? error.response.data.errorCode : "";
    },
    extractErrorMessage(error, fallback) {
      if (!error) {
        return fallback;
      }
      const responseMessage = error.response && error.response.data && (error.response.data.msg || error.response.data.message);
      return responseMessage || error.message || fallback;
    },
    isDeleteBlockedError(error) {
      const code = this.extractBackendErrorCode(error);
      if (code === "ALG_IN_USE") {
        return true;
      }
      const message = this.extractErrorMessage(error, "");
      return message.includes("算法已被执行计划引用");
    },
    extractDeleteErrorMessage(error, algId) {
      const fallback = `删除失败（algId=${algId}）：请稍后重试`;
      if (!error) {
        return fallback;
      }
      if (this.isDeleteBlockedError(error)) {
        const data = error.response && error.response.data ? error.response.data.data : null;
        const refCount = data && data.refPlanCount ? data.refPlanCount : 0;
        const planNames = data && Array.isArray(data.refPlanNames) ? data.refPlanNames.filter(Boolean) : [];
        if (refCount > 0) {
          const planText = planNames.length > 0 ? `（${planNames.join("、")}）` : "";
          return `删除失败（algId=${algId}）：仍被 ${refCount} 个执行计划引用${planText}`;
        }
        return `删除失败（algId=${algId}）：算法已被执行计划引用，请先解除关联`;
      }
      const message = this.extractErrorMessage(error, "");
      if (message.includes("删除未生效")) {
        return `删除失败（algId=${algId}）：后端未确认删除，请刷新后重试`;
      }
      return message ? `删除失败（algId=${algId}）：${message}` : fallback;
    },
    formatFailedIds(ids) {
      const validIds = (ids || []).filter((id) => !!id);
      if (validIds.length === 0) {
        return "失败记录ID未知";
      }
      if (validIds.length <= 3) {
        return `algId=${validIds.join("、")}`;
      }
      return `algId=${validIds.slice(0, 3).join("、")} 等 ${validIds.length} 条`;
    },

  }
};
</script>

<style lang="less" scoped>
.import-dropzone {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 72px;
  margin-bottom: 10px;
  border: 1px dashed #c0ccda;
  border-radius: 6px;
  color: #606266;
  cursor: pointer;
  background: #fafcff;
}
.import-dropzone:hover {
  border-color: #409eff;
  color: #409eff;
}
.import-file-input {
  display: none;
}
</style>
