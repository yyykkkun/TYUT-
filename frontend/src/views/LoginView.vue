<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tab = ref<'login' | 'register'>('login')

// 登录表单
const loginAccount = ref('demo@mall.test')
const loginPassword = ref('123456')

// 注册表单
const regNickname = ref('')
const regPhone = ref('')
const regPassword = ref('')
const regConfirm = ref('')

const submitting = ref(false)
const errorMsg = ref('')

async function doLogin() {
  errorMsg.value = ''
  if (!loginAccount.value || !loginPassword.value) {
    errorMsg.value = '请填写账号和密码'
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await auth.login(loginAccount.value, loginPassword.value)
    router.push(String(route.query.redirect || '/member'))
  } catch (e: any) {
    errorMsg.value = e.message || '账号或密码错误'
  } finally {
    submitting.value = false
  }
}

async function doRegister() {
  errorMsg.value = ''
  if (!regNickname.value || !regPhone.value || !regPassword.value) {
    errorMsg.value = '请填写所有必填项'
    return
  }
  if (regPassword.value !== regConfirm.value) {
    errorMsg.value = '两次密码输入不一致'
    return
  }
  if (regPassword.value.length < 6) {
    errorMsg.value = '密码长度至少 6 位'
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await auth.register(regNickname.value, regPhone.value, regPassword.value)
    router.push(String(route.query.redirect || '/member'))
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <p class="eyebrow">校园优选商城</p>

      <!-- 登录 / 注册 切换 -->
      <div class="tab-row">
        <button :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
        <button :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</button>
      </div>

      <!-- 错误提示 -->
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <!-- ====== 登录表单 ====== -->
      <form v-if="tab === 'login'" class="form-grid" @submit.prevent="doLogin">
        <label>
          手机号 / 邮箱
          <input v-model="loginAccount" autocomplete="username" placeholder="demo@mall.test" />
        </label>
        <label>
          密码
          <input
            v-model="loginPassword"
            type="password"
            autocomplete="current-password"
            placeholder="输入密码"
          />
        </label>
        <button class="btn primary" type="submit" :disabled="submitting">
          {{ submitting ? '登录中...' : '登录' }}
        </button>
        <p class="hint">演示账号：demo@mall.test / 123456</p>
      </form>

      <!-- ====== 注册表单 ====== -->
      <form v-if="tab === 'register'" class="form-grid" @submit.prevent="doRegister">
        <label>
          昵称
          <input v-model="regNickname" autocomplete="nickname" placeholder="给自己取个名字" />
        </label>
        <label>
          手机号
          <input v-model="regPhone" autocomplete="tel" placeholder="用于登录和收货" />
        </label>
        <label>
          密码
          <input
            v-model="regPassword"
            type="password"
            autocomplete="new-password"
            placeholder="至少 6 位"
          />
        </label>
        <label>
          确认密码
          <input
            v-model="regConfirm"
            type="password"
            autocomplete="new-password"
            placeholder="再次输入密码"
          />
        </label>
        <button class="btn primary" type="submit" :disabled="submitting">
          {{ submitting ? '注册中...' : '注册并登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.tab-row {
  display: flex;
  gap: 0;
  margin-bottom: 1.25rem;
  border-bottom: 2px solid var(--border, #e5e7eb);
}
.tab-row button {
  flex: 1;
  padding: 0.6rem 1rem;
  border: none;
  background: transparent;
  font-size: 1rem;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.15s;
}
.tab-row button.active {
  color: var(--primary, #4f46e5);
  border-bottom-color: var(--primary, #4f46e5);
}
.error-msg {
  background: #fef2f2;
  color: #dc2626;
  padding: 0.6rem 0.9rem;
  border-radius: 6px;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}
.hint {
  font-size: 0.85rem;
  color: #9ca3af;
  text-align: center;
  margin: 0;
}
</style>
