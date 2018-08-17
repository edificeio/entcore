import { Directive,  Input, ViewContainerRef, ComponentFactoryResolver, ComponentRef,} from '@angular/core';
import { ComponentDescriptor } from './component-descriptor.model'

/**
* Dynamically load the component describe with the @input dynamic-component
*/
@Directive({
    selector: '[dynamic-component]',
})
export class DynamicComponentDirective {
    constructor(
        private vcr: ViewContainerRef,
        private cfr: ComponentFactoryResolver
    ) {}

    private componentRef:ComponentRef<any>;

    @Input("dynamic-component") 
    componentDesc: ComponentDescriptor;

    load(data?:any):void {
        if (this.isLoaded()) return;
        let componentFactory = this.cfr.resolveComponentFactory(this.componentDesc.type);
        this.vcr.clear();
        this.componentRef = this.vcr.createComponent(componentFactory);
        
        if (data != undefined) {
            this.componentDesc.assignData(data);
        }
        for (let attribute in this.componentDesc.data) {
            this.componentRef.instance[attribute] = this.componentDesc.data[attribute];
            if (attribute == 'hideEvent')
                this.componentRef.instance.hideEvent.subscribe(e => {
                    this.destroy();
                });
        } 
    }

    isLoaded():boolean {
        if (this.componentRef == undefined)
            return false
        else
            return this.componentRef.instance != null
    }

    destroy():void {
        if (this.componentRef.instance.hideEvent != undefined)
            this.componentRef.instance.hideEvent.unsubscribe()
        // this.componentRef.destroy(); => This one usually should work, better to use but do nothing...
        delete this.componentRef;
        this.vcr.clear();
    }
}



