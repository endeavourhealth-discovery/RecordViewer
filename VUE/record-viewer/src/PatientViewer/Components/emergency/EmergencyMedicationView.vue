<template>
  <div class="emergency-medication-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-pills"></i> CURRENT MEDICATIONS - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-pills" title="Current Medications" subtitle="Active medications only for emergency decision-making" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <DataTable :value="medications" :paginator="true" :rows="10" :sortField="'date'" :sortOrder="-1" size="small" scrollable scrollHeight="flex">
          <Column field="name" header="Medication" style="width: 35%;"></Column>
          <Column field="dose" header="Dose" style="width: 20%;"></Column>
          <Column field="quantity" header="Quantity" style="width: 15%;"></Column>
          <Column field="type" header="Type" style="width: 10%;"></Column>
          <Column field="date" header="Last Prescribed" style="width: 10%;"></Column>
          <Column field="orgName" header="Source" style="width: 10%;"></Column>
        </DataTable>

        <div v-if="medications.length === 0" class="no-data">No active medications found.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view current medications.</p>
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
const medications = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateMedications();
  } else {
    medications.value = [];
  }
});

async function populateMedications() {
  searchingForData.value = true;
  try {
    const meds = await ApiPopulation.populateMedication(props.selectedPatient.personId, "1");
    medications.value = meds || [];
  } catch (error) {
    console.error('Error loading medications:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-medication-view { width: 100%; }

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
