<template>
  <div class="message-center">
    <!-- 筛选区域 -->
    <div class="filter-area">
      <el-select v-model="filterStatus" clearable placeholder="消息状态">
        <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        />
      </el-select>

      <el-select v-model="filterType" clearable placeholder="消息类型">
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
          placeholder="搜索标题/内容"
          style="width: 200px"
      />

      <el-button type="danger" @click="handleBatchDelete">批量删除</el-button>
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
          <div class="left-tags">
            <el-tag :type="getTagType(msg.type)">{{ msg.type }}</el-tag>
            <span v-if="msg.status === 'UNREAD'" class="unread-tag">未读</span>
            <span v-if="msg.status === 'READ'||msg.status === 'MARKED'" class="read-tag">已读</span>
          </div>
          <i class="el-icon-delete delete-icon" @click.stop="deleteMessage(msg.id)"></i>
        </div>

        <h3 class="message-title">{{ msg.title }}</h3>

        <div class="card-footer">
          <div class="sender">来自：{{ msg.sendId === 0 ? '系统' : msg.sendId }}</div>
          <div class="time">{{ formatTime(msg.createTime) }}</div>
        </div>

        <div class="mark-icon" @click.stop="toggleMark(msg)">
          <i :class="msg.status === 'MARKED' ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />

    <!-- 详情对话框 -->
    <el-dialog :title="currentMessage.title" :visible.sync="detailVisible">
      <div class="detail-content">
        <p><strong>来自：</strong>{{ currentMessage.sendId === 0 ? '系统' : currentMessage.sendId }}</p>
        <p><strong>时间：</strong>{{ formatTime(currentMessage.createTime) }}</p>
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
    searchKeyword: 'fetchMessages'
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

      msg.status = newStatus
      // 刷新消息列表
      await this.fetchMessages()
    },

    async deleteMessage(id) {
      await this.$request.post(`/message/mark/${id}?status=DELETED`)
      await this.fetchMessages()
    },

    handleBatchDelete() {
      // 需要实现多选功能，此处省略选择逻辑
      this.$message.warning('请先实现消息选择功能')
    },

    getTagType(type) {
      const map = {
        NOTICE: 'success',
        WARNING: 'warning',
        ERROR: 'danger'
      }
      return map[type] || 'info'
    },

    formatTime(timestamp) {
      return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
    },

    handleSizeChange(val) {
      this.pageSize = val
    },

    handleCurrentChange(val) {
      this.currentPage = val
    }
  }
}
</script>

<style scoped>
.message-card {
  margin-bottom: 20px;
  position: relative;
  cursor: pointer;
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
</style>