<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useMemberStore } from '@/stores/member'
import { 
  TrophyOutlined, 
  GiftOutlined, 
  HistoryOutlined, 
  ThunderboltOutlined,
  CheckCircleOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const member = useMemberStore()
const activeTab = ref('coupons')
const exchanging = ref<string | null>(null)

// 模拟优惠券奖励
const couponRewards = [
  { id: 'r1', title: '5元全场通用券', amount: 5, threshold: 0, cost: 200, desc: '无门槛立减' },
  { id: 'r2', title: '10元全场通用券', amount: 10, threshold: 60, cost: 500, desc: '满60元可用' },
  { id: 'r3', title: '30元全场通用券', amount: 30, threshold: 200, cost: 1200, desc: '满200元可用' },
  { id: 'r4', title: '50元生鲜专享券', amount: 50, threshold: 300, cost: 1800, desc: '满300元可用' },
]

// 模拟实物奖励 (占位)
const physicalRewards = [
  { id: 'g1', title: '限定环保帆布袋', cost: 2000, image: 'https://images.unsplash.com/photo-1544816155-12df9643f363?q=80&w=200&h=200&fit=crop', stock: 12 },
  { id: 'g2', title: '品牌不锈钢水杯', cost: 3500, image: 'https://images.unsplash.com/photo-1589365278144-c9e705f843ba?q=80&w=200&h=200&fit=crop', stock: 5 },
]

// 模拟积分任务
const pointsTasks = [
  { id: 't1', title: '每日签到', reward: '+10', icon: 'CheckCircleOutlined', done: true },
  { id: 't2', title: '完善个人资料', reward: '+50', icon: 'TrophyOutlined', done: false },
  { id: 't3', title: '评价购买商品', reward: '+20', icon: 'ThunderboltOutlined', done: false },
  { id: 't4', title: '邀请好友注册', reward: '+100', icon: 'GiftOutlined', done: false },
]

onMounted(async () => {
  member.syncLocal()
  await member.loadProfile()
})

async function handleExchange(reward: typeof couponRewards[0]) {
  if (member.points < reward.cost) {
    message.warning('您的积分不足，去做任务赚取积分吧！')
    return
  }

  exchanging.value = reward.id
  try {
    const ok = await member.exchangeCoupon(reward.cost)
    if (ok) {
      message.success(`成功兑换 ${reward.title}！`)
    }
  } catch (error) {
    message.error('兑换失败，请稍后再试')
  } finally {
    exchanging.value = null
  }
}
</script>

<template>
  <main class="points-mall">
    <!-- 积分看板 -->
    <header class="points-header">
      <div class="header-content">
        <div class="points-info">
          <span class="label">当前可用积分</span>
          <div class="points-value">
            <span class="number">{{ member.points }}</span>
            <a-tag color="gold" class="level-tag">
              <template #icon><trophy-outlined /></template>
              {{ member.level }}
            </a-tag>
          </div>
        </div>
        <div class="points-stats">
          <div class="stat-item">
            <span class="stat-label">已省金额</span>
            <span class="stat-value">¥128.00</span>
          </div>
          <a-divider type="vertical" style="height: 32px; background: rgba(255,255,255,0.2)" />
          <div class="stat-item">
            <span class="stat-label">可用优惠券</span>
            <span class="stat-value">{{ member.availableCoupons.length }}张</span>
          </div>
        </div>
      </div>
    </header>

    <div class="page-body">
      <!-- 任务激励栏 -->
      <section class="tasks-section card-shadow">
        <div class="section-header">
          <h3>赚取积分</h3>
          <a-button type="link" size="small">查看更多 <right-outlined /></a-button>
        </div>
        <div class="tasks-grid">
          <div v-for="task in pointsTasks" :key="task.id" class="task-item" :class="{ 'is-done': task.done }">
            <div class="task-icon">
              <component :is="task.icon" />
            </div>
            <div class="task-info">
              <span class="task-title">{{ task.title }}</span>
              <span class="task-reward">{{ task.reward }}</span>
            </div>
            <a-button size="small" :type="task.done ? 'default' : 'primary'" shape="round" :disabled="task.done">
              {{ task.done ? '已完成' : '去完成' }}
            </a-button>
          </div>
        </div>
      </section>

      <!-- 兑换中心 -->
      <section class="exchange-section">
        <a-tabs v-model:activeKey="activeTab" class="custom-tabs">
          <a-tab-pane key="coupons">
            <template #tab>
              <span><thunderbolt-outlined /> 优惠券兑换</span>
            </template>
            <div class="rewards-grid">
              <a-card v-for="reward in couponRewards" :key="reward.id" class="coupon-card" :bordered="false">
                <div class="coupon-amount">
                  <span class="currency">¥</span>
                  <span class="value">{{ reward.amount }}</span>
                </div>
                <div class="coupon-detail">
                  <h4 class="title">{{ reward.title }}</h4>
                  <p class="desc">{{ reward.desc }}</p>
                  <div class="cost">
                    <span class="points-cost">{{ reward.cost }} 积分</span>
                    <a-button 
                      type="primary" 
                      size="small" 
                      shape="round" 
                      :loading="exchanging === reward.id"
                      @click="handleExchange(reward)"
                    >
                      兑换
                    </a-button>
                  </div>
                </div>
                <div class="coupon-edge"></div>
              </a-card>
            </div>
          </a-tab-pane>

          <a-tab-pane key="gifts">
            <template #tab>
              <span><gift-outlined /> 实物礼品</span>
            </template>
            <div class="gifts-grid">
              <a-card v-for="gift in physicalRewards" :key="gift.id" hoverable class="gift-card">
                <template #cover>
                  <img :alt="gift.title" :src="gift.image" />
                </template>
                <a-card-meta :title="gift.title">
                  <template #description>
                    <div class="gift-footer">
                      <span class="points-cost">{{ gift.cost }} 积分</span>
                      <a-tag v-if="gift.stock < 10" color="orange">仅剩 {{ gift.stock }}</a-tag>
                    </div>
                    <a-button block type="primary" ghost style="margin-top: 12px;">兑换</a-button>
                  </template>
                </a-card-meta>
              </a-card>
            </div>
          </a-tab-pane>

          <a-tab-pane key="history">
            <template #tab>
              <span><history-outlined /> 兑换记录</span>
            </template>
            <a-empty description="暂无兑换记录" style="margin-top: 40px;" />
          </a-tab-pane>
        </a-tabs>
      </section>
    </div>
  </main>
</template>

<style scoped>
.points-mall {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 40px;
}

/* 顶部看板 */
.points-header {
  background: linear-gradient(135deg, #1677ff 0%, #003eb3 100%);
  color: white;
  padding: 40px 24px 80px;
  position: relative;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.points-info .label {
  font-size: 14px;
  opacity: 0.8;
  display: block;
  margin-bottom: 8px;
}

.points-value {
  display: flex;
  align-items: center;
  gap: 16px;
}

.points-value .number {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
}

.level-tag {
  background: rgba(255, 215, 0, 0.2) !important;
  border-color: rgba(255, 215, 0, 0.4) !important;
  color: #ffd700 !important;
  font-weight: 600;
  padding: 2px 10px;
}

.points-stats {
  display: flex;
  gap: 24px;
  align-items: center;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

/* 页面内容区 */
.page-body {
  max-width: 1200px;
  margin: -40px auto 0;
  padding: 0 24px;
  position: relative;
}

.card-shadow {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

/* 任务栏 */
.tasks-section {
  padding: 20px 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.tasks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f0f5ff;
  border-radius: 8px;
  transition: all 0.3s;
}

.task-item.is-done {
  opacity: 0.7;
  background: #f5f5f5;
}

.task-icon {
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
  font-size: 18px;
}

.task-info {
  flex: 1;
}

.task-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.task-reward {
  font-size: 12px;
  color: #ff4d4f;
  font-weight: 600;
}

/* 兑换中心 */
.exchange-section {
  background: white;
  border-radius: 12px;
  padding: 8px 24px 24px;
}

.rewards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding-top: 16px;
}

.coupon-card {
  background: #fffbe6;
  border-left: 4px solid #faad14;
  display: flex;
  padding: 0;
  overflow: hidden;
  position: relative;
}

:deep(.ant-card-body) {
  padding: 16px;
  display: flex;
  width: 100%;
}

.coupon-amount {
  width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px dashed #ffe58f;
  padding-right: 12px;
}

.coupon-amount .currency {
  font-size: 14px;
  color: #d48806;
}

.coupon-amount .value {
  font-size: 32px;
  font-weight: 700;
  color: #d48806;
  line-height: 1;
}

.coupon-detail {
  flex: 1;
  padding-left: 16px;
}

.coupon-detail .title {
  margin: 0 0 4px;
  font-size: 15px;
  color: #262626;
}

.coupon-detail .desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.coupon-detail .cost {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.points-cost {
  color: #f5222d;
  font-weight: 600;
  font-size: 15px;
}

/* 实物卡片 */
.gifts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  padding-top: 16px;
}

.gift-card img {
  height: 180px;
  object-fit: crop;
}

.gift-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .points-stats {
    width: 100%;
    justify-content: space-around;
  }
}
</style>
