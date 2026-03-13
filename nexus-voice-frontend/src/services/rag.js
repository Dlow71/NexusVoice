import apiClient from './api';

export default {
  listKnowledgeBases() {
    return apiClient.get('/v1/rag/knowledge-bases');
  },

  createKnowledgeBase(payload) {
    return apiClient.post('/v1/rag/knowledge-bases', payload);
  },

  getKnowledgeBase(id) {
    return apiClient.get(`/v1/rag/knowledge-bases/${id}`);
  },

  uploadFile(knowledgeBaseId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(`/v1/rag/knowledge-bases/${knowledgeBaseId}/files`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  search(knowledgeBaseId, query, limit = 5) {
    return apiClient.get(`/v1/rag/knowledge-bases/${knowledgeBaseId}/search`, {
      params: { query, limit },
    });
  },

  getCitationContext(knowledgeBaseId, fileId, location, window = 1) {
    return apiClient.get(`/v1/rag/knowledge-bases/${knowledgeBaseId}/files/${fileId}/context`, {
      params: { location, window },
    });
  },

  deleteFile(knowledgeBaseId, fileId) {
    return apiClient.delete(`/v1/rag/knowledge-bases/${knowledgeBaseId}/files/${fileId}`);
  },

  deleteKnowledgeBase(knowledgeBaseId) {
    return apiClient.delete(`/v1/rag/knowledge-bases/${knowledgeBaseId}`);
  },
};
