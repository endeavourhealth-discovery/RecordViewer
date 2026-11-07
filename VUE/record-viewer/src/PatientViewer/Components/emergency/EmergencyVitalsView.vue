<template>
  <div class="emergency-vitals-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-heart-pulse"></i> VITALS & OBSERVATIONS - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-heart-pulse" title="Vitals & Observations" subtitle="Recent clinical observations and vital signs" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <DataTable :value="recentVitals" :paginator="true" :rows="10" :sortField="'date'" :sortOrder="-1" size="small" scrollable scrollHeight="flex">
          <Column field="name" header="Observation" style="width: 45%;"></Column>
          <Column field="category" header="Category" style="width: 15%;"></Column>
          <Column field="date" header="Date" style="width: 15%;"></Column>
          <Column field="orgName" header="Source" style="width: 15%;"></Column>
          <Column field="code" header="Code" style="width: 10%;">
            <template #body="slotProps">
              <span style="font-size: 0.8em; color: #666;">{{ slotProps.data.code }}</span>
            </template>
          </Column>
        </DataTable>

        <div v-if="recentVitals.length === 0" class="no-data">No recent observations recorded.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view vitals and observations.</p>
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
const recentVitals = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateVitals();
  } else {
    recentVitals.value = [];
  }
});

async function populateVitals() {
  searchingForData.value = true;
  try {
    const vitals = await ApiPopulation.populateSummaryObservations(props.selectedPatient.personId);
    recentVitals.value = vitals || [];
  } catch (error) {
    console.error('Error loading vitals:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-vitals-view { width: 100%; }

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
