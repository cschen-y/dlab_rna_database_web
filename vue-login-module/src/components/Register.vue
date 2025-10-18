<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">用户注册</h1>
      <p class="login-subtitle">创建新账号，填写以下信息</p>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required />
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input type="email" id="email" v-model="email" required />
        </div>
        <div class="form-group">
          <label for="phone">手机号</label>
          <input type="tel" id="phone" v-model="phone" />
        </div>
        <button class="btn-primary" type="submit">注册</button>
        <p class="register-hint">已经有账号？<a @click="goToLogin" href="javascript:void(0)">返回登录</a></p>
      </form>
      <p v-if="errorMessage" class="alert error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="alert success">{{ successMessage }}</p>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      username: "",
      password: "",
      email: "",
      phone: "",
      errorMessage: "",
      successMessage: "",
    };
  },
  methods: {
    handleRegister() {
      this.errorMessage = "";
      this.successMessage = "";

      axios
        .post("/api/auth/register", {
          username: this.username,
          password: this.password,
          email: this.email,
          phone: this.phone,
        })
        .then((response) => {
          if (response.data && response.data.success) {
            this.successMessage = "注册成功！即将跳转到登录页...";
            this.username = "";
            this.password = "";
            this.email = "";
            this.phone = "";
            setTimeout(() => {
              this.$router.push("/login");
            }, 3000);
          } else {
            this.errorMessage = (response.data && response.data.message) || "注册失败，请稍后重试";
          }
        })
        .catch((error) => {
          if (error.response && error.response.data) {
            this.errorMessage = error.response.data.message || "注册失败，请稍后重试";
          } else {
            this.errorMessage = "注册失败，请稍后重试";
          }
          console.error(error);
        });
    },
    goToLogin() {
      this.$router.push("/login");
    },
  },
};
</script>

<style scoped>
/* 页面居中与背景，与登录页一致 */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef3f9 100%);
}

/* 卡片样式，与登录页一致 */
.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

/* 标题与副标题 */
.login-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}
.login-subtitle {
  margin: 0 0 24px;
  font-size: 14px;
  color: #6b7280;
}

/* 表单与控件，与登录页一致 */
.login-card form {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.form-group {
  margin-bottom: 16px;
  width: 100%;
  max-width: 360px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}
.form-group input {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}
.form-group input:focus {
  border-color: #3b82f6;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

/* 按钮样式，与登录页一致 */
.btn-primary {
  width: 100%;
  max-width: 360px;
  margin-top: 4px;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  background: #3b82f6;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.15s ease, transform 0.02s ease;
}
.btn-primary:hover {
  filter: brightness(1.05);
}
.btn-primary:active {
  transform: scale(0.99);
}

/* 提示与消息样式，与登录页一致 */
.alert.error {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 14px;
}
.alert.success {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #e7f7e7;
  border: 1px solid #c6efc6;
  color: #14532d;
  font-size: 14px;
}

/* 注册提示，与登录页一致 */
.register-hint {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}
.register-hint a {
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}
.register-hint a:hover {
  text-decoration: underline;
}

/* 响应式微调 */
@media (max-width: 480px) {
  .login-card {
    padding: 24px;
  }
  .login-title {
    font-size: 22px;
  }
}
</style>