<template>
  <el-card shadow="hover" class="category-card">
    <template #header>
      <div class="card-header">
        <h3>分类管理</h3>
        <el-button type="primary" size="small" @click="$emit('add')">
          <el-icon><Plus /></el-icon>添加分类
        </el-button>
      </div>
    </template>
    
    <div class="category-list">
      <el-tag
        v-for="category in categories"
        :key="category.id"
        closable
        :disable-transitions="false"
        class="category-tag"
        @close="$emit('delete', category)"
      >
        {{ category.name }}
        <el-icon class="edit-icon" @click.stop="$emit('edit', category)"><Edit /></el-icon>
      </el-tag>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Plus, Edit } from '@element-plus/icons-vue';

interface Category {
  id: number;
  name: string;
  orderId: number;
}

defineProps<{
  isAdmin: boolean;
  categories: Category[];
}>();

defineEmits<{
  (e: 'add'): void;
  (e: 'edit', category: Category): void;
  (e: 'delete', category: Category): void;
}>();
</script>

<style lang="scss" scoped>
.category-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .category-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    
    .category-tag {
      display: flex;
      align-items: center;
      padding-right: 8px;
      
      .edit-icon {
        margin-left: 5px;
        cursor: pointer;
      }
    }
  }
}
</style> 