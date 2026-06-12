<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tab = ref<'login' | 'register'>('login')
const backendAvailable = ref<boolean | null>(null)  // null=检测中
const checking = ref(false)

// 用公开接口检测后端是否可达
async function checkBackend() {
  checking.value = true
  backendAvailable.value = null
  try {
    const res = await fetch('/api/products?pageSize=1')
    backendAvailable.value = res.ok
    if (res.ok) {
      // 后端可达 → 如果是 mock 假 token，清掉
      const token = localStorage.getItem('mall-token') || ''
      if (token === 'mock-jwt-token-demo') {
        localStorage.removeItem('mall-token')
        localStorage.removeItem('mall-user-name')
        localStorage.removeItem('mall-user-account')
      }
    }
  } catch {
    backendAvailable.value = false
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  checkBackend()
})

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
      <h2 style="text-align: center; margin-bottom: 24px;">校园优选商城</h2>

      <a-alert v-if="errorMsg" :message="errorMsg" type="error" show-icon style="margin-bottom: 24px;" />

      <a-tabs v-model:activeKey="tab" centered>
        <a-tab-pane key="login" tab="登录">
          <a-form layout="vertical" @finish="doLogin">
            <a-form-item label="手机号 / 邮箱">
              <a-input v-model:value="loginAccount" size="large" placeholder="demo@mall.test" />
            </a-form-item>
            <a-form-item label="密码">
              <a-input-password v-model:value="loginPassword" size="large" placeholder="输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block :loading="submitting" @click.prevent="doLogin">
                登录
              </a-button>
            </a-form-item>
            <div style="text-align: center; color: #8c8c8c;">演示账号：demo@mall.test / 123456</div>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form layout="vertical" @finish="doRegister">
            <a-form-item label="昵称">
              <a-input v-model:value="regNickname" size="large" placeholder="给自己取个名字" />
            </a-form-item>
            <a-form-item label="手机号">
              <a-input v-model:value="regPhone" size="large" placeholder="用于登录和收货" />
            </a-form-item>
            <a-form-item label="密码">
              <a-input-password v-model:value="regPassword" size="large" placeholder="至少 6 位" />
            </a-form-item>
            <a-form-item label="确认密码">
              <a-input-password v-model:value="regConfirm" size="large" placeholder="再次输入密码" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block :loading="submitting" @click.prevent="doRegister">
                注册并登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 150px);
  padding: 40px 20px;
}
.auth-panel {
  width: 100%;
  max-width: 420px;
  background: #fff;
  padding: 40px 32px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}
</style>
