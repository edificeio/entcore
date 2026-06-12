import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { SpinnerService } from "ngx-ode-ui";
import { Observable } from "rxjs";

@Injectable()
export class TimezoneService {
  constructor(
    private httpClient: HttpClient,
    private spinner: SpinnerService,
  ) {}

  private availableTimezones: string[] | null = null;

  getAvailableTimezones(): string[] {
    if (this.availableTimezones !== null) return this.availableTimezones;

    const availablePrefixes = ["Eur", "Pac", "Afr", "Ame", "Asi", "Aus"];
    try {
      this.availableTimezones = (Intl as any)
        .supportedValuesOf("timeZone")
        .filter((tz: string) =>
          availablePrefixes.some((prefix) => tz.startsWith(prefix)),
        );
    } catch {
      this.availableTimezones = [
        "Europe/Paris",
        "Europe/Brussels",
        "Europe/Lisbon",
        "Europe/Madrid",
        "Europe/Rome",
        "Europe/Berlin",
        "Europe/London",
        "Pacific/Noumea",
        "Pacific/Tahiti",
        "Pacific/Wallis",
        "America/Bogota",
        "America/Cayenne",
        "America/Guadeloupe",
        "America/Guyana",
        "America/Martinique",
        "America/Mexico_City",
      ];
    }

    return this.availableTimezones;
  }

  setStructureUsersTzAndQuietHours(structureId: string): Observable<string[]> {
    return this.httpClient.get<string[]>(
      `/directory/timetable/classes/${structureId}`,
    );
  }
}
