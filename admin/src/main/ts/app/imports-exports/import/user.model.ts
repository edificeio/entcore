
export type Profile = 'Student' | 'Teacher' | 'Relative' | 'Personnel' | 'Guest';

export type Error = {
    line:string, 
    reason:string, 
    attribute:string, 
    value:string,
    corrected?:boolean 
}; 

export class User {

    private attributes = ['line','firstName','lastName', 'birthDate','login','externalId','profiles','classesStr','state'];

    constructor(data:any){
        this.attributes.forEach(attr => { this[attr] = data[attr] });
        this.errors = new Map<string, Error>();    
        this.reasons = [];
    };
    line: number;
    firstName: string;
    lastName: string;
    birthDate : string;
    login: string;
    externalId: string;
    profiles: string[];
    classesStr: string;
    state : string;
    errors : Map<string,Error>; // <K=attribute,V=error>
    reasons : string[];

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

    private static _filter: any ; // {reasons: string} | {state: string} | Function;
    static filter() { return User._filter }

    static setFilter(type:'errors'| 'reasons' | 'state' | 'none' , value?:string):void {
        switch (type) {
            case 'errors' : User._filter = (u:User) => { return u.errors.size > 0; } ; break;
            case 'reasons' : User._filter = {reasons: value}; break;
            case 'state': User._filter = {state : value}; break;
            default : User._filter = {};
        }
    };
}