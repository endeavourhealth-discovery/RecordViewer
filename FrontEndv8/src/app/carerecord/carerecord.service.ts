import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CareRecordService {
  private _nameCache: any = {};

  constructor(private http: HttpClient) { }

  getMedication(page?: number, size?: number, patientId?: number, active?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());
    params = params.append('active', active.toString());

    return this.http.get('api/events/medication', {params});
  }

  getReferrals(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/referrals', {params});
  }

  getEncounters(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/encounters', {params});
  }

  getDiagnostics(page?: number, size?: number, patientId?: number, term?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());
    params = params.append('term', term.toString());

    return this.http.get('api/events/diagnostics', {params});
  }

  getAppointment(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/appointment', {params});
  }

  getObservation(page?: number, size?: number, patientId?: number, eventType?: number, active?: number, term?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());
    params = params.append('eventType', eventType.toString());
    params = params.append('active', active.toString());
    params = params.append('term', term.toString());

    return this.http.get('api/events/observation', {params});
  }

  getAllergy(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

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

  getFhir(patientId?: number): Observable<any> {
    let params = new HttpParams();
    if (patientId) params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/fhir', {params});
  }

  getFilteredFhir(requestParams?: string): Observable<any> {
    console.log('Inside getFilteredFHI')
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
      })
    };
    return this.http.post('api/events/fhir', requestParams, httpOptions);
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
