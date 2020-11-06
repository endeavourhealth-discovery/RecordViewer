import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CareRecordService {
  private _nameCache: any = {};

  constructor(private http: HttpClient) { }

  getMedication(patientId?: number, active?: number, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());
    params = params.append('active', active.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/medication', {params});
  }

  getReferrals(patientId?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/referrals', {params});
  }

  getRegistries(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/registries', {params});
  }

  getEncounters(patientId?: number, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/encounters', {params});
  }

  getDiagnostics(patientId?: number, term?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());
    params = params.append('term', term.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/diagnostics', {params});
  }

  getAppointment(patientId?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/appointment', {params});
  }

  getObservation(patientId?: number, eventType?: number, active?: number, term?: string, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());
    params = params.append('eventType', eventType.toString());
    params = params.append('active', active.toString());
    params = params.append('term', term.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/observation', {params});
  }

  getAllergy(patientId?: number, summaryMode?: number): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId.toString());
    params = params.append('summaryMode', summaryMode.toString());

    return this.http.get('api/events/allergy', {params});
  }

  getPatients(page?: number, size?: number, name?: string, nhsNumber?: string, dob?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('name', name.toString());
    params = params.append('nhsNumber', nhsNumber.toString());
    params = params.append('dob', dob.toString());

    return this.http.get('api/events/patients', {params});
  }

  getPatientSummary(patientId?: number): Observable<any> {
    let params = new HttpParams();
    if (patientId) params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/patientsummary', {params});
  }

  getDashboard(patientId:string, dateFrom: string, dateTo: string, term: string): Observable<any> {
    let params = new HttpParams();

    params = params.append('patientId', patientId);
    params = params.append('dateFrom', dateFrom);
    params = params.append('dateTo', dateTo);
    params = params.append('term', term);

    return this.http.get('api/events/dashboard', {params});
  }

}
