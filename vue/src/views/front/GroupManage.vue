<template>
  <div class="group-management">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button icon="el-icon-plus" type="primary" @click="showCreateDialog">新建分组</el-button>
      <el-button :disabled="selectedGroups.length === 0" icon="el-icon-delete" type="danger"
                 @click="handleBatchDelete">批量删除
      </el-button>
      <el-input v-model="nameSearch" clearable placeholder="搜索名称(空格分割多关键字)"
                style="width: 300px; margin-left: 10px;"
      ></el-input>
    </div>

    <!-- 分组卡片展示 -->
    <el-row :gutter="20" class="card-row">
      <el-col v-for="group in filteredGroups" :key="group.id" :lg="6" :md="8" :sm="12" :xs="24">
        <el-card class="group-card" shadow="hover" @click.native="showDetail(group)">
          <div class="card-content">
            <div class="meta-info">
              <div class="card-header">
                <!-- 多选复选框 -->
                <div class="card-checkbox">
                  <el-checkbox v-model="selectedGroups" :label="group.id" @click.native.stop><br></el-checkbox>
                </div>
                <div class="group-id">ID: {{ group.id }}</div>
              </div>
              <div class="group-name">{{ group.name }}</div>
              <div class="group-desc">{{ group.description || '暂无描述' }}</div>
              <div class="device-count">设备数量: {{ group.deviceIds?.length || 0 }}</div>
            </div>
            <el-button class="clear-btn" icon="el-icon-delete" type="text"
                       @click.stop="handleClear(group.id)">清空组内设备
            </el-button>
            <el-button class="delete-btn" icon="el-icon-delete" type="text"
                       @click.stop="handleDelete(group.id)">删除设备组
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建分组对话框 -->
    <el-dialog :visible.sync="createVisible" title="新建分组" width="40%">
      <el-form ref="createForm" :model="newGroup" label-width="80px">
        <el-form-item label="组名称" prop="name" required>
          <el-input v-model="newGroup.name" placeholder="请输入组名称"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newGroup.description" placeholder="请输入组描述" type="textarea"></el-input>
        </el-form-item>
        <el-form-item label="添加设备">
          <el-select
              v-model="selectedDevices"
              filterable
              multiple
              placeholder="请选择设备"
              @change="handleDeviceSelect"
          >
            <el-option
                v-for="device in allDevices"
                :key="device.id"
                :label="device.name"
                :value="device.id">
            </el-option>
          </el-select>
          <div v-if="selectedDevices.length > 0" class="selected-devices">
            <div class="selected-title">已选设备：</div>
            <div class="device-previews">
              <device-card-mini
                  v-for="deviceId in selectedDevices"
                  :key="deviceId"
                  :all-groups="groups"
                  :device="getDeviceById(deviceId)"
                  :show-data-types="true"
                  class="preview-item"/>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createGroup">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 分组详情对话框 -->
    <el-dialog :visible.sync="detailVisible" title="分组详情" width="60%">
      <template v-if="currentGroup">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="组名称">
            {{ currentGroup.name }}
            <el-button icon="el-icon-edit" type="text" @click="showEditDialog('name')"></el-button>
          </el-descriptions-item>
          <el-descriptions-item label="组描述">
            {{ currentGroup.description || '暂无描述' }}
            <el-button icon="el-icon-edit" type="text" @click="showEditDialog('description')"></el-button>
          </el-descriptions-item>
          <el-descriptions-item label="包含设备">
            <div class="group-devices">
              <el-tag
                  v-for="deviceId in currentGroup.deviceIds"
                  :key="deviceId"
                  closable
                  @close="removeDevice(deviceId)">
                {{ getDeviceName(deviceId) }}
              </el-tag>
              <el-button
                  icon="el-icon-plus"
                  type="text"
                  @click="showAddDeviceDialog">
                添加设备
              </el-button>
            </div>
            <div class="device-previews">
              <device-card-mini
                  v-for="deviceId in currentGroup.deviceIds"
                  :key="deviceId"
                  :all-groups="groups"
                  :device="getDeviceById(deviceId)"
                  :show-data-types="true"
                  class="preview-item"/>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 编辑名称/描述对话框 -->
    <el-dialog :title="editType === 'name' ? '修改组名' : '修改描述'" :visible.sync="editVisible" width="40%">
      <el-form>
        <el-form-item :label="editType === 'name' ? '新名称' : '新描述'">
          <el-input v-model="editContent" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="updateGroupInfo">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 添加设备对话框 -->
    <el-dialog :visible.sync="addDeviceVisible" title="添加设备到组" width="50%">
      <el-select
          v-model="newDevices"
          filterable
          multiple
          placeholder="请选择设备"
          @change="handleNewDeviceSelect"
      >
        <el-option
            v-for="device in allDevices"
            :key="device.id"
            :disabled="isDeviceInGroup(device.id)"
            :label="device.name"
            :value="device.id">
        </el-option>
      </el-select>
      <div v-if="newDevices.length > 0" class="selected-devices">
        <div class="selected-title">即将添加的设备：</div>
        <div class="device-previews">
          <device-card-mini
              v-for="deviceId in newDevices"
              :key="deviceId"
              :all-groups="groups"
              :device="getDeviceById(deviceId)"
              :show-data-types="true"
              class="preview-item"/>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="addDeviceVisible = false">取消</el-button>
        <el-button type="primary" @click="addDevicesToGroup">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import DeviceCardMini from '@/views/front/module/DeviceCardMini'
import deviceMixin from "@/mixins/device";
import groupMixin from "@/mixins/group";

export default {
  name: 'GroupManagement',
  components: {
    DeviceCardMini
  },
  mixins: [deviceMixin, groupMixin],
  data() {
    return {
      createVisible: false,
      detailVisible: false,
      editVisible: false,
      addDeviceVisible: false,
      currentGroup: null,
      newGroup: {
        name: '',
        description: '',
        deviceIds: []
      },
      selectedDevices: [],
      newDevices: [],
      editType: 'name',
      editContent: '',

      selectedGroups: [],
      nameSearch: '',
    }
  },
  computed: {
    filteredGroups() {
      let filtered = this.groups
      if (this.nameSearch.trim()) {
        const keywords = this.nameSearch.toLowerCase().split(' ').filter(k => k)
        console.log("[组搜索]名称搜索keywords:", keywords)
        if (keywords.length) {
          filtered = filtered.filter(g => {
            const name = g.name.toLowerCase()
            return keywords.every(k => name.includes(k))
          })
        }
      }
      return filtered
    }
  },
  created() {
  },
  methods: {
    handleDeviceSelect() {
      this.newGroup.deviceIds = this.selectedDevices
    },

    handleNewDeviceSelect() {
      // console.log("select", JSON.stringify(this.currentGroup))
      this.newDevices = this.newDevices.filter(d =>
          !this.currentGroup.deviceIds.includes(d)
      )
    },

    isDeviceInGroup(deviceId) {
      // console.log("isDeviceInGroup", JSON.stringify(this.currentGroup))
      return this.currentGroup && this.currentGroup.deviceIds.includes(deviceId)
    },

    showCreateDialog() {
      this.createVisible = true
      this.newGroup = {name: '', description: '', deviceIds: []}
      this.selectedDevices = []
    },

    async createGroup() {
      try {
        const res = await this.$request.post('/group/add', {
          ...this.newGroup,
          deviceIds: this.selectedDevices
        })
        if (res.code === '200') {
          this.createVisible = false
          this.$message.success('创建成功')
          await this.fetchAllGroups()
        }
      } catch (error) {
        this.$message.error('创建失败')
      }
    },

    showDetail(group) {
      this.currentGroup = group
      this.detailVisible = true
    },

    showEditDialog(type) {
      this.editType = type
      this.editContent = type === 'name'
          ? this.currentGroup.name
          : this.currentGroup.description
      this.editVisible = true
    },

    async updateGroupInfo() {
      try {
        const params = {[this.editType === 'name' ? 'name' : 'desc']: this.editContent}
        const res = await this.$request.post(
            `/group/update${this.editType === 'name' ? 'Name' : 'Desc'}/${this.currentGroup.id}`,
            null,
            {params}
        )
        if (res.code === '200') {
          this.currentGroup[this.editType] = this.editContent
          this.editVisible = false
          this.$message.success('修改成功')
        }
      } catch (error) {
        this.$message.error('修改失败')
      }
    },

    showAddDeviceDialog() {
      this.newDevices = []
      this.addDeviceVisible = true
    },

    async addDevicesToGroup() {
      try {
        const res = await this.$request.post(
            `/group/addDevices/${this.currentGroup.id}`,
            this.newDevices
        )
        if (res.code === '200') {
          this.currentGroup.deviceIds.push(...this.newDevices)
          this.addDeviceVisible = false
          this.$message.success('添加成功')
        }
      } catch (error) {
        this.$message.error('添加失败')
      }
    },

    async removeDevice(deviceId) {
      try {
        const res = await this.$request.post(
            `/group/removeDevices/${this.currentGroup.id}`,
            [deviceId]
        )
        if (res.code === '200') {
          this.currentGroup.deviceIds = this.currentGroup.deviceIds.filter(id => id !== deviceId)
          this.$message.success('移除成功')
        }
      } catch (error) {
        this.$message.error('移除失败')
      }
    },

    handleDelete(groupId) {
      this.$confirm('确定删除该分组吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$request.post(`/group/delete/${groupId}`)
          if (res.code === '200') {
            this.groups = this.groups.filter(g => g.id !== groupId)
            this.$message.success('删除成功')
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      }).catch(() => {
        this.$message.info('取消删除')
      })
    },

    handleClear(groupId) {
      this.$confirm('确定清空该设备组内的所有设备吗？(不影响设备)', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        const group = this.groups.find(g => g.id === groupId);
        if (!group || !group.deviceIds.length) {
          this.$message.warning('该组内没有设备，无需清空');
          return;
        }

        // 调用已有的移除设备接口，一次性移除所有设备
        const res = await this.$request.post(
            `/group/removeDevices/${groupId}`,
            group.deviceIds
        );

        if (res.code === '200') {
          group.deviceIds = []; // 清空前端数据
          this.$message.success('清空成功');
        } else {
          this.$message.error(res.message || '清空失败');
        }
      }).catch(() => {
        this.$message.info('取消清空')
      })
    },

    getDeviceById(deviceId) {
      return this.allDevices.find(d => d.id === deviceId)
    },

    getDeviceName(deviceId) {
      const device = this.getDeviceById(deviceId)
      return device ? device.name : '未知设备'
    },
    async handleBatchDelete() {
      if (this.selectedGroups.length === 0) return

      this.$confirm(`确定删除选中的 ${this.selectedGroups.length} 个分组吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const requests = this.selectedGroups.map(id =>
              this.$request.post(`/group/delete/${id}`)
          )
          await Promise.all(requests)
          this.$message.success('批量删除成功')
          this.selectedGroups = []
          await this.fetchAllGroups()
        } catch (error) {
          this.$message.error('部分删除失败')
        }
      }).catch(() => {
      })
    },
  }
}
</script>

<style scoped>
.group-management {
  padding: 20px;
  font-weight: bold;
  height: 100%;
  overflow: hidden; /* 关键样式 */
}


.card-row {
  /*margin: 0 -10px;*/
  background: #fff;
  height: 90%;
  border-radius: 1rem;
  /*box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);*/
  margin-top: 16px;
  overflow: auto;
}

.group-card {
  position: relative;
  margin: 20px;
  cursor: pointer;
  transition: transform 0.2s;
  background: #f8f9fa;
  border-radius: 1.5rem;
  height: 20vh;
  overflow: auto;
}

.group-card:hover {
  transform: translateY(-3px);
}

/*.card-content {*/
/*  display: flex;*/
/*  justify-content: space-between;*/
/*}*/

.meta-info {
  flex: 1;
}

.group-name {
  font-size: 16px;
  font-weight: bold;
}

.group-desc {
  color: #666;
  margin: 8px 0;
  font-size: 14px;
}

.device-count {
  color: #999;
  font-size: 12px;
}

.clear-btn {
  color: #f1b70a;
}

.delete-btn {
  /*position: absolute;*/
  /*right: 10px;*/
  /*bottom: 10px;*/
  /*padding: 8px;*/
  color: #f56c6c;
}

.device-option {
  padding: 8px;
}

.selected-devices {
  margin-top: 10px;
}

.group-id {
  font-size: 12px;
  color: #909399;
}

.selected-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.device-previews {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-item {
  width: 200px;
}

.group-devices {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header {
  display: flex;
  justify-content: left;
  align-items: center;
  border-bottom: 1px solid #eee;
}

::v-deep .el-descriptions-item__label {
  font-weight: bold !important;
  color: #303133 !important;
}

::v-deep .el-descriptions-item__content {
  font-weight: bold !important;
}

::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}

::v-deep .el-button {
  font-weight: bold !important;
}
</style>