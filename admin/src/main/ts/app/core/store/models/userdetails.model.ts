import { Model } from 'entcore-toolkit'
import { GroupModel } from '../../store/models'

export class UserDetailsModel extends Model<UserDetailsModel> {

    constructor() {
        super({
            sync: '/directory/user/:id?manual-groups=true',
            update: '/directory/user/:id'
        })
    }

    id?: string
    activationCode?: string
    firstName?: string
    lastName?: string
    displayName?: string
    externalId?: string
    created?: string
    modified?: string
    lastLogin?: string
    source?: string
    email?: string
    birthDate?: string
    oldemail?: string
    login?: string
    blocked?: boolean
    zipCode: string
    city: string
    address: string
    homePhone: string
    mobile?: string
    type?: Array<string>
    functions?: Array<[string, Array<string>]>
    teaches: boolean
    headTeacher?: Array<string>
    headTeacherManual?: Array<string>
    children?: Array<{id: string, firstName: string, lastName: string, displayName: string, externalId: string}>
    parents?: Array<{id: string, firstName: string, lastName: string, displayName: string, externalId: string}>
    functionalGroups?: GroupModel[]
    manualGroups?: GroupModel[]
    administrativeStructures?: Array<string>
    mergeKey?: string
    loginAlias?: string
    quota?: number
    storage?: number
    maxQuota?: number

    toggleBlock() {
        return this.http.put(`/auth/block/${this.id}`, { block: !this.blocked }).then(() => {
            this.blocked = !this.blocked
        })
    }

    sendResetPassword(dest: {type:  string, value: string}) {
        let payload = new window['URLSearchParams']()
        payload.append('login', this.login)
        if (dest.type === 'email') {
            payload.append('email', dest.value)
        } else if (dest.type === 'mobile') {
            payload.append('mobile', dest.value)
        }

        return this.http.post('/auth/sendResetPassword', payload)
    }

    sendIndividualMassMail(type: string){
        return this.http.get(`/directory/structure/massMail/${this.id}/${type}`, {responseType: 'blob'});
    }

    addRelative(parent) {
        return this.http.put(`/directory/user/${this.id}/related/${parent.id}`).then(() => {
            this.parents.push(parent)
        })
    }

    removeRelative(parent) {
        return this.http.delete(`/directory/user/${this.id}/related/${parent.id}`).then(() => {
            this.parents = this.parents.filter(p => p.id !== parent.id)
        })
    }

    addChild(child) {
        return this.http.put(`/directory/user/${child.id}/related/${this.id}`).then(() => {
            this.children.push(child)
        })
    }

    removeChild(child) {
        return this.http.delete(`/directory/user/${child.id}/related/${this.id}`).then(() => {
            this.children = this.children.filter(c => c.id !== child.id)
        })
    }

    deletePhoto() {
        return this.http.put(`/directory/userbook/${this.id}`, {"picture":""});
    }

    addHeadTeacherManual(structureId: string, structureExternalId: string, structureName: string, classe: any) {
        let relationToAdd = classe.externalId;
        return this.http.post(`/directory/${structureId}/user/${this.id}/headteacher`, {
            classExternalId: relationToAdd,
            structureExternalId : structureExternalId,
            structureName : structureName
        }).then(async (res) => {
            if(this.headTeacherManual === undefined){
                this.headTeacherManual = [];
            }
            this.headTeacherManual.push(relationToAdd);
        })
    }

    updateHeadTeacherManual(structureId: string, structureExternalId: string, classe: any) {
        let relationToRemove = classe.externalId;
        return this.http.put(`/directory/${structureId}/user/${this.id}/headteacher`,{
            classExternalId: relationToRemove,
            structureExternalId : structureExternalId
        }).then(() => {
            this.headTeacherManual.splice(this.headTeacherManual.findIndex((f) => f == relationToRemove), 1);
        })
    }

    addAdml(structureId) {
        return this.http.post(`/directory/user/function/${this.id}`, {
            functionCode: "ADMIN_LOCAL",
            inherit: "s",
            scope:  this.functions.find((f) => f[0] == "ADMIN_LOCAL") == null ? [structureId] : this.functions.find((f) => f[0] == "ADMIN_LOCAL")[1].concat(structureId)
        }).then(async (res) => {
            await this.http.get(`/directory/user/${this.id}/functions`).then((res) => {
                this.functions = res.data[0].functions;
            })
        })
    }


    removeAdml() {
        return this.http.delete(`/directory/user/function/${this.id}/ADMIN_LOCAL`).then(() => {
            this.functions.splice(this.functions.findIndex((f) => f[0] == "ADMIN_LOCAL"), 1);
        })
    }

    isAdml(structureId?: string) {
        if (this.functions && this.functions.length > 0) {
            let admlIndex = this.functions.findIndex((f) => f[0] == "ADMIN_LOCAL");
            if (admlIndex >= 0)
                return this.functions[admlIndex][1].includes(structureId);
        }
    }

    isAdmc() {
        return this.functions && this.functions.find((f) => f[0] == 'SUPER_ADMIN');
    }

    /**
     * Détermine si l'utilisateur n'est pas un ensseignant ou professeur principal venant de l'AAF
     * @param {string} structureExternalId
     * @param {String} classeName
     * @returns {boolean}
     */
    isNotTeacherOrHeadTeacher (structureExternalId: string, classe: any) {
        if(this.teaches === undefined){
            return true;
        }

        if (this.headTeacher && this.headTeacher.length > 0) {
            let headTeacherIndex = this.headTeacher.findIndex((f) => f == classe.externalId);
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
    isTeacherAndHeadTeacherFromAAF (structureExternalId: string, classe: any) {
        if(this.teaches === undefined){
            return false;
        }

        if (this.headTeacher && this.headTeacher.length > 0) {
            let headTeacherIndex = this.headTeacher.findIndex((f) => f == classe.externalId);
            return (headTeacherIndex >= 0);
        } else {
            return false;
        }
    }

    isHeadTeacherManual (structureExternalId: string,  classe: any) {
        if (this.headTeacherManual && this.headTeacherManual.length > 0) {
            let headTeacherManuelIndex = this.headTeacherManual.findIndex((f) => f == classe.externalId);
            return (headTeacherManuelIndex >= 0);
        } else {
            return false;
        }
    }

    generateMergeKey() {
        return this.http.post(`/directory/duplicate/generate/mergeKey/${this.id}`, {}).then((res) => {
            this.mergeKey = res.data.mergeKey
        });
    }

    updateLoginAlias() {
        return this.http.put(`/directory/user/${this.id}`, {loginAlias: this.loginAlias});
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
        }
    }
}
