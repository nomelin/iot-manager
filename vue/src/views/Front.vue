<template>
  <div class="front-layout">
    <!--头部-->
    <div class="front-header">
      <div class="front-header-left">

      </div>
      <div class="front-header-center">
        <div style="font-size: 2rem">消息</div>
      </div>
      <div class="front-header-right">
        <div style="font-size: 2rem">用户</div>
      </div>
    </div>
    <!--主体-->
    <div class="main-body">
      <div class="main-left">
        <div class="main-left-upper">


        </div>

        <el-menu :default-active="$route.path" active-text-color="#0d53ff" class="el-menu" router text-color="#565757">
          <el-menu-item class="el-menu-item" index="/front/view_platform">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">监测平台</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/upload">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">文件上传</span>
          </el-menu-item>

          <el-menu-item class="el-menu-item" index="/front/base_query">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">基础查询</span>
          </el-menu-item>

          <el-menu-item class="el-menu-item" index="/front/template_manage">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">模板管理</span>
          </el-menu-item>

          <el-menu-item class="el-menu-item" index="/front/device_manage">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">设备管理</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/group_manage">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">组管理</span>
          </el-menu-item>
          <el-menu-item class="el-menu-item" index="/front/third_party">
            <i class="el-icon-folder-opened"></i>
            <span slot="title" class="words">第三方</span>
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

export default {
  name: "FrontLayout",

  data() {
    return {
      user: getItemWithExpiry("user"),
    }
  },

  mounted() {
  },
  updated() {
  },
  computed: {},
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
        this.$request.delete("/openapi/logout").then(res => {
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
      this.$router.push('/person');
    },
  }

}
</script>

<style scoped>

.front-layout {
  /*display: flex;*/
  height: 100vh;
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
  /*position: absolute; !* 设置绝对定位 *!*/
  padding-left: 10%;
  padding-top: 10%;
}

.avatar {
  margin-right: 1.4rem; /* 头像与用户名之间的间距 */
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
  flex: 1;
}

.front-header-right {
  width: 200px;
  padding-right: 20px;
  text-align: right;
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


</style>