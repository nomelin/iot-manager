<template>
  <div class="alert-management">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button icon="el-icon-plus" type="primary" @click="showCreateDialog">新建告警规则</el-button>
      <el-button
          :disabled="selectedAlerts.length === 0"
          icon="el-icon-delete"
          type="danger"
          @click="handleBatchDelete"
      >批量删除
      </el-button>
      <el-input
          v-model="nameSearch"
          clearable
          placeholder="搜索名称"
          style="width: 300px; margin-left: 10px;"
      ></el-input>
    </div>

    <!-- 告警规则表格 -->
    <el-table
        :data="filteredAlerts"
        height="calc(100vh - 160px)"
        style="width: 100%"
        @row-click="showDetail"
        @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column label="ID" prop="id" width="80"></el-table-column>
      <el-table-column label="名称" prop="name"></el-table-column>
      <el-table-column label="描述" prop="description"></el-table-column>
      <el-table-column label="设备ID" prop="deviceId" width="100">
        <template #default="{row}">
          {{ row.deviceId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="组ID" prop="groupId" width="100">
        <template #default="{row}">
          {{ row.groupId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{row}">
          <el-tag :type="row.enable ? 'success' : 'danger'" size="small">
            {{ row.enable ? '已启用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button
              :type="row.enable ? 'danger' : 'success'"
              size="mini"
              @click.stop="toggleStatus(row)"
          >
            {{ row.enable ? '停用' : '启用' }}
          </el-button>
          <el-button
              icon="el-icon-delete"
              size="mini"
              type="danger"
              @click.stop="handleDelete(row.id)"
          ></el-button>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createdTime" width="180">
        <template #default="{ row }">
          {{ row.createdTime | formatTime }}
        </template>
      </el-table-column>

      <el-table-column label="最后修改" prop="updatedTime" width="180">
        <template #default="{ row }">
          {{ row.updatedTime | formatTime }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑对话框 -->
    <el-dialog
        :title="dialogType === 'create' ? '新建告警规则' : '编辑告警规则'"
        :visible.sync="dialogVisible"
        width="50%"
    >
      <el-form ref="alertForm" :model="currentAlert" label-width="150px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="名称" prop="name" required>
          <el-input v-model="currentAlert.name"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentAlert.description" type="textarea"></el-input>
        </el-form-item>
        <el-form-item label="设备ID">
          <el-select v-model="currentAlert.deviceId" clearable>
            <el-option
                v-for="device in allDevices"
                :key="device.id"
                :label="device.name"
                :value="device.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="组ID">
          <el-select v-model="currentAlert.groupId" clearable>
            <el-option
                v-for="group in allGroups"
                :key="group.id"
                :label="group.name"
                :value="group.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="通知用户">
          <el-select v-model="currentAlert.actionConfig.notifyUsers" disabled multiple>
            <el-option
                v-for="u in [user]"
                :key="u.id"
                :label="u.name"
                :value="u.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-divider content-position="left">触发条件配置</el-divider>
        <el-form-item label="监测指标" prop="conditionConfig.metric" required>
          <el-input v-model="currentAlert.conditionConfig.metric"></el-input>
        </el-form-item>
        <el-form-item label="持续时长(S)">
          <el-switch v-model="enableDuration" active-text="立即" inactive-text="持续"></el-switch>
          <el-input-number
              v-if="!enableDuration"
              v-model="currentAlert.conditionConfig.duration"
              :min="1">
          </el-input-number>
        </el-form-item>
        <el-form-item label="阈值范围">
          <div class="threshold-inputs">
            <el-input-number
                v-model="currentAlert.conditionConfig.minValue"
                :precision="2"
                placeholder="最小值"
            ></el-input-number>
            <span class="threshold-separator">-</span>
            <el-input-number
                v-model="currentAlert.conditionConfig.maxValue"
                :precision="2"
                placeholder="最大值"
            ></el-input-number>
          </div>
        </el-form-item>

        <el-divider content-position="left">触发动作配置</el-divider>
        <el-form-item label="通知渠道" required>
          <el-checkbox-group v-model="currentAlert.actionConfig.channels">
            <el-checkbox
                v-for="channel in alertChannels"
                :key="channel"
                :label="channel"
            >{{ channel }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="消息类型" required>
          <el-select v-model="currentAlert.actionConfig.messageType">
            <el-option
                v-for="type in messageTypes"
                :key="type"
                :label="type"
                :value="type"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="额外信息">
          <el-input
              v-model="currentAlert.actionConfig.extraMessage"
              type="textarea"
          ></el-input>
        </el-form-item>
        <el-form-item label="静默时间(S)">
          <el-switch v-model="enableSilent" active-text="不静默" inactive-text="静默"></el-switch>
          <el-input-number
              v-if="!enableSilent"
              v-model="currentAlert.actionConfig.silentDuration"
              :min="1">
          </el-input-number>
        </el-form-item>
        <el-form-item v-if="this.dialogType === 'edit'" label="创建时间">
            {{ currentAlert.createdTime | formatTime }}
        </el-form-item>
        <el-form-item v-if="this.dialogType === 'edit'" label="最后修改">
          {{ currentAlert.updatedTime | formatTime }}
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAlert">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>

import deviceMixin from "@/mixins/device";
import groupMixin from "@/mixins/group";
import {getItemWithExpiry} from "@/App"


export default {
  name: 'AlertManagement',
  mixins: [deviceMixin, groupMixin],
  data() {
    return {
      user: getItemWithExpiry("user"),
      alerts: [],
      // allDevices: [],
      // allGroups: [],
      dialogVisible: false,
      dialogType: 'create',
      currentAlert: this.getEmptyAlert(),
      selectedAlerts: [],
      nameSearch: '',

      // 枚举选项
      alertChannels: ['IN_MSG', 'WECHAT_PP'],
      messageTypes: ['NOTICE', 'WARNING', 'ERROR'],
      enableDuration: true,  // 控制持续时间开关
      enableSilent: false,    // 控制静默时间开关
    }
  },
  computed: {
    filteredAlerts() {
      if (!this.nameSearch) return this.alerts
      return this.alerts.filter(a =>
          a.name.toLowerCase().includes(this.nameSearch.toLowerCase())
      )
    }
  },
  created() {
    this.fetchAlerts()
  },
  methods: {
    getEmptyAlert() {
      // console.log("this.user",JSON.stringify(this.user))
      let user=getItemWithExpiry("user");
      this.user=user;
      return {
        id: null,
        name: '默认告警规则',
        description: '默认告警规则描述',
        deviceId: null,
        groupId: null,
        enable: true,
        conditionConfig: {
          duration: 1,
          metric: '',
          minValue: null,
          maxValue: null
        },
        actionConfig: {
          notifyUsers: [user.id],
          channels: [],
          messageType: 'NOTICE',
          extraMessage: '无额外信息',
          silentDuration: 60
        }
      }
    },

    async fetchAlerts() {
      try {
        const res = await this.$request.get('/alert/all')
        if (res.code === '200') {
          this.alerts = res.data
        }
      } catch (error) {
        this.$message.error('获取告警列表失败')
      }
    },
    showCreateDialog() {
      this.currentAlert = this.getEmptyAlert()
      this.dialogType = 'create'
      this.dialogVisible = true
    },

    showDetail(row) {
      this.currentAlert = {...row}
      this.dialogType = 'edit'
      this.dialogVisible = true
    },

    async submitAlert() {
      try {
        const url = this.dialogType === 'create' ? '/alert/add' : '/alert/update'
        const res = await this.$request.post(url, this.currentAlert)
        if (res.code === '200') {
          this.$message.success('操作成功')
          this.dialogVisible = false
          await this.fetchAlerts()
        }
      } catch (error) {
        this.$message.error('操作失败')
      }
    },

    toggleStatus(row) {
      const action = row.enable ? 'disable' : 'enable'
      this.$request.post(`/alert/${action}/${row.id}`)
          .then(() => {
            row.enable = !row.enable
            this.$message.success('状态已更新')
          })
    },

    handleDelete(id) {
      this.$confirm('确定删除该告警规则吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await this.$request.post(`/alert/delete/${id}`)
        await this.fetchAlerts()
        this.$message.success('删除成功')
      })
    },

    handleBatchDelete() {
      console.log("handleBatchDelete", JSON.stringify(this.selectedAlerts))
      if (this.selectedAlerts.length === 0) return

      this.$confirm(`确定删除选中的 ${this.selectedAlerts.length} 个告警规则吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const requests = this.selectedAlerts.map(id =>
              this.$request.post(`/alert/delete/${id}`)
          )
          await Promise.all(requests)
          this.$message.success('批量删除成功')
          this.selectedAlerts = []
          await this.fetchAlerts()
        } catch (error) {
          this.$message.error('部分删除失败')
        }
      }).catch(() => {
      })
    },
    handleSelectionChange(selection) {
      this.selectedAlerts = selection.map(item => item.id);
      // console.log("selection changed",JSON.stringify(selection))
    },
  }
}
</script>

<style scoped>
.alert-management {
  padding: 20px;
  font-weight: bold;
}

.operation-bar {
  margin-bottom: 20px;
}

.threshold-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.threshold-separator {
  margin: 0 10px;
}

::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}

::v-deep .el-table {
  border-radius: 1rem;
  overflow: hidden;
}

.el-table >>> .el-table__row {
  cursor: pointer;
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