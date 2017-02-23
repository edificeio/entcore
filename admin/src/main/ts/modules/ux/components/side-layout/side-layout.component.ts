import { Component, Input, Output, EventEmitter } from '@angular/core'

@Component({
    selector: 'side-layout',
    template: `
        <div class="side-layout">
            <div class="side-card">
                <ng-content select="[side-card]"></ng-content>
            </div>
            <div class="side-companion" *ngIf="showCompanion">
                <ng-content select="[side-companion]"></ng-content>
                <i class="fa fa-times action top-right" (click)="close.emit()"></i>
            </div>
        </div>
    `,
    styles: [`
        div.side-layout {
            display: flex;
            flex-wrap: nowrap;
            align-items: flex-start;
        }

        div.side-card {
            flex: 400px 0 0;
            position: relative;
        }

        div.side-companion {
            flex: 1;
            position: relative;
            margin-left: 25px;
        }
    `]
})
export class SideLayout {
    @Input() showCompanion : boolean = false
    @Output("closeCompanion") close : EventEmitter<void> = new EventEmitter<void>()
}