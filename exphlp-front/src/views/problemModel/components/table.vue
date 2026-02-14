<template>
  <div>
    <div v-if="multipleSelection.length > 0" style="padding: 5px">
      <span>
        已选中：
        <span style="color: blue">{{ multipleSelection.length }}</span>
        项 &nbsp;
      </span>
      <el-popconfirm title="确定要删除选中内容吗？" @confirm="multiDel">
        <el-button
          slot="reference" size="mini" type="danger"
        >批量删除
        </el-button>
      </el-popconfirm>
    </div>
    <el-table
      :data="tableData"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"> </el-table-column>

      <el-table-column prop="instId" label="问题ID" width="flexColumnWidth(label,prop)"> </el-table-column>

      <el-table-column prop="instName" label="问题名称" width="flexColumnWidth(label,prop)">
      </el-table-column>

      <el-table-column prop="description" label="问题描述" width="flexColumnWidth(label,prop)"> </el-table-column>

      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button
            size="mini" @click="handleLook(scope.row)"
          >查看
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button
            size="mini" @click="handleEdit(scope.row)"
          >编辑
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-popconfirm
            title="确定要删除当前内容吗？"
            @confirm="del(scope.row)"
          >
            <el-button
              slot="reference" size="mini" type="danger"
            >删除
            </el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <Dialog :show.sync="show" :dialog-title.sync="title" :row-data="rowData" />
  </div>
</template>

<script>
import Dialog from "./dialog";
import { getProbList, getProById, deleteProbById } from "@/api/vadmin/problem";

export default {
  components: {
    Dialog
  },
  props: ["data"],
  data() {
    return {
      tableData: [],
      title: "",
      show: false,
      rowData: "",
      multipleSelection: []
    };
  },
  created() {
    this.getProbs(1, 10);
  },
  methods: {
    getProbs(pageNum, pageSize) {
      getProbList(pageNum, pageSize).then(res => {
        console.log(res);
        this.tableData = res;
      });
    },
    getProbById(proId) {
      getProById(proId).then(res => {
        console.log(res);
        this.rowData = res;
      });
    },
    handleLook(row) {
      this.getProbById(row.instId);
      this.show = true;
      this.title = "查看";
    },

    handleEdit(row) {
      console.log(row);
      this.getProbById(row.instId);
      this.show = true;
      this.title = "编辑";
    },

    del(row) {
      deleteProbById(row.instId).then(res => {
      });
      this.tableData = this.tableData.filter((i) => i.instId != row.instId);
    },

    handleSelectionChange(val) {
      this.multipleSelection = val;
    },

    multiDel() {
      for (let a = 0; a < this.multipleSelection.length; a++) {
        this.tableData = this.tableData.filter(
          (i) => i.key != this.multipleSelection[a].key
        );
      }
    }
  }
};
</script>

<style lang="scss" scoped>
</style>
