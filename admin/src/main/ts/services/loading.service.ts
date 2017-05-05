import { Injectable, ApplicationRef, ChangeDetectorRef } from '@angular/core'
import { Subject } from 'rxjs'

@Injectable()
export class LoadingService {

    constructor(private appRef: ApplicationRef){}

    timer = 250

    private loading = new Set<any>()
    private timers = new Map<any, number>()

    private _trigger = new Subject()
    get trigger() {
        return this._trigger
    }

    isLoading(something, pending = false) : boolean  {
        return this.loading.has(something) ||
            (pending && this.timers.has(something))
    }

    perform<T>(something, promise: Promise<T>, timer?: number) : Promise<T>{
        this.load(something, timer)
        return promise.then(_ => {
             this.done(something)
             return _
        })
    }

    load(something, timer?: number) : void {
        if(this.timers.has(something)){
            window.clearTimeout(this.timers.get(something))
        }

        let addToQueue = () => {
            this.loading.add(something)
            this.timers.delete(something)
            this.appRef.tick()
            this.trigger.next(true)
        }

        if(timer === 0){
             addToQueue()
        } else {
            this.timers.set(something, window.setTimeout(addToQueue, timer || this.timer))
        }
    }

    done(something) : void {
        window.clearTimeout(this.timers.get(something))
        this.timers.delete(something)
        this.loading.delete(something)
        this.trigger.next(true)
        this.appRef.tick()
    }

    wrap = (func: (...params) => Promise<any>, label: string,
            props : { delay?: number, binding?: any, cdRef?: ChangeDetectorRef } = {}, ...args) => {

        this.load(label, props.delay)
        let promise = props.binding ?
            func.bind(props.binding)(...args) :
            func(...args)

        promise.catch((err) => {
            console.error(err)
        }).then(() => {
            this.done(label)
            if(props.cdRef) props.cdRef.markForCheck()
        })
        if(props.cdRef) props.cdRef.markForCheck()

        return promise
    }

}