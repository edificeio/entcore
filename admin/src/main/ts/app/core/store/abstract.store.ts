import { Subject } from 'rxjs/Subject'

export class AbstractStore {

    constructor(triggers: string[] | string) {
        const createTrigger = trigger => {
            this['_' + trigger] = this[trigger]
                delete this[trigger]
                Object.defineProperty(this, trigger, {
                    set: (x) => {
                        this['_' + trigger] = x
                        this.onchange.next(trigger)
                    },
                    get: () => {
                        return this['_' + trigger]
                    }
                })
        }

        if(triggers instanceof Array) {
            triggers.forEach(createTrigger)
        } else {
            createTrigger(triggers)
        }
    }

    onchange = new Subject<string>()
}