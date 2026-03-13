<template>
  <div class="rag-page">
    <aside class="rag-sidebar">
      <div class="sidebar-header">
        <button class="back-btn" @click="router.push('/characters')">&lt; 返回</button>
        <div>
          <h1>知识库</h1>
          <p>创建、上传、检索测试一体完成</p>
        </div>
      </div>

      <form class="create-form" @submit.prevent="createKnowledgeBase">
        <input v-model.trim="createForm.name" type="text" maxlength="100" placeholder="知识库名称" required />
        <textarea
          v-model.trim="createForm.description"
          maxlength="500"
          rows="3"
          placeholder="知识库描述，可选"
        />
        <button type="submit" :disabled="creating">{{ creating ? '创建中...' : '创建知识库' }}</button>
      </form>

      <div class="kb-list">
        <button
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="kb-card"
          :class="{ active: selectedKnowledgeBaseId === kb.id }"
          @click="selectKnowledgeBase(kb.id)"
        >
          <div class="kb-card-head">
            <strong>{{ kb.name }}</strong>
            <span class="status" :class="statusClass(kb.status)">{{ kb.status }}</span>
          </div>
          <p>{{ kb.description || '暂无描述' }}</p>
          <div class="kb-card-meta">
            <span>{{ kb.fileCount || 0 }} 个文件</span>
            <span>{{ kb.documentCount || 0 }} 个切片</span>
          </div>
        </button>

        <div v-if="!loading && knowledgeBases.length === 0" class="empty-tip">
          还没有知识库，先创建一个。
        </div>
      </div>
    </aside>

    <main class="rag-main">
      <div v-if="loading" class="panel loading">加载中...</div>
      <template v-else-if="currentKnowledgeBase">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>{{ currentKnowledgeBase.name }}</h2>
              <p>{{ currentKnowledgeBase.description || '暂无描述' }}</p>
            </div>
            <button class="danger-btn" @click="deleteKnowledgeBase">删除知识库</button>
          </div>

          <div class="upload-row">
            <input
              ref="fileInput"
              type="file"
              accept=".md,.markdown,.txt,.html,.htm,.pdf"
              @change="handleFileChange"
              style="display: none;"
            />
            <button class="primary-btn" @click="fileInput?.click()" :disabled="uploading">
              {{ uploading ? '上传处理中...' : '上传文档' }}
            </button>
            <span class="hint">支持 `.md` `.txt` `.html` `.pdf`，上传后同步完成切分与向量化。</span>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>检索调试</h3>
              <p>先验证命中，再去聊天页勾选这个知识库。</p>
            </div>
          </div>
          <div class="search-row">
            <input
              v-model.trim="searchQuery"
              type="text"
              placeholder="输入一个问题，例如：这个项目的核心目标是什么？"
              @keydown.enter.prevent="runSearch"
            />
            <button class="primary-btn" @click="runSearch" :disabled="searching || !searchQuery">
              {{ searching ? '检索中...' : '检索' }}
            </button>
          </div>
          <div v-if="searchResults.length > 0" class="search-results">
            <article v-for="item in searchResults" :key="`${item.documentUnitId}-${item.fileId}`" class="search-result-card">
              <div class="search-result-head">
                <strong>{{ item.title || '未命名文件' }}</strong>
                <span>score {{ Number(item.score || 0).toFixed(4) }}</span>
              </div>
              <p>{{ item.content }}</p>
            </article>
          </div>
          <div v-else class="empty-tip">当前还没有检索结果。</div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>文件列表</h3>
              <p>文件状态为 `COMPLETED` 后即可参与聊天 RAG。</p>
            </div>
          </div>

          <table class="file-table" v-if="currentKnowledgeBase.files && currentKnowledgeBase.files.length > 0">
            <thead>
              <tr>
                <th>文件名</th>
                <th>状态</th>
                <th>切片</th>
                <th>向量化</th>
                <th>创建时间</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in currentKnowledgeBase.files" :key="file.id">
                <td>
                  <div class="file-name">{{ file.originalName }}</div>
                  <div class="file-meta">{{ file.fileType }} · {{ formatFileSize(file.fileSize) }}</div>
                  <div v-if="file.errorMessage" class="file-error">{{ file.errorMessage }}</div>
                </td>
                <td><span class="status" :class="statusClass(file.status)">{{ file.status }}</span></td>
                <td>{{ file.chunkCount || 0 }}</td>
                <td>{{ file.vectorizedChunkCount || 0 }}</td>
                <td>{{ formatDateTime(file.createdAt) }}</td>
                <td>
                  <button class="link-btn danger-text" @click="deleteFile(file.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-tip">当前知识库还没有文件。</div>
        </section>
      </template>

      <div v-else class="panel empty-state">
        请选择左侧知识库，或先创建一个新的知识库。
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import ragApi from '../services/rag';

const router = useRouter();

const loading = ref(false);
const creating = ref(false);
const uploading = ref(false);
const searching = ref(false);
const knowledgeBases = ref([]);
const selectedKnowledgeBaseId = ref(null);
const searchResults = ref([]);
const searchQuery = ref('');
const fileInput = ref(null);

const createForm = ref({
  name: '',
  description: '',
});

const currentKnowledgeBase = computed(() =>
  knowledgeBases.value.find(item => item.id === selectedKnowledgeBaseId.value) || null
);

const loadKnowledgeBases = async (preferredId = null) => {
  loading.value = true;
  try {
    const response = await ragApi.listKnowledgeBases();
    if (!response.data.success) {
      throw new Error(response.data.message || '加载知识库失败');
    }

    knowledgeBases.value = response.data.data || [];
    const nextId = preferredId
      || selectedKnowledgeBaseId.value
      || knowledgeBases.value[0]?.id
      || null;

    if (nextId) {
      await selectKnowledgeBase(nextId);
    } else {
      selectedKnowledgeBaseId.value = null;
    }
  } catch (error) {
    console.error('加载知识库失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '加载知识库失败');
  } finally {
    loading.value = false;
  }
};

const selectKnowledgeBase = async (knowledgeBaseId) => {
  selectedKnowledgeBaseId.value = knowledgeBaseId;
  searchResults.value = [];
  if (!knowledgeBaseId) {
    return;
  }

  try {
    const response = await ragApi.getKnowledgeBase(knowledgeBaseId);
    if (!response.data.success) {
      throw new Error(response.data.message || '加载知识库详情失败');
    }
    const detail = response.data.data;
    const index = knowledgeBases.value.findIndex(item => item.id === knowledgeBaseId);
    if (index >= 0) {
      knowledgeBases.value[index] = detail;
    } else {
      knowledgeBases.value.push(detail);
    }
  } catch (error) {
    console.error('加载知识库详情失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '加载知识库详情失败');
  }
};

const createKnowledgeBase = async () => {
  if (!createForm.value.name) {
    return;
  }

  creating.value = true;
  try {
    const response = await ragApi.createKnowledgeBase(createForm.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '创建知识库失败');
    }
    createForm.value = { name: '', description: '' };
    ElMessage.success('知识库创建成功');
    await loadKnowledgeBases(response.data.data.id);
  } catch (error) {
    console.error('创建知识库失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '创建知识库失败');
  } finally {
    creating.value = false;
  }
};

const handleFileChange = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file || !selectedKnowledgeBaseId.value) {
    return;
  }

  uploading.value = true;
  try {
    const response = await ragApi.uploadFile(selectedKnowledgeBaseId.value, file);
    if (!response.data.success) {
      throw new Error(response.data.message || '上传文档失败');
    }
    ElMessage.success(`文档已入库：${file.name}`);
    await selectKnowledgeBase(selectedKnowledgeBaseId.value);
  } catch (error) {
    console.error('上传文档失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '上传文档失败');
  } finally {
    uploading.value = false;
  }
};

const runSearch = async () => {
  if (!selectedKnowledgeBaseId.value || !searchQuery.value) {
    return;
  }

  searching.value = true;
  try {
    const response = await ragApi.search(selectedKnowledgeBaseId.value, searchQuery.value, 5);
    if (!response.data.success) {
      throw new Error(response.data.message || '检索失败');
    }
    searchResults.value = response.data.data || [];
    if (searchResults.value.length === 0) {
      ElMessage.info('当前没有命中结果');
    }
  } catch (error) {
    console.error('知识库检索失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '知识库检索失败');
  } finally {
    searching.value = false;
  }
};

const deleteFile = async (fileId) => {
  try {
    await ElMessageBox.confirm('删除后该文件将不再参与检索，是否继续？', '删除文件', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await ragApi.deleteFile(selectedKnowledgeBaseId.value, fileId);
    ElMessage.success('文件已删除');
    await selectKnowledgeBase(selectedKnowledgeBaseId.value);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除文件失败');
    }
  }
};

const deleteKnowledgeBase = async () => {
  if (!currentKnowledgeBase.value) {
    return;
  }

  try {
    const deletedId = currentKnowledgeBase.value.id;
    await ElMessageBox.confirm(
      '删除知识库会同时移除其所有文档切片和向量数据，是否继续？',
      '删除知识库',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    await ragApi.deleteKnowledgeBase(deletedId);
    const remainingKnowledgeBases = knowledgeBases.value.filter(item => item.id !== deletedId);
    knowledgeBases.value = remainingKnowledgeBases;
    selectedKnowledgeBaseId.value = remainingKnowledgeBases[0]?.id || null;
    searchResults.value = [];
    ElMessage.success('知识库已删除');
    await loadKnowledgeBases(selectedKnowledgeBaseId.value);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || error.message || '删除知识库失败');
    }
  }
};

const formatFileSize = (size) => {
  if (!size) return '0 B';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
};

const formatDateTime = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleString();
};

const statusClass = (status) => {
  if (!status) return 'pending';
  return status.toLowerCase();
};

onMounted(() => {
  loadKnowledgeBases();
});
</script>

<style scoped>
.rag-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 340px 1fr;
  background: linear-gradient(180deg, #f7f4ea 0%, #efe7d2 100%);
}

.rag-sidebar {
  border-right: 1px solid rgba(95, 73, 43, 0.15);
  padding: 24px 20px;
  background: rgba(255, 252, 245, 0.85);
  backdrop-filter: blur(12px);
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.sidebar-header h1 {
  margin: 0;
  font-size: 28px;
  color: #2c2417;
}

.sidebar-header p {
  margin: 4px 0 0;
  color: #7c6b55;
  font-size: 14px;
}

.back-btn,
.primary-btn,
.danger-btn,
.create-form button {
  border: none;
  border-radius: 12px;
  cursor: pointer;
}

.back-btn {
  width: fit-content;
  padding: 10px 14px;
  background: #e6dbc5;
  color: #3d2f1f;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.create-form input,
.create-form textarea,
.search-row input {
  width: 100%;
  border: 1px solid rgba(95, 73, 43, 0.18);
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  background: #fffdf8;
  color: #2f2418;
  box-sizing: border-box;
}

.create-form input::placeholder,
.create-form textarea::placeholder,
.search-row input::placeholder {
  color: #8e7a60;
}

.create-form button,
.primary-btn {
  padding: 12px 16px;
  background: #9a6b2f;
  color: #fff;
  font-weight: 600;
}

.danger-btn {
  padding: 10px 14px;
  background: #d75a4a;
  color: #fff;
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kb-card {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(95, 73, 43, 0.14);
  border-radius: 16px;
  padding: 16px;
  background: #fffdf8;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.kb-card:hover,
.kb-card.active {
  transform: translateY(-1px);
  border-color: #9a6b2f;
  box-shadow: 0 12px 26px rgba(95, 73, 43, 0.12);
}

.kb-card-head,
.kb-card-meta,
.panel-head,
.upload-row,
.search-row,
.search-result-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.kb-card p,
.panel-head p,
.hint,
.file-meta {
  color: #7c6b55;
}

.rag-main {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  background: rgba(255, 252, 246, 0.92);
  border: 1px solid rgba(95, 73, 43, 0.14);
  border-radius: 20px;
  padding: 20px 22px;
  box-shadow: 0 14px 36px rgba(95, 73, 43, 0.08);
}

.panel h2,
.panel h3 {
  margin: 0;
  color: #2c2417;
}

.upload-row,
.search-row {
  align-items: center;
  margin-top: 14px;
}

.search-row input {
  flex: 1;
}

.search-results {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.search-result-card {
  border: 1px solid rgba(95, 73, 43, 0.12);
  border-radius: 14px;
  padding: 14px 16px;
  background: #fffaf1;
}

.search-result-card p {
  margin: 10px 0 0;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #2f2a22;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.file-table th,
.file-table td {
  border-bottom: 1px solid rgba(95, 73, 43, 0.12);
  padding: 14px 10px;
  text-align: left;
  vertical-align: top;
}

.file-name {
  font-weight: 600;
  color: #2c2417;
}

.file-error {
  color: #cc4a32;
  margin-top: 6px;
  font-size: 13px;
}

.link-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.danger-text {
  color: #d75a4a;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status.active,
.status.completed {
  background: #d8f3dc;
  color: #1f7a36;
}

.status.processing,
.status.vectorizing,
.status.parsing,
.status.splitting,
.status.uploading {
  background: #fff1c7;
  color: #a46a00;
}

.status.failed {
  background: #ffe1d9;
  color: #c43c22;
}

.status.pending {
  background: #ececec;
  color: #666;
}

.loading,
.empty-state,
.empty-tip {
  color: #7c6b55;
}

@media (max-width: 960px) {
  .rag-page {
    grid-template-columns: 1fr;
  }

  .rag-sidebar {
    border-right: none;
    border-bottom: 1px solid rgba(95, 73, 43, 0.15);
  }

  .upload-row,
  .search-row,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
