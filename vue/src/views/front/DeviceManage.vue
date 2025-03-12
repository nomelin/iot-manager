<template>
  <div class="device-management">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button icon="el-icon-plus" type="primary" @click="showCreateDialog">新建设备</el-button>
      <!--      <el-button :disabled="selectedDevices.length === 0" icon="el-icon-delete" type="danger"-->
      <!--                 @click="handleBatchDelete">批量删除-->
      <!--      </el-button>-->
    </div>

    <!-- 设备卡片展示 -->
    <el-row :gutter="20" class="card-row">
      <el-col v-for="device in devices" :key="device.id" :lg="6" :md="8" :sm="12" :xs="24">
        <el-card class="device-card" shadow="hover" @click.native="showDetail(device)">
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
              <!--              <div class="device-groups">-->
              <!--                <el-tag v-for="(group, index) in device.groupIds" :key="index" class="tag-item" effect="dark"-->
              <!--                        size="medium" type="info">-->
              <!--                  {{ group }}-->
              <!--                </el-tag>-->
              <!--              </div>-->
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
          <el-select v-model="selectedTemplateId" @change="handleTemplateSelect">
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
  </div>
</template>

<script>
import TemplateCardMini from './module/TemplateCardMini.vue'
import GroupCardMini from "@/views/front/module/GroupCardMini";

export default {
  name: 'DeviceManagement',
  components: {
    TemplateCardMini,
    GroupCardMini
  },
  data() {
    return {
      devices: [],
      templates: [],//当前用户的模板列表
      groups: [],//当前用户的组列表
      selectedDevices: [],
      selectedTemplateId: null,
      detailVisible: false,
      createVisible: false,
      nameEditVisible: false,
      currentDevice: null,
      currentTemplate: null,
      newDevice: {
        name: '',
        tags: [],
        config: {}
      },
      inputTag: '',
      newTag: '',
      newName: '',
      storageModes: []
    }
  },
  computed: {
    // 当前选中的模板
    selectedTemplate() {
      return this.templates.find(t => t.id === this.selectedTemplateId)
    },
    // 设备详情的组列表
    currentGroups() {
      return this.groups.filter(g => this.currentDevice.groupIds.includes(g.id))
    },
    //设备详情的存储模式
    currentStorageMode() {
      return this.storageModes.find(m => m.code === this.currentDevice?.config?.storageMode) || {}
    }
  },
  created() {
    this.fetchDevices()
    this.fetchTemplates()
    this.fetchGroups()
    this.loadStorageModes()
  },
  methods: {
    async fetchDevices() {
      try {
        const res = await this.$request.get('/device/all')
        if (res.code === '200') {
          this.devices = res.data
          for (const device of this.devices) {
            device.tags = device.tags || []//防止tags为null
          }
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取设备列表失败')
      }
    },

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

    async fetchGroups() {
      try {
        const res = await this.$request.get('/group/all')
        if (res.code === '200') {
          this.groups = res.data
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取分组列表失败')
      }
    },

    handleTemplateSelect(templateId) {
      //do nothing
    },
    handleStorageModeSelect(storageMode) {
      // this.newDevice.config.storageMode = storageMode
    },

    showCreateDialog() {
      this.createVisible = true
      this.newDevice = {name: '', tags: [], config: {}}
      this.selectedTemplateId = null
      this.inputTag = ''
    },

    validateCreate() {
      this.$refs.createForm.validate((valid) => {
        if (valid) {
          //校验参数
          if (this.selectedTemplateId === null) {
            this.$message.error('请选择模板')
            return
          }
          this.createDevice()
        }
      })
    },
    async createDevice() {
      console.log("newDevice", JSON.stringify(this.newDevice))
      console.log("selectedTemplateId", this.selectedTemplateId)
      try {
        const res = await this.$request.post('/device/add', {
          device: this.newDevice,
          templateId: this.selectedTemplateId
        })
        if (res.code === '200') {
          this.createVisible = false
          this.$message.success('创建设备成功')
          await this.fetchDevices()
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('创建设备失败')
      }
    },

    //创建设备时，添加标签
    handleNewTagAdd() {
      if (this.inputTag && !this.newDevice.tags.includes(this.inputTag)) {
        this.newDevice.tags.push(this.inputTag)
        this.inputTag = ''
      }
    },

    //创建设备时，关闭标签
    handleNewTagClose(index) {
      this.newDevice.tags.splice(index, 1)
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

    async loadStorageModes() {
      const res = await this.$request.get('/data/storageModes')
      if (res.code !== '200') {
        this.$message.error('获取存储模式失败')
        return
      }
      this.storageModes = res.data || []
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
              this.fetchDevices()
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
              // this.fetchDevices()
            }).catch(() => {
          this.$message.error('清空设备数据失败')
        })
      }).catch((e) => {
        console.log(e)
        this.$message.info('取消清空设备数据')
      })
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
  margin: 20px;
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