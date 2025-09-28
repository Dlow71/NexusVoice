<template>
  <div class="selection-container">
    <div v-if="user" class="user-info-bar">
      <span class="welcome-text">欢迎, {{ user.nickname || '用户' }}</span>
      <button @click="handleLogout" class="logout-button">退出登录</button>
    </div>

    <header class="header">
      <h1 class="title">Nexus Voice</h1>
      <p class="subtitle">选择一位角色，开启对话</p>
    </header>

    <div class="controls-container">
      <div class="tabs">
        <button :class="{ active: activeTab === 'public' }" @click="selectTab('public')">公共角色</button>
        <button :class="{ active: activeTab === 'private' }" @click="selectTab('private')">我的角色</button>
      </div>
      <div class="search-bar">
        <input type="text" v-model="searchQuery" placeholder="搜索角色..." />
      </div>
    </div>

    <main class="card-grid">
      <div v-if="isLoading" class="loading-state">正在加载角色...</div>
      <div v-else-if="characters.length === 0 && activeTab !== 'private'" class="empty-state">没有找到匹配的角色。</div>
      <template v-else>
        <div v-if="activeTab === 'private'" class="character-card-wrapper">
          <button @click="openModalForCreate" class="character-card create-new-card">
            <span class="create-icon">+</span>
            <span class="create-text">创建新角色</span>
          </button>
        </div>
        <div v-for="character in characters" :key="character.id" class="character-card-wrapper">
          <CharacterCard
              :character="{ ...character, avatar: character.avatarUrl }"
              :is-private="activeTab === 'private'"
              @edit="openModalForEdit"
              @delete="handleDelete"
          />
        </div>
      </template>
    </main>

    <footer v-if="!isLoading && pagination.total > pagination.size" class="pagination">
      <button @click="changePage(pagination.page - 1)" :disabled="pagination.page <= 1">&lt;</button>
      <span>第 {{ pagination.page }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="changePage(pagination.page + 1)" :disabled="pagination.page >= totalPages">&gt;</button>
    </footer>

    <div v-if="isModalVisible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h2>{{ modalMode === 'create' ? '创建新角色' : '编辑角色' }}</h2>
        <form @submit.prevent="handleFormSubmit">
          <div class="form-group">
            <label for="name">角色名称 *</label>
            <input id="name" type="text" v-model="characterForm.name" required maxlength="50">
          </div>
          <div class="form-group">
            <label for="description">角色描述</label>
            <input id="description" type="text" v-model="characterForm.description">
          </div>
          <div class="form-group">
            <label for="personaPrompt">人设提示词 *</label>
            <textarea id="personaPrompt" v-model="characterForm.personaPrompt" required></textarea>
          </div>
          <div class="form-group">
            <label for="greetingMessage">开场白文本</label>
            <input id="greetingMessage" type="text" v-model="characterForm.greetingMessage">
          </div>
          <div class="form-group">
            <label for="avatarUrl">头像</label>
            <div class="avatar-upload-group">
              <img v-if="characterForm.avatarUrl" :src="characterForm.avatarUrl" alt="头像预览" class="avatar-preview">
              <div class="upload-inputs">
                <input id="avatarUrl" type="text" v-model="characterForm.avatarUrl" placeholder="可粘贴URL或上传图片">
                <input type="file" ref="fileInput" @change="handleImageUpload" accept="image/*" style="display: none;">
                <button type="button" class="btn-upload" @click="triggerFileInput" :disabled="isUploading">
                  {{ isUploading ? '上传中...' : '上传图片' }}
                </button>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label for="voiceType">TTS声音类型 *</label>
            <div class="voice-type-group">
              <select id="voiceType" v-model="characterForm.voiceType" required>
                <option disabled value="">请选择一个声音</option>
                <option v-for="voice in voiceList" :key="voice.voice_type" :value="voice.voice_type">
                  {{ voice.voice_name }}
                </option>
              </select>
              <button type="button" class="btn-preview" @click="previewVoice" :disabled="!characterForm.voiceType" title="试听声音">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"></path>
                </svg>
              </button>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" @click="closeModal" class="btn-cancel">取消</button>
            <button type="submit" class="btn-submit">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import CharacterCard from '../components/CharacterCard.vue';
import characterApi from '../services/character';
import { ElMessage } from 'element-plus';

const authStore = useAuthStore();
const user = computed(() => authStore.user);

const activeTab = ref('public');
const characters = ref([]);
const isLoading = ref(false);
const searchQuery = ref('');
const pagination = ref({ page: 1, size: 7, total: 0 });
let searchDebounceTimer = null;

const isModalVisible = ref(false);
const modalMode = ref('create');
const editingCharacterId = ref(null);
const characterForm = ref({
  name: '', description: '', personaPrompt: '',
  greetingMessage: '', avatarUrl: '', voiceType: '',
});

const fileInput = ref(null);
const isUploading = ref(false);
const voiceList = ref([]);
let currentPreviewAudio = null;

const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.size));

const fetchVoiceList = async () => {
  try {
    const response = await characterApi.getVoiceList();
    voiceList.value = response.data;
  } catch (error) {
    console.error("获取声音列表失败:", error);
    ElMessage.error("加载声音列表失败，可能存在跨域问题，请检查浏览器控制台");
  }
};

onMounted(() => {
  fetchVoiceList();
});

const fetchData = async () => {
  isLoading.value = true;
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size,
      keyword: searchQuery.value,
    };
    const response = activeTab.value === 'public'
        ? await characterApi.getPublicCharacters(params)
        : await characterApi.getMyCharacters(params);

    if (response && response.data.success) {
      characters.value = response.data.data.records || [];
      pagination.value.total = response.data.data.total || 0;
    }
  } catch (error) {
    console.error(`获取角色列表失败:`, error);
    ElMessage.error('获取角色列表失败，请刷新页面重试');
    characters.value = [];
    pagination.value.total = 0;
  } finally {
    isLoading.value = false;
  }
};

const handleLogout = () => { authStore.logout(); };

const selectTab = (tab) => {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  pagination.value.page = 1;
  searchQuery.value = '';
};

const changePage = (newPage) => {
  if (newPage > 0 && newPage <= totalPages.value) { pagination.value.page = newPage; }
};

const handleDelete = async (id) => {
  if (window.confirm('您确定要删除这个角色吗？此操作不可撤销。')) {
    try {
      await characterApi.deleteCharacter(id);
      ElMessage.success('角色删除成功！');
      if (characters.value.length === 1 && pagination.value.page > 1) {
        pagination.value.page--;
      } else {
        fetchData();
      }
    } catch (error) {
      console.error("删除角色失败:", error);
      ElMessage.error(error.response?.data?.message || '删除失败，请稍后再试。');
    }
  }
};

const resetForm = () => {
  characterForm.value = {
    name: '', description: '', personaPrompt: '',
    greetingMessage: '', avatarUrl: '', voiceType: '',
  };
};

const openModalForCreate = () => {
  resetForm();
  modalMode.value = 'create';
  editingCharacterId.value = null;
  isModalVisible.value = true;
};

const openModalForEdit = (character) => {
  characterForm.value = { ...character };
  modalMode.value = 'edit';
  editingCharacterId.value = character.id;
  isModalVisible.value = true;
};

const closeModal = () => {
  if (currentPreviewAudio) {
    currentPreviewAudio.pause();
  }
  isModalVisible.value = false;
};

const handleFormSubmit = async () => {
  try {
    if (modalMode.value === 'create') {
      await characterApi.createCharacter(characterForm.value);
      ElMessage.success('角色创建成功！');
    } else {
      await characterApi.updateCharacter(editingCharacterId.value, characterForm.value);
      ElMessage.success('角色更新成功！');
    }
    closeModal();
    fetchData();
  } catch (error) {
    console.error("保存角色失败:", error);
    ElMessage.error(error.response?.data?.message || '保存失败，请检查填写的内容。');
  }
};

const triggerFileInput = () => { fileInput.value.click(); };

const handleImageUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  isUploading.value = true;
  try {
    const response = await characterApi.uploadImage(file);
    if (response.data && response.data.success) {
      const imageUrl = response.data.message;
      characterForm.value.avatarUrl = imageUrl;
      ElMessage.success('图片上传成功！');
    } else {
      throw new Error(response.data.message || '上传失败');
    }
  } catch (error) {
    console.error("图片上传失败:", error);
    ElMessage.error(error.message || '图片上传失败，请稍后再试。');
  } finally {
    isUploading.value = false;
    event.target.value = '';
  }
};

const previewVoice = () => {
  if (currentPreviewAudio) {
    currentPreviewAudio.pause();
    currentPreviewAudio.currentTime = 0;
  }

  const selectedVoice = voiceList.value.find(
      voice => voice.voice_type === characterForm.value.voiceType
  );

  if (selectedVoice && selectedVoice.url) {
    currentPreviewAudio = new Audio(selectedVoice.url);
    currentPreviewAudio.play().catch(e => {
      console.error("音频播放失败:", e);
      ElMessage.error("无法播放此声音预览");
    });
  } else {
    ElMessage.warning('当前声音没有可用的试听链接');
  }
};

watch([activeTab, () => pagination.value.page], fetchData, { immediate: true });

watch(searchQuery, () => {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    pagination.value.page = 1;
    fetchData();
  }, 500);
});
</script>

<style scoped>
/* 页面主容器的样式 */
.selection-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 2rem 4rem 4rem;
  box-sizing: border-box;
}

/* 用户信息栏样式 */
.user-info-bar {
  position: absolute;
  top: 2rem;
  right: 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: rgba(47, 47, 47, 0.5);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  backdrop-filter: blur(5px);
  z-index: 10;
}
.welcome-text { color: #d1d5db; font-weight: 500; }
.logout-button { background-color: transparent; border: 1px solid #4b5563; color: #d1d5db; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
.logout-button:hover { background-color: #374151; border-color: #6b7280; }

/* 头部区域的样式 */
.header { text-align: center; margin-bottom: 1.5rem; }
.title { font-size: 3.2rem; font-weight: bold; }
.subtitle { font-size: 1.2rem; color: #888; margin-top: 0.5rem; }

/* 控制区域样式 */
.controls-container {
  width: 100%;
  max-width: 1200px;
  margin-bottom: 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 选项卡样式 */
.tabs { display: flex; justify-content: center; gap: 1rem; }
.tabs button { background: none; border: none; border-bottom: 2px solid transparent; color: #9ca3af; padding: 0.5rem 1rem; font-size: 1.1rem; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.tabs button:hover { color: #e5e7eb; }
.tabs button.active { color: #3b82f6; border-bottom-color: #3b82f6; }

/* 搜索框样式 */
.search-bar { display: flex; justify-content: center; }
.search-bar input {
  width: 100%;
  max-width: 500px;
  background-color: #374151;
  border: 1px solid #4b5563;
  border-radius: 8px;
  color: #e5e7eb;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-bar input:focus { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.4); }

/* 角色卡片网格的样式 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
  width: 100%;
  max-width: 1200px;
}
.loading-state, .empty-state {
  grid-column: 1 / -1;
  text-align: center;
  color: #9ca3af;
  font-size: 1.2rem;
  padding: 4rem 0;
}
.character-card-wrapper { min-height: 220px; }
.character-card.create-new-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 2px dashed #4b5563;
  background-color: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
  height: 100%;
}
.character-card.create-new-card:hover { border-color: #6b7280; background-color: #374151; color: #e5e7eb; }
.create-icon { font-size: 3rem; font-weight: 200; }
.create-text { margin-top: 0.5rem; font-weight: 500; }

/* 分页样式 */
.pagination {
  margin-top: 3rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  color: #9ca3af;
}
.pagination button {
  background-color: #374151;
  border: 1px solid #4b5563;
  color: #e5e7eb;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.pagination button:hover:not(:disabled) { background-color: #4b5563; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }

/* 弹窗样式 */
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background-color: rgba(0, 0, 0, 0.7); display: flex;
  justify-content: center; align-items: center; z-index: 1000;
}
.modal-content {
  background-color: #2d3748; padding: 2rem; border-radius: 12px;
  width: 90%; max-width: 500px; max-height: 90vh;
  overflow-y: auto; box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}
.modal-content h2 { margin-top: 0; margin-bottom: 1.5rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; color: #a0aec0; }
.form-group input, .form-group textarea, .form-group select {
  width: 100%; background-color: #1a202c; border: 1px solid #4a5568;
  border-radius: 6px; padding: 0.75rem; color: #e2e8f0; box-sizing: border-box;
}
.form-group textarea { min-height: 100px; resize: vertical; }
.form-actions {
  margin-top: 2rem; display: flex; justify-content: flex-end; gap: 1rem;
}
.form-actions button {
  padding: 0.75rem 1.5rem; border-radius: 6px; border: none;
  font-weight: 500; cursor: pointer;
}
.btn-cancel { background-color: #4a5568; color: #e2e8f0; }
.btn-submit { background-color: #3b82f6; color: white; }

/* 头像上传区域样式 */
.avatar-upload-group { display: flex; align-items: flex-start; gap: 1rem; }
.avatar-preview {
  width: 80px; height: 80px; border-radius: 8px; object-fit: cover;
  background-color: #1a202c; border: 1px solid #4a5568;
}
.upload-inputs { flex: 1; display: flex; flex-direction: column; gap: 0.5rem; }
.btn-upload {
  width: 100%; padding: 0.75rem; background-color: #4a5568; color: #e2e8f0;
  border: none; border-radius: 6px; cursor: pointer;
  transition: background-color 0.2s; font-weight: 500;
}
.btn-upload:hover:not(:disabled) { background-color: #6b7280; }
.btn-upload:disabled { opacity: 0.7; cursor: not-allowed; }

/* TTS声音选择框和试听按钮的样式 */
.voice-type-group { display: flex; align-items: center; gap: 0.75rem; }
.voice-type-group select { flex: 1; }
.btn-preview {
  flex-shrink: 0; display: flex; justify-content: center; align-items: center;
  width: 44px; height: 44px; background-color: #4a5568; border: none;
  border-radius: 6px; color: #e2e8f0; cursor: pointer; transition: background-color 0.2s;
}
.btn-preview:hover:not(:disabled) { background-color: #6b7280; }
.btn-preview:disabled { opacity: 0.5; cursor: not-allowed; }

/* 响应式布局 */
@media (max-width: 1024px) { .card-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .selection-container { padding: 1.5rem; }
  .card-grid { grid-template-columns: repeat(2, 1fr); }
  .title { font-size: 2.5rem; }
  .user-info-bar { position: static; margin-bottom: 2rem; order: -1; justify-content: center;}
}
@media (max-width: 480px) { .card-grid { grid-template-columns: 1fr; } }
</style>