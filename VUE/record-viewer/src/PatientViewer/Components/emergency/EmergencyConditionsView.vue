<template>
  <div class="emergency-conditions-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-diagnoses"></i> ACTIVE CONDITIONS - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-diagnoses" title="Active Conditions" subtitle="Current active conditions affecting clinical decision-making" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <DataTable :value="activeConditions" :paginator="true" :rows="10" size="small" scrollable scrollHeight="flex">
          <Column field="name" header="Condition" style="width: 55%;"></Column>
          <Column field="code" header="SNOMED Code" style="width: 20%;">
            <template #body="slotProps">
              <span style="font-size: 0.8em; color: #666;">{{ slotProps.data.code }}</span>
            </template>
          </Column>
          <Column field="date" header="Onset Date" style="width: 15%;"></Column>
          <Column field="orgName" header="Source" style="width: 10%;"></Column>
        </DataTable>

        <div v-if="activeConditions.length === 0" class="no-data">No active conditions recorded.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view active conditions.</p>
      </template>
    </Card>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue';
import { ref, watch } from 'vue';
import ApiPopulation from '@/API/ApiPopulation';

const props = defineProps({
  selectedPatient: Object,
  viewMode: String,
});

const searchingForData = ref(false);
const activeConditions = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateConditions();
  } else {
    activeConditions.value = [];
  }
});

async function populateConditions() {
  searchingForData.value = true;
  try {
    const conditions = await ApiPopulation.populateActiveConditionsProblems(props.selectedPatient.personId);
    activeConditions.value = conditions || [];
  } catch (error) {
    console.error('Error loading conditions:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-conditions-view { width: 100%; }

.emergency-mode .emergency-banner {
  background: linear-gradient(90deg, #c0392b, #e74c3c);
  color: white;
  padding: 8px 16px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 15px;
  border-radius: 4px;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
  font-style: italic;
}
</style>
