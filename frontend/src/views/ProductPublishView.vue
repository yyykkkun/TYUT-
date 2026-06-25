<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UploadOutlined } from '@ant-design/icons-vue'
import { fetchCategories, publishProduct } from '@/api/products'
import { uploadFile } from '@/api/upload'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types/domain'

const router = useRouter()
const auth = useAuthStore()

const categories = ref<Category[]>([])
const submitting = ref(false)
const uploading = ref(false)
const error = ref('')

const form = ref({
  title: '',
  subtitle: '',
  categoryId: '',
  price: 0,
  stock: 1,
  image: '',
  city: '太原',
  condition: '九成新',
  listingType: 'idle' as 'idle' | 'urgent' | 'campus',
  specsText: '默认',
  tagsText: '同校交易,可当面验货',
})

const canSubmit = computed(
  () =>
    Boolean(form.value.title.trim()) &&
    Boolean(form.value.categoryId) &&
    form.value.price > 0 &&
    form.value.stock > 0 &&
    Boolean(form.value.image),
)

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login?redirect=/products/new')
    return
  }
  categories.value = await fetchCategories()
})

async function handleImageChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  error.value = ''
  try {
    form.value.image = await uploadFile(file)
  } catch (err: any) {
    error.value = err?.message || '图片上传失败'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function splitText(value: string): string[] {
  return value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

async function submit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    const product = await publishProduct({
      title: form.value.title.trim(),
      subtitle: form.value.subtitle.trim(),
      categoryId: Number(form.value.categoryId),
      price: Number(form.value.price),
      stock: Number(form.value.stock),
      image: form.value.image,
      city: form.value.city.trim(),
      condition: form.value.condition,
      listingType: form.value.listingType,
      specs: splitText(form.value.specsText),
      tags: splitText(form.value.tagsText),
    })
    router.push(`/products/${product.id}`)
  } catch (err: any) {
    error.value = err?.message || '发布失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="stack-page publish-page">
    <div class="page-title row">
      <div>
        <p class="eyebrow">个人发布</p>
        <h1>发布闲置商品</h1>
        <p>每个用户都可以作为卖家发布商品，也可以作为买家下单购买。</p>
      </div>
      <a-button @click="$router.push('/member/listings')">我的发布</a-button>
    </div>

    <a-alert
      v-if="error"
      class="publish-alert"
      type="error"
      show-icon
      :message="error"
    />

    <section class="publish-grid">
      <a-card :bordered="false" class="publish-card">
        <a-form layout="vertical" @submit.prevent="submit">
          <a-form-item label="商品标题" required>
            <a-input v-model:value="form.title" size="large" placeholder="例如：九成新 Kindle Paperwhite" />
          </a-form-item>

          <a-form-item label="商品描述">
            <a-textarea
              v-model:value="form.subtitle"
              :rows="4"
              placeholder="描述成色、购入时间、交易方式和注意事项"
            />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="分类" required>
                <a-select v-model:value="form.categoryId" size="large" placeholder="选择分类">
                  <a-select-option v-for="category in categories" :key="category.id" :value="category.id">
                    {{ category.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="所在城市">
                <a-input v-model:value="form.city" size="large" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="价格" required>
                <a-input-number
                  v-model:value="form.price"
                  :min="0.01"
                  :precision="2"
                  size="large"
                  style="width: 100%"
                  addon-before="￥"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="数量" required>
                <a-input-number v-model:value="form.stock" :min="1" size="large" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="成色">
                <a-select v-model:value="form.condition" size="large">
                  <a-select-option value="全新">全新</a-select-option>
                  <a-select-option value="九成新">九成新</a-select-option>
                  <a-select-option value="八成新">八成新</a-select-option>
                  <a-select-option value="有使用痕迹">有使用痕迹</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="发布类型">
                <a-select v-model:value="form.listingType" size="large">
                  <a-select-option value="idle">普通闲置</a-select-option>
                  <a-select-option value="campus">同校好物</a-select-option>
                  <a-select-option value="urgent">急出</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="规格">
            <a-input v-model:value="form.specsText" size="large" placeholder="多个规格用逗号分隔，例如：默认,带保护套" />
          </a-form-item>

          <a-form-item label="标签">
            <a-input v-model:value="form.tagsText" size="large" placeholder="多个标签用逗号分隔" />
          </a-form-item>

          <a-form-item label="商品图片" required>
            <label class="upload-box">
              <input type="file" accept="image/*" @change="handleImageChange" />
              <span v-if="!form.image">
                <UploadOutlined />
                {{ uploading ? '上传中...' : '上传商品图片' }}
              </span>
              <img v-else :src="form.image" alt="商品图片预览" />
            </label>
          </a-form-item>

          <div class="publish-actions">
            <a-button size="large" @click="$router.back()">取消</a-button>
            <a-button type="primary" size="large" :disabled="!canSubmit" :loading="submitting" @click="submit">
              发布商品
            </a-button>
          </div>
        </a-form>
      </a-card>

      <aside class="publish-tips">
        <h2>发布规则</h2>
        <p>请确保商品信息真实、图片清晰。建议优先选择同校当面交易，沟通时确认价格、地点和验货方式。</p>
        <ul>
          <li>同一账号既可以买，也可以卖。</li>
          <li>发布后商品会立即进入闲置市场。</li>
          <li>买家可加购、下单并通过消息沟通。</li>
        </ul>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.publish-alert {
  margin-bottom: 16px;
}
.publish-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}
.publish-card,
.publish-tips {
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}
.publish-tips {
  padding: 22px;
  background: var(--surface);
  border-radius: var(--radius-lg);
}
.publish-tips h2 {
  margin: 0 0 10px;
}
.publish-tips p,
.publish-tips li {
  color: var(--ink-muted);
}
.upload-box {
  display: grid;
  place-items: center;
  min-height: 220px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-lg);
  background: var(--surface-soft);
  cursor: pointer;
  overflow: hidden;
}
.upload-box input {
  display: none;
}
.upload-box span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--primary);
  font-weight: 800;
}
.upload-box img {
  width: 100%;
  height: 260px;
  object-fit: cover;
}
.publish-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
@media (max-width: 900px) {
  .publish-grid {
    grid-template-columns: 1fr;
  }
}
</style>
