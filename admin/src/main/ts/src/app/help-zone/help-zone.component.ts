import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, NgZone, OnDestroy, OnInit, Output } from '@angular/core';

/**
 * Edifice-branded Zendesk launcher, same behaviour as @edifice.io/react's HelpZone:
 * - the logo links to the Edifice release notes, the "?" toggles the widget;
 * - the wordmark collapses to its mark on the first scroll and stays compact;
 * - the whole zone fades out while the widget is open.
 * Rendered only when the `layout.helpzone` uiOverride is enabled (see ZendeskGuideService).
 */
@Component({
    selector: 'ode-help-zone',
    templateUrl: './help-zone.component.html',
    styleUrls: ['./help-zone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HelpZoneComponent implements OnInit, OnDestroy {
    /** True while the widget panel is open. */
    @Input() isOpen = false;
    @Output() widgetToggle = new EventEmitter<void>();

    public isCompact = false;

    constructor(private readonly zone: NgZone,
                private readonly changeDetector: ChangeDetectorRef) {
    }

    ngOnInit() {
        // Some layouts scroll an inner container instead of the window; scroll
        // events don't bubble, but a capture-phase listener still sees them.
        // Listen outside Angular so scrolling doesn't trigger change detection.
        this.zone.runOutsideAngular(() => {
            document.addEventListener('scroll', this.goCompact, true);
        });
    }

    ngOnDestroy() {
        document.removeEventListener('scroll', this.goCompact, true);
    }

    // Once compact, the zone never expands back (even back at the top)
    private goCompact = () => {
        document.removeEventListener('scroll', this.goCompact, true);
        this.zone.run(() => {
            this.isCompact = true;
            this.changeDetector.markForCheck();
        });
    }
}
