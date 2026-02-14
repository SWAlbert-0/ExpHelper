<template>
  <el-form ref="form" :model="formData" :label-width="labelWidth">
    <el-form-item
      v-for="item in formItem"
      :key="item.prop"
      :prop="item.prop"
      :label="item.label"
      :rules="item.rules"
    >
      <!--Input-->
      <el-input
        v-if="item.type === 'Input'"
        v-model.trim="formData[item.prop]"
        :placeholder="`${type_msg[item.type]}${item.label}`"
        :style="{ width: item.width }"
        :disabled="item.disabled"
      ></el-input>
      <!--textArea-->
      <el-input
        v-if="item.type === 'textArea'"
        type="textarea"
        v-model="formData[item.prop]"
        :disabled="item.disabled"
        :placeholder="`${type_msg[item.type]}${item.label}`"
        :rows="item.rows"
      ></el-input>
      <!--Select-->
      <el-select
        v-if="item.type === 'Select'"
        v-model.trim="formData[item.prop]"
        :placeholder="`${type_msg[item.type]}${item.label}`"
        :style="{ width: item.width }"
        :disabled="item.disabled"
      >
        <el-option
          v-for="selectItem in item.options"
          :key="selectItem.value || selectItem[item.select_vlaue]"
          :value="selectItem.value || selectItem[item.select_vlaue]"
          :label="selectItem.label || selectItem[item.select_label]"
        ></el-option>
      </el-select>
      <!--Selects-->
      <el-select
        v-if="item.type === 'Selects'"
        v-model.trim="formData[item.prop]"
        :placeholder="`${type_msg[item.type]}${item.label}`"
        :style="{ width: item.width }"
        :disabled="item.disabled"
        multiple
      >
        <el-option
          v-for="selectItem in item.options"
          :key="selectItem.value || selectItem[item.select_vlaue]"
          :value="selectItem.value || selectItem[item.select_vlaue]"
          :label="selectItem.label || selectItem[item.select_label]"
        >
        </el-option>
      </el-select>
      <el-button
        v-if="item.type === 'button'"
        :type="item.type1"
        @click="item.handler && item.handler()"
        >{{ "新增" + item.label }}</el-button
      >
      <!--date-->
      <el-date-picker
        v-if="item.type === 'date'"
        v-model="formData[item.prop]"
        :placeholder="`${type_msg[item.type]}${item.label}`"
        :style="{ width: item.width }"
        :disabled="item.disabled"
        type="date"
      >
      </el-date-picker>
    </el-form-item>

    <!--Table-->
    <el-form-item v-if="formTable && formTable.table_data.length > 0">
      <div style="width:100%">
        <TabalData ref="table" :config="formTable">
          <template
            v-for="col in ['name', 'type', 'value']"
            :slot="col"
            slot-scope="slotData"
          >
            <div :key="col">
              <el-input
                v-if="slotData.data.editable && col != 'type'"
                style="margin: -5px 0"
                :value="slotData.data[col]"
                v-model="slotData.data[col]"
              />
              <el-select
                v-else-if="slotData.data.editable && col == 'type'"
                v-model="slotData.data[col]"
                :value="slotData.data[col]"
              >
                <el-option
                  v-for="selectItem in option"
                  :key="selectItem.value"
                  :value="selectItem.value"
                  :label="selectItem.label"
                ></el-option>
              </el-select>
              <template v-else>{{ slotData.data[col] }}</template>
            </div>
          </template>
          <!--操作-->
          <template v-slot:operation="slotData">
            <el-button size="mini" @click="editInput(slotData.data)"
              >编辑
            </el-button>
            <el-divider direction="vertical"></el-divider>
            <el-popconfirm
              title="确定要删除当前内容吗？"
              @confirm="delConfirm(slotData.data.name)"
            >
              <el-button size="mini" slot="reference" type="danger"
                >删除
              </el-button>
            </el-popconfirm>
          </template>
        </TabalData>
      </div>
    </el-form-item>
    <!--按钮-->
    <el-form-item>
      <el-button
        v-for="item in formHandler"
        :key="item.key"
        :type="item.type"
        @click="item.handler && item.handler()"
        >{{ item.label }}</el-button
      >
    </el-form-item>
  </el-form>
</template>

<script>
import TabalData from "../../components/tableData";
export default {
  name: "Form",
  components: { TabalData },
  props: {
    formItem: {
      type: Array,
      default: () => []
    },
    formData: {
      type: Object,
      default: () => []
    },
    //按钮
    formHandler: {
      type: Array,
      default: () => []
    },
    formTable: {
      type: Object,
      default: () => {}
    },
    labelWidth: {
      type: String,
      default: "120px"
    }
  },
  data() {
    return {
      //是否存在必填规则
      type_msg: {
        Input: "请输入",
        textArea: "请输入",
        Select: "请选择",
        Selects: "请选择",
        date: "请选择"
      },
      oldData: [],
      option: [
        {
          value: "int",
          label: "int"
        },
        {
          value: "string",
          label: "string"
        },
        {
          value: "number",
          label: "number"
        },
        {
          value: "boolean",
          label: "boolean"
        },
        {
          value: "float",
          label: "float"
        },
        {
          value: "Object",
          label: "Object"
        }
      ]
    };
  },
  methods: {
    initFormData() {
      console.log(this.formItem);
      this.formItem.forEach(item => {
        //rules规则
        if (item.required) {
          this.rules(item);
        }
        //自定义规则
        // if (item.validator) {
        //   item.rules = item.validator;
        // }
      });
    },
    rules(item) {
      // console.log(item);

      const requestRules = [
        {
          required: true,
          message:
            item.requiredMsg || `${this.type_msg[item.type]}${item.label}`,
          trigger: "change"
        }
      ];
      //其他的rules的规则
      if (item.rules && item.rules.length > 0) {
        item.rules = requestRules.concat(item.rules);
      } else {
        item.rules = requestRules;
      }
      console.log(item.rules);
    },
    delConfirm(name) {
      // console.log(this.formTable.table_data);
      this.formTable.table_data = this.formTable.table_data.filter(
        item => item.name != name
      );
      console.log(data);
      // this.$emit("showSelect", val);
    },
    editInput(record) {
      console.log(record);
      this.oldData = JSON.parse(JSON.stringify(record.name));

      // return false;
      const newData = [...this.formTable.table_data];
      console.log(newData);
      const target = newData.filter(item => record.name === item.name)[0];
      console.log("target", target);
      if (target) {
        target.editable = !target.editable;
        this.formTable.table_data = newData;
      }
      console.log("formTable", this.formTable.table_data);
    }
  },
  watch: {
    formItem: {
      handler(newValue) {
        this.initFormData();
      },
      immediate: true,
      deep: true
    }
  }
};
</script>

<style lang="scss" scoped></style>
