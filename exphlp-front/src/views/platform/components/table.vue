<template>
  <div>
    <div style="padding: 5px" v-if="multipleSelection.length > 0">
      <span>
        已选中：
        <span style="color: blue">{{ multipleSelection.length }}</span>
        项 &nbsp;
      </span>
      <el-popconfirm title="确定要删除选中内容吗？" @confirm="multiDel">
        <el-button slot="reference" size="mini" type="danger"
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

      <el-table-column type="index" width="50"> </el-table-column>

      <!-- <el-table-column prop="sid" label="学号" width="150"> </el-table-column> -->

      <el-table-column prop="name" label="姓名" width="100"> </el-table-column>

      <el-table-column prop="password" label="密码" width="150">
      </el-table-column>

      <el-table-column prop="email" label="email" width="200">
      </el-table-column>

      <el-table-column prop="wnum" label="微信号" width="200">
      </el-table-column>

      <el-table-column label="操作">
        <template slot-scope="scope">
          <el-button size="mini" @click="handleEdit(scope.$index, scope.row)"
            >编辑
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-popconfirm
            title="确定要删除当前内容吗？"
            @confirm="del(scope.row)"
          >
            <el-button slot="reference" size="mini" type="danger"
              >删除
            </el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <Dialog :show.sync="show" :dialogTitle.sync="title" :rowData="rowData" />
  </div>
</template>

<script>
import Dialog from "./dialog";

export default {
  props: ["data"],
  components: {
    Dialog,
  },
  data() {
    return {
      tableData: this.data,
      title: "",
      show: false,
      rowData: "",
      multipleSelection: [],
    };
  },
  methods: {
    handleLook(index, row) {
      this.show = true;
      this.title = "查看";
      this.rowData = row;
      console.log(index, row);
    },

    handleEdit(index, row) {
      this.show = true;
      this.title = "编辑";
      this.rowData = row;
      console.log(index, row);
    },

    handleSelectionChange(val) {
      this.multipleSelection = val;
    },

    del(row) {
      this.tableData = this.tableData.filter((i) => i.id != row.id);
    },

    multiDel() {
      for (let a = 0; a < this.multipleSelection.length; a++) {
        this.tableData = this.tableData.filter(
          (i) => i.id != this.multipleSelection[a].id
        );
      }
    },
  },
};
</script>

<style lang="scss" scoped>
</style>