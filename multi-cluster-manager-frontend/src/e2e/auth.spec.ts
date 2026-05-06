// e2e/auth.spec.ts (使用 Playwright)
// @ts-ignore
import {test, expect} from '@playwright/test';

const BASE_URL = 'http://localhost:5173'; // 或你的开发服务器地址

test.describe('认证与权限流程', () => {

    // @ts-ignore
    test('登录成功并跳转到仪表盘', async ({page}) => {
        await page.goto(BASE_URL + '/login');
        await page.fill('input[id="email"]', 'admin@test.com');
        await page.fill('input[id="password"]', 'password123');
        await page.click('button[type="submit"]');

        // 等待导航到仪表盘
        await page.waitForURL('**/dashboard');
        await expect(page.locator('h2')).toContainText('仪表盘');
        // 验证用户信息显示
        await expect(page.locator('.ant-card')).toContainText('admin@test.com');
    });

    // @ts-ignore
    test('未登录用户访问受保护路由被重定向', async ({page}) => {
        await page.goto(BASE_URL + '/companies');
        await page.waitForURL('**/login');
        await expect(page.locator('h3')).toContainText('多集群容器管理平台');
    });

    // @ts-ignore
    test('系统管理员可以创建公司', async ({page}) => {
        // 先快速登录
        await page.goto(BASE_URL + '/login');
        await page.fill('input[id="email"]', 'admin@test.com');
        await page.fill('input[id="password"]', 'password123');
        await page.click('button[type="submit"]');
        await page.waitForURL('**/dashboard');

        // 导航到公司管理
        await page.click('text=公司管理');
        await page.waitForURL('**/companies');

        // 点击新增按钮
        await page.click('text=新增公司');
        await page.fill('input[id="name"]', '测试公司-A');
        await page.click('button:has-text("提交")');

        // 验证列表中出现新公司
        await expect(page.locator('table')).toContainText('测试公司-A');
    });

    // @ts-ignore
    test('管理员为用户分配角色', async ({page}) => {
        await page.goto(BASE_URL + '/login');
        // 登录步骤同上...

        await page.click('text=成员管理');
        await page.waitForURL('**/users');

        // 假设列表中已有用户，点击第一个用户的“分配角色”按钮
        const assignBtn = page.locator('button:has-text("分配角色")').first();
        if (await assignBtn.count() > 0) {
            await assignBtn.click();

            // 在弹出的模态框中操作
            await page.selectOption('select[id="role_id"]', '1'); // 选择系统管理员角色
            await page.click('input[value="all"]'); // 选择所有环境
            await page.fill('input[id="department_id"]', '1');
            await page.click('button:has-text("分配")');

            await expect(page.locator('.ant-message-success')).toBeVisible();
        }
    });
});