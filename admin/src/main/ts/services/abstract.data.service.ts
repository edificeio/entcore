import { Subject } from 'rxjs'

export class AbstractDataService {

    constructor(triggers: string[]) {
       triggers.forEach(trigger => {
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
        })
    }

    onchange = new Subject<string>()
}