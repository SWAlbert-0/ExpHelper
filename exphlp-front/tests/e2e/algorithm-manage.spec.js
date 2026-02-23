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

test("algorithm manage refresh label shown and delete success works", async ({ page }) => {
  await mockAuth(page);

  let deleted = false;
  await page.route("**/api/AlgController/getAlgs**", async route => {
    const payload = deleted ? [] : [{
      algId: "alg-1",
      algName: "nsga2-zdt1-ls",
      serviceName: "nsga2-zdt1-ls",
      description: "demo",
      defParas: []
    }];
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(payload)
    });
  });
  await page.route("**/api/AlgController/countAllAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(deleted ? 0 : 1)
    });
  });
  await page.route("**/api/AlgController/deleteAlgById**", async route => {
    deleted = true;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        msg: "删除成功",
        data: { deletedCount: 1, repaired: false }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await expect(page).toHaveURL(/#\/(index|problemModel\/index)(\?.*)?$/);

  await page.getByRole("menubar").getByRole("link", { name: "算法库管理" }).click();
  await expect(page).toHaveURL(/#\/algorithmConfig\/index(\?.*)?$/);
  await expect(page.getByRole("button", { name: "刷新" })).toBeVisible();
  const tableBody = page.locator(".el-table__body");
  await expect(tableBody.getByText("nsga2-zdt1-ls").first()).toBeVisible();

  await tableBody.getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "删除成功" })).toBeVisible();
  await expect(tableBody.getByText("nsga2-zdt1-ls")).toHaveCount(0);
});

test("algorithm manage delete treats missing record as noop success", async ({ page }) => {
  await mockAuth(page);

  let deleted = false;
  await page.route("**/api/AlgController/getAlgs**", async route => {
    const payload = deleted ? [] : [{
      algId: "alg-missing",
      algName: "broken-alg",
      serviceName: "broken-alg",
      description: "demo",
      defParas: []
    }];
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(payload)
    });
  });
  await page.route("**/api/AlgController/countAllAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(deleted ? 0 : 1)
    });
  });
  await page.route("**/api/AlgController/deleteAlgById**", async route => {
    deleted = true;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        msg: "删除成功",
        data: {
          algId: "alg-missing",
          deletedCount: 0,
          existed: false,
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
  await page.getByRole("menubar").getByRole("link", { name: "算法库管理" }).click();

  await page.locator(".el-table__body").getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "记录已不存在" })).toBeVisible();
  await expect(page.locator(".el-table__body").getByText("broken-alg")).toHaveCount(0);
});

test("algorithm manage shows repaired success hint when backend repaired legacy data", async ({ page }) => {
  await mockAuth(page);

  let deleted = false;
  await page.route("**/api/AlgController/getAlgs**", async route => {
    const payload = deleted ? [] : [{
      algId: "legacy-id-1",
      algName: "legacy-alg",
      serviceName: "legacy-svc",
      description: "legacy",
      defParas: []
    }];
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(payload)
    });
  });
  await page.route("**/api/AlgController/countAllAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(deleted ? 0 : 1)
    });
  });
  await page.route("**/api/AlgController/deleteAlgById**", async route => {
    deleted = true;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        msg: "删除成功",
        data: {
          algId: "legacy-id-1",
          deletedCount: 1,
          existed: true,
          repaired: true
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByRole("menubar").getByRole("link", { name: "算法库管理" }).click();

  await page.locator(".el-table__body").getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "已自动修复历史数据" })).toBeVisible();
});

test("algorithm manage delete blocked when algorithm is referenced by plans", async ({ page }) => {
  await mockAuth(page);

  await page.route("**/api/AlgController/getAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([{
        algId: "alg-in-use",
        algName: "in-use-alg",
        serviceName: "in-use-svc",
        description: "demo",
        defParas: []
      }])
    });
  });
  await page.route("**/api/AlgController/countAllAlgs**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(1)
    });
  });
  await page.route("**/api/AlgController/deleteAlgById**", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 409,
        errorCode: "ALG_IN_USE",
        msg: "删除失败，算法已被执行计划引用，请先解除关联",
        data: {
          algId: "alg-in-use",
          blocked: true,
          refPlanCount: 1,
          refPlanNames: ["计划A"]
        }
      })
    });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();
  await page.getByRole("menubar").getByRole("link", { name: "算法库管理" }).click();

  await page.locator(".el-table__body").getByRole("button", { name: "删除" }).first().click();
  await page.locator(".el-message-box__btns").getByRole("button", { name: "确定" }).click();
  await expect(page.locator(".el-message__content").filter({ hasText: "算法已被执行计划引用" })).toBeVisible();
});
