import { Component, Input, OnInit, ChangeDetectorRef } from "@angular/core";
import { AbstractSection } from "../abstract.section";
import { HttpClient } from '@angular/common/http';
import { Quota } from "./Quota";
import { UserModel } from "../../../../core/store";
import { MaxQuotas } from "./MaxQuotas";
import { Unit } from "./Unit";
import { NumberRepresentation } from "./NumberRepresentation";
import { NotifyService } from "../../../../core/services";

@Component({
    selector: 'user-quota-section',
    template: `
        <panel-section section-title="users.details.section.quota">
            <div class="quota-container">
                <div class="quota-data">
                    <div class="quota-data-number">                
                        {{ round2digits(storage.value) }} / {{ floor2digits(quota.value) }} {{ quota.unit.label | translate }} {{ "quota.usedSpace" | translate }}
                    </div>
                    
                    <div class="quota-data-bar">
                        <div [ngStyle]="{ 'flex-basis': storageRatio + '%' }"></div>
                    </div>
                </div>

                <div class="quota-form">
                    <div class="quota-form-title">
                        <s5l>quota.form.title</s5l>
                    </div>
                    <div class="quota-form-body">
                        <form #quotaForm="ngForm">
                            <select name="selectQuotaUnitValue"
                                [(ngModel)]="selectQuotaUnitValue"
                                (change)="inputQuotaValue = inputQuotaValueInBytes / selectQuotaUnitValue">
                                <option *ngFor="let unit of units" [value]="unit.value">
                                    {{ unit.label | translate }}
                                </option>
                            </select>

                            <input type="number" 
                                name="inputQuotaValue"
                                [(ngModel)]="inputQuotaValue"
                                (change)="inputQuotaValueInBytes = inputQuotaValue * selectQuotaUnitValue" />

                            <button (click)="saveQuota()" 
                                [disabled]="quotaForm.pristine || quotaForm.invalid || isQuotaInferiorToStorage() || isQuotaSuperiorToMaxQuota()">
                                <s5l>save.modifications</s5l>
                                <i class="fa fa-floppy-o"></i>
                            </button>
                        </form>
                    </div>
                    <div class="quota-form-footer">
                        <em><s5l>quota.maxQuota</s5l> {{ floor2digits(maxQuota.value) }} {{ maxQuota.unit.label | translate }}</em>
                    </div>
                </div>
            </div>
        </panel-section>
    `
})
export class UserQuotaSection extends AbstractSection implements OnInit {
    @Input() user: UserModel;

    /** user storage and quota display. */
    storage: NumberRepresentation = {"value": 0, unit: {"label": "", "value": 0}};
    quota: NumberRepresentation = {"value": 0, unit: {"label": "", "value": 0}};
    unit: Unit;
    
    /** ratio for quota bar. */
    storageRatio: number;

    /** user storage and quota form inputs. */
    selectQuotaUnitValue: number;
    inputQuotaValue: number;
    inputQuotaValueInBytes: number;
    storageInBytes: number;

    /** maxQuota per profile. */
    maxQuotas: MaxQuotas[] = [];
    maxQuota: NumberRepresentation = {"value": 0, unit: {"label": "", "value": 0}};
    maxQuotaInBytes: number;
    
    units: Array<Unit> = [
        { label: "quota.byte", value: 1 },
        { label: "quota.kilobyte", value: 1024 },
        { label: "quota.megabyte", value: 1048576 },
        { label: "quota.gigabyte", value: 1073741824 }
    ];
    
    constructor(private http: HttpClient, 
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super();
    }

    ngOnInit(): void {
        this.http.get<MaxQuotas[]>('/workspace/quota/default').subscribe((data: MaxQuotas[]) => {
            this.maxQuotas = data;
        });
    }

    protected onUserChange(): void {
        if(!this.details.storage && this.details.storage != 0) {
            this.http.get<Quota>('/workspace/quota/user/' + this.user.id).subscribe((data: Quota) => {
                this.initQuota(data);
                // save values in user to prevent fetching data again
                this.details.storage = data.storage;
                this.details.quota = data.quota;
                this.details.maxQuota = this.maxQuotaInBytes;
            });
        } else {
            this.initQuota({storage: this.details.storage, quota: this.details.quota});
        }
    }

    private initQuota(data: Quota) {
        this.quota = this.getNumberRepresentation(data.quota);
        this.unit = this.quota.unit;
        this.storage = {
            "value": this.calcStorage(data.storage, this.unit),
            "unit": this.unit
        };
        this.storageInBytes = data.storage;

        this.inputQuotaValue = this.quota.value;
        this.inputQuotaValueInBytes = data.quota;

        this.selectQuotaUnitValue = this.quota.unit.value;
        this.storageRatio = this.getStorageRatio(data.storage, data.quota);

        this.maxQuota = this.getMaxUserQuota();
        this.maxQuotaInBytes = this.maxQuota.value * this.maxQuota.unit.value;
    }

    private getNumberRepresentation(bytes: number): NumberRepresentation {
        let unit = 0;
        let finalValue = bytes;
        while(finalValue >= 1024 && unit < 3) {
            finalValue = finalValue / 1024;
            unit++;
        }
        return { value: finalValue, unit: this.units[unit] };
    }

    private calcStorage(storage: number, quotaUnit: Unit): number {
        return storage / quotaUnit.value;
    }

    floor2digits(nb: number): number {
        return (Math.floor(nb * 100) / 100);
    }

    round2digits(nb: number): number {
        return (Math.round(nb * 100) / 100);
    }

    private getStorageRatio(storage: number, quota: number): number {
        return Math.min(100, Math.round((storage * 100) / quota * 100) / 100);
    }

    saveQuota(): void {
        this.http.put('/workspace/quota', {
            users: [this.details.id],
            quota: Math.floor(this.inputQuotaValueInBytes)
        }).subscribe(
            data => {
                // update storage, quota and ratio display
                this.quota = this.getNumberRepresentation(this.inputQuotaValueInBytes);
                this.storage = {value: this.calcStorage(this.storageInBytes, this.quota.unit), unit: this.quota.unit};
                this.storageRatio = this.getStorageRatio(this.storageInBytes, this.inputQuotaValueInBytes);
                // update quota in user details
                this.details.quota = this.inputQuotaValueInBytes;

                this.ns.success(
                    { 
                        key: 'notify.user.quota.update.content', 
                        parameters: {
                            user: this.details.firstName + ' ' + this.user.lastName 
                        } 
                    }, 'notify.user.quota.update.title');
                this.cdRef.markForCheck();
            },
            error => {
                this.ns.error(
                    {
                        key: 'notify.user.quota.update.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName
                        }
                    }, 'notify.user.quota.update.error.title', error);
            }
        );
    }

    getMaxUserQuota(): NumberRepresentation {
        let maxQuotaObj = this.maxQuotas.find(maxQuota => maxQuota.name == this.user.type);
        return this.getNumberRepresentation(maxQuotaObj.maxQuota);
    }

    isQuotaInferiorToStorage(): boolean {
        return (this.inputQuotaValue * this.selectQuotaUnitValue) < this.storageInBytes;
    }

    isQuotaSuperiorToMaxQuota(): boolean {
        return (this.inputQuotaValue * this.selectQuotaUnitValue) > this.maxQuotaInBytes;
    }
}