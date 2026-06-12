import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { SpinnerService } from "ngx-ode-ui";
import { Observable } from "rxjs";

export type QuietHoursPreferences = {
  timezone: string;
  quietHours: {
    enabled: boolean;
    schedule: Array<number[]>;
  };
};

@Injectable()
export class TimezoneService {
  constructor(
    private httpClient: HttpClient,
    private spinner: SpinnerService,
  ) {}

  private availableTimezones: string[] | null = null;

  getAvailableTimezones(): string[] {
    if (this.availableTimezones !== null) return this.availableTimezones;

    const availablePrefixes = ["Eur", "Pac", "Afr", "Ame", "Asi", "Aus", "Ind"];
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
        "Indian/Mayotte",
        "Indian/Reunion",
        "Pacific/Noumea",
        "Pacific/Tahiti",
        "Pacific/Wallis",
        "America/Bogota",
        "America/Cayenne",
        "America/Guadeloupe",
        "America/Guyana",
        "America/Marigot",
        "America/Martinique",
        "America/Mexico_City",
        "America/Miquelon",
        "America/St_Barthelemy",
      ];
    }

    return this.availableTimezones;
  }

  getStructureQuietHours(
    structureId: string,
  ): Observable<QuietHoursPreferences> {
    return this.httpClient.get<QuietHoursPreferences>(
      `/admin/api/structures/${structureId}/quiethours-preferences`,
    );
  }

  setStructureQuietHours(
    structureId: string,
    timezone: string,
    enabled: boolean,
  ): Observable<void> {
    // By default, not quiet hours from 8h to 20h
    const defaultSchedule: Array<number[]> = [
      [0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23],
      [0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23],
      [0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23],
      [0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23],
      [0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23],
      [
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23,
      ],
      [
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23,
      ],
    ];

    const payload: QuietHoursPreferences = {
      timezone,
      quietHours: {
        enabled,
        schedule: defaultSchedule,
      },
    };

    return this.httpClient.post<void>(
      `/admin/api/structures/${structureId}/quiethours-preferences`,
      payload,
    );
  }
}
