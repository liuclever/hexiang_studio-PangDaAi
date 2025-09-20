<template>
  <el-card shadow="hover" class="page-header-card">
    <div class="page-title">
      <h2>资料管理</h2>
      <el-button type="primary" @click="$emit('add')">
        <el-icon><Upload /></el-icon>上传资料
      </el-button>
    </div>
  </el-card>

  <!-- 搜索和筛选 -->
  <el-card shadow="hover" class="search-card">
    <div class="search-container">
      <el-input
        :model-value="searchQuery"
        placeholder="搜索资料名称或描述"
        class="search-input"
        clearable
        @update:modelValue="$emit('update:searchQuery', $event)"
        @clear="$emit('search')"
        @keyup.enter="$emit('search')"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="$emit('search')">搜索</el-button>
        </template>
      </el-input>

      <div class="filter-container">
        <el-select
          :model-value="filterCategory"
          placeholder="资料分类"
          clearable
          @update:modelValue="$emit('update:filterCategory', $event)"
          @change="$emit('search')"
        >
          <el-option label="全部分类" value="" />
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>

        <el-select
          :model-value="filterType"
          placeholder="文件类型"
          clearable
          @update:modelValue="$emit('update:filterType', $event)"
          @change="$emit('search')"
        >
          <el-option label="全部类型" value="" />
          <el-option
            v-for="fileType in fileTypes"
            :key="fileType.value"
            :label="fileType.label"
            :value="fileType.value"
          />
        </el-select>

        <el-date-picker
          :model-value="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @update:modelValue="$emit('update:dateRange', $event)"
          @change="$emit('search')"
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Upload, Search } from '@element-plus/icons-vue';

interface Category {
  id: number;
  name: string;
  orderId: number;
}

interface FileType {
  value: string;
  label: string;
}

defineProps<{
  searchQuery: string;
  filterCategory: string;
  filterType: string;
  dateRange: [string, string] | null;
  categories: Category[];
  fileTypes: FileType[];
}>();

defineEmits<{
  (e: 'add'): void;
  (e: 'search'): void;
  (e: 'update:searchQuery', value: string): void;
  (e: 'update:filterCategory', value: string): void;
  (e: 'update:filterType', value: string): void;
  (e: 'update:dateRange', value: [string, string] | null): void;
}>();
</script>

<style lang="scss" scoped>
.page-header-card {
  .page-title {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0;
      font-size: 1.5rem;
      color: var(--el-text-color-primary);
    }
  }
}

.search-card {
  .search-container {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 15px;

    .search-input {
      width: 300px;
    }

    .filter-container {
      display: flex;
      gap: 15px;
      flex-wrap: wrap;

      .el-select {
        width: 120px;
      }
    }

    @media (max-width: 768px) {
      .search-input {
        width: 100%;
      }

      .filter-container {
        width: 100%;
      }
    }
  }
}
</style> 