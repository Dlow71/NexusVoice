/**
 * 安全工具函数库
 * 用于防止XSS攻击和验证用户输入
 * 
 * @author NexusVoice
 * @since 2025-10-20
 */

/**
 * JWT Token格式验证
 * @param {string} token - JWT token字符串
 * @returns {boolean} - 是否为有效的JWT格式
 */
export const validateJWTFormat = (token) => {
  if (!token || typeof token !== 'string') {
    return false;
  }
  // JWT格式：header.payload.signature (三个部分，Base64URL编码)
  const jwtPattern = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*$/;
  return jwtPattern.test(token);
};

/**
 * 清理和验证字符串参数，防止XSS
 * @param {string} value - 待验证的值
 * @param {number} maxLength - 最大长度
 * @returns {string|null} - 清理后的值或null
 */
export const sanitizeString = (value, maxLength = 500) => {
  if (!value || typeof value !== 'string') {
    return null;
  }
  
  // 移除潜在的XSS字符
  const cleaned = value
    .replace(/[<>"'`]/g, '') // 移除HTML特殊字符
    .replace(/javascript:/gi, '') // 移除javascript协议
    .replace(/on\w+=/gi, '') // 移除事件处理器
    .replace(/data:/gi, '') // 移除data协议
    .replace(/vbscript:/gi, '') // 移除vbscript协议
    .trim();
  
  // 长度限制
  if (cleaned.length > maxLength) {
    return cleaned.substring(0, maxLength);
  }
  
  return cleaned || null;
};

/**
 * 验证和清理URL
 * @param {string} url - 待验证的URL
 * @returns {string|null} - 清理后的URL或null
 */
export const sanitizeURL = (url) => {
  if (!url || typeof url !== 'string') {
    return null;
  }
  
  try {
    // 只允许http和https协议
    const urlObj = new URL(url);
    if (urlObj.protocol !== 'http:' && urlObj.protocol !== 'https:') {
      console.warn(`不安全的URL协议: ${urlObj.protocol}`);
      return null;
    }
    return urlObj.href;
  } catch (e) {
    // URL格式无效
    console.warn('URL格式无效:', url);
    return null;
  }
};

/**
 * 验证邮箱格式
 * @param {string} email - 邮箱地址
 * @returns {string|null} - 有效的邮箱或null
 */
export const validateEmail = (email) => {
  if (!email || typeof email !== 'string') {
    return null;
  }
  
  // RFC 5322 简化版邮箱正则
  const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (emailPattern.test(email) && email.length <= 100) {
    return email.toLowerCase(); // 统一转为小写
  }
  return null;
};

/**
 * 验证ISO 8601日期时间格式
 * @param {string} dateTime - 日期时间字符串
 * @returns {string|null} - 有效的日期时间或null
 */
export const validateDateTime = (dateTime) => {
  if (!dateTime || typeof dateTime !== 'string') {
    return null;
  }
  
  // ISO 8601格式验证
  const isoPattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z?$/;
  if (isoPattern.test(dateTime)) {
    const date = new Date(dateTime);
    if (!isNaN(date.getTime())) {
      return dateTime;
    }
  }
  return null;
};

/**
 * 验证用户ID（必须为正整数）
 * @param {string|number} userId - 用户ID
 * @returns {number|null} - 有效的用户ID或null
 */
export const validateUserId = (userId) => {
  if (!userId) {
    return null;
  }
  
  const id = typeof userId === 'number' ? userId : parseInt(userId, 10);
  if (isNaN(id) || id <= 0 || id > Number.MAX_SAFE_INTEGER) {
    return null;
  }
  return id;
};

/**
 * 验证手机号（中国大陆手机号）
 * @param {string} phone - 手机号
 * @returns {string|null} - 有效的手机号或null
 */
export const validatePhone = (phone) => {
  if (!phone || typeof phone !== 'string') {
    return null;
  }
  
  // 中国大陆手机号正则：1开头，第二位是3-9，后面9位数字
  const phonePattern = /^1[3-9]\d{9}$/;
  if (phonePattern.test(phone)) {
    return phone;
  }
  return null;
};

/**
 * HTML实体编码（用于在DOM中安全显示用户输入）
 * @param {string} str - 待编码的字符串
 * @returns {string} - 编码后的字符串
 */
export const htmlEncode = (str) => {
  if (!str || typeof str !== 'string') {
    return '';
  }
  
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
};

/**
 * 验证文件类型（用于文件上传）
 * @param {string} fileName - 文件名
 * @param {Array<string>} allowedTypes - 允许的文件扩展名数组
 * @returns {boolean} - 是否为允许的文件类型
 */
export const validateFileType = (fileName, allowedTypes = ['jpg', 'jpeg', 'png', 'gif']) => {
  if (!fileName || typeof fileName !== 'string') {
    return false;
  }
  
  const extension = fileName.split('.').pop().toLowerCase();
  return allowedTypes.includes(extension);
};

/**
 * 验证文件大小
 * @param {number} fileSize - 文件大小（字节）
 * @param {number} maxSize - 最大允许大小（字节），默认5MB
 * @returns {boolean} - 是否在允许的大小范围内
 */
export const validateFileSize = (fileSize, maxSize = 5 * 1024 * 1024) => {
  if (typeof fileSize !== 'number' || fileSize <= 0) {
    return false;
  }
  return fileSize <= maxSize;
};

/**
 * 清理对象中的所有字符串属性（递归）
 * @param {Object} obj - 待清理的对象
 * @param {number} maxLength - 字符串最大长度
 * @returns {Object} - 清理后的对象
 */
export const sanitizeObject = (obj, maxLength = 500) => {
  if (!obj || typeof obj !== 'object') {
    return obj;
  }
  
  const cleaned = {};
  for (const [key, value] of Object.entries(obj)) {
    if (typeof value === 'string') {
      cleaned[key] = sanitizeString(value, maxLength);
    } else if (typeof value === 'object' && value !== null) {
      cleaned[key] = sanitizeObject(value, maxLength);
    } else {
      cleaned[key] = value;
    }
  }
  return cleaned;
};

/**
 * 生成Content Security Policy (CSP) nonce
 * @returns {string} - 随机生成的nonce值
 */
export const generateCSPNonce = () => {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array));
};

/**
 * 检测可能的SQL注入模式（前端基础防护）
 * @param {string} input - 用户输入
 * @returns {boolean} - 是否检测到可疑模式
 */
export const detectSQLInjection = (input) => {
  if (!input || typeof input !== 'string') {
    return false;
  }
  
  // 常见SQL注入模式
  const sqlPatterns = [
    /(\bOR\b|\bAND\b).*=.*=/i,
    /UNION.*SELECT/i,
    /DROP.*TABLE/i,
    /INSERT.*INTO/i,
    /DELETE.*FROM/i,
    /UPDATE.*SET/i,
    /--/,
    /;.*--/,
    /\/\*.*\*\//
  ];
  
  return sqlPatterns.some(pattern => pattern.test(input));
};

/**
 * 验证密码强度
 * @param {string} password - 密码
 * @returns {Object} - 包含强度等级和提示的对象
 */
export const validatePasswordStrength = (password) => {
  if (!password || typeof password !== 'string') {
    return { level: 'weak', message: '密码不能为空' };
  }
  
  const checks = {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
  };
  
  const passedChecks = Object.values(checks).filter(Boolean).length;
  
  if (passedChecks === 5) {
    return { level: 'strong', message: '密码强度：强' };
  } else if (passedChecks >= 3) {
    return { level: 'medium', message: '密码强度：中' };
  } else {
    return { level: 'weak', message: '密码强度：弱，建议包含大小写字母、数字和特殊字符' };
  }
};

export default {
  validateJWTFormat,
  sanitizeString,
  sanitizeURL,
  validateEmail,
  validateDateTime,
  validateUserId,
  validatePhone,
  htmlEncode,
  validateFileType,
  validateFileSize,
  sanitizeObject,
  generateCSPNonce,
  detectSQLInjection,
  validatePasswordStrength
};
