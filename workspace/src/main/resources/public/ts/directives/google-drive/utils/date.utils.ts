import { moment } from "entcore";

export class DateUtils {
  static format(date: any, format: string): string {
    return moment(date).format(format);
  }
}
