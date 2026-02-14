<template>
  <div>
    <el-card class="center">
      <el-form :inline="true" :model="search" class="demo-form-inline">
        <el-row type="flex">
          <el-col :span="5">
            <el-form-item label="计划名称">
              <el-input
                v-model="search.name"
                placeholder="请输入计划名称"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="计划状态">
              <el-select v-model="search.status" placeholder="请输入计划状态">
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
                v-model="search.startTime"
                type="date"
                placeholder="选择开始日期"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="search.endTime"
                type="date"
                placeholder="选择结束日期"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item>
              <el-button type="primary">查询</el-button>
            </el-form-item>
            <el-form-item>
              <el-button @click="goToAdd()">添加</el-button>
            </el-form-item>
            <el-form-item v-if="this.selectKeys.length > 0">
              <el-button type="danger">批量删除</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
    <br />
    <el-card class="center">
      <!-- 表格数据 -->
      <TabalData
        ref="table"
        :config="table_config"
        @showSelect="getSelectValue"
      >
        <!--操作-->
        <template #operation="slotData">
          <el-button size="mini" @click="goToTree()">查看 </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button
            size="mini"
            @click="goToEdit(slotData.data)"
          >编辑
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-popconfirm
            title="确定要删除当前内容吗？"
            @confirm="delConfirm(slotData.data.name)"
          >
            <el-button
              slot="reference"
              size="mini"
              type="danger"
            >删除
            </el-button>
          </el-popconfirm>
        </template>
      </TabalData>
    </el-card>
    <algcommonDialog ref="algDialog" />
  </div>
</template>

<script>
import TabalData from "../../components/tableData";
import algcommonDialog from "./dialog/index.vue";
export default {
  components: { TabalData, algcommonDialog },
  data() {
    return {
      // 表格配置
      table_config: {
        thead: [
          {
            label: "计划id",
            prop: "index",
            type: "index",
            width: 100
          },
          {
            label: "计划名称",
            prop: "name",
            type: "name"
          },
          {
            label: "开始时间",
            prop: "startTime",
            type: "startTime"
          },
          {
            label: "结束时间",
            prop: "endTime",
            type: "endTime"
          },
          {
            label: "执行状态",
            prop: "status",
            type: "status"
          },
          {
            label: "操作",
            type: "slot",
            width: 240,
            slotName: "operation"
          }
        ],
        table_data: [
          {
            name: "计划示例1",
            startTime: "2021-08-10 10:05:01",
            endTime: "2021-08-13 10:59:01",
            status: "未执行"
          },
          {
            name: "计划示例2",
            startTime: "2021-08-10 10:05:01",
            endTime: "2021-08-13 10:59:01",
            status: "进行中"
          },

          {
            name: "计划示例3",
            startTime: "2021-08-10 10:05:01",
            endTime: "2021-08-13 10:59:01",
            status: "正常结束"
          },
          {
            name: "计划示例4",
            startTime: "2021-08-10 10:05:01",
            endTime: "2021-08-13 10:59:01",
            status: "异常结束"
          }
        ]
      },
      search: {
        name: "",
        status: "",
        startTime: "",
        endTime: ""
      },
      selectKeys: "",
      options: [
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
      ]
    };
  },
  methods: {
    showDialog(key, value) {
      console.log(key);
      key == "add"
        ? this.$refs.algDialog.add(value)
        : key == "edit"
          ? this.$refs.algDialog.edit(value)
          : this.$refs.algDialog.detail(key, value);
    },
    getSelectValue(val) {
      console.log("val", val);
      this.selectKeys = val;
    },
    delConfirm(name) {
      this.table_config.table_data = this.table_config.table_data.filter(
        item => item.name != name
      );
      console.log(this.table_config.table_data);
    },
    goToTree() {
      this.$router.push({ name: "planDetail" });
    },
    goToAdd() {
      this.$router.push({ name: "addPlan" });
    },
    goToEdit(record) {
      this.$router.push({ name: "addPlan", params: record });
    }
  }
};
</script>

<style>
/* .content {
  height: 100%;
  width: 100%;
  display: flex;
  flex-flow: column;
  align-items: center;
}
.center {
  margin-top: 30px;
  width: 90%;
  display: flex;
  flex-flow: column;
} */
</style>
