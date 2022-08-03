import { Injectable } from "@angular/core";
import { globalStore } from "../store/global.store";
import { StructureModel } from "../store/models/structure.model";
import { UserModel } from "../store/models/user.model";
import { UserDetailsModel } from "../store/models/userdetails.model";
import http from 'axios';

@Injectable()
export class UserService {

    isEligibleForDirection(userDetails: UserDetailsModel, structure: StructureModel): boolean {
        return (userDetails.hasTeacherProfile() || userDetails.hasPersonnelProfile()) && globalStore.structures.get(structure.id).is1D();
    }

    visibleStructures(user: UserModel): Array<{id: string, name: string, externalId: string}> {
        return user.structures.filter(structure => globalStore.structures.data
            .find(manageableStructure => manageableStructure.id === structure.id));
    }

    invisibleStructures(user: UserModel): Array<{id: string, name: string, externalId: string}> {
        return user.structures.filter(structure => globalStore.structures.data
            .every(manageableStructure => manageableStructure.id !== structure.id));
    }

    addStructure(user: UserModel, structureId: string): Promise<void> {
        return http.put(`/directory/structure/${structureId}/link/${user.id}`)
            .then(() => {
                const targetStructure = globalStore.structures.data.find(s => s.id === structureId);
                if (targetStructure) {
                    user.structures.push({id: targetStructure.id, name: targetStructure.name, externalId: null});
                    if (targetStructure.users.data.length > 0)
                    {
                        targetStructure.users.data.push(user);
                        targetStructure.removedUsers.data = targetStructure.removedUsers.data
                            .filter(u => u.id !== user.id);
                    }
                    user.userDetails.unremoveFromStructure(targetStructure);
                }
            });
    }

    removeStructure(user: UserModel, structureId: string): Promise<void> {
        return http.delete(`/directory/structure/${structureId}/unlink/${user.id}`)
            .then(() => {
                user.structures = user.structures.filter(s => s.id !== structureId);
                const targetStructure = globalStore.structures.data.find(s => s.id === structureId);
                if (targetStructure)
                {
                    if(targetStructure.users.data.length > 0)
                    {
                        targetStructure.users.data = targetStructure.users.data
                            .filter(u => u.id !== user.id);
                        targetStructure.removedUsers.data.push(user);
                    }
                    user.userDetails.removeFromStructure(targetStructure);
                }
            });
    }

    separateDuplicate(user: UserModel, duplicateId: string): Promise<void> {
        return http.delete(`/directory/duplicate/ignore/${user.id}/${duplicateId}`).then(() => {
            const duplicate = user.duplicates.find(d => d.id === duplicateId);
            duplicate.structures.forEach(duplicatedStructure => {
                const structure = globalStore.structures.data.find(struct => struct.id === duplicatedStructure.id);
                if (structure && structure.users.data.length > 0) {
                    const user = structure.users.data.find(rUser => rUser.id === duplicateId);
                    if (user) { user.duplicates = user.duplicates.filter(d => d.id !== user.id); }
                }
            });
            user.duplicates = user.duplicates.filter(d => d.id !== duplicateId);
        });
    }

    visibleRemovedStructures(user: UserModel): Array<StructureModel> {
        let rmStructs = user.userDetails.removedFromStructures != null ? user.userDetails.removedFromStructures : [];
        return globalStore.structures.data.filter(struct => rmStructs.indexOf(struct.externalId) != -1);
    }
}