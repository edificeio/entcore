import { GroupModel, RoleModel } from "../../core/store";

export interface Role {
    id: string;
    name: string;
}

export type Profile = 'Teacher' | 'Student' | 'Relative' | 'Guest' | 'Personnel';

export interface Structure {
    id: string;
    name: string;
}

export interface MassAssignment {
    roles: Array<Role>;
    profiles: Array<Profile>;
    structure: Structure;
}

export interface Assignment {
    group: GroupModel;
    role: RoleModel;
}