import {Component, OnInit, ViewChild, ElementRef} from '@angular/core';
import {CareRecordService, AiReviewResponse, ChatMessage} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";
import {Globals} from "../globals";

@Component({
  selector: 'app-ai-review-view',
  templateUrl: './ai-review-view.component.html',
  styleUrls: ['./ai-review-view.component.scss']
})
export class AiReviewViewComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  @ViewChild('chatMessagesRef') chatMessagesRef: ElementRef;

  globals: Globals;
  viewMode = 'gp';
  activeTabIndex = 0;
  generatingSummary = false;
  chatLoading = false;
  aiResponse: AiReviewResponse | null = null;
  error = '';
  symptomsInput = '';
  chatInput = '';
  conversationHistory: ChatMessage[] = [];

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService,
    globals: Globals) {
    this.globals = globals;
  }

  ngOnInit() {}

  generateSummary() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.generatingSummary = true;
    this.error = '';
    this.aiResponse = null;

    this.carerecordService.generateAiReview(nhsNumber, this.symptomsInput || undefined).subscribe({
      next: (response: AiReviewResponse) => {
        this.aiResponse = response;
        if (response.summary) {
          this.conversationHistory.push({role: 'assistant', content: response.summary});
        }
        this.generatingSummary = false;
      },
      error: (err: any) => {
        this.error = err.message || 'Failed to generate AI summary.';
        this.generatingSummary = false;
      }
    });
  }

  clearAndRegenerate() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.carerecordService.clearAiCache(nhsNumber).subscribe({
      next: () => {
        this.aiResponse = null;
        this.conversationHistory = [];
        this.symptomsInput = '';
        this.generateSummary();
      },
      error: (err: any) => {
        this.error = 'Error clearing cache: ' + err.message;
      }
    });
  }

  sendChatMessage() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!this.chatInput.trim() || !nhsNumber || nhsNumber === '0' || this.chatLoading) return;

    const userMessage = this.chatInput.trim();
    this.conversationHistory.push({role: 'user', content: userMessage});
    this.chatInput = '';
    this.chatLoading = true;

    this.carerecordService.sendAiChat(nhsNumber, this.conversationHistory.slice(-20), this.symptomsInput || undefined).subscribe({
      next: (response: AiReviewResponse) => {
        const reply = response.chat_response || response.summary || '';
        if (reply) {
          this.conversationHistory.push({role: 'assistant', content: reply});
        }
        this.chatLoading = false;
        this.scrollToBottom();
      },
      error: (err: any) => {
        this.conversationHistory.push({
          role: 'assistant',
          content: `Error: ${err.message || 'Failed to get AI response.'}`
        });
        this.chatLoading = false;
        this.scrollToBottom();
      }
    });
  }

  onTabClick(event: any) {
    this.activeTabIndex = event.index;
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    let html = text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/#{1,6}\s*(.*?)(\n|$)/g, '<h4>$1</h4>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br/>');
    return html;
  }

  scrollToBottom() {
    if (this.chatMessagesRef) {
      setTimeout(() => {
        this.chatMessagesRef.nativeElement.scrollTop = this.chatMessagesRef.nativeElement.scrollHeight;
      }, 100);
    }
  }

  setViewMode(mode: string) {
    this.viewMode = mode;
  }
}
