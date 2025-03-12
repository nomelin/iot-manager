<template>
  <div class="container">
    <div class="main-content">
      <!-- 筛选区域 -->
      <div class="filter-area">
        <el-select v-model="filterStatus" clearable placeholder="消息状态筛选">
          <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>

        <el-select v-model="filterType" clearable placeholder="消息类型筛选">
          <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>

        <el-input
            v-model="searchKeyword"
            clearable
            placeholder="搜索标题&内容"
            style="width: 300px"
        />

        <el-button type="danger" @click="handleBatchDelete">批量删除 ({{ selectedMessages.length }})</el-button>
        <el-checkbox
            v-model="selectAll"
            :indeterminate="isIndeterminate"
            style="margin-left: 15px"
            @change="handleSelectAll"
        >全选本页
        </el-checkbox>
      </div>

      <!-- 消息列表 -->
      <div class="message-list">
        <el-card
            v-for="msg in paginatedMessages"
            :key="msg.id"
            :class="{ 'read-card': msg.status === 'READ' }"
            class="message-card"
            @click.native="showDetail(msg)"
        >
          <div class="card-header">
            <el-checkbox
                v-model="selectedMessages"
                :label="msg.id"
                style="margin-right: 10px"
                @click.native.stop
            ><br></el-checkbox>
            <div class="left-tags">
              <el-tag :type="getTagType(msg.type)">{{ getTagText(msg.type) }}</el-tag>
              <span v-if="msg.status === 'UNREAD'" class="unread-tag">未读</span>
              <span v-if="msg.status === 'READ'||msg.status === 'MARKED'" class="read-tag">已读</span>
            </div>
            <i class="el-icon-delete delete-icon" @click.stop="deleteMessage(msg.id)"></i>
          </div>

          <h3 class="message-title">{{ msg.title }}</h3>

          <div class="card-footer">
            <div class="sender">来自：
              <span v-if="msg.sendId === 0"><i class="el-icon-warning"></i> 系统</span>
              <span v-else>{{ msg.sendId }}</span>
            </div>
            <div class="time">{{ formatTime(msg.createTime) }}</div>
          </div>

          <div class="mark-icon" @click.stop="toggleMark(msg)">
            <i :class="msg.status === 'MARKED' ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
          </div>
        </el-card>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <!-- 分页 -->
        <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>

      <!-- 详情对话框 -->
      <el-dialog :title="currentMessage.title" :visible.sync="detailVisible">
        <div class="detail-content">
          <p><strong>发信人：</strong>
            <span v-if="currentMessage.sendId === 0"><i class="el-icon-warning"></i> 系统</span>
            <span v-else>{{ currentMessage.sendId }}</span>
          </p>
          <p><strong>发送时间：</strong>{{ formatTime(currentMessage.createTime) }}</p>
          <div class="message-content">{{ currentMessage.content }}</div>
        </div>

        <div slot="footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
              :icon="currentMessage.status === 'MARKED' ? 'el-icon-star-on' : 'el-icon-star-off'"
              type="warning"
              @click="toggleMark(currentMessage)"
          >
            {{ currentMessage.status === 'MARKED' ? '取消标记' : '标记' }}
          </el-button>
          <el-button type="danger" @click="deleteMessage(currentMessage.id)">删除</el-button>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'

export default {
  data() {
    return {
      messages: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      filterStatus: null,
      filterType: null,
      searchKeyword: '',
      detailVisible: false,
      currentMessage: {},
      statusOptions: [
        {value: 'UNREAD', label: '未读'},
        {value: 'READ', label: '已读'},
        {value: 'MARKED', label: '标记'}
      ],
      typeOptions: [
        {value: 'NOTICE', label: '通知'},
        {value: 'WARNING', label: '警告'},
        {value: 'ERROR', label: '错误'}
      ],
      selectedMessages: [], // 存储选中的消息 ID
      selectAll: false,
      isIndeterminate: false,
    }
  },

  computed: {
    paginatedMessages() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.messages.slice(start, start + this.pageSize)
    }
  },

  watch: {
    filterStatus: 'fetchMessages',
    filterType: 'fetchMessages',
    searchKeyword: 'fetchMessages',
    selectedMessages(newVal) {
      const currentPageIds = this.paginatedMessages.map(m => m.id)
      this.selectAll = newVal.length > 0 && currentPageIds.every(id => newVal.includes(id))
      this.isIndeterminate = !this.selectAll && newVal.length > 0
    }
  },

  mounted() {
    this.fetchMessages()
  },

  methods: {
    async fetchMessages() {
      try {
        const params = {
          type: this.filterType,
          status: this.filterStatus,
          keyword: this.searchKeyword
        }

        const endpoint = this.searchKeyword
            ? '/message/list/simple/search'
            : '/message/list/simple'

        const res = await this.$request.get(endpoint, {params})
        if (res.code === '200') {
          this.messages = res.data
          this.total = res.data.length
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        this.$message.error('获取消息失败')
      }
    },

    async showDetail(msg) {
      const res = await this.$request.get(`/message/get/${msg.id}`)
      if (res.code === '200') {
        this.currentMessage = res.data
        this.detailVisible = true
      }

      if (msg.status === 'UNREAD') {
        await this.$request.post(`/message/mark/${msg.id}?status=READ`)
        this.$set(msg, 'status', 'READ') // 更新列表数据
        this.$set(this.currentMessage, 'status', 'READ') // 确保详情状态一致
      }
    },

    async toggleMark(msg) {
      if (msg.status === 'UNREAD') {
        this.$message.warning('请先阅读消息，才能标记')
        return
      }
      const newStatus = msg.status === 'MARKED' ? 'READ' : 'MARKED'
      await this.$request.post(`/message/mark/${msg.id}?status=${newStatus}`)

      this.$set(msg, 'status', newStatus) // 更新列表数据
      // 刷新消息列表
      await this.fetchMessages()
    },

    async deleteMessage(id) {
      try {
        await this.$confirm('确定要删除这条消息吗？', '提示', {
          type: 'warning'
        })
        await this.$request.post('/message/delete', [id])

        await this.fetchMessages()
        // 从已选列表中移除
        this.selectedMessages = this.selectedMessages.filter(mId => mId !== id)
        this.currentMessage = {}
        this.detailVisible = false
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },

    async handleBatchDelete() {
      if (this.selectedMessages.length === 0) {
        this.$message.warning('请选择要删除的消息')
        return
      }

      try {
        await this.$confirm(`确定要删除选中的 ${this.selectedMessages.length} 条消息吗？`, '提示', {
          type: 'warning'
        })

        const res = await this.$request.post('/message/delete', this.selectedMessages)

        if (res.code === '200') {
          this.$message.success('删除成功')
          await this.fetchMessages()
          this.selectedMessages = []
          this.currentPage = 1
        } else {
          this.$message.error(res.msg)
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },

    getTagType(type) {
      const map = {
        NOTICE: 'success',
        WARNING: 'warning',
        ERROR: 'danger'
      }
      return map[type] || 'info'
    },
    getTagText(type) {
      return this.typeOptions.find(item => item.value === type).label
    },

    formatTime(timestamp) {
      return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
    },

    handleSizeChange(val) {
      this.pageSize = val
    },

    handleCurrentChange(val) {
      this.currentPage = val
    },

    handleSelectAll(val) {
      const currentPageIds = this.paginatedMessages.map(m => m.id)
      if (val) {
        // 去重添加
        const newSelection = [...new Set([...this.selectedMessages, ...currentPageIds])]
        this.selectedMessages = newSelection
      } else {
        // 过滤保留不在当前页的选项
        this.selectedMessages = this.selectedMessages.filter(id => !currentPageIds.includes(id))
      }
    },
  }
}
</script>

<style scoped>
.container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止全局滚动条 */
  font-weight: bold;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  overflow: hidden; /* 关键样式 */
  /*justify-content: center;*/
}

.filter-area {
  display: flex;
  gap: 15px; /* 设置子元素之间的间隔 */
  align-items: center; /* 可选，确保子元素垂直居中对齐 */
  margin-bottom: 10px;
}
.message-list {
  overflow: auto;
  padding-bottom: 20px;
  background-color: #fff;
  border-radius: 1rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  justify-items: center;
}

.message-card {
  margin-top: 10px;
  margin-bottom: 20px;
  position: relative;
  cursor: pointer;
  border-radius: 1rem;
  width: 80%;
  transition: transform 0.1s ease, box-shadow 0.1s ease; /* 添加平滑过渡效果 */
}

.message-card:hover {
  transform: translateX(4px);
  box-shadow: 2px 2px 8px rgba(0, 0, 0, 0.1); /* 只在悬停时显示阴影 */
}

.read-card {
  background-color: #f5f5f5;
  opacity: 0.8;
}

.unread-tag {
  color: #ff0000;
  margin-left: 10px;
}

.read-tag {
  color: #8c939d;
  margin-left: 10px;
}

.delete-icon {
  position: absolute;
  right: 20px;
  top: 20px;
  font-size: 18px;
}

.mark-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 24px;
  color: #e6a23c;
}

.message-title {
  font-size: 18px;
  margin: 10px 0;
}

.card-header {
  display: flex;
  justify-content: left;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  color: #999;
  font-size: 12px;
}

.detail-content {
  line-height: 1.6;
}

.message-content {
  margin-top: 20px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px; /* 可根据需要调整分页按钮与内容的间距 */
}

/*.message-card ::v-deep .el-checkbox__label {*/
/*  display: none !important;*/
/*}*/

::v-deep .el-button {
  font-weight: bold !important;
}

::v-deep .el-collapse-item__header {
  font-weight: bold !important;
}

::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}

::v-deep .el-select .el-input__inner::placeholder {
  font-weight: bold !important; /* 设置 placeholder 为粗体 */
}

::v-deep .el-input__inner {
  font-weight: bold !important; /* 设置输入框字体为粗体 */
}

/*::v-deep .el-select .el-option {*/
/*  font-weight: bold !important; !* 设置下拉选项字体为粗体 *!*/
/*}*/
</style>