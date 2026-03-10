import { HttpClient } from "@angular/common/http";
import { ChangeDetectorRef, Component, Injector } from "@angular/core";
import { Data, Params } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { SpinnerService } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { routing } from "src/app/core/services/routing.service";
import { GroupModel } from "src/app/core/store/models/group.model";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { ServicesStore } from "../../services.store";
import { filterWidgetsByLevelsOfEducation } from "../../_shared/services-list/services-list.component";
import { Assignment } from "../../_shared/services-types";
import { SessionModel } from 'src/app/core/store/models/session.model';
import { Session } from 'src/app/core/store/mappings/session';

type SmartWidgetTab = 'assignment' | 'massAssignment' | 'myappsParameters';

@Component({
    selector: 'ode-smart-widget',
    templateUrl: 'smart-widget-component.html',
    styles: [`
        .has-bottom-margin-30 {
            margin-bottom: 30px;
        }
    `]
})
export class SmartWidgetComponent extends OdeComponent {
    private _currentTab:SmartWidgetTab  = 'assignment';
    public get currentTab():SmartWidgetTab {
        return this._currentTab;
    }
    public set currentTab( newTab:SmartWidgetTab ) {
        this._currentTab = newTab;
        if( newTab==='myappsParameters' ) {
            const widgetName = this.servicesStore.widget?.name || this.servicesStore.widget?.displayName || '';
            if (widgetName === 'my-apps') {
                this.router.navigate( ['myapps-params'], {relativeTo: this.route});
            } else if (this.isRssChannelsParamsWidget(widgetName)) {
                const type = this.getRssChannelsType(widgetName);
                this.router.navigate( ['bibliocollege-params'], { relativeTo: this.route, queryParams: { type } });
            }
        } else {
            this.router.navigate( ['.'], {relativeTo: this.route});
        }
    }

    public assignmentGroupPickerList: Array<GroupModel>;
    public currentStructure:StructureModel;

    public currentWidgetLevel:string = '';

    private session: Session;
    public showTab:boolean = true;

    constructor(
        injector: Injector,
        public servicesStore: ServicesStore,
        private httpClient: HttpClient,
        private notifyService: NotifyService,
        private spinnerService: SpinnerService) {
        super(injector);
    }

    private get cdr(): ChangeDetectorRef {
        return this.injector.get(ChangeDetectorRef);
    }

    async ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.route.params.subscribe((params: Params) => {
            if (params['widgetId']) {
                this.servicesStore.widget = this.servicesStore.structure.widgets.data.find(a => a.id === params['widgetId']);
                this.checkStructureLevelOfEducation();
                const childPath = this.route.snapshot.firstChild?.routeConfig?.path;
                if (childPath === 'myapps-params' || childPath === 'bibliocollege-params') {
                    this._currentTab = 'myappsParameters';
                } else {
                    this._currentTab = 'assignment';
                }
            }
        }));

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.currentStructure = data.structure;
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
                this.checkStructureLevelOfEducation();
                this.showMassAssignmentTab(this.currentStructure);
                const childPath = this.route.snapshot.firstChild?.routeConfig?.path;
                if (childPath !== 'myapps-params' && childPath !== 'bibliocollege-params') {
                    this._currentTab = 'assignment';
                }
                this.cdr.markForCheck();
            }
        }));

        this.session = await SessionModel.getSession();
        this.currentTab = 'assignment';
        this.cdr.markForCheck();
    }

    private showMassAssignmentTab(structure):void {        
        if (structure.children && 
            structure.children.length > 0) {
            this.showTab = true;
        } else {
            this.showTab = false;
        }
    }

    private checkStructureLevelOfEducation(): void {
        if( this.currentStructure 
                && this.servicesStore.widget.levelsOfEducation
                && this.servicesStore.widget.levelsOfEducation.length
                && filterWidgetsByLevelsOfEducation([this.servicesStore.widget], this.currentStructure.levelsOfEducation).length === 0
            ) {
            this.currentWidgetLevel = ''+this.servicesStore.widget.levelsOfEducation[0]+'D';
        } else {
            this.currentWidgetLevel = '';
        }
    }

    /** True when the "Paramétrage spécifique" tab should be shown. Conditions depend on the widget. */
    public get canEditMyAppsParameters():boolean {
        const widget = this.servicesStore.widget;
        const structure = this.currentStructure || this.servicesStore.structure;
        if (!widget || !structure) {
            return false;
        }
        const hasRights = this.isAdmc() || this.isAdml(structure.id);
        const widgetName = (widget.name || widget.displayName || '').trim();

        if (widgetName === 'my-apps') {
            return this.is2D && hasRights;
        }
        if (this.isRssChannelsParamsWidget(widgetName)) {
            return hasRights;
        }
        return false;
    }

    /** Matches bibliocollege / biblicollege widget (with one or two 'l'). */
    private isBibliocollegeWidget(name: string): boolean {
        if (!name) return false;
        const n = name.trim().toLowerCase();
        return n === 'biblicollege-widget';
    }

    /** Widgets that use the RSS/channels "Paramétrage spécifique" (bibliocollege-params). */
    private isRssChannelsParamsWidget(name: string): boolean {
        if (!name) return false;
        const n = name.trim().toLowerCase();
        return this.isBibliocollegeWidget(name) || n === 'e-sidoc-widget';
    }

    /** Query param type for GET /rss/channels/structure/:id (e-sidoc-widget -> e-sidoc, bibliocollege -> bibliocollege). */
    private getRssChannelsType(widgetName: string): string {
        const n = (widgetName || '').trim().toLowerCase();
        if ( n === 'e-sidoc-widget') return 'e-sidoc';
        return 'bibliocollege';
    }

    private get is2D() {
        const structure = this.currentStructure || this.servicesStore.structure;
        return structure?.levelsOfEducation?.length && structure.levelsOfEducation.indexOf(2) >= 0;
    }

    private isAdmc() {
        return this.session && this.session.isADMC();
    }

    private isAdml( structureId:string ) {
        if (this.session && this.session.functions && this.session.functions["ADMIN_LOCAL"]) {            
            const { scope } = this.session.functions["ADMIN_LOCAL"];
            return scope.includes( structureId );
        }
        return false;
    }

    public async handleMandatoryToggle(assignment: Assignment): Promise<void> {
        // toggle to not mandatory
        if (assignment.group.mandatory) {
            this.spinnerService.perform(
                'portal-content',
                this.httpClient.delete(`/appregistry/widget/${this.servicesStore.widget.id}/mandatory/${assignment.group.id}`, {}).
                toPromise().
                then(() => {
                    assignment.group.mandatory = false;
                    this.notifyService.success(
                        {
                            key: 'notify.services.widgets.notmandatory.success.content', 
                            parameters: {groupName: assignment.group.name}
                        }, 
                        'notify.services.widgets.notmandatory.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.widgets.notmandatory.error.content',
                            parameters: {groupName: assignment.group.name}
                        }, 
                        'notify.services.widgets.notmandatory.error.title',
                        err
                    );
                })
            );
        } else { // toggle to mandatory
            this.spinnerService.perform(
                'portal-content',
                this.httpClient.put(`/appregistry/widget/${this.servicesStore.widget.id}/mandatory/${assignment.group.id}`, {}).
                toPromise().
                then(() => {
                    assignment.group.mandatory = true;
                    this.notifyService.success(
                        {
                            key: 'notify.services.widgets.mandatory.success.content', 
                            parameters: {groupName: assignment.group.name}
                        }, 
                        'notify.services.widgets.mandatory.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.widgets.mandatory.error.content',
                            parameters: {groupName: assignment.group.name}
                        }, 
                        'notify.services.widgets.mandatory.error.title',
                        err
                    );
                })
            );
        }
    }

    public async onAddAssignment(assignment: Assignment): Promise<void> {
        this.spinnerService.perform(
            'portal-content', 
            this.httpClient.post(`/appregistry/widget/${this.servicesStore.widget.id}/link/${assignment.group.id}`, {}).
                toPromise().
                then(() => {
                    assignment.role.groups.push(assignment.group);
                    this.notifyService.success(
                        {
                            key: 'notify.services.assignment.role.added.success.content', 
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.added.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.assignment.role.added.error.content',
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.added.error.title',
                        err
                    );
                }));
    }

    public async onRemoveAssignment(assignment: Assignment): Promise<void> {
        this.spinnerService.perform(
            'portal-content', 
            this.httpClient.delete(`/appregistry/widget/${this.servicesStore.widget.id}/link/${assignment.group.id}`).
                toPromise().
                then(() => {
                    const groupIndex = assignment.role.groups.findIndex(g => g.id == assignment.group.id);
                    assignment.role.groups.splice(groupIndex, 1);
                    this.notifyService.success(
                        {
                            key: 'notify.services.assignment.role.deleted.success.content', 
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.deleted.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.assignment.role.deleted.error.content',
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.deleted.error.title',
                        err
                    );
                }));
    }

    public onMassChange(): void {
        this.servicesStore.widget.syncRoles(this.servicesStore.structure.id);
    }
}