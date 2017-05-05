import { Component, ChangeDetectionStrategy, ChangeDetectorRef, AfterContentInit } from '@angular/core'

@Component({
    selector: 'admin-home',
    template: `
    <div>
        <h1><i class="fa fa-cog"></i><s5l>admin.title</s5l></h1>
        <h3><s5l>pick.a.structure</s5l></h3>
    </div>`,
     changeDetection: ChangeDetectionStrategy.OnPush
})
export class Home implements AfterContentInit {

    constructor(private cdRef: ChangeDetectorRef){}

    ngAfterContentInit(){
        this.cdRef.markForCheck()
    }

}