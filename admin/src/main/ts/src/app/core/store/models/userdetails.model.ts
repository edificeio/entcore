import {Model} from 'entcore-toolkit';
import {GroupModel} from '../../store/models/group.model';
import { StructureModel } from './structure.model';
import { globalStore } from '../global.store';

export enum UserProfiles
{
    Student = "Student",
    Relative = "Relative",
    Teacher = "Teacher",
    Personnel = "Personnel",
    Guest = "Guest"
}

export class UserDetailsModel extends Model<UserDetailsModel> {

    constructor() {
        super({
            sync: '/directory/user/:id?manual-groups=true',
            update: '/directory/user/:id'
        });
    }

    id?: string;
    activationCode?: string;
    firstName?: string;
    lastName?: string;
    displayName?: string;
    externalId?: string;
    created?: string;
    modified?: string;
    lastLogin?: string;
    source?: string;
    email?: string;
    birthDate?: string;
    oldemail?: string;
    login?: string;
    blocked?: boolean;
    zipCode: string;
    city: string;
    address: string;
    homePhone: string;
    mobile?: string;
    profiles: Array<String> = [];
    type?: Array<string>;
    functions?: Array<[string, Array<string>]>;
    teaches: boolean;
    headTeacher?: Array<string>;
    headTeacherManual?: Array<string>;
    direction?: Array<string>;
    directionManual?: Array<string>;
    children?: Array<{id: string, firstName: string, lastName: string, displayName: string, externalId: string}>;
    parents?: Array<{id: string, firstName: string, lastName: string, displayName: string, externalId: string}>;
    functionalGroups?: GroupModel[];
    manualGroups?: GroupModel[];
    administrativeStructures?: Array<string>;
    mergeKey?: string;
    mergedLogins?: Array<string>;
    mergedWith?: string;
    loginAlias?: string;
    quota?: number;
    storage?: number;
    maxQuota?: number;
    structureNodes?: Array<any>;
    removedFromStructures?: Array<String>;

    toggleBlock() {
        return this.http.put(`/auth/block/${this.id}`, { block: !this.blocked }).then(() => {
            this.blocked = !this.blocked;
        });
    }

    sendResetPassword(dest: {type: string, value: string}) {
        const payload = new window.URLSearchParams();
        payload.append('login', this.login);
        if (dest.type === 'email') {
            payload.append('email', dest.value);
        } else if (dest.type === 'mobile') {
            payload.append('mobile', dest.value);
        }

        return this.http.post('/auth/sendResetPassword', payload);
    }

    sendIndividualMassMail(type: string) {
        return this.http.get(`/directory/structure/massMail/${this.id}/${type}`, {responseType: 'blob'});
    }

    addRelative(parent) {
        return this.http.put(`/directory/user/${this.id}/related/${parent.id}`).then(() => {
            this.parents.push(parent);
        });
    }

    removeRelative(parent) {
        return this.http.delete(`/directory/user/${this.id}/related/${parent.id}`).then(() => {
            this.parents = this.parents.filter(p => p.id !== parent.id);
        });
    }

    addChild(child) {
        return this.http.put(`/directory/user/${child.id}/related/${this.id}`).then(() => {
            this.children.push(child);
        });
    }

    removeChild(child) {
        return this.http.delete(`/directory/user/${child.id}/related/${this.id}`).then(() => {
            this.children = this.children.filter(c => c.id !== child.id);
        });
    }

    deletePhoto() {
        return this.http.put(`/directory/userbook/${this.id}`, {picture: ''});
    }

    userMotto() {
        return this.http.get(`/directory/userbook/${this.id}`);
    }

    deleteUserMotto() {
        return this.http.put(`/directory/userbook/${this.id}`, {motto: ''});
    }

    addHeadTeacherManual(structureId: string, structureExternalId: string, classe: any) {
        const relationToAdd = classe.externalId;
        return this.http.post(`/directory/${structureId}/user/${this.id}/headteacher`, {
            classExternalId: relationToAdd,
            structureExternalId
        }).then(async (res) => {
            if (this.headTeacherManual === undefined) {
                this.headTeacherManual = [];
            }
            this.headTeacherManual.push(relationToAdd);
        });
    }

    updateHeadTeacherManual(structureId: string, structureExternalId: string, classe: any) {
        const relationToRemove = classe.externalId;
        return this.http.put(`/directory/${structureId}/user/${this.id}/headteacher`, {
            classExternalId: relationToRemove,
            structureExternalId
        }).then(() => {
            this.headTeacherManual.splice(this.headTeacherManual.findIndex((f) => f === relationToRemove), 1);
        });
    }

    addDirectionManual(structureId: string, structureExternalId: string) {
        return this.http.post(`/directory/${structureId}/user/${this.id}/direction`, {
            structureExternalId
        }).then(async (res) => {
            if (this.directionManual === undefined) {
                this.directionManual = [];
            }
            this.directionManual.push(structureExternalId);
        });
    }

    removeDirectionManual(structureId: string, structureExternalId: string) {
        return this.http.put(`/directory/${structureId}/user/${this.id}/direction`, {
            structureExternalId
        }).then(() => {
            this.directionManual.splice(this.directionManual.findIndex((f) => f === structureExternalId), 1);
        });
    }

    addAdml(structureId) {
        return this.http.post(`/directory/user/function/${this.id}`, {
            functionCode: 'ADMIN_LOCAL',
            inherit: 's',
            scope:  this.functions.find((f) => f[0] === 'ADMIN_LOCAL') == null ? [structureId] : this.functions.find((f) => f[0] === 'ADMIN_LOCAL')[1].concat(structureId)
        }).then(async (res) => {
            await this.http.get(`/directory/user/${this.id}/functions`).then((rRes) => {
                this.functions = rRes.data[0].functions;
            });
        });
    }


    removeAdml() {
        return this.http.delete(`/directory/user/function/${this.id}/ADMIN_LOCAL`).then(() => {
            this.functions.splice(this.functions.findIndex((f) => f[0] === 'ADMIN_LOCAL'), 1);
        });
    }

    isAdml(structureId?: string) {
        if (this.functions && this.functions.length > 0) {
            const admlIndex = this.functions.findIndex((f) => f[0] === 'ADMIN_LOCAL');
            if (admlIndex >= 0) {
                return this.functions[admlIndex][1].includes(structureId);
            }
        }
    }

    isAdmc() {
        return this.functions && this.functions.find((f) => f[0] === 'SUPER_ADMIN');
    }

    hasStudentProfile(): boolean {
        return this.profiles.indexOf(UserProfiles.Student) != -1;
    }

    hasRelativeProfile(): boolean {
        return this.profiles.indexOf(UserProfiles.Relative) != -1;
    }

    hasTeacherProfile(): boolean {
        return this.profiles.indexOf(UserProfiles.Teacher) != -1;
    }

    hasPersonnelProfile(): boolean {
        return this.profiles.indexOf(UserProfiles.Personnel) != -1;
    }

    hasGuestProfile(): boolean {
        return this.profiles.indexOf(UserProfiles.Guest) != -1;
    }

    /**
     * Détermine si l'utilisateur n'est pas un ensseignant ou professeur principal venant de l'AAF
     * @param {string} structureExternalId
     * @param {String} classeName
     * @returns {boolean}
     */
    isNotTeacherOrHeadTeacher(structureExternalId: string, classe: any) {

        if (!this.hasTeacherProfile()) {
            return true;
        }

        if (this.headTeacher && this.headTeacher.length > 0) {
            const headTeacherIndex = this.headTeacher.findIndex((f) => f === classe.externalId);
            return (headTeacherIndex >= 0);
        } else {
            return false;
        }
    }

    /**
     * Détermine si l'utilisateur est ensseignant et professeur principal venant de l'AAF
     * @param {string} structureExternalId
     * @param {String} classeName
     * @returns {boolean}
     */
    isTeacherAndHeadTeacherFromAAF(structureExternalId: string, classe: any) {
        if (this.teaches === undefined) {
            return false;
        }

        if (this.headTeacher && this.headTeacher.length > 0) {
            const headTeacherIndex = this.headTeacher.findIndex((f) => f === classe.externalId);
            return (headTeacherIndex >= 0);
        } else {
            return false;
        }
    }

    isHeadTeacherManual(structureExternalId: string,  classe: any) {
        if (this.headTeacherManual && this.headTeacherManual.length > 0) {
            const headTeacherManuelIndex = this.headTeacherManual.findIndex((f) => f === classe.externalId);
            return (headTeacherManuelIndex >= 0);
        } else {
            return false;
        }
    }

    /**
     * Détermine si l'utilisateur est directeur venant de l'AAF
     * @param {string} structureExternalId
     * @returns {boolean}
     */
    isDirectionFromAAF(structureExternalId: string) {
        if (this.direction && this.direction.length > 0) {
            const directionIndex = this.direction.findIndex((f) => f === structureExternalId);
            return (directionIndex >= 0);
        } else {
            return false;
        }
    }

    isEligibleForDirection(structure: StructureModel)
    {
        return (this.hasTeacherProfile() || this.hasPersonnelProfile()) && globalStore.structures.get(structure.id).is1D();
    }

    isDirectionManual(structureExternalId: string) {
        if (this.directionManual && this.directionManual.length > 0) {
            const directionIndex = this.directionManual.findIndex((f) => f === structureExternalId);
            return (directionIndex >= 0);
        } else {
            return false;
        }
    }

    generateMergeKey() {
        return this.http.post(`/directory/duplicate/generate/mergeKey/${this.id}`, {}).then((res) => {
            this.mergeKey = res.data.mergeKey;
        });
    }

    updateLoginAlias() {
        return this.http.put(`/directory/user/${this.id}`, {loginAlias: this.loginAlias});
    }

    updateLogin() {
        return this.http.put(`/directory/user/login/${this.id}`, {login: this.login});
    }

    removeFromStructure(struct: StructureModel)
    {
        if(this.removedFromStructures == null)
            this.removedFromStructures = [];
        this.removedFromStructures.push(struct.externalId);
    }

    unremoveFromStructure(struct: StructureModel)
    {
        if(this.removedFromStructures == null)
            this.removedFromStructures = [];
        this.removedFromStructures = this.removedFromStructures.filter(s => s != struct.externalId);
    }

    toJSON() {
        return {
            firstName:      this.firstName,
            lastName:       this.lastName,
            displayName:    this.displayName,
            birthDate:      this.birthDate,
            address:        this.address,
            city:           this.city,
            zipCode:        this.zipCode,
            email:          this.email,
            homePhone:      this.homePhone,
            mobile:         this.mobile
        };
    }
}
