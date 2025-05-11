<template>
  <div class="main-content">
    <el-card class="card">
      <div class="header-bar">
        <div class="control">
          <el-tooltip content="全局配置" placement="bottom" transition="none">
            <i class="el-icon-setting icon" @click="goToConfig"></i>
          </el-tooltip>
          <el-tooltip content="Debug" placement="bottom" transition="none">
            <i class="el-icon-data-analysis icon" @click="goToDebug"></i>
          </el-tooltip>
        </div>
        <div class="password">
          <el-button class="primary-button" style="margin-right: 10rem" type="primary" @click="updatePassword">修改密码
          </el-button>
        </div>
      </div>
      <el-form :model="user" class="form" label-width="80px">
        <div style="margin: 15px; text-align: center;align-items: center; justify-content: center;width: 100%;">
          <el-avatar
              :size="100"
              :src="defaultAvatar"
              class="avatar">
          </el-avatar>
        </div>
        <el-form-item label="ID" prop="username">
          <el-input v-model="user.id" class="input" disabled placeholder="id"></el-input>
        </el-form-item>
        <el-form-item label="用户名" prop="name">
          <el-input v-model="user.name" class="input" maxlength="50" placeholder="用户名"></el-input>
        </el-form-item>
        <el-form-item label="PushPlus好友令牌" prop="pushplusToken">
          <el-input v-model="user.pushplusToken" class="input" maxlength="50"
                    placeholder="请联系管理员获取(用于微信推送消息)"></el-input>
        </el-form-item>
        <div class="btn-group">
          <el-button class="primary-button" type="primary" @click="update">保 存</el-button>
        </div>
      </el-form>
    </el-card>
    <el-dialog :close-on-click-modal="false" :visible.sync="dialogVisible" class="dialog" destroy-on-close
               title="修改密码" width="30%">
      <el-form ref="formRef" :model="user" :rules="rules" label-width="80px" style="padding-right: 20px">
        <el-form-item label="原始密码" prop="password">
          <el-input v-model="user.oldPassword" class="input" placeholder="原始密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="user.newPassword" class="input" placeholder="新密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="user.confirmPassword" class="input" placeholder="确认密码" show-password></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog
        :visible.sync="configDialogVisible"
        destroy-on-close
        title="全局配置"
        width="30%">
      <el-form label-width="auto">
        <el-form-item label="IoTDB重试次数">
          <el-input-number v-model="retryCount" :min="0" :step="1"/>
        </el-form-item>
        <el-form-item label="是否启用缓存">
          <el-switch v-model="cacheEnabled" active-text="启用" inactive-text="禁用"/>
        </el-form-item>
        <el-form-item label="查询时尝试使用快速聚合">
          <el-switch v-model="fastAggregateEnabled" active-text="启用" inactive-text="禁用"/>
        </el-form-item>
      </el-form>
      <div style="color: gray; font-size: 12px; margin-top: 10px;">
        注意：本页面的配置在后端重启后会重置为配置文件初始值。
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="configDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveRetryConfig">保 存</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import CryptoJS from 'crypto-js'
import defaultAvatar from "@/assets/imgs/default-avatar.jpg"

import {getItemWithExpiry, updateItemWithExpiry} from "@/App"

export default {
  data() {
    const validatePassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请确认密码'))
      } else if (value !== this.user.newPassword) {
        callback(new Error('确认密码错误'))
      } else {
        callback()
      }
    }
    return {
      user: getItemWithExpiry("user"),
      dialogVisible: false,
      defaultAvatar: defaultAvatar,

      configDialogVisible: false,
      retryCount: 0,
      cacheEnabled: false,
      fastAggregateEnabled: false,

      rules: {
        oldPassword: [
          {required: true, message: '请输入原始密码', trigger: 'blur'},
        ],
        newPassword: [
          {required: true, message: '请输入新密码', trigger: 'blur'},
        ],
        confirmPassword: [
          {validator: validatePassword, required: true, trigger: 'blur'},
        ],
      }
    }
  },
  created() {

  },
  methods: {
    update() {
      // 保存当前的用户信息到数据库
      this.$request.put('/user', this.user).then(res => {
        if (res.code === '200') {
          // 成功更新
          this.$message.success('个人信息保存成功')
          // 更新浏览器缓存里的用户信息
          updateItemWithExpiry("user", this.user);

          // 触发父级的数据更新
          this.$emit('update:user')
        } else {
          this.$message.error(res.code + ": " + res.msg)
        }
      })
    },
    // 修改密码
    updatePassword() {
      this.dialogVisible = true
    },
    save() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          // 对密码进行哈希和加盐处理
          let saltedOldPassword = this.user.name + this.user.oldPassword;
          let saltedNewPassword = this.user.name + this.user.newPassword;
          // 使用哈希过的密码
          let oldPassword = CryptoJS.SHA256(saltedOldPassword).toString();
          let newPassword = CryptoJS.SHA256(saltedNewPassword).toString();
          this.$request.put('user/updatePassword', {
            name: this.user.name,
            oldPassword: oldPassword,
            newPassword: newPassword
          }).then(res => {
            if (res.code === '200') {
              // 成功更新
              this.$message.success('修改密码成功')
              this.$request.delete("/user/logout").then(res => {
                if (res.code === '200') {
                  this.$message.success("请重新登录")
                  localStorage.removeItem("user");
                  this.$router.push("/login");
                } else {
                  this.$message.error(res.code + ": " + res.msg)
                }
              }).catch(error => {
                this.$message.error("退出失败")
              });
            } else {
              this.$message.error(res.msg)
            }
          })
        }
      })
    },
    goToConfig() {
      this.$request.get("/global/getIoTDBRetry").then(res => {
        if (res.code === '200') {
          this.retryCount = res.data
          this.configDialogVisible = true
        } else {
          this.$message.error("获取IoTDB重试次数配置失败：" + res.msg)
        }
      })
      this.$request.get("/global/getCacheEnabled").then(res => {
        if (res.code === '200') {
          this.cacheEnabled = res.data;
          this.configDialogVisible = true;
        } else {
          this.$message.error("获取缓存配置失败：" + res.msg);
        }
      })
      this.$request.get("/global/getFastAggregateEnabled").then(res => {
        if (res.code === '200') {
          this.fastAggregateEnabled = res.data;
          this.configDialogVisible = true;
        } else {
          this.$message.error("获取快速聚合配置失败：" + res.msg);
        }
      });
    },
    saveRetryConfig() {
      const setRetry = this.$request.get(`/global/setIoTDBRetry/${this.retryCount}`);
      const setCache = this.$request.get(`/global/setCacheEnabled/${this.cacheEnabled}`);
      const setFastAgg = this.$request.get(`/global/setFastAggregateEnabled/${this.fastAggregateEnabled}`);

      Promise.all([setRetry, setCache, setFastAgg]).then(([retryRes, cacheRes, fastAggRes]) => {
        if (retryRes.code === '200' && cacheRes.code === '200' && fastAggRes.code === '200') {
          this.$message.success("配置保存成功");
          this.configDialogVisible = false;
        } else {
          this.$message.error("保存失败：" + (retryRes.msg || cacheRes.msg || fastAggRes.msg));
        }
      });
    },
    goToDebug() {
      // this.$router.push("/debug");
      window.open('/debug', '_blank');
    },
  }
}
</script>

<style scoped>
::v-deep .el-form-item__label {
  font-weight: bold;
}


::v-deep .avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border-radius: 30%;
}

::v-deep .avatar-uploader .el-upload:hover {
  border-color: #0d53ff;
}

/*.avatar-uploader-icon {*/
/*  font-size: 28px;*/
/*  color: #8c939d;*/
/*  width: 50%;*/
/*  height:50%;*/
/*  line-height: 120px;*/
/*  text-align: center;*/
/*  border-radius: 50%;*/
/*}*/

/*.avatar {*/
/*  width: 9rem;*/
/*  height: 9rem;*/
/*  display: block;*/
/*  border-radius: 20%;*/
/*}*/

.main-content {
  width: 95%;
  height: 80%;

}

.card {
  width: 80%;
  height: auto;
  padding-bottom: 3rem;
  box-shadow: 0 2rem 7rem rgba(217, 236, 255, 0.5);
  border-radius: 5rem;
  margin: 2rem auto 0;
}

.header-bar {
  padding-left: 3rem;
  display: flex; /* 使用flex布局 */
  justify-content: space-between; /* 左右两边对齐 */
  align-items: center; /* 垂直居中对齐 */
  width: 100%; /* 宽度占满父容器 */
  margin-bottom: 1rem; /* 底部留空，避免贴近表单 */
}

.control {
  display: flex; /* 让左侧按钮横向排列 */
  align-items: center; /* 垂直居中 */
}

.password {

  display: flex; /* 右侧按钮对齐 */
  align-items: center; /* 垂直居中 */
}

.form {
  width: 70%;
  height: 100%;
  margin: 0 auto;
}

::v-deep .input .el-input__inner {
  width: 100%;
  text-align: left;
  border: 1px solid #d9d9d9;
  outline: none;
  font-weight: bold;
  font-size: 1rem;
  height: 3rem;
  border-radius: 0.5rem;
}

.btn-group {
  margin-top: 1rem;
  text-align: center;
}

.dialog-footer {
  text-align: center;
}

.dialog {
  border-radius: 0.5rem;
}

.icon {
  font-size: 2rem;
  margin-right: 1.5rem;
  cursor: pointer;
}

::v-deep .el-dialog {
  border-radius: 1.5rem !important;
}

</style>