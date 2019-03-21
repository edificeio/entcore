import { Component, Input } from '@angular/core'

@Component({
    selector: 'panel-section',
    template: `
        <section class="panel-section">
            <div class="panel-section-header is-flex has-space-between" (click)="folded !== null ? folded=!folded : null" [class.foldable]="folded !== null">
                <div class="is-flex">
                    <s5l>{{ sectionTitle }}</s5l>
                    <div class="has-left-padding-10">
                        <ng-content select="[panel-section-header-icons]"></ng-content>
                    </div>
                </div>

                <i class="opener" *ngIf="folded !== null"
                    [class.opened]="!folded"></i>
            </div>
            <div class="panel-section-content" *ngIf="!folded">
                <ng-content></ng-content>
            </div>
        </section>
    `,
    styles: [`
        .panel-section {}
        .panel-section-header {
            font-size: 1.1em;
            padding: 10px 10px;
        }
        .panel-section-header.foldable {
            cursor: pointer;
        }
        .panel-section-content {
            padding: 15px;
        }
    `]
})
export class PanelSectionComponent {
    @Input("section-title") sectionTitle : string
    @Input() folded : boolean = null
}