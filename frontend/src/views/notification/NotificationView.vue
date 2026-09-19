<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as api from '../../api/notification'
const router=useRouter();const loading=shallowRef(false);const records=shallowRef<any[]>([]);const total=shallowRef(0);const query=reactive({page:1,size:15})
async function load(){loading.value=true;try{const r=await api.page(query);records.value=r.data.records;total.value=r.data.total}catch(e:any){ElMessage.error(e.response?.data?.message||'消息加载失败')}finally{loading.value=false}}
async function open(item:any){try{if(!item.read)await api.read(item.id);if(item.businessType==='WORK_ORDER'&&item.businessId)await router.push(`/work-orders/${item.businessId}`);else await load()}catch(e:any){ElMessage.error(e.response?.data?.message||'消息处理失败')}}
async function readAll(){try{await api.readAll();records.value=records.value.map(item=>({...item,read:true}));ElMessage.success('全部消息已读');await load()}catch(e:any){ElMessage.error(e.response?.data?.message||'操作失败')}}
onMounted(load)
</script>
<template><section class="module-page"><header class="module-header"><div><p class="eyebrow">NOTIFICATION CENTER</p><h1>消息中心</h1><p>查看分配给你的 SLA 超时升级提醒。</p></div><div class="header-actions"><el-button :disabled="!records.some(i=>!i.read)" @click="readAll">全部已读</el-button></div></header><el-card shadow="never"><div v-loading="loading" class="notification-list"><button v-for="item in records" :key="item.id" type="button" class="notification-row" :class="{unread:!item.read}" @click="open(item)"><span class="notification-dot"/><span class="notification-copy"><strong>{{item.title}}</strong><span>{{item.content}}</span><small>{{item.createdAt}}</small></span><el-tag v-if="!item.read" type="primary">未读</el-tag><span class="notification-arrow">{{item.businessType==='WORK_ORDER'&&item.businessId?'查看工单 →':'标记已读'}}</span></button><el-empty v-if="!loading&&!records.length" description="暂无站内消息" :image-size="72"/></div><div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @change="load"/></div></el-card></section></template>
