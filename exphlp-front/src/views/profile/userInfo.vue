<template>
  <el-form ref="form" :model="user" :rules="rules" label-width="80px">
    <el-form-item label="用户昵称" prop="username">
      <el-input v-model="user.username" />
    </el-form-item>
    <el-form-item label="微信号" prop="wechat">
      <el-input v-model="user.wechat" maxlength="64" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="user.email" maxlength="50" />
    </el-form-item>
    <el-form-item label="手机号" prop="mobile">
      <el-input v-model="user.mobile" maxlength="11" />
    </el-form-item>
    <el-form-item label="QQ号" prop="qq">
      <el-input v-model="user.qq" maxlength="12" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="mini" @click="submit">保存</el-button>
      <el-button type="danger" size="mini" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { updateProfile } from "@/api/auth";

export default {
  props: {
    user: {
      type: Object
    }
  },
  data() {
    return {
      // 表单校验
      rules: {
        username: [
          { required: true, message: "用户昵称不能为空", trigger: "blur" }
        ],
        email: [
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        mobile: [
          {
            pattern: /^1\d{10}$/,
            message: "请输入11位手机号",
            trigger: "blur"
          }
        ],
        qq: [
          {
            pattern: /^[1-9]\d{4,11}$/,
            message: "请输入正确QQ号",
            trigger: "blur"
          }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          updateProfile(this.user).then(response => {
            this.msgSuccess("修改成功");
            this.$emit("profile-updated");
          });
        }
      });
    },
    close() {
      this.$store.dispatch("tagsView/delView", this.$route);
      this.$router.push({ path: "/index" });
    }
  }
};
</script>
