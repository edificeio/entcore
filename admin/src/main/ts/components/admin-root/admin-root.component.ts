import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit } from '@angular/core'

@Component({
    selector: 'admin-app',
    templateUrl: require('./admin-root.component.html'),//'/admin/public/templates/admin-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminRoot implements OnInit {

    constructor(private _cdRef: ChangeDetectorRef){}

    ngOnInit() {}

}
