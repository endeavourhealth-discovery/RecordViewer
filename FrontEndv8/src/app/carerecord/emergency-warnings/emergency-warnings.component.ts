import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({selector: 'app-emergency-warnings', templateUrl: './emergency-warnings.component.html', styleUrls: ['./emergency-warnings.component.scss']})
export class EmergencyWarningsComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp'; searchingForData = false; activeWarnings: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() { if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') this.loadWarnings(); }
  loadWarnings() { const n = this.precisComponentReference?.nhsNumber; if (!n || n === '0') return; this.searchingForData = true; this.carerecordService.getObservation(n, 8, 1, '', 0).subscribe({next: (r: any) => { this.activeWarnings = r?.results || []; }, error: (e: any) => this.log.error('Error loading warnings:', e), complete: () => { this.searchingForData = false; }}); }
  setViewMode(m: string) { this.viewMode = m; }
}
