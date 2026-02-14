<template>
  <div>
    <div class="ele-page">
      <div class="left-card">
        <!-- 左侧树 -->
        <el-card style="height: 100%; overflow: auto">
          <el-tree
            class="treeClass"
            :data="treeData"
            :props="defaultProps"
            :default-expand-all="true"
            @node-click="handleNodeClick"
          ></el-tree>
        </el-card>
      </div>
      <div class="right-card">
        <div style="height: 100%; overflow: auto">
          <!-- <el-card class="center">
            <el-form :inline="true" :model="search" class="demo-form-inline">
              <el-row type="flex" justify="space-between">
                <el-col :span="8">
                  <el-form-item label="算法名称">
                    <el-input
                      v-model="search.name"
                      placeholder="请输入算法名称"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="4">
                  <el-form-item>
                    <el-button type="primary">查询</el-button>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>
          <br /> -->
          <el-card class="center">
            <div slot="header" class="clearfix">
              <span>问题示例1</span>
            </div>
            <!-- 表格数据 -->
            <TabalData ref="table" :config="table_config">
              <!--操作-->
              <template #operation="slotData">
                <el-button
                  size="small"
                  @click="showDialog('detail', slotData.data)"
                >查看</el-button>
              </template>
            </TabalData>

            <br />

            <el-card>
              <div slot="header" class="clearfix">
                <span>通知人员</span>
              </div>

              <MesTable :table-data="tableData" />
            </el-card>
          </el-card>
          <configModal ref="config" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { treeData } from "./js/index";
import TabalData from "../../components/tableData";
import configModal from "./dialog/configModal.vue";

import MesTable from "./components/mesTable.vue";
import { tableData } from "./data/tableData";

export default {
  components: {
    TabalData,
    configModal,
    MesTable
  },
  data() {
    return {
      tableData,

      treeData,
      search: {
        name: ""
      },
      // 表格配置
      table_config: {
        thead: [
          {
            label: "序号",
            prop: "index",
            type: "index",
            width: 50
          },
          {
            label: "算法名称",
            prop: "name",
            type: "name",
            width: 100
          },
          {
            label: "算法描述",
            prop: "content",
            type: "content"
          },

          {
            label: "操作",
            type: "slot",
            width: 140,
            slotName: "operation"
          }
        ],
        table_data: [
          {
            id: "1",
            name: "bFOA",
            content:
              "2013年,Wang Ling,《A novel binary fruit fly optimization algorithm for solving the multidimensional knapsack problem》,《Knowledge-Based Systems》"
          },
          {
            id: "2",
            name: "HPSOGO",
            content:
              "2018年,Luis Fernando,《Multidimensional knapsack problem optimization using a binary particle swarm model with genetic operations》,《Soft Computing》"
          }
        ],
        checkbox: false
      },
      defaultProps: {
        children: "children",
        label: "label"
      }
    };
  },
  methods: {
    showDialog(key, value) {
      this.$refs.config.detail(key, value);
    },
    handleNodeClick(data) {
      console.log(data);
    }
  }
};
</script>

<style>
.ele-page {
  display: flex;
  justify-content: center;
  height: 85vh;
}
.left-card {
  flex: 1;
}
.right-card {
  position: relative;
  flex: 2;
}
.el-tree-node__label {
  font-size: 15px;
}
</style>
