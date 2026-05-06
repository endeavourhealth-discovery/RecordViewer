import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({selector: 'app-emergency-vitals', templateUrl: './emergency-vitals.component.html', styleUrls: ['./emergency-vitals.component.scss']})
export class EmergencyVitalsComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp'; searchingForData = false; recentVitals: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() { if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') this.loadVitals(); }
  loadVitals() { const n = this.precisComponentReference?.nhsNumber; if (!n || n === '0') return; this.searchingForData = true; this.carerecordService.getObservation(n, 2, 0, '', 1).subscribe({next: (r: any) => { this.recentVitals = r?.results || []; }, error: (e: any) => this.log.error('Error loading vitals:', e), complete: () => { this.searchingForData = false; }}); }
  setViewMode(m: string) { this.viewMode = m; }
}
