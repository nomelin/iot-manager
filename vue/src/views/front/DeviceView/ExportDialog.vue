<template>
  <el-dialog
      :visible.sync="visible"
      title="数据导出"
      width="40%"
      @closed="handleClose"
  >
    <div class="export-dialog">
      <el-form label-position="left" >
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportFormat">
            <el-radio label="csv">CSV 合并多文件</el-radio>
            <el-radio label="zip">ZIP 压缩包</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否包含标签列">
          <el-checkbox v-model="needTagColumn"
                       :disabled="this.exportFormat === 'csv'"
          >
          </el-checkbox>
        </el-form-item>
      </el-form>
      <h4>预览结构：</h4>
      <div class="structure-preview">
        <template>
          <pre>{{ structurePreview }}</pre>
        </template>
      </div>
    </div>
    <h4>即将导出的内容：</h4>
    <div class="export-preview">
      <ul>
        <li v-for="device in selectedDevices" :key="device.id">
          <strong>{{ device.name }}</strong>
          <p v-if="exportFormat === 'csv'">标签: {{ formatTagList(device.id) }}</p>
          <p>传感器: {{ getFilteredFields(device.id).join(', ') || '无' }}</p>
        </li>
      </ul>
    </div>

    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button
          :loading="exporting"
          type="primary"
          @click="handleExport"
      >
        {{ exporting ? '导出中...' : '开始导出' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import JSZip from 'jszip'
import {saveAs} from 'file-saver'

export default {
  name: 'ExportDialog',
  props: {
    value: Boolean,
    selectedDeviceIds: {
      type: Array,
      default: () => []
    },
    devices: {
      type: Array,
      default: () => []
    },
    selectedTags: {
      type: Object,
      default: () => ({})
    },
    preprocessedData: {
      type: Object,
      default: () => ({})
    },
    filterText: String
  },
  data() {
    return {
      exportFormat: 'csv',
      exporting: false,
      needTagColumn: true,
    }
  },
  watch: {
    exportFormat(val) {
      if (val === 'csv') {
        this.needTagColumn = true
      }else if (val === 'zip') {
        this.needTagColumn = false
      }
    },
  },
  computed: {
    visible: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    structurePreview() {
      const time = this.generateTimeString()
      // const deviceNames = this.selectedDevices.map(d => d.name)

      if (this.exportFormat === 'csv') {
        return [
          ...this.selectedDevices.map(d => `├── ${d.name}_导出_${time}.csv`)
        ].join('\n');
      } else if (this.exportFormat === 'zip') {

        return [
          `导出_${time}.zip/`,
          ...this.selectedDevices.map(d =>
              `├── ${d.name}/` +
              (this.selectedTags[d.id]?.map(t =>
                      `\n│   ├── ${this.formatTagName(t)}.csv`
                  ).join('') || '\n│   └── (无数据)'
              ))
        ].join('\n')
      }
    },
    selectedDevices() {
      return this.selectedDeviceIds
          .map(id => this.devices.find(d => d.id === id))
          .filter(Boolean)
    },
    formatTagList() {
      return (deviceId) => this.sortTags([...this.selectedTags[deviceId] || []]).join('/')|| '无'
    },
  },
  methods: {
    async handleExport() {
      this.exporting = true
      try {
        if (this.exportFormat === 'csv') {
          await this.exportCSV()
        } else {
          await this.exportZIP()
        }
      } catch (error) {
        console.error(error)
        this.$message.error(`导出失败: ${error.message}`)
      } finally {
        this.exporting = false
        this.visible = false
      }
    },

    async exportCSV() {
      const devices = this.prepareExportData()
      if (!devices.length) return

      for (const device of devices) {
        const csvContent = this.generateDeviceCSV(device)
        if (csvContent) {
          const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'})
          saveAs(blob, this.generateFileName(device.name))
        }
      }
    },

    async exportZIP() {
      const devices = this.prepareExportData()
      if (!devices.length) return

      const zip = new JSZip()
      const timeFolder = zip.folder(this.generateTimeString())

      for (const device of devices) {
        const deviceFolder = timeFolder.folder(device.name)
        for (const tag of device.tags) {
          const content = this.generateTagCSV(device, tag)
          if (content) {
            deviceFolder.file(`${this.formatTagName(tag)}.csv`, content)
          }
        }
      }

      const content = await zip.generateAsync({type: 'blob'})
      saveAs(content, `导出_${this.generateTimeString()}.zip`)
    },

    generateDeviceCSV(device) {
      const {data, tags, name, fields} = device
      if (!data || tags.length === 0 || fields.length === 0) return null

      // 元信息
      const metaInfo = [
        `# 设备名称: ${name}`,
        `选择标签: ${tags.map(t => this.formatTagDisplay(t)).join('/')}`,
        `选择传感器: ${fields.join('/')}`,
        `生成时间: ${this.getBeijingTimeString()}`
      ].join('; ')

      const rows = []
      const headers = ['标签', '序号', ...fields]
      rows.push(headers.join(','))

      tags.forEach(tag => {
        const formattedTag = this.formatTagDisplay(tag)
        const sequenceMap = {}

        let maxSequence = 0
        fields.forEach(field => {
          const tagData = data[field]?.[tag]
          if (tagData) {
            tagData.data.forEach(([seq, value]) => {
              if (!sequenceMap[seq]) {
                sequenceMap[seq] = {[field]: value}
                if (seq > maxSequence) maxSequence = seq
              } else {
                sequenceMap[seq][field] = value
              }
            })
          }
        })

        for (let seq = 1; seq <= maxSequence; seq++) {
          const row = [formattedTag, seq]
          fields.forEach(field => {
            row.push(sequenceMap[seq]?.[field] ?? '')
          })
          rows.push(row.join(','))
        }
      })

      if (rows.length === 0) return null

      return '\ufeff' + [metaInfo, ...rows].join('\n')
    },

    generateTagCSV(device, tag) {
      const {data, name, fields} = device
      const formattedTag = this.formatTagDisplay(tag)

      // 元信息（不包含标签信息）
      const metaInfo = [
        `# 设备名称: ${name}`,
        `选择传感器: ${fields.join('/')}`,
        `生成时间: ${this.getBeijingTimeString()}`
      ].join('; ')

      const rows = []
      const headers = ['序号', ...fields]
      if (this.needTagColumn) {
        headers.unshift('标签')
      }
      rows.push(headers.join(','))

      const sequenceMap = {}
      let maxSequence = 0

      fields.forEach(field => {
        const tagData = data[field]?.[tag]
        if (tagData) {
          tagData.data.forEach(([seq, value]) => {
            if (!sequenceMap[seq]) {
              sequenceMap[seq] = {[field]: value}
              if (seq > maxSequence) maxSequence = seq
            } else {
              sequenceMap[seq][field] = value
            }
          })
        }
      })

      for (let seq = 1; seq <= maxSequence; seq++) {
        let row = []
        if (this.needTagColumn) {
          row = [formattedTag, seq]
        } else {
          row = [seq]
        }
        fields.forEach(field => {
          row.push(sequenceMap[seq]?.[field] ?? '')
        })
        rows.push(row.join(','))
      }

      if (rows.length === 0) return null

      return '\ufeff' + [metaInfo, ...rows].join('\n')
    },


    prepareExportData() {
      // console.log("selectedTags", JSON.stringify(this.selectedTags))
      return this.selectedDevices.map(device => ({
        id: device.id,
        name: device.name,
        data: this.preprocessedData[device.id],
        tags: this.sortTags([...this.selectedTags[device.id] || []]),
        fields: this.getFilteredFields(device.id)
      })).filter(d => d.tags.length && d.fields.length)
    },

    getFilteredFields(deviceId) {
      // console.log("deviceId",deviceId)
      // console.log("this.preprocessedData", JSON.stringify(this.preprocessedData))
      const filter = this.parseFilterSyntax(this.filterText)
      return this.naturalSort(
          Object.keys(this.preprocessedData[deviceId] || {}).filter(f =>
              filter.regex ? filter.regex.test(f) : true
          ))
    },

    generateFileName(deviceName) {
      const timestamp = this.generateTimeString()
      return `${deviceName}_导出_${timestamp}.csv`
    },

    generateTimeString() {
      const now = new Date();

      // 转换到东八区时间
      const beijingTime = new Intl.DateTimeFormat('zh-CN', {
        timeZone: 'Asia/Shanghai',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).format(now);

      // 格式处理："2025/03/10 21:04:09" -> "20250310-210409"
      return beijingTime.replace(/\//g, '').replace(/ /, '-').replace(/:/g, '');
    },

    getBeijingTimeString() {
      const now = new Date();
      const beijingTime = new Intl.DateTimeFormat('zh-CN', {
        timeZone: 'Asia/Shanghai',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).format(now).replace(/\//g, '-');

      // 获取毫秒（确保是 3 位数）
      const milliseconds = String(now.getMilliseconds()).padStart(3, '0');

      const result = `${beijingTime}.${milliseconds}`;
      console.log("getBeijingTimeString", result);
      return result;
    },

    formatTagDisplay(tag) {
      return tag === 'NO_TAG' ? '无标签' : tag
    },

    formatTagName(tag) {
      return tag === 'NO_TAG' ? '无标签' : tag
    },

    handleClose() {
      this.exportFormat = 'csv'
    },
    sortTags(tags) {
      //NO_TAG放第一个，其次是整数类字符串从小到大排序，最后是其他字符串，按字母顺序排序
      return [...tags].sort((a, b) => {
        // 优先处理NO_TAG
        if (a === 'NO_TAG') return -1
        if (b === 'NO_TAG') return 1

        // 判断是否为纯数字字符串
        const aIsNumeric = /^\d+$/.test(a)
        const bIsNumeric = /^\d+$/.test(b)

        // 数字类型比较
        if (aIsNumeric && bIsNumeric) {
          return parseInt(a, 10) - parseInt(b, 10)
        }

        // 数字类型优先于非数字类型
        if (aIsNumeric) return -1
        if (bIsNumeric) return 1

        // 普通字符串按字母顺序排序
        return a.localeCompare(b)
      })
    },
    // 解析高级查询语法
    parseFilterSyntax(input) {
      // 辅助方法：转义正则特殊字符
      function escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      }

      // 处理空值情况
      if (!input) return {regex: null, isExact: false}

      // 精确匹配模式 {条件1|条件2}
      const exactMatch = input.match(/^{(.*)}$/)
      if (exactMatch) {
        const conditions = exactMatch[1].split('|').map(s => s.trim())
        // 创建不区分大小写的正则表达式，匹配完整字段名
        return {
          regex: new RegExp(`^(${conditions.map(c => escapeRegExp(c)).join('|')})$`, 'i'),
          isExact: true
        }
      }

      // 普通模糊匹配（保留原有逻辑）
      return {
        regex: new RegExp(escapeRegExp(input), 'i'),
        isExact: false
      }
    },
    naturalSort(arr) {
      // console.log("arr", JSON.stringify(arr))
      return arr.slice().sort((a, b) =>
          a.localeCompare(b, undefined, {numeric: true, sensitivity: 'base'})
      )
    },
  }
}
</script>

<style scoped>
.export-dialog {
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.structure-preview {
  max-height: 30vh;
  flex: 1;
  margin-top: 10px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
  overflow: auto;
}

.structure-preview pre {
  margin: 0;
  white-space: pre-wrap;
}

.export-preview{
  max-height: 20vh;
  flex: 1;
  margin-top: 10px;
  padding: 30px;
  background: #f8f9fa;
  border-radius: 4px;
  overflow: auto;
  font-weight: normal;
}
</style>