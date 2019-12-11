import { Component, Injector, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { Observable } from 'rxjs';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { Trace } from 'src/app/types/trace';
import { BlockProfilesService } from './block-profiles.service';
import { BlockProfile } from './BlockProfile';
import { MatSort } from '@angular/material/sort';
import { map } from 'rxjs/operators';

@Component({
    selector: 'ode-block-profiles',
    templateUrl: './block-profiles.component.html',
    styleUrls: ['./block-profiles.component.scss']
})
export class BlockProfilesComponent extends OdeComponent implements OnInit, OnDestroy {
    private structure: StructureModel;
    public blockProfiles: BlockProfile[] = [];
    public tracesDataSource: MatTableDataSource<Trace>;
    public tracesTableColumnsToDisplay = ['created', 'action', 'profile', 'ownerDisplayName'];

    @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
    @ViewChild(MatSort, {static: true}) sort: MatSort;

    constructor(injector: Injector,
                private blockProfilesService: BlockProfilesService,
                private notifyService: NotifyService) {
        super(injector);
    }

    ngOnInit(): void {
        this.blockProfiles = [
            {profile: 'Personnel', block: false},
            {profile: 'Teacher', block: false},
            {profile: 'Relative', block: false},
            {profile: 'Student', block: false},
            {profile: 'Guest', block: false}
        ];
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.initDataSource();
            }
        }));
    }

    initDataSource() {
        this.tracesDataSource = new MatTableDataSource();
        this.getTraces().subscribe(data => this.tracesDataSource.data = data);
        this.tracesDataSource.sort = this.sort;
        this.tracesDataSource.paginator = this.paginator;
    }

    block(profile: BlockProfile): void {
        profile.block = true;
        this.blockProfilesService
            .update(this.structure.id, profile)
            .subscribe(
                () => {
                    // update users in structure (because of cache)
                    this.structure.users.data.filter(user => user.type === profile.profile).forEach(u => u.blocked = true);
                    this.notifyService.success(
                        {key: 'management.block.profile.block.success.content', parameters: {profile: profile.profile}}
                        , 'management.block.profile.block.success.title');
                    this.getTraces().subscribe(data => this.tracesDataSource.data = data);
                },
                error => this.notifyService.error(
                    {key: 'management.block.profile.block.error.content', parameters: {profile: profile.profile}}, 
                    'management.block.profile.block.error.title'));
    }

    unblock(profile: BlockProfile): void {
        profile.block = false;
        this.blockProfilesService
            .update(this.structure.id, profile)
            .subscribe(
                () => {
                    // update users in structure (because of cache)
                    this.structure.users.data.filter(user => user.type === profile.profile).forEach(u => u.blocked = false);
                    this.notifyService.success(
                        {key: 'management.block.profile.unblock.success.content', parameters: {profile: profile.profile}}
                        , 'management.block.profile.unblock.success.title');
                    this.getTraces().subscribe(data => this.tracesDataSource.data = data);
                },
                error => this.notifyService.error(
                    {key: 'management.block.profile.unblock.error.content', parameters: {profile: profile.profile}}, 
                    'management.block.profile.unblock.error.title'));
    }

    getTraces(): Observable<Trace[]> {
        return this.blockProfilesService
            .getTraces(this.structure.id)
            .pipe(
                map(tracesResponse => tracesResponse.map(traceResponse => {
                    const trace: Trace = {
                        id: traceResponse._id,
                        action: traceResponse.action,
                        profile: traceResponse.profile,
                        structureId: traceResponse.structureId,
                        created: traceResponse.created.$date,
                        modified: traceResponse.modified.$date,
                        ownerId: traceResponse.owner.userId,
                        ownerDisplayName: traceResponse.owner.displayName
                    };
                    return trace;
                }))
            );
    }
}
