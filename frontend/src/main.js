import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import ElementPlus from 'element-plus'
import './assets/main.css'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import permission from './directives/permission'

const app = createApp(App)
app.directive('permission', permission)
app
  .use(pinia)
  .use(ElementPlus)
  .use(router)
  .mount('#app')
