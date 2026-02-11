import { test, expect } from "@playwright/test";

test.describe("Home", () => {
  test("landing page loads", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Ajeitai|ajeitai/i);
  });

  test("can navigate to cliente welcome", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("link", { name: /cliente|entrar|começar/i }).first().click();
    await expect(page).toHaveURL(/\/(cliente|welcome)/);
  });
});
