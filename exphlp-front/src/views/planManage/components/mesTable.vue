<template>
  <div>
    <el-table :data="tableData">
      <el-table-column type="index" width="50"> </el-table-column>

      <el-table-column prop="name" label="姓名"> </el-table-column>

      <el-table-column prop="email" label="email">
        <template slot-scope="scope">
          <el-checkbox
            v-model="scope.row.eChecked"
            :disabled="!isShow"
          ></el-checkbox>
          <span style="margin-left: 10px">{{ scope.row.email }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="wnum" label="微信号">
        <template slot-scope="scope">
          <el-checkbox
            v-model="scope.row.wChecked"
            :disabled="!isShow"
          ></el-checkbox>
          <span style="margin-left: 10px">{{ scope.row.email }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  props: ["tableData"],
  data() {
    return {
      isShow: false
    };
  }
};
</script>

<style lang="less" scoped>
</style>
