import { useAuthStore } from '../stores/auth'

export default {
  mounted(element, binding) {
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    if (!required.some((code) => useAuthStore().hasPermission(code))) element.remove()
  },
}
