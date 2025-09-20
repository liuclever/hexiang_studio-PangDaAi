<template>
  <el-main class="app-main-content">
    <slot></slot> <!-- Content from router-view -->
  </el-main>
</template>

<script setup lang="ts">
// No specific script needed for this simple wrapper
</script>

<style lang="scss" scoped>
.app-main-content {
  padding: 20px;
  background-color: var(--app-bg-color);
  min-height: calc(100vh - 60px); // Full height minus header
  overflow-y: auto;
}
</style> 