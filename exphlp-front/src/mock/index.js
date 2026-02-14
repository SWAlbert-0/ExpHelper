export const userInfo = {
  code: 200,
  data: {
    permissions: ["*:*:*"],
    roles: ["admin"],
    user: {
      id: 1,
      create_datetime: "2021-02-27 06:20:09",
      update_datetime: "2021-08-04 14:55:42",
      creator_name: "admin",
      admin: true,
      deptId: 1,
      unread_msg_count: 0,
      last_login: "2021-08-04T14:49:22.799198",
      first_name: "",
      last_name: "",
      is_staff: true,
      is_active: true,
      description: null,
      modifier: null,
      dept_belong_id: "1",
      username: "admin",
      email: "admin@qq.com",
      mobile: null,
      avatar: null,
      name: "管理员",
      gender: "2",
      remark: "1",
      user_type: 2,
      dept: {
        id: 1,
        description: "",
        modifier: "admin",
        dept_belong_id: "1",
        update_datetime: "2021-02-27T07:26:20.518695",
        create_datetime: "2021-02-27T15:18:39",
        deptName: "XX创新科技",
        orderNum: 1,
        owner: null,
        phone: "15888888888",
        email: "cxkj@qq.com",
        status: "1",
        creator: 1,
        parentId: null
      },
      post: [
        {
          id: 1,
          description: "",
          modifier: "admin",
          dept_belong_id: "1",
          update_datetime: "2021-02-27T07:16:10.725970",
          create_datetime: "2021-02-27T07:16:10.726016",
          postName: "董事长",
          postCode: "ceo",
          postSort: 1,
          status: "1",
          remark: null,
          creator: 1
        }
      ],
      role: [
        {
          id: 1,
          description: "",
          modifier: "admin",
          dept_belong_id: "1",
          update_datetime: "2021-07-13T19:50:16.468462",
          create_datetime: "2021-02-27T08:48:08.064911",
          roleName: "超级管理员",
          roleKey: "admin",
          roleSort: 1,
          status: "1",
          admin: true,
          dataScope: "2",
          remark: null,
          creator: 1,
          dept: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
          menu: [
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31,
            32,
            33,
            34,
            35,
            36,
            37,
            38,
            39,
            40,
            41,
            42,
            43,
            44,
            45,
            46,
            47,
            48,
            49,
            50,
            51,
            52,
            53,
            54,
            55,
            56,
            57,
            58,
            59,
            60,
            61,
            62,
            63,
            64,
            65,
            66,
            70,
            71,
            72,
            73,
            74,
            75,
            76,
            77,
            78,
            79,
            80,
            81,
            82,
            83,
            85,
            86,
            87,
            88,
            90,
            91,
            97
          ]
        }
      ]
    }
  },
  msg: "success",
  status: "success"
};

export const routers = {
  code: 200,
  data: [
    // {
    //   id: 1,
    //   name: "System",
    //   path: "/system",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/system",
    //   component: "Layout",
    //   meta: {
    //     title: "系统管理",
    //     icon: "system",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 2,
    //   name: "Permission",
    //   path: "/permission",
    //   orderNum: 2,
    //   hidden: false,
    //   redirect: "/permission",
    //   component: "Layout",
    //   meta: {
    //     title: "权限管理",
    //     icon: "peoples",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 3,
    //   name: "Dict",
    //   path: "dict",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "dict",
    //   component: "vadmin/system/dict/index",
    //   meta: {
    //     title: "字典管理",
    //     icon: "dict",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 4,
    //   name: "Config",
    //   path: "config",
    //   orderNum: 2,
    //   hidden: false,
    //   redirect: "config",
    //   component: "vadmin/system/config/index",
    //   meta: {
    //     title: "参数管理",
    //     icon: "edit",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 5,
    //   name: "Post",
    //   path: "post",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "post",
    //   component: "vadmin/permission/post/index",
    //   meta: {
    //     title: "岗位管理",
    //     icon: "post",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 6,
    //   name: "Dept",
    //   path: "dept",
    //   orderNum: 2,
    //   hidden: false,
    //   redirect: "dept",
    //   component: "vadmin/permission/dept/index",
    //   meta: {
    //     title: "部门管理",
    //     icon: "tree",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 7,
    //   name: "Menu",
    //   path: "menu",
    //   orderNum: 3,
    //   hidden: false,
    //   redirect: "menu",
    //   component: "vadmin/permission/menu/index",
    //   meta: {
    //     title: "菜单管理",
    //     icon: "tree-table",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 8,
    //   name: "Role",
    //   path: "role",
    //   orderNum: 4,
    //   hidden: false,
    //   redirect: "role",
    //   component: "vadmin/permission/role/index",
    //   meta: {
    //     title: "角色管理",
    //     icon: "peoples",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 9,
    //   name: "User",
    //   path: "user",
    //   orderNum: 5,
    //   hidden: false,
    //   redirect: "user",
    //   component: "vadmin/permission/user/index",
    //   meta: {
    //     title: "用户管理",
    //     icon: "user",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 11,
    //   name: "Savefile",
    //   path: "savefile",
    //   orderNum: 3,
    //   hidden: false,
    //   redirect: "savefile",
    //   component: "vadmin/system/savefile/index",
    //   meta: {
    //     title: "文件管理",
    //     icon: "job",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 47,
    //   name: "Message",
    //   path: "message",
    //   orderNum: 4,
    //   hidden: false,
    //   redirect: "message",
    //   component: "vadmin/system/message/index",
    //   meta: {
    //     title: "通知公告",
    //     icon: "message",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 61,
    //   name: "Log",
    //   path: "log",
    //   orderNum: 5,
    //   hidden: false,
    //   redirect: "log",
    //   component: "ParentView",
    //   meta: {
    //     title: "日志管理",
    //     icon: "log",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 62,
    //   name: "Logininfor",
    //   path: "logininfor",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "logininfor",
    //   component: "vadmin/monitor/logininfor/index",
    //   meta: {
    //     title: "登录日志",
    //     icon: "logininfor",
    //     noCache: false
    //   },
    //   parentId: 61
    // },
    // {
    //   id: 63,
    //   name: "Operlog",
    //   path: "operlog",
    //   orderNum: 2,
    //   hidden: false,
    //   redirect: "operlog",
    //   component: "vadmin/monitor/operlog/index",
    //   meta: {
    //     title: "操作日志",
    //     icon: "log",
    //     noCache: false
    //   },
    //   parentId: 61
    // },
    // {
    //   id: 66,
    //   name: "Monitor",
    //   path: "/monitor",
    //   orderNum: 3,
    //   hidden: false,
    //   redirect: "/monitor",
    //   component: "Layout",
    //   meta: {
    //     title: "系统监控",
    //     icon: "monitor",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 70,
    //   name: "Celery",
    //   path: "celery",
    //   orderNum: 2,
    //   hidden: false,
    //   redirect: "celery",
    //   component: "vadmin/monitor/celery/index",
    //   meta: {
    //     title: "定时任务",
    //     icon: "job",
    //     noCache: false
    //   },
    //   parentId: 66
    // },
    // {
    //   id: 79,
    //   name: "Celerylog",
    //   path: "celerylog",
    //   orderNum: 3,
    //   hidden: false,
    //   redirect: "celerylog",
    //   component: "vadmin/monitor/celery/celerylog/index",
    //   meta: {
    //     title: "定时日志",
    //     icon: "job",
    //     noCache: false
    //   },
    //   parentId: 61
    // },
    // {
    //   id: 82,
    //   name: "Tool",
    //   path: "/tool",
    //   orderNum: 4,
    //   hidden: false,
    //   redirect: "/tool",
    //   component: "Layout",
    //   meta: {
    //     title: "系统工具",
    //     icon: "tool",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 83,
    //   name: "Build",
    //   path: "build",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "build",
    //   component: "vadmin/tool/build/index",
    //   meta: {
    //     title: "表单构建",
    //     icon: "build",
    //     noCache: false
    //   },
    //   parentId: 82
    // },
    // {
    //   id: 92,
    //   name: "Server",
    //   path: "/monitor/server",
    //   orderNum: 3,
    //   hidden: false,
    //   redirect: "/monitor/server",
    //   component: "vadmin/monitor/server/index",
    //   meta: {
    //     title: "服务监控",
    //     icon: "server",
    //     noCache: false
    //   },
    //   parentId: 66
    // },
    // {
    //   id: 97,
    //   name: "Index",
    //   path: "/index",
    //   orderNum: 0,
    //   hidden: false,
    //   redirect: "/index",
    //   component: "Layout/index",
    //   meta: {
    //     title: "问题建模",
    //     icon: "dashboard",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 1,
    //   name: "problemModel",
    //   path: "/problemModel",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/problemModel",
    //   component: "Layout",
    //   meta: {
    //     title: "问题实例管理",
    //     icon: "tool",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 11,
    //   name: "problemModel-index",
    //   path: "/index",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/index",
    //   component: "problemModel/index",
    //   meta: {
    //     title: "问题实例管理",
    //     noCache: false
    //   },
    //   parentId: 1
    // },
    // {
    //   id: 2,
    //   name: "algorithm",
    //   path: "/algorithm",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/algorithm",
    //   component: "Layout",
    //   meta: {
    //     title: "算法管理",
    //     icon: "job",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 21,
    //   name: "algorithmConfig",
    //   path: "/algorithmConfig",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/algorithmConfig",
    //   component: "algorithm/algorithmConfig",
    //   meta: {
    //     title: "算法配置",
    //     noCache: false
    //   },
    //   parentId: 2
    // },
    // {
    //   id: 3,
    //   name: "planManage",
    //   path: "/planManage",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/planManage",
    //   component: "Layout",
    //   meta: {
    //     title: "执行计划管理",
    //     icon: "build",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 31,
    //   name: "planQuery",
    //   path: "/planQuery",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/planQuery",
    //   component: "planManage/planQuery",
    //   meta: {
    //     title: "计划查询",
    //     noCache: false
    //   },
    //   parentId: 3
    // },
    // {
    //   id: 32,
    //   name: "addPlan",
    //   path: "/addPlan",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/addPlan",
    //   component: "planManage/addPlan",
    //   meta: {
    //     title: "计划制定",
    //     noCache: false
    //   },
    //   parentId: 3
    // },
    // {
    //   id: 33,
    //   name: "planDetail",
    //   path: "/planDetail",
    //   orderNum: 1,
    //   hidden: true,
    //   redirect: "/planDetail",
    //   component: "planManage/planDetail",
    //   meta: {
    //     title: "计划详情",
    //     noCache: false
    //   },
    //   parentId: 3
    // },
    // {
    //   id: 4,
    //   name: "platform",
    //   path: "/platform",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/platform",
    //   component: "Layout",
    //   meta: {
    //     title: "平台管理",
    //     icon: "system",
    //     noCache: false
    //   },
    //   parentId: null
    // },
    // {
    //   id: 41,
    //   name: "platformManage",
    //   path: "/platformManage",
    //   orderNum: 1,
    //   hidden: false,
    //   redirect: "/platformManage",
    //   component: "platform/platformManage",
    //   meta: {
    //     title: "用户管理",
    //     noCache: false
    //   },
    //   parentId: 4
    // }
  ],
  msg: "success",
  status: "success"
};
