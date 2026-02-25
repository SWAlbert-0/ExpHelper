<template>
  <div class="plan-manage-page">
    <div v-show="editVisible">
      <el-container class="plan-edit-container">
        <el-aside width="420px" class="plan-edit-aside">
          <div class="plan-edit-aside-body">
            <div class="panel-title">计划基础信息</div>
            <el-form :model="exePlan" ref="exePlan" label-width="100px" class="demo-dynamic plan-edit-form">
              <el-form-item prop="planName" label="计划名称"
                            :rules="{ required: true, message: '请输入计划名称', trigger: 'blur' }">
                <el-input v-model="exePlan.planName" placeholder="请输入计划名称" :readonly="!addFlag"></el-input>
              </el-form-item>
              <el-form-item label="问题实例" prop="probInstIds">
                <el-tree
                  :data="treeData"
                  show-checkbox
                  node-key="id"
                  ref="tree"
                  default-expand-all
                  highlight-current
                  :props="defaultProps">
                </el-tree>
              </el-form-item>
              <el-form-item>
                <router-link :to="{ path: '/index', query: { source: 'planManage' } }">
                  <el-button type="success">添加问题实例</el-button>
                </router-link>
              </el-form-item>
              <el-form-item label="计划状态" v-if="!addFlag">
                <el-input v-model="exePlan.exeState" placeholder="请输入计划名称" :readonly="!addFlag"></el-input>
              </el-form-item>
              <el-form-item label="开始时间">
                <el-input v-model="exePlan.exeStartTime" readonly></el-input>
              </el-form-item>
              <el-form-item label="结束时间">
                <el-input v-model="exePlan.exeEndTime" readonly></el-input>
              </el-form-item>
              <el-form-item label="计划描述" prop="description"
                            :rules="{ required: true, message: '请输入计划描述', trigger: 'blur' }">
                <el-input type="textarea" autosize v-model="exePlan.description"
                          placeholder="请输入计划描述"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="submitForm('exePlan')" v-if="addFlag == true">确 定</el-button>
                <el-button type="primary" @click="backToExePlanTable()">返回列表</el-button>
              </el-form-item>
              <div class="plan-edit-hint">
                请先确认基础信息，再在右侧完成算法配置与通知人选择。
              </div>
            </el-form>
          </div>
        </el-aside>
        <el-main class="plan-edit-main">
          <div v-if="rightVisible" class="plan-edit-content">
            <div class="right-card">
              <el-steps :active="active" finish-status="success" align-center>
                <el-step title="算法选择及配置"></el-step>
                <el-step title="通知人员与保存"></el-step>
              </el-steps>
            </div>
            <div v-show="this.active == 1 && rightVisible">
              <div class="panel-title">算法选择及配置</div>
              <el-alert
                title="说明：‘去算法库新建算法’用于创建新算法记录；‘加入计划’用于将当前下拉选中的算法加入本计划。"
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 12px;"
              />
              <el-row :gutter="12" class="alg-select-row">
                <el-col :span="6">
                  <router-link :to="{ path: '/algorithmConfig/index', query: { source: 'planManage' } }">
                    <el-button type="success">去算法库新建算法</el-button>
                  </router-link>
                </el-col>
                <el-col :span="14">
                  <el-form :inline="true" class="demo-form-inline" align="center">
                    <el-form-item label="算法">
                      <el-select
                        v-model="algId"
                        placeholder="请选择解决问题的算法"
                      >
                        <el-option
                          v-for="(selectItem, index) in algInfos"
                          :key="index"
                          :value="index"
                          :label="selectItem.algName"
                        ></el-option>
                      </el-select>
                    </el-form-item>
                  </el-form>
                </el-col>
                <el-col :span="4">
                  <el-button type="primary" @click="addAlgInfo()">加入计划</el-button>
                </el-col>
              </el-row>
              <br>
              <el-table
                :data="exePlan.algRunInfos"
                border
                fit
                highlight-current-row
              >
                <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
                <el-table-column prop="showedAlgName" label="算法名称" width="150" align="center"></el-table-column>
                <el-table-column prop="description" label="算法描述" align="center"></el-table-column>
                <el-table-column prop="runNum" label="运行次数" align="center"></el-table-column>
                <el-table-column label="操作" align="center" width="300">
                  <template slot-scope="scope">
                    <el-button size="mini" icon="el-icon-edit" @click="openRunParaTable(scope.row)">参数编辑</el-button>
                    <el-button type="primary" size="mini" icon="el-icon-edit" @click="openRunNumForm(scope.row)">编辑
                    </el-button>
                    <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteAlgInfo(scope.row)">删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <!--        -->
              <el-dialog
                title="编辑"
                :visible.sync="dialogRunNumFormVisible"
                width="30%"
                align="center"
                :close-on-click-modal="false"
              >
                <el-form :model="runNumForm" ref="runNumForm" label-width="80px">
                  <el-form-item label="运行次数" prop="runNum"
                                :rules="{required: true, message: '请输入运行次数', trigger: 'blur'}">
                    <el-input v-model="runNumForm.runNum" placeholder="请输入运行次数"></el-input>
                  </el-form-item>
                </el-form>
                <div>
                  <el-button @click="closeRunNumForm('runNumForm')">取 消</el-button>
                  <el-button type="primary" @click="submitRunNumForm('runNumForm')">确 定</el-button>
                </div>
              </el-dialog>
              <!--        -->
              <el-dialog
                title="参数列表"
                :visible.sync="dialogRunParaTableVisible"
                center
                width="60%"
                :before-close="beforeCloseRunParaTable"
                :close-on-click-modal="false"
              >
                <el-table :data="runParaTable" ref="runParaTable" border fit highlight-current-row>
                  <el-table-column property="paraId" label="序号" width="100" align="center"></el-table-column>
                  <el-table-column property="paraName" label="参数名" width="100" align="center"></el-table-column>
                  <el-table-column property="paraType" label="参数类型" width="100" align="center"></el-table-column>
                  <el-table-column property="paraValue" label="参数值" width="100" align="center"></el-table-column>
                  <el-table-column property="description" label="描述" align="center"></el-table-column>
                  <el-table-column label="操作" align="center" width="200">
                    <template slot-scope="scope">
                      <el-button type="primary" size="mini" icon="el-icon-edit" @click="openRunParaForm(scope.row)">编辑
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <!--        -->
                <el-dialog
                  width="30%"
                  center
                  title="编辑"
                  :visible.sync="dialogRunParaFormVisible"
                  append-to-body
                  :close-on-click-modal="false"
                >
                  <el-form :model="runParaForm" ref="runParaForm" label-width="80px">
                    <el-form-item label="参数值" prop="paraValue"
                                  :rules="{ required: true, message: '请输入参数值', trigger: 'blur' }">
                      <el-input v-model="runParaForm.paraValue" placeholder="请输入参数值"></el-input>
                    </el-form-item>
                  </el-form>
                  <span slot="footer" class="dialog-footer">
            <el-button @click="closeRunParaForm('runParaForm')">取 消</el-button>
            <el-button type="primary" @click="submitRunParaForm('runParaForm')">确 定</el-button>
          </span>
                </el-dialog>
              </el-dialog>
            </div>
            <div v-show="this.active == 2 && rightVisible">
              <div class="panel-title">通知人员与保存</div>
              <el-table
                :data="userInfos"
                border
                fit
                highlight-current-row
                ref="userInfos"
                @selection-change="handleSelectionChange2"
              >
                <el-table-column type="selection" width="60" align="center"></el-table-column>
                <el-table-column prop="userName" label="姓名" width="150" align="center"></el-table-column>
                <el-table-column prop="email" label="邮箱" align="center"></el-table-column>
                <el-table-column prop="wechat" label="微信" align="center"></el-table-column>
              </el-table>
            </div>
          </div>
          <div v-if="rightVisible" class="plan-edit-footer-actions">
            <div class="footer-step-tip">
              当前步骤：{{ active === 1 ? "算法选择及配置" : (active === 2 ? "通知人员与保存" : "-") }}
            </div>
            <div class="footer-actions-right">
              <el-button @click="backToExePlanTable()">返回列表</el-button>
              <el-button v-show="this.active === 2" @click="prev">上一步</el-button>
              <el-button v-show="this.active === 1" type="primary" @click="next">下一步</el-button>
              <el-button v-show="this.active === 2" type="primary" @click="submitExePlan()">保存计划</el-button>
            </div>
          </div>
        </el-main>
      </el-container>
    </div>
    <div v-show="showExePlanTableVisible">
      <el-container class="plan-list-container">
        <el-header class="plan-list-header" height="180px">
          <plan-list-header
            :search="search"
            :options="options"
            @update:search="onSearchUpdate"
            @add="addExePlan"
            @open-wizard="wizardVisible = true"
            @open-readiness="runtimeReadinessVisible = true"
            @delete-batch="deleteBatchExePlan"
            @query="onSearchQuery"
            @refresh="clearSearchCondition"
          />
        </el-header>
        <el-main class="plan-list-main">
          <el-table
            :data="exePlans"
            border
            fit
            highlight-current-row
            class="plan-list-table"
            @selection-change="handleSelectionChange1"
          >
            <el-table-column type="selection" width="60" align="center"></el-table-column>
            <el-table-column prop="planName" label="计划名称" align="center"></el-table-column>
            <el-table-column prop="planScale" label="规模(实例/算法)" width="130" align="center"></el-table-column>
            <el-table-column prop="exeStartTime" label="开始时间" width="170" align="center"></el-table-column>
            <el-table-column prop="exeEndTime" label="结束时间" width="170" align="center"></el-table-column>
            <el-table-column prop="exeState" label="执行状态" width="100" align="center"></el-table-column>
            <el-table-column prop="lastError" label="异常原因" width="220" align="center" show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" align="center" width="430">
              <template slot-scope="scope">
                <el-button type="success" size="mini" icon="el-icon-check" @click="doExePlan(scope.row)"
                           :disabled="scope.row.exeState != '未执行'">执行
                </el-button>
                <el-button type="warning" size="mini" icon="el-icon-refresh" @click="reExecutePlan(scope.row)"
                           :disabled="scope.row.exeState != '异常结束'">重新执行
                </el-button>
                <el-button type="primary" size="mini" icon="el-icon-view" @click="viewExePlan(scope.row)">查看
                </el-button>
                <el-button type="primary" size="mini" icon="el-icon-edit" @click="editExePlan(scope.row)"
                           :disabled="scope.row.exeState != '未执行'">编辑
                </el-button>
                <el-button type="danger" size="mini" icon="el-icon-delete" @click="deleteExePlan(scope.row)">删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
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
        </el-main>
      </el-container>

    </div>
    <div v-show="viewVisible">
      <el-container class="plan-view-container">
        <el-aside width="400px" class="plan-view-aside">
          <div class="plan-view-aside-body">
            <div class="panel-title">计划基础信息</div>
            <div class="plan-view-state-line">
              <span class="state-label">当前状态</span>
              <el-tag size="small" :type="showedExePlan.exeState === '正常结束' ? 'success' : (showedExePlan.exeState === '异常结束' ? 'danger' : 'info')">
                {{ showedExePlan.exeState || "-" }}
              </el-tag>
            </div>
            <el-form :model="showedExePlan" label-width="80px" class="plan-view-form">
              <el-form-item label="计划名称">
                <el-input v-model="showedExePlan.planName" readonly></el-input>
              </el-form-item>
              <el-form-item label="问题实例">
                <el-tree
                  :data="treeData"
                  show-checkbox
                  node-key="id"
                  highlight-current
                  default-expand-all
                  :props="defaultProps">
                </el-tree>
              </el-form-item>
              <el-form-item label="执行状态">
                <el-input v-model="showedExePlan.exeState" readonly></el-input>
              </el-form-item>
              <el-form-item label="开始时间">
                <el-input v-model="showedExePlan.exeStartTime" readonly></el-input>
              </el-form-item>
              <el-form-item label="结束时间">
                <el-input v-model="showedExePlan.exeEndTime" readonly></el-input>
              </el-form-item>
              <el-form-item label="异常原因" v-if="showedExePlan.lastError">
                <el-input type="textarea" v-model="showedExePlan.lastError" readonly></el-input>
              </el-form-item>
              <el-form-item label="计划描述">
                <el-input type="textarea" v-model="showedExePlan.description" readonly></el-input>
              </el-form-item>
            </el-form>
            <div class="view-action-row">
              <el-button type="warning" @click="openPlanLogsDialog">执行日志</el-button>
              <el-button type="primary" @click="backFromView">返回列表</el-button>
            </div>
          </div>
        </el-aside>
        <el-main class="plan-view-main">
          <div class="view-section-card">
            <div class="panel-title">算法信息</div>
            <div class="panel-subtitle">可查看每个算法的参数列表和执行结果指标。</div>
          <el-table
            class="view-table"
            :data="showedExePlan.algRunInfos"
            max-height="280"
            border
            highlight-current-row
            style="width: 100%">
            <el-table-column type="index" label="序号" width="70" align="center"></el-table-column>
            <el-table-column prop="showedAlgName" label="算法名称" min-width="150" align="center"></el-table-column>
            <el-table-column prop="description" label="算法描述" align="center"></el-table-column>
            <el-table-column prop="runNum" label="运行次数" width="100" align="center"></el-table-column>
            <el-table-column label="参数列表" width="90" align="center">
              <template slot-scope="scope">
                <el-button size="mini" @click="viewRunParas(scope.row)">查看</el-button>
              </template>
            </el-table-column>
            <el-table-column label="执行结果" width="90" align="center">
              <template slot-scope="scope">
                <el-button size="mini" @click="showExeResults(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          </div>
          <div class="view-section-card panel-gap">
          <div class="panel-title">通知人员</div>
          <div class="panel-subtitle">用于接收计划执行通知，可在执行日志中查看投递结果。</div>
          <el-table
            class="view-table"
            :data="showedExePlan.userInfos"
            max-height="240"
            border
            highlight-current-row
            style="width: 100%">
            <el-table-column type="index" label="序号" width="70" align="center"></el-table-column>
            <el-table-column prop="userName" label="用户名称" width="140" align="center"></el-table-column>
            <el-table-column prop="email" label="邮箱" align="center"></el-table-column>
            <el-table-column prop="wechat" label="微信" width="160" align="center"></el-table-column>
          </el-table>
          </div>
          <el-dialog
            title="参数列表"
            :visible.sync="dialogViewRunParasVisible"
            center
            width="60%"
            :close-on-click-modal="false"
          >
            <el-table :data="runParaTable" border fit highlight-current-row>
              <el-table-column property="paraId" label="序号" width="100" align="center"></el-table-column>
              <el-table-column property="paraName" label="参数名" width="100" align="center"></el-table-column>
              <el-table-column property="paraType" label="参数类型" width="100" align="center"></el-table-column>
              <el-table-column property="paraValue" label="参数值" width="100" align="center"></el-table-column>
              <el-table-column property="description" label="描述" align="center"></el-table-column>
            </el-table>
          </el-dialog>
          <el-dialog
            title="执行结果"
            :visible.sync="dialogViewExeResultsVisible"
            center
            width="60%"
            :close-on-click-modal="false"
          >
            <el-alert
              :title="`状态: ${exeResultDetail.status || '-'}，原因: ${exeResultDetail.reasonCode || '-'}，${exeResultDetail.message || ''}`"
              :type="exeResultDetail.status === 'SUCCESS' ? 'success' : (exeResultDetail.status === 'MISSING' || exeResultDetail.status === 'EMPTY' ? 'warning' : 'info')"
              :closable="false"
              show-icon
              style="margin-bottom: 12px;"
            />
            <el-descriptions title="指标汇总（均值）" :column="4" border size="small" style="margin-bottom: 12px;">
              <el-descriptions-item label="运行条数">{{ exeResultDetail.aggregate.runCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Runtime(ms)">{{ formatMetric(exeResultDetail.aggregate.runtimeMsMean) }}</el-descriptions-item>
              <el-descriptions-item label="HV">{{ formatMetric(exeResultDetail.aggregate.hvMean) }}</el-descriptions-item>
              <el-descriptions-item label="IGD+">{{ formatMetric(exeResultDetail.aggregate.igdPlusMean) }}</el-descriptions-item>
              <el-descriptions-item label="GD">{{ formatMetric(exeResultDetail.aggregate.gdMean) }}</el-descriptions-item>
              <el-descriptions-item label="Coverage">{{ formatCoverageMetric(exeResultDetail.aggregate.coverageMean) }}</el-descriptions-item>
              <el-descriptions-item label="Spread(Δ)">{{ formatMetric(exeResultDetail.aggregate.spreadDeltaMean) }}</el-descriptions-item>
              <el-descriptions-item label="Spacing">{{ formatMetric(exeResultDetail.aggregate.spacingMean) }}</el-descriptions-item>
              <el-descriptions-item label="指标版本">{{ exeResultDetail.metricVersion || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-if="coverageHintText()"
              :title="coverageHintText()"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 12px;"
            />
            <el-table :data="exeResultsTable" border fit highlight-current-row>
              <el-table-column property="runIndex" label="run" width="70" align="center"></el-table-column>
              <el-table-column property="probInstName" label="问题实例" min-width="120" align="center"></el-table-column>
              <el-table-column property="runtimeMs" label="Runtime(ms)" width="110" align="center"></el-table-column>
              <el-table-column property="paretoSize" label="Pareto点数" width="100" align="center"></el-table-column>
              <el-table-column label="HV" width="100" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.hv) }}</template>
              </el-table-column>
              <el-table-column label="IGD+" width="100" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.igdPlus) }}</template>
              </el-table-column>
              <el-table-column label="GD" width="100" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.gd) }}</template>
              </el-table-column>
              <el-table-column label="Coverage" width="110" align="center">
                <template slot-scope="scope">{{ formatCoverageMetric(scope.row.coverage) }}</template>
              </el-table-column>
              <el-table-column label="Spread(Δ)" width="110" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.spreadDelta) }}</template>
              </el-table-column>
              <el-table-column label="Spacing" width="110" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.spacing) }}</template>
              </el-table-column>
              <el-table-column property="metricStatus" label="指标状态" width="100" align="center"></el-table-column>
              <el-table-column property="reasonCode" label="原因码" width="160" align="center" show-overflow-tooltip></el-table-column>
            </el-table>
          </el-dialog>
          <el-dialog
            title="执行日志"
            :visible.sync="dialogPlanLogsVisible"
            center
            width="70%"
            custom-class="plan-logs-dialog"
            :before-close="closePlanLogsDialog"
            :close-on-click-modal="false"
          >
            <div class="log-toolbar">
              <div class="log-toolbar-left">
                <el-radio-group v-model="planLogScope" size="mini" @change="switchPlanLogScope">
                  <el-radio-button label="latest">最新执行</el-radio-button>
                  <el-radio-button label="all">全部历史</el-radio-button>
                </el-radio-group>
              </div>
              <div class="log-toolbar-right">
                <el-button size="mini" icon="el-icon-refresh" @click="fetchPlanLogs(true)">刷新</el-button>
                <el-button size="mini" icon="el-icon-download" @click="exportPlanLogsAs('csv')">导出CSV</el-button>
                <el-button size="mini" icon="el-icon-download" @click="exportPlanLogsAs('json')">导出JSON</el-button>
              </div>
            </div>
            <el-table :data="planLogs" border fit height="420" class="log-table" :row-class-name="planLogRowClassName">
              <el-table-column property="seq" label="序号" width="90" align="center"></el-table-column>
              <el-table-column property="ts" label="时间" width="180" align="center">
                <template slot-scope="scope">
                  {{ formatTimestampToDateTime(scope.row.ts) }}
                </template>
              </el-table-column>
              <el-table-column property="level" label="级别" width="90" align="center">
                <template slot-scope="scope">
                  <el-tag size="mini" :type="planLogLevelTagType(scope.row.level)">{{ scope.row.level || "-" }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column property="stage" label="阶段" width="120" align="center"></el-table-column>
              <el-table-column label="算法名称" width="180" show-overflow-tooltip>
                <template slot-scope="scope">
                  {{ resolveAlgNameByLog(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column property="runIndex" label="run" width="70" align="center"></el-table-column>
              <el-table-column label="问题实例" width="180" show-overflow-tooltip>
                <template slot-scope="scope">
                  {{ resolveProbInstNameByLog(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column label="内容" min-width="260">
                <template slot-scope="scope">
                  <div class="log-cell">
                    <div :class="['log-cell-content', { expanded: isPlanLogExpanded(scope.row, 'message') }]">
                      {{ scope.row.message || "-" }}
                    </div>
                    <el-button
                      v-if="(scope.row.message || '').length > 60"
                      type="text"
                      class="log-expand-btn"
                      @click="togglePlanLogExpanded(scope.row, 'message')"
                    >{{ isPlanLogExpanded(scope.row, 'message') ? '收起' : '展开' }}</el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="详情" min-width="220">
                <template slot-scope="scope">
                  <div class="log-cell">
                    <div :class="['log-cell-content', { expanded: isPlanLogExpanded(scope.row, 'details') }]">
                      {{ scope.row.details || "-" }}
                    </div>
                    <el-button
                      v-if="(scope.row.details || '').length > 50"
                      type="text"
                      class="log-expand-btn"
                      @click="togglePlanLogExpanded(scope.row, 'details')"
                    >{{ isPlanLogExpanded(scope.row, 'details') ? '收起' : '展开' }}</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <div class="notify-section">
              <el-divider content-position="left">通知记录</el-divider>
              <el-alert
                title="若出现 MAIL_CONFIG_INVALID / MAIL_FROM_EMPTY / MAIL_FROM_INVALID，请先补齐SMTP配置；若出现 MAIL_TENCENT_CONFIG_INVALID，请补齐腾讯SES配置。"
                type="warning"
                :closable="false"
                show-icon
                class="notify-alert"
              />
              <el-table :data="notificationLogs" border fit height="220" v-loading="notificationLoading">
                <el-table-column property="createdAt" label="创建时间" width="180" align="center">
                  <template slot-scope="scope">
                    {{ formatTimestampToDateTime(scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column property="userId" label="用户ID" width="150" show-overflow-tooltip></el-table-column>
                <el-table-column property="toEmail" label="邮箱" min-width="180" show-overflow-tooltip></el-table-column>
                <el-table-column property="status" label="状态" width="130" align="center"></el-table-column>
                <el-table-column property="retryCount" label="重试" width="80" align="center"></el-table-column>
                <el-table-column property="lastErrorCode" label="原因码" width="170" show-overflow-tooltip></el-table-column>
                <el-table-column property="lastErrorMsg" label="原因" min-width="180" show-overflow-tooltip></el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template slot-scope="scope">
                    <el-button
                      size="mini"
                      type="warning"
                      :disabled="scope.row.status !== 'FAILED_FINAL' && scope.row.status !== 'FAILED_RETRY'"
                      @click="resendOneNotification(scope.row)"
                    >补发</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="notify-toolbar">
                <el-button size="mini" icon="el-icon-refresh" @click="fetchNotificationLogs">刷新通知记录</el-button>
                <el-button size="mini" type="warning" @click="resendFailedByExecution">补发本批次失败通知</el-button>
                <span class="notify-count">共 {{ notificationTotal }} 条</span>
              </div>
            </div>
            <div class="log-footer-actions">
              <el-button size="mini" type="primary" @click="runPreCheckFromLogs">执行前检查</el-button>
              <el-button
                size="mini"
                type="warning"
                :disabled="showedExePlan.exeState !== '异常结束'"
                @click="reExecutePlan(showedExePlan)"
              >重新执行</el-button>
              <span class="log-plan-state">计划状态：{{ showedExePlan.exeState }}</span>
            </div>
          </el-dialog>
        </el-main>
      </el-container>
    </div>
    <execution-wizard
      :visible.sync="wizardVisible"
      :prob-insts="probInsts"
      :alg-infos="algInfos"
      @refresh="clearSearchCondition"
    />
    <runtime-readiness-dialog
      :visible.sync="runtimeReadinessVisible"
      :prob-insts="probInsts"
      :alg-infos="algInfos"
      @open-wizard="openWizardFromReadiness"
    />
  </div>


</template>


<script>
import {getProbInstById, getProbInstList} from "@/api/exphlp/probInstMgr";
import {getAlgs} from "@/api/exphlp/algLibMgr";
import {getUserList} from "@/api/exphlp/platMgr";
import {
  addExePlan, countAllExePlans,
  getExePlanByName,
  getExePlans,
  updateExePlanById
} from "@/api/exphlp/exePlanMgr";
import ExecutionWizard from "@/views/planManage/components/ExecutionWizard";
import RuntimeReadinessDialog from "@/views/planManage/components/RuntimeReadinessDialog";
import PlanListHeader from "@/views/planManage/components/PlanListHeader";
import { planResultMethods } from "@/views/planManage/modules/planResultMethods";
import { planLogMethods } from "@/views/planManage/modules/planLogMethods";
import { planExecutionMethods } from "@/views/planManage/modules/planExecutionMethods";
import { planDeleteMethods } from "@/views/planManage/modules/planDeleteMethods";
import {
  formatTimestampToDateTime as formatTs,
  normalizeToEpochMsOrZero,
  buildProbInstsTreeData as buildTreeData,
  getCheckedProbInstIds,
  handleSameAlgName as buildAlgDisplayNames,
  hasDuplicateParas as hasDuplicateParasInPlan,
} from "@/views/planManage/modules/planUiHelpers";

export default {
  components: { ExecutionWizard, RuntimeReadinessDialog, PlanListHeader },
  data() {
    return {
      probInsts: [],
      algInfos: [],
      pageOne: 1,
      totalSize: 10000,

      exePlanId: '',

      algId: '',
      algRunInfoId: '',
      multipleSelection2: [],
      exePlan: {
        planName: '',
        probInsts: [],
        probInstIds: [],
        exeState: '',
        exeStartTime: '',
        exeEndTime: '',
        userIds: [],
        description: '',
        algRunInfos: [],
      },
      addFlag: true,
      rightVisible: false,
      active: 1,

      dialogRunNumFormVisible: false,
      dialogRunParaFormVisible: false,
      dialogRunParaTableVisible: false,
      runNumForm: {
        runNum: '',
      },
      runParaTable: [],
      exeResultsTable: [],
      exeResultDetail: {
        status: "",
        reasonCode: "",
        message: "",
        metricVersion: "",
        runs: [],
        aggregate: {},
      },
      runParaForm: {},
      userInfos: [],

      editVisible: false,
      viewVisible: false,
      showExePlanTableVisible: true,
      search: {
        planName: "",
        exeState: "",
        exeStartTime: "",
        exeEndTime: "",
      },
      options: [
        {
          label: "未执行",
          value: "未执行"
        },
        {
          label: "执行中",
          value: "执行中"
        },
        {
          label: "异常结束",
          value: "异常结束"
        },
        {
          label: "正常结束",
          value: "正常结束"
        }
      ],
      exePlans: [],
      multipleSelection1: [],
      dialogViewRunParasVisible: false,
      dialogViewExeResultsVisible: false,
      dialogPlanLogsVisible: false,
      planLogs: [],
      planLogAfterSeq: 0,
      planLogScope: "latest",
      planLogExecutionId: "",
      planLogTimer: null,
      planLogExpandedCells: {},
      notificationLogs: [],
      notificationTotal: 0,
      notificationLoading: false,
      showedExePlan: {
        planName: '',
        probInsts: [],
        probInstIds: [],
        instName: '',
        exeState: '',
        exeStartTime: '',
        exeEndTime: '',
        userIds: [],
        userInfos: [],
        description: '',
        algRunInfos: [],
      },
      pageHelper: {
        currentPageNum: 1,
        pageSize: 10,
        totalSize: 0,
      },

      defaultProps: {
        children: 'children',
        label: 'label'
      },
      treeData: [],
      duplicateParaFlag : false,
      wizardVisible: false,
      runtimeReadinessVisible: false,
    }
  },
  created() {
    this.listProbInsts();
    this.getAlgInfos();
    this.getUserInfos();
    this.getExePlans();
    this.countAllExePlans();
    this.ensureViewState();
  },
  activated() {
    this.ensureViewState();
  },
  beforeDestroy() {
    this.stopPlanLogPolling();
  },
  methods: {
    ensureViewState() {
      if (!this.editVisible && !this.viewVisible && !this.showExePlanTableVisible) {
        this.showExePlanTableVisible = true;
      }
    },
    listProbInsts() {
      getProbInstList(this.pageOne, this.totalSize).then(res => {
        this.probInsts = res;
      });
    },
    getAlgInfos() {
      getAlgs(this.pageOne, this.totalSize).then(res => {
        this.algInfos = res;
      });
    },
    getUserInfos() {
      getUserList(this.pageOne, this.totalSize).then(res => {
        this.userInfos = res;
      });
    },

    submitForm(formName) {
      if (this.getCheckedNodes() == '') {
        this.$message({type: "warning", message: "请选择问题实例",});
        return;
      }
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.rightVisible = true;
          this.active = 1;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    next() {
      if (this.active < 2) {
        this.active += 1;
      }
    },
    prev() {
      if (this.active > 1) {
        this.active -= 1;
      }
    },
    addAlgInfo() {
      // for (var i = 0; i < this.exePlan.algRunInfos.length; i++) {
      //   if (this.algInfos[this.algId].algId == this.exePlan.algRunInfos[i].algId) {
      //     this.$message({type: "info", message: "已存在相同算法",});
      //     break;
      //   }
      // }
      if (this.algId || this.algId === 0) {
        this.algInfos[this.algId].algRunInfoId = this.exePlan.algRunInfos.length + 1;
        this.algInfos[this.algId].runParas = this.algInfos[this.algId].defParas;
        this.algInfos[this.algId].runNum = 20;
        this.exePlan.algRunInfos.push(JSON.parse(JSON.stringify(this.algInfos[this.algId])));
        var showedAlgName = this.handleSameAlgName(this.exePlan.algRunInfos);
        for (var i = 0; i < showedAlgName.length; i++) {
          this.exePlan.algRunInfos[i].showedAlgName = showedAlgName[i];
        }
      }
      if(this.hasDuplicateParas()){
        this.$message({type: "warning", message: "相同算法存在相同的运行参数",});
      }
    },
    handleSelectionChange2(val) {
      this.multipleSelection2 = val;
    },

    openRunNumForm(scope) {
      this.runNumForm = scope;
      this.dialogRunNumFormVisible = true;
    },
    closeRunNumForm(formName) {
      this.$refs[formName].clearValidate();
      this.dialogRunNumFormVisible = false;
    },
    submitRunNumForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.dialogRunNumFormVisible = false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    openRunParaTable(scope) {
      this.algRunInfoId = scope.algRunInfoId;
      this.runParaTable = scope.defParas;
      if (scope.runParas.length != 0) {
        this.runParaTable = scope.runParas;
      }
      this.dialogRunParaTableVisible = true;
    },
    beforeCloseRunParaTable(done) {
      this.exePlan.algRunInfos[this.algRunInfoId - 1].runParas = this.runParaTable;
      done();
    },
    openRunParaForm(scope) {
      this.runParaForm = scope;
      this.dialogRunParaFormVisible = true;
    },
    closeRunParaForm(formName) {
      this.$refs[formName].clearValidate();
      this.dialogRunParaFormVisible = false;
    },
    submitRunParaForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if(this.hasDuplicateParas()){
            this.$message({type: "warning", message: "相同算法存在相同的运行参数",});
          }
          this.dialogRunParaFormVisible = false;
        } else {
          this.$message({type: "warning", message: "请完整填写信息",});
        }
      });
    },
    deleteAlgInfo(scope) {
      this.exePlan.algRunInfos.splice(scope.algRunInfoId - 1, 1);
      for (var i = 0; i < this.exePlan.algRunInfos.length; i++) {
        this.exePlan.algRunInfos[i].algRunInfoId = i + 1;
      }
      var showedAlgName = this.handleSameAlgName(this.exePlan.algRunInfos);
      for (var i = 0; i < showedAlgName.length; i++) {
        this.exePlan.algRunInfos[i].showedAlgName = showedAlgName[i];
      }
      // 删除后立即刷新重复参数状态，避免提交时读取到旧缓存值。
      this.hasDuplicateParas();
    },
    submitExePlan() {
      this.exePlan.userIds = [];
      var flag = true;
      if (this.addFlag) {
        getExePlanByName(this.exePlan.planName).then(res => {
          if (res && res.planId) {
            this.$message({type: "warning", message: "计划名称已存在",});
          } else {
            if (this.exePlan.algRunInfos.length == 0) {
              flag = false;
              this.$message({type: "warning", message: "请选择算法",});
            } else if (this.multipleSelection2.length == 0) {
              flag = false;
              this.$message({type: "warning", message: "请选择通知人员",});
            } else {
              for (var i = 0; i < this.multipleSelection2.length; i++) {
                this.exePlan.userIds.push(this.multipleSelection2[i].userId);
              }
            }
            if (flag == true) {
              if(this.hasDuplicateParas()){
                this.$message({type: "warning", message: "相同算法存在相同的运行参数，请修改运行参数后再提交",});
              }else {
                this.warnHighCostParas(this.exePlan.algRunInfos);
                this.exePlan.exeStartTime = normalizeToEpochMsOrZero(this.exePlan.exeStartTime);
                this.exePlan.exeEndTime = normalizeToEpochMsOrZero(this.exePlan.exeEndTime);
                this.exePlan.probInstIds = this.getCheckedNodes();
                this.exePlan.exeState = 1;
                addExePlan(this.exePlan)
                  .then(res => {
                    this.$message({type: "success", message: "添加成功",});
                    this.resetForm('exePlan');
                    this.clearSearchCondition();
                    this.editVisible = false;
                    this.showExePlanTableVisible = true;
                  });
              }
            }
          }
        }).catch(() => {
          this.$message({type: "error", message: "计划名称校验失败，请重试",});
        });
      } else {
        if (this.exePlan.algRunInfos.length == 0) {
          flag = false;
          this.$message({type: "warning", message: "请选择算法",});
        } else if (this.multipleSelection2.length == 0) {
          flag = false;
          this.$message({type: "warning", message: "请选择通知人员",});
        } else {
          for (var i = 0; i < this.multipleSelection2.length; i++) {
            this.exePlan.userIds.push(this.multipleSelection2[i].userId);
          }
        }
        if (flag == true) {
          if (this.hasDuplicateParas()) {
            this.$message({type: "warning", message: "相同算法存在相同的运行参数，请修改运行参数后再提交",});
          } else {
            this.warnHighCostParas(this.exePlan.algRunInfos);
            for (var i = 0; i < this.options.length; i++) {
              if (this.exePlan.exeState == this.options[i].value) {
                this.exePlan.exeState = i + 1;
                break;
              }
            }
            this.exePlan.exeStartTime = normalizeToEpochMsOrZero(this.exePlan.exeStartTime);
            this.exePlan.exeEndTime = normalizeToEpochMsOrZero(this.exePlan.exeEndTime);
            updateExePlanById(this.exePlan)
              .then(res => {
                this.$message({type: "success", message: "编辑成功",});
                this.editVisible = false;
                this.showExePlanTableVisible = true;
                this.clearSearchCondition();
              });
          }
        }
      }

    },

    getExePlans() {
      getExePlans(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.exePlans = res;
        for (var i = 0; i < this.exePlans.length; i++) this.decoratePlanRow(this.exePlans[i]);
        this.countAllExePlans();
      });
    },
    searchByCondition() {
      if (this.search.planName == '') {
        this.$message({type: "error", message: "计划名称不能为空",});
        return;
      }
      this.exePlans = [];
      getExePlanByName(this.search.planName).then(res => {
        if (res != '') {
          this.decoratePlanRow(res);
          this.pageHelper.totalSize = 1;
          this.exePlans.push(res);
        } else {
          this.pageHelper.totalSize = 0;
        }
      });
    },
    clearSearchCondition() {
      this.pageHelper.currentPageNum = 1;
      this.pageHelper.pageSize = 10;
      if (this.$refs.userInfos && typeof this.$refs.userInfos.clearSelection === "function") {
        this.$refs.userInfos.clearSelection();
      }
      this.countAllExePlans();
      getExePlans(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.exePlans = res;
        for (var i = 0; i < this.exePlans.length; i++) {
          this.decoratePlanRow(this.exePlans[i]);
        }
      });
      this.search = {
        planName: "",
        exeState: "",
        exeStartTime: "",
        exeEndTime: "",
      };
    },
    addExePlan() {
      this.algId = '';
      this.exePlan = {
        planName: '',
        probInsts: [],
        probInstIds: [],
        exeState: '',
        exeStartTime: '--',
        exeEndTime: '--',
        userIds: [],
        description: '',
        algRunInfos: [],
      };
      this.treeData = this.buildProbInstsTreeData(this.probInsts);
      this.rightVisible = false;
      this.addFlag = true;
      this.showExePlanTableVisible = false;
      this.editVisible = true;
    },
    handleSelectionChange1(val) {
      this.multipleSelection1 = val;
    },
    viewExePlan(scope) {
      this.stopPlanLogPolling();
      this.dialogPlanLogsVisible = false;
      this.planLogs = [];
      this.planLogAfterSeq = 0;
      this.planLogExecutionId = "";
      this.planLogScope = "latest";
      this.planLogExpandedCells = {};
      this.notificationLogs = [];
      this.notificationTotal = 0;
      this.exeResultsTable = [];
      this.exeResultDetail = {
        status: "",
        reasonCode: "",
        message: "",
        metricVersion: "",
        runs: [],
        aggregate: {},
      };
      this.exePlanId = scope.planId;
      this.showedExePlan = {...scope};
      this.showedExePlan.userInfos = [];
      this.showedExePlan.probInsts = [];
      for (var i = 0; i < scope.probInstIds.length; i++) {
        for (var j = 0; j < this.probInsts.length; j++) {
          if (scope.probInstIds[i] == this.probInsts[j].instId) {
            this.showedExePlan.probInsts.push(this.probInsts[j]);
            break;
          }
        }
      }
      this.treeData = this.buildProbInstsTreeData(this.showedExePlan.probInsts);
      for (var i = 0; i < this.treeData.length; i++) {
        this.treeData[i].disabled = true;
        for (var j = 0; j < this.treeData[i].children.length; j++) {
          this.treeData[i].children[j].disabled = true;
        }
      }
      for (var i = 0; i < this.showedExePlan.algRunInfos.length; i++)
        for (var j = 0; j < this.algInfos.length; j++)
          if (this.showedExePlan.algRunInfos[i].algId == this.algInfos[j].algId) {
            this.showedExePlan.algRunInfos[i].description = this.algInfos[j].description;
            break;
          }
      var showedAlgName = this.handleSameAlgName(this.showedExePlan.algRunInfos)
      for (var i = 0; i < showedAlgName.length; i++) {
        this.showedExePlan.algRunInfos[i].showedAlgName = showedAlgName[i];
      }
      for (var i = 0; i < this.showedExePlan.userIds.length; i++)
        for (var j = 0; j < this.userInfos.length; j++)
          if (this.showedExePlan.userIds[i] == this.userInfos[j].userId) {
            this.showedExePlan.userInfos.push(this.userInfos[j]);
            break;
          }
      this.showExePlanTableVisible = false;
      this.viewVisible = true;

    },
    viewRunParas(scope) {
      this.runParaTable = scope.runParas;
      this.dialogViewRunParasVisible = true;
    },
    showExeResults(scope) {
      return planResultMethods.showExeResults.call(this, scope);
    },
    getExeResults(scope) {
      return planResultMethods.getExeResults.call(this, scope);
    },
    formatMetric(value) {
      return planResultMethods.formatMetric.call(this, value);
    },
    formatCoverageMetric(value) {
      return planResultMethods.formatCoverageMetric.call(this, value);
    },
    coverageHintText() {
      return planResultMethods.coverageHintText.call(this, this.exeResultDetail);
    },
    openPlanLogsDialog() {
      return planLogMethods.openPlanLogsDialog.call(this);
    },
    closePlanLogsDialog(done) {
      return planLogMethods.closePlanLogsDialog.call(this, done);
    },
    startPlanLogPolling() {
      return planLogMethods.startPlanLogPolling.call(this);
    },
    stopPlanLogPolling() {
      return planLogMethods.stopPlanLogPolling.call(this);
    },
    fetchPlanLogs(reset) {
      return planLogMethods.fetchPlanLogs.call(this, reset);
    },
    switchPlanLogScope() {
      return planLogMethods.switchPlanLogScope.call(this);
    },
    fetchNotificationLogs() {
      return planLogMethods.fetchNotificationLogs.call(this);
    },
    resendOneNotification(row) {
      return planLogMethods.resendOneNotification.call(this, row);
    },
    resendFailedByExecution() {
      return planLogMethods.resendFailedByExecution.call(this);
    },
    exportPlanLogsAs(format) {
      return planLogMethods.exportPlanLogsAs.call(this, format);
    },
    downloadTextFile(filename, content, mimeType) {
      return planLogMethods.downloadTextFile.call(this, filename, content, mimeType);
    },
    planLogLevelTagType(level) {
      if (level === "ERROR") {
        return "danger";
      }
      if (level === "WARN") {
        return "warning";
      }
      if (level === "INFO") {
        return "success";
      }
      return "info";
    },
    planLogRowClassName({ row }) {
      if (row.level === "ERROR" || row.stage === "PLAN_FAIL" || row.stage === "ALG_FAIL") {
        return "plan-log-row-error";
      }
      if (row.level === "WARN") {
        return "plan-log-row-warn";
      }
      return "";
    },
    getPlanLogCellKey(row, field) {
      const seq = row && row.seq ? row.seq : "x";
      return `${seq}-${field}`;
    },
    isPlanLogExpanded(row, field) {
      const key = this.getPlanLogCellKey(row, field);
      return this.planLogExpandedCells[key] === true;
    },
    togglePlanLogExpanded(row, field) {
      const key = this.getPlanLogCellKey(row, field);
      this.$set(this.planLogExpandedCells, key, !this.planLogExpandedCells[key]);
    },
    resolveAlgNameByLog(row) {
      const fromRow = row && (row.algName || row.showedAlgName);
      if (fromRow) {
        return fromRow;
      }
      const algId = row && row.algId;
      if (!algId) {
        return "未关联算法";
      }
      const matched = (this.algInfos || []).find((item) => item.algId === algId);
      return matched && matched.algName ? matched.algName : "未知算法";
    },
    resolveProbInstNameByLog(row) {
      const fromRow = row && row.probInstName;
      if (fromRow) {
        return fromRow;
      }
      const probInstId = row && row.probInstId;
      if (!probInstId) {
        return "未关联问题实例";
      }
      const matched = (this.probInsts || []).find((item) => item.instId === probInstId);
      return matched && matched.instName ? matched.instName : "未知问题实例";
    },
    backFromView() {
      this.stopPlanLogPolling();
      this.dialogPlanLogsVisible = false;
      this.planLogExecutionId = "";
      this.planLogScope = "latest";
      this.planLogExpandedCells = {};
      this.notificationLogs = [];
      this.notificationTotal = 0;
      this.viewVisible = false;
      this.showExePlanTableVisible = true;
    },
    editExePlan(scope) {
      this.algId = '';
      this.exePlan = {...scope};
      this.exePlan.probInsts = [];
      for (var i = 0; i < scope.probInstIds.length; i++) {
        for (var j = 0; j < this.probInsts.length; j++) {
          if (scope.probInstIds[i] == this.probInsts[j].instId) {
            this.exePlan.probInsts.push(this.probInsts[j]);
            break;
          }
        }
      }
      this.treeData = this.buildProbInstsTreeData(this.exePlan.probInsts);
      for (var i = 0; i < this.treeData.length; i++) {
        this.treeData[i].disabled = true;
        for (var j = 0; j < this.treeData[i].children.length; j++) {
          this.treeData[i].children[j].disabled = true;
        }
      }
      var showedAlgName = this.handleSameAlgName(this.exePlan.algRunInfos);
      for (var i = 0; i < showedAlgName.length; i++) {
        this.exePlan.algRunInfos[i].showedAlgName = showedAlgName[i];
      }
      for (var i = 0; i < this.exePlan.algRunInfos.length; i++)
        for (var j = 0; j < this.algInfos.length; j++)
          if (this.exePlan.algRunInfos[i].algId == this.algInfos[j].algId) {
            this.exePlan.algRunInfos[i].description = this.algInfos[j].description;
            break;
          }
      var temUserInfos = [];
      for (var i = 0; i < this.exePlan.userIds.length; i++)
        for (var j = 0; j < this.userInfos.length; j++)
          if (this.exePlan.userIds[i] == this.userInfos[j].userId) {
            temUserInfos.push(this.userInfos[j]);
            break;
          }
      temUserInfos.forEach(row => {
        this.$refs.userInfos.toggleRowSelection(row);
      });
      this.addFlag = false;
      this.showExePlanTableVisible = false;
      this.editVisible = true;
      this.rightVisible = true;
      this.active = 1;
    },
    deleteExePlan(scope) {
      return planDeleteMethods.deleteExePlan.call(this, scope);
    },
    deleteBatchExePlan() {
      return planDeleteMethods.deleteBatchExePlan.call(this);
    },
    backToExePlanTable() {
      this.algId = '';
      if (this.$refs['exePlan'] && typeof this.$refs['exePlan'].clearValidate === "function") {
        this.$refs['exePlan'].clearValidate();
      }
      if (this.$refs.userInfos && typeof this.$refs.userInfos.clearSelection === "function") {
        this.$refs.userInfos.clearSelection();
      }
      this.clearSearchCondition();
      this.editVisible = false;
      this.viewVisible = false;
      this.showExePlanTableVisible = true;
      this.ensureViewState();
    },
    handleSizeChange(val) {
      this.pageHelper.pageSize = val
      if (this.search.planName != "") {
        this.searchByCondition();
      } else {
        this.getExePlans();
      }
    },
    handleCurrentChange(val) {
      this.pageHelper.currentPageNum = val
      if (this.search.planName != "") {
        this.searchByCondition();
      } else {
        this.getExePlans();
      }
    },
    countAllExePlans() {
      countAllExePlans().then(res => {
        this.pageHelper.totalSize = res;
      });
    },
    doExePlan(scope) {
      return planExecutionMethods.doExePlan.call(this, scope);
    },
    onSearchUpdate(nextSearch) {
      this.search = { ...nextSearch };
    },
    onSearchQuery() {
      this.pageHelper.currentPageNum = 1;
      this.searchByCondition();
    },
    reExecutePlan(scope) {
      return planExecutionMethods.reExecutePlan.call(this, scope);
    },
    openWizardFromReadiness() {
      this.runtimeReadinessVisible = false;
      this.wizardVisible = true;
    },
    executePlan(scope, isRetry) {
      return planExecutionMethods.executePlan.call(this, scope, isRetry);
    },
    showPreCheckFailure(resp) {
      return planExecutionMethods.showPreCheckFailure.call(this, resp);
    },
    runPreCheckFromLogs() {
      return planExecutionMethods.runPreCheckFromLogs.call(this);
    },
    formatTimestampToDateTime(timestamp) {
      return formatTs(timestamp);
    },
    decoratePlanRow(row) {
      if (!row) return row;
      const rawState = row.exeState;
      if (rawState === "" || rawState === null || rawState === undefined) {
        row.exeState = "未执行";
      } else if (typeof rawState === "number") {
        row.exeState = (this.options[rawState - 1] || {}).value || String(rawState);
      } else if (/^\d+$/.test(String(rawState))) {
        const idx = Number(rawState);
        row.exeState = (this.options[idx - 1] || {}).value || String(rawState);
      }
      row.exeStartTime = this.formatTimestampToDateTime(row.exeStartTime);
      row.exeEndTime = this.formatTimestampToDateTime(row.exeEndTime);
      row.planScale = this.buildPlanScale(row);
      return row;
    },
    buildPlanScale(row) {
      const probCount = Array.isArray(row && row.probInstIds) ? row.probInstIds.length : 0;
      const algCount = Array.isArray(row && row.algRunInfos) ? row.algRunInfos.length : 0;
      if (probCount === 0 && algCount === 0) {
        return "--";
      }
      return `${probCount}/${algCount}`;
    },
    warnHighCostParas(algRunInfos) {
      if (!Array.isArray(algRunInfos) || algRunInfos.length === 0) {
        return;
      }
      for (let i = 0; i < algRunInfos.length; i++) {
        const info = algRunInfos[i] || {};
        const paraMap = {};
        const runParas = Array.isArray(info.runParas) ? info.runParas : [];
        for (let j = 0; j < runParas.length; j++) {
          const para = runParas[j] || {};
          if (para.paraName) {
            paraMap[String(para.paraName)] = para.paraValue;
          }
        }
        const pop = Number(paraMap.populationSize || 0);
        const gen = Number(paraMap.maxGenerations || 0);
        const nVars = Number(paraMap.nVars || 0);
        const runNum = Number(info.runNum || 0);
        const workLoad = pop * gen * Math.max(runNum, 1);
        if (Number.isFinite(workLoad) && workLoad >= 2000000) {
          this.$message({
            type: "warning",
            duration: 8000,
            message: `算法[${info.algName || "--"}]参数计算量较大（populationSize*maxGenerations*runNum=${workLoad}），若出现超时请降低参数或提高后端ALG_CALL_READ_TIMEOUT_MS`
          });
          return;
        }
        if (Number.isFinite(nVars) && nVars > 1000) {
          this.$message({
            type: "warning",
            duration: 8000,
            message: `算法[${info.algName || "--"}]的nVars=${nVars}偏大，建议先做小规模验证后再放大`
          });
          return;
        }
      }
    },
    buildProbInstsTreeData(probInsts) {
      return buildTreeData(probInsts);
    },
    getCheckedNodes() {
      return getCheckedProbInstIds(this.$refs.tree);
    },
    handleSameAlgName(algRunInfos) {
      return buildAlgDisplayNames(algRunInfos);
    },
    hasDuplicateParas() {
      const duplicated = hasDuplicateParasInPlan(this.exePlan.algRunInfos);
      this.duplicateParaFlag = duplicated;
      return duplicated;
    },
  }
}
</script>

<style scoped lang="scss">
.plan-manage-page {
  color: #303133;
}

.el-aside {
  background-color: white;
  text-align: center;
}

.el-main {
  text-align: center;
  border: solid 1px #eee;
}

.plan-edit-container {
  height: 720px;
  border: 1px solid #eee;
}

.plan-edit-aside {
  background: #fff;
  border-right: 1px solid #f0f2f5;
}

.plan-edit-aside-body {
  padding: 28px 18px 18px 18px;
}

.plan-edit-main {
  text-align: left;
  padding: 16px 16px 0 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.plan-edit-form {
  margin-top: 6px;
}

.plan-edit-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: left;
  line-height: 1.6;
}

.right-card {
  padding: 8px 0 4px 0;
  margin-bottom: 12px;
}

.plan-edit-content {
  flex: 1;
  overflow: auto;
  padding-right: 2px;
}

.submit-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.plan-edit-footer-actions {
  border-top: 1px solid #ebeef5;
  background: #fff;
  padding: 10px 2px 12px 2px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.footer-step-tip {
  color: #606266;
  font-size: 13px;
}

.footer-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alg-select-row {
  margin-top: 4px;
}

.plan-list-container {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.plan-list-header {
  height: auto !important;
  padding: 16px 16px 8px 16px;
}

.plan-list-main {
  border-top: 1px solid #f2f6fc;
  padding-top: 14px;
  width: 100%;
}

.plan-list-table ::v-deep .el-table th {
  background: #f8fafc;
  color: #4a5568;
  font-weight: 600;
}

.plan-list-table ::v-deep .el-table td,
.plan-list-table ::v-deep .el-table th {
  padding: 9px 0;
}

.plan-list-table {
  width: 100%;
}

.plan-view-container {
  height: 720px;
  border: 1px solid #eee;
}

.plan-view-aside {
  background-color: #fff;
  text-align: left;
}

.plan-view-aside-body {
  padding: 28px 20px 16px 20px;
}

.plan-view-main {
  text-align: left;
  padding: 20px;
}

.plan-view-form ::v-deep .el-form-item {
  margin-bottom: 14px;
}

.plan-view-state-line {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.state-label {
  color: #606266;
  font-size: 13px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.panel-subtitle {
  margin-top: -6px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 12px;
}

.panel-gap {
  margin-top: 20px;
}

.view-action-row {
  display: flex;
  justify-content: flex-start;
  gap: 8px;
}

.view-section-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 14px 10px 14px;
  background: #fff;
}

.view-table ::v-deep .el-table__header th {
  background: #f7f9fc;
  color: #4a5568;
}

::v-deep .plan-logs-dialog .el-dialog__body {
  max-height: 75vh;
  overflow-y: auto;
  padding-top: 10px;
}

.log-toolbar {
  position: sticky;
  top: 0;
  z-index: 6;
  background: #fff;
  padding: 6px 0 10px 0;
  margin-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-toolbar-left {
  display: flex;
  align-items: center;
}

.log-toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.log-table ::v-deep .el-table__header-wrapper th {
  background: #f8fafc;
}

.log-table ::v-deep .plan-log-row-error > td {
  background: #fff1f0 !important;
}

.log-table ::v-deep .plan-log-row-warn > td {
  background: #fffbe6 !important;
}

.log-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.log-cell-content {
  max-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}

.log-cell-content.expanded {
  max-height: none;
  white-space: normal;
  line-height: 1.5;
}

.log-expand-btn {
  padding: 0;
  margin-top: 2px;
}

.notify-section {
  margin-top: 12px;
  text-align: left;
}

.notify-alert {
  margin-bottom: 8px;
}

.notify-toolbar {
  margin-top: 8px;
  text-align: right;
}

.notify-count {
  margin-left: 12px;
  color: #909399;
}

.log-footer-actions {
  margin-top: 12px;
  text-align: right;
}

.log-plan-state {
  margin-left: 10px;
}

@media (max-width: 1400px) {
  .plan-list-container {
    border-radius: 6px;
  }

  .plan-view-aside-body {
    padding: 18px 14px 12px 14px;
  }

  .plan-view-main {
    padding: 12px;
  }

  .view-section-card {
    padding: 10px;
  }

  .plan-edit-footer-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .footer-actions-right {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .log-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

</style>
