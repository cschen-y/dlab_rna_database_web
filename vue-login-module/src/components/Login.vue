<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">登录</h1>
      <p class="login-subtitle">欢迎回来，请输入账号信息</p>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" required />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" required />
        </div>
        <button class="btn-primary" type="submit">登录</button>
        <p class="register-hint">还没有账号？<a @click="goToRegister" href="javascript:void(0)">立即注册</a></p>
      </form>
      <p v-if="errorMessage" class="alert error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import { useRouter } from "vue-router";

export default {
  data() {
    return {
      username: "",
      password: "",
      errorMessage: "",
    };
  },
  setup() {
    const router = useRouter();
    return { router };
  },
  methods: {
    handleLogin() {
      axios
        .post("/api/auth/login", {
          username: this.username,
          password: this.password,
        })
        .then((response) => {
          if (response.data.token) {
            localStorage.setItem("token", response.data.token);
            this.$router.push("/upload");
          } else {
            this.errorMessage = response.data.message || "登录失败，请稍后重试";
          }
        })
        .catch((error) => {
          this.errorMessage = "登录失败，请稍后重试";
          console.error(error);
        });
    },
    goToRegister() {
      this.$router.push("/register");
    },
  },
};
</script>

<style scoped>
/* 页面居中与背景 */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef3f9 100%);
}

/* 登录卡片 */
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

/* 表单元素 */
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

/* 按钮 */
.btn-primary {
   width: 20%;
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

/* 错误提醒 */
.alert.error {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 14px;
}

/* 注册提示 */
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
  /* 居中整表单内容 */
  .login-card form {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
</style>