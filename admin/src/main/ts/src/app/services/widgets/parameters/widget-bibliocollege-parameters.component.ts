import { ChangeDetectorRef, Component, Injector } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { OdeComponent } from 'ngx-ode-core';
import { NotifyService } from 'src/app/core/services/notify.service';
import { ServicesStore } from '../../services.store';
import { routing } from 'src/app/core/services/routing.service';
import { Data } from '@angular/router';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { BibliocollegeFeed } from './bibliocollege-channels.types';
import { Channel } from './bibliocollege-channels.types';
import http from 'axios';

const URL_PATTERN = /^https?:\/\/.+/i;

/**
 * Bibliocollege "Paramétrage spécifique" – on load:
 * 1. GET api/platform/config for optional params rss-bibliocollege-flux, rss-bibliocollege-title, rss-bibliocollege-show
 *    (shown as one read-only entry if present).
 * 2. ChannelsResolver: GET rss/channels/structure/:structureId; if no channel, POST rss/channel with
 *    { feeds: [], structureID } to create one. We always get a channel with _id.
 * Add/update/delete: PUT rss/channel/:channelId with { feeds } (full array; for delete, remaining feeds).
 */
/** Platform conf keys for the single read-only bibliocollege feed. */
const CONF_KEYS = {
    link: 'rss-bibliocollege-flux',
    title: 'rss-bibliocollege-title',
    show: 'rss-bibliocollege-show'
} as const;

const SHOW_OPTIONS: { value: 3 | 5; labelKey: string }[] = [
    { value: 3, labelKey: 'services.widget.bibliocollege.feed.show.3' },
    { value: 5, labelKey: 'services.widget.bibliocollege.feed.show.5' }
];

@Component({
    selector: 'ode-widget-bibliocollege-parameters',
    templateUrl: './widget-bibliocollege-parameters.component.html',
    styleUrls: ['./widget-bibliocollege-parameters.component.scss']
})
export class WidgetBibliocollegeParametersComponent extends OdeComponent {

    structure: StructureModel;
    /** First channel for the structure (from API). */
    channel: Channel | null = null;
    /** Working copy of feeds; synced from channel on load, saved to API on update. */
    feeds: BibliocollegeFeed[] = [];
    /** Read-only feed from platform conf (only when widget is bibliocollege-widget and conf has keys). */
    confFeed: BibliocollegeFeed | null = null;

    showRssLightbox = false;
    editingIndex: number | null = null;

    readonly showOptions = SHOW_OPTIONS;

    rssForm = new FormGroup({
        link: new FormControl('', [Validators.required, Validators.pattern(URL_PATTERN)]),
        title: new FormControl('', [Validators.required]),
        show: new FormControl<number>(3, [Validators.required])
    });

    constructor(
        injector: Injector,
        public servicesStore: ServicesStore,
        private notifyService: NotifyService,
        private cdr: ChangeDetectorRef
    ) {
        super(injector);
    }

    get linkControl(): FormControl { return this.rssForm.get('link') as FormControl; }
    get titleControl(): FormControl { return this.rssForm.get('title') as FormControl; }
    get showControl(): FormControl { return this.rssForm.get('show') as FormControl; }
    get isEditing(): boolean { return this.editingIndex !== null; }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
            }
            if (data.channels && Array.isArray(data.channels) && data.channels.length > 0) {
                this.channel = data.channels[0];
                this.feeds = (this.channel.feeds || []).map(f => ({ ...f }));
            } else {
                this.channel = null;
                this.feeds = [];
            }
        }));
        this.loadPlatformConfig();
    }

    /** Fetch platform config on load of bibliocollege-params for optional conf entry. */
    private loadPlatformConfig(): void {
        http.get<Record<string, unknown>>('/admin/api/platform/config')
            .then(res => {
                this.updateConfFeed(res?.data);
                this.cdr.markForCheck();
            })
            .catch(() => {
                this.updateConfFeed(undefined);
                this.cdr.markForCheck();
            });
    }

    private updateConfFeed(config: Record<string, unknown> | undefined): void {
        const structure = this.structure || this.servicesStore.structure;
        const widgetId = routing.getParam(this.route.snapshot, 'widgetId');
        const widget = structure?.widgets?.data?.find(w => w.id === widgetId);
        const name = (widget?.name || widget?.displayName || '').trim().toLowerCase();
        const isBibliocollege = ['bibliocollege-widget', 'bibliocollege', 'biblicollege-widget', 'biblicollege'].includes(name);
        if (!widget || !isBibliocollege || !config) {
            this.confFeed = null;
            return;
        }
        const link = config[CONF_KEYS.link];
        const title = config[CONF_KEYS.title];
        const showRaw = config[CONF_KEYS.show];
        if (link == null || link === '') {
            this.confFeed = null;
            return;
        }
        const showNum = showRaw === 5 || showRaw === '5' ? 5 : 3;
        this.confFeed = {
            link: String(link),
            title: title != null ? String(title) : '',
            show: showNum,
            fromConf: true
        };
    }

    openAdd(): void {
        this.editingIndex = null;
        this.rssForm.reset({ link: '', title: '', show: 3 });
        this.showRssLightbox = true;
    }

    openEdit(index: number): void {
        const feed = this.feeds[index];
        this.editingIndex = index;
        this.rssForm.setValue({
            link: feed.link || '',
            title: feed.title || '',
            show: feed.show ?? 3
        });
        this.showRssLightbox = true;
    }

    closeLightbox(): void {
        this.showRssLightbox = false;
        this.editingIndex = null;
        this.rssForm.reset({ link: '', title: '', show: 3 });
    }

    saveRssFeed(): void {
        if (this.rssForm.invalid) {
            this.rssForm.markAllAsTouched();
            return;
        }
        const link = (this.linkControl.value || '').trim();
        const title = (this.titleControl.value || '').trim();
        const showVal = this.showControl.value;
        const show = (showVal === 5 || showVal === '5') ? 5 : 3;

        const newFeed: BibliocollegeFeed = { link, title, show };

        if (this.editingIndex !== null) {
            this.feeds[this.editingIndex] = newFeed;
            this.notifyService.success(
                'services.widget.bibliocollege.rss.updated',
                'services.widget.bibliocollege.rss.updated.title'
            );
        } else {
            this.feeds.push(newFeed);
            this.notifyService.success(
                'services.widget.bibliocollege.rss.added',
                'services.widget.bibliocollege.rss.added.title'
            );
        }
        this.saveChannels();
        this.closeLightbox();
    }

    deleteRssFeed(index: number): void {
        this.feeds.splice(index, 1);
        this.notifyService.success(
            'services.widget.bibliocollege.rss.deleted',
            'services.widget.bibliocollege.rss.deleted.title'
        );
        this.saveChannels();
    }

    private get channelId(): string {
        return this.channel?._id || '';
    }

    private saveChannels(): void {
        const channelId = this.channelId;
        if (!channelId) return;

        const payload = { feeds: this.feeds };

        http.put(`/rss/channel/${channelId}`, payload)
            .then(() => {})
            .catch(() => {
                this.notifyService.error(
                    'services.widget.bibliocollege.rss.save.error',
                    'services.widget.bibliocollege.rss.save.error.title'
                );
            });
    }
}
