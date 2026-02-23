const { test, expect } = require("@playwright/test");

async function mockAuth(page) {
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
}

test("problem manage delete success and noop both refresh list", async ({ page }) => {
  await mockAuth(page);

  let phase = 0;
  await page.route("**/api/ProbController/getProblems**", async route => {
    const payload = phase === 0 ? [{
      instId: "prob-1",
      instName: "zdt1-caseA",
      categoryName: "类别1",
      dirName: "/tmp",
      machineName: "localhost",
      machineIp: "127.0.0.1",
      description: "demo"
    }] : [];
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(payload)
    });
  });
  await page.route("**/api/ProbController/countAllProbInsts**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(phase === 0 ? 1 : 0)
    });
  });
  await page.route("**/api/ProbController/deleteProblemById**", async route => {
    if (phase === 0) {
      phase = 1;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          msg: "删除成功",
          data: {
            proId: "prob-1",
            deletedCount: 1,
            repaired: false,
            noop: false
          }
        })
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        msg: "删除成功",
        data: {
          proId: "prob-1",
          deletedCount: 0,
          repaired: false,
          noop: true
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByRole("menubar").getByRole("link", { name: "问题实例管理" }).click();

  const tableBody = page.locator(".el-table__body");
  await tableBody.getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "删除成功（proId=prob-1）" })).toBeVisible();
  await expect(tableBody.getByText("zdt1-caseA")).toHaveCount(0);
});

test("problem manage delete blocked when referenced by plans", async ({ page }) => {
  await mockAuth(page);

  await page.route("**/api/ProbController/getProblems**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([{
        instId: "prob-in-use",
        instName: "zdt1-caseB",
        categoryName: "类别1",
        dirName: "/tmp",
        machineName: "localhost",
        machineIp: "127.0.0.1",
        description: "demo"
      }])
    });
  });
  await page.route("**/api/ProbController/countAllProbInsts**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(1)
    });
  });
  await page.route("**/api/ProbController/deleteProblemById**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 409,
        errorCode: "PROB_IN_USE",
        msg: "删除失败，问题实例已被执行计划引用，请先解除关联",
        data: {
          proId: "prob-in-use",
          blocked: true,
          refPlanCount: 2,
          refPlanNames: ["计划A", "计划B"]
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByRole("menubar").getByRole("link", { name: "问题实例管理" }).click();

  await page.locator(".el-table__body").getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "问题实例已被执行计划引用" })).toBeVisible();
});
