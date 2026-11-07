<template>
  <div class="emergency-warnings-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-exclamation-triangle"></i> WARNINGS & FLAGS - EMERGENCY VIEW
    </div>

    <PageHeader icon="fa-solid fa-exclamation-triangle" title="Warnings & Flags" subtitle="Active clinical warnings and safety flags" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <div class="warning-summary" v-if="activeWarnings.length > 0">
          <Tag severity="danger" :value="`Active Warnings: ${activeWarnings.length}`" size="large"/>
        </div>

        <DataTable :value="activeWarnings" :paginator="true" :rows="10" size="small" scrollable scrollHeight="flex">
          <Column field="name" header="Warning / Flag" style="width: 50%;">
            <template #body="slotProps">
              <span class="warning-name">{{ slotProps.data.name }}</span>
            </template>
          </Column>
          <Column field="code" header="Code" style="width: 20%;">
            <template #body="slotProps">
              <span style="font-size: 0.8em; color: #666;">{{ slotProps.data.code }}</span>
            </template>
          </Column>
          <Column field="date" header="Date" style="width: 15%;"></Column>
          <Column field="orgName" header="Source" style="width: 15%;"></Column>
        </DataTable>

        <div v-if="activeWarnings.length === 0" class="no-data">No active warnings or flags recorded.</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view warnings and flags.</p>
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
const activeWarnings = ref([]);

watch(() => props.selectedPatient, async (newPatient) => {
  if (newPatient && newPatient.personId) {
    await populateWarnings();
  } else {
    activeWarnings.value = [];
  }
});

async function populateWarnings() {
  searchingForData.value = true;
  try {
    const warnings = await ApiPopulation.populateWarningsAndFlags(props.selectedPatient.personId);
    activeWarnings.value = warnings || [];
  } catch (error) {
    console.error('Error loading warnings:', error);
  } finally {
    searchingForData.value = false;
  }
}
</script>

<style scoped>
.emergency-warnings-view { width: 100%; }

.emergency-mode .emergency-banner {
  background: linear-gradient(90deg, #c0392b, #e74c3c);
  color: white;
  padding: 8px 16px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 15px;
  border-radius: 4px;
}

.warning-summary {
  margin-bottom: 15px;
  display: flex;
  justify-content: flex-end;
}

.warning-name {
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
