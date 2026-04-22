import { boot } from 'quasar/wrappers'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

export default boot(({ store }) => {
  store.use(piniaPluginPersistedstate)
})
