import { ImportCSVService } from './import-csv.service'

export type Profile = 'Student' | 'Teacher' | 'Relative' | 'Personnel' | 'Guest';
export type FilterUser = 'errors'| 'reasons' | 'state' | 'none';
export type UserEditableProps = 'lastName'| 'firstName' | 'birthDate' | 'state';
export type UserState = 'Créé'|'Modifié'|'Supprimé' ;

export type Error = {
    line:string, 
    reason:string, 
    attribute:string, 
    value:string,
    corrected?:boolean 
}; 

export class User {

    private attributes = ['line','firstName','lastName', 'birthDate','login','loginAlias','externalId','profiles','classesStr','state','profile'];

    constructor(data:any){
        this.attributes.forEach(attr => { this[attr] = data[attr] });
        this.profiles = this.profiles || [this.profile]; // Hack 
        this.errors = new Map<string, Error>();
        this.reasons = [];
    };
    line: number;
    firstName: string;
    lastName: string;
    birthDate : string;
    login: string;
    loginAlias: string;
    externalId: string;
    profiles: string[];
    classesStr: string;
    state : string;
    errors : Map<string,Error>; // <K=attribute,V=error>
    reasons : string[];
    profile : string; // Use for delete lines. TODO use properties 'profiles' instead 

    isCorrected(attribute:string):boolean {
        if (!!this.errors.has(attribute))
            return this.errors.get(attribute).corrected;
        return false;
    };

    isWrong(attribute:string):boolean {
        if (this.errors.has(attribute))
            return !this.errors.get(attribute).corrected;
        return false;
    };
    
    hasProfile(profile:Profile): boolean {
        if (this.profiles != null && this.profiles[0] == profile)
            return true; 
        return false;
    };

    private static _filter:any = {} ; // {reasons: string} | {state: string} | Function;
    static filter() { return User._filter }

    static setFilter(type:FilterUser , value?:string):void {
        switch (type) {
            case 'errors' : User._filter = (u:User) => { return u.errors.size > 0; } ; break;
            case 'reasons' : User._filter = {reasons: value}; break;
            case 'state': User._filter = {state : value}; break;
            default : User._filter = {};
        }
    };
    static hasFilter(type:FilterUser, value:string):boolean {
        if (User._filter[type] != undefined) {
            if (User._filter[type] == value) {
                return true;
            }
        }
        return false;
    }

    static possibleState(state:UserState):UserState[]{
        switch (state) {
            case 'Créé' : return ['Créé','Supprimé'];
            case 'Modifié' :return ['Modifié','Supprimé'];
            case 'Supprimé': return ['Supprimé', 'Modifié'];
        }
    }

    /**
    * 
    * @param importId 
    * @param property 
    */
    async delete(importId:string){
        let res = await ImportCSVService.deleteLineReport(importId, this.profiles[0], this.line);
        if (res.error) {
            throw new Error(res.error);
        }
    }

    /**
     * Create a user's line in repport to keep user (avoid pre-deletion)
     * @param importId 
     * @param property 
     */
    async keep(importId:string){
        let data = {
            externalId : this.externalId,
            firstName : this.firstName,
            lastName : this.lastName
        };
        let res = await ImportCSVService.updateReport('post', importId, this.profiles[0], data);
        if (res.error) {
            throw new Error(res.error);
        }
    }

    /**
     * 
     * @param importId 
     * @param property 
     */
    async update(importId:string, property:UserEditableProps){
        let data = {
            line : this.line,
        };
        data[property] = this[property];
        let res = await ImportCSVService.updateReport('put', importId,this.profiles[0], data);
        if (res.error) {
            throw new Error(res.error);
        }
    }

    hasErrorsNotCorrected(): boolean {
        let res = false;
        if (this.errors) {
            const nbErrors = this.errors.size;
            if (nbErrors > 0) {
                res = true;
                let nbCorrected = 0;
                this.errors.forEach(error => {
                    if (error.corrected) {
                        nbCorrected++;
                    }
                });
                if (nbCorrected === nbErrors) {
                    res = false;
                }
            }
        } 
        return res;
    }

    hasWarnings(): boolean {
        if (this.errors && this.errors.size === 0) {
            return false;
        }
        return Array.from(this.errors.values()).every(error => error.reason === 'missing.student.soft');
    }
}