import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { expect, test } from '@playwright/test'

test.setTimeout(60_000)
test('statistics drilldown, heatmap and CSV export work in Chrome', async ({ page, request }) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const captchaResponse = await request.get('/api/auth/captcha')
  expect(captchaResponse.ok()).toBeTruthy()
  const captcha = (await captchaResponse.json()).data
  const code = execFileSync(process.env.E2E_REDIS_CLI, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
  const loginResponse = await request.post('/api/auth/login', {
    data: { username: 'admin', password: process.env.E2E_PASSWORD, captchaId: captcha.captchaId, captchaCode: code },
  })
  expect(loginResponse.ok()).toBeTruthy()
  const tokens = (await loginResponse.json()).data
  await page.addInitScript(value => {
    localStorage.setItem('maintenance.accessToken', value.accessToken)
    localStorage.setItem('maintenance.refreshToken', value.refreshToken)
  }, tokens)

  await page.goto('/statistics')
  await expect(page.getByRole('heading', { name: '维修工单多维分析' })).toBeVisible()
  await expect(page.getByRole('img', { name: '每日维修完成热力图' })).toBeVisible()
  await expect(page.locator('.statistics-page .el-loading-mask')).toBeHidden()
  const range = page.getByRole('combobox', { name: '预设时间范围' })
  await range.focus()
  await range.press('ArrowDown')
  await range.press('Enter')
  await expect(page.getByRole('button', { name: /筛选按车间/ }).first()).toBeVisible()
  const filtered = page.waitForResponse(response => response.url().includes('/api/statistics/drilldowns') && response.url().includes('workshopId=') && response.ok())
  await page.getByRole('button', { name: /筛选按车间/ }).first().click()
  await filtered
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出图表数据 CSV' }).click()
  const exported = await download
  expect(exported.suggestedFilename()).toMatch(/运维统计_.*\.csv/)
  expect(readFileSync(await exported.path(), 'utf8')).toContain('车间维修')

  const equipmentResponse = await request.get('/api/equipment?page=1&size=1', {
    headers: { Authorization: `Bearer ${tokens.accessToken}` },
  })
  expect(equipmentResponse.ok()).toBeTruthy()
  const equipment = (await equipmentResponse.json()).data.records[0]
  await page.goto(`/equipment/${equipment.id}`)
  await expect(page.getByRole('button', { name: '打印二维码' })).toBeEnabled()
  await page.getByRole('tab', { name: '维修工单' }).click()
  await expect(page.getByRole('tab', { name: '备件领用' })).toBeVisible()
  await page.getByRole('tab', { name: '备件领用' }).click()
  await expect(page.getByText(/暂无备件领用记录|关联工单/).first()).toBeVisible()
  const popupPromise = page.waitForEvent('popup')
  await page.getByRole('button', { name: '打印二维码' }).click()
  const popup = await popupPromise
  await expect(popup.getByAltText('设备报修二维码')).toBeVisible()
  await expect.poll(() => popup.locator('#qr').evaluate(image => image.naturalWidth)).toBeGreaterThan(0)
  await popup.close()
  expect(errors).toEqual([])
})
