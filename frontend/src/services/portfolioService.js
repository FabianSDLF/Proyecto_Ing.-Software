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

  /**
   * Obtener historial completo de versiones de una planificación
   * @param {number|string} id
   */
  async getLessonPlanHistory(id, options) {
    return apiClient.get(`/api/portafolio/planificaciones/${id}/historial`, options);
  },

  async getPlanificacionHistorial(id, options) {
    return this.getLessonPlanHistory(id, options);
  },

  /**
   * Obtener detalle de planificación por ID (modo solo lectura)
   * @param {number|string} id
   */
  async getLessonPlanById(id, options) {
    return apiClient.get(`/api/portafolio/planificaciones/${id}`, options);
  },

  async getLessonPlan(id, options) {
    return this.getLessonPlanById(id, options);
  },

  async getPlanificacionDetalle(id, options) {
    return this.getLessonPlanById(id, options);
  },

  /**
   * Crear nueva versión o editar planificación existente
   * @param {number|string} id
   * @param {FormData|object} planData
   */
  async createLessonPlanVersion(id, planData, options) {
    let formData;
    if (planData instanceof FormData) {
      formData = planData;
    } else {
      formData = new FormData();
      if (planData.file) formData.append('file', planData.file);
      if (planData.schoolGrade) formData.append('schoolGrade', planData.schoolGrade);
      if (planData.mathAxis) formData.append('mathAxis', planData.mathAxis);
      if (planData.objective || planData.baseLearningObjective) {
        formData.append('objective', planData.objective || planData.baseLearningObjective);
      }
      if (planData.specificObjectives || planData.objectives) {
        formData.append('specificObjectives', planData.specificObjectives || planData.objectives);
      }
      if (planData.classActivities || planData.contentJson) {
        const activities = planData.classActivities || planData.contentJson;
        const activitiesStr = typeof activities === 'object' ? JSON.stringify(activities) : activities;
        formData.append('classActivities', activitiesStr);
      }
      if (planData.assignmentId) formData.append('assignmentId', planData.assignmentId);
    }
    return apiClient.upload(`/api/portafolio/planificaciones/${id}/editar`, formData, options);
  },

  async editarPlanificacion(id, planData, options) {
    return this.createLessonPlanVersion(id, planData, options);
  },

  async updateLessonPlan(id, planData, options) {
    return this.createLessonPlanVersion(id, planData, options);
  },

  /**
   * Obtener documentos de término del estudiante autenticado
   * @param {Object} [options] - Opciones de fetch (signal, headers, etc.)
   */
  async getFinalDocuments(options) {
    return apiClient.get('/api/portafolio/documentos-finales', options);
  },

  /**
   * Alias en español para obtener documentos de término
   */
  async getDocumentosFinales(options) {
    return this.getFinalDocuments(options);
  },

  /**
   * Subir un documento final de término de práctica
   * @param {FormData|File|Object} payload - FormData, File o payload con { file, type, replaceConfirmed, replace, assignmentId }
   * @param {string} [type] - Tipo de documento si payload es File
   * @param {boolean} [replace=false] - Booleano de reemplazo
   * @param {Object} [options] - Opciones de fetch
   */
  async uploadFinalDocument(payload, type, replace = false, options) {
    let formData;
    let fetchOptions = options;

    if (payload instanceof FormData) {
      formData = payload;
      fetchOptions = type;
    } else if (payload instanceof File) {
      formData = new FormData();
      formData.append('file', payload);
      formData.append('type', type || 'INFORME_FINAL');
      formData.append('replace', replace ? 'true' : 'false');
      formData.append('replaceConfirmed', replace ? 'true' : 'false');
    } else if (payload && typeof payload === 'object') {
      formData = new FormData();
      if (payload.file) formData.append('file', payload.file);
      formData.append('type', payload.type || type || 'INFORME_FINAL');
      const isRep = payload.replaceConfirmed !== undefined
        ? payload.replaceConfirmed
        : (payload.replace !== undefined ? payload.replace : replace);
      formData.append('replace', isRep ? 'true' : 'false');
      formData.append('replaceConfirmed', isRep ? 'true' : 'false');
      if (payload.assignmentId) formData.append('assignmentId', payload.assignmentId);
      fetchOptions = type;
    } else {
      formData = new FormData();
    }
    return apiClient.upload('/api/portafolio/documentos-finales/upload', formData, fetchOptions);
  },

  /**
   * Alias en español para subir documento final
   */
  async uploadDocumentoFinal(payload, type, replace, options) {
    return this.uploadFinalDocument(payload, type, replace, options);
  },

  /**
   * Eliminar un documento de término por ID
   * @param {number|string} id - ID del documento
   * @param {Object} [options] - Opciones de fetch
   */
  async deleteFinalDocument(id, options) {
    return apiClient.delete(`/api/portafolio/documentos-finales/${id}`, options);
  },

  /**
   * Alias en español para eliminar documento final
   */
  async deleteDocumentoFinal(id, options) {
    return this.deleteFinalDocument(id, options);
  },
  // HU1: Consultas para Docente Supervisor
  async obtenerEvidenciasPorAsignacion(assignmentId, options) {
    return apiClient.get(`/api/portafolio/asignacion/${assignmentId}/evidencias`, options);
  },

  async obtenerPlanificacionesPorAsignacion(assignmentId, options) {
    return apiClient.get(`/api/portafolio/asignacion/${assignmentId}/planificaciones`, options);
  },

  async obtenerDocumentosFinalesPorAsignacion(assignmentId, options) {
    return apiClient.get(`/api/portafolio/documentos-finales/asignacion/${assignmentId}`, options);
  },
  async agregarRetroalimentacionDocumento(id, feedback, options) {
    return apiClient.put(`/api/portafolio/documentos/${id}/retroalimentacion`, { feedback }, options);
  },

  async agregarRetroalimentacionPlan(id, feedback, options) {
    return apiClient.put(`/api/portafolio/planificaciones/${id}/retroalimentacion`, { feedback }, options);
  },
};

export default portfolioService;
