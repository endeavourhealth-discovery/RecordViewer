<template>
  <div class="ai-admin-view">
    <PageHeader icon="fa-solid fa-file-shield" title="AI Administration" subtitle="Analyze incoming patient correspondence and extract actionable information" :searching="analyzing"/>

    <Card>
      <template #content>
        <!-- Section 1: Correspondence Input -->
        <div class="input-section">
          <h4><i class="fa-solid fa-envelope"></i> Correspondence</h4>
          <Textarea v-model="correspondenceText" placeholder="Paste or type the correspondence text here (letter, email, fax, message...)" rows="8" style="width: 100%;"/>

          <div class="known-patient-fields">
            <div class="field-group">
              <label>Patient Name (optional)</label>
              <InputText v-model="knownPatientName" placeholder="e.g., John Jones"/>
            </div>
            <div class="field-group">
              <label>Date of Birth (optional)</label>
              <InputText v-model="knownPatientDob" placeholder="e.g., 01/01/1990"/>
            </div>
            <div class="field-group">
              <label>NHS Number (optional)</label>
              <InputText v-model="knownNhsNumber" placeholder="e.g., 123 456 7890"/>
            </div>
          </div>

          <!-- Attachment Upload -->
          <div class="attachment-section">
            <label><i class="fa-solid fa-paperclip"></i> Attach scanned document or image (optional, max 10MB)</label>
          <div class="attachment-controls">
            <label class="attach-button" title="Upload attachment" v-if="!selectedAttachment">
              <i class="fa-solid fa-cloud-arrow-up"></i> Choose File
              <input 
                type="file" 
                ref="attachmentInputRef" 
                accept="image/*,.pdf" 
                style="display: none;" 
                @change="onAttachmentSelect"
              />
            </label>
              <div v-if="selectedAttachment" class="selected-attachment">
                <span>{{ selectedAttachment.name }} ({{ (selectedAttachment.size / 1024).toFixed(0) }}KB)</span>
                <Button icon="fa-solid fa-times" severity="danger" size="small" @click="removeSelectedAttachment"/>
              </div>
            </div>
          </div>

          <div class="action-buttons">
            <Button label="Analyze Correspondence" icon="fa-solid fa-magnifying-glass" @click="analyzeCorrespondence" :loading="analyzing" severity="primary"/>
            <Button label="Clear Results" icon="fa-solid fa-rotate-left" @click="clearResults" severity="secondary" :disabled="!hasAnalyzed"/>
          </div>
        </div>

        <!-- Section 2: Analysis Results -->
        <div class="results-section" v-if="summary || identifiedPatient || actionItems?.length || urgency">
          <h4><i class="fa-solid fa-clipboard-check"></i> Analysis Results</h4>

          <!-- Patient Identification -->
          <div class="result-card patient-card" v-if="identifiedPatient">
            <h5><i class="fa-solid fa-user"></i> Patient Identified</h5>
            <div class="patient-info">
              <div class="info-row">
                <span class="label">Name:</span>
                <span class="value">{{ identifiedPatient.name }}</span>
              </div>
              <div class="info-row">
                <span class="label">DOB:</span>
                <span class="value">{{ identifiedPatient.dob }}</span>
              </div>
              <div class="info-row">
                <span class="label">NHS Number:</span>
                <span class="value">{{ identifiedPatient.nhsNumber }}</span>
              </div>
              <div class="info-row">
                <span class="label">Gender:</span>
                <span class="value">{{ identifiedPatient.gender }}</span>
              </div>
              <div class="info-row" v-if="identifiedPatient.usual_gp">
                <span class="label">Usual GP:</span>
                <span class="value">{{ identifiedPatient.usual_gp }}</span>
              </div>
            </div>
            <Button label="View Patient Record" icon="fa-solid fa-right-to-bracket" @click="viewPatientRecord" severity="primary" size="small"/>
          </div>

          <!-- Urgency Badge -->
          <div class="result-card urgency-card" v-if="urgency">
            <h5><i class="fa-solid fa-flag"></i> Urgency</h5>
            <Tag :value="urgency" :severity="urgencySeverity"/>
          </div>

          <!-- Summary -->
          <div class="result-card summary-card" v-if="summary">
            <h5><i class="fa-solid fa-file-lines"></i> Summary</h5>
            <div class="summary-content" v-html="renderMarkdown(summary)"></div>
          </div>

          <!-- Action Items -->
          <div class="result-card actions-card" v-if="actionItems && actionItems.length > 0">
            <h5><i class="fa-solid fa-list-check"></i> Action Items</h5>
            <ul>
              <li v-for="(item, idx) in actionItems" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <!-- No patient identified -->
          <div class="result-card no-patient-card" v-if="!identifiedPatient && hasAnalyzed">
            <h5><i class="fa-solid fa-triangle-exclamation"></i> Patient Not Identified</h5>
            <p>The correspondence did not contain sufficient identifying information to match a patient. Check the summary for available details.</p>
          </div>

          <!-- Debug: Processed Images -->
          <div class="result-card debug-images-card" v-if="debugImages && debugImages.length > 0">
            <h5 @click="debugImagesOpen = !debugImagesOpen" style="cursor: pointer;">
              <i :class="debugImagesOpen ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-right'" style="font-size: 0.8em; transition: transform 0.2s;"></i>
              <i class="fa-solid fa-bug"></i> Debug: Processed Images ({{ debugImages.length }})
            </h5>
            <div v-if="debugImagesOpen" class="debug-images-grid">
              <div v-for="(img, idx) in debugImages" :key="idx" class="debug-image-item" @click="previewImage = img; previewOpen = true">
                <img :src="img" :alt="'Processed page ' + (idx + 1)" />
                <div class="debug-image-label">Page {{ idx + 1 }} <i class="fa-solid fa-magnifying-glass-plus" title="Click to enlarge"></i></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="errorMessage" class="error-message">
          <i class="fa-solid fa-circle-exclamation"></i> {{ errorMessage }}
        </div>

        <!-- Empty State -->
        <div v-if="!hasAnalyzed && !errorMessage" class="empty-state">
          <i class="fa-solid fa-file-shield" style="font-size: 3em; color: #ccc;"></i>
          <p>Paste correspondence text above and click "Analyze Correspondence" to get AI-powered analysis.</p>
        </div>
      </template>
    </Card>

    <!-- Image Preview Modal -->
    <div v-if="previewOpen" class="preview-overlay" @click.self="previewOpen = false">
      <div class="preview-modal">
        <div class="preview-header">
          <span class="preview-title">Image Preview</span>
          <button class="preview-close" @click="previewOpen = false"><i class="fa-solid fa-times"></i></button>
        </div>
        <div class="preview-body">
          <img :src="previewImage" alt="Enlarged preview" class="preview-image" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue';
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import ApiPopulation from '@/API/ApiPopulation';

const router = useRouter();

const correspondenceText = ref('');
const knownPatientName = ref('');
const knownPatientDob = ref('');
const knownNhsNumber = ref('');
const analyzing = ref(false);
const summary = ref('');
const identifiedPatient = ref(null);
const actionItems = ref([]);
const urgency = ref('');
const errorMessage = ref('');
const selectedAttachment = ref(null);
const attachmentInputRef = ref(null);
const debugImages = ref([]);
const debugImagesOpen = ref(false);
const previewImage = ref('');
const previewOpen = ref(false);
const MAX_FILE_SIZE = 8 * 1024 * 1024;

const hasAnalyzed = ref(false);

function getUrgencySeverity(urgencyVal) {
  if (!urgencyVal) return 'secondary';
  const lower = urgencyVal.toLowerCase();
  if (lower === 'urgent') return 'danger';
  if (lower === 'semi-urgent') return 'warn';
  if (lower === 'routine') return 'info';
  return 'secondary';
}

async function analyzeCorrespondence() {
  if (!correspondenceText.value.trim() && !selectedAttachment.value) {
    errorMessage.value = 'Please enter correspondence text or attach a document to analyze.';
    return;
  }

  analyzing.value = true;
  errorMessage.value = '';
  summary.value = '';
  identifiedPatient.value = null;
  actionItems.value = [];
  urgency.value = '';
  hasAnalyzed.value = false;

  try {
    const response = await ApiPopulation.adminAnalyze(
      correspondenceText.value,
      knownPatientName.value || undefined,
      knownPatientDob.value || undefined,
      knownNhsNumber.value || undefined,
      selectedAttachment.value || undefined
    );

    if (response.summary) {
      summary.value = response.summary;
    }
    if (response.identified_patient) {
      identifiedPatient.value = response.identified_patient;
      if (response.identified_patient.name) {
        knownPatientName.value = response.identified_patient.name;
      }
      if (response.identified_patient.dob) {
        knownPatientDob.value = response.identified_patient.dob;
      }
      if (response.identified_patient.nhsNumber) {
        knownNhsNumber.value = response.identified_patient.nhsNumber;
      }
    }
    if (response.action_items) {
      actionItems.value = response.action_items;
    }
    if (response.urgency) {
      urgency.value = response.urgency;
    }
    if (response.chat_response) {
      if (!summary.value) {
        summary.value = response.chat_response;
      }
    }
    if (response.debug_images) {
      debugImages.value = response.debug_images;
      debugImagesOpen.value = false;
    } else {
      debugImages.value = [];
    }
    hasAnalyzed.value = true;
  } catch (err) {
    errorMessage.value = err.message || 'Failed to analyze correspondence.';
  } finally {
    analyzing.value = false;
  }
}

function clearResults() {
  summary.value = '';
  identifiedPatient.value = null;
  actionItems.value = [];
  urgency.value = '';
  hasAnalyzed.value = false;
  errorMessage.value = '';
  debugImages.value = [];
  debugImagesOpen.value = false;
  previewImage.value = '';
  previewOpen.value = false;
  correspondenceText.value = '';
  knownPatientName.value = '';
  knownPatientDob.value = '';
  knownNhsNumber.value = '';
  selectedAttachment.value = null;
  if (attachmentInputRef.value) {
    attachmentInputRef.value.value = '';
  }
}

function viewPatientRecord() {
  if (!identifiedPatient.value) return;
  
  // Navigate to patient demographics by setting the search criteria
  // and triggering a patient search through the parent App
  const nhsNum = identifiedPatient.value.nhsNumber;
  if (nhsNum) {
    // Emit event to parent to trigger patient search
    window.dispatchEvent(new CustomEvent('admin-lookup-patient', {
      detail: { nhsNumber: nhsNum, name: identifiedPatient.value.name }
    }));
    // Also try direct navigation
    router.push({ path: '/', query: { nhsNum: nhsNum } });
  } else {
    // Fall back to name-based search
    const name = identifiedPatient.value.name || '';
    router.push({ path: '/', query: { name: name } });
  }
}

function onAttachmentSelect(event) {
  const target = event.target;
  const file = target.files?.[0];
  if (!file) return;
  
  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = `File too large. Maximum size is 10MB.`;
    target.value = '';
    return;
  }
  
  selectedAttachment.value = file;
}

function removeSelectedAttachment() {
  selectedAttachment.value = null;
  if (attachmentInputRef.value) {
    attachmentInputRef.value.value = '';
  }
}

function renderMarkdown(text) {
  if (!text) return '';
  let html = text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/#{1,6}\s*(.*?)(\n|$)/g, '<h4>$1</h4>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br/>');
  return html;
}


</script>

<style scoped>
.ai-admin-view {
  width: 100%;
}

.input-section {
  margin-bottom: 20px;
}

.input-section h4,
.results-section h4,
.chat-container .section-header h4 {
  color: #2c3e50;
  margin-top: 0;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.known-patient-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 12px 0;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-group label {
  font-size: 0.85em;
  font-weight: 600;
  color: #6c757d;
}

.attachment-section {
  margin: 12px 0;
}

.attachment-section label {
  font-size: 0.85em;
  font-weight: 600;
  color: #6c757d;
  display: block;
  margin-bottom: 6px;
}

.attachment-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.attach-button {
  cursor: pointer;
  padding: 6px 14px;
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9em;
  color: #495057;
  transition: all 0.2s;
}

.attach-button:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
}

.selected-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background-color: #f0f4f8;
  border-radius: 4px;
  font-size: 0.85em;
  color: #495057;
}

.action-buttons {
  margin-top: 15px;
}

.results-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 20px;
  margin-top: 20px;
}

.result-card {
  background-color: #f8f9fa;
  border-radius: 6px;
  padding: 14px;
  margin-bottom: 12px;
}

.result-card h5 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #495057;
  font-size: 0.95em;
  display: flex;
  align-items: center;
  gap: 6px;
}

.patient-card {
  border-left: 3px solid #3498db;
}

.patient-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.info-row {
  display: flex;
  gap: 6px;
  font-size: 0.9em;
}

.info-row .label {
  font-weight: 600;
  color: #6c757d;
  min-width: 80px;
}

.info-row .value {
  color: #2c3e50;
}

.urgency-card {
  border-left: 3px solid #e74c3c;
  display: flex;
  align-items: center;
  gap: 12px;
}

.urgency-card h5 {
  margin-bottom: 0;
}

.summary-card {
  border-left: 3px solid #2ecc71;
}

.summary-content {
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 0.95em;
}

.actions-card {
  border-left: 3px solid #f39c12;
}

.actions-card ul {
  margin: 0;
  padding-left: 20px;
}

.actions-card li {
  margin-bottom: 4px;
  color: #2c3e50;
  line-height: 1.5;
}

.no-patient-card {
  border-left: 3px solid #f39c12;
  background-color: #fff8e1;
}

.no-patient-card p {
  color: #856404;
  margin: 0;
}

.debug-images-card {
  border-left: 3px solid #6c757d;
  background-color: #f1f3f5;
}

.debug-images-card h5 {
  color: #6c757d;
  font-size: 0.85em;
  user-select: none;
}

.debug-images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
}

.debug-image-item {
  position: relative;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.debug-image-item img {
  display: block;
  max-width: 300px;
  max-height: 400px;
  width: auto;
  height: auto;
}

.debug-image-item {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.debug-image-item:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.debug-image-label i.fa-magnifying-glass-plus {
  margin-left: 4px;
  opacity: 0.5;
}

.debug-image-item:hover .debug-image-label i.fa-magnifying-glass-plus {
  opacity: 1;
}

.debug-image-label {
  padding: 4px 8px;
  font-size: 0.75em;
  color: #6c757d;
  background: #f8f9fa;
  border-top: 1px solid #dee2e6;
  text-align: center;
}

.error-message {
  color: #e74c3c;
  padding: 20px;
  text-align: center;
  background-color: #fdecea;
  border-radius: 4px;
  margin-top: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.preview-modal {
  background: #fff;
  border-radius: 8px;
  max-width: 95vw;
  max-height: 95vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #dee2e6;
  background: #f8f9fa;
}

.preview-title {
  font-weight: 600;
  color: #495057;
  font-size: 0.9em;
}

.preview-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #6c757d;
  font-size: 1.1em;
  padding: 4px;
}

.preview-close:hover {
  color: #e74c3c;
}

.preview-body {
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  max-height: calc(95vh - 44px);
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: calc(95vh - 44px);
  width: auto;
  height: auto;
}

@media (max-width: 768px) {
  .known-patient-fields {
    grid-template-columns: 1fr;
  }

  .patient-info {
    grid-template-columns: 1fr;
  }

  .chat-message {
    max-width: 95%;
  }
}
</style>
