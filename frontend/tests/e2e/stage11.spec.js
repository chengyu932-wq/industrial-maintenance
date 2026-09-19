import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { expect, test } from '@playwright/test'

const evidenceDir = '../docs/evidence/stage11'
const redisCli = process.env.E2E_REDIS_CLI
const password = process.env.E2E_PASSWORD

test.setTimeout(60_000)

async function authenticate(request, username) {
  if (!redisCli || !password) throw new Error('E2E_REDIS_CLI and E2E_PASSWORD are required')
  const captchaResponse = await request.get('/api/auth/captcha')
  expect(captchaResponse.ok()).toBeTruthy()
  const captcha = (await captchaResponse.json()).data
  const code = execFileSync(redisCli, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
  const loginResponse = await request.post('/api/auth/login', { data: { username, password, captchaId: captcha.captchaId, captchaCode: code } })
  expect(loginResponse.ok()).toBeTruthy()
  return (await loginResponse.json()).data
}

async function prepare(page, request, viewport) {
  const auth = await authenticate(request, 'admin')
  await page.setViewportSize(viewport)
  await page.addInitScript(tokens => {
    localStorage.setItem('maintenance.accessToken', tokens.accessToken)
    localStorage.setItem('maintenance.refreshToken', tokens.refreshToken)
  }, auth)
}

function watchConsole(page) {
  const errors = []
  page.on('console', message => { if (message.type() === 'error') errors.push(`console: ${message.text()}`) })
  page.on('pageerror', error => errors.push(`pageerror: ${error.message}`))
  return errors
}

test.beforeAll(() => mkdirSync(evidenceDir, { recursive: true }))

test('1366 dashboard and statistics use real APIs with explainable KPI states', async ({ page, request }) => {
  const errors = watchConsole(page)
  await prepare(page, request, { width: 1366, height: 768 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '运维运行概览' })).toBeVisible()
  await expect(page.getByRole('link', { name: /查看完整统计分析/ })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage11-home-kpi-1366x768.png`, fullPage: true })

  const statisticsResponse = page.waitForResponse(response => response.url().includes('/api/statistics/charts') && response.ok())
  await page.getByRole('link', { name: /查看完整统计分析/ }).click()
  await statisticsResponse
  await expect(page.getByRole('heading', { name: '运维统计分析' })).toBeVisible()
  await expect(page.getByText('一次修复率（首次验收通过）', { exact: true })).toBeVisible()
  await page.getByLabel('核心指标详情').screenshot({ path: `${evidenceDir}/stage11-kpi-detail-1366x768.png` })
  await page.getByText('平均故障间隔时间', { exact: true }).hover()
  await expect(page.getByText('运行小时增量合计 / 完成维修工单数', { exact: true })).toBeVisible()

  const equipmentChart = page.getByLabel('设备状态统计图')
  await equipmentChart.screenshot({ path: `${evidenceDir}/stage11-equipment-statistics-1366x768.png` })
  const trendChart = page.getByLabel('维修完成趋势统计图')
  await trendChart.screenshot({ path: `${evidenceDir}/stage11-work-order-trend-1366x768.png` })
  const inventoryChart = page.getByLabel('备件消耗统计图')
  await inventoryChart.screenshot({ path: `${evidenceDir}/stage11-inventory-statistics-1366x768.png` })

  const sevenDayResponse = page.waitForResponse(response => response.url().includes('/api/statistics/charts') && response.url().includes('LAST_7_DAYS') && response.ok())
  const rangeBox = page.getByRole('combobox', { name: '预设时间范围' })
  await rangeBox.focus()
  await rangeBox.press('ArrowDown')
  await rangeBox.press('Home')
  await rangeBox.press('Enter')
  await sevenDayResponse
  await expect(page.getByText(/统计周期：\d{4}-\d{2}-\d{2} 至 \d{4}-\d{2}-\d{2}/)).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage11-range-7-days-1366x768.png`, fullPage: true })
  expect(errors).toEqual([])
})

test('1920 statistics page has no horizontal overflow or browser errors', async ({ page, request }) => {
  const errors = watchConsole(page)
  await prepare(page, request, { width: 1920, height: 1080 })
  const statisticsResponse = page.waitForResponse(response => response.url().includes('/api/statistics/charts') && response.ok())
  await page.goto('/statistics')
  await statisticsResponse
  await expect(page.getByRole('heading', { name: '运维统计分析' })).toBeVisible()
  await expect(page.getByText('设备状态分布', { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage11-statistics-1920x1080.png`, fullPage: true })
  expect(errors).toEqual([])
})
