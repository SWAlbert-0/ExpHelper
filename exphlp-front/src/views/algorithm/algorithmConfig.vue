<template>
  <div class="app-container">
    <el-alert
      title="算法接入建议：先导入算法元数据，再上传源码构建，最后进入执行计划做执行前检查。"
      type="info"
      :closable="false"
      show-icon
      class="algo-page-alert"
    />
    <div class="algo-guide-grid">
      <div class="guide-card">
        <div class="guide-index">1</div>
        <div class="guide-main">
          <div class="guide-title">录入算法</div>
          <div class="guide-desc">添加/JSON导入算法，确保 serviceName 与 Nacos 注册名一致。</div>
        </div>
      </div>
      <div class="guide-card">
        <div class="guide-index">2</div>
        <div class="guide-main">
          <div class="guide-title">上传源码</div>
          <div class="guide-desc">上传 zip（根目录含 exphlp-alg.json），运行时需与算法库一致。</div>
        </div>
      </div>
      <div class="guide-card">
        <div class="guide-index">3</div>
        <div class="guide-main">
          <div class="guide-title">构建并联调</div>
          <div class="guide-desc">构建成功后到执行计划管理做执行检查，通过后再执行计划。</div>
        </div>
      </div>
    </div>
    <div class="toolbar-row">
      <div class="toolbar-left">
        <el-button type="success" icon="el-icon-plus" @click="openAlgInfoAddForm()">添加</el-button>
        <el-button type="primary" icon="el-icon-upload2" @click="openImportDialog()">JSON导入</el-button>
        <el-button type="default" icon="el-icon-document" @click="openManualDialog">查看操作手册</el-button>
        <span class="toolbar-split"></span>
        <el-button type="danger" icon="el-icon-delete" :loading="batchDeleteLoading" @click="deleteBatch()">批量删除</el-button>
      </div>
      <div class="toolbar-right">
        <el-input v-model="algName" placeholder="请输入算法名称" clearable class="toolbar-search-input"/>
        <el-button type="primary" icon="el-icon-search" @click="pageHelper.currentPageNum = 1, getByAlgName()">查询</el-button>
        <el-button type="default" icon="el-icon-refresh" @click="back()">刷新</el-button>
      </div>
    </div>

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
      <el-table-column prop="runtimeType" label="运行时" width="120" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.runtimeType === 'python' ? 'success' : 'info'">
            {{ scope.row.runtimeType === "python" ? "Python" : "Java" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="算法描述" width="300" align="center"></el-table-column>
      <el-table-column label="参数" align="center">
        <template slot-scope="scope">
          <el-button  type="text" size="mini" @click="openDefParasTable(scope.row)">参数列表</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="340">
        <template slot-scope="scope">
          <el-button type="warning" size="mini" icon="el-icon-upload2" @click="openSourceUploadDialog(scope.row)">源码</el-button>
          <el-button type="primary" size="mini" icon="el-icon-edit" :disabled="!canManageAlg(scope.row)" @click="openAlgInfoUpdateForm(scope.row)">编辑</el-button>
          <el-button
            type="danger"
            size="mini"
            icon="el-icon-delete"
            :loading="!!deletingAlgIds[scope.row.algId]"
            :disabled="!!deletingAlgIds[scope.row.algId] || !canManageAlg(scope.row)"
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
        <el-form-item label="运行时" prop="runtimeType">
          <el-select v-model="algInfoAddForm.runtimeType" style="width: 100%;">
            <el-option v-for="item in runtimeTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
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
        <el-form-item label="运行时" prop="runtimeType">
          <el-select v-model="algInfoUpdateForm.runtimeType" style="width: 100%;">
            <el-option v-for="item in runtimeTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
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
        placeholder='示例：[{"algName":"nsga2-zdt1-ls","serviceName":"nsga2-zdt1-ls","runtimeType":"java","description":"demo","defParas":[{"paraName":"nVars","paraType":"int","paraValue":"100","description":"变量维度"}]}]'
      ></el-input>
      <div style="margin-top: 12px; text-align: right;">
        <el-button @click="dialogImportVisible=false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImportJson">开始导入</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="源码上传与构建"
      :visible.sync="dialogSourceUploadVisible"
      width="55%"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="`当前算法：${sourceUploadForm.algName || '--'}（${sourceUploadForm.runtimeType || 'java'}）`"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px;"
      />
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px;">
        <el-descriptions-item label="运行容器">{{ sourceUploadForm.runtimeInfo.containerName || "--" }}</el-descriptions-item>
        <el-descriptions-item label="容器状态">
          <el-tag size="mini" :type="sourceUploadForm.runtimeInfo.containerRunning ? 'success' : 'info'">
            {{ sourceUploadForm.runtimeInfo.containerStatus || (sourceUploadForm.runtimeInfo.containerExists ? "存在(未运行)" : "未创建") }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="镜像">{{ sourceUploadForm.runtimeInfo.containerImage || sourceUploadForm.runtimeInfo.imageName || "--" }}</el-descriptions-item>
        <el-descriptions-item label="Nacos健康实例">{{ sourceUploadForm.runtimeInfo.nacosHealthyCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近构建状态">{{ sourceUploadForm.runtimeInfo.taskStatus || "--" }}</el-descriptions-item>
        <el-descriptions-item label="端口映射">{{ sourceUploadForm.runtimeInfo.containerPorts || "--" }}</el-descriptions-item>
        <el-descriptions-item label="最近上传时间">{{ formatTimestamp(sourceUploadForm.runtimeInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="最近启动时间">{{ formatTimestamp(sourceUploadForm.runtimeInfo.startedAt) }}</el-descriptions-item>
        <el-descriptions-item label="最近结束时间">{{ formatTimestamp(sourceUploadForm.runtimeInfo.finishedAt) }}</el-descriptions-item>
        <el-descriptions-item label="迁移信息">{{ sourceUploadForm.runtimeInfo.migrationText || "--" }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="sourceUploadForm.runtimeInfo.message"
        :title="sourceUploadForm.runtimeInfo.message"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 10px;"
      />
      <el-alert
        v-if="!sourceUploadForm.runtimeInfo.canOperate"
        title="当前账号仅可查看运行信息，无上传/运维权限。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px;"
      />
      <el-upload
        class="upload-demo"
        drag
        action=""
        :auto-upload="false"
        :show-file-list="true"
        :file-list="sourceUploadForm.fileList"
        :on-change="onSourceFileChange"
        :on-remove="onSourceFileRemove"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将 zip 源码包拖到此处，或<em>点击上传</em></div>
        <div slot="tip" class="el-upload__tip">必须包含 exphlp-alg.json</div>
      </el-upload>
      <div style="margin-top: 12px;">
        <el-button type="primary" :loading="sourceUploadLoading" :disabled="!sourceUploadForm.runtimeInfo.canOperate" @click="submitSourceUpload">上传源码</el-button>
        <el-button type="success" :loading="buildStartLoading" :disabled="!sourceUploadForm.taskId || !sourceUploadForm.runtimeInfo.canOperate" @click="startBuildTask">构建并启动</el-button>
        <el-button type="warning" :loading="runtimeOperateLoading.OFFLINE" :disabled="!sourceUploadForm.runtimeInfo.canOffline" @click="operateRuntime('OFFLINE')">下线</el-button>
        <el-button type="success" :loading="runtimeOperateLoading.ONLINE" :disabled="!sourceUploadForm.runtimeInfo.canOnline" @click="operateRuntime('ONLINE')">上线</el-button>
        <el-button type="primary" :loading="runtimeOperateLoading.RESTART" :disabled="!sourceUploadForm.runtimeInfo.canRestart" @click="operateRuntime('RESTART')">重启</el-button>
        <el-button type="danger" :loading="runtimeOperateLoading.PRUNE_IMAGES" :disabled="!sourceUploadForm.runtimeInfo.canPruneImages" @click="operateRuntime('PRUNE_IMAGES')">清理旧资源</el-button>
        <el-button type="default" :loading="runtimeInfoLoading" @click="refreshSourcePanel(false)">刷新</el-button>
      </div>
      <el-descriptions v-if="sourceUploadForm.taskId" :column="2" border style="margin-top: 14px;">
        <el-descriptions-item label="任务ID">{{ sourceUploadForm.taskId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ sourceUploadForm.status || "--" }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ sourceUploadForm.phase || "--" }}</el-descriptions-item>
        <el-descriptions-item label="错误码">{{ sourceUploadForm.errorCode || "--" }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ sourceUploadForm.errorMessage || "--" }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="sourceUploadForm.fixHints && sourceUploadForm.fixHints.length > 0"
        :title="`修复建议：${sourceUploadForm.fixHints[0]}`"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 10px;"
      />
      <el-input
        v-if="sourceUploadForm.contractCheckText"
        type="textarea"
        :rows="4"
        v-model="sourceUploadForm.contractCheckText"
        readonly
        style="margin-top: 10px;"
      />
      <el-input
        v-if="sourceUploadForm.taskId"
        type="textarea"
        :rows="12"
        v-model="sourceUploadForm.logs"
        readonly
        style="margin-top: 12px;"
      />
    </el-dialog>
    <manual-doc-dialog
      :visible.sync="manualDialogVisible"
      :page-key="manualPageKey"
    />
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
  uploadAlgSource,
  triggerAlgBuild,
  getAlgBuildLogs,
  getAlgSourceRuntimeInfo,
  operateAlgSourceRuntime,
} from "@/api/exphlp/algLibMgr";
import { normalizeAlgorithmImportJson } from "@/utils/jsonImportNormalizer";
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
    }
  },

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
      dialogSourceUploadVisible: false,
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
      sourceUploadLoading: false,
      buildStartLoading: false,
      runtimeInfoLoading: false,
      sourceRuntimeTimer: null,
      runtimeOperateLoading: {
        OFFLINE: false,
        ONLINE: false,
        RESTART: false,
        PRUNE_IMAGES: false,
      },
      sourceUploadForm: {
        algId: "",
        algName: "",
        runtimeType: "java",
        file: null,
        fileList: [],
        taskId: "",
        status: "",
        phase: "",
        errorCode: "",
        errorMessage: "",
        fixHints: [],
        contractCheckText: "",
        logs: "",
        runtimeInfo: {
          hasBuildTask: false,
          containerExists: false,
          containerRunning: false,
          containerName: "",
          containerStatus: "",
          containerImage: "",
          containerPorts: "",
          imageName: "",
          taskStatus: "",
          nacosHealthyCount: 0,
          createdAt: 0,
          startedAt: 0,
          finishedAt: 0,
          canOffline: false,
          canOnline: false,
          canRestart: false,
          canPruneImages: false,
          canOperate: false,
          migrationText: "",
          message: "",
        },
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
        runtimeType: "java",
        defParas:[],
        description:"",
      },
      algInfoUpdateForm:{
        algName:"",
        serviceName:'',
        runtimeType: "java",
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
      runtimeTypeOptions: [
        { value: "java", label: "Java" },
        { value: "python", label: "Python" },
      ],
      manualDialogVisible: false,
      manualPageKey: "algorithm",
      algInfoFormRules: {
        algName: [
          { required: true, message: '请输入算法名称', trigger: 'blur' },
        ],
        serviceName:[
          { required: true, message: '请输入服务名', trigger: 'blur' },
        ],
        runtimeType:[
          { required: true, message: '请选择运行时', trigger: 'change' },
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
  watch: {
    dialogSourceUploadVisible(val) {
      if (val) {
        this.startSourceRuntimeAutoRefresh();
      } else {
        this.stopSourceRuntimeAutoRefresh();
      }
    },
  },
  beforeDestroy() {
    this.stopSourceRuntimeAutoRefresh();
  },
  methods: {
    canManageAlg(row) {
      if (this.isAdmin) {
        return true;
      }
      if (!row) {
        return false;
      }
      const ownerUserName = (row.ownerUserName || "").toString().trim();
      if (ownerUserName) {
        return ownerUserName === (this.name || "").toString().trim();
      }
      return false;
    },
    openManualDialog() {
      this.manualPageKey = "algorithm";
      this.manualDialogVisible = true;
    },
    getAlgInfos(){
      getAlgs(this.pageHelper.currentPageNum,this.pageHelper.pageSize).then(res => {
        this.algInfo = this.normalizeAlgRows(res);
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
        this.algInfo = this.normalizeAlgRows(res);
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
    openSourceUploadDialog(row) {
      this.sourceUploadForm = {
        algId: row.algId,
        algName: row.algName,
        runtimeType: this.normalizeRuntimeType(row.runtimeType),
        file: null,
        fileList: [],
        taskId: "",
        status: "",
        phase: "",
        errorCode: "",
        errorMessage: "",
        fixHints: [],
        contractCheckText: "",
        logs: "",
        runtimeInfo: {
          hasBuildTask: false,
          containerExists: false,
          containerRunning: false,
          containerName: row.containerName || "",
          containerStatus: "",
          containerImage: "",
          containerPorts: "",
          imageName: "",
          taskStatus: "",
          nacosHealthyCount: 0,
          createdAt: 0,
          startedAt: 0,
          finishedAt: 0,
          canOffline: false,
          canOnline: false,
          canRestart: false,
          canPruneImages: false,
          migrationText: "",
          canOperate: false,
          message: "",
        },
      };
      this.dialogSourceUploadVisible = true;
      this.refreshSourcePanel(true);
    },
    onSourceFileChange(file, fileList) {
      this.sourceUploadForm.file = file && file.raw ? file.raw : null;
      this.sourceUploadForm.fileList = fileList.slice(-1);
    },
    onSourceFileRemove() {
      this.sourceUploadForm.file = null;
      this.sourceUploadForm.fileList = [];
    },
    submitSourceUpload() {
      if (!this.sourceUploadForm.algId) {
        this.$message({ type: "warning", message: "算法ID为空，无法上传" });
        return;
      }
      if (!this.sourceUploadForm.file) {
        this.$message({ type: "warning", message: "请先选择 zip 源码包" });
        return;
      }
      this.sourceUploadLoading = true;
      uploadAlgSource(this.sourceUploadForm.algId, this.sourceUploadForm.file).then((res) => {
        const task = res && res.data ? res.data : {};
        this.applyBuildTask(task);
        this.$message({ type: "success", message: "源码上传成功，可开始构建" });
      }).catch((error) => {
        const data = error && error.response && error.response.data ? error.response.data.data : null;
        const fixHints = data && Array.isArray(data.fixHints) ? data.fixHints : [];
        const message = this.extractErrorMessage(error, "源码上传失败");
        if (fixHints.length > 0) {
          this.sourceUploadForm.fixHints = fixHints;
          this.sourceUploadForm.contractCheckText = `校验阶段: ${data.phase || "VALIDATE"}\n` + fixHints.map((item, index) => `${index + 1}. ${item}`).join("\n");
        }
        this.$message({ type: "error", message });
      }).finally(() => {
        this.sourceUploadLoading = false;
      });
    },
    startBuildTask() {
      if (!this.sourceUploadForm.taskId) {
        this.$message({ type: "warning", message: "请先上传源码" });
        return;
      }
      this.buildStartLoading = true;
      triggerAlgBuild(this.sourceUploadForm.taskId).then((res) => {
        const task = res && res.data ? res.data : {};
        this.applyBuildTask(task);
        this.$message({ type: "success", message: "构建任务已启动" });
        this.refreshSourcePanel(false);
      }).catch((error) => {
        const message = this.extractErrorMessage(error, "构建启动失败");
        this.$message({ type: "error", message });
      }).finally(() => {
        this.buildStartLoading = false;
      });
    },
    refreshSourcePanel(silent) {
      this.fetchSourceRuntimeInfo(this.sourceUploadForm.algId, { silent: !!silent });
    },
    fetchSourceRuntimeInfo(algId, options = {}) {
      if (!algId) {
        return;
      }
      const silent = !!options.silent;
      this.runtimeInfoLoading = true;
      getAlgSourceRuntimeInfo(algId).then((res) => {
        const info = res && res.data ? res.data : {};
        this.sourceUploadForm.runtimeInfo = {
          hasBuildTask: !!info.hasBuildTask,
          containerExists: !!info.containerExists,
          containerRunning: !!info.containerRunning,
          containerName: info.containerName || "",
          containerStatus: info.containerStatus || "",
          containerImage: info.containerImage || "",
          containerPorts: info.containerPorts || "",
          imageName: info.imageName || "",
          taskStatus: info.taskStatus || "",
          nacosHealthyCount: Number(info.nacosHealthyCount || 0),
          createdAt: Number(info.createdAt || 0),
          startedAt: Number(info.startedAt || 0),
          finishedAt: Number(info.finishedAt || 0),
          canOffline: !!info.canOffline,
          canOnline: !!info.canOnline,
          canRestart: !!info.canRestart,
          canPruneImages: !!info.canPruneImages,
          canOperate: !!info.canOperate,
          migrationText: this.resolveMigrationText(info.migrationInfo),
          message: info.message || "",
        };
        if (info.taskId) {
          this.sourceUploadForm.taskId = info.taskId;
          this.sourceUploadForm.status = info.taskStatus || "";
          this.sourceUploadForm.phase = info.taskPhase || "";
          this.sourceUploadForm.errorCode = info.taskErrorCode || "";
          this.sourceUploadForm.errorMessage = info.taskErrorMessage || "";
          this.loadBuildLogs(info.taskId);
        } else {
          this.sourceUploadForm.taskId = "";
          this.sourceUploadForm.status = "";
          this.sourceUploadForm.phase = "";
          this.sourceUploadForm.errorCode = "";
          this.sourceUploadForm.errorMessage = "";
          this.sourceUploadForm.logs = "";
        }
      }).catch((error) => {
        if (!silent) {
          const message = this.extractErrorMessage(error, "读取运行信息失败");
          this.$message({ type: "warning", message });
        }
      }).finally(() => {
        this.runtimeInfoLoading = false;
      });
    },
    loadBuildLogs(taskId) {
      if (!taskId) {
        this.sourceUploadForm.logs = "";
        return;
      }
      getAlgBuildLogs(taskId, 300).then((res) => {
        this.sourceUploadForm.logs = res && res.data ? (res.data.logs || "") : "";
      });
    },
    startSourceRuntimeAutoRefresh() {
      this.stopSourceRuntimeAutoRefresh();
      this.sourceRuntimeTimer = setInterval(() => {
        if (!this.dialogSourceUploadVisible) {
          return;
        }
        this.refreshSourcePanel(true);
      }, 10000);
    },
    stopSourceRuntimeAutoRefresh() {
      if (this.sourceRuntimeTimer) {
        clearInterval(this.sourceRuntimeTimer);
        this.sourceRuntimeTimer = null;
      }
    },
    operateRuntime(action) {
      if (!this.sourceUploadForm.algId || !action) {
        return;
      }
      this.$set(this.runtimeOperateLoading, action, true);
      operateAlgSourceRuntime(this.sourceUploadForm.algId, action).then((res) => {
        const op = res && res.data ? res.data.operation : null;
        const detail = this.formatRuntimeOperateMessage(action, op);
        this.$message({ type: "success", message: detail || ((op && op.message) || "操作成功") });
        this.refreshSourcePanel(true);
      }).catch((error) => {
        const message = this.extractErrorMessage(error, "操作失败");
        this.$message({ type: "error", message });
      }).finally(() => {
        this.$set(this.runtimeOperateLoading, action, false);
      });
    },
    formatRuntimeOperateMessage(action, op) {
      if (!op || typeof op !== "object") {
        return "";
      }
      if (action !== "PRUNE_IMAGES") {
        return op.message || "";
      }
      const removedContainers = Number(op.removedContainers || 0);
      const removedImages = Number(op.removedImages || 0);
      const skipped = Array.isArray(op.skippedRunningContainers) ? op.skippedRunningContainers.length : 0;
      return `旧资源清理完成：容器${removedContainers}个，镜像${removedImages}个，跳过运行中${skipped}个`;
    },
    resolveMigrationText(migrationInfo) {
      if (!migrationInfo || typeof migrationInfo !== "object" || Object.keys(migrationInfo).length === 0) {
        return "";
      }
      if (migrationInfo.migrated) {
        return `已迁移: ${migrationInfo.from || "-"} -> ${migrationInfo.to || "-"}`;
      }
      if (migrationInfo.error) {
        return `迁移失败: ${migrationInfo.error}`;
      }
      return "";
    },
    formatTimestamp(ts) {
      const value = Number(ts || 0);
      if (!value || value <= 0) {
        return "--";
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return "--";
      }
      const pad = (n) => (n < 10 ? `0${n}` : `${n}`);
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
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
        this.algInfo = this.normalizeAlgRows(res);
        this.countAlgInfosByAlgName(this.algName);
      });
    },
    //
    openAlgInfoAddForm(){
      this.algInfoAddForm = {
        algName:"",
        serviceName: '',
        runtimeType: "java",
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
      if (!this.canManageAlg(row)) {
        this.$message({ type: "warning", message: "仅可编辑自己创建的算法" });
        return;
      }
      this.algInfoUpdateForm = Object.assign({}, row, {
        runtimeType: this.normalizeRuntimeType(row && row.runtimeType),
      });
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
      const row = (this.algInfo || []).find((item) => item.algId === algId);
      if (row && !this.canManageAlg(row)) {
        this.$message({ type: "warning", message: "仅可删除自己创建的算法" });
        return;
      }
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
            const selected = this.multipleSelection.filter((item) => this.canManageAlg(item));
            if (selected.length === 0) {
              this.batchDeleteLoading = false;
              this.$message({ type: "warning", message: "仅可批量删除自己创建的算法" });
              return;
            }
            const deleteTasks = selected.map((item) => deleteAlgById(item.algId));
            Promise.allSettled(deleteTasks).then((results) => {
              let deletedCount = 0;
              let noopCount = 0;
              let blockedCount = 0;
              let repairedCount = 0;
              const blockedAlgIds = [];
              const failedAlgIds = [];
              for (let i = 0; i < results.length; i++) {
                const current = results[i];
                const currentAlgId = selected[i] && selected[i].algId ? selected[i].algId : "";
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
    normalizeRuntimeType(runtimeType) {
      const text = (runtimeType || "").toString().trim().toLowerCase();
      return text === "python" ? "python" : "java";
    },
    normalizeAlgRows(rows) {
      if (!Array.isArray(rows)) {
        return [];
      }
      return rows.map((row) => Object.assign({}, row, {
        runtimeType: this.normalizeRuntimeType(row && row.runtimeType),
      }));
    },
    applyBuildTask(task) {
      const nextTask = task || {};
      this.sourceUploadForm.taskId = nextTask.taskId || this.sourceUploadForm.taskId || "";
      this.sourceUploadForm.status = nextTask.status || "";
      this.sourceUploadForm.phase = nextTask.phase || "";
      this.sourceUploadForm.errorCode = nextTask.errorCode || "";
      this.sourceUploadForm.errorMessage = nextTask.errorMessage || "";
      this.sourceUploadForm.fixHints = Array.isArray(nextTask.fixHints) ? nextTask.fixHints : [];
      const contractCheck = nextTask.contractCheck || null;
      if (contractCheck && typeof contractCheck === "object") {
        const fields = [];
        if (contractCheck.metaEntryName) {
          fields.push(`meta文件: ${contractCheck.metaEntryName}`);
        }
        if (contractCheck.runtimeType || contractCheck.metaRuntimeType) {
          fields.push(`runtime: 算法库=${contractCheck.runtimeType || "-"} / 源码包=${contractCheck.metaRuntimeType || "-"}`);
        }
        if (contractCheck.algServiceName || contractCheck.metaServiceName) {
          fields.push(`serviceName: 算法库=${contractCheck.algServiceName || "-"} / 源码包=${contractCheck.metaServiceName || "-"}`);
        }
        if (contractCheck.metaPort) {
          fields.push(`port: ${contractCheck.metaPort}`);
        }
        if (contractCheck.metaEntry) {
          fields.push(`entry: ${contractCheck.metaEntry}`);
        }
        const errors = Array.isArray(contractCheck.errors) ? contractCheck.errors : [];
        if (errors.length > 0) {
          fields.push(`错误: ${errors.join("；")}`);
        }
        this.sourceUploadForm.contractCheckText = fields.join("\n");
      } else {
        this.sourceUploadForm.contractCheckText = "";
      }
    },

  }
};
</script>

<style lang="less" scoped>
.algo-page-alert {
  margin-bottom: 12px;
}
.algo-guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(200px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.guide-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e8eef7;
  border-radius: 8px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
}
.guide-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  line-height: 24px;
  text-align: center;
  font-weight: 600;
  flex-shrink: 0;
}
.guide-main {
  min-width: 0;
}
.guide-title {
  font-size: 13px;
  font-weight: 600;
  color: #2f3b52;
  margin-bottom: 4px;
}
.guide-desc {
  font-size: 12px;
  color: #5f6b7f;
  line-height: 1.45;
}
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.toolbar-right {
  margin-left: auto;
}
.toolbar-split {
  width: 1px;
  height: 24px;
  background: #e4e7ed;
}
.toolbar-search-input {
  width: 240px;
}
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
@media (max-width: 1200px) {
  .algo-guide-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-right {
    margin-left: 0;
  }
}
</style>
