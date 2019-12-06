import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpParams} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CareRecordService {
  private _nameCache: any = {};

  constructor(private http: HttpClient) { }

  getMedication(page?: number, size?: number, patientId?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/medication', {params});
  }

  getObservation(page?: number, size?: number, patientId?: number, eventType?: number): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('patientId', patientId.toString());
    params = params.append('eventType', eventType.toString());

    return this.http.get('api/events/observation', {params});
  }

  getPatients(page?: number, size?: number, name?: string, nhsNumber?: string): Observable<any> {
    console.log("page: "+page+", size: "+size);
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('name', name.toString());
    params = params.append('nhsNumber', nhsNumber.toString());

    return this.http.get('api/events/patients', {params});
  }

  getPatientSummary(patientId?: number): Observable<any> {
    let params = new HttpParams();
    if (patientId) params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/patientsummary', {params});
  }

  getDemographic(patientId?: number): Observable<any> {
    let params = new HttpParams();
    if (patientId) params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/demographic', {params});
  }

  getFhir(patientId?: number): Observable<any> {
    let params = new HttpParams();
    if (patientId) params = params.append('patientId', patientId.toString());

    return this.http.get('api/events/fhir', {params});
  }
}
