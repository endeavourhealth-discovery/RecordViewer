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

              <div v-for="(msg, idx) in conversationHistory" :key="idx" class="chat-message" :class="msg.role">
                <div class="message-avatar">
                  <i v-if="msg.role === 'user'" class="fa-solid fa-user"></i>
                  <i v-else class="fa-solid fa-robot"></i>
                </div>
                <div class="message-content-wrapper">
                  <!-- Display inline images if present -->
                  <img 
                    v-if="msg.images && msg.images.length > 0" 
                    :src="msg.images[0]" 
                    alt="Uploaded image" 
                    class="inline-image"
                    @click="openImageModal(msg.images[0])"
                  />
                  <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                </div>
              </div>

              <div v-for="(msg, idx) in conversationHistory.filter(m => m.role === 'assistant' && m.thinking_content)" :key="'think-' + idx" class="chat-message assistant">
                <div class="message-avatar">
                  <i class="fa-solid fa-robot"></i>
                </div>
                <div class="thinking-container">
                  <details class="thinking-details">
                    <summary class="thinking-summary">
                      <i class="fa-solid fa-brain"></i> Thinking Process
                    </summary>
                    <div class="thinking-content" v-html="renderMarkdown(msg.thinking_content)"></div>
                  </details>
                </div>
              </div>

              <div v-if="chatLoading" class="chat-message assistant">
                <div class="message-avatar">
                  <i class="fa-solid fa-robot"></i>
                </div>
                <div class="message-content loading-indicator">
                  <ProgressSpinner style="width: 20px; height: 20px;"/>
                </div>
              </div>
            </div>

            <!-- Image Preview Area -->
            <div v-if="selectedImage" class="image-upload-area">
              <div class="image-preview-container">
                <img :src="imagePreviewUrl" alt="Selected image preview"/>
                <button class="remove-image-btn" @click="removeSelectedImage" title="Remove image">
                  <i class="fa-solid fa-times"></i>
                </button>
              </div>
            </div>

            <!-- Image Upload + Chat Input -->
            <div class="chat-input-area-with-upload">
              <input 
                type="file" 
                ref="imageInputRef" 
                accept="image/*" 
                style="display: none;" 
                @change="onImageSelect"
              />
              
              <!-- Image upload button -->
              <label class="image-upload-label" title="Upload image (max 10MB)" v-if="!selectedImage">
                <i class="fa-solid fa-image"></i>
                <input type="file" ref="imageInputRef" accept="image/*" style="display: none;" @change="onImageSelect"/>
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
const selectedImage = ref(null);
const imagePreviewUrl = ref(null);
const MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB default
const imageInputRef = ref(null);

watch(() => props.selectedPatient, async (newPatient) => {
  if (!newPatient || !newPatient.personId) {
    aiStore.clear();
    error.value = '';
    symptomsInput.value = '';
    conversationHistory.value = [];
    chatInput.value = '';
  } else {
    aiStore.clearForPatient(newPatient.personId);
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

function onImageSelect(event) {
  const target = event.target;
  const file = target.files?.[0];
  
  if (!file) return;
  
  // Validate size (10MB limit)
  if (file.size > MAX_IMAGE_SIZE) {
    error.value = `Image too large. Maximum size is ${MAX_IMAGE_SIZE / 1024 / 1024}MB.`;
    target.value = '';
    return;
  }
  
  selectedImage.value = file;
  
  // Generate preview URL
  const reader = new FileReader();
  reader.onload = (e) => {
    imagePreviewUrl.value = e.target?.result;
  };
  reader.readAsDataURL(file);
}

function removeSelectedImage() {
  selectedImage.value = null;
  imagePreviewUrl.value = null;
  if (imageInputRef.value) {
    imageInputRef.value.value = '';
  }
}

async function sendChatMessage() {
  if ((!chatInput.value.trim() && !selectedImage.value) || !props.selectedPatient?.personId || chatLoading.value) return;

  const userMessage = chatInput.value.trim();
  
  // If image selected, use the special endpoint with images
  if (selectedImage.value) {
    chatLoading.value = true;
    
    // Store file in local variable to preserve Blob identity through async call
    const imageFile = selectedImage.value;
    
    // Add user message to history first (without image for display purposes - we'll add it after response)
    const msgWithImages = { 
      role: 'user', 
      content: userMessage || '',
      images: []  // Will be populated with base64 data URLs after sending
    };
    
    conversationHistory.value.push(msgWithImages);
    chatInput.value = '';
    
    try {
      const response = await ApiPopulation.sendAiChatWithImages(
        props.selectedPatient.personId,
        conversationHistory.value.slice(-20),
        symptomsInput.value || undefined,
        imageFile
      );

      if (response.chat_response) {
        // Update the last user message with image data URL for display
        const lastUserMsg = conversationHistory.value[conversationHistory.value.length - 1];
        if (lastUserMsg.role === 'user') {
          // We need to convert file back to base64 for display
          const reader = new FileReader();
          reader.onload = () => {
            lastUserMsg.images = [reader.result];
          };
          reader.readAsDataURL(imageFile);
        }
        
        const assistantMessage = { 
          role: 'assistant', 
          content: response.chat_response 
        };
        if (response.thinking_content) {
          assistantMessage.thinking_content = response.thinking_content;
        }
        conversationHistory.value.push(assistantMessage);
      } else if (response.summary) {
        conversationHistory.value.push({ role: 'assistant', content: response.summary });
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
      clearSelectedImage();
      await nextTick();
      scrollToBottom();
    }
  } else {
    // Original text-only flow
    conversationHistory.value.push({ role: 'user', content: userMessage });
    chatInput.value = '';
    chatLoading.value = true;

    try {
      const response = await ApiPopulation.sendAiChat(
        props.selectedPatient.personId,
        conversationHistory.value.slice(-20),
        symptomsInput.value || undefined
      );

      if (response.chat_response) {
        const assistantMessage = { role: 'assistant', content: response.chat_response };
        if (response.thinking_content) {
          assistantMessage.thinking_content = response.thinking_content;
        }
        conversationHistory.value.push(assistantMessage);
      } else if (response.summary) {
        conversationHistory.value.push({ role: 'assistant', content: response.summary });
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

function clearSelectedImage() {
  selectedImage.value = null;
  imagePreviewUrl.value = null;
  if (imageInputRef.value) {
    imageInputRef.value.value = '';
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

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-upload-area {
  margin-bottom: 8px;
}

.image-preview-container {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e0e0e0;
}

.image-preview-container img {
  max-width: 150px;
  max-height: 150px;
  display: block;
}

.remove-image-btn {
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

.remove-image-btn:hover {
  background-color: rgba(192, 57, 43, 1);
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

.image-upload-label {
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

.image-upload-label:hover {
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
</style>
