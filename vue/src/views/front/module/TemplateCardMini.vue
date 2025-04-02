<template>
  <div class="template-card-mini"
       :style="{ backgroundColor: template.config.deviceType === 'DATASET' ? '#f0ecf7' : '#f8f9fa' }"
  >
    <div class="template-name">{{ template.name }}</div>
    <div class="template-id">ID: {{ template.id }}</div>
    <div class="template-config">
      <div>存储聚合时间粒度: {{ template.config.aggregationTime }}ms</div>
      <div>存储模式: {{ storageMode }}</div>
      <div v-if="showDataTypes">
        传感器数据类型配置：
        <el-tag v-for="(type, name) in template.config.dataTypes" :key="name" class="data-type-tag">
          {{ name }}: {{ type }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    template: {
      type: Object,
      required: true
    },
    //是否显示数据类型标签
    showDataTypes: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    storageMode() {
      return this.template.config.storageMode
    }
  }
}
</script>

<style>
.template-card-mini {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 1rem;
  margin-top: 8px;
}

.template-name {
  font-weight: bold;
}

.template-id {
  font-size: 0.8em;
  color: #909399;
}

.template-config {
  font-size: 0.9em;
  margin-top: 6px;
}
</style>