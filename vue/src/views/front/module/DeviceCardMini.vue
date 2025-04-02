<template>
  <div class="device-card-mini"
       :style="{ backgroundColor: device.config.deviceType === 'DATASET' ? '#f0ecf7' : '#f8f9fa' }"
  >
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
      <div v-if="device.groupIds && device.groupIds.length > 0">
        所属组：
        <el-tag v-for="groupName in groupNames " :key="groupName" class="group-tag">
          {{ groupName }}
        </el-tag>
      </div>
      <div v-else>所属组：不属于任何组</div>
      <div v-if="device.tags && device.tags.length > 0">
        标签：
        <el-tag v-for="tag in device.tags" :key="tag" class="group-tag">
          {{ tag }}
        </el-tag>
      </div>
      <div v-else>标签：无标签</div>
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
    allGroups: {
      type: Array,
      required: false
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
    },
    groupNames() {
      if (this.allGroups && this.allGroups.length > 0) {
        return this.allGroups.filter(group => this.device.groupIds.includes(group.id)).map(group => group.name);
      }
      return [];
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
