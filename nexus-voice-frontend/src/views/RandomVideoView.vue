<template>
  <div class="video-view-container">
    <!-- 返回按钮 -->
    <button @click="goBack" class="back-button">
      <span class="back-icon">←</span>
      <span>返回</span>
    </button>

    <!-- 模式切换按钮 -->
    <div class="mode-switcher">
      <button 
        @click="switchMode('single')" 
        :class="['mode-btn', { active: viewMode === 'single' }]">
        单视频
      </button>
      <button 
        @click="switchMode('list')" 
        :class="['mode-btn', { active: viewMode === 'list' }]">
        列表模式
      </button>
      <button 
        @click="switchMode('swipe')" 
        :class="['mode-btn', { active: viewMode === 'swipe' }]">
        滑动模式
      </button>
    </div>

    <!-- 滑动模式容器 -->
    <div v-if="viewMode === 'swipe'" :class="['swipe-container', { 'no-snap': isRotatingSwipe }]" ref="swipeContainer" @scroll="handleSwipeScroll">
      <div 
        v-for="(item, index) in videoCache" 
        :key="item.url || index"
        class="swipe-item">
        <!-- 视频容器 -->
        <div class="swipe-video-wrapper">
          <video
            :ref="el => setVideoRef(el, index)"
            playsinline
            muted
            webkit-playsinline
            x5-playsinline
            preload="auto"
            :data-index="index"
            class="swipe-video"
            @click="handleSwipeVideoClick"
          ></video>
          
          <!-- 加载状态（当前正在播放的视频不显示加载提示） -->
          <div v-if="!item.loaded && !(swipeCurrentIndex === index && !swipeVideoPaused)" class="swipe-loading">
            <div class="spinner"></div>
            <div class="loading-text">加载中...</div>
          </div>
          
          <!-- 播放提示 -->
          <div v-else-if="swipeCurrentIndex === index && swipeVideoPaused" class="swipe-play-hint">
            <div class="play-icon-large">▶</div>
            <div class="play-hint-text">点击播放</div>
          </div>
        </div>

        <!-- 视频信息和控制 -->
        <div class="swipe-info">
          <div class="swipe-info-content">
            <button @click.stop="toggleMute" :class="['swipe-mute-btn', { active: isMuted }]">
              {{ isMuted ? '🔇' : '🔊' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 单视频和列表模式容器 -->
    <div v-else class="player-container" :class="{ 'list-mode': viewMode === 'list' }">
      <!-- 视频容器 -->
      <div class="video-container">
        <!-- 加载状态 -->
        <div v-show="isLoading" class="loading-overlay">
          <div class="spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>

        <!-- 视频包装器 -->
        <div class="video-wrapper" @click="togglePlayPause">
          <video
            ref="videoPlayer"
            class="video-player"
            @loadeddata="onVideoLoaded"
            @play="onPlay"
            @pause="onPause"
            @ended="onEnded"
            @error="onError"
            playsinline
            preload="auto"
          ></video>
        </div>
      </div>

      <!-- 控制按钮 -->
      <div class="controls">
        <button @click="toggleMute" :class="['control-btn', { 'mute-active': isMuted }]">
          {{ isMuted ? '取消静音' : '开启静音' }}
        </button>
        <button @click="toggleAutoPlay" :class="['control-btn', { 'auto-play-active': autoPlayEnabled }]">
          {{ autoPlayEnabled ? '关闭连播' : '自动连播' }}
        </button>
        <button @click="loadNextVideo" class="control-btn">
          下个视频
        </button>
      </div>

      <!-- 预览列表（列表模式） -->
      <div v-if="viewMode === 'list'" class="preview-list">
        <div class="preview-header">
          <span>预览列表</span>
          <span class="cache-count">{{ displayedVideos.length }}/{{ videoCache.length }}</span>
        </div>
        <div class="preview-scroll">
          <div 
            v-for="(item, index) in displayedVideos" 
            :key="index"
            @click="switchToVideo(index)"
            :class="['preview-item', { 
              active: currentVideoIndex === index,
              loading: !item.loaded 
            }]">
            <!-- 缩略图或占位符 -->
            <div class="preview-thumbnail">
              <img v-if="item.thumbnail" :src="item.thumbnail" alt="预览" />
              <div v-else class="preview-placeholder">
                <span class="play-icon">▶</span>
              </div>
            </div>
            <!-- 播放状态指示 -->
            <div v-if="currentVideoIndex === index" class="playing-indicator">
              <span class="pulse-dot"></span>
              <span>播放中</span>
            </div>
            <!-- 加载中指示 -->
            <div v-else-if="!item.loaded" class="loading-indicator">
              <div class="mini-spinner"></div>
            </div>
            <!-- 序号 -->
            <div class="preview-index">{{ index + 1 }}</div>
          </div>
          
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// DOM引用
const videoPlayer = ref(null);
const swipeContainer = ref(null);
const videoRefs = ref([]);

// 计算属性：列表模式显示的视频（当前+下一条）
const displayedVideos = computed(() => {
  return videoCache.value.slice(0, 2);
});


// 状态管理
const VIEW_MODE_KEY = 'randomVideo.viewMode';
const MUTE_KEY = 'randomVideo.isMuted';
const AUTOPLAY_KEY = 'randomVideo.autoPlay';

const viewMode = ref('single'); // 'single' | 'list' | 'swipe'
const isLoading = ref(false);
const isMuted = ref(true);
const autoPlayEnabled = ref(false);
const isPlaying = ref(false);
const playPending = ref(false);
const currentVideoIndex = ref(0);
const swipeCurrentIndex = ref(0);
const swipeVideoPaused = ref(true);
const isRotatingSwipe = ref(false);
let scrollTimeout = null;

// 检测设备类型
const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

// 配置常量（按需加载策略）
const config = {
  MAX_RETRIES: 3,
  MAX_CACHE_SIZE: 2,  // 滑动模式：仅缓存2条（当前+下一条），单视频/列表模式：缓存3条
  PRELOAD_COUNT: 1,   // 仅预加载1条下一个视频
  VIDEO_LOAD_TIMEOUT: 10000,  // 视频加载超时10秒
  API_RATE_LIMIT: 1200,  // API调用最小间隔（毫秒），防止被限流
  BASE_API_URLS: [
    'https://v2.xxapi.cn/api/meinv?return=302',
    'https://api.jkyai.top/API/jxhssp.php',
    'https://api.jkyai.top/API/jxbssp.php',
    'https://api.jkyai.top/API/rmtmsp/api.php',
    'https://api.jkyai.top/API/qcndxl.php',
    'https://www.hhlqilongzhu.cn/api/MP4_xiaojiejie.php',
    'https://api.yujn.cn/api/zzxjj.php?type=video'
  ],
  API_317AK_BASE: 'https://api.317ak.cn/api/',
  API_317AK_PARAMS: '?ckey=1LJFEBWBK52F50SZK82S',
  API_317AK_PATHS: [
    'sp/jmxl', 'jhsp', 'sp/xtmx', 'sp/gzhy', 'sp/ttbm',
    'sp/xqhh', 'sp/xxmm', 'sp/hljj', 'sp/lbjj', 'sp/xlmn',
    'sp/ttmn', 'sp/lbll', 'sp/duilian', 'sp/qmtj', 'sp/didjj',
    'sp/lbxg', 'sp/ldxl', 'sp/qfhy', 'sp/zycx', 'sp/hzxl',
    'sp/xjxl', 'sp/aqxl', 'sp/blsx', 'sp/yexl', 'sp/dexl',
    'sp/kkxl', 'sp/qbhc', 'sp/yjxl', 'sp/slxl', 'sp/ndxl',
    'sp/ddsp', 'sp/mncd', 'sp/rwxl', 'sp/smwx', 'sp/ywxl',
    'sp/slmm', 'sp/btqb', 'sp/cqng', 'sp/jpyz', 'sp/zyqx',
    'sp/yzmt', 'sp/xxxl', 'sp/qcxl', 'sp/myxl', 'sp/mhyx',
    'sp/cosxl', 'sp/yzxl', 'sp/qttj', 'sp/llxl', 'sp/gjbz',
    'sp/dmbz', 'sp/hcyx', 'sp/dxbz', 'sp/ddxl', 'sp/cblx',
    'sp/bybz', 'sp/hssp', 'sp/yjsp', 'sp/amxx', 'sp/wpxl',
    'sp/yqkd', 'sp/hbss', 'sp/mxny', 'sp/npxl', 'sp/ycyy',
    'sp/sqxl', 'sp/yssp', 'sp/bssp'
  ]
};

// 缓存管理（简单队列）
const videoCache = ref([]);  // 视频队列，最多3条
const isCaching = ref(false);
let currentApiIndex = -1;
let current317akIndex = -1;
let retryAttempts = 0;
// 移除黑屏定时重置逻辑，改为最小缓存策略降低卡顿
let lastApiCallTime = 0;  // 上次API调用时间，用于频率限制

// 生成视频缩略图
const generateThumbnail = (videoElement) => {
  try {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    canvas.width = 120;
    canvas.height = 213;
    ctx.drawImage(videoElement, 0, 0, 120, 213);
    return canvas.toDataURL('image/jpeg', 0.7);
  } catch (error) {
    // CORS跨域导致canvas污染，静默处理，使用占位符
    if (error.name === 'SecurityError') {
      return null;
    }
    console.error('生成缩略图失败:', error);
    return null;
  }
};

// 切换到指定视频（列表模式使用，保持两条缓存语义）
const switchToVideo = (index) => {
  if (index < 0 || index >= videoCache.value.length) return;
  const item = videoCache.value[index];
  if (!item || !item.loaded) return;

  if (index === currentVideoIndex.value) {
    // 点击当前，直接按缓存播放
    loadVideoFromCache();
    return;
  }
  if (index === currentVideoIndex.value + 1) {
    // 点击下一条，后续进行预取+轮转
    currentVideoIndex.value = index;
    loadVideoFromCache();
    return;
  }
};

// 设置视频引用（简单的ref管理）
const setVideoRef = (el, index) => {
  if (el) {
    videoRefs.value[index] = el;
    
    // 加载/更新视频源（确保旋转后能正确切换）
    const cached = videoCache.value[index];
    if (cached?.loaded && el.src !== cached.url) {
      el.src = cached.url;
    }
    
    // 设置基本属性
    el.muted = isMuted.value;
    el.loop = true;
    
    // 监听播放状态（仅在滑动模式下，避免重复绑定 & 索引错位）
    if (viewMode.value === 'swipe') {
      if (el._onPlayListener) {
        el.removeEventListener('play', el._onPlayListener);
      }
      if (el._onPauseListener) {
        el.removeEventListener('pause', el._onPauseListener);
      }

      el._onPlayListener = () => {
        const i = Number(el.dataset.index ?? index);
        if (i === swipeCurrentIndex.value) {
          swipeVideoPaused.value = false;
        }
      };
      el._onPauseListener = () => {
        const i = Number(el.dataset.index ?? index);
        if (i === swipeCurrentIndex.value) {
          swipeVideoPaused.value = true;
        }
      };

      el.addEventListener('play', el._onPlayListener);
      el.addEventListener('pause', el._onPauseListener);
    }
  } else {
    // 元素移除，清理引用
    if (videoRefs.value[index]) {
      videoRefs.value[index].pause();
      videoRefs.value[index] = null;
    }
  }
};

// 模式切换
const switchMode = async (mode) => {
  // 暂停当前播放
  if (viewMode.value === 'swipe') {
    pauseAllSwipeVideos();
  } else if (videoPlayer.value) {
    safePause();
  }

  viewMode.value = mode;
  try { localStorage.setItem(VIEW_MODE_KEY, mode); } catch (e) {}

  // 切换到滑动模式时，初始化视频
  if (mode === 'swipe') {
    await nextTick();
    initSwipeMode();
  } else if (mode === 'single' || mode === 'list') {
    // 切回其他模式时，恢复播放
    await nextTick();
    if (videoPlayer.value) {
      loadVideoFromCache();
    }
  }
};

// 初始化滑动模式（仅加载2条：当前+下一条）
const initSwipeMode = async () => {
  swipeCurrentIndex.value = 0;
  
  // 清空旧缓存
  pauseAllSwipeVideos();
  videoCache.value = [];
  videoRefs.value = [];
  
  // 加载2条视频（当前+下一条）
  console.log('🎬 滑动模式：加载2条视频...');
  for (let i = 0; i < 2; i++) {
    try {
      const videoUrl = await getRandomAPI();
      const videoData = {
        url: videoUrl,
        loaded: false,
        thumbnail: null,
        loadError: false
      };
      videoCache.value.push(videoData);
      await loadVideo(i);
    } catch (error) {
      console.error(`初始化视频${i}失败:`, error);
    }
  }
  
  // 等待DOM渲染
  await nextTick();
  
  setTimeout(() => {
    // 为所有视频设置源和属性
    videoCache.value.forEach((item, index) => {
      if (item.loaded && videoRefs.value[index]) {
        const video = videoRefs.value[index];
        if (!video.src) {
          video.src = item.url;
        }
        video.muted = isMuted.value;
        video.loop = true;
      }
    });

    // 播放第一个视频
    if (videoRefs.value[0]) {
      playSwipeVideo(0, false);
    }
  }, 100);
};

// 播放指定滑动视频（保证尽量自动播放，必要时强制静音以通过策略）
const playSwipeVideo = async (index, userInitiated = false) => {
  const video = videoRefs.value[index];
  if (!video) {
    console.log(`⚠️ 视频元素不存在 索引${index}`);
    return;
  }
  if (!videoCache.value[index]?.loaded) {
    console.log(`⏳ 视频还未加载完成 索引${index}，等待加载后自动播放`);
    return;
  }
  
  // 先暂停所有其他视频（确保只有一个播放源）
  videoRefs.value.forEach((v, i) => {
    if (v && i !== index && !v.paused) {
      v.pause();
      console.log(`⏸️ 暂停视频索引${i}`);
    }
  });
  
  // 自动播放时优先静音以提高成功率；用户点击则遵循用户偏好
  video.muted = userInitiated ? isMuted.value : true;

  let retryCount = 0;
  const maxRetries = 3;

  const attemptPlay = async () => {
    try {
      // 确保视频已加载
      if (video.readyState < 2) {
        await new Promise(resolve => {
          video.addEventListener('loadeddata', resolve, { once: true });
          video.load();
        });
      }

      await video.play();
      // 播放成功，状态会由play事件监听器自动更新
      console.log(`滑动视频 ${index} 播放成功`);
      // 若是用户未触发且用户偏好为非静音，不强制修改全局；必要时可在用户交互后再恢复
    } catch (error) {
      console.error(`滑动视频播放失败 (尝试 ${retryCount + 1}/${maxRetries}):`, error);
      
      if (error.name === 'NotAllowedError' || error.name === 'NotSupportedError') {
        // 浏览器阻止自动播放：开启静音并同步全局静音偏好，重试
        if (!video.muted) video.muted = true;
        if (!isMuted.value) isMuted.value = true;
        try { localStorage.setItem(MUTE_KEY, JSON.stringify(true)); } catch (e) {}
        if (retryCount < maxRetries) {
          retryCount++;
          setTimeout(attemptPlay, 400 * retryCount);
          return;
        }
        console.log('视频需要用户交互才能播放，等待用户点击');
        // 播放失败，状态会由pause事件监听器自动更新（或手动设置）
        if (index === swipeCurrentIndex.value) {
          swipeVideoPaused.value = true;
        }
      } else if (error.name === 'AbortError' && retryCount < maxRetries) {
        // 播放被中断，重试
        retryCount++;
        setTimeout(attemptPlay, 300);
      }
    }
  };

  await attemptPlay();
};

// 暂停所有滑动视频
const pauseAllSwipeVideos = () => {
  videoRefs.value.forEach(video => {
    if (video && !video.paused) {
      video.pause();
    }
  });
};

// 滑动处理（极简：仅保留当前+下一条）
const handleSwipeScroll = () => {
  if (!swipeContainer.value) return;

  clearTimeout(scrollTimeout);
  scrollTimeout = setTimeout(async () => {
    const container = swipeContainer.value;
    const scrollTop = container.scrollTop;
    const itemHeight = container.clientHeight;
    const newIndex = Math.round(scrollTop / itemHeight);

    if (newIndex !== swipeCurrentIndex.value && newIndex >= 0 && newIndex < videoCache.value.length) {
      // 暂停当前视频
      const currentVideo = videoRefs.value[swipeCurrentIndex.value];
      if (currentVideo && !currentVideo.paused) {
        currentVideo.pause();
      }

      // 更新索引
      const oldIndex = swipeCurrentIndex.value;
      swipeCurrentIndex.value = newIndex;
      console.log(`📍 滑动: 索引${oldIndex} → ${newIndex}`);

      // 播放新视频
      playSwipeVideo(newIndex, false);

      // 1) 到达索引1：确保存在下一条（第三条），不删除上一条，允许上滑回看
      if (newIndex === 1 && !isCaching.value) {
        if (videoCache.value.length < 3) {
          console.log('➕ 索引1：预取下一条，保留上一条');
          isCaching.value = true;
          try {
            const videoUrl = await getRandomAPI();
            const videoData = {
              url: videoUrl,
              loaded: false,
              thumbnail: null,
              loadError: false
            };
            videoCache.value.push(videoData);
            await loadVideo(videoCache.value.length - 1);
          } catch (error) {
            console.error('预加载失败:', error);
          } finally {
            isCaching.value = false;
          }
        }
      }

      // 2) 到达索引>=2：向前滑动，执行窗口轮转，维持最多3条（上一条、当前、下一条）
      if (newIndex >= 2 && !isCaching.value) {
        console.log('🔄 索引>=2：执行轮转，维持3条窗口');
        isCaching.value = true;
        isRotatingSwipe.value = true;
        try {
          // 预取一条作为新的“下一条”
          const videoUrl = await getRandomAPI();
          const videoData = {
            url: videoUrl,
            loaded: false,
            thumbnail: null,
            loadError: false
          };
          videoCache.value.push(videoData);
          await loadVideo(videoCache.value.length - 1);

          // 移除最旧一条
          videoCache.value.shift();
          videoRefs.value.shift();

          // 新的当前索引回退1
          swipeCurrentIndex.value = newIndex - 1;

          // 调整滚动位置对齐
          await nextTick();
          if (swipeContainer.value) {
            const h = swipeContainer.value.clientHeight;
            swipeContainer.value.scrollTop = h * swipeCurrentIndex.value;
          }

          console.log(`✅ 轮转完成，当前索引: ${swipeCurrentIndex.value}，缓存数: ${videoCache.value.length}`);
        } catch (error) {
          console.error('轮转失败:', error);
        } finally {
          isCaching.value = false;
          requestAnimationFrame(() => { isRotatingSwipe.value = false; });
        }
      }
    }
  }, 150);
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 生成带时间戳的URL避免缓存
const getTimestampUrl = (url) => {
  // 如果已有查询参数，则使用&拼接，否则使用?
  const hasQuery = url.includes('?');
  return `${url}${hasQuery ? '&' : '?'}t=${Date.now()}`;
};

// API频率控制：确保调用间隔不小于配置的限制
const waitForApiRateLimit = async () => {
  const now = Date.now();
  const timeSinceLastCall = now - lastApiCallTime;
  
  if (timeSinceLastCall < config.API_RATE_LIMIT) {
    const waitTime = config.API_RATE_LIMIT - timeSinceLastCall;
    console.log(`⏰ API频率限制，等待${waitTime}ms...`);
    await new Promise(resolve => setTimeout(resolve, waitTime));
  }
  
  lastApiCallTime = Date.now();
};

// 获取随机API（带频率控制）
const getRandomAPI = async () => {
  // 频率控制
  await waitForApiRateLimit();
  
  const use317ak = Math.random() > 0.5;

  if (use317ak && config.API_317AK_PATHS.length > 0) {
    let newIndex;
    do {
      newIndex = Math.floor(Math.random() * config.API_317AK_PATHS.length);
    } while (newIndex === current317akIndex && config.API_317AK_PATHS.length > 1);

    current317akIndex = newIndex;
    const fullUrl = `${config.API_317AK_BASE}${config.API_317AK_PATHS[newIndex]}${config.API_317AK_PARAMS}`;
    return getTimestampUrl(fullUrl);
  } else {
    let newIndex;
    do {
      newIndex = Math.floor(Math.random() * config.BASE_API_URLS.length);
    } while (newIndex === currentApiIndex && config.BASE_API_URLS.length > 1);

    currentApiIndex = newIndex;
    return getTimestampUrl(config.BASE_API_URLS[newIndex]);
  }
};


// 安全播放视频
const safePlay = async () => {
  if (playPending.value || !videoPlayer.value) return;

  playPending.value = true;

  try {
    if (!videoPlayer.value.paused) {
      playPending.value = false;
      return;
    }

    await videoPlayer.value.play();
    isPlaying.value = true;
  } catch (error) {
    console.error('播放失败:', error);
    
    if (error.name === 'NotAllowedError') {
      // 自动播放被阻止，尝试静音后播放
      videoPlayer.value.muted = true;
      isMuted.value = true;
      try {
        await videoPlayer.value.play();
        isPlaying.value = true;
      } catch (err) {
        console.error('静音后播放仍失败:', err);
      }
    }
  } finally {
    playPending.value = false;
  }
};

// 安全暂停视频
const safePause = () => {
  if (playPending.value || !videoPlayer.value) return;

  if (!videoPlayer.value.paused) {
    videoPlayer.value.pause();
    isPlaying.value = false;
  }
};

// 滑动视频点击处理
const handleSwipeVideoClick = async () => {
  const video = videoRefs.value[swipeCurrentIndex.value];
  if (!video) return;

  if (video.paused) {
    await playSwipeVideo(swipeCurrentIndex.value, true);
  } else {
    video.pause();
  }
};

// 切换播放/暂停
const togglePlayPause = () => {
  if (playPending.value) return;

  // 滑动模式下特殊处理
  if (viewMode.value === 'swipe') {
    handleSwipeVideoClick();
    return;
  }

  // 单视频和列表模式
  if (videoPlayer.value) {
    if (videoPlayer.value.paused) {
      safePlay();
    } else {
      safePause();
    }
  }
};


// 加载当前视频（统一走两条缓存逻辑）
const loadCurrentVideo = async () => {
  if (!videoPlayer.value) return;
  safePause();
  isLoading.value = true;
  await ensureMinimumCache();
  currentVideoIndex.value = 0;
  await loadVideoFromCache();
};

// 从缓存加载视频（单视频/列表模式）
const loadVideoFromCache = async () => {
  // 确保当前+下一条缓存
  await ensureMinimumCache();

  if (videoCache.value.length === 0) return;
  if (currentVideoIndex.value >= videoCache.value.length) {
    currentVideoIndex.value = 0;
  }

  const cachedVideo = videoCache.value[currentVideoIndex.value];
  if (cachedVideo && cachedVideo.loaded && videoPlayer.value) {
    isLoading.value = true;
    videoPlayer.value.src = cachedVideo.url;
    videoPlayer.value.loop = !autoPlayEnabled.value;

    // 如果切至下一条（索引1），预取新视频并轮转，始终保持2条缓存
    if (currentVideoIndex.value === 1) {
      await prefetchAndRotate();
    }
  }
};


// 确保仅保留“当前+下一条”两个缓存项
const ensureMinimumCache = async () => {
  const minSize = 2;
  while (videoCache.value.length < minSize && !isCaching.value) {
    isCaching.value = true;
    try {
      const videoUrl = await getRandomAPI();  // 带频率限制
      const videoData = {
        url: videoUrl,
        loaded: false,
        thumbnail: null,
        loadError: false
      };
      videoCache.value.push(videoData);
      await loadVideo(videoCache.value.length - 1);
    } catch (error) {
      console.error('补充缓存失败:', error);
    } finally {
      isCaching.value = false;
    }
  }
  // 超出容量时裁剪
  if (videoCache.value.length > minSize) {
    videoCache.value.splice(minSize);
  }
};

// 预取一条并轮转（用于单视频/列表模式“下一条”播放后）
const prefetchAndRotate = async () => {
  if (isCaching.value) return;
  isCaching.value = true;
  try {
    const videoUrl = await getRandomAPI();
    const videoData = {
      url: videoUrl,
      loaded: false,
      thumbnail: null,
      loadError: false
    };
    videoCache.value.push(videoData);
    const newIdx = videoCache.value.length - 1;
    await loadVideo(newIdx);

    // 轮转：删除已播的第一条，当前索引回到0
    videoCache.value.shift();
    currentVideoIndex.value = 0;
  } catch (e) {
    console.error('预取/轮转失败:', e);
  } finally {
    isCaching.value = false;
  }
};

// 加载指定索引的视频（带超时且正确清理）
const loadVideo = async (index) => {
  const item = videoCache.value[index];
  if (!item || item.loaded) return;

  try {
    // 创建临时video元素预加载
    const tempVideo = document.createElement('video');
    tempVideo.src = item.url;
    tempVideo.preload = 'metadata';  // 仅加载元数据，减轻带宽压力
    tempVideo.muted = true;

    const loadWithTimeout = () => new Promise((resolve, reject) => {
      let settled = false;
      const cleanup = () => {
        tempVideo.onloadeddata = null;
        tempVideo.onerror = null;
      };
      const timer = setTimeout(() => {
        if (settled) return;
        settled = true;
        cleanup();
        reject(new Error('视频加载超时'));
      }, config.VIDEO_LOAD_TIMEOUT);

      tempVideo.onloadeddata = () => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        cleanup();
        item.loaded = true;
        item.loadError = false;
        item.thumbnail = generateThumbnail(tempVideo);
        console.log(`✅ 视频索引[${index}]加载成功`);
        resolve();
      };
      tempVideo.onerror = (e) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        cleanup();
        console.error(`❌ 视频索引[${index}]加载失败:`, e);
        reject(new Error('视频加载失败'));
      };
      tempVideo.load();
    });

    await loadWithTimeout();

    // 滑动模式：若当前项正是刚加载的视频，自动尝试播放
    try {
      await nextTick();
      if (viewMode.value === 'swipe') {
        const current = videoCache.value[swipeCurrentIndex.value];
        if (current && current.url === item.url) {
          const idx = swipeCurrentIndex.value;
          console.log(`🎬 当前视频加载完成，自动播放 索引${idx}`);
          playSwipeVideo(idx, false);
        }
      }
    } catch (e) {}
  } catch (error) {
    console.error(`视频索引[${index}]加载失败:`, error.message);
    item.loaded = false;
    item.loadError = true;
  }
};


// 加载下一个视频
const loadNextVideo = async () => {
  retryAttempts = 0;
  if (videoCache.value.length === 0) {
    await loadCurrentVideo();
    return;
  }
  currentVideoIndex.value = Math.min(1, videoCache.value.length - 1);
  await loadVideoFromCache();
};

// 切换静音
const toggleMute = () => {
  isMuted.value = !isMuted.value;
  try { localStorage.setItem(MUTE_KEY, JSON.stringify(isMuted.value)); } catch (e) {}
  
  // 滑动模式：更新所有视频的静音状态
  if (viewMode.value === 'swipe') {
    videoRefs.value.forEach(video => {
      if (video) {
        video.muted = isMuted.value;
      }
    });
  } else if (videoPlayer.value) {
    // 单视频和列表模式
    videoPlayer.value.muted = isMuted.value;
  }
};

// 切换自动连播
const toggleAutoPlay = () => {
  autoPlayEnabled.value = !autoPlayEnabled.value;
  try { localStorage.setItem(AUTOPLAY_KEY, JSON.stringify(autoPlayEnabled.value)); } catch (e) {}
  if (videoPlayer.value) {
    videoPlayer.value.loop = !autoPlayEnabled.value;
  }
};

// 视频加载完成
const onVideoLoaded = () => {
  isLoading.value = false;
  retryAttempts = 0;
  safePlay();
};

// 视频开始播放
const onPlay = () => {
  isPlaying.value = true;
};

// 视频暂停
const onPause = () => {
  isPlaying.value = false;
};

// 视频播放结束
const onEnded = () => {
  if (autoPlayEnabled.value) {
    loadNextVideo();
  }
};


// 视频加载错误
const onError = () => {
  retryAttempts++;
  
  if (retryAttempts <= config.MAX_RETRIES) {
    setTimeout(() => {
      loadVideoFromCache();
    }, 1000);
  } else {
    retryAttempts = 0;
    loadVideoFromCache();
  }
};

// 组件挂载
onMounted(async () => {
  // 恢复偏好
  try {
    const savedMode = localStorage.getItem(VIEW_MODE_KEY);
    if (savedMode && ['single', 'list', 'swipe'].includes(savedMode)) {
      viewMode.value = savedMode;
    }
    const savedMute = localStorage.getItem(MUTE_KEY);
    if (savedMute !== null) {
      isMuted.value = JSON.parse(savedMute);
    }
    const savedAuto = localStorage.getItem(AUTOPLAY_KEY);
    if (savedAuto !== null) {
      autoPlayEnabled.value = JSON.parse(savedAuto);
    }
  } catch (e) {}

  if (viewMode.value === 'swipe') {
    await nextTick();
    initSwipeMode();
    return;
  }

  // 单视频/列表模式
  if (videoPlayer.value) {
    videoPlayer.value.controls = true;
    videoPlayer.value.muted = isMuted.value;
    videoPlayer.value.loop = !autoPlayEnabled.value;
    await loadVideoFromCache();
  }
});

// 组件卸载
onBeforeUnmount(() => {
  clearTimeout(scrollTimeout);
  
  // 暂停所有视频
  pauseAllSwipeVideos();
  if (videoPlayer.value && !videoPlayer.value.paused) {
    videoPlayer.value.pause();
  }
  
  // 清理缓存
  videoCache.value = [];
  videoRefs.value = [];
  console.log('🧹 组件卸载，缓存已清理');
});
</script>

<style scoped>
.video-view-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a, #1e293b, #0f172a);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  position: relative;
  overflow: hidden;
}

/* 背景动态效果 */
.video-view-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.03) 0%, transparent 70%);
  animation: pulse 15s ease-in-out infinite;
  pointer-events: none;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.1); opacity: 0.5; }
}

/* 返回按钮 */
.back-button {
  position: fixed;
  top: 1rem;
  left: 1rem;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px 20px;
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 200;
}

.back-button:hover {
  transform: translateY(-2px);
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.2);
}

.back-icon {
  font-size: 18px;
  font-weight: bold;
}

/* 模式切换按钮 */
.mode-switcher {
  position: fixed;
  top: 1rem;
  right: 1rem;
  display: flex;
  gap: 8px;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 6px;
  z-index: 200;
}

.mode-btn {
  padding: 8px 16px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mode-btn:hover {
  color: #e2e8f0;
  background: rgba(148, 163, 184, 0.1);
}

.mode-btn.active {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(59, 130, 246, 0.2));
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.4);
}

/* 播放器容器 */
.player-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 24px;
  max-width: 1200px;
  z-index: 10;
}

.player-container.list-mode {
  max-width: 1600px;
  gap: 32px;
}

/* 视频容器 */
.video-container {
  position: relative;
  height: calc(100vh - 3rem);
  width: auto;
  aspect-ratio: 9 / 16;
  max-width: 90vw;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4),
              0 0 40px rgba(16, 185, 129, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.player-container.list-mode .video-container {
  max-width: 60vw;
}

.video-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(16, 185, 129, 0.6), transparent);
  z-index: 1;
}

.video-container:hover {
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 24px 72px rgba(0, 0, 0, 0.5),
              0 0 60px rgba(16, 185, 129, 0.2);
}

/* 视频包装器 */
.video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
}

video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.hide-controls::-webkit-media-controls {
  display: none !important;
}

/* 加载遮罩 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.spinner {
  width: 60px;
  height: 60px;
  border: 4px solid rgba(16, 185, 129, 0.2);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 16px;
  color: #e2e8f0;
  font-size: 16px;
  font-weight: 500;
}

/* 控制按钮 */
.controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.control-btn {
  min-width: 130px;
  padding: 12px 20px;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.control-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.3), transparent);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
  border-radius: 50%;
}

.control-btn:hover::before {
  width: 300px;
  height: 300px;
}

.control-btn:hover {
  transform: translateY(-2px);
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.2);
}

.control-btn:active {
  transform: translateY(0);
}

/* 激活状态 */
.mute-active,
.auto-play-active {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(59, 130, 246, 0.2));
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.3);
}

.mute-active:hover,
.auto-play-active:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.3), rgba(59, 130, 246, 0.3));
  border-color: rgba(16, 185, 129, 0.5);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .video-container {
    height: calc(100vh - 2.5rem);
    max-width: 88vw;
  }

  .player-container {
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .video-view-container {
    padding: 1rem;
  }

  .player-container {
    flex-direction: column;
    gap: 20px;
  }

  .video-container {
    height: calc(75vh - 2rem);
    max-width: 95vw;
  }

  .controls {
    flex-direction: row;
    gap: 10px;
    width: 100%;
    justify-content: center;
  }

  .control-btn {
    flex: 1;
    min-width: 0;
    max-width: 130px;
    padding: 10px 16px;
    font-size: 13px;
  }

  .back-button {
    top: 0.75rem;
    left: 0.75rem;
    padding: 10px 16px;
    font-size: 13px;
  }
}

@media (max-width: 450px) {
  .video-view-container {
    padding: 0.75rem;
  }

  .video-container {
    height: calc(70vh - 2rem);
    max-width: 96vw;
  }

  .control-btn {
    padding: 9px 12px;
    font-size: 12px;
    max-width: 110px;
  }

  .back-button {    
    top: 0.5rem;
    left: 0.5rem;
  }

  .mode-switcher {
    top: 0.5rem;
    right: 0.5rem;
    padding: 4px;
  }

  .mode-btn {
    padding: 6px 12px;
    font-size: 12px;
  }

  .player-container.list-mode {
    flex-direction: column;
  }

  .player-container.list-mode .video-container {
    max-width: 95vw;
  }

  .preview-list {
    width: 100%;
    max-width: 95vw;
    height: 200px;
    flex-direction: row;
    padding: 12px;
  }

  .preview-header {
    writing-mode: vertical-rl;
    transform: rotate(180deg);
    padding-bottom: 0;
    padding-right: 12px;
    border-bottom: none;
    border-right: 1px solid rgba(148, 163, 184, 0.2);
  }

  .preview-scroll {
    flex-direction: row;
    padding-right: 0;
    padding-bottom: 6px;
  }

  .preview-scroll::-webkit-scrollbar {
    width: auto;
    height: 6px;
  }

  .preview-item {
    min-width: 80px;
    width: 80px;
  }

  .more-videos-hint {
    min-width: 150px;
    padding: 12px;
    font-size: 11px;
  }

  .hint-icon {
    font-size: 20px;
  }

  .cache-total {
    font-size: 12px;
  }
}

/* 预览列表样式 */
.preview-list {
  width: 280px;
  height: calc(100vh - 3rem);
  background: rgba(30, 41, 59, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
}

.cache-count {
  color: #10b981;
  font-size: 13px;
}

.preview-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 6px;
}

/* 自定义滚动条 */
.preview-scroll::-webkit-scrollbar {
  width: 6px;
}

.preview-scroll::-webkit-scrollbar-track {
  background: rgba(148, 163, 184, 0.1);
  border-radius: 3px;
}

.preview-scroll::-webkit-scrollbar-thumb {
  background: rgba(16, 185, 129, 0.3);
  border-radius: 3px;
}

.preview-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(16, 185, 129, 0.5);
}

/* 预览项 */
.preview-item {
  position: relative;
  aspect-ratio: 9 / 16;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  background: rgba(15, 23, 42, 0.6);
}

.preview-item:hover {
  transform: translateX(-4px);
  border-color: rgba(16, 185, 129, 0.5);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.2);
}

.preview-item.active {
  border-color: #10b981;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.4);
}

.preview-item.loading {
  opacity: 0.6;
  pointer-events: none;
}

/* 预览缩略图 */
.preview-thumbnail {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.8);
}

.preview-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.9), rgba(30, 41, 59, 0.9));
}

.play-icon {
  font-size: 48px;
  color: rgba(16, 185, 129, 0.6);
}

/* 播放中指示器 */
.playing-indicator {
  position: absolute;
  bottom: 8px;
  left: 8px;
  right: 8px;
  background: rgba(16, 185, 129, 0.9);
  backdrop-filter: blur(10px);
  padding: 6px 10px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #fff;
  border-radius: 50%;
  animation: pulse-animation 1.5s ease-in-out infinite;
}

@keyframes pulse-animation {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

/* 加载中指示器 */
.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.mini-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(16, 185, 129, 0.2);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 序号 */
.preview-index {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #e2e8f0;
}

/* 更多视频提示 */
.more-videos-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px dashed rgba(16, 185, 129, 0.3);
  border-radius: 12px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.hint-icon {
  font-size: 24px;
}

.cache-total {
  color: #10b981;
  font-weight: 600;
  font-size: 13px;
}

/* ========== 滑动模式样式 ========== */
.swipe-container {
  width: 100vw;
  height: 100vh;
  overflow-y: scroll;
  overflow-x: hidden;
  scroll-snap-type: y mandatory;
  -webkit-overflow-scrolling: touch;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 5;
  background: #000;
  /* 隐藏滚动条 */
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.swipe-container.no-snap {
  scroll-snap-type: none;
}

.swipe-container::-webkit-scrollbar {
  display: none;
}

/* 滑动项 */
.swipe-item {
  width: 100vw;
  height: 100vh;
  scroll-snap-align: start;
  scroll-snap-stop: always;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

/* 视频包装器 */
.swipe-video-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

/* 视频元素 */
.swipe-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

/* 加载状态 */
.swipe-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.85);
  z-index: 10;
}

/* 播放提示 */
.swipe-play-hint {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  z-index: 10;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.play-icon-large {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px);
  border: 2px solid rgba(16, 185, 129, 0.5);
  border-radius: 50%;
  color: #10b981;
  font-size: 36px;
  padding-left: 6px;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);
  animation: pulse-play 2s infinite;
}

.play-hint-text {
  margin-top: 16px;
  color: #e2e8f0;
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
}

@keyframes pulse-play {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 8px 32px rgba(16, 185, 129, 0.5);
  }
}

/* 视频信息和控制 */
.swipe-info {
  position: absolute;
  bottom: 100px;
  left: 20px;
  right: 20px;
  display: flex;
  justify-content: center;
  z-index: 20;
  pointer-events: none;
}

.swipe-info * {
  pointer-events: auto;
}

.swipe-info-content {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.swipe-index {
  color: #10b981;
  font-size: 14px;
  font-weight: 600;
}

.swipe-mute-btn {
  width: 40px;
  height: 40px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 10px;
  color: #e2e8f0;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.swipe-mute-btn:hover {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(16, 185, 129, 0.5);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.3);
  transform: scale(1.05);
}

.swipe-mute-btn.active {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(59, 130, 246, 0.2));
  border-color: rgba(16, 185, 129, 0.5);
}

/* 滑动模式响应式 */
@media (max-width: 768px) {
  .swipe-info {
    bottom: 80px;
    left: 12px;
    right: 12px;
  }

  .swipe-info-content {
    padding: 10px 16px;
    gap: 12px;
  }

  .swipe-index {
    font-size: 13px;
  }

  .swipe-mute-btn {
    width: 36px;
    height: 36px;
    font-size: 18px;
  }
}

@media (max-width: 450px) {
  .swipe-info {
    bottom: 60px;
    left: 8px;
    right: 8px;
  }

  .swipe-info-content {
    padding: 8px 12px;
    gap: 10px;
  }

  .swipe-index {
    font-size: 12px;
  }

  .swipe-mute-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .play-icon-large {
    width: 70px;
    height: 70px;
    font-size: 32px;
  }

  .play-hint-text {
    font-size: 14px;
  }
}
</style>
