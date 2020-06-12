import {Component, Inject, OnInit} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {MatTableDataSource} from "@angular/material/table";
import {PageEvent} from "@angular/material/paginator";

export interface DialogData {

}

@Component({
  selector: 'app-trend',
  templateUrl: './trend.component.html',
  styleUrls: ['./trend.component.scss']
})

export class TrendComponent {
  page: number = 0;
  size: number = 10;

  constructor(
    public dialogRef: MatDialogRef<TrendComponent>,
    private log: LoggerService,
    @Inject(MAT_DIALOG_DATA) public data: DialogData) {

  }


  onPage(event: PageEvent) {
    this.page = event.pageIndex;
    this.size = event.pageSize;
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }
}
