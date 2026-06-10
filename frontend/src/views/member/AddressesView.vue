<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useMemberStore } from '@/stores/member'
import { regions } from '@/data/regions'
import Autocomplete from '@/components/Autocomplete.vue'
import EmptyState from '@/components/EmptyState.vue'

const member = useMemberStore()
const errorMsg = ref('')
const successMsg = ref('')

const form = reactive({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

// Autocomplete 选中的 code
const provinceCode = ref('')
const cityCode = ref('')
const districtCode = ref('')

// 省份选项
const provinceOpts = computed(() => regions.map(r => ({ code: r.code, name: r.name })))

// 城市选项：按省份 code 筛选
const cityOpts = computed(() => {
  const p = regions.find(r => r.code === provinceCode.value)
  return (p?.children || []).map(c => ({ code: c.code, name: c.name }))
})

// 区县选项：按城市 code 筛选
const districtOpts = computed(() => {
  const c = regions.flatMap(r => r.children || []).find(c => c.code === cityCode.value)
  return (c?.children || []).map(d => ({ code: d.code, name: d.name }))
})

// 省份变更 → 重置城市、区县
function onProvinceChange() {
  cityCode.value = ''
  districtCode.value = ''
  form.city = ''
  form.district = ''
}
function onCityChange() {
  districtCode.value = ''
  form.district = ''
}

// 手机号校验
const PHONE_RE = /^1[3-9]\d{9}$/
function validatePhone(phone: string): boolean {
  return PHONE_RE.test(phone)
}

onMounted(() => {
  member.loadAddresses()
})

async function add() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.name.trim()) { errorMsg.value = '请填写收货人姓名'; return }
  if (!validatePhone(form.phone)) { errorMsg.value = '请填写正确的 11 位手机号'; return }
  if (!form.detail.trim()) { errorMsg.value = '请填写详细地址'; return }

  await member.addAddress({ ...form })

  // 重置表单
  form.name = ''
  form.phone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.detail = ''
  form.isDefault = false
  provinceCode.value = ''
  cityCode.value = ''
  districtCode.value = ''

  successMsg.value = '地址已保存'
  setTimeout(() => (successMsg.value = ''), 2000)
}
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>地址管理</h1>
      <p>新增、删除和设为默认地址。</p>
    </div>

    <!-- 已有地址 -->
    <section v-if="member.addresses.length" class="address-grid">
      <article v-for="addr in member.addresses" :key="addr.id" class="address-card" :class="{ selected: addr.isDefault }">
        <div class="addr-header">
          <strong>{{ addr.name }}</strong>
          <span class="phone">{{ addr.phone }}</span>
          <span v-if="addr.isDefault" class="default-badge">默认</span>
        </div>
        <p>{{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}</p>
        <div class="action-row">
          <button v-if="!addr.isDefault" class="btn small" type="button" @click="member.setDefaultAddress(addr.id)">设为默认</button>
          <button class="btn small subtle" type="button" @click="member.removeAddress(addr.id)">删除</button>
        </div>
      </article>
    </section>

    <EmptyState v-else title="暂无收货地址" />

    <!-- 新增地址 -->
    <section class="panel">
      <h2>新增地址</h2>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

      <form class="address-form" @submit.prevent="add">
        <div class="form-row">
          <label>
            收货人
            <input v-model="form.name" placeholder="姓名" />
          </label>
          <label>
            手机号
            <input v-model="form.phone" placeholder="11 位手机号" maxlength="11" :class="{ invalid: form.phone && !validatePhone(form.phone) }" />
          </label>
        </div>

        <div class="form-row three">
          <label>
            省 / 直辖市
            <Autocomplete
              v-model="form.province"
              :options="provinceOpts"
              placeholder="输入或选择省份"
              @update:code="provinceCode = $event; onProvinceChange()"
            />
          </label>
          <label>
            市 / 区
            <Autocomplete
              v-model="form.city"
              :options="cityOpts"
              placeholder="输入或选择城市"
              @update:code="cityCode = $event; onCityChange()"
            />
          </label>
          <label>
            区 / 县
            <Autocomplete
              v-model="form.district"
              :options="districtOpts"
              placeholder="输入或选择区县"
              @update:code="districtCode = $event"
            />
          </label>
        </div>

        <label>
          详细地址
          <input v-model="form.detail" placeholder="街道、小区、门牌号等" />
        </label>

        <label class="checkbox-line">
          <input v-model="form.isDefault" type="checkbox" /> 设为默认地址
        </label>

        <button class="btn primary" type="submit">保存地址</button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: .75rem;
  margin-bottom: .75rem;
}
.form-row.three {
  grid-template-columns: 1fr 1fr 1fr;
}
input.invalid {
  border-color: #dc2626 !important;
  background: #fef2f2;
}
.error-msg {
  background: #fef2f2;
  color: #dc2626;
  padding: .6rem .9rem;
  border-radius: 6px;
  font-size: .9rem;
  margin-bottom: 1rem;
}
.success-msg {
  background: #f0fdf4;
  color: #16a34a;
  padding: .6rem .9rem;
  border-radius: 6px;
  font-size: .9rem;
  margin-bottom: 1rem;
}
.default-badge {
  background: var(--primary, #4f46e5);
  color: #fff;
  font-size: .72rem;
  padding: .15rem .5rem;
  border-radius: 4px;
  margin-left: .5rem;
}
.addr-header {
  display: flex;
  align-items: center;
  gap: .5rem;
  margin-bottom: .35rem;
}
.phone { color: #6b7280; font-size: .9rem; }
</style>
