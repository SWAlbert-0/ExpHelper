<template>
  <div>
    <!-- 表格数据 -->
    <el-table
      v-loading="loading_data"
      element-loading-text="加载中"
      :data="table_config.table_data"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        v-if="table_config.checkbox"
        type="selection"
        width="40"
      ></el-table-column>
      <template v-for="item in this.table_config.thead">
        <el-table-column
          v-if="item.type === 'function'"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
        >
          <template slot-scope="scope">
            <span
              v-html="item.callback && item.callback(scope.row, item.prop)"
            ></span>
          </template>
        </el-table-column>
        <!-- 插槽slot -->
        <el-table-column
          v-else-if="item.type === 'slot'"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
        >
          <template slot-scope="scope">
            <slot :name="item.slotName" :data="scope.row"></slot>
          </template>
        </el-table-column>
        <el-table-column
          v-else-if="item.type === 'index'"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
          type="index"
        >
        </el-table-column>
        <el-table-column
          v-else
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
        >
        </el-table-column>
      </template>
    </el-table>
    <el-row class="padding-top-30">
      <el-col :span="4"><div style="padding: 20px;"></div></el-col>
      <el-col :span="20">
        <el-pagination
          v-if="table_config.pagination"
          class="pull-right"
          background
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="10"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
        </el-pagination>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import algorithmConfig from "@/views/algorithm/algorithmConfig";
export default {
  name: "TableComponent",
  props: {
    config: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      loading_data: false,
      table_data: [],
      table_config: {
        thead: [],
        checkbox: true,
        table_data: [],
        pagination: true,
        data: {}
      },
      // 页码
      total: 0,
      // 当前页码
      currentPage: 1,
      pageSize: 10
    };
  },
  watch: {
    config: {
      handler(newValue) {
        this.initConfig();
      },
      immediate: true,
      deep: true
    }
  },
  beforeMount() {
    // this.getParkingList();
  },
  methods: {
    initConfig() {
      for (const key in this.config) {
        if (Object.keys(this.table_config).includes(key)) {
          this.table_config[key] = this.config[key];
        }
      }
      this.total = this.table_config.table_data.length;
      // this.loadData();
    },
    handleSelectionChange(val) {
      this.$emit("showSelect", val);
    },
    /** 页码 */
    handleSizeChange(val) {
      this.pageSize = val;
      algorithmConfig.methods.getAlgList(this.currentPage, this.pageSize);
    },
    handleCurrentChange(val) {
      this.pageNum = val;
      algorithmConfig.methods.getAlgList(this.currentPage, this.pageSize);
    }
  }
};
</script>

<style>
.padding-top-30 {
  padding-top: 30px;
}
</style>
