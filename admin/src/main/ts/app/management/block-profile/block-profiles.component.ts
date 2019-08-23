import { Component, OnInit, OnDestroy } from "@angular/core";
import { Subscription } from "rxjs";
import { ActivatedRoute, Data } from "@angular/router";
import { BlockProfileModel } from "../../core/store/models/blockprofile.model";
import { BlockProfilesService } from "./block-profiles.service";
import { NotifyService, routing } from "../../core/services";
import { StructureModel } from "../../core/store";

@Component({
    selector: 'block-profiles',
    template: `
        <div class="block-profiles container">
            <h2>
                <s5l>management.block.profile.header</s5l>
            </h2>
            <p>
                <s5l>management.block.profile.info.1</s5l>
                <a routerLink="../../users/filter" [queryParams]="{blocked: true}">
                    <s5l>management.block.profile.info.2</s5l>
                </a>
            </p>
            <p class="block-profiles-warning">
                <i class="block-profiles-warning_icon fa fa-exclamation-circle"></i>
                <span class="block-profiles-warning_text"><s5l>management.block.profile.warning</s5l></span>
            </p>

            <table class="block-profiles-table">
                <tr class="block-profiles-table-line" *ngFor="let profile of blockProfiles">
                    <td class="block-profiles-table-profileCell">{{ profile.profile | translate }}</td>
                    <td><button type="button" (click)="block(profile)"><s5l>block</s5l></button></td>
                    <td><button type="button" (click)="unblock(profile)"><s5l>unblock</s5l></button></td>
                </tr>
            </table>
        </div>
    `,
    styles: [`
        .block-profiles-table {width: auto;}
        .block-profiles-table-line:hover {background-color: unset;}
        .block-profiles-table-profileCell {min-width: 120px;}
        .block-profiles-warning_icon {color: indianred; font-size: 1.5em;}
        .block-profiles-warning_text {font-weight: bold;}
    `]
})
export class BlockProfilesComponent implements OnInit, OnDestroy {
    public blockProfiles: BlockProfileModel[] = [];
    private structure: StructureModel;
    private routeSubscription: Subscription = new Subscription();

    constructor(private activatedRoute: ActivatedRoute,
        private blockProfilesService: BlockProfilesService,
        private notifyService: NotifyService) {}

    ngOnInit(): void {
        this.blockProfiles = [
            {profile: 'Personnel', block: false},
            {profile: 'Teacher', block: false},
            {profile: 'Relative', block: false},
            {profile: 'Student', block: false},
            {profile: 'Guest', block: false}
        ];
        this.routeSubscription = routing.observe(this.activatedRoute, "data").subscribe((data: Data) => {
            if (data['structure']) {
                this.structure = data['structure'];
            }
        });
    }

    ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
    }

    block(profile: BlockProfileModel): void {
        profile.block = true;
        this.blockProfilesService
            .update(this.structure.id, profile)
            .subscribe(
                () => {
                    // update users in structure (because of cache)
                    this.structure.users.data.filter(user => user.type === profile.profile).forEach(u => u.blocked = true);
                    this.notifyService.success(
                        {key: 'management.block.profile.block.success.content', parameters: {profile: profile.profile}}
                        , 'management.block.profile.block.success.title')
                }, 
                error => this.notifyService.error(
                    {key: 'management.block.profile.block.error.content', parameters: {profile: profile.profile}}, 
                    'management.block.profile.block.error.title'));
    }

    unblock(profile: BlockProfileModel): void {
        profile.block = false;
        this.blockProfilesService
            .update(this.structure.id, profile)
            .subscribe(
                () => {
                    // update users in structure (because of cache)
                    this.structure.users.data.filter(user => user.type === profile.profile).forEach(u => u.blocked = false);
                    this.notifyService.success(
                        {key: 'management.block.profile.unblock.success.content', parameters: {profile: profile.profile}}
                        , 'management.block.profile.unblock.success.title')
                }, 
                error => this.notifyService.error(
                    {key: 'management.block.profile.unblock.error.content', parameters: {profile: profile.profile}}, 
                    'management.block.profile.unblock.error.title'));
    }
}
