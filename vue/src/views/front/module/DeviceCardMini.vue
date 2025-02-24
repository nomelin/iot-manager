<template>
  <div class="device-card-mini">
    <div class="device-name">{{ device.name }}</div>
    <div class="device-id">ID: {{ device.id }}</div>
    <div class="device-config">
      <div>存储聚合时间粒度: {{ device.config.aggregationTime }}ms</div>
      <div>存储模式: {{ storageMode }}</div>
      <div v-if="showDataTypes">
        传感器数据类型配置：
        <el-tag v-for="(type, name) in device.config.dataTypes" :key="name" class="data-type-tag">
          {{ name }}: {{ type }}
        </el-tag>
      </div>
      <div v-if="device.groups && device.groups.length > 0">
        所属组：
        <el-tag v-for="groupId in device.groupIds" :key="group" class="group-tag">
          {{ groupId }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    device: {
      type: Object,
      required: true
    },
    // 是否显示数据类型标签
    showDataTypes: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    storageMode() {
      return this.device.config.storageMode;
    }
  }
}
</script>

<style scoped>
.device-card-mini {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 1rem;
  margin-top: 8px;
}

.device-name {
  font-weight: bold;
}

.device-id {
  font-size: 0.8em;
  color: #909399;
}

.device-config {
  font-size: 0.9em;
  margin-top: 6px;
}

.group-tag {
  margin-top: 4px;
  margin-right: 4px;
}
</style>
