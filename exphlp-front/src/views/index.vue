<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>后台管理框架</h2>
        <p>
         您可以基于此系统进行持续开发，以做出符合自己系统。所有前端后台代码封装过后十分精简易上手，
          出错概率低。
        </p>
        <p>
          <b>当前版本:</b> <span>v{{ version }}</span>
        </p>
        <p>
          <el-tag type="danger">免费开源</el-tag>
        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>Python</li>
              <li>Django</li>
              <li>django-redis</li>
              <li>django-celery-beat</li>
              <li>django-rest-framework</li>
              <li>django-rest-framework-jwt</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>更新日志</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="">
              <template slot="title">
                v1.1.2 - 2021-07-11&nbsp;&nbsp;&nbsp;
                <el-badge value="new" class="item" style="padding-top: 10px;padding-left: 10px;" />
              </template>
              <ol>
                <li>新功能: 所有包引入都加apps</li>
                <li>新功能: 支持多级目录建app，python manage.py createapp 一级文件名/app01 ... </li>
                <li>新功能: 新增弹出框设置参数，在点击弹窗外不关闭弹窗功能</li>
                <li>新功能: 接入drf-yasg，进行swagger形式的api展示</li>
                <li>新功能: 配置文件中可取消redis</li>
                <li>修复BUG: Windows文件上传问题修复</li>
                <li>修复BUG: CURD表单点击重置分页器失灵修复</li>
                <li>修复BUG: 修复系统接口菜单无法显示问题</li>
                <li>修复BUG: 部门动态加载,根据角色分配</li>
                <li>修复BUG: init初始数据中的普通用户角色，默认添加首页权限</li>
                <li>修复BUG: 取消在线用户功能</li>
                <li>修复BUG: 修复定时任务只显示10条问题</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="">
              <template slot="title">
                v1.1.1 - 2021-05-17&nbsp;&nbsp;&nbsp;
                <el-badge class="item" style="padding-top: 10px;padding-left: 10px;" />
              </template>
              <ol>
                <li>新功能(前端框架): 前端封装快速搭建CRUD</li>
                <li>新功能(一键创建app命令): 一键创建app，并注册到settings和urls中</li>
                <li>修复BUG(用户信息): 修复无法管理员更新用户信息BUG</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="">
              <template slot="title">
                v1.1.0 - 2021-05-05&nbsp;&nbsp;&nbsp;
                <el-badge class="item" style="padding-top: 10px;padding-left: 10px;" />
              </template>
              <ol>
                <li>新增服务监控功能</li>
                <li>新增操作日志功能</li>
                <li>新增导入功能</li>
                <li>新增celery定时任务</li>
                <li>新增消息通知功能</li>
                <li>新增后端接口文档</li>
                <li>新增docker-compose部署</li>
                <li>新增支持重写用户模型</li>
                <li>数据权限完善</li>
                <li>登录日志优化</li>
                <li>后端代码架构优化</li>
                <li>文件管理功能完善</li>
                <li>修复创建用户密码问题</li>
                <li>其他细节优化</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.0.0 - 2021-03-01">
              <ol>
                <li>dvAdmin前后端分离系统正式发布</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "1.1.1"
    };
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    }
  }
};
</script>

<style scoped lang="scss">
  .home {
    blockquote {
      padding: 10px 20px;
      margin: 0 0 20px;
      font-size: 17.5px;
      border-left: 5px solid #eee;
    }

    hr {
      margin-top: 20px;
      margin-bottom: 20px;
      border: 0;
      border-top: 1px solid #eee;
    }

    .col-item {
      margin-bottom: 20px;
    }

    ul {
      padding: 0;
      margin: 0;
    }

    font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 13px;
    color: #676a6c;
    overflow-x: hidden;

    ul {
      list-style-type: none;
    }

    h4 {
      margin-top: 0px;
    }

    h2 {
      margin-top: 10px;
      font-size: 26px;
      font-weight: 100;
    }

    p {
      margin-top: 10px;

      b {
        font-weight: 700;
      }
    }

    .update-log {
      ol {
        display: block;
        list-style-type: decimal;
        margin-block-start: 1em;
        margin-block-end: 1em;
        margin-inline-start: 0;
        margin-inline-end: 0;
        padding-inline-start: 40px;
      }
    }
  }
</style>

