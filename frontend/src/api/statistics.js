import http from './http'

export function getStatisticsOverview(params) { return http.get('/statistics/overview', { params }) }
export function getStatisticsKpis(params) { return http.get('/statistics/kpis', { params }) }
export function getStatisticsCharts(params) { return http.get('/statistics/charts', { params }) }
