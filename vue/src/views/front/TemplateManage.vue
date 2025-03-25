<template>
  <div class="template-management">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button icon="el-icon-plus" type="primary" @click="showCreateDialog">新建模板</el-button>
      <el-button :disabled="selectedTemplates.length === 0" icon="el-icon-delete" type="danger"
                 @click="handleBatchDelete">批量删除
      </el-button>
      <el-select v-model="deviceTypeFilter" placeholder="类型" clearable style="width: 150px; margin-left: 10px;">
        <el-option v-for="type in deviceTypes" :key="type.code" :label="type.name" :value="type.code"></el-option>
        <el-option label="其它" value="OTHER"></el-option>
      </el-select>
      <el-input v-model="nameSearch" clearable placeholder="搜索名称(空格分割多关键字)"
                style="width: 300px; margin-left: 10px;"
      ></el-input>
    </div>

    <!-- 模板卡片展示 -->
    <el-row :gutter="20" class="card-row">
      <el-col v-for="template in filteredTemplates" :key="template.id" :lg="6" :md="8" :sm="12" :xs="24">
        <el-card class="template-card" shadow="hover" @click.native="showDetail(template)"
                 :style="{ backgroundColor: template.config.deviceType === 'DATASET' ? '#E6E0F2' : '#f8f9fa' }"
        >
          <!-- 模板基本信息 -->
          <div class="card-content">
            <div class="meta-info">
              <div class="card-header">
                <!-- 多选复选框 -->
                <div class="card-checkbox">
                  <el-checkbox v-model="selectedTemplates" :label="template.id" @click.native.stop>
                    <br>
                  </el-checkbox>
                </div>
                <div class="template-id">ID: {{ template.id }}</div>
              </div>
              <div class="template-name">{{ template.name }}</div>
            </div>
            <el-button class="delete-btn" icon="el-icon-delete" type="text"
                       @click.stop="handleDelete(template.id)">删除模板
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情对话框 -->
    <el-dialog :visible.sync="detailVisible" title="模板详情" width="50%">
      <template v-if="currentTemplate">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板ID">{{ currentTemplate.id }}</el-descriptions-item>
          <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentTemplate.userId }}</el-descriptions-item>

          <template v-if="currentTemplate.config">
            <el-descriptions-item label="存储聚合时间粒度">{{
                currentTemplate.config.aggregationTime
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

            <el-descriptions-item :span="2" label="传感器数据类型配置">
              <el-tag v-for="(type, name) in currentTemplate.config.dataTypes" :key="name"
                      class="data-type-tag">
                {{ name }}: {{ type }}
              </el-tag>
            </el-descriptions-item>
          </template>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 创建模板对话框 -->
    <el-dialog :visible.sync="createVisible" title="创建新模板" width="40%">
      <el-form ref="createForm" :model="newTemplate" label-width="120px">
        <el-form-item :rules="[{ required: true, message: '请输入模板名称', trigger: 'blur' }]" label="模板名称"
                      prop="name" required>
          <el-input v-model="newTemplate.name" placeholder="请输入模板名称"></el-input>
        </el-form-item>
        <!-- 数据类型映射 -->
        <el-form-item label="传感器数据类型映射" required>
          <div v-for="(item, index) in newTemplate.config.dataTypes" :key="index" class="data-type-item">
            <el-input v-model="item.key" placeholder="数据项名称"></el-input>
            <el-select v-model="item.value" placeholder="选择类型">
              <el-option
                  v-for="type in dataTypeOptions"
                  :key="type.code"
                  :label="type.code"
                  :value="type.code">
              </el-option>
            </el-select>
            <el-button circle icon="el-icon-remove" @click.prevent="removeDataType(index)"></el-button>
          </div>
          <el-button icon="el-icon-plus" type="text" @click="addDataType">添加数据类型</el-button>
        </el-form-item>
        <!-- 文件上传，自动识别部分 -->
        <el-form-item label="自动识别类型">
          <el-upload
              :auto-upload="false"
              :on-change="handleCsvUpload"
              :show-file-list="false"
              accept=".csv"
              action=""
          >
            <el-button icon="el-icon-upload" plain type="primary">上传CSV文件</el-button>
            <!--            <span slot="tip" class="upload-tip">（自动识别数据类型）</span>-->
          </el-upload>
        </el-form-item>

        <!-- 聚合时间 -->
        <el-form-item :rules="[{ required: true, message: '请选择聚合时间', trigger: 'change' }]" label="聚合时间"
                      prop="config.aggregationTime">
          <el-select v-model="newTemplate.config.aggregationTime" placeholder="请选择">
            <el-option
                v-for="time in storageAggregationTimes"
                :key="time"
                :label="`${time} 毫秒`"
                :value="time">
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 存储模式 -->
        <el-form-item :rules="{ required: true, message: '请选择存储模式', trigger: 'change' }" label="存储模式"
                      prop="config.storageMode">
          <el-select v-model="newTemplate.config.storageMode" placeholder="请选择">
            <el-option
                v-for="mode in storageModes"
                :key="mode.code"
                :label="`${mode.code} (${mode.name})`"
                :value="mode.code">
            </el-option>
          </el-select>
        </el-form-item>
        <!-- 设备类型 -->
        <el-form-item label="类型">
          <el-select v-model="newTemplate.config.deviceType" placeholder="请选择">
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
  </div>
</template>

<script>
export default {
  name: 'TemplateManagement',
  data() {
    return {
      templates: [],
      selectedTemplates: [],
      detailVisible: false,
      createVisible: false,
      currentTemplate: null,
      newTemplate: {
        name: null,
        config: {
          dataTypes: [{}], // 初始空数据项
          aggregationTime: 1,
          storageMode: 'COVER',
          deviceType: 'DATASET',
        }
      },
      storageModes: [],
      dataTypeOptions: [],
      storageAggregationTimes: [],
      deviceTypes: [],
      nameSearch: '',
      deviceTypeFilter: '',
    }
  },
  computed: {
    currentStorageMode() {
      return this.storageModes.find(m => m.code === this.currentTemplate?.config?.storageMode) || {}
    },
    currentDeviceType() {
      return this.deviceTypes.find(t => t.code === this.currentTemplate?.config?.deviceType) || {}
    },
    filteredTemplates() {
      let filtered = this.templates
      // 按名称筛选
      if (this.nameSearch.trim()) {
        const keywords = this.nameSearch.toLowerCase().split(' ').filter(k => k)
        console.log("[模板搜索]名称搜索keywords:", keywords)
        if (keywords.length) {
          filtered = filtered.filter(t => {
            const name = t.name.toLowerCase()
            return keywords.every(k => name.includes(k))
          })
        }
      }
      // 按设备类型筛选
      if (this.deviceTypeFilter) {
        if (this.deviceTypeFilter === 'OTHER') {
          filtered = filtered.filter(t => !t.config?.deviceType || !this.deviceTypes.some(d => d.code === t.config.deviceType));
        } else {
          filtered = filtered.filter(t => t.config?.deviceType === this.deviceTypeFilter);
        }
      }
      return filtered
    }
  },
  created() {
    this.loadEnums();
    this.fetchTemplates()
  },
  methods: {
    // 获取模板列表
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

    // 显示详情对话框
    showDetail(template) {
      this.currentTemplate = template
      this.detailVisible = true
    },

    // 删除单个模板
    handleDelete(id) {
      this.$confirm('此操作将永久删除该模板，并删除使用此模板的所有设备，以及设备保存的所有数据！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$request.delete(`/template/delete/${id}`)
          if (res.code === '200') {
            this.$message.success('删除成功')
            await this.fetchTemplates()
          } else {
            this.$message.error(res.msg)
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      }).catch(() => {
      })
    },

    // 批量删除模板
    handleBatchDelete() {
      if (this.selectedTemplates.length === 0) return

      this.$confirm(`确定删除选中的 ${this.selectedTemplates.length} 个模板吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const requests = this.selectedTemplates.map(id =>
              this.$request.delete(`/template/delete/${id}`)
          )
          await Promise.all(requests)
          this.$message.success('批量删除成功')
          this.selectedTemplates = []
          await this.fetchTemplates()
        } catch (error) {
          this.$message.error('部分删除失败')
        }
      }).catch(() => {
      })
    },

    addDataType() {
      this.newTemplate.config.dataTypes.push({key: '', value: ''})
      // console.log("newTemplate", JSON.stringify(this.newTemplate));
    },
    removeDataType(index) {
      this.newTemplate.config.dataTypes.splice(index, 1)
      // console.log("newTemplate", JSON.stringify(this.newTemplate));
    },
    // 验证方法
    validateCreate() {
      this.$refs.createForm.validate(valid => {
        if (valid) {
          // 验证数据类型映射
          const invalidItems = this.newTemplate.config.dataTypes.some(item =>
              !item.key || !item.value
          )

          if (this.newTemplate.config.dataTypes.length === 0) {
            this.$message.warning('至少需要添加一个数据类型')
            return
          }

          if (invalidItems) {
            this.$message.warning('请填写完整数据类型配置')
            return
          }

          // 校验传感器名称
          const sensorNamePattern = /^[\u2E80-\u9FFF_A-Za-z@:#{\}$][\u2E80-\u9FFF_A-Za-z0-9@:#{\}$]*$/;

          for (const item of this.newTemplate.config.dataTypes) {
            if (!item.key) {
              this.$message.warning('传感器名称不能为空');
              return;
            }
            if (!sensorNamePattern.test(item.key)) {
              this.$message.warning(`传感器名称 "${item.key}" 不符合命名规则`);
              return;
            }
            if (/^\d/.test(item.key)) {
              this.$message.warning('传感器名称不能以数字开头');
              return;
            }
          }

          this.createTemplate()
        }
      })
    },

    // 创建方法
    async createTemplate() {
      try {
        // 转换数据类型格式为对象
        //传感器名称不能为“tag”
        for (let i = 0; i < this.newTemplate.config.dataTypes.length; i++) {
          const item = this.newTemplate.config.dataTypes[i];
          if (item.key === 'tag') {
            this.$message.warning('传感器名称不能为“tag”');
            return;
          }
        }

        const postData = {
          ...this.newTemplate,
          config: {
            ...this.newTemplate.config,
            dataTypes: this.newTemplate.config.dataTypes.reduce((acc, cur) => {
              if (cur.key && cur.value) acc[cur.key] = cur.value
              return acc
            }, {})
          }
        }

        console.log("postData", JSON.stringify(postData));

        const res = await this.$request.post('/template/add', postData)
        if (res.code === '200') {
          this.$message.success('创建成功')
          this.createVisible = false
          this.resetForm()
          await this.fetchTemplates()
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('创建失败')
      }
    },

    resetForm() {
      this.newTemplate = {
        name: '',
        userId: 1,
        config: {
          dataTypes: [{}],
          aggregationTime: 1000,
          storageMode: 'COMPATIBLE'
        }
      }
    },

    showCreateDialog() {
      this.createVisible = true
    },
    async loadEnums() {
      try {
        const [modesRes, typesRes, timesRes, deviceTypesRes] = await Promise.all([
          this.$request.get('/data/storageModes'),
          this.$request.get('/data/dataTypes'),
          this.$request.get('/data/StorageAggregationTimes'),
          this.$request.get('/data/deviceTypes')
        ]);

        this.storageModes = modesRes.data || [];
        this.dataTypeOptions = typesRes.data || [];
        this.storageAggregationTimes = timesRes.data || [];
        this.deviceTypes = deviceTypesRes.data || [];

        console.log("storageModes", JSON.stringify(this.storageModes));
        console.log("dataTypeOptions", JSON.stringify(this.dataTypeOptions));
        console.log("storageAggregationTimes", JSON.stringify(this.storageAggregationTimes));
        console.log("deviceTypes", JSON.stringify(this.deviceTypes));

      } catch (error) {
        this.$message.error('配置项加载失败');
      }
    },


    // CSV文件处理方法
    async handleCsvUpload(file) {
      try {
        const content = await this.readFile(file.raw);
        const {headers, dataRow} = this.parseCsv(content);
        console.log("headers", JSON.stringify(headers));
        console.log("dataRow", JSON.stringify(dataRow));

        // 识别数据类型
        const dataTypes = headers.map((header, index) => {
          const value = dataRow[index];
          return {
            key: header,
            value: this.detectDataType(value)
          };
        });

        // 更新表单数据
        this.newTemplate.config.dataTypes = dataTypes;

        this.$message.success(`成功识别 ${dataTypes.length} 个传感器`);
      } catch (error) {
        this.$message.error(error.message || '文件处理失败');
      }
    },

    // 文件读取方法
    readFile(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = e => resolve(e.target.result);
        reader.onerror = () => reject(new Error('文件读取失败'));
        reader.readAsText(file);
      });
    },

    // CSV解析方法
    parseCsv(content) {
      const lines = content.split('\n')
          .map(line => line.trim())
      if (lines.length < 3) {
        this.$message.error('CSV文件需要包含表头和数据行(首行不包括)');
      }

      return {
        headers: lines[1].split(',').map(h => h.trim()).slice(1),
        dataRow: lines[2].split(',').map(d => d.trim()).slice(1)
      };
    },

    // 数据类型检测方法
    detectDataType(value) {
      if (/^-?\d+$/.test(value)) return 'INT';
      if (/^-?\d+\.\d+$/.test(value)) return 'FLOAT';
      // if (/^(true|false)$/i.test(value)) return 'BOOL';
      // return 'STRING';
    },
  },


}
</script>

<style scoped>
.template-management {
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

.template-card {
  position: relative;
  margin: 20px;
  cursor: pointer;
  transition: transform 0.2s;
  background: #f8f9fa;
  border-radius: 1.5rem;
  height: 20vh;
  overflow: auto;
}

.template-card:hover {
  transform: translateY(-3px);
}

.card-header {
  display: flex;
  justify-content: left;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.card-checkbox {
  top: 10px;
  left: 10px;
  z-index: 1;
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 30px;
}

.meta-info {
  flex: 1;
}

.template-id {
  font-size: 12px;
  color: #909399;
}

.template-name {
  margin-top: 8px;
}

.delete-btn {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 8px;
  color: #f56c6c;
}

.data-type-tag {
  margin: 2px 4px;
}

.data-type-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
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