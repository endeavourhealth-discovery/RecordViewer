<template>
  <div class="ai-review-view" :class="{ 'emergency-mode': viewMode === 'emergency' }">
    <div v-if="viewMode === 'emergency'" class="emergency-banner">
      <i class="fa-solid fa-robot"></i> AI RECORD REVIEW
    </div>

    <PageHeader icon="fa-solid fa-robot" title="AI Record Review" subtitle="AI-powered analysis of the patient's complete clinical record" :searching="generatingSummary"/>

    <Card v-if="selectedPatient">
      <template #content>
        <!-- Controls -->
        <div class="summary-controls">
          <Button label="Generate AI Summary" icon="fa-solid fa-wand-magic-sparkles" @click="generateSummary" :loading="generatingSummary" severity="danger"/>
          <Button label="Clear Cache & Regenerate" icon="fa-solid fa-rotate-right" @click="clearAndRegenerate" style="margin-left: 10px;" v-if="aiResponse?.summary"/>
        </div>

        <!-- Symptoms Input -->
        <div class="symptoms-input" v-if="viewMode === 'emergency'">
          <label><i class="fa-solid fa-stethoscope"></i> Report Patient Symptoms (optional):</label>
          <Textarea v-model="symptomsInput" placeholder="e.g., Chest pain, shortness of breath, fever..." rows="2" style="width: 100%; margin-top: 5px;"/>
        </div>

        <!-- Content + Legend Row -->
        <div class="content-legend-row">
          <!-- Single Scrollable Panel -->
          <div class="ai-content-panel" ref="aiContentPanelRef">
          <!-- Critical Findings -->
          <div class="critical-findings-box" v-if="aiStore.emergencyFlags && aiStore.emergencyFlags.critical_findings?.length > 0">
            <h4><i class="fa-solid fa-triangle-exclamation"></i> Critical Findings</h4>
            <ul>
              <li v-for="(finding, idx) in (aiStore.emergencyFlags?.critical_findings || [])" :key="idx">{{ finding }}</li>
            </ul>
          </div>

          <!-- Emergency Flag Badges -->
          <div class="emergency-flag-badges" v-if="aiStore.emergencyFlags">
            <Tag v-if="aiStore.emergencyFlags.has_active_allergies" value="Active Allergies" severity="danger"/>
            <Tag v-if="aiStore.emergencyFlags.has_warnings" value="Active Warnings" severity="warning"/>
            <Tag v-if="aiStore.emergencyFlags.has_drug_interactions" value="Drug Interaction Risk" severity="warn"/>
          </div>

          <!-- AI Summary -->
          <div class="ai-summary-content" v-if="aiStore.aiSummary">
            <div class="ai-disclaimer">
              <i class="fa-solid fa-circle-info"></i> AI-generated clinical guidance. Does not replace professional judgment.
            </div>
            <div class="ai-text" v-html="renderMarkdown(aiStore.aiSummary)"></div>
          </div>

          <!-- Error Message -->
          <div v-else-if="error || aiStore.error" class="error-message">
            <i class="fa-solid fa-circle-exclamation"></i> {{ error || aiStore.error }}
          </div>

          <!-- Empty State -->
          <div v-else class="empty-state">
            <i class="fa-solid fa-robot" style="font-size: 3em; color: #ccc;"></i>
            <p>Click "Generate AI Summary" to analyze this patient's complete clinical record.</p>
          </div>

          <!-- Chat Section -->
          <div class="chat-container">
            <div class="section-header">
              <h4><i class="fa-solid fa-comments"></i> Ask Follow-up Questions</h4>
            </div>
            <div class="chat-messages" ref="chatMessagesRef">
              <div v-if="conversationHistory.length === 0 && !aiStore.aiSummary" class="empty-chat">
                <i class="fa-solid fa-comments" style="font-size: 2em; color: #ccc;"></i>
                <p>No questions yet. Ask about medications, conditions, allergies, or symptoms.</p>
              </div>

              <template v-for="(msg, idx) in conversationHistory" :key="idx">
                <div class="chat-message" :class="msg.role">
                  <!-- Note messages: centered italic text, no avatar -->
                  <template v-if="msg.role === 'note'">
                    <div class="message-content note-content" v-html="msg.content"></div>
                  </template>
                  <template v-else>
                    <div class="message-avatar">
                      <i v-if="msg.role === 'user'" class="fa-solid fa-user"></i>
                      <i v-else class="fa-solid fa-robot"></i>
                    </div>
                    <div class="message-content-wrapper">
                      <!-- Display attachment indicator if present -->
                      <div v-if="msg.hasAttachment" class="attachment-indicator">
                        <i :class="msg.attachmentType === 'image' ? 'fa-solid fa-image' : 'fa-solid fa-file-lines'"></i>
                        {{ msg.attachmentType === 'image' ? 'Image Attached' : 'Document Attached' }}
                      </div>
                      <!-- Display inline images if present -->
                      <img 
                        v-if="msg.images && msg.images.length > 0" 
                        :src="msg.images[0]" 
                        alt="Uploaded image" 
                        class="inline-image"
                        @click="openImageModal(msg.images[0])"
                      />
                      <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                      <div v-if="msg.role === 'assistant' && (msg.prompt_tokens != null || msg.completion_tokens != null)" class="token-count">
                        <span :class="{ 'token-count-warning': msg.prompt_tokens != null && msg.max_context != null && msg.prompt_tokens >= msg.max_context }">
                          <i class="fa-solid fa-book-open"></i> {{ msg.prompt_tokens }} context
                          <span v-if="msg.max_context != null">(max: {{ msg.max_context }})</span>
                          <i v-if="msg.prompt_tokens != null && msg.max_context != null && msg.prompt_tokens >= msg.max_context" class="fa-solid fa-triangle-exclamation" style="margin-left: 4px;"></i>
                        </span>
                        <span style="margin: 0 8px; opacity: 0.4;">|</span>
                        <span :class="{ 'token-count-warning': msg.completion_tokens != null && msg.max_tokens != null && msg.completion_tokens >= msg.max_tokens }">
                          <i class="fa-solid fa-pen"></i> {{ msg.completion_tokens }} response
                          <span v-if="msg.max_tokens != null">(max: {{ msg.max_tokens }})</span>
                          <i v-if="msg.completion_tokens != null && msg.max_tokens != null && msg.completion_tokens >= msg.max_tokens" class="fa-solid fa-triangle-exclamation" style="margin-left: 4px;"></i>
                        </span>
                      </div>
                      <div v-if="msg.role === 'assistant' && msg.thinking_content" class="thinking-inline-container">
                        <details class="thinking-details">
                          <summary class="thinking-summary">
                            <i class="fa-solid fa-brain"></i> Thinking Process
                          </summary>
                          <div class="thinking-content" v-html="renderMarkdown(msg.thinking_content)"></div>
                        </details>
                      </div>
                    </div>
                  </template>
                </div>
              </template>

              <div v-if="chatLoading" class="chat-message assistant">
                <div class="message-avatar">
                  <i class="fa-solid fa-robot"></i>
                </div>
                <div class="message-content loading-indicator">
                  <ProgressSpinner style="width: 20px; height: 20px;"/>
                </div>
              </div>
            </div>

            <!-- Attachment Preview Area -->
            <div v-if="selectedAttachment" class="attachment-upload-area">
              <div class="attachment-preview-container">
                <!-- Image preview -->
                <div v-if="isImageAttachment" class="image-preview-wrapper">
                  <img :src="attachmentPreviewUrl" alt="Selected attachment preview"/>
                  <button class="remove-attachment-btn" @click="removeSelectedAttachment" title="Remove attachment">
                    <i class="fa-solid fa-times"></i>
                  </button>
                </div>
                <!-- Document preview (PDF, DOC, etc.) -->
                <div v-else class="document-preview-wrapper">
                  <div class="document-preview-icon" :class="getDocumentIconClass()">
                    <i :class="getDocumentIcon()"></i>
                  </div>
                  <div class="document-preview-info">
                    <div class="document-preview-name">{{ selectedAttachment.name }}</div>
                    <div class="document-preview-size">{{ (selectedAttachment.size / 1024 / 1024).toFixed(2) }} MB</div>
                  </div>
                  <button class="remove-attachment-btn" @click="removeSelectedAttachment" title="Remove attachment">
                    <i class="fa-solid fa-times"></i>
                  </button>
                </div>
              </div>
            </div>

            <!-- Attachment Upload + Chat Input -->
            <div class="chat-input-area-with-upload">
              <input 
                type="file" 
                ref="attachmentInputRef" 
                accept="image/*,.pdf,.doc,.docx,.rtf,.odt,.txt,.csv,.xls,.xlsx" 
                style="display: none;" 
                @change="onAttachmentSelect"
              />
              
              <!-- Unified attachment upload button -->
              <label class="attachment-upload-label" title="Upload attachment (max 10MB)" v-if="!selectedAttachment">
                <i class="fa-solid fa-paperclip"></i>
                <input 
                  type="file" 
                  ref="attachmentInputRef" 
                  accept="image/*,.pdf,.doc,.docx,.rtf,.odt,.txt,.csv,.xls,.xlsx" 
                  style="display: none;" 
                  @change="onAttachmentSelect"
                />
              </label>

              <!-- Text input + Send button -->
              <Textarea v-model="chatInput" placeholder="Ask about medications, conditions, allergies, or symptoms..." rows="2" @keydown.enter.exact.prevent="sendChatMessage" style="flex: 1;"/>
              <Button label="Send" icon="fa-solid fa-paper-plane" @click="sendChatMessage" :loading="chatLoading" severity="danger" class="send-button"/>
            </div>

            <div class="chat-disclaimer">
              <i class="fa-solid fa-circle-info"></i> AI-generated clinical guidance. Does not replace professional judgment.
            </div>
          </div>
        </div>

        <!-- Legend Panel -->
        <div class="token-legend">
          <div class="token-legend-header">
            <i class="fa-solid fa-circle-info"></i> Legend — Token Warnings
          </div>
          <div class="token-legend-content">
            <div class="legend-item">
              <i class="fa-solid fa-book-open"></i>
              <strong>Context tokens exceeded</strong> — The input exceeded the model's context window. Earlier parts of the patient record may have been truncated, making the analysis incomplete or unreliable.
            </div>
            <div class="legend-item">
              <i class="fa-solid fa-pen"></i>
              <strong>Response tokens exceeded</strong> — The LLM was cut off mid-response due to the max_tokens limit. The answer is incomplete and should be regenerated with a higher limit.
            </div>
          </div>
        </div>
      </div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <p style="text-align: center; padding: 40px;">Please select a patient to use the AI Record Review.</p>
      </template>
    </Card>
  </div>
</template>

<script setup>
import PageHeader from '@/components/PageHeader.vue';
import { ref, watch, nextTick } from 'vue';
import ApiPopulation from '@/API/ApiPopulation';
import { useAiReviewStore } from '@/stores/aiReview';

const props = defineProps({
  selectedPatient: Object,
  viewMode: String,
});

const generatingSummary = ref(false);
const chatLoading = ref(false);
const aiStore = useAiReviewStore();
const error = ref('');
const symptomsInput = ref('');
const chatInput = ref('');
const conversationHistory = ref([]);
const chatMessagesRef = ref(null);
const aiContentPanelRef = ref(null);
const selectedAttachment = ref(null);
const attachmentPreviewUrl = ref(null);
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB default
const attachmentInputRef = ref(null);

function stripThinkingContent(history) {
  return history.map(msg => {
    const { thinking_content, ...rest } = msg;
    return rest;
  });
}

function stripThinkingContentAndNotes(history) {
  return history
    .filter(msg => msg.role !== 'note')
    .map(msg => {
      if (msg.role === 'note') return msg;
      const { thinking_content, ...rest } = msg;
      return rest;
    });
}

watch(() => props.selectedPatient, async (newPatient, oldPatient) => {
  const newId = newPatient?.personId;
  const oldId = oldPatient?.personId;

  if (!newId) {
    aiStore.clear();
    error.value = '';
    symptomsInput.value = '';
    conversationHistory.value = [];
    chatInput.value = '';
  } else if (oldId && newId !== oldId) {
    aiStore.clearForPatient(newId);
    conversationHistory.value = [];
    symptomsInput.value = '';
    chatInput.value = '';
  }
});

async function generateSummary() {
  if (!props.selectedPatient?.personId) return;

  generatingSummary.value = true;
  error.value = '';

  try {
    const response = await ApiPopulation.generateAiReview(
      props.selectedPatient.personId,
      symptomsInput.value || undefined
    );
    aiStore.setResponse(props.selectedPatient.personId, response);
    conversationHistory.value.push({
      role: 'note',
      content: 'Patient record has been loaded. What would you like to know?'
    });
    await nextTick();
    scrollToBottom();
  } catch (err) {
    error.value = err.message || 'Failed to generate AI summary.';
  } finally {
    generatingSummary.value = false;
  }
}

async function clearAndRegenerate() {
  try {
    await ApiPopulation.clearAiCache(props.selectedPatient.personId);
    aiStore.clear();
    conversationHistory.value = [];
    symptomsInput.value = '';
    await generateSummary();
  } catch (err) {
    error.value = 'Error clearing cache: ' + err.message;
  }
}

function isImageAttachment() {
  if (!selectedAttachment.value) return false;
  const file = selectedAttachment.value;
  return file.type && file.type.startsWith('image/');
}

function onAttachmentSelect(event) {
  const target = event.target;
  const file = target.files?.[0];
  
  if (!file) return;
  
  // Validate size (10MB limit)
  if (file.size > MAX_FILE_SIZE) {
    error.value = `File too large. Maximum size is ${MAX_FILE_SIZE / 1024 / 1024}MB.`;
    target.value = '';
    return;
  }
  
  // Validate file type - only allow images and supported document types
  const allowedTypes = [
    'image/png', 'image/jpeg', 'image/gif', 'image/webp', 'image/svg+xml',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/rtf', 'text/rtf',
    'application/vnd.oasis.opendocument.text',
    'text/plain',
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ];
  
  if (!allowedTypes.includes(file.type) && !file.name.match(/\.(png|jpe?g|gif|webp|svg|pdf|doc|docx|rtf|odt|txt|csv|xls|xlsx)$/i)) {
    error.value = 'Unsupported file type. Please upload images, PDFs, or document files.';
    target.value = '';
    return;
  }
  
  selectedAttachment.value = file;
  
  // Generate preview URL for images
  if (file.type && file.type.startsWith('image/')) {
    const reader = new FileReader();
    reader.onload = (e) => {
      attachmentPreviewUrl.value = e.target?.result;
    };
    reader.readAsDataURL(file);
  } else {
    attachmentPreviewUrl.value = null;
  }
}

function removeSelectedAttachment() {
  selectedAttachment.value = null;
  attachmentPreviewUrl.value = null;
  if (attachmentInputRef.value) {
    attachmentInputRef.value.value = '';
  }
}

function getDocumentIcon() {
  if (!selectedAttachment.value) return 'fa-solid fa-file';
  const type = selectedAttachment.value.type;
  const name = selectedAttachment.value.name.toLowerCase();
  
  if (type === 'application/pdf' || name.endsWith('.pdf')) return 'fa-solid fa-file-pdf';
  if (type === 'application/msword' || type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' || name.endsWith('.doc') || name.endsWith('.docx')) return 'fa-solid fa-file-word';
  if (type === 'application/vnd.ms-excel' || type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || name.endsWith('.xls') || name.endsWith('.xlsx')) return 'fa-solid fa-file-excel';
  if (name.endsWith('.csv')) return 'fa-solid fa-file-csv';
  if (type === 'text/plain' || name.endsWith('.txt')) return 'fa-solid fa-file-alt';
  if (type === 'application/rtf' || type === 'text/rtf' || name.endsWith('.rtf')) return 'fa-solid fa-file-word';
  if (type === 'application/vnd.oasis.opendocument.text' || name.endsWith('.odt')) return 'fa-solid fa-file-alt';
  
  return 'fa-solid fa-file';
}

function getDocumentIconClass() {
  if (!selectedAttachment.value) return 'document-icon-default';
  const type = selectedAttachment.value.type;
  const name = selectedAttachment.value.name.toLowerCase();
  
  if (type === 'application/pdf' || name.endsWith('.pdf')) return 'document-icon-pdf';
  if (type === 'application/msword' || type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' || name.endsWith('.doc') || name.endsWith('.docx')) return 'document-icon-word';
  if (type === 'application/vnd.ms-excel' || type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || name.endsWith('.xls') || name.endsWith('.xlsx')) return 'document-icon-excel';
  
  return 'document-icon-default';
}

async function sendChatMessage() {
  if ((!chatInput.value.trim() && !selectedAttachment.value) || !props.selectedPatient?.personId || chatLoading.value) return;

  const userMessage = chatInput.value.trim();
  
  // If attachment selected, use the special endpoint with attachments
  if (selectedAttachment.value) {
    chatLoading.value = true;
    
    // Store file in local variable to preserve identity through async call
    const attachmentFile = selectedAttachment.value;
    const isImage = attachmentFile.type && attachmentFile.type.startsWith('image/');
    
    // Add user message to history first (without attachment for display purposes)
    const msgWithAttachment = { 
      role: 'user', 
      content: userMessage || '',
      images: [],
      hasAttachment: true,
      attachmentType: isImage ? 'image' : 'document'
    };
    
    conversationHistory.value.push(msgWithAttachment);
    chatInput.value = '';
    
    try {
      const historyToSend = stripThinkingContentAndNotes(conversationHistory.value.slice(-20));
      const response = await ApiPopulation.sendAiChatWithAttachment(
        props.selectedPatient.personId,
        historyToSend,
        symptomsInput.value || undefined,
        attachmentFile
      );

      if (response.chat_response) {
        // Update the last user message with image data URL for display
        const lastUserMsg = conversationHistory.value[conversationHistory.value.length - 1];
        if (lastUserMsg.role === 'user' && isImage) {
          // We need to convert file back to base64 for display
          const reader = new FileReader();
          reader.onload = () => {
            lastUserMsg.images = [reader.result];
          };
          reader.readAsDataURL(attachmentFile);
        }
        
        const assistantMessage = {
          role: 'assistant',
          content: response.chat_response,
          prompt_tokens: response.prompt_tokens,
          completion_tokens: response.completion_tokens,
          total_tokens: response.total_tokens,
          max_tokens: response.max_tokens,
          max_context: response.max_context
        };
        if (response.thinking_content) {
          assistantMessage.thinking_content = response.thinking_content;
        }
        conversationHistory.value.push(assistantMessage);
      } else if (response.summary) {
        conversationHistory.value.push({
          role: 'assistant',
          content: response.summary,
          prompt_tokens: response.prompt_tokens,
          completion_tokens: response.completion_tokens,
          total_tokens: response.total_tokens,
          max_tokens: response.max_tokens,
          max_context: response.max_context
        });
      }
    } catch (err) {
      // Remove the user message on error since it wasn't sent
      conversationHistory.value.pop();
      conversationHistory.value.push({
        role: 'assistant',
        content: `Error: ${err.message || 'Failed to get AI response.'}`
      });
    } finally {
      chatLoading.value = false;
      clearSelectedAttachments();
      await nextTick();
      scrollToBottom();
    }
  } else {
    // Original text-only flow
    conversationHistory.value.push({ role: 'user', content: userMessage });
    chatInput.value = '';
    chatLoading.value = true;

    try {
      const historyToSend = stripThinkingContentAndNotes(conversationHistory.value.slice(-20));
      const response = await ApiPopulation.sendAiChat(
        props.selectedPatient.personId,
        historyToSend,
        symptomsInput.value || undefined
      );

      if (response.chat_response) {
        const assistantMessage = {
          role: 'assistant',
          content: response.chat_response,
          prompt_tokens: response.prompt_tokens,
          completion_tokens: response.completion_tokens,
          total_tokens: response.total_tokens,
          max_tokens: response.max_tokens,
          max_context: response.max_context
        };
        if (response.thinking_content) {
          assistantMessage.thinking_content = response.thinking_content;
        }
        conversationHistory.value.push(assistantMessage);
      } else if (response.summary) {
        conversationHistory.value.push({
          role: 'assistant',
          content: response.summary,
          prompt_tokens: response.prompt_tokens,
          completion_tokens: response.completion_tokens,
          total_tokens: response.total_tokens,
          max_tokens: response.max_tokens,
          max_context: response.max_context
        });
      }
    } catch (err) {
      conversationHistory.value.pop(); // Remove user message on error
      conversationHistory.value.push({
        role: 'assistant',
        content: `Error: ${err.message || 'Failed to get AI response.'}`
      });
    } finally {
      chatLoading.value = false;
      await nextTick();
      scrollToBottom();
    }
  }
}

function clearSelectedAttachments() {
  selectedAttachment.value = null;
  attachmentPreviewUrl.value = null;
  if (attachmentInputRef.value) {
    attachmentInputRef.value.value = '';
  }
}

function openImageModal(imageUrl) {
  // Simple alert for now - could be replaced with a proper modal component
  const newWindow = window.open();
  if (newWindow) {
    newWindow.document.write(`
      <html>
        <head><title>Image Preview</title></head>
        <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f5f5f5;">
          <img src="${imageUrl}" style="max-width:90%;max-height:90%;object-fit:contain;" onclick="window.close()"/>
        </body>
      </html>
    `);
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

function scrollToBottom() {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
  }
  if (aiContentPanelRef.value) {
    aiContentPanelRef.value.scrollTop = aiContentPanelRef.value.scrollHeight;
  }
}
</script>

<style scoped>
.ai-review-view {
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

.summary-controls {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.symptoms-input {
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.symptoms-input label {
  font-weight: 600;
  color: #2c3e50;
}

.critical-findings-box {
  background-color: #fdecea;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 15px;
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

.emergency-flag-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.ai-summary-content {
  padding: 10px;
}

.ai-disclaimer {
  background-color: #fff3cd;
  border: 1px solid #ffc107;
  color: #856404;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 0.85em;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-text {
  line-height: 1.7;
  white-space: pre-wrap;
}

.error-message {
  color: #e74c3c;
  padding: 20px;
  text-align: center;
  background-color: #fdecea;
  border-radius: 4px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

/* Single scrollable panel */
.ai-content-panel {
  width: 50%;
  margin: 20px auto;
  max-height: calc(100vh - 350px);
  overflow-y: auto;
  padding-right: 10px;
  box-sizing: border-box;
}

.ai-content-panel * {
  box-sizing: border-box;
}

/* Chat styles */
.chat-container {
  display: flex;
  flex-direction: column;
  margin-top: 20px;
  border-top: 1px solid #e0e0e0;
  padding-top: 15px;
}

.section-header h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #2c3e50;
}

.chat-messages {
  flex: 1;
  min-height: 60px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 10px;
}

.empty-chat {
  text-align: center;
  color: #999;
  padding-top: 60px;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  max-width: 85%;
}

.chat-message.user {
  align-self: flex-end;
  margin-left: auto;
}

.chat-message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
}

.chat-message.user .message-avatar {
  background-color: #3498db;
  color: white;
}

.chat-message.assistant .message-avatar {
  background-color: #e74c3c;
  color: white;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
}

.inline-image {
  max-width: 300px;
  max-height: 200px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.inline-image:hover {
  transform: scale(1.02);
}

.message-content {
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.chat-message.user .message-content {
  background-color: #3498db;
  color: white;
  border-bottom-right-radius: 4px;
}

.chat-message.assistant .message-content {
  background-color: white;
  color: #2c3e50;
  border: 1px solid #ddd;
  border-bottom-left-radius: 4px;
}

.chat-message.note {
  align-self: center;
  max-width: 100%;
  text-align: center;
  padding: 8px 16px;
  font-style: italic;
  color: #6c757d;
  font-size: 0.85em;
  margin-bottom: 10px;
}

.chat-message.note .message-content {
  background: none;
  border: none;
  padding: 0;
  color: #6c757d;
  font-style: italic;
  white-space: normal;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attachment-upload-area {
  margin-bottom: 8px;
}

.attachment-preview-container {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.image-preview-wrapper,
.pdf-preview-wrapper {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
}

.image-preview-wrapper img {
  max-width: 150px;
  max-height: 150px;
  display: block;
}

.pdf-preview-icon {
  width: 150px;
  height: 100px;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 36px;
}

.pdf-preview-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 6px;
  font-size: 10px;
}

.pdf-preview-name {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pdf-preview-size {
  opacity: 0.8;
}

.document-preview-wrapper {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
}

.document-preview-icon {
  width: 150px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 36px;
}

.document-icon-pdf {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
}

.document-icon-word {
  background: linear-gradient(135deg, #2b579a, #1e3f6f);
}

.document-icon-excel {
  background: linear-gradient(135deg, #217346, #185a36);
}

.document-icon-default {
  background: linear-gradient(135deg, #6c757d, #495057);
}

.document-preview-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 6px;
  font-size: 10px;
}

.document-preview-name {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.document-preview-size {
  opacity: 0.8;
}

.attachment-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background-color: #fdecea;
  border: 1px solid #e74c3c;
  color: #c0392b;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.8em;
  font-weight: 600;
  margin-bottom: 8px;
}

.attachment-indicator i {
  font-size: 0.9em;
}

.remove-attachment-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: rgba(231, 76, 60, 0.9);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  padding: 0;
}

.remove-attachment-btn:hover {
  background-color: rgba(192, 57, 43, 1);
}

.pdf-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background-color: #fdecea;
  border: 1px solid #e74c3c;
  color: #c0392b;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.8em;
  font-weight: 600;
  margin-bottom: 8px;
}

.pdf-indicator i {
  font-size: 0.9em;
}

.chat-input-area-with-upload {
  display: flex;
  gap: 8px;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #ddd;
  align-items: center;
}

.attachment-upload-label {
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f8f9fa;
  border: 2px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #6c757d;
  transition: all 0.2s;
}

.attachment-upload-label:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
}

.chat-input-area {
  display: none !important;
}

.send-button {
  align-self: flex-end;
}

.chat-disclaimer {
  text-align: center;
  font-size: 0.8em;
  color: #999;
  padding: 8px;
}

.thinking-inline-container {
  margin-top: 8px;
}

.thinking-container {
  width: 100%;
  margin-top: 8px;
}

.thinking-details {
  background-color: #f0f4f8;
  border-radius: 6px;
  border: 1px solid #d1d9e0;
}

.thinking-summary {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 0.85em;
  color: #6c757d;
  user-select: none;
  list-style: none;
  display: flex;
  align-items: center;
  gap: 6px;
}

.thinking-summary::-webkit-details-marker {
  display: none;
}

.thinking-summary::before {
  content: '\f187';
  font-family: 'Font Awesome 7 Free';
  font-weight: 900;
  font-size: 0.8em;
}

.thinking-details[open] > .thinking-summary::before {
  content: '\f01d';
}

.thinking-content {
  padding: 12px;
  font-size: 0.85em;
  color: #6c757d;
  line-height: 1.5;
  border-top: 1px solid #d1d9e0;
}

.token-count {
  font-size: 0.75em;
  color: #6c757d;
  margin-top: 6px;
  padding: 3px 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  display: inline-block;
}

.token-count-warning {
  color: #e74c3c;
  background-color: #fdecea;
  font-weight: 600;
}

@media (max-width: 768px) {
  .summary-controls {
    flex-direction: column;
    gap: 10px;
  }

  .summary-controls Button {
    width: 100%;
  }

  .chat-message {
    max-width: 95%;
  }
}

.token-legend {
  flex: 0 0 20%;
  min-width: 180px;
  border: 1px solid #d1d9e0;
  border-radius: 4px;
  background-color: #f8f9fa;
}

.token-legend-header {
  padding: 6px 10px;
  font-size: 0.8em;
  font-weight: 600;
  color: #495057;
  border-bottom: 1px solid #d1d9e0;
}

.token-legend-header i {
  margin-right: 5px;
}

.token-legend-content {
  padding: 8px 10px;
  font-size: 0.75em;
  color: #6c757d;
  line-height: 1.5;
  overflow-wrap: anywhere;
  word-wrap: break-word;
}

.legend-item {
  margin-bottom: 6px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-item i {
  margin-right: 5px;
  width: 14px;
  text-align: center;
}

.content-legend-row {
  display: flex;
  gap: 12px;
  margin-bottom: 15px;
}

.content-legend-row .ai-content-panel {
  flex: 1 1 auto;
}
</style>
