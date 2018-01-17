const MESSAGES = {
    'import.info.file': 10,
    'import.info.deleteAccount' : 1,
    'import.info.transition' : 5,
    'import.classesChecking.info': 4
};

export class Messages {
    constructor() {
        this.dictionary = {};
    }
    private dictionary:{};

     get(key:string):string[] {
        if (this.dictionary[key] == undefined) {
            if (MESSAGES[key] == undefined) {
                throw new Error(`Messages : key : ${key} is not dedined`);
            }
            let value : string[] = [];
            for (let index = 1; index < MESSAGES[key]+1; index++) {
                value.push(key+ '.' + index)
            }
            this.dictionary[key] = value;
        }
        return this.dictionary[key];
    }

}