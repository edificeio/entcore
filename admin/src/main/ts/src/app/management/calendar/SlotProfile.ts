export interface SlotProfile {
    _id?: string;
    name: string;
    schoolId: string;
    slots: Slot[];
}

export interface Slot {
    id?: string;
    name: string;
    startHour: string;
    endHour: string;
}
