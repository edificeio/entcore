export interface Unit {
    /** i18n key of unit translation. */
    label: string;
    value: number;
}

export const UNITS: Array<Unit> = [
    { label: "quota.byte", value: 1 },
    { label: "quota.kilobyte", value: 1024 },
    { label: "quota.megabyte", value: Math.pow(1024, 2) },
    { label: "quota.gigabyte", value: Math.pow(1024, 3) }
];

export function toDecimal (value: number, decimal: number): number {
    return Math.round(value * Math.pow(10, decimal)) / Math.pow(10, decimal);
}

export function getUnit(bytes: number): Unit {
    let unit = 0;
    let finalValue = bytes;
    while(finalValue >= 1024 && unit < 3) {
        finalValue = finalValue / 1024;
        unit++;
    }
    return UNITS[unit];
}