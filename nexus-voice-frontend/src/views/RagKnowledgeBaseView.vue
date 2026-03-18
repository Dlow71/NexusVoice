<template>
  <div class="knowledge-base-page">
    <div class="page-grid"></div>
    <div class="page-glow glow-top"></div>
    <div class="page-glow glow-bottom"></div>

    <aside class="knowledge-sidebar">
      <div class="sidebar-top">
        <div class="sidebar-title-row">
          <SparklesBadge class="brand-icon" :hoverable="true" :size="50" :icon-size="30" />
          <div>
            <h1>知识库</h1>
            <p>创建、上传、检索一体化</p>
          </div>
        </div>

        <button class="create-button" :disabled="creating" @click="toggleCreateKnowledgeBase">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>{{ creating ? '创建中...' : '新建知识库' }}</span>
        </button>

        <Transition name="create-kb-inline">
          <div v-if="createDialogVisible" class="create-kb-inline-panel">
            <div class="create-kb-inline-header">新建知识库</div>

            <form class="create-kb-inline-form" @submit.prevent="submitCreateKnowledgeBase">
              <input
                ref="createNameInput"
                v-model.trim="createForm.name"
                type="text"
                maxlength="100"
                placeholder="知识库名称"
                :disabled="creating"
              />

              <textarea
                v-model.trim="createForm.description"
                maxlength="500"
                rows="3"
                placeholder="描述（可选）"
                :disabled="creating"
              />

              <p v-if="createFormError" class="create-kb-error">{{ createFormError }}</p>

              <button class="create-kb-submit" type="submit" :disabled="creating">
                {{ creating ? '创建中...' : '创建' }}
              </button>
            </form>
          </div>
        </Transition>
      </div>

      <div class="knowledge-list">
        <button
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="knowledge-card"
          :class="{ active: selectedKnowledgeBaseId === kb.id }"
          @click="selectKnowledgeBase(kb.id)"
        >
          <div class="knowledge-card-head">
            <strong>{{ kb.name }}</strong>
            <span class="knowledge-status-chip" :class="knowledgeBaseStatusClass(kb.status)">
              {{ knowledgeBaseStatusLabel(kb.status) }}
            </span>
          </div>
          <p>{{ kb.description || '暂无知识库描述' }}</p>
          <div class="knowledge-card-meta">
            <span>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 4.5h6l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
                <path d="M13 4.5V9h4" />
              </svg>
              {{ kb.fileCount || 0 }} 个文件
            </span>
            <span>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 3 5 6.5 12 10l7-3.5L12 3Z" />
                <path d="m5 11 7 3.5 7-3.5" />
                <path d="m5 15.5 7 3.5 7-3.5" />
              </svg>
              {{ kb.documentCount || 0 }} 个切片
            </span>
          </div>
        </button>

        <div v-if="!loading && knowledgeBases.length === 0" class="sidebar-empty">
          还没有知识库，先创建一个开始上传文档。
        </div>
      </div>

      <button class="back-home-button" @click="router.push('/characters')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M15 6 9 12l6 6" />
        </svg>
        <span>返回主页</span>
      </button>
    </aside>

    <main class="knowledge-main">
      <div v-if="loading" class="main-state">
        <div class="main-state-card">知识库加载中...</div>
      </div>

      <div v-else-if="currentKnowledgeBase" class="main-shell">
        <input
          ref="fileInput"
          type="file"
          accept=".md,.markdown,.txt,.html,.htm,.pdf"
          multiple
          hidden
          @change="handleFileChange"
        />

        <header class="main-header">
          <div class="main-header-info">
            <SparklesBadge class="main-header-icon" :hoverable="true" :size="50" :icon-size="30" />
            <div class="main-header-copy">
              <h2>{{ currentKnowledgeBase.name }}</h2>
              <p>{{ currentKnowledgeBase.description || '用于测试 RAG 检索的知识文档集合' }}</p>
            </div>
          </div>

          <div class="main-header-actions">
            <div class="summary-pill">
              <div class="summary-item">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M7 4.5h6l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
                  <path d="M13 4.5V9h4" />
                </svg>
                <strong>{{ totalFileCount }}</strong>
                <span>文件</span>
              </div>
              <div class="summary-divider"></div>
              <div class="summary-item">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 3 5 6.5 12 10l7-3.5L12 3Z" />
                  <path d="m5 11 7 3.5 7-3.5" />
                  <path d="m5 15.5 7 3.5 7-3.5" />
                </svg>
                <strong>{{ totalChunkCount }}</strong>
                <span>切片</span>
              </div>
            </div>

            <button class="delete-knowledge-button" @click="deleteKnowledgeBase">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 4.5h6" />
                <path d="M4.5 7h15" />
                <path d="M8 7V18a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V7" />
                <path d="M10 11v5" />
                <path d="M14 11v5" />
              </svg>
              <span>删除知识库</span>
            </button>
          </div>
        </header>

        <nav class="tab-bar">
          <button
            class="tab-button"
            :class="{ active: activeTab === 'upload' }"
            @click="activeTab = 'upload'"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 16V5" />
              <path d="m7.5 9.5 4.5-4.5 4.5 4.5" />
              <path d="M5 18.5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2" />
            </svg>
            <span>上传文档</span>
          </button>

          <button
            class="tab-button"
            :class="{ active: activeTab === 'search' }"
            @click="activeTab = 'search'"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="m16 16 4 4" />
            </svg>
            <span>检索调试</span>
          </button>

          <button
            class="tab-button"
            :class="{ active: activeTab === 'files' }"
            @click="activeTab = 'files'"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 4.5h6l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
              <path d="M13 4.5V9h4" />
            </svg>
            <span>文件列表</span>
            <span class="tab-badge">{{ currentFiles.length }}</span>
          </button>
        </nav>

        <section class="tab-panel">
          <template v-if="activeTab === 'upload'">
            <div class="stats-grid">
              <article class="stat-card">
                <div class="stat-card-title">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M7 4.5h6l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
                    <path d="M13 4.5V9h4" />
                  </svg>
                  <span>文件总数</span>
                </div>
                <strong>{{ totalFileCount }}</strong>
              </article>

              <article class="stat-card">
                <div class="stat-card-title">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 3 5 6.5 12 10l7-3.5L12 3Z" />
                    <path d="m5 11 7 3.5 7-3.5" />
                    <path d="m5 15.5 7 3.5 7-3.5" />
                  </svg>
                  <span>切片总数</span>
                </div>
                <strong>{{ totalChunkCount }}</strong>
              </article>

              <article class="stat-card">
                <div class="stat-card-title">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <ellipse cx="12" cy="6" rx="6.2" ry="2.8" />
                    <path d="M5.8 6v4.8c0 1.5 2.8 2.8 6.2 2.8s6.2-1.3 6.2-2.8V6" />
                    <path d="M5.8 10.7v4.8c0 1.5 2.8 2.8 6.2 2.8s6.2-1.3 6.2-2.8v-4.8" />
                  </svg>
                  <span>向量化完成</span>
                </div>
                <strong>{{ vectorizedCompletedCount }}</strong>
              </article>
            </div>

            <div
              class="upload-dropzone"
              :class="{ dragging: isDragging, disabled: uploading }"
              @click="triggerFilePicker"
              @dragenter.prevent="isDragging = true"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="handleDrop"
            >
              <div class="upload-icon-shell">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 16V5" />
                  <path d="m7.5 9.5 4.5-4.5 4.5 4.5" />
                  <path d="M5 18.5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2" />
                </svg>
              </div>
              <h3>
                拖放文件到此处，或
                <span>{{ uploading ? '上传中...' : '点击上传' }}</span>
              </h3>
              <p>支持 .md · .txt · .html · .pdf，上传后自动完成切分与向量化</p>
              <div class="upload-tags">
                <span>.md</span>
                <span>.txt</span>
                <span>.html</span>
                <span>.pdf</span>
              </div>
            </div>
          </template>

          <template v-else-if="activeTab === 'search'">
            <div class="search-notice">
              先验证命中，再去聊天页勾选这个知识库参与对话。
            </div>

            <div class="search-row">
              <div class="search-input-shell">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="11" cy="11" r="6.5" />
                  <path d="m16 16 4 4" />
                </svg>
                <input
                  v-model.trim="searchQuery"
                  type="text"
                  placeholder="输入一个问题，例如：这个项目的核心目标是什么？"
                  @keydown.enter.prevent="runSearch"
                />
              </div>
              <button class="search-button" :disabled="searching || !searchQuery" @click="runSearch">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m13 3-7 10h5l-1 8 8-11h-5l1-7Z" />
                </svg>
                <span>{{ searching ? '检索中' : '检索' }}</span>
              </button>
            </div>

            <div v-if="searchResults.length > 0" class="search-results">
              <article
                v-for="item in searchResults"
                :key="`${item.documentUnitId}-${item.fileId}`"
                class="search-result-card"
              >
                <div class="search-result-head">
                  <strong>{{ item.title || '未命名文件' }}</strong>
                  <span>Score {{ formatSearchScore(item.score) }}</span>
                </div>
                <div
                  v-if="shouldRenderSearchResultAsMarkdown(item)"
                  class="search-result-markdown"
                  v-html="renderSearchResultMarkdown(item.content)"
                ></div>
                <p v-else>{{ item.content }}</p>
              </article>
            </div>

            <div v-else class="search-empty-state">
              {{ hasSearched ? '没有命中结果，换个问题再试一次。' : '当前还没有检索结果，输入问题后按 Enter 或点击搜索' }}
            </div>
          </template>

          <template v-else>
            <div class="files-head">
              <div>
                <h3>文件列表</h3>
                <p>文件状态为 COMPLETED 后即可参与聊天 RAG</p>
              </div>
              <button class="upload-more-button" :disabled="uploading" @click="triggerFilePicker">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 16V5" />
                  <path d="m7.5 9.5 4.5-4.5 4.5 4.5" />
                  <path d="M5 18.5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2" />
                </svg>
                <span>{{ uploading ? '上传中...' : '上传更多' }}</span>
              </button>
            </div>

            <div class="file-table-shell">
              <table v-if="currentFiles.length > 0" class="file-table">
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
                  <tr v-for="file in currentFiles" :key="file.id">
                    <td>
                      <div class="file-name-cell">
                        <div class="file-icon">
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M7 4.5h6l4 4V18a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2Z" />
                            <path d="M13 4.5V9h4" />
                          </svg>
                        </div>
                        <div class="file-name-copy">
                          <strong>{{ file.originalName }}</strong>
                          <span>{{ formatFileType(file.fileType) }} · {{ formatFileSize(file.fileSize) }}</span>
                          <em v-if="file.errorMessage">{{ file.errorMessage }}</em>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span class="file-status-chip" :class="fileStatusClass(file.status)">
                        {{ fileStatusLabel(file.status) }}
                      </span>
                    </td>
                    <td class="numeric-cell">{{ displayChunkCount(file) }}</td>
                    <td class="numeric-cell">{{ displayVectorizedCount(file) }}</td>
                    <td class="time-cell">{{ formatDateTime(file.createdAt) }}</td>
                    <td class="action-cell">
                      <button class="delete-file-button" @click="deleteFile(file.id)">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path d="M9 4.5h6" />
                          <path d="M4.5 7h15" />
                          <path d="M8 7V18a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V7" />
                          <path d="M10 11v5" />
                          <path d="M14 11v5" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>

              <div v-else class="files-empty-state">
                当前知识库还没有文件，先上传文档再进行检索调试。
              </div>
            </div>
          </template>
        </section>
      </div>

      <div v-else class="main-state">
        <div class="main-state-card">请选择左侧知识库，或先新建一个知识库。</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import ragApi from '../services/rag';
import SparklesBadge from '../components/SparklesBadge.vue';

const router = useRouter();

const loading = ref(false);
const creating = ref(false);
const uploading = ref(false);
const searching = ref(false);
const isDragging = ref(false);
const hasSearched = ref(false);
const activeTab = ref('upload');
const knowledgeBases = ref([]);
const selectedKnowledgeBaseId = ref(null);
const searchResults = ref([]);
const searchQuery = ref('');
const fileInput = ref(null);
const createNameInput = ref(null);
const createDialogVisible = ref(false);
const createFormError = ref('');
const createForm = ref({
  name: '',
  description: '',
});
const SELECTED_KB_REFRESH_MS = 8000;
const SELECTED_KB_ACTIVE_REFRESH_MS = 2500;
let selectedKnowledgeBaseRefreshTimer = null;

const currentKnowledgeBase = computed(() =>
  knowledgeBases.value.find(item => item.id === selectedKnowledgeBaseId.value) || null
);

const currentFiles = computed(() => currentKnowledgeBase.value?.files || []);

const totalFileCount = computed(() => currentKnowledgeBase.value?.fileCount ?? currentFiles.value.length);

const totalChunkCount = computed(() => currentKnowledgeBase.value?.documentCount
  ?? currentFiles.value.reduce((sum, file) => sum + Number(file.chunkCount || 0), 0));

const vectorizedCompletedCount = computed(() =>
  currentFiles.value.filter(file => {
    const chunkCount = Number(file.chunkCount || 0);
    const vectorizedCount = Number(file.vectorizedChunkCount || 0);
    return chunkCount > 0 && vectorizedCount >= chunkCount;
  }).length
);

const hasInProgressFiles = computed(() =>
  currentFiles.value.some(file => isFileInProgress(file))
);

const normalizeKnowledgeBase = (knowledgeBase = {}) => ({
  id: knowledgeBase.id ?? null,
  name: knowledgeBase.name || '未命名知识库',
  description: knowledgeBase.description || '',
  status: knowledgeBase.status || 'ACTIVE',
  fileCount: Number(knowledgeBase.fileCount || 0),
  documentCount: Number(knowledgeBase.documentCount || 0),
  totalSize: Number(knowledgeBase.totalSize || 0),
  createdAt: knowledgeBase.createdAt || null,
  updatedAt: knowledgeBase.updatedAt || null,
  files: Array.isArray(knowledgeBase.files) ? knowledgeBase.files : [],
});

const upsertKnowledgeBase = (knowledgeBase) => {
  const normalized = normalizeKnowledgeBase(knowledgeBase);
  const index = knowledgeBases.value.findIndex(item => item.id === normalized.id);
  if (index >= 0) {
    knowledgeBases.value[index] = {
      ...knowledgeBases.value[index],
      ...normalized,
    };
  } else {
    knowledgeBases.value = [normalized, ...knowledgeBases.value];
  }
  return normalized;
};

const clearSelectedKnowledgeBaseRefresh = () => {
  if (selectedKnowledgeBaseRefreshTimer) {
    window.clearTimeout(selectedKnowledgeBaseRefreshTimer);
    selectedKnowledgeBaseRefreshTimer = null;
  }
};

const scheduleSelectedKnowledgeBaseRefresh = () => {
  clearSelectedKnowledgeBaseRefresh();

  if (!selectedKnowledgeBaseId.value || document.hidden) {
    return;
  }

  const refreshDelay = hasInProgressFiles.value
    ? SELECTED_KB_ACTIVE_REFRESH_MS
    : SELECTED_KB_REFRESH_MS;

  selectedKnowledgeBaseRefreshTimer = window.setTimeout(async () => {
    try {
      await refreshSelectedKnowledgeBase(true);
    } finally {
      scheduleSelectedKnowledgeBaseRefresh();
    }
  }, refreshDelay);
};

const loadKnowledgeBases = async (preferredId = null) => {
  loading.value = true;
  try {
    const response = await ragApi.listKnowledgeBases();
    if (!response.data.success) {
      throw new Error(response.data.message || '加载知识库失败');
    }

    knowledgeBases.value = (response.data.data || []).map(normalizeKnowledgeBase);
    const nextId = preferredId || selectedKnowledgeBaseId.value || knowledgeBases.value[0]?.id || null;
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
    scheduleSelectedKnowledgeBaseRefresh();
  }
};

const fetchKnowledgeBaseDetail = async (knowledgeBaseId, { silent = false } = {}) => {
  try {
    const response = await ragApi.getKnowledgeBase(knowledgeBaseId);
    if (!response.data.success) {
      throw new Error(response.data.message || '加载知识库详情失败');
    }

    const detail = response.data.data;
    upsertKnowledgeBase(detail);
    return detail;
  } catch (error) {
    console.error('加载知识库详情失败:', error);
    if (!silent) {
      ElMessage.error(error.response?.data?.message || error.message || '加载知识库详情失败');
    }
    throw error;
  }
};

const selectKnowledgeBase = async (knowledgeBaseId) => {
  selectedKnowledgeBaseId.value = knowledgeBaseId;
  searchResults.value = [];
  searchQuery.value = '';
  hasSearched.value = false;

  if (!knowledgeBaseId) {
    clearSelectedKnowledgeBaseRefresh();
    return;
  }

  try {
    await fetchKnowledgeBaseDetail(knowledgeBaseId);
  } finally {
    scheduleSelectedKnowledgeBaseRefresh();
  }
};

const refreshSelectedKnowledgeBase = async (silent = false) => {
  if (!selectedKnowledgeBaseId.value) {
    return;
  }

  await fetchKnowledgeBaseDetail(selectedKnowledgeBaseId.value, { silent });
};

const handleWindowFocus = async () => {
  await loadKnowledgeBases(selectedKnowledgeBaseId.value);
};

const handleVisibilityChange = async () => {
  if (document.hidden) {
    clearSelectedKnowledgeBaseRefresh();
    return;
  }

  await loadKnowledgeBases(selectedKnowledgeBaseId.value);
};

const resetCreateForm = () => {
  createForm.value = {
    name: '',
    description: '',
  };
  createFormError.value = '';
};

const openCreateKnowledgeBase = async () => {
  resetCreateForm();
  createDialogVisible.value = true;
  await nextTick();
  createNameInput.value?.focus();
};

const toggleCreateKnowledgeBase = async () => {
  if (createDialogVisible.value) {
    closeCreateKnowledgeBase();
    return;
  }

  await openCreateKnowledgeBase();
};

const closeCreateKnowledgeBase = (force = false) => {
  if (creating.value && !force) {
    return;
  }
  createDialogVisible.value = false;
  createFormError.value = '';
};

const submitCreateKnowledgeBase = async () => {
  const name = createForm.value.name.trim();
  const description = createForm.value.description.trim();

  if (!name) {
    createFormError.value = '请输入知识库名称';
    await nextTick();
    createNameInput.value?.focus();
    return;
  }

  createFormError.value = '';
  creating.value = true;

  try {
    const response = await ragApi.createKnowledgeBase({ name, description: description || '' });
    if (!response.data.success) {
      throw new Error(response.data.message || '创建知识库失败');
    }

    const createdKnowledgeBase = upsertKnowledgeBase({
      ...response.data.data,
      name,
      description,
    });

    selectedKnowledgeBaseId.value = createdKnowledgeBase.id;
    activeTab.value = 'upload';
    searchResults.value = [];
    searchQuery.value = '';
    hasSearched.value = false;
    closeCreateKnowledgeBase(true);
    ElMessage.success('知识库创建成功');

    if (createdKnowledgeBase.id) {
      try {
        await selectKnowledgeBase(createdKnowledgeBase.id);
        await loadKnowledgeBases(createdKnowledgeBase.id);
      } catch (detailError) {
        console.warn('创建后拉取知识库详情失败，保留本地结果', detailError);
      }
    } else {
      await loadKnowledgeBases();
    }
  } catch (error) {
    console.error('创建知识库失败:', error);
    createFormError.value = error.response?.data?.message || error.message || '创建知识库失败';
  } finally {
    creating.value = false;
  }
};

const triggerFilePicker = () => {
  if (!currentKnowledgeBase.value || uploading.value) {
    return;
  }
  fileInput.value?.click();
};

const uploadFiles = async (files) => {
  if (!files.length || !selectedKnowledgeBaseId.value) {
    return;
  }

  uploading.value = true;
  let successCount = 0;

  try {
    for (const file of files) {
      const response = await ragApi.uploadFile(selectedKnowledgeBaseId.value, file);
      if (!response.data.success) {
        throw new Error(response.data.message || `上传失败：${file.name}`);
      }
      successCount += 1;
    }

    ElMessage.success(successCount > 1 ? `已上传 ${successCount} 个文档` : `文档已入库：${files[0].name}`);
    await loadKnowledgeBases(selectedKnowledgeBaseId.value);
  } catch (error) {
    console.error('上传文档失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '上传文档失败');
  } finally {
    uploading.value = false;
    isDragging.value = false;
  }
};

const handleFileChange = async (event) => {
  const files = Array.from(event.target.files || []);
  event.target.value = '';
  await uploadFiles(files);
};

const handleDrop = async (event) => {
  isDragging.value = false;
  if (uploading.value) {
    return;
  }
  const files = Array.from(event.dataTransfer?.files || []);
  await uploadFiles(files);
};

const runSearch = async () => {
  if (!selectedKnowledgeBaseId.value || !searchQuery.value) {
    return;
  }

  searching.value = true;
  hasSearched.value = true;
  try {
    const response = await ragApi.search(selectedKnowledgeBaseId.value, searchQuery.value, 5);
    if (!response.data.success) {
      throw new Error(response.data.message || '知识库检索失败');
    }
    searchResults.value = response.data.data || [];
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
    await loadKnowledgeBases(selectedKnowledgeBaseId.value);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
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
    searchQuery.value = '';
    hasSearched.value = false;
    ElMessage.success('知识库已删除');
    await loadKnowledgeBases(selectedKnowledgeBaseId.value);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.response?.data?.message || error.message || '删除知识库失败');
    }
  }
};

const knowledgeBaseStatusLabel = (status) => {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return 'ACTIVE';
    case 'PROCESSING':
      return 'PROCESSING';
    case 'ARCHIVED':
      return 'ARCHIVED';
    default:
      return 'UNKNOWN';
  }
};

const knowledgeBaseStatusClass = (status) => {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return 'status-active';
    case 'PROCESSING':
      return 'status-processing';
    case 'ARCHIVED':
      return 'status-archived';
    default:
      return 'status-archived';
  }
};

const fileStatusLabel = (status) => {
  switch ((status || '').toUpperCase()) {
    case 'COMPLETED':
      return '已完成';
    case 'FAILED':
      return '失败';
    case 'PENDING':
      return '待处理';
    case 'UPLOADING':
    case 'PARSING':
    case 'SPLITTING':
    case 'VECTORIZING':
      return '处理中';
    default:
      return '处理中';
  }
};

const fileStatusClass = (status) => {
  switch ((status || '').toUpperCase()) {
    case 'COMPLETED':
      return 'file-status-completed';
    case 'FAILED':
      return 'file-status-failed';
    case 'PENDING':
      return 'file-status-pending';
    default:
      return 'file-status-processing';
  }
};

const formatFileType = (fileType) => (fileType || 'FILE').toUpperCase();

const formatFileSize = (size) => {
  if (!size) return '0 B';
  if (size < 1000) return `${size} B`;
  if (size < 1000 * 1000) return `${(size / 1000).toFixed(1)} KB`;
  return `${(size / (1000 * 1000)).toFixed(1)} MB`;
};

const formatDateTime = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
};

const formatSearchScore = (score) => Number(score || 0).toFixed(4);

const shouldRenderSearchResultAsMarkdown = (item) => {
  const title = (item?.title || '').toLowerCase();
  if (['.md', '.markdown', '.mdx'].some(ext => title.endsWith(ext))) {
    return true;
  }

  const content = item?.content || '';
  return /(^|\n)\s{0,3}(#{1,6}\s|[-*+]\s|\d+\.\s|```|>\s)/.test(content);
};

const renderSearchResultMarkdown = (content) => {
  if (!content) {
    return '';
  }

  marked.setOptions({
    gfm: true,
    breaks: true,
  });

  return DOMPurify.sanitize(marked.parse(content));
};

const isFileInProgress = (file) => !['COMPLETED', 'FAILED'].includes((file?.status || '').toUpperCase());

const displayChunkCount = (file) => (isFileInProgress(file) ? '—' : Number(file.chunkCount || 0));

const displayVectorizedCount = (file) => (isFileInProgress(file) ? '—' : Number(file.vectorizedChunkCount || 0));

onMounted(() => {
  loadKnowledgeBases();
  window.addEventListener('focus', handleWindowFocus);
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  clearSelectedKnowledgeBaseRefresh();
  window.removeEventListener('focus', handleWindowFocus);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});
</script>

<style scoped>
.knowledge-base-page {
  --page-bg: #f7fbff;
  --panel-bg: rgba(255, 255, 255, 0.82);
  --panel-strong: rgba(255, 255, 255, 0.92);
  --panel-border: rgba(148, 163, 184, 0.22);
  --panel-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  --text-main: #0f172a;
  --text-subtle: #64748b;
  --text-muted: #94a3b8;
  --blue-main: #0ea5e9;
  --blue-strong: #0284c7;
  --blue-soft: rgba(14, 165, 233, 0.12);
  --blue-soft-strong: rgba(14, 165, 233, 0.2);
  --line-color: rgba(15, 23, 42, 0.05);
  --danger: #ef4444;
  position: relative;
  display: grid;
  grid-template-columns: 318px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--page-bg);
  overflow: hidden;
}

.page-grid,
.page-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.page-grid {
  background-image:
    linear-gradient(var(--line-color) 1px, transparent 1px),
    linear-gradient(90deg, var(--line-color) 1px, transparent 1px);
  background-size: 54px 54px;
}

.page-glow {
  filter: blur(70px);
  opacity: 0.5;
}

.glow-top {
  top: -180px;
  right: -120px;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.16) 0%, transparent 68%);
}

.glow-bottom {
  bottom: -180px;
  left: -120px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.12) 0%, transparent 70%);
}

.knowledge-sidebar,
.knowledge-main {
  position: relative;
  z-index: 1;
}

.knowledge-sidebar {
  display: flex;
  flex-direction: column;
  padding: 18px 16px 14px;
  border-right: 1px solid rgba(203, 213, 225, 0.75);
  background: rgba(248, 252, 255, 0.82);
  backdrop-filter: blur(18px);
}

.sidebar-top {
  position: relative;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
}

.sidebar-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.brand-icon,
.main-header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(6, 182, 212, 0.22));
  color: var(--blue-main);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.brand-icon {
  width: 40px;
  height: 40px;
}

.brand-icon svg,
.main-header-icon svg,
.knowledge-card-meta svg,
.summary-item svg,
.stat-card-title svg,
.file-icon svg,
.create-button svg,
.back-home-button svg,
.tab-button svg,
.upload-more-button svg,
.delete-knowledge-button svg,
.delete-file-button svg,
.create-kb-modal-header svg,
.upload-icon-shell svg,
.search-input-shell svg,
.search-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sidebar-title-row h1,
.main-header-copy h2,
.files-head h3 {
  margin: 0;
  color: var(--text-main);
}

.sidebar-title-row h1 {
  font-size: 18px;
  font-weight: 700;
}

.sidebar-title-row p,
.main-header-copy p,
.files-head p {
  margin: 4px 0 0;
  color: var(--text-subtle);
  font-size: 13px;
  line-height: 1.4;
}

.create-button,
.back-home-button,
.delete-knowledge-button,
.search-button,
.upload-more-button,
.delete-file-button {
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.create-button {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 48px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(203, 213, 225, 0.92);
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(148, 163, 184, 0.08);
}

.create-button:hover:not(:disabled) {
  border-color: rgba(14, 165, 233, 0.36);
  color: var(--blue-strong);
}

.create-button:disabled,
.search-button:disabled,
.upload-more-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.knowledge-list {
  flex: 1;
  padding: 18px 0;
  overflow: auto;
}

.knowledge-card {
  width: 100%;
  margin-bottom: 12px;
  padding: 16px 14px;
  text-align: left;
  border-radius: 22px;
  border: 1px solid transparent;
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.knowledge-card:hover {
  background: rgba(255, 255, 255, 0.62);
  border-color: rgba(203, 213, 225, 0.72);
}

.knowledge-card.active {
  background: rgba(125, 211, 252, 0.22);
  border-color: rgba(14, 165, 233, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.knowledge-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.knowledge-card-head strong {
  display: block;
  color: #0b4d6b;
  font-size: 16px;
  line-height: 1.35;
}

.knowledge-card p {
  margin: 8px 0 12px;
  color: var(--text-subtle);
  font-size: 13px;
  line-height: 1.45;
}

.knowledge-card-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--text-subtle);
  font-size: 13px;
}

.knowledge-card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.knowledge-card-meta svg {
  width: 15px;
  height: 15px;
  color: #64748b;
}

.knowledge-status-chip,
.file-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 66px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-active {
  background: rgba(134, 239, 172, 0.3);
  color: #22c55e;
}

.status-processing {
  background: rgba(125, 211, 252, 0.34);
  color: #0284c7;
}

.status-archived {
  background: rgba(226, 232, 240, 0.88);
  color: #64748b;
}

.sidebar-empty {
  padding: 18px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.52);
  color: var(--text-subtle);
  font-size: 13px;
  line-height: 1.6;
}

.back-home-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 10px 4px 6px 0;
  background: transparent;
  color: var(--text-subtle);
  font-size: 14px;
}

.back-home-button:hover {
  color: var(--blue-strong);
}

.knowledge-main {
  display: flex;
  min-width: 0;
}

.main-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 34px 16px;
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
}

.main-header-info {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.main-header-icon {
  width: 48px;
  height: 48px;
  color: var(--blue-main);
}

.main-header-copy h2 {
  font-size: 19px;
  font-weight: 700;
}

.main-header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-pill {
  display: inline-flex;
  align-items: center;
  height: 46px;
  padding: 0 16px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.88);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 22px rgba(148, 163, 184, 0.08);
}

.summary-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-subtle);
  font-size: 14px;
}

.summary-item strong {
  color: var(--text-main);
  font-size: 14px;
}

.summary-item svg {
  width: 17px;
  height: 17px;
  color: var(--blue-main);
}

.summary-divider {
  width: 1px;
  height: 20px;
  margin: 0 14px;
  background: rgba(203, 213, 225, 0.94);
}

.delete-knowledge-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(248, 113, 113, 0.52);
  color: var(--danger);
  font-size: 14px;
  font-weight: 700;
}

.delete-knowledge-button:hover {
  background: rgba(254, 242, 242, 0.95);
}

.tab-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 34px;
  height: 54px;
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(16px);
}

.tab-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 100%;
  padding: 0 4px;
  border: none;
  background: transparent;
  color: var(--text-subtle);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.tab-button.active {
  color: var(--blue-main);
}

.tab-button.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #0ea5e9, #38bdf8);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.18);
  color: var(--blue-main);
  font-size: 12px;
  font-weight: 700;
}

.tab-panel {
  flex: 1;
  padding: 36px 34px;
  overflow: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

.stat-card {
  min-height: 94px;
  padding: 18px 22px;
  border-radius: 22px;
  border: 1px solid var(--panel-border);
  background: var(--panel-strong);
  box-shadow: var(--panel-shadow);
}

.stat-card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
}

.stat-card-title svg {
  color: var(--blue-main);
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text-main);
  font-size: 44px;
  line-height: 1;
  font-weight: 800;
}

.upload-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 250px;
  border-radius: 24px;
  border: 2px dashed rgba(186, 230, 253, 0.9);
  background: rgba(255, 255, 255, 0.28);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-dropzone:hover,
.upload-dropzone.dragging {
  border-color: rgba(14, 165, 233, 0.54);
  background: rgba(255, 255, 255, 0.44);
}

.upload-dropzone.disabled {
  cursor: wait;
}

.upload-icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--blue-main);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.12);
}

.upload-dropzone h3 {
  margin: 16px 0 10px;
  color: var(--text-main);
  font-size: 18px;
  font-weight: 700;
}

.upload-dropzone h3 span {
  color: var(--blue-main);
}

.upload-dropzone p {
  margin: 0;
  color: var(--text-subtle);
  font-size: 15px;
}

.upload-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 18px;
}

.upload-tags span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.88);
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-subtle);
  font-size: 14px;
}

.search-notice {
  margin-bottom: 18px;
  padding: 14px 18px;
  border-radius: 18px;
  border: 1px solid rgba(251, 191, 36, 0.5);
  background: rgba(255, 247, 237, 0.94);
  color: #f97316;
  font-size: 15px;
  font-weight: 600;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input-shell {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  height: 48px;
  padding: 0 16px;
  border-radius: 18px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  background: rgba(255, 255, 255, 0.76);
  color: var(--text-muted);
}

.search-input-shell input {
  flex: 1;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--text-main);
  font-size: 16px;
  outline: none;
}

.search-input-shell input::placeholder {
  color: #94a3b8;
}

.search-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100px;
  height: 48px;
  border-radius: 18px;
  background: rgba(240, 249, 255, 0.95);
  color: #334155;
  box-shadow: 0 10px 22px rgba(148, 163, 184, 0.08);
}

.search-button:not(:disabled):hover {
  background: rgba(224, 242, 254, 0.95);
  color: var(--blue-strong);
}

.search-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  color: #94a3b8;
  font-size: 18px;
  text-align: center;
}

.search-results {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.search-result-card {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid var(--panel-border);
  background: var(--panel-strong);
  box-shadow: var(--panel-shadow);
}

.search-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-subtle);
  font-size: 13px;
}

.search-result-head strong {
  color: var(--text-main);
  font-size: 15px;
}

.search-result-card p {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.files-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.upload-more-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.2);
}

.upload-more-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 32px rgba(14, 165, 233, 0.24);
}

.file-table-shell {
  border-radius: 22px;
  border: 1px solid rgba(203, 213, 225, 0.82);
  background: rgba(255, 255, 255, 0.38);
  overflow: hidden;
}

.file-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.file-table thead th {
  padding: 14px 22px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.95);
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  background: rgba(255, 255, 255, 0.72);
}

.file-table tbody td {
  padding: 18px 22px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  vertical-align: middle;
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.26);
}

.file-table tbody tr:last-child td {
  border-bottom: none;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(240, 249, 255, 0.95);
  color: var(--blue-main);
}

.file-name-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name-copy strong {
  color: var(--text-main);
  font-size: 15px;
}

.file-name-copy span {
  color: var(--text-subtle);
  font-size: 13px;
}

.file-name-copy em {
  color: #ef4444;
  font-size: 12px;
  font-style: normal;
}

.file-status-completed {
  background: rgba(220, 252, 231, 0.92);
  border: 1px solid rgba(34, 197, 94, 0.24);
  color: #10b981;
}

.file-status-processing {
  background: rgba(219, 234, 254, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.24);
  color: #3b82f6;
}

.file-status-failed {
  background: rgba(254, 226, 226, 0.92);
  border: 1px solid rgba(248, 113, 113, 0.24);
  color: #ef4444;
}

.file-status-pending {
  background: rgba(241, 245, 249, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #64748b;
}

.numeric-cell {
  color: var(--blue-main);
  font-size: 16px;
  font-weight: 700;
}

.time-cell {
  color: #64748b;
  white-space: nowrap;
}

.action-cell {
  width: 60px;
  text-align: right;
}

.delete-file-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
}

.delete-file-button:hover {
  background: rgba(254, 242, 242, 0.95);
  color: var(--danger);
}

.files-empty-state,
.main-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
}

.files-empty-state {
  padding: 48px 24px;
  color: #94a3b8;
  font-size: 16px;
}

.main-state-card {
  padding: 24px 28px;
  border-radius: 24px;
  border: 1px solid var(--panel-border);
  background: var(--panel-bg);
  color: var(--text-subtle);
  font-size: 16px;
  box-shadow: var(--panel-shadow);
}

.create-kb-inline-panel {
  width: 100%;
  margin-top: 18px;
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.create-kb-inline-header {
  width: calc(100% - 12px);
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid rgba(90, 202, 241, 0.8);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(207, 240, 250, 0.96) 0%, rgba(196, 234, 247, 0.92) 100%);
  color: #0396d6;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.02em;
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.08);
  user-select: none;
}

.create-kb-inline-form {
  margin-top: 16px;
  width: calc(100% - 18px);
  padding: 16px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1.5px solid rgba(132, 220, 248, 0.72);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(214, 240, 248, 0.94) 0%, rgba(208, 236, 246, 0.9) 100%);
  box-shadow: 0 18px 36px rgba(14, 165, 233, 0.1);
  box-sizing: border-box;
}

.create-kb-inline-form input,
.create-kb-inline-form textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1.5px solid rgba(220, 230, 238, 0.98);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.98);
  color: #0f172a;
  outline: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.create-kb-inline-form input {
  height: 50px;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 500;
}

.create-kb-inline-form textarea {
  min-height: 78px;
  padding: 14px 16px;
  resize: none;
  font-size: 15px;
  line-height: 1.45;
}

.create-kb-inline-form input::placeholder,
.create-kb-inline-form textarea::placeholder {
  color: #a6b3bf;
}

.create-kb-inline-form input:focus,
.create-kb-inline-form textarea:focus {
  border-color: rgba(90, 202, 241, 0.92);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.08);
}

.create-kb-inline-form input:disabled,
.create-kb-inline-form textarea:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.create-kb-error {
  margin: -2px 4px 2px;
  color: #ef4444;
  font-size: 13px;
  font-weight: 600;
}

.create-kb-submit {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(180deg, #5fc8e8 0%, #47b7db 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.04em;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(71, 183, 219, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.create-kb-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(71, 183, 219, 0.28);
}

.create-kb-submit:disabled {
  cursor: not-allowed;
  filter: saturate(0.8);
  opacity: 0.76;
}

.create-kb-inline-enter-active,
.create-kb-inline-leave-active {
  overflow: hidden;
  transition: max-height 0.28s cubic-bezier(0.2, 0.8, 0.2, 1), opacity 0.2s ease, transform 0.28s ease;
}

.create-kb-inline-enter-from,
.create-kb-inline-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-8px);
}

.create-kb-inline-enter-to,
.create-kb-inline-leave-from {
  opacity: 1;
  max-height: 280px;
  transform: translateY(0);
}

.search-result-markdown {
  margin-top: 12px;
  color: #334155;
  line-height: 1.7;
}

.search-result-markdown :deep(h1),
.search-result-markdown :deep(h2),
.search-result-markdown :deep(h3),
.search-result-markdown :deep(h4) {
  margin: 0.35em 0 0.5em;
  color: var(--text-main);
  font-size: 1em;
  font-weight: 700;
}

.search-result-markdown :deep(p) {
  margin: 0.45em 0;
}

.search-result-markdown :deep(ul),
.search-result-markdown :deep(ol) {
  margin: 0.45em 0;
  padding-left: 1.4em;
}

.search-result-markdown :deep(code) {
  padding: 0.15em 0.35em;
  border-radius: 6px;
  background: rgba(14, 165, 233, 0.08);
  color: #0f172a;
  font-size: 0.92em;
}

.search-result-markdown :deep(pre) {
  margin: 0.6em 0;
  padding: 0.75em 0.9em;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.95);
  overflow-x: auto;
}

.search-result-markdown :deep(pre code) {
  padding: 0;
  background: transparent;
}

.search-result-markdown :deep(blockquote) {
  margin: 0.6em 0;
  padding-left: 0.9em;
  border-left: 3px solid rgba(14, 165, 233, 0.26);
  color: #475569;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .main-header,
  .tab-bar,
  .tab-panel {
    padding-left: 24px;
    padding-right: 24px;
  }

  .main-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 960px) {
  .knowledge-base-page {
    grid-template-columns: 1fr;
  }

  .knowledge-sidebar {
    border-right: none;
    border-bottom: 1px solid rgba(203, 213, 225, 0.75);
  }

  .search-row,
  .files-head,
  .main-header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-pill {
    justify-content: center;
  }
}

@media (max-width: 720px) {
  .tab-bar {
    overflow-x: auto;
  }

  .tab-panel {
    padding-top: 24px;
  }

  .summary-pill {
    width: 100%;
  }

  .summary-item {
    flex: 1;
    justify-content: center;
  }

  .file-table thead {
    display: none;
  }

  .file-table,
  .file-table tbody,
  .file-table tr,
  .file-table td {
    display: block;
    width: 100%;
  }

  .file-table tbody td {
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .file-table tbody tr {
    padding: 12px 0;
    border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  }

  .file-table tbody tr:last-child {
    border-bottom: none;
  }

  .action-cell {
    text-align: left;
  }

  .create-kb-inline-panel {
    width: 100%;
  }

  .create-kb-inline-header {
    width: calc(100% - 10px);
    min-height: 54px;
    border-radius: 22px;
    font-size: 16px;
  }

  .create-kb-inline-form {
    width: calc(100% - 16px);
    padding: 16px 14px;
    border-radius: 22px;
  }

  .create-kb-inline-form input,
  .create-kb-inline-form textarea {
    font-size: 15px;
  }

  .create-kb-inline-form input {
    height: 48px;
  }

  .create-kb-submit {
    height: 46px;
    border-radius: 18px;
    font-size: 15px;
  }
}
</style>
