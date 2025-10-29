<template>
  <div class="apikey-management-container">
    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <button @click="goBack" class="back-button">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回首页
      </button>
      <h1 class="page-title">API密钥管理</h1>
      <div class="spacer"></div>
    </div>

    <!-- 统计信息卡片 -->
    <div class="stats-container">
      <div class="stat-card">
        <div class="stat-label">总密钥数</div>
        <div class="stat-value">{{ statistics.totalKeys || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">活跃密钥</div>
        <div class="stat-value success">{{ statistics.activeKeys || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">总请求数</div>
        <div class="stat-value">{{ formatNumber(statistics.totalRequests) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日请求</div>
        <div class="stat-value">{{ formatNumber(statistics.todayRequests) }}</div>
      </div>
    </div>

    <!-- 操作区域 -->
    <div class="actions-bar">
      <button @click="openCreateModal" class="btn-primary">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        创建新密钥
      </button>
      <div class="search-box">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索密钥名称..."
          @input="handleSearch"
        />
      </div>
    </div>

    <!-- 密钥列表 -->
    <div class="apikey-list">
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <div v-else-if="filteredApiKeys.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
        </svg>
        <p>{{ searchQuery ? '未找到匹配的密钥' : '暂无API密钥，点击上方按钮创建' }}</p>
      </div>

      <div v-else class="apikey-cards">
        <div v-for="apiKey in filteredApiKeys" :key="apiKey.id" class="apikey-card">
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="key-info">
              <h3 class="key-name">{{ apiKey.keyName }}</h3>
              <span :class="['status-badge', apiKey.status.toLowerCase()]">
                {{ getStatusText(apiKey.status) }}
              </span>
            </div>
            <div class="card-actions">
              <button @click="toggleApiKeyStatus(apiKey)" class="btn-icon" :title="apiKey.status === 'ACTIVE' ? '禁用' : '启用'">
                <svg v-if="apiKey.status === 'ACTIVE'" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="15" y1="9" x2="9" y2="15"></line>
                  <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              </button>
              <button @click="openEditModal(apiKey)" class="btn-icon" title="编辑">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
              <button @click="confirmDelete(apiKey)" class="btn-icon btn-danger" title="删除">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            </div>
          </div>

          <!-- 密钥显示 -->
          <div class="key-display">
            <code class="key-value">{{ maskApiKey(apiKey.keyPrefix) }}</code>
            <button @click="copyToClipboard(maskApiKey(apiKey.keyPrefix))" class="btn-copy" title="复制">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
            </button>
          </div>

          <!-- 详细信息 -->
          <div class="key-details">
            <div class="detail-row">
              <span class="detail-label">权限范围:</span>
              <span class="detail-value">
                <span v-for="scope in apiKey.scopes" :key="scope" class="scope-tag">{{ scope }}</span>
              </span>
            </div>
            <div class="detail-row" v-if="apiKey.appName">
              <span class="detail-label">应用名称:</span>
              <span class="detail-value">{{ apiKey.appName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">请求限制:</span>
              <span class="detail-value">{{ apiKey.rateLimitPerMinute || '-' }}/分钟, {{ apiKey.rateLimitPerDay || '-' }}/天</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">总请求数:</span>
              <span class="detail-value">{{ formatNumber(apiKey.totalRequestCount) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间:</span>
              <span class="detail-value">{{ formatDate(apiKey.createdAt) }}</span>
            </div>
            <div class="detail-row" v-if="apiKey.expireAt">
              <span class="detail-label">过期时间:</span>
              <span class="detail-value" :class="{ 'text-danger': isExpiringSoon(apiKey.expireAt) }">
                {{ formatDate(apiKey.expireAt) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑密钥弹窗 -->
    <div v-if="isModalVisible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ modalMode === 'create' ? '创建API密钥' : '编辑API密钥' }}</h2>
          <button @click="closeModal" class="btn-close">×</button>
        </div>
        
        <form @submit.prevent="handleFormSubmit" class="modal-form">
          <div class="form-group">
            <label for="keyName">密钥名称 *</label>
            <input 
              id="keyName" 
              v-model="formData.keyName" 
              type="text" 
              placeholder="例如：生产环境密钥" 
              required
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label for="appName">应用名称</label>
            <input 
              id="appName" 
              v-model="formData.appName" 
              type="text" 
              placeholder="例如：我的聊天应用"
              maxlength="100"
            />
          </div>

          <div class="form-group">
            <label>权限范围 *</label>
            <div class="checkbox-group">
              <label class="checkbox-item">
                <input type="checkbox" value="chat" v-model="formData.scopes" />
                <span>聊天 (chat)</span>
              </label>
              <label class="checkbox-item">
                <input type="checkbox" value="image" v-model="formData.scopes" />
                <span>图像生成 (image)</span>
              </label>
              <label class="checkbox-item">
                <input type="checkbox" value="tts" v-model="formData.scopes" />
                <span>语音合成 (tts)</span>
              </label>
              <label class="checkbox-item">
                <input type="checkbox" value="asr" v-model="formData.scopes" />
                <span>语音识别 (asr)</span>
              </label>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="rateLimitPerMinute">每分钟限制</label>
              <input 
                id="rateLimitPerMinute" 
                v-model.number="formData.rateLimitPerMinute" 
                type="number" 
                placeholder="60"
                min="1"
              />
            </div>
            <div class="form-group">
              <label for="rateLimitPerDay">每日限制</label>
              <input 
                id="rateLimitPerDay" 
                v-model.number="formData.rateLimitPerDay" 
                type="number" 
                placeholder="10000"
                min="1"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="ipWhitelist">IP白名单（可选）</label>
            <input 
              id="ipWhitelist" 
              v-model="ipWhitelistInput" 
              type="text" 
              placeholder="多个IP用逗号分隔，如：192.168.1.1,192.168.1.2"
            />
            <small class="form-hint">留空表示不限制IP访问</small>
          </div>

          <div class="form-group">
            <label for="expireAt">过期时间（可选）</label>
            <input 
              id="expireAt" 
              v-model="formData.expireAt" 
              type="datetime-local"
            />
            <small class="form-hint">留空表示永不过期</small>
          </div>

          <div class="form-actions">
            <button type="button" @click="closeModal" class="btn-secondary">取消</button>
            <button type="submit" class="btn-primary" :disabled="isSubmitting">
              {{ isSubmitting ? '处理中...' : (modalMode === 'create' ? '创建' : '保存') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 首次创建成功弹窗（显示完整密钥） -->
    <div v-if="isSuccessModalVisible" class="modal-overlay" @click.self="closeSuccessModal">
      <div class="modal-content success-modal">
        <div class="modal-header success-header">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="success-icon">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
          <h2>API密钥创建成功！</h2>
        </div>
        
        <div class="success-content">
          <div class="warning-box">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <div>
              <strong>重要提示：</strong>
              <p>这是您唯一一次看到完整密钥的机会，请立即复制并妥善保管。关闭此窗口后，密钥将被永久隐藏。</p>
            </div>
          </div>

          <div class="key-display-full">
            <label>您的API密钥：</label>
            <div class="key-copy-group">
              <code class="full-key">{{ newApiKey }}</code>
              <button @click="copyFullKey" class="btn-copy-large">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                </svg>
                {{ copiedFull ? '已复制！' : '复制密钥' }}
              </button>
            </div>
          </div>
        </div>

        <div class="success-footer">
          <button @click="closeSuccessModal" class="btn-primary btn-large">
            我已保存，关闭窗口
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 提示 -->
    <div v-if="toast.show" :class="['toast', toast.type]">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import apiKeyService from '../services/apikey';

const router = useRouter();

// 数据
const apiKeys = ref([]);
const statistics = ref({});
const isLoading = ref(false);
const searchQuery = ref('');

// 模态框状态
const isModalVisible = ref(false);
const isSuccessModalVisible = ref(false);
const modalMode = ref('create'); // 'create' | 'edit'
const isSubmitting = ref(false);
const newApiKey = ref('');
const copiedFull = ref(false);

// 表单数据
const formData = ref({
  id: null,
  keyName: '',
  appName: '',
  scopes: [],
  rateLimitPerMinute: null,
  rateLimitPerDay: null,
  ipWhitelist: [],
  expireAt: null
});

const ipWhitelistInput = ref('');

// Toast提示
const toast = ref({
  show: false,
  message: '',
  type: 'success' // 'success' | 'error' | 'info'
});

// 计算属性：过滤后的密钥列表
const filteredApiKeys = computed(() => {
  if (!searchQuery.value) {
    return apiKeys.value;
  }
  const query = searchQuery.value.toLowerCase();
  return apiKeys.value.filter(key => 
    key.keyName.toLowerCase().includes(query) ||
    (key.appName && key.appName.toLowerCase().includes(query))
  );
});

// 生命周期
onMounted(() => {
  loadApiKeys();
  loadStatistics();
});

// 方法
async function loadApiKeys() {
  isLoading.value = true;
  try {
    const response = await apiKeyService.listApiKeys();
    if (response.data.success) {
      apiKeys.value = response.data.data || [];
    } else {
      showToast(response.data.message || '加载失败', 'error');
    }
  } catch (error) {
    console.error('加载API密钥失败:', error);
    showToast('加载API密钥失败', 'error');
  } finally {
    isLoading.value = false;
  }
}

async function loadStatistics() {
  try {
    const response = await apiKeyService.getStatistics();
    if (response.data.success) {
      statistics.value = response.data.data || {};
    }
  } catch (error) {
    console.error('加载统计信息失败:', error);
  }
}

function goBack() {
  router.push('/characters');
}

function openCreateModal() {
  modalMode.value = 'create';
  resetForm();
  isModalVisible.value = true;
}

function openEditModal(apiKey) {
  modalMode.value = 'edit';
  formData.value = {
    id: apiKey.id,
    keyName: apiKey.keyName,
    appName: apiKey.appName || '',
    scopes: [...apiKey.scopes],
    rateLimitPerMinute: apiKey.rateLimitPerMinute,
    rateLimitPerDay: apiKey.rateLimitPerDay,
    ipWhitelist: apiKey.ipWhitelist || [],
    expireAt: apiKey.expireAt ? formatDateForInput(apiKey.expireAt) : null
  };
  ipWhitelistInput.value = (apiKey.ipWhitelist || []).join(', ');
  isModalVisible.value = true;
}

function closeModal() {
  isModalVisible.value = false;
  resetForm();
}

function closeSuccessModal() {
  isSuccessModalVisible.value = false;
  newApiKey.value = '';
  copiedFull.value = false;
}

function resetForm() {
  formData.value = {
    id: null,
    keyName: '',
    appName: '',
    scopes: [],
    rateLimitPerMinute: null,
    rateLimitPerDay: null,
    ipWhitelist: [],
    expireAt: null
  };
  ipWhitelistInput.value = '';
}

async function handleFormSubmit() {
  if (formData.value.scopes.length === 0) {
    showToast('请至少选择一个权限范围', 'error');
    return;
  }

  isSubmitting.value = true;
  try {
    // 处理IP白名单
    if (ipWhitelistInput.value) {
      formData.value.ipWhitelist = ipWhitelistInput.value
        .split(',')
        .map(ip => ip.trim())
        .filter(ip => ip);
    } else {
      formData.value.ipWhitelist = [];
    }

    let response;
    if (modalMode.value === 'create') {
      response = await apiKeyService.createApiKey(formData.value);
      if (response.data.success) {
        newApiKey.value = response.data.data.apiKey;
        isSuccessModalVisible.value = true;
        closeModal();
        await loadApiKeys();
        await loadStatistics();
      } else {
        showToast(response.data.message || '创建失败', 'error');
      }
    } else {
      response = await apiKeyService.updateApiKey(formData.value);
      if (response.data.success) {
        showToast('更新成功', 'success');
        closeModal();
        await loadApiKeys();
      } else {
        showToast(response.data.message || '更新失败', 'error');
      }
    }
  } catch (error) {
    console.error('操作失败:', error);
    showToast(error.response?.data?.message || '操作失败', 'error');
  } finally {
    isSubmitting.value = false;
  }
}

async function toggleApiKeyStatus(apiKey) {
  try {
    if (apiKey.status === 'ACTIVE') {
      const response = await apiKeyService.disableApiKey(apiKey.id);
      if (response.data.success) {
        showToast('密钥已禁用', 'success');
        await loadApiKeys();
      } else {
        showToast(response.data.message || '禁用失败', 'error');
      }
    } else {
      const response = await apiKeyService.enableApiKey(apiKey.id);
      if (response.data.success) {
        showToast('密钥已启用', 'success');
        await loadApiKeys();
      } else {
        showToast(response.data.message || '启用失败', 'error');
      }
    }
  } catch (error) {
    console.error('切换状态失败:', error);
    showToast('操作失败', 'error');
  }
}

function confirmDelete(apiKey) {
  if (confirm(`确定要删除密钥 "${apiKey.keyName}" 吗？此操作不可恢复。`)) {
    deleteApiKey(apiKey.id);
  }
}

async function deleteApiKey(id) {
  try {
    const response = await apiKeyService.deleteApiKey(id);
    if (response.data.success) {
      showToast('删除成功', 'success');
      await loadApiKeys();
      await loadStatistics();
    } else {
      showToast(response.data.message || '删除失败', 'error');
    }
  } catch (error) {
    console.error('删除失败:', error);
    showToast('删除失败', 'error');
  }
}

function handleSearch() {
  // 搜索逻辑由计算属性自动处理
}

// 工具函数
function maskApiKey(keyPrefix) {
  // 格式：sk-nv-xxxxxxxx，显示 sk-nv-****xxxx（保留前缀和后4位）
  if (!keyPrefix) return '****';
  
  // 假设密钥格式为 sk-nv-xxxxxxxxxxxxxxxx
  // 保留前缀 sk-nv- 和后4位，中间用*替代
  const parts = keyPrefix.split('-');
  if (parts.length >= 3) {
    const lastPart = parts[parts.length - 1];
    if (lastPart.length > 4) {
      const masked = lastPart.slice(0, -4).replace(/./g, '*') + lastPart.slice(-4);
      parts[parts.length - 1] = masked;
    }
    return parts.join('-');
  }
  
  return keyPrefix;
}

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    showToast('已复制到剪贴板', 'success');
  }).catch(err => {
    console.error('复制失败:', err);
    showToast('复制失败', 'error');
  });
}

function copyFullKey() {
  navigator.clipboard.writeText(newApiKey.value).then(() => {
    copiedFull.value = true;
    showToast('密钥已复制到剪贴板', 'success');
    setTimeout(() => {
      copiedFull.value = false;
    }, 2000);
  }).catch(err => {
    console.error('复制失败:', err);
    showToast('复制失败', 'error');
  });
}

function formatNumber(num) {
  if (!num) return '0';
  return Number(num).toLocaleString();
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatDateForInput(dateStr) {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function isExpiringSoon(dateStr) {
  if (!dateStr) return false;
  const expireDate = new Date(dateStr);
  const now = new Date();
  const diffDays = (expireDate - now) / (1000 * 60 * 60 * 24);
  return diffDays < 7 && diffDays > 0;
}

function getStatusText(status) {
  const statusMap = {
    'ACTIVE': '正常',
    'DISABLED': '已禁用',
    'EXPIRED': '已过期'
  };
  return statusMap[status] || status;
}

function showToast(message, type = 'success') {
  toast.value = { show: true, message, type };
  setTimeout(() => {
    toast.value.show = false;
  }, 3000);
}
</script>

<style scoped>
@import '../assets/apikey-styles.css';
</style>
