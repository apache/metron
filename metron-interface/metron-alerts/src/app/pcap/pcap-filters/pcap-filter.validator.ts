import { PcapRequest } from '../model/pcap.request'

export class FilterValidator {

  isStartTimeValid: boolean = true;
  isEndTimeValid: boolean = true;
  isFilterValid: boolean = true;

  validate(request: PcapRequest) {
    this.isStartTimeValid = !!(request.startTime);
    this.isEndTimeValid = !!(request.endTime);
    this.isFilterValid = this.isStartTimeValid && this.isEndTimeValid;

    return this.isFilterValid;
  }
}
