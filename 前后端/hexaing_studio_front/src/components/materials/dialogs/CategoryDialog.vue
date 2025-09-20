<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEditing ? '编辑分类' : '添加分类'"
    width="400px"
    @update:modelValue="$emit('update:modelValue', $event)"
    @close="resetForm"
  >
    <el-form
      ref="categoryFormRef"
      :model="categoryForm"
      :rules="categoryRules"
      label-width="80px"
    >
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
      </el-form-item>
      
      <el-form-item label="排序" prop="orderId">
        <el-input-number v-model="categoryForm.orderId" :min="1" :max="999" />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" @click="submitCategory">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';

interface CategoryForm {
  id?: number;
  name: string;
  orderId: number;
}

const props = defineProps<{
  modelValue: boolean;
  isEditing: boolean;
  initialData?: CategoryForm;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'save', category: CategoryForm): void;
}>();

const categoryFormRef = ref<FormInstance>();
const categoryForm = reactive<CategoryForm>({
  name: '',
  orderId: 1
});

const categoryRules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ]
});

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    resetForm();
    if (props.isEditing && props.initialData) {
      Object.assign(categoryForm, props.initialData);
    } else {
      categoryForm.name = '';
      categoryForm.orderId = 1;
      delete categoryForm.id;
    }
  }
});

const submitCategory = async () => {
  if (!categoryFormRef.value) return;
  await categoryFormRef.value.validate((valid) => {
    if (valid) {
      emit('save', { ...categoryForm });
    }
  });
};

const resetForm = () => {
  categoryFormRef.value?.resetFields();
};
</script> 