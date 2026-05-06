import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({selector: 'app-emergency-encounters', templateUrl: './emergency-encounters.component.html', styleUrls: ['./emergency-encounters.component.scss']})
export class EmergencyEncountersComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp'; searchingForData = false; recentEncounters: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() { if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') this.loadEncounters(); }
  loadEncounters() { const n = this.precisComponentReference?.nhsNumber; if (!n || n === '0') return; this.searchingForData = true; this.carerecordService.getEncounters(n, 1).subscribe({next: (r: any) => { this.recentEncounters = r?.results || []; }, error: (e: any) => this.log.error('Error loading encounters:', e), complete: () => { this.searchingForData = false; }}); }
  setViewMode(m: string) { this.viewMode = m; }
}
