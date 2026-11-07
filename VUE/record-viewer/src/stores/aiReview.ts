import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AiReviewResponse } from '@/API/ApiPopulation'

export const useAiReviewStore = defineStore('aiReview', () => {
  const aiSummary = ref('')
  const emergencyFlags = ref<AiReviewResponse['emergency_flags'] | null>(null)
  const error = ref('')
  const lastPersonId = ref<string | null>(null)
  const configuredMaxTokens = ref<number | null>(null)
  const configuredMaxContext = ref<number | null>(null)

  const hasSummary = computed(() => !!aiSummary.value && aiSummary.value !== '')

  function setResponse(personId: string, response: AiReviewResponse) {
    lastPersonId.value = personId
    aiSummary.value = response.summary || ''
    emergencyFlags.value = response.emergency_flags || null
    error.value = ''
    if (response.max_tokens != null) {
      configuredMaxTokens.value = response.max_tokens
    }
    if (response.max_context != null) {
      configuredMaxContext.value = response.max_context
    }
  }

  function setChatTokens(totalTokens: number, maxTokens: number) {
    if (maxTokens != null) {
      configuredMaxTokens.value = maxTokens
    }
    return { totalTokens, maxTokens }
  }

  function clear() {
    aiSummary.value = ''
    emergencyFlags.value = null
    error.value = ''
    lastPersonId.value = null
    configuredMaxTokens.value = null
    configuredMaxContext.value = null
  }

  function clearForPatient(personId: string) {
    if (lastPersonId.value === personId) {
      clear()
    }
  }

  return { aiSummary, emergencyFlags, error, hasSummary, configuredMaxTokens, configuredMaxContext, setResponse, setChatTokens, clear, clearForPatient }
})
