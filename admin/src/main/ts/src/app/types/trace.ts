import { Profile } from './profile';

export interface TraceResponse {
    _id: string;
    action: string;
    profile: Profile;
    structureId: string;
    created: {$date: string};
    modified: {$date: string};
    owner: {userId: string, displayName: string};
}

export interface Trace {
    id: string;
    action: string;
    profile: Profile;
    structureId: string;
    created: string;
    modified: string;
    ownerId: string;
    ownerDisplayName: string;
}