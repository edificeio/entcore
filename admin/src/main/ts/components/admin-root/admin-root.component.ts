import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit } from '@angular/core'

@Component({
    selector: 'admin-app',
    templateUrl: './admin-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminRoot implements OnInit {

    constructor(private _cdRef: ChangeDetectorRef){}

    ngOnInit() {}

}
