import { Directive,  Input, ViewContainerRef, ComponentFactoryResolver, ComponentRef,} from '@angular/core';
import { ComponentDescriptor } from './component-descriptor.model'

/**
* Dynamically load the component describe with the @input dynamic-component
*/
@Directive({
    selector: '[dynamic-component]',
})
export class DynamicComponent {
    constructor(
        private vcr: ViewContainerRef,
        private cfr: ComponentFactoryResolver
    ) {}

    private componentRef:ComponentRef<any>;

    @Input("dynamic-component") 
    componentDesc: ComponentDescriptor;

    load(data?:any):void {
        let componentFactory = this.cfr.resolveComponentFactory(this.componentDesc.type);
        this.vcr.clear();
        this.componentRef = this.vcr.createComponent(componentFactory);
        
        if (data != undefined) {
            this.componentDesc.assignData(data);
        }
        for (let attribute in this.componentDesc.data) {
            this.componentRef.instance[attribute] = this.componentDesc.data[attribute];
        } 
    }

    isLoaded():boolean {
        if (this.componentRef == undefined)
            return false
        else
            return this.componentRef.instance != null
    }

    destroy():void {
        this.componentRef.destroy();
        this.vcr.clear();
    }
}



