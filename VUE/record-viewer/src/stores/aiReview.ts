import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AiReviewResponse } from '@/API/ApiPopulation'

export const useAiReviewStore = defineStore('aiReview', () => {
  const aiSummary = ref('')
  const emergencyFlags = ref<AiReviewResponse['emergency_flags'] | null>(null)
  const error = ref('')
  const lastPersonId = ref<string | null>(null)

  const hasSummary = computed(() => !!aiSummary.value && aiSummary.value !== '')

  function setResponse(personId: string, response: AiReviewResponse) {
    lastPersonId.value = personId
    aiSummary.value = response.summary || ''
    emergencyFlags.value = response.emergency_flags || null
    error.value = ''
  }

  function clear() {
    aiSummary.value = ''
    emergencyFlags.value = null
    error.value = ''
    lastPersonId.value = null
  }

  function clearForPatient(personId: string) {
    if (lastPersonId.value === personId) {
      clear()
    }
  }

  return { aiSummary, emergencyFlags, error, hasSummary, setResponse, clear, clearForPatient }
})
