import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({selector: 'app-emergency-allergy', templateUrl: './emergency-allergy.component.html', styleUrls: ['./emergency-allergy.component.scss']})
export class EmergencyAllergyComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp'; searchingForData = false; allergies: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() { if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') this.loadAllergies(); }
  loadAllergies() { const n = this.precisComponentReference?.nhsNumber; if (!n || n === '0') return; this.searchingForData = true; this.carerecordService.getAllergy(n, 0).subscribe({next: (r: any) => { this.allergies = r?.results || []; }, error: (e: any) => this.log.error('Error loading allergies:', e), complete: () => { this.searchingForData = false; }}); }
  setViewMode(m: string) { this.viewMode = m; }
}
