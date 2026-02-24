<template>
  <div>
    <div v-show="editVisible">
      <el-container style="height: 700px; border: 1px solid #eee">
        <el-aside width="400px">
          <div style="padding-top: 50px">
            <el-form :model="exePlan" ref="exePlan" label-width="100px" class="demo-dynamic">
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
                <el-button type="primary" @click="backToExePlanTable()">返 回</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-aside>
        <el-main>
          <div class="right-card">
            <div v-if="rightVisible" style="height: 100%; overflow: auto">
              <el-steps :active="active" finish-status="success" align-center>
                <el-step title="算法选择及配置"></el-step>
                <el-step title="通知人员"></el-step>
                <el-step title="提交"></el-step>
              </el-steps>
              <el-button v-show="this.active != 3" style="margin-top: 12px" @click="next">下一步</el-button>
              <el-button v-show="this.active != 1" style="margin-top: 12px" @click="prev">上一步</el-button>
            </div>
          </div>
          <div v-show="this.active == 1 && rightVisible">
            <br>
            <el-row>
              <el-col :span="4">
                <router-link :to="{ path: '/algorithmConfig/index', query: { source: 'planManage' } }">
                  <el-button type="success">添加算法</el-button>
                </router-link>
              </el-col>
              <el-col :span="15">
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
              <el-col :span="2">
                <el-button type="primary" @click="addAlgInfo()">新增</el-button>
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
            <br>
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
          <div v-show="this.active == 3 && rightVisible">
            <br><br><br>
            <el-button type="primary" @click="submitExePlan()">提 交</el-button>
          </div>
        </el-main>
      </el-container>
    </div>
    <div v-show="showExePlanTableVisible">
      <el-container>
        <el-header style="padding-top: 20px" height="170px">
          <el-alert
            title="推荐使用“执行向导”：问题实例 -> 算法 -> 环境检查 -> 一键执行。服务名必须与 Nacos 注册名一致。"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-form :model="search" :inline="true" class="demo-form-inline" align="center">
            <el-row type="flex">
              <el-col :span="5">
                <el-form-item label="计划名称">
                  <el-input
                    v-model="search.planName"
                    placeholder="请输入计划名称"
                    clearable
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item label="计划状态">
                  <el-select v-model="search.exeState" placeholder="请输入计划状态" clearable>
                    <el-option
                      v-for="selectItem in options"
                      :key="selectItem.value"
                      :value="selectItem.value"
                      :label="selectItem.label"
                    ></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item label="开始日期">
                  <el-date-picker
                    v-model="search.exeStartTime"
                    type="date"
                    placeholder="选择开始日期"
                    clearable
                  >
                  </el-date-picker>
                </el-form-item>
              </el-col>
              <el-col :span="5">
                <el-form-item label="结束日期">
                  <el-date-picker
                    v-model="search.exeEndTime"
                    type="date"
                    placeholder="选择结束日期"
                    clearable
                  >
                  </el-date-picker>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <el-row>
            <el-col :span="12">
              <div class="grid-content bg-purple">
                <el-button type="success" icon="el-icon-plus" @click="addExePlan()">添加</el-button>
                <el-button type="primary" icon="el-icon-magic-stick" @click="wizardVisible = true">执行向导</el-button>
                <el-button type="danger" icon="el-icon-delete" @click="deleteBatchExePlan()">批量删除</el-button>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="grid-content action-group-right">
                <el-button type="primary" icon="el-icon-search"
                           @click="pageHelper.currentPageNum = 1, searchByCondition()">查询
                </el-button>
                <el-button type="default" icon="el-icon-refresh" @click="clearSearchCondition()">刷新</el-button>
              </div>
            </el-col>
          </el-row>
        </el-header>
        <el-main>
          <el-table
            :data="exePlans"
            border
            fit
            highlight-current-row
            @selection-change="handleSelectionChange1"
          >
            <el-table-column type="selection" width="60" align="center"></el-table-column>
            <el-table-column prop="planId" label="计划ID" width="220" align="center"></el-table-column>
            <el-table-column prop="planName" label="计划名称" align="center"></el-table-column>
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
      <el-container style="height: 700px; border: 1px solid #eee">
        <el-aside width="400px">
          <div style="padding-top: 50px">
            <el-form :model="showedExePlan" label-width="80px">
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
            <el-button type="warning" @click="openPlanLogsDialog">执行日志</el-button>
            <el-button type="primary" @click="backFromView">返 回</el-button>
          </div>
        </el-aside>
        <el-main>
          <span>算法信息</span>
          <br><br>
          <el-table
            :data="showedExePlan.algRunInfos"
            height="250"
            border
            highlight-current-row
            style="width: 100%">
            <el-table-column type="index" label="序号" width="150" align="center"></el-table-column>
            <el-table-column prop="showedAlgName" label="算法名称" width="180" align="center"></el-table-column>
            <el-table-column prop="description" label="算法描述" align="center"></el-table-column>
            <el-table-column prop="runNum" label="运行次数" width="180" align="center"></el-table-column>
            <el-table-column label="参数列表" align="center">
              <template slot-scope="scope">
                <el-button size="mini" @click="viewRunParas(scope.row)">查看</el-button>
              </template>
            </el-table-column>
            <el-table-column label="执行结果" align="center">
              <template slot-scope="scope">
                <el-button size="mini" @click="showExeResults(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <br><br>
          <span>通知人员</span>
          <br><br>
          <el-table
            :data="showedExePlan.userInfos"
            height="250"
            border
            highlight-current-row
            style="width: 100%">
            <el-table-column type="index" label="序号" width="150" align="center"></el-table-column>
            <el-table-column prop="userName" label="用户名称" width="180" align="center"></el-table-column>
            <el-table-column prop="email" label="邮箱" align="center"></el-table-column>
            <el-table-column prop="wechat" label="微信" width="180" align="center"></el-table-column>
          </el-table>
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
              <el-descriptions-item label="Spread(Δ)">{{ formatMetric(exeResultDetail.aggregate.spreadDeltaMean) }}</el-descriptions-item>
              <el-descriptions-item label="指标版本">{{ exeResultDetail.metricVersion || '-' }}</el-descriptions-item>
            </el-descriptions>
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
              <el-table-column label="Spread(Δ)" width="110" align="center">
                <template slot-scope="scope">{{ formatMetric(scope.row.spreadDelta) }}</template>
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
            :before-close="closePlanLogsDialog"
            :close-on-click-modal="false"
          >
            <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
              <div>
                <el-radio-group v-model="planLogScope" size="mini" @change="switchPlanLogScope">
                  <el-radio-button label="latest">最新执行</el-radio-button>
                  <el-radio-button label="all">全部历史</el-radio-button>
                </el-radio-group>
                <span style="margin-left: 12px; color: #909399;">执行批次：{{ planLogExecutionId || '-' }}</span>
              </div>
              <div>
                <el-button size="mini" icon="el-icon-refresh" @click="fetchPlanLogs(true)">刷新</el-button>
                <el-button size="mini" icon="el-icon-download" @click="exportPlanLogsAs('csv')">导出CSV</el-button>
                <el-button size="mini" icon="el-icon-download" @click="exportPlanLogsAs('json')">导出JSON</el-button>
              </div>
            </div>
            <el-table :data="planLogs" border fit height="420">
              <el-table-column property="seq" label="序号" width="90" align="center"></el-table-column>
              <el-table-column property="ts" label="时间" width="180" align="center">
                <template slot-scope="scope">
                  {{ formatTimestampToDateTime(scope.row.ts) }}
                </template>
              </el-table-column>
              <el-table-column property="level" label="级别" width="90" align="center"></el-table-column>
              <el-table-column property="stage" label="阶段" width="120" align="center"></el-table-column>
              <el-table-column property="executionId" label="批次ID" width="220" show-overflow-tooltip></el-table-column>
              <el-table-column property="algId" label="算法ID" width="160" show-overflow-tooltip></el-table-column>
              <el-table-column property="runIndex" label="run" width="70" align="center"></el-table-column>
              <el-table-column property="probInstId" label="实例ID" width="160" show-overflow-tooltip></el-table-column>
              <el-table-column property="message" label="内容" min-width="260"></el-table-column>
              <el-table-column property="details" label="详情" min-width="180" show-overflow-tooltip></el-table-column>
            </el-table>
            <div style="margin-top: 12px; text-align: left;">
              <el-divider content-position="left">通知记录</el-divider>
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
              <div style="margin-top: 8px; text-align: right;">
                <el-button size="mini" icon="el-icon-refresh" @click="fetchNotificationLogs">刷新通知记录</el-button>
                <el-button size="mini" type="warning" @click="resendFailedByExecution">补发本批次失败通知</el-button>
                <span style="margin-left: 12px; color: #909399;">共 {{ notificationTotal }} 条</span>
              </div>
            </div>
            <div style="margin-top: 12px; text-align: right;">
              <el-button size="mini" type="primary" @click="runPreCheckFromLogs">执行前检查</el-button>
              <el-button
                size="mini"
                type="warning"
                :disabled="showedExePlan.exeState !== '异常结束'"
                @click="reExecutePlan(showedExePlan)"
              >重新执行</el-button>
              <span>计划状态：{{ showedExePlan.exeState }}</span>
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
import { planResultMethods } from "@/views/planManage/modules/planResultMethods";
import { planLogMethods } from "@/views/planManage/modules/planLogMethods";
import { planExecutionMethods } from "@/views/planManage/modules/planExecutionMethods";
import { planDeleteMethods } from "@/views/planManage/modules/planDeleteMethods";

export default {
  components: { ExecutionWizard },
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
    }
  },
  created() {
    this.listProbInsts();
    this.getAlgInfos();
    this.getUserInfos();
    this.getExePlans();
    this.countAllExePlans();
  },
  beforeDestroy() {
    this.stopPlanLogPolling();
  },
  methods: {
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
      if (this.active++ > 2) this.active = 1;
    },
    prev() {
      if (this.active-- < 0) this.active = 3;
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
              if(this.duplicateParaFlag){
                this.$message({type: "warning", message: "相同算法存在相同的运行参数，请修改运行参数后再提交",});
              }else {
                this.exePlan.exeStartTime = new Date(this.exePlan.exeStartTime).getTime();
                this.exePlan.exeEndTime = new Date(this.exePlan.exeEndTime).getTime();
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
          for (var i = 0; i < this.options.length; i++) {
            if (this.exePlan.exeState == this.options[i].value) {
              this.exePlan.exeState = i + 1;
              break;
            }
          }
          var dateObject1 = new Date(this.exePlan.exeStartTime);
          var dateObject2 = new Date(this.exePlan.exeEndTime);
          this.exePlan.exeStartTime = dateObject1.getTime();
          this.exePlan.exeEndTime = dateObject2.getTime();
          updateExePlanById(this.exePlan)
            .then(res => {
              this.$message({type: "success", message: "编辑成功",});
              this.editVisible = false;
              this.showExePlanTableVisible = true;
              this.clearSearchCondition();
            });
        }
      }

    },

    getExePlans() {
      getExePlans(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.exePlans = res;
        for (var i = 0; i < this.exePlans.length; i++) {
          this.exePlans[i].exeState = this.options[this.exePlans[i].exeState - 1].value;
          this.exePlans[i].exeStartTime = this.formatTimestampToDateTime(this.exePlans[i].exeStartTime);
          this.exePlans[i].exeEndTime = this.formatTimestampToDateTime(this.exePlans[i].exeEndTime);
        }
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
          if (res.exeState == '') {
            res.exeState = '未执行';
          } else {
            res.exeState = this.options[res.exeState - 1].value;
          }
          if (res.exeStartTime == 0) {
            res.exeStartTime = '';
          } else {
            res.exeStartTime = this.formatTimestampToDateTime(res.exeStartTime);
          }
          if (res.exeEndTime == 0) {
            res.exeEndTime = '';
          } else {
            res.exeEndTime = this.formatTimestampToDateTime(res.exeEndTime);
          }
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
      this.$refs.userInfos.clearSelection();
      this.countAllExePlans();
      getExePlans(this.pageHelper.currentPageNum, this.pageHelper.pageSize).then(res => {
        this.exePlans = res;
        for (var i = 0; i < this.exePlans.length; i++) {
          if (this.exePlans[i].exeState == '') {
            this.exePlans[i].exeState = '未执行';
          } else {
            this.exePlans[i].exeState = this.options[this.exePlans[i].exeState - 1].value;
          }
          if (this.exePlans[i].exeStartTime == 0) {
            this.exePlans[i].exeStartTime = '';
          } else {
            this.exePlans[i].exeStartTime = this.formatTimestampToDateTime(this.exePlans[i].exeStartTime);
          }
          if (this.exePlans[i].exeEndTime == 0) {
            this.exePlans[i].exeEndTime = '';
          } else {
            this.exePlans[i].exeEndTime = this.formatTimestampToDateTime(this.exePlans[i].exeEndTime);
          }
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
      var currentDateTime = this.getCurrentDateTime();
      this.algId = '';
      this.exePlan = {
        planName: '',
        probInsts: [],
        probInstIds: [],
        exeState: '',
        exeStartTime: currentDateTime,
        exeEndTime: currentDateTime,
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
    backFromView() {
      this.stopPlanLogPolling();
      this.dialogPlanLogsVisible = false;
      this.planLogExecutionId = "";
      this.planLogScope = "latest";
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
      this.$refs['exePlan'].clearValidate();
      this.$refs.userInfos.clearSelection();
      this.clearSearchCondition();
      this.editVisible = false;
      this.showExePlanTableVisible = true
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
    reExecutePlan(scope) {
      return planExecutionMethods.reExecutePlan.call(this, scope);
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
      const dateObject = new Date(timestamp);
      const options = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      };
      const formattedDate = new Intl.DateTimeFormat('zh-CN', options).format(dateObject);
      return formattedDate.replace(/\//g, '-');
    },
    getCurrentDateTime() {
      const currentDateTime = new Date();
      const options = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false, // 24小时制
      };
      return new Intl.DateTimeFormat('zh-CN', options).format(currentDateTime).replace(/\//g, '-');
    },
    buildProbInstsTreeData(probInsts) {
      var categoryNames = [];
      var treeData = [];
      for (var i = 0; i < probInsts.length; i++) {
        categoryNames.push(probInsts[i].categoryName);
      }
      categoryNames = categoryNames.filter((value, index, self) => {
        return self.indexOf(value) === index;
      });
      for (var i = 0; i < categoryNames.length; i++) {
        treeData.push({'id': categoryNames[i], 'label': categoryNames[i], 'children': []});
      }
      for (var i = 0; i < probInsts.length; i++) {
        var index = categoryNames.indexOf(probInsts[i].categoryName);
        treeData[index].children.push({'id': probInsts[i].instId, 'label': probInsts[i].instName})
      }
      return treeData;
    },
    getCheckedNodes() {
      var selectedProbInsts = this.$refs.tree.getCheckedNodes();
      var probInstIds = [];
      for (var i = 0; i < selectedProbInsts.length; i++) {
        if (selectedProbInsts[i].children == null) {
          probInstIds.push(selectedProbInsts[i].id);
        }
      }
      return probInstIds;
    },
    handleSameAlgName(algRunInfos) {
      var algIds = {};
      var algNames = [];
      for (var i = 0; i < algRunInfos.length; i++) {
        if (!algIds.hasOwnProperty(algRunInfos[i].algId)) {
          algIds[algRunInfos[i].algId] = [];
        }
        algIds[algRunInfos[i].algId].push(i);
      }
      for (var key in algIds) {
        if (algIds.hasOwnProperty(key)) {
          if (algIds[key].length > 1) {
            for (var i = 0; i < algIds[key].length; i++) {
              algNames[algIds[key][i]] = algRunInfos[algIds[key][i]].algName + '-' + (i + 1);
            }
          } else {
            algNames[algIds[key]] = algRunInfos[algIds[key]].algName;
          }
        }
      }
      return algNames;
    },
    hasDuplicateParas() {
      var arr = [];
      for (var i = 0; i < this.exePlan.algRunInfos.length; i++) {
        arr.push({'runParas':this.exePlan.algRunInfos[i].runParas, 'algName':this.exePlan.algRunInfos[i].algName});
      }
      const seen = new Set();
      for (const obj of arr) {
        const objString = JSON.stringify(obj);
        if (seen.has(objString)) {
          this.duplicateParaFlag = true;
          return true;
        }
        seen.add(objString);
      }
      this.duplicateParaFlag = false;
      return false;
    },
  }
}
</script>

<style scoped lang="scss">
.el-aside {
  background-color: white;
  text-align: center;
}

.el-main {
  text-align: center;
  border: solid 1px #eee;
}

.action-group-right {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}


</style>
