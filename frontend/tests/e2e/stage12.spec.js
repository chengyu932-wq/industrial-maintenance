import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { expect, test } from '@playwright/test'

const evidenceDir = '../docs/evidence/stage12'
const redisCli = process.env.E2E_REDIS_CLI
const password = process.env.E2E_PASSWORD

async function authenticate(request) {
  if (!redisCli || !password) throw new Error('E2E_REDIS_CLI and E2E_PASSWORD are required')
  const captcha = (await (await request.get('/api/auth/captcha')).json()).data
  const code = execFileSync(redisCli, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
  const response = await request.post('/api/auth/login', {
    data: { username: 'admin', password, captchaId: captcha.captchaId, captchaCode: code },
  })
  expect(response.ok()).toBeTruthy()
  return (await response.json()).data
}

test.beforeAll(() => mkdirSync(evidenceDir, { recursive: true }))

test('all five roles can sign in through the browser login form', async ({ page }) => {
  if (!redisCli || !password) throw new Error('E2E_REDIS_CLI and E2E_PASSWORD are required')
  const users = ['admin', 'supervisor', 'engineer', 'warehouse', 'reporter']
  for (const username of users) {
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
    const captchaResponse = page.waitForResponse(response => response.url().endsWith('/api/auth/captcha'))
    await page.goto('/login')
    const captcha = (await (await captchaResponse).json()).data
    const code = execFileSync(redisCli, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
    await page.getByLabel('账号').fill(username)
    await page.getByLabel('密码').fill(password)
    await page.getByPlaceholder('输入验证码').fill(code)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page.getByRole('heading', { name: '工作台' })).toBeVisible()
    await expect(page.getByText(`你好，`).first()).toBeVisible()
  }
  await page.screenshot({ path: `${evidenceDir}/stage12-five-role-login-1366x768.png`, fullPage: true })
})

test('stage12 task-book gap features are reachable with real backend data', async ({ page, request }) => {
  const consoleErrors = []
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  page.on('pageerror', error => consoleErrors.push(error.message))
  const auth = await authenticate(request)
  const headers = { Authorization: `Bearer ${auth.accessToken}` }
  await page.setViewportSize({ width: 1366, height: 768 })
  await page.addInitScript(tokens => {
    localStorage.setItem('maintenance.accessToken', tokens.accessToken)
    localStorage.setItem('maintenance.refreshToken', tokens.refreshToken)
  }, auth)

  await page.goto('/system/users')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Excel 导入' })).toBeVisible()
  await expect(page.getByRole('button', { name: '下载模板' })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage12-users-1366x768.png`, fullPage: true })

  const template = await request.get('/api/users/import-template', { headers })
  expect(template.ok()).toBeTruthy()
  expect(template.headers()['content-type']).toContain('spreadsheetml')

  const equipmentPage = await request.get('/api/equipment?page=1&size=1', { headers })
  const equipment = (await equipmentPage.json()).data.records[0]
  await page.goto(`/equipment/${equipment.id}`)
  await expect(page.getByRole('heading', { name: equipment.name })).toBeVisible()
  const runtimeTab = page.getByRole('tab', { name: '运行小时' })
  await runtimeTab.click()
  await expect(runtimeTab).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByText(/暂无人工运行小时记录|归属日期/).first()).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage12-runtime-hours-1366x768.png`, fullPage: true })

  const equipmentAttachmentName = `stage12-equipment-${Date.now()}.txt`
  const equipmentUpload = await request.post(`/api/equipment/${equipment.id}/attachments`, {
    headers,
    multipart: { file: { name: equipmentAttachmentName, mimeType: 'text/plain', buffer: Buffer.from('stage12 equipment attachment evidence') } },
  })
  expect(equipmentUpload.ok()).toBeTruthy()
  const equipmentAttachments = await request.get(`/api/equipment/${equipment.id}/attachments`, { headers })
  const equipmentAttachment = (await equipmentAttachments.json()).data.find(item => item.fileName === equipmentAttachmentName)
  expect(equipmentAttachment).toBeTruthy()
  const equipmentDownload = await request.get(`/api/equipment/${equipment.id}/attachments/${equipmentAttachment.id}`, { headers })
  expect(equipmentDownload.ok()).toBeTruthy()
  await page.reload()
  const attachmentTab = page.getByRole('tab', { name: '设备附件' })
  await attachmentTab.click()
  await expect(page.getByText(equipmentAttachmentName)).toBeVisible()
  await expect(page.getByRole('button', { name: '上传附件' })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage12-equipment-attachments-1366x768.png`, fullPage: true })

  const orders = await request.get('/api/work-orders?page=1&size=1', { headers })
  const order = (await orders.json()).data.records[0]
  const pdf = await request.get(`/api/work-orders/${order.id}/pdf`, { headers })
  expect(pdf.ok()).toBeTruthy()
  expect(pdf.headers()['content-type']).toContain('application/pdf')
  expect((await pdf.body()).subarray(0, 4).toString()).toBe('%PDF')

  const orderAttachmentName = `stage12-work-order-${Date.now()}.txt`
  const orderUpload = await request.post(`/api/work-orders/${order.id}/attachments?type=REPAIR`, {
    headers,
    multipart: { file: { name: orderAttachmentName, mimeType: 'text/plain', buffer: Buffer.from('stage12 work order attachment evidence') } },
  })
  expect(orderUpload.ok()).toBeTruthy()
  const orderAttachments = await request.get(`/api/work-orders/${order.id}/attachments`, { headers })
  const orderAttachment = (await orderAttachments.json()).data.find(item => item.fileName === orderAttachmentName)
  expect(orderAttachment).toBeTruthy()
  const orderDownload = await request.get(`/api/work-orders/${order.id}/attachments/${orderAttachment.id}`, { headers })
  expect(orderDownload.ok()).toBeTruthy()
  await page.goto(`/work-orders/${order.id}`)
  await expect(page.getByText('工单附件', { exact: true })).toBeVisible()
  await expect(page.getByText(orderAttachmentName)).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage12-work-order-attachments-1366x768.png`, fullPage: true })

  const logs = await request.get('/api/operation-logs?page=1&size=10', { headers })
  expect(logs.ok()).toBeTruthy()
  expect((await logs.json()).data.total).toBeGreaterThan(0)
  expect(consoleErrors).toEqual([])
})
