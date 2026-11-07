import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface EmergencyClinicalData {
  medication: any[]
  allergies: any[]
  activeConditions: any[]
  warnings: any[]
  patientHeight: number | null
  patientWeight: number | null
  patientBMI: number | null
  criticalAlerts: any[]
}

export const useEmergencyDataStore = defineStore('emergencyData', () => {
  const medication = ref<any[]>([])
  const allergies = ref<any[]>([])
  const activeConditions = ref<any[]>([])
  const warnings = ref<any[]>([])
  const patientHeight = ref<number | null>(null)
  const patientWeight = ref<number | null>(null)
  const patientBMI = ref<number | null>(null)
  const criticalAlerts = ref<any[]>([])
  const lastPersonId = ref<string | null>(null)

  function setData(personId: string, data: EmergencyClinicalData) {
    lastPersonId.value = personId
    medication.value = data.medication || []
    allergies.value = data.allergies || []
    activeConditions.value = data.activeConditions || []
    warnings.value = data.warnings || []
    patientHeight.value = data.patientHeight ?? null
    patientWeight.value = data.patientWeight ?? null
    patientBMI.value = data.patientBMI ?? null
    criticalAlerts.value = data.criticalAlerts || []
  }

  function clearForPatient(personId: string) {
    if (lastPersonId.value === personId) {
      resetAll()
    }
  }

  function clear() {
    resetAll()
  }

  function resetAll() {
    medication.value = []
    allergies.value = []
    activeConditions.value = []
    warnings.value = []
    patientHeight.value = null
    patientWeight.value = null
    patientBMI.value = null
    criticalAlerts.value = []
    lastPersonId.value = null
  }

  function hasData() {
    return !!lastPersonId.value && (medication.value.length > 0 || activeConditions.value.length > 0)
  }

  return {
    medication, allergies, activeConditions, warnings,
    patientHeight, patientWeight, patientBMI, criticalAlerts,
    lastPersonId, hasData, setData, clearForPatient, clear, resetAll
  }
})
