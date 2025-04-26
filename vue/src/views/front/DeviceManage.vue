<template>
  <div class="device-management">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button icon="el-icon-plus" type="primary" @click="showCreateDialog(null)">新建设备</el-button>
      <el-button :disabled="selectedDevices.length === 0" icon="el-icon-delete" type="danger"
                 @click="handleBatchDelete">批量删除
      </el-button>
      <el-button icon="el-icon-check" type="primary" @click="handleSelectAll">全选本页</el-button>
      <el-select v-model="deviceTypeFilter" clearable placeholder="类型" style="width: 100px; margin-left: 10px;">
        <el-option v-for="type in deviceTypes" :key="type.code" :label="type.name" :value="type.code"></el-option>
        <el-option label="其它" value="OTHER"></el-option>
      </el-select>
      <el-select v-model="selectedGroup" clearable filterable placeholder="选择组"
                 style="margin-left: 10px;width: 150px;">
        <el-option
            v-for="group in allGroups"
            :key="group.id"
            :label="group.name"
            :value="group.id">
        </el-option>
      </el-select>
      <el-button type="primary" @click="handleAddToGroup">添加到组</el-button>

      <el-input v-model="nameSearch" clearable placeholder="搜索名称(空格分割多关键字)"
                style="width: 200px; margin-left: 10px;"
      ></el-input>
      <el-tooltip content="搜索范围包括设备id，名称，标签；组id，名称；模板名称；传感器名称" placement="bottom">
        <el-input v-model="fullSearch" clearable
                  placeholder="全文搜索(空格分割多关键字)" style="width: 300px; margin-left: 10px;"></el-input>
      </el-tooltip>
      <el-button icon="el-icon-search" type="primary" @click="handleClearSearch">清空</el-button>
      <el-button
          style="margin-left: 10px;"
          type="primary"
          @click="toggleLayout"
      >
        {{ layoutButtonText }}
      </el-button>
      <el-button
          :icon="layoutButtonIcon"
          style="margin-left: 5px;"
          type="primary"
          @click="toggleLayout2"
      ></el-button>
    </div>

    <!-- 设备卡片展示 -->
    <el-row v-if="!isTableLayout" :gutter="0" class="card-row">
      <el-col v-for="device in filteredDevices" :key="device.id"
              :lg="currentLayout.lg"
              :md="currentLayout.md"
              :sm="currentLayout.sm"
              :xs="currentLayout.xs">
        <el-card :style="{ backgroundColor: device.config.deviceType === 'DATASET' ? '#f0ecf7' : '#f8f9fa' }"
                 class="device-card" shadow="hover"
                 @click.native="showDetail(device)"
        >
          <!-- 设备基本信息 -->
          <div class="card-content">
            <div class="meta-info">
              <div class="card-header">
                <!-- 多选复选框 -->
                <div class="card-checkbox">
                  <el-checkbox v-model="selectedDevices" :label="device.id" @click.native.stop><br></el-checkbox>
                </div>
                <div class="device-id">ID: {{ device.id }}</div>
              </div>
              <div class="device-name">{{ device.name }}</div>
              <div class="device-tags">
                <el-tag v-for="(tag, index) in device.tags" :key="index" class="tag-item" size="mini">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
            <el-button class="clear-btn" icon="el-icon-delete" type="text"
                       @click.stop="handleClear(device.id)">清空设备数据
            </el-button>
            <el-button class="delete-btn" icon="el-icon-delete" type="text"
                       @click.stop="handleDelete(device.id)">删除设备
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备表格展示 -->
    <el-table
        v-if="isTableLayout"
        :data="filteredDevices"
        :row-class-name="tableRowClassName"
        border
        class="card-row"
        style="width: 100%; margin-top: 20px;"
        height="tableHeight"
        @row-click="handleRowClick"
    >
      <el-table-column label="设备ID" prop="id" width="150"/>
      <el-table-column label="名称" prop="name" width="180"/>
      <el-table-column label="标签">
        <template v-slot="scope">
          <el-tag
              v-for="(tag, index) in scope.row.tags"
              :key="index"
              size="mini"
              style="margin-right: 5px;"
          >
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所属模板">
        <template v-slot="scope">
          {{ scope.row.config.templateName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="所在组">
        <template v-slot="scope">
          {{
            (scope.row.groupIds || []).map(id => {
              const group = allGroups.find(g => g.id === id);
              return group ? group.name : '';
            }).join(', ')
          }}
        </template>
      </el-table-column>
      <el-table-column label="存储聚合粒度(ms)" prop="config.aggregationTime" width="150"/>
      <el-table-column label="存储模式" width="120">
        <template v-slot="scope">
          {{ scope.row.config.storageMode || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="设备类型" width="120">
        <template v-slot="scope">
          {{ scope.row.config.deviceType || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template v-slot="scope">
          <div style="display: flex; gap: 10px;">
            <el-button
                class="clear-btn"
                icon="el-icon-delete"
                size="mini"
                type="text"
                @click.stop="handleClear(scope.row.id)"
            >
              清空
            </el-button>
            <el-button
                class="delete-btn"
                icon="el-icon-delete"
                size="mini"
                type="text"
                @click.stop="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>


    <!-- 详情对话框 -->
    <el-dialog :visible.sync="detailVisible" title="设备详情" width="50%">
      <template v-if="currentDevice">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="设备ID">{{ currentDevice.id }}</el-descriptions-item>
          <el-descriptions-item label="设备名称">
            {{ currentDevice.name }}
            <el-button icon="el-icon-edit" type="text" @click="showNameEditDialog"></el-button>
          </el-descriptions-item>
          <el-descriptions-item label="所属模板">
            <template-card-mini :template="currentTemplate"/>
          </el-descriptions-item>
          <el-descriptions-item label="所在组">
            <!--所在组可以有多个-->
            <div class="device-groups">
              <group-card-mini v-for="(group,index) in currentGroups" :key="index" :group="group" class="preview-item"/>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            <el-tag
                v-for="(tag, index) in currentDevice.tags"
                :key="index"
                closable
                @close="handleTagClose(index)">
              {{ tag }}
            </el-tag>
            <el-input
                v-model="newTag"
                class="input-new-tag"
                placeholder="添加标签"
                size="small"
                @blur="handleTagAdd"
                @keyup.enter.native="handleTagAdd">
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item label="存储聚合时间粒度">{{
              currentDevice.config.aggregationTime
            }}ms
          </el-descriptions-item>
          <el-descriptions-item label="存储模式">{{ currentStorageMode.code }} ({{
              currentStorageMode.name
            }})
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentDeviceType.code }} ({{
              currentDeviceType.name
            }})
          </el-descriptions-item>
          <el-descriptions-item label="传感器数据类型配置">
            <el-tag v-for="(type, name) in currentDevice.config.dataTypes" :key="name"
                    class="data-type-tag">
              {{ name }}: {{ type }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 创建设备对话框 -->
    <el-dialog :visible.sync="createVisible" title="创建设备" width="40%">
      <el-form ref="createForm" :model="newDevice" label-width="120px">
        <el-form-item :rules="[{ required: true, message: '请输入设备名称', trigger: 'blur' }]" label="设备名称"
                      prop="name" required>
          <el-input v-model="newDevice.name" placeholder="请输入设备名称"></el-input>
        </el-form-item>

        <el-form-item label="创建模板" required>
          <el-select v-model="selectedTemplateId" filterable @change="handleTemplateSelect">
            <el-option
                v-for="template in templates"
                :key="template.id"
                :label="template.name"
                :value="template.id">
            </el-option>
          </el-select>
          <div v-if="selectedTemplate" class="template-preview">
            <h4>模板预览：</h4>
            <template-card-mini :show-data-types="true" :template="selectedTemplate"/>
          </div>
        </el-form-item>

        <el-form-item label="设备标签">
          <el-tag
              v-for="(tag, index) in newDevice.tags"
              :key="index"
              closable
              @close="handleNewTagClose(index)">
            {{ tag }}
          </el-tag>
          <el-input
              v-model="inputTag"
              class="input-new-tag"
              placeholder="添加标签"
              size="small"
              @blur="handleNewTagAdd"
              @keyup.enter.native="handleNewTagAdd">
          </el-input>
        </el-form-item>
        <el-form-item label="存储模式">
          <el-select v-model="newDevice.config.storageMode" placeholder="模板配置存储模式"
                     @change="handleStorageModeSelect">
            <el-option
                v-for="storageMode in storageModes"
                :key="storageMode.code"
                :label="storageMode.code + '（' + storageMode.name+ '）'"
                :value="storageMode.code">
            </el-option>
          </el-select>
        </el-form-item>
        <!-- 设备类型 -->
        <el-form-item label="类型">
          <el-select v-model="newDevice.config.deviceType" placeholder="模板配置设备类型">
            <el-option
                v-for="type in deviceTypes"
                :key="type.code"
                :label="`${type.code} (${type.name})`"
                :value="type.code">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="validateCreate">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 修改名称对话框 -->
    <el-dialog :visible.sync="nameEditVisible" title="修改设备名称" width="30%">
      <el-form>
        <el-form-item label="新名称">
          <el-input v-model="newName" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="nameEditVisible = false">取 消</el-button>
        <el-button type="primary" @click="updateDeviceName">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 添加到组对话框 -->
    <el-dialog :visible.sync="addToGroupDialogVisible" title="添加到组" width="40%">
      <el-select v-model="selectedTargetGroup" clearable placeholder="选择目标组">
        <el-option
            v-for="group in allGroups"
            :key="group.id"
            :label="group.name"
            :value="group.id">
        </el-option>
      </el-select>
      <group-card-mini v-if="selectedTargetGroup" :group="selectedGroupObject" style="margin-top: 15px;"/>
      <span slot="footer">
    <el-button @click="addToGroupDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="confirmAddToGroup">确 定</el-button>
  </span>
    </el-dialog>

  </div>

</template>

<script>
import TemplateCardMini from './module/TemplateCardMini.vue'
import GroupCardMini from "@/views/front/module/GroupCardMini";
import deviceCreationMixin from "@/mixins/deviceCreationMixin";
import deviceMixin from "@/mixins/device";
import groupMixin from "@/mixins/group";

export default {
  name: 'DeviceManagement',
  components: {
    TemplateCardMini,
    GroupCardMini
  },
  mixins: [deviceCreationMixin, deviceMixin, groupMixin],
  data() {
    return {
      templates: [],//当前用户的模板列表
      selectedDevices: [],
      detailVisible: false,
      nameEditVisible: false,
      currentDevice: null,
      currentTemplate: null,
      newTag: '',
      newName: '',
      selectedGroup: null,
      nameSearch: '',
      fullSearch: '',
      deviceTypeFilter: '',
      addToGroupDialogVisible: false,
      selectedTargetGroup: null,
      isCompactLayout: true,
      isTableLayout: false, // 是否表格模式
    }
  },
  computed: {
    // 当前设备类型
    currentDeviceType() {
      return this.deviceTypes.find(t => t.code === this.currentDevice?.config?.deviceType) || {}
    },
    // 当前选中的模板
    selectedTemplate() {
      return this.templates.find(t => t.id === this.selectedTemplateId)
    },
    // 设备详情的组列表
    currentGroups() {
      return this.allGroups.filter(g => this.currentDevice.groupIds.includes(g.id))
    },
    //设备详情的存储模式
    currentStorageMode() {
      return this.storageModes.find(m => m.code === this.currentDevice?.config?.storageMode) || {}
    },
    // 筛选后的设备列表
    filteredDevices() {
      let filtered = this.allDevices

      // 组筛选
      if (this.selectedGroup) {
        console.log("[设备搜索]selectedGroup:", this.selectedGroup)
        filtered = filtered.filter(d => d.groupIds.includes(this.selectedGroup))
      }

      // 按设备类型筛选
      if (this.deviceTypeFilter) {
        if (this.deviceTypeFilter === 'OTHER') {
          filtered = filtered.filter(t => !t.config?.deviceType || !this.deviceTypes.some(d => d.code === t.config.deviceType));
        } else {
          filtered = filtered.filter(t => t.config?.deviceType === this.deviceTypeFilter);
        }
      }

      // 名称搜索
      if (this.nameSearch.trim()) {
        const keywords = this.nameSearch.toLowerCase().split(' ').filter(k => k)
        console.log("[设备搜索]名称搜索keywords:", keywords)
        if (keywords.length) {
          filtered = filtered.filter(d => {
            const name = d.name.toLowerCase()
            return keywords.every(k => name.includes(k))
          })
        }
      }

      // 全文搜索
      if (this.fullSearch.trim()) {
        const keywords = this.fullSearch.toLowerCase().split(' ').filter(k => k)
        console.log("[设备搜索]全文搜索keywords:", keywords)
        if (keywords.length) {
          filtered = filtered.filter(d => {
            // 收集所有搜索字段
            const groupNames = this.allGroups
                .filter(g => d.groupIds.includes(g.id))
                .map(g => g.name.toLowerCase())
                .join(' ')

            const template = this.templates.find(t => t.id === d.templateId)
            const templateName = template ? template.name.toLowerCase() : ''
            const tags = (d.tags || []).join(' ').toLowerCase()
            const sensorNames = d.config.dataTypes
                ? Object.keys(d.config.dataTypes).join(' ').toLowerCase()
                : ''

            const searchContent = [
              d.id.toString(),
              d.name.toLowerCase(),
              d.groupIds.join(' '),
              groupNames,
              templateName,
              tags,
              sensorNames
            ].join(' ')

            return keywords.every(k => searchContent.includes(k))
          })
        }
      }

      return filtered
    },
    selectedGroupObject() {
      return this.allGroups.find(g => g.id === this.selectedTargetGroup) || null;
    },
    layoutButtonText() {
      return this.isCompactLayout ? '大' : '小';
    },
    currentLayout() {
      return this.isCompactLayout ?
          {lg: 4, md: 6, sm: 8, xs: 12} :  // 大布局
          {lg: 6, md: 8, sm: 12, xs: 24}; // 紧凑布局
    },
    layoutButtonText2() {
      return this.isTableLayout ? '卡片模式' : '表格模式';
    },
    layoutButtonIcon() {
      return this.isTableLayout ? 'el-icon-menu' : 'el-icon-s-operation';
    },
    tableHeight() {
      return 'calc(100%)';//为了固定表头，同时保证表格高度自适应
    },
  },
  created() {
    this.fetchTemplates()
  },
  methods: {

    async fetchTemplates() {
      try {
        const res = await this.$request.get('/template/all')
        if (res.code === '200') {
          this.templates = res.data
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取模板列表失败')
      }
    },


    handleTemplateSelect(templateId) {
      //do nothing
    },
    handleStorageModeSelect(storageMode) {
      // this.newDevice.config.storageMode = storageMode
    },


    showDetail(device) {
      this.currentDevice = device
      this.currentTemplate = this.templates.find(t => t.id === device.templateId)
      this.detailVisible = true
    },

    showNameEditDialog() {
      this.newName = this.currentDevice.name
      this.nameEditVisible = true
    },

    async updateDeviceName() {
      try {
        const res = await this.$request.post(`/device/updateName/${this.currentDevice.id}`, null, {
          params: {name: this.newName}
        })
        if (res.code === '200') {
          this.currentDevice.name = this.newName
          this.nameEditVisible = false
          this.$message.success('名称修改成功')
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('名称修改失败')
      }
    },

    //在详情页面，添加标签
    handleTagAdd() {
      if (this.newTag && !this.currentDevice.tags.includes(this.newTag)) {
        this.currentDevice.tags.push(this.newTag)
        this.$request.post(`/device/addTags/${this.currentDevice.id}`, [this.newTag])
            .then((res) => {
              if (res.code === '200') {
                this.newTag = ''
              } else {
                this.$message.error(res.msg)
              }
            })
            .catch(() => {
              this.$message.error('标签添加失败')
            })
      }
    },

    //在详情页面，关闭标签
    handleTagClose(index) {
      const removedTag = this.currentDevice.tags.splice(index, 1)
      this.$request.post(`/device/removeTags/${this.currentDevice.id}`, removedTag)
          .then((res) => {
            if (res.code === '200') {
              this.$message.success('标签删除成功')
            } else {
              this.$message.error(res.msg)
            }
          })
          .catch(() => {
            this.$message.error('标签删除失败')
          })
    },

    handleDelete(deviceId) {
      this.$confirm('此操作将永久删除该设备，并删除此设备的所有数据！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        console.log("delete device", deviceId)
        this.$request.post(`/device/delete/${deviceId}`)
            .then((res) => {
              if (res.code === '200') {
                this.$message.success('删除成功')
              } else {
                this.$message.error(res.msg)
              }
              this.fetchAllDevices()
            })
            .catch(() => {
              this.$message.error('删除失败')
            })
      }).catch((e) => {
        console.log(e)
        this.$message.info('取消删除')
      })
    },
    handleClear(deviceId) {
      this.$confirm('此操作将永久清空该设备的所有数据！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        console.log("clear device", deviceId)
        this.$request.post(`/device/clear/${deviceId}`)
            .then((res) => {
              if (res.code === '200') {
                this.$message.success('清空设备数据成功')
              } else {
                this.$message.error(res.msg)
              }
            }).catch(() => {
          this.$message.error('清空设备数据失败')
        })
      }).catch((e) => {
        console.log(e)
        this.$message.info('取消清空设备数据')
      })
    },
    async handleBatchDelete() {
      if (this.selectedDevices.length === 0) return

      this.$confirm(`确定删除选中的 ${this.selectedDevices.length} 个设备吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const requests = this.selectedDevices.map(id =>
              this.$request.post(`/device/delete/${id}`)
          )
          await Promise.all(requests)
          this.$message.success('批量删除成功')
          this.selectedDevices = []
          await this.fetchAllDevices()
        } catch (error) {
          console.log("部分删除失败", error)
          this.$message.error('部分删除失败')
        }
      }).catch(() => {
      })
    },
    handleClearSearch() {
      this.nameSearch = ''
      this.fullSearch = ''
      this.selectedGroup = null
    },
    // 添加到组相关方法
    handleAddToGroup() {
      if (this.selectedDevices.length === 0) {
        this.$message.warning('请先选择要添加的设备');
        return;
      }
      this.addToGroupDialogVisible = true;
    },

    async confirmAddToGroup() {
      if (!this.selectedTargetGroup) {
        this.$message.warning('请选择目标组');
        return;
      }
      try {
        const res = await this.$request.post(
            `/group/addDevices/${this.selectedTargetGroup}`,
            this.selectedDevices
        );
        if (res.code === '200') {
          this.$message.success('添加成功');
          this.addToGroupDialogVisible = false;
          await this.fetchAllDevices(); // 刷新设备列表
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        console.log("添加到组失败", error)
        this.$message.error('添加到组失败');
      }
    },

    // 全选方法
    handleSelectAll() {
      this.selectedDevices = this.filteredDevices.map(d => d.id);
    },
    toggleLayout() {
      this.isCompactLayout = !this.isCompactLayout;
    },
    toggleLayout2() {
      this.isTableLayout = !this.isTableLayout;
    },

    handleRowClick(row) {
      this.showDetail(row);
    },
    tableRowClassName({row}) {
      if (row.config.deviceType === 'DATASET') {
        return 'dataset-row';
      } else {
        return 'normal-row';
      }
    },
  },
  watch: {
    selectedGroup() {
      this.selectedDevices = [];
    },
    deviceTypeFilter() {
      this.selectedDevices = [];
    },
    nameSearch() {
      this.selectedDevices = [];
    },
    fullSearch() {
      this.selectedDevices = [];
    }
  }
}
</script>

<style scoped>
.device-management {
  padding: 20px;
  font-weight: bold;
  height: 100%;
  overflow: hidden; /* 关键样式 */
}

.operation-bar {
  margin-bottom: 20px;
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

.device-card {
  position: relative;
  margin: 10px;
  cursor: pointer;
  transition: transform 0.2s;
  background: #f8f9fa;
  border-radius: 1.5rem;
  height: 20vh;
  overflow: auto;
}

.card-header {
  display: flex;
  justify-content: left;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.device-card:hover {
  transform: translateY(-3px);
}

.device-id {
  font-size: 12px;
  color: #909399;
}

.device-name {
  margin-top: 8px;
}

.device-tags {
  margin-top: 8px;
}

.device-groups {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  margin-right: 4px;
}

.template-preview {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.input-new-tag {
  width: 90px;
  margin-left: 10px;
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

.preview-item {
  width: 200px;
}

.data-type-tag {
  margin: 2px 4px;
}

::v-deep .dataset-row {
  background-color: #f0ecf7;
}

::v-deep .normal-row {
  background-color: #f8f9fa;
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