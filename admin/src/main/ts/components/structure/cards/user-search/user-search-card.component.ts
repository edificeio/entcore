import { Component, Input, AfterViewInit, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { StructureModel } from '../../../../models'

@Component({
    selector: 'user-search-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-user"></i>
                <s5l>search.user</s5l>
            </span>
        </div>
        <div class="card-body relative">
            <search-input
                [delay]="500"
                [attr.placeholder]="'search.user' | translate"
                (onChange)="inputValue = $event"></search-input>
            <!-- position hack ... -->
                <i class="fa fa-spinner fa-pulse fa-2x fa-fw"
                    *ngIf="loading"
                    style="position: absolute; top: 35px; right: -20px;"></i>
            <div class="card-list">
                <div class="card-big-margin" *ngIf="!foundUsers || foundUsers.length === 0">
                    <em *ngIf="!inputValue">
                        <s5l>users.quick.search.intro</s5l>.
                    </em>
                    <em *ngIf="inputValue">
                        <s5l>no.user.found</s5l>.
                    </em>
                </div>
                <ul *ngIf="foundUsers && foundUsers.length > 0">
                    <li *ngFor="let user of foundUsers">
                        <a [routerLink]="['users', user.id]">
                            {{ user.lastName | uppercase }} {{ user.firstName }}
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserSearchCard implements AfterViewInit {

    constructor(private cdRef: ChangeDetectorRef){}

    @Input() structure: StructureModel
    private _inputValue: string
    get inputValue(){ return this._inputValue }
    set inputValue(value) {
        this._inputValue = value
        if(this._inputValue && !this.loading) {
            this.loading = true
            this.structure.quickSearchUsers(this._inputValue).then(res => {
                this.foundUsers = res.data
            }).catch(err => {
                console.error(err)
            }).then(() => {
                this.loading = false
                this.cdRef.markForCheck()
            })
        } else {
            this.foundUsers = []
        }
        this.cdRef.markForCheck()
    }
    private loading : boolean = false
    private foundUsers: Array<{id: string, firstName: string, lastName: string}> = []

    ngAfterViewInit() {
        this.cdRef.markForCheck()
        this.cdRef.detectChanges()
    }
}