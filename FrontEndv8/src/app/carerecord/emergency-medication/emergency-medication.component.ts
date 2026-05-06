import {Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({
  selector: 'app-emergency-medication',
  templateUrl: './emergency-medication.component.html',
  styleUrls: ['./emergency-medication.component.scss']
})
export class EmergencyMedicationComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;
  viewMode = 'gp';
  searchingForData = false;
  medications: any[] = [];

  constructor(private carerecordService: CareRecordService, private log: LoggerService) {}

  ngOnInit() {
    if (this.precisComponentReference?.nhsNumber && this.precisComponentReference.nhsNumber !== '0') {
      this.loadMedications();
    }
  }

  loadMedications() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.searchingForData = true;
    this.carerecordService.getMedication(nhsNumber, 1, 0).subscribe({
      next: (result: any) => { this.medications = result?.results || []; },
      error: (error: any) => this.log.error('Error loading medications:', error),
      complete: () => { this.searchingForData = false; }
    });
  }

  setViewMode(mode: string) { this.viewMode = mode; }
}
