import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';

export interface Role {
    id: string;
    name: string;
}

export type Profile = 'Teacher' | 'Student' | 'Relative' | 'Guest' | 'Personnel' | 'AdminLocal';

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
