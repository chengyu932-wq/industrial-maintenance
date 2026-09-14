import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { expect, test } from '@playwright/test'

const evidenceDir = '../docs/evidence/stage10'
const redisCli = process.env.E2E_REDIS_CLI
const password = process.env.E2E_PASSWORD

async function authenticate(request, username) {
  if (!redisCli || !password) throw new Error('E2E_REDIS_CLI and E2E_PASSWORD are required')
  const captchaResponse = await request.get('/api/auth/captcha')
  expect(captchaResponse.ok()).toBeTruthy()
  const captcha = (await captchaResponse.json()).data
  const code = execFileSync(redisCli, ['GET', `auth:captcha:${captcha.captchaId}`], { encoding: 'utf8' }).trim()
  const loginResponse = await request.post('/api/auth/login', {
    data: { username, password, captchaId: captcha.captchaId, captchaCode: code },
  })
  expect(loginResponse.ok()).toBeTruthy()
  return (await loginResponse.json()).data
}

async function prepare(page, request, username, viewport) {
  const auth = await authenticate(request, username)
  await page.setViewportSize(viewport)
  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem('maintenance.accessToken', accessToken)
    localStorage.setItem('maintenance.refreshToken', refreshToken)
  }, auth)
  return auth
}

function authorized(token) {
  return { Authorization: `Bearer ${token}` }
}

function watchConsole(page) {
  const errors = []
  page.on('console', message => {
    if (message.type() === 'error') errors.push(`console: ${message.text()} @ ${message.location().url}`)
  })
  page.on('pageerror', error => errors.push(`pageerror: ${error.message}`))
  return errors
}

async function createPendingKnowledge(request) {
  const engineer = await authenticate(request, 'engineer')
  const orderResponse = await request.get('/api/work-orders?type=REPAIR&status=COMPLETED&page=1&size=100', {
    headers: authorized(engineer.accessToken),
  })
  expect(orderResponse.ok()).toBeTruthy()
  const orders = (await orderResponse.json()).data.records
  for (const order of orders) {
    const createResponse = await request.post(`/api/knowledge/from-work-order/${order.id}`, {
      headers: authorized(engineer.accessToken),
    })
    if (!createResponse.ok()) continue
    const articleId = (await createResponse.json()).data
    const submitResponse = await request.post(`/api/knowledge/${articleId}/submit`, {
      headers: authorized(engineer.accessToken),
    })
    expect(submitResponse.ok()).toBeTruthy()
    return { articleId, order }
  }
  throw new Error('需要至少一张尚未沉淀知识且维修记录完整的已完成维修工单')
}

test.beforeAll(() => mkdirSync(evidenceDir, { recursive: true }))

test('1366 supervisor reviews knowledge and inspects TF-IDF Top5', async ({ page, request }) => {
  const errors = watchConsole(page)
  const pending = await createPendingKnowledge(request)
  await prepare(page, request, 'supervisor', { width: 1366, height: 768 })

  await page.goto(`/knowledge?article=${pending.articleId}`)
  await expect(page.getByRole('heading', { name: '故障知识库' })).toBeVisible()
  const drawer = page.getByRole('dialog', { name: '知识详情' })
  await expect(drawer.getByText('待审核', { exact: true })).toBeVisible()
  await expect(drawer.getByText(pending.order.workOrderNo, { exact: true })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage10-knowledge-review-1366x768.png`, fullPage: true })

  await drawer.getByRole('button', { name: '审核通过并发布' }).click()
  const prompt = page.getByRole('dialog', { name: '审核通过' })
  await prompt.getByRole('textbox').fill('维修结论完整，审核通过')
  const reviewResponse = page.waitForResponse(response =>
    response.url().endsWith(`/api/knowledge/${pending.articleId}/review`) && response.request().method() === 'POST')
  await prompt.getByRole('button', { name: 'OK' }).click()
  expect((await reviewResponse).ok()).toBeTruthy()
  await expect(page.getByText('知识已正式发布')).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage10-knowledge-published-1366x768.png`, fullPage: true })

  await page.goto(`/work-orders/${pending.order.id}`)
  await expect(page.getByRole('heading', { name: pending.order.workOrderNo })).toBeVisible()
  await expect(page.getByText('历史相似工单', { exact: true })).toBeVisible()
  await expect(page.getByText(/Top [1-5]/)).toBeVisible()
  await expect(page.getByText('仅供维修经验参考', { exact: false })).toBeVisible()
  await page.screenshot({ path: `${evidenceDir}/stage10-similar-top5-1366x768.png`, fullPage: true })
  expect(errors).toEqual([])
})

test('1920 published knowledge and similar-work-order pages have no overflow', async ({ page, request }) => {
  const errors = watchConsole(page)
  const auth = await prepare(page, request, 'supervisor', { width: 1920, height: 1080 })
  const knowledgeResponse = await request.get('/api/knowledge?status=PUBLISHED&page=1&size=10', {
    headers: authorized(auth.accessToken),
  })
  expect(knowledgeResponse.ok()).toBeTruthy()
  const article = (await knowledgeResponse.json()).data.records[0]
  expect(article).toBeTruthy()

  await page.goto(`/knowledge?article=${article.id}`)
  await expect(page.getByRole('heading', { name: '故障知识库' })).toBeVisible()
  await expect(page.getByRole('dialog', { name: '知识详情' }).getByText('已发布', { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage10-knowledge-detail-1920x1080.png`, fullPage: true })

  await page.goto(`/work-orders/${article.sourceWorkOrderId}`)
  await expect(page.getByText('历史相似工单', { exact: true })).toBeVisible()
  await expect(page.getByText(/Top [1-5]/)).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBeTruthy()
  await page.screenshot({ path: `${evidenceDir}/stage10-similar-top5-1920x1080.png`, fullPage: true })
  expect(errors).toEqual([])
})
