import { TestBed } from '@angular/core/testing';

import { CareRecordService } from './carerecord.service';

describe('CareRecordService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: CareRecordService = TestBed.get(CareRecordService);
    expect(service).toBeTruthy();
  });
});
