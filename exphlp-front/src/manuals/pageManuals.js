const PAGE_MANUALS = {
  problem: {
    title: "问题实例管理操作手册",
    summary: "用于维护可执行的问题实例，是执行计划的基础输入。",
    sections: [
      {
        heading: "日常流程",
        steps: [
          "点击“添加”创建新问题实例，必填实例名称、类别、目录、机器信息与描述。",
          "通过“JSON导入”批量入库，支持拖拽文件或粘贴 JSON 文本。",
          "用顶部“查询/刷新”确认实例状态，再进入执行计划页面选择实例。"
        ],
        tips: [
          "建议按“类别名称 + 实例名称”统一命名，后续检索更稳定。",
          "批量导入前先用 1 条样例验证字段是否符合要求。",
          "machineName/machineIp/dirName 用于记录实验环境上下文与追溯，不作为平台调度路由依据。"
        ],
        warnings: [
          "若实例已被计划引用，删除会被后端拦截并提示引用计划。"
        ]
      },
      {
        heading: "导入与删除说明",
        steps: [
          "JSON 导入会自动做旧字段兼容映射并返回标准化结果统计。",
          "单条或批量删除后，列表会自动刷新以保证前后端一致。"
        ],
        tips: [
          "出现“记录不存在”提示时，表示后端已进行幂等处理并同步列表。"
        ]
      }
    ]
  },
  algorithm: {
    title: "算法库管理操作手册",
    summary: "用于维护算法元数据、参数定义与源码构建任务。推荐按“录入算法 -> 上传源码 -> 构建检查”顺序执行。",
    sections: [
      {
        heading: "第一步：算法入库",
        steps: [
          "点击“添加”维护算法名称、服务名、运行时与描述。",
          "在“参数列表”中补全参数类型与默认值，提交后可被计划直接复用。",
          "使用“JSON导入”批量创建算法记录。"
        ],
        tips: [
          "服务名必须与 Nacos 注册名一致，否则执行前检查会失败。",
          "参数类型建议与算法服务接口严格对齐（int/long/double/String/boolean/Object）。",
          "Python 案例可直接使用 docs/cases/moo-python-nsga2-zdt1 与 docs/cases/moo-python-moead-lsmop。"
        ],
        warnings: [
          "已被执行计划引用的算法无法直接删除。"
        ]
      },
      {
        heading: "第二步：源码上传与构建",
        steps: [
          "点击“源码”上传 zip 包（需包含 exphlp-alg.json）。",
          "上传后点击“构建并启动”，再用“刷新状态”查看构建日志。",
          "构建成功后到执行计划执行“执行前检查”确认服务可用。"
        ],
        tips: [
          "Python 源码至少包含 main.py + requirements.txt + exphlp-alg.json。",
          "若报 runtimeType/serviceName 不一致，请同时核对算法库字段和 exphlp-alg.json。",
          "构建失败优先检查 Docker、网络与依赖源可用性。"
        ]
      }
    ]
  },
  plan: {
    title: "执行计划管理操作手册",
    summary: "用于组合问题实例与算法，完成执行、复执行、日志与结果查看。",
    sections: [
      {
        heading: "创建与执行（推荐走执行向导）",
        steps: [
          "点击“添加”填写计划基础信息并选择问题实例。",
          "在右侧步骤中将算法“加入计划”，配置运行次数与参数后保存。",
          "返回列表点击“执行”，或先用“执行向导/执行前检查”验证运行环境。"
        ],
        tips: [
          "若出现重复参数告警，先删除多余算法行再重新加入。",
          "执行前检查通过后再执行，可显著降低运行失败率。"
        ],
        warnings: [
          "算法服务未在 Nacos 有可用实例时，计划会在检查阶段直接失败。"
        ]
      },
      {
        heading: "日志与结果解读",
        steps: [
          "在“查看”中打开执行日志，关注 ERROR/WARN 阶段与详情。",
          "执行结果可查看 HV、IGD+、GD、Coverage、Spread、Spacing 等指标。",
          "异常结束后可先修复配置，再使用“重新执行”。"
        ],
        tips: [
          "可导出 CSV/JSON 便于留档与复盘。"
        ]
      }
    ]
  },
  platform: {
    title: "平台管理操作手册",
    summary: "用于维护通知接收人信息，支撑执行结果邮件通知。",
    sections: [
      {
        heading: "用户维护",
        steps: [
          "点击“添加”维护姓名、邮箱、微信、手机号、QQ。",
          "通过“查询/刷新”确认人员信息后，在计划中勾选通知人员。",
          "需要时可使用“重置密码”恢复账号。"
        ],
        tips: [
          "邮箱是通知链路核心字段，建议优先保证格式正确且可达。"
        ],
        warnings: [
          "删除用户会影响后续通知接收，请先确认该用户是否仍被计划使用。"
        ]
      },
      {
        heading: "通知联动说明",
        steps: [
          "计划执行完成后系统按所选人员投递通知。",
          "可在执行日志弹窗的“通知记录”中查看状态与失败原因。"
        ],
        tips: [
          "若出现 MAIL_CONFIG_INVALID 等错误，请先补齐 SMTP/Tencent SES 配置。"
        ]
      }
    ]
  }
};

export function getPageManual(pageKey) {
  if (PAGE_MANUALS[pageKey]) {
    return PAGE_MANUALS[pageKey];
  }
  return {
    title: "操作手册",
    summary: "未找到对应页面说明，请联系管理员补充。",
    sections: []
  };
}
