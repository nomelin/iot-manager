<template>
  <div class="group-card-mini">
    <div class="group-name">{{ group.name }}</div>
    <div class="group-id">ID: {{ group.id }}</div>
    <div class="group-description">{{ group.description }}</div>
    <div class="group-device-count">
      设备数量: {{ groupDeviceCount }}
    </div>
  </div>
</template>

<script>
export default {
  props: {
    group: {
      type: Object,
      required: true
    }
  },
  computed: {
    groupDevices() {
      // 获取设备ID列表
      return this.group.deviceIds || [];
    },
    groupDeviceCount() {
      // 计算设备数量
      return this.groupDevices.length;
    }
  }
}
</script>

<style scoped>
.group-card-mini {
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 1rem;
  margin-top: 8px;
  width: auto;
}

.group-name {
  font-weight: bold;
  font-size: 1.1em;
}

.group-id {
  font-size: 0.8em;
  color: #909399;
}

.group-description {
  font-size: 0.9em;
  color: #606266;
  margin-top: 4px;
}

.group-device-count {
  margin-top: 6px;
  font-size: 0.9em;
  color: #409EFF;
}
</style>
