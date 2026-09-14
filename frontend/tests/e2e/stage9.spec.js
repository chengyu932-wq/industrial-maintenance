import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { expect, test } from '@playwright/test'

const evidenceDir = '../docs/evidence/stage9'
const redisCli = process.env.E2E_REDIS_CLI
const password = process.env.E2E_PASSWORD

async function authenticate(request) {
  if (!redisCli || !password) throw new Error('E2E_REDIS_CLI and E2E_PASSWORD are required')
  const captchaResponse = await request.get('/api/auth/captcha')
  expect(captchaResponse.ok()).toBeTruthy()
  const captcha = (await captchaResponse.json()).data
  const code = execFileSync(redisCli, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
  const loginResponse = await request.post('/api/auth/login', {
    data: { username: 'supervisor', password, captchaId: captcha.captchaId, captchaCode: code },
  })
  expect(loginResponse.ok()).toBeTruthy()
  return (await loginResponse.json()).data
}

async function prepare(page, request, viewport) {
  const auth = await authenticate(request)
  await page.setViewportSize(viewport)
  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem('maintenance.accessToken', accessToken)
    localStorage.setItem('maintenance.refreshToken', refreshToken)
  }, auth)
  return auth
}

async function pendingOrder(request, accessToken) {
  const response = await request.get('/api/work-orders?type=REPAIR&status=PENDING_ASSIGN&page=1&size=100', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  expect(response.ok()).toBeTruthy()
  const records = (await response.json()).data.records
  const order = records.find(item => item.slaRuleId)
  expect(order, '需要至少一张已绑定 SLA 的待派维修工单').toBeTruthy()
  return order
}

function watchConsole(page) {
  const errors = []
  page.on('console', message => {
    if (message.type() === 'error') errors.push(`console: ${message.text()} @ ${message.location().url}`)
  })
  page.on('pageerror', error => errors.push(`pageerror: ${error.message}`))
  return errors
}

test.beforeAll(() => mkdirSync(evidenceDir, { recursive: true }))

test('1366 supervisor can inspect Top3, manually choose Top2, and use SLA messages', async ({ page, request }) => {
  const errors = watchConsole(page)
  const auth = await prepare(page, request, { width: 1366, height: 768 })
  const order = await pendingOrder(request, auth.accessToken)
  const recommendationResponse = await request.get(`/api/work-orders/${order.id}/dispatch-candidates`, {
    headers: { Authorization: `Bearer ${auth.accessToken}` },
  })
  expect(recommendationResponse.ok()).toBeTruthy()
  const recommendations = (await recommendationResponse.json()).data
  expect(recommendations).toHaveLength(3)

  await page.goto(`/work-orders/${order.id}`)
  await expect(page.getByRole('heading', { name: order.workOrderNo })).toBeVisible()
  await expect(page.getByText('SLA 时限')).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage9-work-order-sla-1366x768.png`, fullPage: true })

  await page.getByRole('button', { name: '智能推荐派单' }).click()
  const dialog = page.getByRole('dialog', { name: '智能派单推荐' })
  await expect(dialog.getByText('Top 3 推荐')).toBeVisible()
  await expect.poll(() => dialog.evaluate(element => {
    let current = element
    let minimumOpacity = 1
    while (current) {
      minimumOpacity = Math.min(minimumOpacity, Number.parseFloat(getComputedStyle(current).opacity) || 1)
      current = current.parentElement
    }
    return minimumOpacity
  })).toBe(1)
  await expect(dialog.getByText(recommendations[0].engineerName, { exact: true })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage9-dispatch-top3-1366x768.png`, fullPage: true })

  await dialog.getByRole('combobox').first().click()
  await page.getByRole('option', { name: new RegExp(recommendations[1].engineerName) }).click()
  await dialog.getByRole('textbox').last().fill('主管人工选择 Top2，验证推荐不替代人工决策')
  await page.screenshot({ path: `${evidenceDir}/stage9-dispatch-manual-top2-1366x768.png`, fullPage: true })
  const assignResponse = page.waitForResponse(response =>
    response.url().endsWith(`/api/work-orders/${order.id}/assign`) && response.request().method() === 'POST')
  await dialog.getByRole('button', { name: '确认派单' }).click()
  expect((await assignResponse).ok()).toBeTruthy()
  await expect(page.getByText('已派单', { exact: true }).first()).toBeVisible()
  const assigned = await request.get(`/api/work-orders/${order.id}`, {
    headers: { Authorization: `Bearer ${auth.accessToken}` },
  })
  expect((await assigned.json()).data.workOrder.assignedEngineerId).toBe(recommendations[1].engineerId)
  await page.screenshot({ path: `${evidenceDir}/stage9-dispatch-confirmed-1366x768.png`, fullPage: true })

  await page.goto('/sla')
  await expect(page.getByRole('heading', { name: 'SLA 管理' })).toBeVisible()
  await expect(page.getByText('30').first()).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage9-sla-rules-1366x768.png`, fullPage: true })
  await page.getByRole('tab', { name: '超时事件' }).click()
  await expect(page.getByRole('columnheader', { name: '升级对象' })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage9-sla-events-1366x768.png`, fullPage: true })

  await page.goto('/notifications')
  await expect(page.getByRole('heading', { name: '消息中心' })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage9-notifications-unread-1366x768.png`, fullPage: true })
  const unread = page.getByText('未读', { exact: true })
  if (await unread.count()) {
    const notification = page.getByRole('button', { name: /工单响应SLA超时.*未读/ }).first()
    const markReadResponse = page.waitForResponse(response => /\/api\/notifications\/\d+\/read$/.test(response.url()))
    await notification.click()
    expect((await markReadResponse).ok()).toBeTruthy()
    await expect(page).toHaveURL(/\/work-orders\/\d+$/)
    await page.goBack()
    await expect(page.getByRole('heading', { name: '消息中心' })).toBeVisible()
  }
  const readAll = page.getByRole('button', { name: '全部已读' })
  if (await page.getByText('未读', { exact: true }).count()) {
    const readResponse = page.waitForResponse(response => response.url().endsWith('/api/notifications/read-all'))
    await readAll.click()
    expect((await readResponse).ok()).toBeTruthy()
  }
  await expect(page.getByText('未读', { exact: true })).toHaveCount(0)
  await page.screenshot({ path: `${evidenceDir}/stage9-notifications-read-1366x768.png`, fullPage: true })
  expect(errors).toEqual([])
})

test('1920 key dispatch and SLA pages render without overflow or console errors', async ({ page, request }) => {
  const errors = watchConsole(page)
  const auth = await prepare(page, request, { width: 1920, height: 1080 })
  const order = await pendingOrder(request, auth.accessToken)
  await page.goto(`/work-orders/${order.id}`)
  await page.getByRole('button', { name: '智能推荐派单' }).click()
  const dialog = page.getByRole('dialog', { name: '智能派单推荐' })
  await expect(dialog.getByText('Top 3 推荐')).toBeVisible()
  await expect.poll(() => dialog.evaluate(element => {
    let current = element
    let minimumOpacity = 1
    while (current) {
      minimumOpacity = Math.min(minimumOpacity, Number.parseFloat(getComputedStyle(current).opacity) || 1)
      current = current.parentElement
    }
    return minimumOpacity
  })).toBe(1)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage9-dispatch-top3-1920x1080.png`, fullPage: true })

  await page.goto('/sla')
  await expect(page.getByRole('heading', { name: 'SLA 管理' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage9-sla-rules-1920x1080.png`, fullPage: true })

  await page.goto('/notifications')
  await expect(page.getByRole('heading', { name: '消息中心' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage9-notifications-1920x1080.png`, fullPage: true })
  expect(errors).toEqual([])
})
