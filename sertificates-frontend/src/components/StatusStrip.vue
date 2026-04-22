<template>
  <div class="status-strip" :style="{ background: color }"></div>
</template>

<script setup>
defineProps({
  color: { type: String, default: '#9e9e9e' }
})
</script>

<style scoped>
.status-strip {
  width: 6px;
  border-radius: 6px;
}
</style>
