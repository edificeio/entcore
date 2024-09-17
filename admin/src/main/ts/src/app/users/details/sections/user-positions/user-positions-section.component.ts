import {
  ChangeDetectorRef,
  Component,
  OnInit,
  ViewEncapsulation,
} from "@angular/core";

import { AbstractSection } from "../abstract.section";
import { SpinnerService } from "ngx-ode-ui";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";
import { UserInfoService } from "../info/user-info.service";
import { NotifyService } from "src/app/core/services/notify.service";

@Component({
  selector: "ode-user-positions-section",
  templateUrl: "./user-positions-section.component.html",
  styleUrls: ["./user-positions-section.component.scss"],
  inputs: ["user", "structure"],
  encapsulation: ViewEncapsulation.None,
})
export class UserPositionsSectionComponent
  extends AbstractSection
  implements OnInit
{
  /** List of all positions existing in structures the user is ADMx of. */
  positionList: UserPosition[];
  userPositions: UserPosition[];
  filteredList: UserPosition[] = [];
  searchContent: string = "";
  newPosition: UserPosition = {name: "", source: "MANUAL"};
  showUserPositionSelectionLightbox: boolean = false;
  showUserPositionCreationLightbox: boolean = false;
  get showEmptyScreen(): boolean{
    return this.filteredList.length === 0 && this.searchContent?.length > 0;
  };

  showConfirmRemovePosition: boolean = false;
  positionToRemove: UserPosition;

  /** Truthy when detecting user's positions changes */
  get hasUserPositionsChanged(): boolean {
    return (!this.details.userPositions && this.userPositions.length > 0) || 
      (this.details.userPositions && 
        this.details.userPositions.map((p) => p.id) !== this.userPositions.map((p) => p.id));
  }

  /** List of selectable positions = all positions except duplicates. */
  get filteredPositionList() {
    // Extract and trim names
    const filteredList = this.positionList?.map(position => position.name.trim()) ?? [];
    // Remove duplicates
    return filteredList.filter((name, index) => (index+1>=filteredList.length || filteredList.indexOf(name, index+1)<0))
      // return result as an array of UserPosition
      .map(name => ({name}));
  }

  constructor(
    private ns: NotifyService,
    public spinner: SpinnerService,
    protected cdRef: ChangeDetectorRef,
    private userInfoService: UserInfoService,
    private userPositionServices: UserPositionServices
  ) {
    super();
  }

  async ngOnInit() {
    this.positionList = await this.spinner
      .perform('portal-content', this.userPositionServices.searchUserPositions())
      .catch(err => {
        this.ns.error(
          'notify.user-position.read.error.content',
          'notify.user-position.read.error.title', 
          err
        );
        return [];
      });
    this.memoizeInitialUserPositions();
  }

  protected onUserChange() {
    this.memoizeInitialUserPositions();
  }

  private memoizeInitialUserPositions() {
    this.userPositions = this.details.userPositions
      ? [...this.details.userPositions.filter(position => !!position.id)]
      : [];
  }

  selectUserPosition(position: UserPosition) {
    const name = position.name?.trim();
    const structureId = this.structure.id;
    // Search the structure for a UserPosition with this name.
    Promise.resolve(this.positionList.find(pos => pos.name==name && pos.structureId==structureId))
    .then( async (positionToAdd) => positionToAdd 
        ? positionToAdd
        // If none is found then create one before selecting it.
        : await this.spinner.perform<UserPosition|undefined>('portal-content', 
            this.userPositionServices.createUserPosition({name, structureId})
            .then(created => {
                this.positionList.push(created);
                this.ns.success(
                    "notify.user-position.create.success.content",
                    "notify.user-position.success.title"
                );
                console.log(created);
                return created;
            })
            .catch(err => {
                this.ns.error({
                      key: 'notify.user-position.create.error.content',
                      parameters: {position: name}},
                  'notify.user-position.create.error.title',
                  err
                );
                return undefined;
            })
        )
    )
    .then(addedPosition => {
      if(addedPosition) {
        // Do not duplicate positions
        const addedName = addedPosition.name?.trim();
        if( addedName && this.userPositions.findIndex(pos => pos.name?.trim() == addedName) < 0 ) {
          this.userPositions.push(addedPosition);
          this.saveUpdate();
        }
        this.showUserPositionSelectionLightbox = false;
      }
    });
  }

  removeUserPosition(position: UserPosition) {
    this.positionToRemove = position;
    this.showConfirmRemovePosition = true;
  }

  removeUserPositionCancel() {
    this.searchContent = "";
    this.showConfirmRemovePosition = false;
  }

  removeUserPositionConfirmed() {
    this.userPositions = this.userPositions.filter((p) => p.id !== this.positionToRemove.id);
    this.saveUpdate(true);
    this.searchContent = "";
    this.showConfirmRemovePosition = false;
  }

  openUserPositionCreationModal() {
    this.newPosition = { name: this.searchContent || "", source: "MANUAL" };
    this.showUserPositionSelectionLightbox = false;
    this.showUserPositionCreationLightbox = true;
  }

  addUserPositionToList(position: UserPosition | undefined) {
    if( position ) {
      this.positionList.push(position);
      this.userPositions.push(position);
      this.saveUpdate();
    }
    this.showUserPositionCreationLightbox = false;
  }

  saveUpdate(removePosition = false) {
    if( this.userPositions && this.userPositions.length>0) {
      this.details.userPositions = [...this.userPositions];
    } else {
      this.details.userPositions = [];
    }
    this.spinner
      .perform('portal-content', this.details.updateUserPositions())
      .then(() => {
        this.ns.success(
          removePosition ? 'notify.user-position.remove.success.content' :'notify.user-position.assign.success.content',
          'notify.user-position.success.title'
        );
        this.userInfoService.setState(this.details);
      })
      .catch(err => {
        this.ns.error(
          removePosition ? 'notify.user-position.remove.error.content':  'notify.user-position.assign.error.content',
          'notify.user-position.assign.error.title', 
          err
        );
      });
  }

  filteredListChange(filteredList: UserPosition[]) {
    this.filteredList = filteredList;
    this.cdRef.detectChanges();
  }
}
