import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface EmergencyFlags {
  has_active_allergies: boolean;
  has_warnings: boolean;
  has_drug_interactions: boolean;
  critical_findings: string[];
}

export interface AiReviewResponse {
  summary: string;
  emergency_flags: EmergencyFlags;
  recommendations: string[];
  chat_response?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CareRecordService {
  private _nameCache: any = {};

  constructor(private http: HttpClient) { }

  getMedication(nhsNumber?: string, active?: number, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);
    params = params.append('active', active.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/medication', {params});
  }

  getReferrals(nhsNumber?: string): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);

    return this.http.get('api/events/referrals', {params});
  }

  getRegistries(page?: number, size?: number, nhsNumber?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('nhsNumber', nhsNumber);

    return this.http.get('api/events/registries', {params});
  }

  getEncounters(nhsNumber?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/encounters', {params});
  }

  getDiagnostics(nhsNumber?: string, term?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);
    params = params.append('term', term.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/diagnostics', {params});
  }

  getAppointment(nhsNumber?: string): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);

    return this.http.get('api/events/appointment', {params});
  }

  getObservation(nhsNumber?: string, eventType?: number, active?: number, term?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);
    params = params.append('eventType', eventType.toString());
    params = params.append('active', active.toString());
    params = params.append('term', term.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/observation', {params});
  }

  getAllergy(nhsNumber?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('nhsNumber', nhsNumber);
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/allergy', {params});
  }

  getPatients(page?: number, size?: number, name?: string, nhsNumber?: string, dob?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('name', name.toString());
    params = params.append('nhsNumber', nhsNumber);
    params = params.append('dob', dob.toString());

    return this.http.get('api/events/patients', {params});
  }

  getPatientSummary(nhsNumber?: string): Observable<any> {
    let params = new HttpParams();

    if (nhsNumber) params = params.append('nhsNumber', nhsNumber);

    return this.http.get('api/events/patientsummary', {params});
  }

  getDashboard(nhsNumber:string, dateFrom: string, dateTo: string, term: string): Observable<any> {
    let params = new HttpParams();
    params = params.append('nhsNumber', nhsNumber);
    params = params.append('dateFrom', dateFrom);
    params = params.append('dateTo', dateTo);
    params = params.append('term', term);
    return this.http.get('api/events/dashboard', {params});
  }

  generateAiReview(nhsNumber: string, symptoms?: string): Observable<AiReviewResponse> {
    const body = { nhsNumber, symptoms };
    return this.http.post<AiReviewResponse>('api/events/ai-review/generate-summary', body);
  }

  sendAiChat(nhsNumber: string, conversationHistory: ChatMessage[], symptoms?: string): Observable<AiReviewResponse> {
    const body = { nhsNumber, conversationHistory, symptoms };
    return this.http.post<AiReviewResponse>('api/events/ai-review/chat', body);
  }

  clearAiCache(nhsNumber?: string): Observable<any> {
    let params = new HttpParams();
    if (nhsNumber) {
      params = params.append('nhsNumber', nhsNumber);
    }
    return this.http.get('api/events/ai-review/cache/clear', {params});
  }

}

}
