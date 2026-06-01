<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
page {
  background-color: #F8FAFC;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #0b1c30;
}

.container-margin {
  padding-left: 16px;
  padding-right: 16px;
}

.card-padding {
  padding: 20px;
}
</style>
