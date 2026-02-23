const { test, expect } = require("@playwright/test");

test("plan manage add flow shows required problem validation", async ({ page }) => {
  await page.route("**/api/**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: null
      })
    });
  });

  await page.route("**/api/auth/login", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          token: "pw-mock-token"
        }
      })
    });
  });

  await page.route("**/api/auth/me", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          user: {
            userId: "pw-user",
            username: "admin",
            name: "管理员",
            avatar: "",
            unread_msg_count: 0
          },
          roles: ["admin"],
          permissions: ["*:*:*"]
        }
      })
    });
  });

  await page.route("**/api/ProbController/getProblems**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([])
    });
  });

  await page.route("**/api/AlgController/getAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([])
    });
  });

  await page.route("**/api/PlatController/getUsersByPage**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([])
    });
  });

  await page.route("**/api/ExePlanController/getExePlans**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([])
    });
  });

  await page.route("**/api/ExePlanController/countAllExePlans**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(0)
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page).toHaveURL(/#\/(index|problemModel\/index)(\?.*)?$/);

  await page.getByRole("menubar").getByRole("link", { name: "执行计划管理" }).click();
  await expect(page).toHaveURL(/#\/exePlan\/index(\?.*)?$/);

  await page.getByRole("button", { name: "添加" }).click();
  await page.getByRole("button", { name: "确 定" }).first().click();
  await expect(page.locator(".el-message__content").filter({ hasText: "请选择问题实例" })).toBeVisible();
});

test("plan manage shows lastError and allows re-execute for abnormal plan", async ({ page }) => {
  let planListFetchTimes = 0;
  await page.route("**/api/**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: null
      })
    });
  });

  await page.route("**/api/auth/login", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          token: "pw-mock-token"
        }
      })
    });
  });

  await page.route("**/api/auth/me", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          user: {
            userId: "pw-user",
            username: "admin",
            name: "管理员",
            avatar: "",
            unread_msg_count: 0
          },
          roles: ["admin"],
          permissions: ["*:*:*"]
        }
      })
    });
  });

  await page.route("**/api/ProbController/getProblems**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/AlgController/getAlgs**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/PlatController/getUsersByPage**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/ExePlanController/countAllExePlans**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(1) });
  });
  await page.route("**/api/ExePlanController/getExePlans**", async route => {
    planListFetchTimes += 1;
    const exeState = planListFetchTimes >= 2 ? 2 : 3;
    const lastError = planListFetchTimes >= 2 ? null : "算法服务未注册: nsga2-zdt1-ls";
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([{
        planId: "plan-abnormal-1",
        planName: "plan-zdt1",
        probInstIds: [],
        algRunInfos: [],
        userIds: [],
        exeStartTime: 1700000000000,
        exeEndTime: 1700003600000,
        exeState,
        description: "demo",
        lastError
      }])
    });
  });
  await page.route("**/api/ExePlanController/execute**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        msg: "ok",
        data: {
          planId: "plan-abnormal-1",
          accepted: true,
          state: 2,
          lastError: null
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page).toHaveURL(/#\/(index|problemModel\/index)(\?.*)?$/);

  await page.getByRole("menubar").getByRole("link", { name: "执行计划管理" }).click();
  await expect(page).toHaveURL(/#\/exePlan\/index(\?.*)?$/);

  await expect(page.getByText("算法服务未注册: nsga2-zdt1-ls")).toBeVisible();
  await page.getByRole("button", { name: "重新执行" }).click();
  await page.getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "计划重新执行中" })).toBeVisible();
});

test("plan manage view supports execution log polling dialog", async ({ page }) => {
  await page.route("**/api/**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: null
      })
    });
  });
  await page.route("**/api/auth/login", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ code: 200, data: { token: "pw-mock-token" } })
    });
  });
  await page.route("**/api/auth/me", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          user: { userId: "pw-user", username: "admin", name: "管理员", avatar: "", unread_msg_count: 0 },
          roles: ["admin"],
          permissions: ["*:*:*"]
        }
      })
    });
  });
  await page.route("**/api/ProbController/getProblems**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/AlgController/getAlgs**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/PlatController/getUsersByPage**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify([]) });
  });
  await page.route("**/api/ExePlanController/countAllExePlans**", async route => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(1) });
  });
  await page.route("**/api/ExePlanController/getExePlans**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([{
        planId: "plan-log-1",
        planName: "plan-log",
        probInstIds: [],
        algRunInfos: [],
        userIds: [],
        exeStartTime: 1700000000000,
        exeEndTime: 0,
        exeState: 2,
        description: "log-demo",
        lastError: null
      }])
    });
  });
  await page.route("**/api/ExePlanController/getPlanLogs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: {
          items: [{
            planId: "plan-log-1",
            seq: 1,
            ts: 1700000000000,
            level: "INFO",
            stage: "PLAN_START",
            message: "计划开始执行"
          }],
          nextSeq: 1,
          planState: 2,
          lastError: null
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByRole("menubar").getByRole("link", { name: "执行计划管理" }).click();

  await page.getByRole("button", { name: "查看" }).click();
  await page.getByRole("button", { name: "执行日志" }).click();
  await expect(page.getByRole("dialog", { name: "执行日志" })).toBeVisible();
  await expect(page.getByText("计划开始执行")).toBeVisible();
});
