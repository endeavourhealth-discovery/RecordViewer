<template>
  <div class="emergency-encounters-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-user-doctor"></i> RECENT ENCOUNTERS - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-user-doctor" title="Recent Encounters" subtitle="Most recent clinical encounters for context" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <DataTable :value="recentEncounters" :paginator="true" :rows="10" :sortField="'date'" :sortOrder="-1" size="small" scrollable scrollHeight="flex">
          <Column field="type" header="Type" style="width: 25%;"></Column>
          <Column field="location" header="Location" style="width: 25%;"></Column>
          <Column field="practitioner" header="Practitioner" style="width: 20%;"></Column>
          <Column field="date" header="Date" style="width: 15%;"></Column>
          <Column field="orgName" header="Organization" style="width: 15%;"></Column>
        </DataTable>

        <div v-if="recentEncounters.length === 0" class="no-data">No recent encounters recorded.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view recent encounters.</p>
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
const recentEncounters = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateEncounters();
  } else {
    recentEncounters.value = [];
  }
});

async function populateEncounters() {
  searchingForData.value = true;
  try {
    const encounters = await ApiPopulation.populateEncounters(props.selectedPatient.personId, "1");
    recentEncounters.value = encounters || [];
  } catch (error) {
    console.error('Error loading encounters:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-encounters-view { width: 100%; }

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
