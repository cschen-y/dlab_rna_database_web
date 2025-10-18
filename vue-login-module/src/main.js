import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import App from "./App.vue";
import Login from "./components/Login.vue";
import Register from "./components/Register.vue";
import HelloWorld from "./components/HelloWorld.vue";

const routes = [
  { path: "/", component: Login },
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/hello", component: HelloWorld, meta: { requiresAuth: true } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// JWT 有效性校验：存在且未过期
function isTokenValid(token) {
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    if (payload.exp && Math.floor(Date.now() / 1000) < payload.exp) {
      return true;
    }
    return false;
  } catch (e) {
    return false;
  }
}

// 路由守卫：未登录访问受保护页面则跳转登录
const publicPaths = ["/", "/login", "/register"];
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");
  const isPublic = publicPaths.includes(to.path);
  if (!isPublic && !isTokenValid(token)) {
    next({ path: "/login", query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

const app = createApp(App);
app.use(router);
app.mount("#app");