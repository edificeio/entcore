import { Component, Input, OnInit, ChangeDetectorRef } from "@angular/core";
import { AbstractSection } from "../abstract.section";
import { HttpClient } from '@angular/common/http';
import { UsedSpace } from "./UsedSpace";
import { UserModel } from "../../../../core/store";
import { MaxQuotas } from "./MaxQuotas";
import { Unit, getUnit, UNITS } from "../../../../shared/utils/math";
import { NotifyService } from "../../../../core/services";

@Component({
    selector: 'user-quota-section',
    template: `
        <panel-section section-title="users.details.section.quota">
            <div class="quota-container">
                <div *ngIf="isDataInitialized()">
                    <div class="quota-numbers">
                        {{ details.storage | bytes: unit.value:2 }} / {{ details.quota | bytes: unit.value }} {{ unit?.label | translate }} {{ "quota.usedSpace" | translate }}
                    </div>
                    
                    <div class="quota-bar">
                        <div class="quota-bar__used-space" 
                            [ngStyle]="{ 'flex-basis': storageRatio + '%' }">
                        </div>
                        <div class="quota-bar__unused-space">
                        </div>
                    </div>
                </div>

                <div class="quota-form">
                    <div class="quota-form-title">
                        <s5l>quota.form.title</s5l>
                    </div>
                    <div class="quota-form-body">
                        <form #quotaForm="ngForm">
                            <select name="newQuotaUnitValue"
                                [ngModel]="newQuotaUnitValue"
                                (ngModelChange)="refreshNewQuotaValue($event)">
                                <option *ngFor="let unit of units" [value]="unit.value">
                                    {{ unit?.label | translate }}
                                </option>
                            </select>

                            <input type="text" 
                                name="newQuotaValue"
                                [(ngModel)]="newQuotaValue" 
                                class="quota-form-body__input" />

                            <button (click)="saveQuota()" 
                                [disabled]="quotaForm.pristine || quotaForm.invalid || isQuotaInferiorToStorage() || isQuotaSuperiorToMaxQuota()">
                                <s5l>save.modifications</s5l>
                                <i class="fa fa-floppy-o"></i>
                            </button>
                        </form>
                    </div>
                    <div class="quota-form-footer" *ngIf="maxQuota">
                        <em><s5l>quota.maxQuota</s5l> {{ maxQuota | bytes: unit.value }} {{ unit?.label | translate }}</em>
                    </div>
                </div>
            </div>
        </panel-section>
    `
})
export class UserQuotaSection extends AbstractSection implements OnInit {
    @Input() user: UserModel;

    /** unit calculated from user quota */
    unit: Unit;

    units = UNITS;
    
    /** ratio for quota bar. */
    storageRatio: number;

    /** new quota form inputs. */
    newQuotaUnitValue: number;
    newQuotaValue: number;

    /** maxQuota per profile. */
    maxQuotas: MaxQuotas[] = [];
    maxQuota: number;
    
    constructor(private http: HttpClient, 
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super();
    }

    ngOnInit(): void {
        this.http.get<MaxQuotas[]>('/workspace/quota/default').subscribe((data: MaxQuotas[]) => {
            this.maxQuotas = data;
            this.maxQuota = this.getMaxUserQuota();
        });
    }

    protected onUserChange(): void {
        if(!this.details.storage && this.details.storage != 0) {
            this.http.get<UsedSpace>(`/workspace/quota/user/${this.user.id}`).subscribe((data: UsedSpace) => {
                this.details.storage = data.storage;
                this.details.quota = data.quota;
                this.initData();
            });
        } else {
            this.initData();
        }
    }

    initData(): void {
        this.unit = getUnit(this.details.quota);
        this.newQuotaUnitValue = this.unit.value;
        this.newQuotaValue = this.details.quota / this.newQuotaUnitValue;
        this.storageRatio = this.getStorageRatio(this.details.storage, this.details.quota);
        
        if(this.maxQuotas.length > 0) {
            this.maxQuota = this.getMaxUserQuota();
        }

        this.cdRef.markForCheck();
    }

    isDataInitialized(): boolean {
        return this.details.storage != null;
    }

    saveQuota(): void {
        let newQuotaValue = this.newQuotaValue * this.newQuotaUnitValue;

        this.http.put('/workspace/quota', {
            users: [this.details.id],
            quota: Math.floor(newQuotaValue)
        }).subscribe(
            data => {
                // update storage and ratio display
                this.details.quota = newQuotaValue;
                this.storageRatio = this.getStorageRatio(this.details.storage, this.details.quota);

                this.ns.success(
                    { 
                        key: 'notify.user.quota.update.content', 
                        parameters: {
                            user: `${this.details.firstName} ${this.user.lastName}`
                        } 
                    }, 'notify.user.quota.update.title');
                this.cdRef.markForCheck();
            },
            error => {
                this.ns.error(
                    {
                        key: 'notify.user.quota.update.error.content',
                        parameters: {
                            user: `${this.details.firstName} ${this.user.lastName}`
                        }
                    }, 'notify.user.quota.update.error.title', error);
            }
        );
    }

    /**
     * Refresh newQuotaValue with new selected unit value.
     * @param event new selected unit value
     */
    refreshNewQuotaValue(event): void {
        let newQuotaValueBytes: number = this.newQuotaValue * this.newQuotaUnitValue;
        this.newQuotaValue = newQuotaValueBytes / event;
        this.newQuotaUnitValue = event;
    }

    getMaxUserQuota(): number {
        let maxQuotaObj = this.maxQuotas.find(maxQuota => maxQuota.name == this.user.type);
        return maxQuotaObj.maxQuota;
    }

    isQuotaInferiorToStorage(): boolean {
        let newQuotaValueBytes: number = this.newQuotaValue * this.newQuotaUnitValue;
        return newQuotaValueBytes < this.details.storage;
    }

    isQuotaSuperiorToMaxQuota(): boolean {
        let newQuotaValueBytes: number = this.newQuotaValue * this.newQuotaUnitValue;
        return newQuotaValueBytes > this.maxQuota;
    }

    private getStorageRatio(storage: number, quota: number): number {
        return Math.min(100, Math.round((storage * 100) / quota * 100) / 100);
    }
}