export interface IGoogleDriveQuotaResponse {
  limit: number | null;
  usage: number;
  usageInDrive: number;
  usageInDriveTrash: number;
}

export class GoogleDriveQuota {
  used!: number;
  total!: number;
  unit!: string;
  unlimited!: boolean;
  usedDisplay!: string;
  usedRatio!: number;
  // Raw byte values, kept alongside the rounded/unit-converted ones above (used/total are Mo or Go,
  // rounded for display) — needed for anything that must compare an exact file size against remaining
  // capacity (e.g. a pre-upload quota check) without reintroducing rounding error.
  usedBytes!: number;
  totalBytes!: number;

  build(data: IGoogleDriveQuotaResponse): GoogleDriveQuota {
    this.unlimited = data.limit === null || data.limit === 0;
    const usedBytes = data.usageInDrive || data.usage || 0;
    const totalBytes = data.limit || 0;
    this.usedBytes = usedBytes;
    this.totalBytes = totalBytes;

    const usedMo = usedBytes / (1024 * 1024);
    const totalMo = totalBytes / (1024 * 1024);
    const totalGo = totalMo / 1024;
    const usedGo = usedMo / 1024;

    if (!this.unlimited && totalMo > 2000) {
      this.total = Math.round(totalGo * 100) / 100;
      this.used = Math.round(usedGo * 100) / 100;
      this.unit = "Go";
    } else if (!this.unlimited) {
      this.total = Math.round(totalMo * 100) / 100;
      this.used = Math.round(usedMo * 100) / 100;
      this.unit = "Mo";
    } else {
      this.total = 0;
      this.used = 0;
      this.unit = "Mo";
    }

    if (usedBytes < 1024) {
      this.usedDisplay = `${usedBytes} o`;
    } else if (usedBytes < 1024 * 1024) {
      this.usedDisplay = `${Math.round(usedBytes / 1024)} Ko`;
    } else if (usedBytes < 1024 * 1024 * 1024) {
      this.usedDisplay = `${Math.round((usedBytes / (1024 * 1024)) * 100) / 100} Mo`;
    } else {
      this.usedDisplay = `${Math.round((usedBytes / (1024 * 1024 * 1024)) * 100) / 100} Go`;
    }

    this.usedRatio = totalBytes > 0 ? Math.min(100, (usedBytes / totalBytes) * 100) : 0;

    return this;
  }
}
