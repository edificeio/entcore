import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit} from '@angular/core';
import {CalendarService} from './calendar.service';
import {OdeComponent} from 'ngx-ode-core';
import {StructureModel} from '../../core/store/models/structure.model';
import {routing} from '../../core/services/routing.service';
import {Data} from '@angular/router';
import {NgForm} from '@angular/forms';
import {NotifyService} from '../../core/services/notify.service';
import {Slot, SlotProfile} from './SlotProfile';

@Component({
    selector: 'ode-app-calendar',
    templateUrl: './calendar.component.html',
    styleUrls: ['./calendar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class CalendarComponent extends OdeComponent implements OnInit, OnDestroy, AfterViewInit {

    private structure: StructureModel;
    public viewLoaded = false;
    public grids: SlotProfile[] = [];
    public selectedGrid: SlotProfile;
    public selectedSlot: Slot = null;
    public gridFormActivated = false;
    public slotInputDisplay = false;
    public slotModel: Slot = {name: '', startHour: '', endHour: ''};
    public showConfirmation = false;

    constructor(injector: Injector,
                private calendarService: CalendarService,
                private notifyService: NotifyService) {
        super(injector);
    }

    ngOnInit(): void {
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.hideEditGridForm();
                this.hideSlotInputForm();
                this._deselectGrid();
                this._getGrids();
            }
        }));
    }

    /**
     * Set a grid as selected.
     * @param gridId Id of the grid to select
     */
    private _selectGrid(gridId: string): void {
        for (const grid of this.grids) {
            if (grid._id === gridId) {
                this.selectedGrid = grid;
                return;
            }
        }
    }

    /**
     * Deselect the currently selected grid.
     */
    private _deselectGrid(): void {
        this.selectedGrid = {_id: '', name: '', schoolId: '', slots: []};
    }

    /**
     * Get all grids for the current structure.
     */
    private _getGrids(): void {
       this.subscriptions.add(this.calendarService
            .getGrids(this.structure._id)
            .subscribe(data => {
                this.grids = data;
                this.viewLoaded = true;
                if (!this.isGridSelected() && !this.isGridEmpty()) {
                    this.selectedGrid = this.grids[0];
                } else {
                    this._selectGrid(this.selectedGrid._id);
                }
                this.changeDetector.detectChanges();
            }, err => {
                this.notifyService.notify(
                    'management.calendar.get.grids.error.content',
                    'management.calendar.get.grids.error.title', err.error.error, 'error');
            }));
    }

    /**
     * Check if a grid has been selected.
     */
    public isGridSelected(): boolean {
        return (this.selectedGrid._id !== '');
    }

    /**
     * Check if the grid is empty or not.
     */
    public isGridEmpty(): boolean {
        return (this.grids[0] === undefined);
    }

    /**
     * Display the form to add a new grid.
     */
    public showAddGridForm(): void {
        this._deselectGrid();
        this.gridFormActivated = true;
    }

    /**
     * Display the form to edit the selected grid.
     */
    public showEditGridForm(): void {
        this.gridFormActivated = true;
    }

    /**
     * Hide the form to edit the selected grid.
     */
    public hideEditGridForm(): void {
        this.gridFormActivated = false;
    }

    /**
     * Display the form to edit/add a slot.
     */
    public showSlotInputForm(): void {
        this.slotInputDisplay = true;
    }

    /**
     * Hide the form to edit/add a slot.
     */
    public hideSlotInputForm(): void {
        this.slotInputDisplay = false;
    }

    /**
     * Add a new slot for the currently selected grid.
     * @param form The form data containing the slot info.
     */
    public addSlot(form: NgForm): void {
        this.subscriptions.add(this.calendarService.addSlot(this.selectedGrid._id, form.value)
            .subscribe(() => {
                this.hideSlotInputForm();
                this._getGrids();
            }, err => {
                this.notifyService.notify(
                    'management.calendar.add.error.content',
                    'management.calendar.add.error.title', err.error.error, 'error');
            }));
    }

    /**
     * Edit the selected slot.
     * @param slot Slot to be edited.
     * @param form The form data containing the slot info.
     */
    public editSlot(slot: Slot, form: NgForm): void {
        this.subscriptions.add(this.calendarService.updateSlot(this.selectedGrid._id, slot.id, form.value)
            .subscribe(() => {
                    this.selectedSlot = null;
                    this._getGrids();
                }, err => {
                    this.notifyService.notify(
                        'management.calendar.slot.edit.error.content',
                        'management.calendar.edit.error.title', err.error.error, 'error');
                }
            ));
    }

    /**
     * Select a slot.
     * @param slot Slot to be selected.
     */
    public selectSlot(slot: Slot): void {
        this.selectedSlot = slot;
    }

    /**
     * Delete a slot from the selected grid.
     * @param slot Slot to be deleted.
     */
    public deleteSlot(slot: Slot): void {
        this.subscriptions.add(this.calendarService.deleteSlot(this.selectedGrid._id, slot.id).subscribe(
            () => {
                this._getGrids();
            },
            err => {
                this.notifyService.notify(
                    'management.calendar.slot.delete.error.content',
                    'management.calendar.delete.error.title', err.error.error, 'error');
            }
        ));
    }


    /**
     * Save the form data to edit a grid, or to add a new grid if none have been selected.
     * @param form The form data with the edited/new grid name.
     */
    public saveGrid(form: NgForm): void {
        const slotProfile: SlotProfile = {name: form.value.gridName, schoolId: this.structure._id, slots: []};
        this.gridFormActivated = false;
        if (this.isGridSelected()) {
            this.subscriptions.add(this.calendarService.updateGrid(this.selectedGrid._id, slotProfile).subscribe(
                () => {
                    this._getGrids();
                },
                (err) => {
                    this.notifyService.notify(
                        'management.calendar.add.error.content',
                        'management.calendar.add.error.title', err.error.error, 'error');
                }
            ));
        } else {
            this.subscriptions.add(this.calendarService.addGrid(slotProfile).subscribe(
                () => {
                    this._getGrids();
                },
                (err) => {
                    this.notifyService.notify(
                        'management.calendar.grid.edit.error.content',
                        'management.calendar.edit.error.title', err.error.error, 'error');
                }
            ));
        }
    }

    /**
     * Delete the selected grid.
     */
    public deleteGrid() {
        this.subscriptions.add(this.calendarService.deleteGrid(this.selectedGrid._id).subscribe(
            () => {
                this.notifyService.success(
                    'management.calendar.grid.delete.success.content',
                    'management.calendar.grid.delete.success.title');
                this.showConfirmation = false;
                this._deselectGrid();
                this._getGrids();
            },
            (err) => {
                this.notifyService.notify(
                    'management.calendar.grid.delete.error.content',
                    'management.calendar.delete.error.title', err.error.error, 'error');
            }
        ));
    }
}
