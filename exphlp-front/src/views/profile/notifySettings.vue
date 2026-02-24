<template>
  <el-form ref="form" :model="form" label-width="130px">
    <el-form-item label="通知邮箱" prop="email">
      <el-input v-model.trim="form.email" placeholder="用于接收计划执行结束通知" />
    </el-form-item>
    <el-form-item label="启用邮件通知">
      <el-switch v-model="form.emailEnabled" />
    </el-form-item>
    <el-form-item label="计划结束通知">
      <el-switch v-model="form.eventPlanDoneEnabled" />
    </el-form-item>
    <el-form-item label="启用静默时段">
      <el-switch v-model="form.quietHoursEnabled" />
    </el-form-item>
    <el-form-item label="静默开始" v-if="form.quietHoursEnabled">
      <el-time-picker
        v-model="quietStart"
        format="HH:mm"
        value-format="HH:mm"
        placeholder="选择开始时间"
        style="width: 220px;"
      />
    </el-form-item>
    <el-form-item label="静默结束" v-if="form.quietHoursEnabled">
      <el-time-picker
        v-model="quietEnd"
        format="HH:mm"
        value-format="HH:mm"
        placeholder="选择结束时间"
        style="width: 220px;"
      />
    </el-form-item>
    <el-form-item label="时区">
      <el-input v-model.trim="form.timezone" placeholder="默认 Asia/Shanghai" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存通知设置</el-button>
      <el-button @click="load">刷新</el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { getNotifyProfile, updateNotifyProfile } from "@/api/exphlp/notification";

export default {
  name: "NotifySettings",
  data() {
    return {
      saving: false,
      form: {
        email: "",
        emailEnabled: true,
        eventPlanDoneEnabled: true,
        quietHoursEnabled: false,
        quietHoursStart: "23:00",
        quietHoursEnd: "08:00",
        timezone: "Asia/Shanghai",
      },
      quietStart: "23:00",
      quietEnd: "08:00",
    };
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      getNotifyProfile().then((res) => {
        const data = (res && res.data) || {};
        this.form = {
          email: data.email || "",
          emailEnabled: data.emailEnabled !== false,
          eventPlanDoneEnabled: data.eventPlanDoneEnabled !== false,
          quietHoursEnabled: data.quietHoursEnabled === true,
          quietHoursStart: data.quietHoursStart || "23:00",
          quietHoursEnd: data.quietHoursEnd || "08:00",
          timezone: data.timezone || "Asia/Shanghai",
        };
        this.quietStart = this.form.quietHoursStart;
        this.quietEnd = this.form.quietHoursEnd;
      });
    },
    save() {
      this.saving = true;
      const payload = {
        ...this.form,
        quietHoursStart: this.quietStart || "23:00",
        quietHoursEnd: this.quietEnd || "08:00",
      };
      updateNotifyProfile(payload).then(() => {
        this.$message({ type: "success", message: "通知设置已保存" });
      }).finally(() => {
        this.saving = false;
      });
    },
  },
};
</script>

