<template>
  <div class="emergency-dashboard" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-truck-medical"></i> EMERGENCY CLINICIAN VIEW MODE
    </div>

    <PageHeader icon="fa-solid fa-truck-medical" title="Emergency Dashboard" subtitle="At-a-glance patient overview for emergency clinicians" :searching="searchingForData"/>

    <Card v-if="selectedPatient">
      <template #content>
        <!-- Patient Summary -->
        <Panel header="Patient Summary" :toggleable="true" style="margin-bottom: 15px;">
          <div class="patient-summary-grid">
            <div class="summary-item">
              <label>Name</label>
              <span>{{ selectedPatient.name }}</span>
            </div>
            <div class="summary-item">
              <label>Date of Birth</label>
              <span>{{ formatDate(selectedPatient.dob) }}</span>
            </div>
            <div class="summary-item">
              <label>Age</label>
              <span>{{ selectedPatient.age }} years</span>
            </div>
            <div class="summary-item" v-if="selectedPatient.gender">
              <label>Gender</label>
              <span>{{ selectedPatient.gender }}</span>
            </div>
            <div class="summary-item" v-if="selectedPatient.nhsNumber">
              <label>NHS Number</label>
              <span>{{ selectedPatient.nhsNumber }}</span>
            </div>
            <div class="summary-item" v-if="emergencyStore.patientHeight || emergencyStore.patientWeight || emergencyStore.patientBMI">
              <label>Height</label>
              <span>{{ emergencyStore.patientHeight ? emergencyStore.patientHeight + ' cm' : 'N/A' }}</span>
            </div>
            <div class="summary-item" v-if="emergencyStore.patientHeight || emergencyStore.patientWeight || emergencyStore.patientBMI">
              <label>Weight</label>
              <span>{{ emergencyStore.patientWeight ? emergencyStore.patientWeight + ' kg' : 'N/A' }}</span>
            </div>
            <div class="summary-item" v-if="emergencyStore.patientHeight || emergencyStore.patientWeight || emergencyStore.patientBMI">
              <label>BMI</label>
              <Tag v-if="emergencyStore.patientBMI" :value="emergencyStore.patientBMI.toFixed(1)" :severity="getBMISeverity(emergencyStore.patientBMI)"/>
              <span v-else>N/A</span>
            </div>
          </div>
        </Panel>

        <!-- Critical Alerts -->
        <div class="emergency-alerts" v-if="emergencyStore.criticalAlerts.length > 0">
          <div class="alert-item" v-for="(alert, index) in emergencyStore.criticalAlerts" :key="index" :class="'alert-' + alert.type">
            <i :class="alert.icon"></i>
            <span>{{ alert.text }}</span>
          </div>
        </div>

        <!-- Main Emergency Data - Two Column Layout -->
        <div class="accordionDiv">
          <!-- Left Column: Active Conditions & Allergies -->
          <Accordion ref="leftAccordionRef" id="leftAccordion" expandIcon="fa-solid fa-plus" collapseIcon="fa-solid fa-minus" :multiple="true"
                      :activeIndex="leftActive" style="margin: 0 15px 0 0; border: var(--surface-100) double 1px;">
            <AccordionTab :disabled="emergencyStore.activeConditions.length === 0">
              <template #header>
                <div class="accordion-header-text">
                  Active conditions/problems
                  <div v-if="searchingForData && !emergencyStore.activeConditions.length" class="none-found">(searching)</div>
                  <div v-else-if="emergencyStore.activeConditions.length === 0" class="none-found">(none found)</div>
                </div>
                <i class="fa-solid fa-diagnoses" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
              </template>
              <Accordion class="sub-accordion" :multiple="true">
                <AccordionTab v-for="tab in emergencyStore.activeConditions" :key="tab.code" :header="tab.name">
                  <p>Status: <b>{{ tab.status }}</b></p>
                  <p>Date: <b>{{ tab.date }}</b></p>
                  <p>Source: <b>{{ tab.orgName }}</b></p>
                  <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                  <p>Code: <b>{{ tab.code }}</b></p>
                </AccordionTab>
              </Accordion>
            </AccordionTab>

            <AccordionTab :disabled="emergencyStore.allergies.length === 0">
              <template #header>
                <div class="accordion-header-text">
                  Allergies/intolerances
                  <div v-if="searchingForData && !emergencyStore.allergies.length" class="none-found">(searching)</div>
                  <div v-else-if="emergencyStore.allergies.length === 0" class="none-found">(none found)</div>
                </div>
                <i class="fa-solid fa-allergies" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
              </template>
              <Accordion class="sub-accordion" :multiple="true">
                <AccordionTab v-for="tab in emergencyStore.allergies" :key="tab.name" :header="tab.name">
                  <p>Code: <b>{{ tab.code }}</b></p>
                  <p>Status: <b>{{ tab.status }}</b></p>
                  <p>Organisation: <b>{{ tab.orgName }}</b></p>
                  <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                  <p>Date: <b>{{ tab.date }}</b></p>
                </AccordionTab>
              </Accordion>
            </AccordionTab>

            <AccordionTab :disabled="emergencyStore.warnings.length === 0">
              <template #header>
                <div class="accordion-header-text">
                  Warnings/flags
                  <div v-if="searchingForData && !emergencyStore.warnings.length" class="none-found">(searching)</div>
                  <div v-else-if="emergencyStore.warnings.length === 0" class="none-found">(none found)</div>
                </div>
                <i class="fa-solid fa-exclamation-triangle" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
              </template>
              <Accordion class="sub-accordion" :multiple="true">
                <AccordionTab v-for="tab in emergencyStore.warnings" :key="tab.name" :header="tab.name">
                  <p>Status: <b>{{ tab.status }}</b></p>
                  <p>Onset date: <b>{{ tab.date }}</b></p>
                  <p>Organisation: <b>{{ tab.orgName }}</b></p>
                </AccordionTab>
              </Accordion>
            </AccordionTab>
          </Accordion>

          <!-- Right Column: Current Medications -->
          <Accordion ref="rightAccordionRef" id="rightAccordion" expandIcon="fa-solid fa-plus" collapseIcon="fa-solid fa-minus" :multiple="true"
                      :activeIndex="rightActive" style="margin: 0 0 0 15px; border: var(--surface-100) double 1px;">
            <AccordionTab :disabled="emergencyStore.medication.length === 0">
              <template #header>
                <div class="accordion-header-text">
                  Current medication
                  <div v-if="searchingForData && !emergencyStore.medication.length" class="none-found">(searching)</div>
                  <div v-else-if="emergencyStore.medication.length === 0" class="none-found">(none found)</div>
                </div>
                <i class="fa-solid fa-pills" style="font-size:1.6rem"></i>
              </template>
              <Accordion class="sub-accordion" :multiple="true">
                <AccordionTab v-for="tab in emergencyStore.medication" :key="tab.name" :header="tab.name">
                  <p>Code: <b>{{ tab.code }}</b></p>
                  <p>Dose: <b>{{ tab.dose }}</b></p>
                  <p>Quantity: <b>{{ tab.quantity }}</b></p>
                  <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                  <p>Status: <b>{{ tab.status }}</b></p>
                  <p>Date: <b>{{ tab.date }}</b></p>
                </AccordionTab>
              </Accordion>
            </AccordionTab>

            <!-- AI Review Section -->
            <AccordionTab :disabled="false">
              <template #header>
                <div class="accordion-header-text">
                  AI Record Review
                </div>
                <i class="fa-solid fa-robot" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
              </template>
              <div class="ai-review-content">
                <Button label="Generate AI Summary" icon="fa-solid fa-wand-magic-sparkles" @click="generateAiSummary" :loading="generatingSummary" severity="danger"/>
				<br>
                    <div v-if="aiStore.error" class="ai-error">{{ aiStore.error }}</div>
                    <div v-else-if="!aiStore.hasSummary" style="padding: 10px; color: #999;">Click "Generate AI Summary" to begin.</div>
                    <div class="ai-summary-content" v-else>
                      <div v-if="aiStore.emergencyFlags && aiStore.emergencyFlags.critical_findings && aiStore.emergencyFlags.critical_findings.length > 0" class="critical-findings-box">
                        <h4><i class="fa-solid fa-triangle-exclamation"></i> Critical Findings</h4>
                        <ul>
                          <li v-for="(finding, idx) in (aiStore.emergencyFlags?.critical_findings || [])" :key="idx">{{ finding }}</li>
                        </ul>
                      </div>
                      <div class="ai-summary-text" v-html="renderMarkdown(aiStore.aiSummary)"></div>
                    </div>
              </div>
            </AccordionTab>
          </Accordion>
        </div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to view the emergency dashboard.</p>
      </template>
    </Card>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue';
import { ref, watch, nextTick, watchEffect } from 'vue';
import ApiPopulation from '@/API/ApiPopulation';
import { useAiReviewStore } from '@/stores/aiReview';
import { useEmergencyDataStore } from '@/stores/emergencyData';

const props = defineProps({
  selectedPatient: Object,
  viewMode: String,
});

const searchingForData = ref(false);
const emergencyStore = useEmergencyDataStore();
const aiStore = useAiReviewStore();
const generatingSummary = ref(false);

const leftActive = ref([]);
const rightActive = ref([1]);
const aiAnalysisActive = ref([0]);

// Synchronous init from cached data during setup (before render) - handles nav-back case
if (emergencyStore.lastPersonId && emergencyStore.hasData()) {
  if (emergencyStore.activeConditions.length > 0) leftActive.value.push(0);
  if (emergencyStore.allergies.length > 0) leftActive.value.push(1);
  if (emergencyStore.warnings.length > 0) leftActive.value.push(2);
}
if (emergencyStore.medication.length > 0) {
  rightActive.value = [0, 1];
} else {
  rightActive.value = [1];
}

function expandLeftAccordion() {
  leftActive.value = [];
  if (emergencyStore.activeConditions.length > 0) leftActive.value.push(0);
  if (emergencyStore.allergies.length > 0) leftActive.value.push(1);
  if (emergencyStore.warnings.length > 0) leftActive.value.push(2);
}

function expandRightAccordion() {
  rightActive.value = [1];
  if (emergencyStore.medication.length > 0) rightActive.value = [0, 1];
}

watch(() => props.selectedPatient, async (newPatient) => {
  if (!newPatient || !newPatient.personId) return;

  console.log('[EmergencyDashboard] Watch fired for patient:', newPatient.personId);
  
  await populateData();
}, { immediate: true });

async function populateData() {
  console.log('[EmergencyDashboard] populateData called');
  searchingForData.value = true;
  const personId = props.selectedPatient?.personId;

  // Check if we already have cached data for this patient (navigated back from another view)
  if (personId && emergencyStore.lastPersonId === personId && emergencyStore.hasData()) {
    console.log('[EmergencyDashboard] Using cached data');
    expandLeftAccordion();
    expandRightAccordion();
    searchingForData.value = false;
    return;
  }

  // No cached data - fetch fresh
  if (personId) {
    emergencyStore.clearForPatient(personId);
    aiStore.clearForPatient(personId);
  } else {
    emergencyStore.clear();
    aiStore.clear();
  }
  leftActive.value = [];
  rightActive.value = [1];

  try {
    console.log('[EmergencyDashboard] Fetching data for personId:', personId);
    
    // Fetch all data in parallel
    const [meds, allergiesData, conditions, warningsData] = await Promise.all([
      ApiPopulation.populateMedication(personId, "1"),
      ApiPopulation.populateAllergies(personId),
      ApiPopulation.populateActiveConditionsProblems(personId),
      ApiPopulation.populateWarningsAndFlags(personId),
    ]);

    console.log('[EmergencyDashboard] Data received - meds:', meds?.length, 'allergies:', allergiesData?.length, 'conditions:', conditions?.length, 'warnings:', warningsData?.length);

    emergencyStore.setData(personId, {
      medication: meds || [],
      allergies: allergiesData || [],
      activeConditions: conditions || [],
      warnings: warningsData || [],
      patientHeight: null,
      patientWeight: null,
      patientBMI: null,
      criticalAlerts: []
    });

    // Extract vitals from observations (if available)
    try {
      const vitals = await ApiPopulation.populateSummaryObservations(personId);
      extractVitals(vitals);
    } catch (e) {
      console.warn('Could not fetch vitals:', e);
    }

    buildCriticalAlerts();

    // Expand accordions after data loads - setTimeout gives PrimeVue time to initialize DOM
    await nextTick();
    setTimeout(() => {
      expandLeftAccordion();
      expandRightAccordion();
    }, 50);

  } catch (error) {
    console.error('Error populating emergency dashboard data:', error);
  } finally {
    searchingForData.value = false;
  }
}

function extractVitals(vitals) {
  emergencyStore.patientHeight = null;
  emergencyStore.patientWeight = null;
  emergencyStore.patientBMI = null;

  if (!vitals || vitals.length === 0) return;

  for (const vital of vitals) {
    const nameLower = (vital.name || '').toLowerCase();
    
    if (nameLower.includes('height') && !emergencyStore.patientHeight) {
      emergencyStore.patientHeight = extractNumericValue(vital);
    } else if (nameLower.includes('weight') && !emergencyStore.patientWeight) {
      emergencyStore.patientWeight = extractNumericValue(vital);
    } else if (nameLower.includes('bmi') && !emergencyStore.patientBMI) {
      emergencyStore.patientBMI = extractNumericValue(vital);
    }
  }

  // Calculate BMI if height and weight are available but BMI is not
  if (!emergencyStore.patientBMI && emergencyStore.patientHeight && emergencyStore.patientWeight) {
    const heightMeters = emergencyStore.patientHeight / 100;
    emergencyStore.patientBMI = emergencyStore.patientWeight / (heightMeters * heightMeters);
  }
}

function extractNumericValue(vital) {
  if (!vital) return null;
  
  // Try to get value from various possible fields
  const value = vital.value || vital.observationValue || vital.resultValue;
  if (value !== undefined && value !== null) {
    const num = parseFloat(value);
    return isNaN(num) ? null : num;
  }
  
  // Try to extract number from name or other text fields
  const text = vital.name || vital.description || '';
  const match = text.match(/(\d+\.?\d*)/);
  return match ? parseFloat(match[1]) : null;
}

function formatDate(dateString) {
  if (!dateString) return 'N/A';
  
  try {
    let date;
    if (typeof dateString === 'string') {
      date = new Date(dateString);
    } else {
      date = new Date(dateString);
    }
    
    if (isNaN(date.getTime())) return dateString;
    
    return date.toLocaleDateString('en-GB', { 
      day: '2-digit', 
      month: '2-digit', 
      year: 'numeric' 
    });
  } catch (e) {
    return dateString;
  }
}

function getBMISeverity(bmi) {
  if (bmi < 18.5) return 'info';
  if (bmi >= 18.5 && bmi < 25) return 'success';
  if (bmi >= 25 && bmi < 30) return 'warn';
  return 'danger';
}

function buildCriticalAlerts() {
  emergencyStore.criticalAlerts = [];

  if (emergencyStore.allergies.length > 0) {
    const allergyNames = emergencyStore.allergies.slice(0, 3).map(a => a.name).join(', ');
    emergencyStore.criticalAlerts.push({
      type: 'allergy',
      icon: 'fa-solid fa-triangle-exclamation',
      text: `ALLERGIES (${emergencyStore.allergies.length}): ${allergyNames}${emergencyStore.allergies.length > 3 ? '...' : ''}`
    });
  }

  if (emergencyStore.warnings.length > 0) {
    const warningNames = emergencyStore.warnings.slice(0, 2).map(w => w.name).join(', ');
    emergencyStore.criticalAlerts.push({
      type: 'warning',
      icon: 'fa-solid fa-exclamation-circle',
      text: `WARNINGS (${emergencyStore.warnings.length}): ${warningNames}${emergencyStore.warnings.length > 2 ? '...' : ''}`
    });
  }

  if (emergencyStore.medication.length >= 5) {
    emergencyStore.criticalAlerts.push({
      type: 'polypharmacy',
      icon: 'fa-solid fa-pills',
      text: `POLYPHARMACY: ${emergencyStore.medication.length} current medications`
    });
  }

  if (emergencyStore.activeConditions.length >= 3 && emergencyStore.medication.length >= 3) {
    emergencyStore.criticalAlerts.push({
      type: 'multimorbidity',
      icon: 'fa-solid fa-user-injured',
      text: `MULTI-MORBIDITY: ${emergencyStore.activeConditions.length} conditions, ${emergencyStore.medication.length} medications`
    });
  }
}

async function generateAiSummary() {
  if (!props.selectedPatient || !props.selectedPatient.personId) return;

  generatingSummary.value = true;
  aiStore.error = '';

  try {
    const response = await ApiPopulation.generateAiReview(props.selectedPatient.personId);
    aiStore.setResponse(props.selectedPatient.personId, response);
	aiAnalysisActive.value = [1];
  } catch (error) {
    console.error('Error generating AI summary:', error);
    aiStore.error = error.message || 'Failed to generate AI review. Please try again.';
  } finally {
    generatingSummary.value = false;
  }
}

function renderMarkdown(text) {
  if (!text) return '';
  let html = text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/#{1,6}\s*(.*?)(\n|$)/g, '<h4>$1</h4>')
    .replace(/\n/g, '<br/>');
  return html;
}

function clearAllData() {
  emergencyStore.clear();
  if (props.selectedPatient?.personId) {
    aiStore.clearForPatient(props.selectedPatient.personId);
  } else {
    aiStore.clear();
  }
  leftActive.value = [];
  rightActive.value = [];
}
</script>

<style scoped>
.emergency-dashboard {
  width: 100%;
}

.emergency-mode .emergency-banner {
  background: linear-gradient(90deg, #c0392b, #e74c3c);
  color: white;
  padding: 8px 16px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 15px;
  border-radius: 4px;
}

.patient-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  padding: 10px;
}

.summary-item {
  display: flex;
  flex-direction: column;
}

.summary-item label {
  font-size: 0.8em;
  color: #666;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.summary-item span {
  font-size: 1.1em;
  font-weight: 600;
  color: #2c3e50;
}

.emergency-alerts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.alert-item {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 0.85em;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.alert-allergy {
  background-color: #fdecea;
  color: #c0392b;
  border: 1px solid #e74c3c;
}

.alert-warning {
  background-color: #fef9e7;
  color: #b7950b;
  border: 1px solid #f1c40f;
}

.alert-polypharmacy {
  background-color: #fdf2e9;
  color: #d35400;
  border: 1px solid #e67e22;
}

.alert-multimorbidity {
  background-color: #ebf5fb;
  color: #2980b9;
  border: 1px solid #3498db;
}

.none-found {
  margin-left: 24px;
}

.accordionDiv {
  display: flex;
}

#leftAccordion {
  width: 46%;
}

#rightAccordion {
  width: 46%;
}

.accordion-header-text {
  display: flex;
  width: 80%;
  text-align: left;
}

.sub-accordion {
  text-align: left;
  margin-top: 3px;
  border-width: 0px;
}

.ai-review-content {
  padding: 10px;
}

.critical-findings-box {
  background-color: #fdecea;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  padding: 12px;
  margin-top: 8px;
  margin-bottom: 8px;
}

.critical-findings-box h4 {
  color: #c0392b;
  margin-top: 0;
  margin-bottom: 8px;
}

.critical-findings-box ul {
  margin: 0;
  padding-left: 20px;
}

.critical-findings-box li {
  margin-bottom: 4px;
  color: #c0392b;
}

.ai-summary-text {
  line-height: 1.6;
  white-space: pre-wrap;
}

.ai-error {
  color: #e74c3c;
  padding: 15px;
  text-align: center;
}

@media (max-width: 1000px) {
  .accordionDiv {
    display: inline;
  }

  #leftAccordion {
    width: 90%;
  }

  #rightAccordion {
    width: 90%;
  }
}
</style>
