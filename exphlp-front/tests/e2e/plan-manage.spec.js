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
