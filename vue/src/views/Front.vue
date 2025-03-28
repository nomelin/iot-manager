<template>
  <div class="front-layout">
    <!--头部-->
    <div class="front-header">
      <div class="front-header-left">

      </div>
      <div class="front-header-center">

      </div>
      <div class="front-header-right">
        <router-link class="message-icon" to="/front/message_center">
          <img
              alt="消息"
              class="icon-svg"
              src="@/assets/imgs/msg_box.svg"
              style="width: 3rem; height: 3rem; opacity: 0.8; "
          />
          <span v-if="unreadCount > 0" class="badge">{{ unreadCountDisplay }}</span>
        </router-link>
        <div class="user-info" @click="goToUserProfile">
          <el-avatar
              :size="45"
              :src="defaultAvatar"
              class="avatar">
          </el-avatar>
          <div class="username">{{ user?.name || '用户' }}</div>
        </div>
      </div>
    </div>
    <!--主体-->
    <div class="main-body">
      <div class="main-left">
        <!--        <div class="main-left-upper">-->


        <!--        </div>-->

        <el-menu :default-active="$route.path" active-text-color="#0d53ff" class="el-menu" router text-color="#565757">
          <el-menu-item class="el-menu-item" index="/front/view_platform">
            <i class="el-icon-monitor"></i>
            <span slot="title" class="words">监测平台</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/tag_view">
            <i class="el-icon-data-line"></i>
            <span slot="title" class="words">标识视图</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/base_query">
            <i class="el-icon-postcard"></i>
            <span slot="title" class="words">基础查询</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/upload">
            <i class="el-icon-upload2"></i>
            <span slot="title" class="words">文件上传</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/template_manage">
            <i class="el-icon-document-copy"></i>
            <span slot="title" class="words">模板管理</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/device_manage">
            <i class="el-icon-menu"></i>
            <span slot="title" class="words">设备/数据集</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/group_manage">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">组管理</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/alarm_manage">
            <i class="el-icon-warning"></i>
            <span slot="title" class="words">告警管理</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/message_center">
            <i class="el-icon-message-solid"></i>
            <span slot="title" class="words">消息中心</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCountDisplay }}</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/third_party">
            <i class="el-icon-link"></i>
            <span slot="title" class="words">第三方</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/user_profile">
            <i class="el-icon-user"></i>
            <span slot="title" class="words">我的</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" @click="logout">
            <i class="el-icon-switch-button"></i>
            <span slot="title" class="words">退出登录</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="main-right">
        <div class="content-scroll"> <!-- 滚动容器 -->
          <router-view @update:user="updateUser"/>
        </div>
      </div>
    </div>
  </div>

</template>

<script>
import {getItemWithExpiry} from "@/App"
import defaultAvatar from "@/assets/imgs/default-avatar.jpg"

export default {
  name: "FrontLayout",

  data() {
    return {
      user: getItemWithExpiry("user"),
      defaultAvatar: defaultAvatar,
      unreadCount: 0, // 未读消息数量
    }
  },

  mounted() {
    this.fetchUnreadCount();
    // 每隔5s刷新未读消息数量
    this.unreadInterval = setInterval(this.fetchUnreadCount, 5 * 1000);
  },
  updated() {
  },
  beforeDestroy() {
    clearInterval(this.unreadInterval); // 清理定时器
  },
  computed: {
    unreadCountDisplay() {
      return this.unreadCount > 99 ? '99+' : this.unreadCount;
    },
  },
  methods: {

    updateUser() {
      this.user = getItemWithExpiry("user")   // 重新获取下用户的最新信息
    },
    // 退出登录
    logout() {
      this.$confirm('确定要退出吗？', '确认退出', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'success',
        center: true
      }).then(response => {
        this.$request.delete("/user/logout").then(res => {
          if (res.code === '200') {
            this.$message.success("退出成功")
            localStorage.removeItem("user");
            this.$router.push("/login");
          } else {
            this.$message.error(res.code + ": " + res.msg)
          }
        }).catch(error => {
          this.$message.error("退出失败")
        });
      }).catch(error => {
        // 用户点击了取消按钮，可以在这里处理取消事件，比如关闭对话框
      });
    },
    goToUserProfile() {
      this.$router.push('/front/user_profile');
    },
    async fetchUnreadCount() {
      try {
        const response = await this.$request.get("/message/unread-count");
        if (response.code === "200") {
          console.log("获取未读消息数量成功", response.data);
          this.unreadCount = response.data;
        }
      } catch (error) {
        console.error("获取未读消息数量失败", error);
      }
    },
  }

}
</script>

<style scoped>

.front-layout {
  /*display: flex;*/
  margin: 0;
  padding: 0;
  height: 100vh;
  width: 100vw;
  overflow-x: hidden; /* 避免水平滚动条 */
  box-sizing: border-box;
}

.main-body {
  display: flex;
  /*height: 100%;*/
  height: 92vh;
  /*flex-grow: 1;*/
  overflow: hidden; /* 关键1：阻止外层滚动 */
}


.main-left {
  width: 15vw;
  height: 100%;
  min-width: 150px;
}

.main-left-upper {
  background-color: #ffffff;
  border: none;
  border-radius: 1.5rem;
  height: auto;
  width: 80%;
  margin-left: 10%;
  margin-top: 10%;
  padding-bottom: 10%;
  /*position: relative; !* 设置相对定位，为了让进度条容器相对于该 div 定位 *!*/
}

.user-info {
  display: flex; /* 使用 Flexbox 布局 */
  align-items: center; /* 垂直方向上居中对齐 */
  font-size: 1.2rem;
  font-weight: bold;
  color: #333333;
  margin-left: 10%;
  padding-right: 10%;
  /*position: absolute; !* 设置绝对定位 *!*/
  /*padding-left: 10%;*/
  /*padding-top: 10%;*/
}


.user-name {
  font-size: 1rem; /* 可根据需要调整用户名的样式 */
  font-weight: bold; /* 加粗 */
  color: #333333; /* 字体颜色 */
}

.progress {
  /*position: absolute; !* 设置绝对定位 *!*/
  margin-top: 10%;
  padding-left: 5%;
  width: 90%;
}

.el-menu {
  border: none;
  border-radius: 1.5rem;
  height: auto;
  width: 80%;
  margin-left: 10%;
  margin-top: 10%;
  padding-bottom: 30%;
  padding-top: 5%;
}

.el-menu-item {
  height: 3.5rem;
  font-size: 1rem;

  /*padding-top: 20px; !* 增加上边距 *!*/
  /*margin-top: 0.5rem; !* 修正菜单项的高度 *!*/
}

.el-menu-item:hover {
  font-size: 1rem;
  height: 3.5rem;
  padding-top: 2px; /* 增加上边距 */
  /*margin-top: 0.5rem; !* 修正菜单项的高度 *!*/

}

.main-right {
  width: 100%;
  height: 100%;
  background-color: #f9f9f9;
  border-radius: 1.5rem;
  margin-right: 1%;
  min-width: 0; /* 关键2：允许在flex布局中收缩 */
  display: flex; /* 新增 */
}

.content-scroll {
  flex: 1;
  overflow-x: auto; /* 关键3：内容区横向滚动 */
  /*padding: 20px; !* 根据实际需求调整 *!*/
  min-width: 0; /* 继承收缩特性 */
}

.front-layout {
  background: #ebeef5;
}

/*.front-notice {*/
/*  height: 0;*/
/*  padding: 5px 20px;*/
/*  color: #666;*/
/*  font-size: 12px*/
/*}*/
.logo {
  width: 10vw;
  margin-top: 3%;
}

.front-header {
  display: flex;
  height: 7vh;
  width: 100vw;
  /*line-height: 60px;*/
  /*border-bottom: 1px solid #eee;*/
  /*background: #f5f6f7;*/
}

.front-header-left {
  width: 50%;
  /*display: flex;*/
  /*align-items: center;*/
  padding-left: 2%;
}

.front-header-dropdown img {
  width: 40px;
  height: 40px;
  border-radius: 50%
}

.front-header-center {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%; /* 确保 div 具有高度 */
  width: 100%;
}

.front-header-right {
  display: flex;
  width: 300px;
  justify-content: center;
  align-items: center;
  height: 100%; /* 确保 div 具有高度 */
  padding-right: 20px;
  /*text-align: right;*/
}

.icon-svg {
  margin-right: 10px;
}

.front-header-dropdown {
  display: flex;
  align-items: center;
  justify-content: right;
}

.el-dropdown-menu {
  width: 100px !important;
  text-align: center !important;
}

/*页面具体样式自定义*/
.main-content {
  width: 100%;
  margin: 5px auto;
}

/* ElementUI 样式覆盖 */
/*.el-menu.el-menu--horizontal {*/
/*  border: none !important;*/
/*  height: 80px;*/
/*  border-radius: 10px;*/
/*}*/


.words {
  font-weight: bold;
}

/*::v-deep .el-menu-item .is-active {*/
/*  background-color: #3370ff !important;*/
/*  color: #fff;*/
/*}*/
.user-space-info {
  width: 80%;
}

.user-space {
  display: inline-block; /* 将 span 元素设置为行内块级元素 */
  font-weight: bold;
  font-size: 0.8rem;
  width: 100%; /* 设置宽度为100% */
  text-align: right; /* 将文本内容右对齐 */
  box-sizing: border-box; /* 使用边框盒模型，确保宽度包含 padding 和 border */
}

.avatar {
  border-radius: 50%;
  margin-right: 1rem;
  overflow: hidden; /* 防止头像溢出 */
}

.message-icon {
  position: relative; /* 关键，确保 .badge 基于此定位 */
}

.badge {
  position: absolute;
  top: 0.4rem;
  right: 0.8rem;
  background-color: #fa155a;
  color: #f8f9fa;
  border-radius: 50%;
  width: 1.3rem; /* 控制红点大小 */
  height: 1.3rem; /* 控制红点大小 */
  font-size: 0.7rem; /* 字体大小 */
  line-height: 1.3rem; /* 垂直居中 */
  display: flex; /* 让数字在圆点中居中 */
  justify-content: center;
  align-items: center;
  transform: translate(50%, -50%);
}


.username {
  white-space: nowrap; /* 确保用户名不换行 */
  overflow: hidden; /* 防止超出隐藏 */
  text-overflow: ellipsis; /* 超出显示省略号 */
  max-width: 120px; /* 根据需要设置合适的最大宽度 */
}
</style>