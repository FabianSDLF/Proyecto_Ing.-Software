import apiClient from './apiClient';

export const portfolioService = {
  /**
   * Obtener evidencias de aprendizaje del estudiante autenticado
   */
  async getDocuments(options) {
    return apiClient.get('/api/portafolio/evidencias', options);
  },

  /**
   * Subir un archivo de evidencia de aprendizaje
   * @param {File} file
   * @param {number|string} [assignmentId]
   */
  async uploadEvidence(file, assignmentId, options) {
    const formData = new FormData();
    formData.append('file', file);
    if (assignmentId) {
      formData.append('assignmentId', assignmentId);
    }
    return apiClient.upload('/api/portafolio/evidencias/upload', formData, options);
  },

  /**
   * Eliminar una evidencia por ID
   * @param {number|string} id
   */
  async deleteDocument(id, options) {
    return apiClient.delete(`/api/portafolio/documentos/${id}`, options);
  },

  /**
   * Descargar un documento por su URL o endpoint
   * @param {string} fileUrl
   * @param {string} fileName
   */
  async downloadDocument(fileUrl, fileName, options) {
    return apiClient.download(fileUrl, fileName, options);
  },

  /**
   * Obtener planificaciones didácticas del estudiante autenticado
   */
  async getLessonPlans(options) {
    return apiClient.get('/api/portafolio/planificaciones', options);
  },

  /**
   * Crear y subir una planificación didáctica
   * @param {Object} planData
   * @param {File} [planData.file]
   * @param {string} planData.schoolGrade
   * @param {string} planData.mathAxis
   * @param {string} planData.objective
   * @param {string} [planData.specificObjectives]
   * @param {string|Object} [planData.classActivities]
   * @param {number|string} [planData.assignmentId]
   */
  async createLessonPlan(planData, options) {
    const formData = new FormData();
    if (planData.file) {
      formData.append('file', planData.file);
    }
    formData.append('schoolGrade', planData.schoolGrade);
    formData.append('mathAxis', planData.mathAxis);
    formData.append('objective', planData.objective);
    
    if (planData.specificObjectives) {
      formData.append('specificObjectives', planData.specificObjectives);
    }
    if (planData.classActivities) {
      const activitiesStr = typeof planData.classActivities === 'object' 
        ? JSON.stringify(planData.classActivities) 
        : planData.classActivities;
      formData.append('classActivities', activitiesStr);
    }
    if (planData.assignmentId) {
      formData.append('assignmentId', planData.assignmentId);
    }

    return apiClient.upload('/api/portafolio/planificaciones/upload', formData, options);
  },

  /**
   * Eliminar una planificación didáctica por ID
   * @param {number|string} id
   */
  async deleteLessonPlan(id, options) {
    return apiClient.delete(`/api/portafolio/planificaciones/${id}`, options);
  },
};

export default portfolioService;
