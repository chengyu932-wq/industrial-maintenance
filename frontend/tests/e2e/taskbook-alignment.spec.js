import { execFileSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { expect, test } from '@playwright/test'
const evidenceDir = '../docs/evidence/taskbook-alignment'
async function login(request,username){
 const captcha=(await(await request.get('/api/auth/captcha')).json()).data
 const code=execFileSync(process.env.E2E_REDIS_CLI,['-n',process.env.E2E_REDIS_DB||'0','GET',`auth:captcha:${captcha.captchaId}`],{encoding:'utf8'}).trim()
 const response=await request.post('/api/auth/login',{data:{username,password:process.env.E2E_PASSWORD,captchaId:captcha.captchaId,captchaCode:code}})
 expect(response.ok()).toBeTruthy();return(await response.json()).data
}
async function session(page,tokens,path){await page.goto('/login');await page.evaluate(tokens=>{localStorage.setItem('maintenance.accessToken',tokens.accessToken);localStorage.setItem('maintenance.refreshToken',tokens.refreshToken)},tokens);await page.goto(path)}
test('reporter submits evidence, supervisor confirms, and only authorized routes appear',async({page,request})=>{
 mkdirSync(evidenceDir,{recursive:true})
 const errors=[];page.on('pageerror',e=>errors.push(e.message))
 const reporter=await login(request,'reporter');const headers={Authorization:`Bearer ${reporter.accessToken}`}
 const running=(await(await request.get('/api/equipment?status=RUNNING&page=1&size=100',{headers})).json()).data.records
 let pending,equipment
 for(const item of running){const response=await request.post('/api/repair-requests',{headers,data:{equipmentId:item.id,priority:'NORMAL',faultDescription:'约束对齐浏览器测试：异常振动',source:'PC'}});if(response.ok()){pending=(await response.json()).data;equipment=item;break}}
 expect(pending).toBeTruthy();expect(pending.workOrderId).toBeNull()
 const uploaded=await request.post(`/api/repair-requests/${pending.repairRequestId}/attachments`,{headers,multipart:{file:{name:'现场证据.txt',mimeType:'text/plain',buffer:Buffer.from('故障现场描述')}}});expect(uploaded.ok()).toBeTruthy()
 await session(page,reporter,'/repair-requests')
 await expect(page.getByRole('heading',{name:'故障报修'})).toBeVisible()
 await expect(page.getByText(pending.requestNo)).toBeVisible()
 await expect(page.getByRole('button',{name:'确认报修',exact:true})).toHaveCount(0)
 const ownRow=page.locator('.el-table__row').filter({hasText:pending.requestNo})
 await ownRow.getByRole('button',{name:'现场资料'}).click()
 const downloadPromise=page.waitForEvent('download')
 await page.getByRole('dialog',{name:'报修现场资料'}).getByRole('button',{name:'下载'}).click()
 expect((await downloadPromise).suggestedFilename()).toBe('现场证据.txt')
 await page.goto('/system/users');await expect(page.getByText('403', {exact:true})).toBeVisible()
 const supervisor=await login(request,'supervisor');await session(page,supervisor,'/repair-requests')
 const row=page.locator('.el-table__row').filter({hasText:pending.requestNo})
 await expect(row.getByRole('button',{name:'确认报修'})).toBeVisible()
 await page.setViewportSize({width:1366,height:768});await page.screenshot({path:`${evidenceDir}/report-confirmation.png`,fullPage:true})
 await row.getByRole('button',{name:'确认报修'}).click()
 await page.getByRole('button',{name:'确认',exact:true}).click()
 await expect(page).toHaveURL(/work-orders\/\d+/)
 await expect(page.getByText('现场证据.txt')).toBeVisible()
 await expect(page.getByRole('button',{name:'智能推荐派单'})).toBeVisible()
 await page.screenshot({path:`${evidenceDir}/confirmed-order.png`,fullPage:true})
 const admin=await login(request,'admin');await session(page,admin,'/system/users');await expect(page.getByRole('button',{name:'删除',exact:true}).first()).toBeVisible()
 expect(errors).toEqual([])
})
