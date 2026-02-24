<template>
  <div>
    <div class="ele-page">
      <div class="left-card">
        <!-- 左侧树 -->
        <el-card style="height: 100%; overflow: auto">
          <el-form :inline="true" label-width="120px">
            <el-form-item v-show="!editData.name" label="计划名称">
              <el-input
                v-model="state.name"
                style="width: 220px"
                placeholder="请输入计划名称"
              ></el-input>
            </el-form-item>
            <el-form-item v-show="!editData.name">
              <el-button type="primary" @click="addTree">新增</el-button>
            </el-form-item>
          </el-form>
          <VueForm
            v-show="editData.name"
            :form-data="form_data"
            :form-item="form_item"
            :form-handler="form_handler"
          >
          </VueForm>
          <el-tree
            :data="editData.name ? treeData : data"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
          >
            <span slot-scope="{ node, data }" class="custom-tree-node">
              <span>{{ node.label }}</span>
              <span>
                <i
                  v-if="data.children"
                  class="el-icon-circle-plus-outline"
                  style="margin-right: 10px; color: blue"
                  @click="() => append(data)"
                ></i>
                <i
                  class="el-icon-remove-outline"
                  style="color: red"
                  @click="() => remove(node, data)"
                ></i>
              </span>
            </span>
          </el-tree>
        </el-card>
      </div>
      <div class="right-card">
        <div
          v-if="rightVisible || editData.name"
          style="height: 100%; overflow: auto"
        >
          <el-card class="center">
            <el-steps :active="active" finish-status="success">
              <el-step title="算法选择及配置"> </el-step>
              <el-step title="通知人员"></el-step>
              <el-step title="提交"></el-step>
            </el-steps>

            <el-button
              v-show="this.active != 3"
              style="margin-top: 12px"
              @click="next"
            >下一步</el-button>
            <el-button
              v-show="this.active != 1"
              style="margin-top: 12px"
              @click="prev"
            >上一步</el-button>
          </el-card>
          <br />
          <div v-show="this.active == 1">
            <el-card>
              <el-form :inline="true" :model="search" class="demo-form-inline">
                <el-row type="flex" justify="space-between">
                  <el-col :span="8">
                    <el-form-item label="算法">
                      <el-select
                        v-model="search.calName"
                        placeholder="请选择解决问题的算法"
                        @change="handlerChange"
                      >
                        <el-option
                          v-for="selectItem in options"
                          :key="selectItem.value"
                          :value="selectItem.value"
                          :label="selectItem.label"
                        ></el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item>
                      <el-button
                        type="primary" @click="tableAdd"
                      >新增</el-button>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-card>
            <br />
            <el-card class="center">
              <div
                v-if="this.selectKeys.length > 0"
                style="float: right; margin-bottom: 10px"
              >
                <el-button type="danger">批量删除</el-button>
              </div>

              <!-- 表格数据 -->
              <TabalData
                ref="table"
                :config="table_config"
                @showSelect="getSelectValue"
              >
                <template #num="slotData">
                  <el-input
                    v-if="slotData.data.editable"
                    v-model="slotData.data.num"
                    style="margin: -5px 0"
                    :value="slotData.data.num"
                  />

                  <template v-else>{{ slotData.data.num }}</template>
                </template>
                <template #operation="slotData">
                  <el-button
                    size="mini" @click="showDialog(slotData.data)"
                  >编辑参数配置
                  </el-button>
                  <el-divider direction="vertical"></el-divider>
                  <el-button
                    size="mini" @click="editTable(slotData.data)"
                  >编辑
                  </el-button>
                  <el-divider direction="vertical"></el-divider>
                  <el-popconfirm
                    title="确定要删除当前内容吗？"
                    @confirm="delConfirm(slotData.data.name)"
                  >
                    <el-button
                      slot="reference" size="mini" type="danger"
                    >删除
                    </el-button>
                  </el-popconfirm>
                </template>
              </TabalData>
            </el-card>
          </div>

          <div v-show="this.active == 2">
            <el-card class="center">
              <!-- 表格数据 -->
              <TabalData
                ref="table"
                :config="table_config1"
                @showSelect="getSelectValue"
              >
                <template
                  v-for="col in ['email', 'wechat']"
                  :slot="col"
                  slot-scope="slotData"
                >
                  <div :key="col">
                    <el-checkbox v-model="checked">{{
                      slotData.data[col]
                    }}</el-checkbox>
                  </div>
                </template>
                <template #operation="slotData">
                  <el-button
                    size="mini"
                    @click="showDialog('edit', slotData.data)"
                  >查看
                  </el-button>

                  <el-divider direction="vertical"></el-divider>
                  <el-popconfirm
                    title="确定要删除当前内容吗？"
                    @confirm="delConfirm(slotData.data.name)"
                  >
                    <el-button
                      slot="reference" size="mini" type="danger"
                    >删除
                    </el-button>
                  </el-popconfirm>
                </template>
              </TabalData>
            </el-card>
          </div>

          <div v-show="this.active == 3" class="btn">
            <el-button type="primary" style="margin-right: 5px">保存</el-button>
            <el-button @click="resetform()">重置</el-button>
          </div>
        </div>
        <addDialog ref="config" @getVisible="getVisible" />
        <configModaledit ref="config1" />
      </div>
    </div>
  </div>
</template>

<script>
import { treeData } from "./js/index";
import TabalData from "../../components/tableData";
import VueForm from "../../components/form/index.vue";
import addDialog from "./dialog/addDialog.vue";
import configModaledit from "./dialog/configModaledit.vue";
export default {
  components: {
    TabalData,
    addDialog,
    configModaledit,
    VueForm
  },
  data() {
    return {
      active: 1,
      treeData,
      search: {
        name: ""
      },
      state: {
        name: "",
        status: "",
        startTime: "",
        endTime: ""
      },

      data: [],
      hadTable: [],
      rightVisible: false,
      selectKeys: [],
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
            label: "运行次数",
            prop: "num",
            width: 100,
            type: "slot",
            slotName: "num"
          },
          {
            label: "操作",
            type: "slot",
            width: 300,
            slotName: "operation"
          }
        ],
        table_data: []
      },
      table_config1: {
        thead: [
          {
            label: "姓名",
            prop: "name",
            type: "name",
            width: 100
          },
          {
            label: "email",
            prop: "email",
            type: "slot",
            slotName: "email"
          },
          {
            label: "微信",
            prop: "wechat",
            type: "slot",
            slotName: "wechat"
          }
        ],
        table_data: [
          {
            name: "韩绝",
            email: "	307148762@qq.com",
            wechat: "	lin15280375031"
          },
          {
            name: "李目一",
            email: "lmy3344@163.com",
            wechat: "lmy3344"
          }
        ]
      },
      tableData: [
        {
          name: "bFOA",
          content:
            "	2013年,Wang Ling,《A novel binary fruit fly optimization algorithm for solving the multidimensional knapsack problem》,《Knowledge-Based Systems》",
          num: "20",
          editable: false
        },
        {
          name: "HPSOGO",
          content:
            "2018年,Luis Fernando,《Multidimensional knapsack problem optimization using a binary particle swarm model with genetic operations》,《Soft Computing》",
          num: "20",
          editable: false
        }
      ],
      defaultProps: {
        children: "children",
        label: "label"
      },
      options: [
        {
          label: "bFOA",
          value: "bFOA"
        },
        {
          label: "HPSOGO",
          value: "HPSOGO"
        }
      ],
      editData: [],
      oldData: [],
      options1: [
        {
          label: "未执行",
          value: "未执行"
        },
        {
          label: "进行中",
          value: "进行中"
        },
        {
          label: "正常结束",
          value: "正常结束"
        },
        {
          label: "异常结束",
          value: "异常结束"
        }
      ],
      form_data: {
        name: "",
        status: "",
        startTime: "",
        endTime: ""
      },
      form_item: [
        { type: "Input", label: "计划名称", prop: "name", width: "220px" },
        { type: "Select", label: "状态", prop: "status", options: [] },
        { type: "date", label: "开始时间", prop: "startTime" },
        { type: "date", label: "结束时间", prop: "endTime" }
      ],
      form_handler: [
        {
          label: "保存修改",
          key: "submit",
          type: "primary",
          handler: () => this.submit()
        }
      ]
    };
  },
  created() {
    this.editData = this.$route.params;
    const data = this.form_item.filter((item) => item.type == "Select");
    console.log(data);
    data[0].options = this.options1;
    if (this.editData.name) {
      for (const key in this.editData) {
        this.form_data[key] = this.editData[key];
      }

      this.table_config.table_data = this.tableData;
    }
  },
  methods: {
    next() {
      if (this.active++ > 2) this.active = 1;
    },
    prev() {
      if (this.active-- < 0) this.active = 3;
    },
    showDialog(key, value) {
      console.log(key);

      this.$refs.config1.edit(key, value);
    },
    handleNodeClick(data) {
      console.log(data);
    },
    append(data) {
      console.log(data);
      this.$refs.config.treeAdd(data);
    },

    remove(node, data) {
      const parent = node.parent;
      const children = parent.data.children || parent.data;
      const index = children.findIndex((d) => d.id === data.id);
      children.splice(index, 1);
    },
    addTree() {
      // 通过时间戳生成 UUID
      const uuid = Math.round(new Date().getTime()).toString();
      console.log(this.state.name);
      if (this.data.length == 0) {
        this.data.push({
          id: uuid,
          label: this.state.name,
          children: []
        });
      }
    },
    handlerChange(value) {
      console.log(value);
      this.hadTable = this.tableData.filter((item) => item.name == value);
    },
    tableAdd() {
      console.log(this.hadTable);
      if (this.hadTable.length) {
        this.table_config.table_data.push(this.hadTable[0]);
      }
    },
    delConfirm(name) {
      this.table_config.table_data = this.table_config.table_data.filter(
        (item) => item.name != name
      );
      console.log(this.table_config.table_data);
    },
    resetform() {
      this.data = [];
      this.name = "";
      this.startTime = "";
      this.endTime = "";
    },
    getVisible() {
      this.rightVisible = true;
    },
    getSelectValue(val) {
      console.log("val", val);
      this.selectKeys = val;
    },
    editTable(record) {
      console.log(record);
      this.oldData = JSON.parse(JSON.stringify(record.name));

      // return false;
      const newData = [...this.table_config.table_data];
      console.log(newData);
      const target = newData.filter((item) => record.name === item.name)[0];
      console.log("target", target);
      if (target) {
        target.editable = !target.editable;
        this.table_config.table_data = newData;
      }
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
.btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
}
.el-tree-node__label {
  font-size: 15px;
}
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style>
