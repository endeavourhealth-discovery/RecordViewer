import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({selector: 'app-emergency-conditions', templateUrl: './emergency-conditions.component.html', styleUrls: ['./emergency-conditions.component.scss']})
export class EmergencyConditionsComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp'; searchingForData = false; activeConditions: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() { if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') this.loadConditions(); }
  loadConditions() { const n = this.precisComponentReference?.nhsNumber; if (!n || n === '0') return; this.searchingForData = true; this.carerecordService.getObservation(n, 1, 1, '', 0).subscribe({next: (r: any) => { this.activeConditions = r?.results || []; }, error: (e: any) => this.log.error('Error loading conditions:', e), complete: () => { this.searchingForData = false; }}); }
  setViewMode(m: string) { this.viewMode = m; }
}
