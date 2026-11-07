<template>
  <div class="emergency-allergy-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-triangle-exclamation"></i> ALLERGIES & INTOLERANCES - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-triangle-exclamation" title="Allergies & Intolerances" subtitle="Complete allergy history for emergency safety checks" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <div class="allergy-summary" v-if="activeAllergies.length > 0">
          <Tag severity="danger" :value="`Active Allergies: ${activeAllergies.length}`" size="large"/>
        </div>

        <DataTable :value="activeAllergies" :paginator="true" :rows="10" size="small" scrollable scrollHeight="flex">
          <Column field="name" header="Allergy / Intolerance" style="width: 50%;">
            <template #body="slotProps">
              <span class="allergy-name">{{ slotProps.data.name }}</span>
            </template>
          </Column>
          <Column field="date" header="First Recorded" style="width: 20%;"></Column>
          <Column field="orgName" header="Source" style="width: 15%;"></Column>
          <Column field="code" header="Code" style="width: 15%;">
            <template #body="slotProps">
              <span style="font-size: 0.8em; color: #666;">{{ slotProps.data.code }}</span>
            </template>
          </Column>
        </DataTable>

        <div v-if="activeAllergies.length === 0" class="no-data">No allergies recorded for this patient.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view allergy information.</p>
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
const activeAllergies = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateAllergies();
  } else {
    activeAllergies.value = [];
  }
});

async function populateAllergies() {
  searchingForData.value = true;
  try {
    const allergies = await ApiPopulation.populateAllergies(props.selectedPatient.personId);
    activeAllergies.value = allergies || [];
  } catch (error) {
    console.error('Error loading allergies:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-allergy-view { width: 100%; }

.emergency-mode .emergency-banner {
  background: linear-gradient(90deg, #c0392b, #e74c3c);
  color: white;
  padding: 8px 16px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 15px;
  border-radius: 4px;
}

.allergy-summary {
  margin-bottom: 15px;
  display: flex;
  justify-content: flex-end;
}

.allergy-name {
  font-weight: 600;
  color: #c0392b;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
  font-style: italic;
}
</style>
