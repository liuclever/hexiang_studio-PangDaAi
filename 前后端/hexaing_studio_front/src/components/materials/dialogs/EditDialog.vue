<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑资料信息"
    width="500px"
    @update:modelValue="$emit('update:modelValue', $event)"
  >
    <el-form
      v-if="editMaterialForm"
      ref="editMaterialFormRef"
      :model="editMaterialForm"
      label-width="100px"
    >
      <el-form-item label="资料分类">
        <el-select v-model="editMaterialForm.categoryId" placeholder="请选择资料分类" style="width: 100%">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="资料描述">
        <el-input
          v-model="editMaterialForm.description"
          type="textarea"
          placeholder="请输入资料描述"
          :rows="3"
        />
      </el-form-item>
      
      <el-form-item label="权限设置">
        <el-radio-group v-model="editMaterialForm.isPublic">
          <el-radio :label="true">公开</el-radio>
          <el-radio :label="false">私有</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="submitUpdate">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import type { FormInstance } from 'element-plus';

interface Category {
  id: number;
  name: string;
}

interface MaterialUpdate {
  id: number;
  categoryId?: number;
  description?: string;
  isPublic: boolean;
}

interface MaterialData {
  id: number;
  categoryId: number | null;
  description: string | null;
  isPublic: number;
}

const props = defineProps<{
  modelValue: boolean;
  materialData?: MaterialData | null;
  categories: Category[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'save', data: MaterialUpdate): void;
}>();

const editMaterialFormRef = ref<FormInstance>();
const editMaterialForm = ref<any>(null);

watch(() => props.materialData, (newData) => {
  if (newData) {
    editMaterialForm.value = {
      id: newData.id,
      categoryId: newData.categoryId || 0,
      description: newData.description || '',
      isPublic: newData.isPublic === 1,
    };
  } else {
    editMaterialForm.value = null;
  }
}, { immediate: true, deep: true });

const submitUpdate = async () => {
  if (!editMaterialFormRef.value || !editMaterialForm.value) return;

  const updateData: MaterialUpdate = {
    id: editMaterialForm.value.id,
    isPublic: editMaterialForm.value.isPublic,
  };
  if (editMaterialForm.value.categoryId > 0) {
    updateData.categoryId = editMaterialForm.value.categoryId;
  }
  if (editMaterialForm.value.description.trim()) {
    updateData.description = editMaterialForm.value.description;
  }
  
  emit('save', updateData);
};
</script> 