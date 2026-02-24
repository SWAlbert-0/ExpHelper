const { test, expect } = require("@playwright/test");

test("login flow redirects to main page", async ({ page }) => {
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

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();

  // Router uses hash mode and may land on /#/index first, then redirect.
  await expect(page).toHaveURL(/#\/(index|problemModel\/index)(\?.*)?$/);
  await expect(
    page.getByRole("menubar").getByRole("link", { name: "问题实例管理" })
  ).toBeVisible();
});

test("broken avatar url falls back to default avatar", async ({ page }) => {
  await page.route("**/api/auth/login", async route => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        code: 200,
        data: { token: "pw-mock-token" }
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
            avatar: "/api/auth/avatar/missing-avatar.png",
            unread_msg_count: 0
          },
          roles: ["admin"],
          permissions: ["*:*:*"]
        }
      })
    });
  });

  await page.route("**/api/auth/avatar/missing-avatar.png", async route => {
    await route.fulfill({ status: 404 });
  });

  await page.goto("/login");
  await page.getByPlaceholder("账号").fill("admin");
  await page.getByPlaceholder("密码").fill("123456");
  await page.getByRole("button", { name: /登\s*录/ }).click();

  const navAvatar = page.locator(".avatar-wrapper .user-avatar");
  await expect(navAvatar).toHaveAttribute("src", /default-avatar\..*\.svg|default-avatar\.svg/);
});
