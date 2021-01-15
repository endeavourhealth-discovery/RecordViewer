import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';

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

}
