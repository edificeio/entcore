export class Eventer{
    private events: Map<string, ((data?: any) => void)[]>;

    constructor(){
        this.events = new Map<string, ((data?: any) => void)[]>();
    }

    trigger(eventName: string, data?: any): void{
        if(this.events[eventName]){
            this.events[eventName](data);
        }
    }

    on(eventName: string, cb: (data?: any) => void): void {
        if(!this.events[eventName]){
            this.events[eventName] = [];
        }

        this.events[eventName].push(cb);
    }

    off(eventName: string, cb?: (data?: any) => void): void {
        if(!this.events[eventName]){
            return;
        }

        if(cb === undefined){
            this.events[eventName] = [];
            return;
        }

        let index = this.events[eventName].indexOf(cb);
        if(index !== -1){
            this.events[eventName].splice(index, 1);
        }
    }

    once(eventName: string, cb: (data?: any) => void): void {
        let callback = (data) => {
            cb(data);
            this.off(eventName, callback);
        };

        this.on(eventName, callback);
    }
}