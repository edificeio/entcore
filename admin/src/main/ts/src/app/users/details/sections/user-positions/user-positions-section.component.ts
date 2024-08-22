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
  searchPrefix: string = "";
  newPosition: UserPosition = {name: "", source: "MANUAL"};
  showUserPositionSelectionLightbox: boolean = false;
  showUserPositionCreationLightbox: boolean = false;
  showEmptyScreen: boolean = false;

  /** Truthy when detecting user's positions changes */
  get hasUserPositionsChanged(): boolean {
    return (!this.details.userPositions && this.userPositions.length > 0) || 
      (this.details.userPositions && 
        this.details.userPositions.map((p) => p.id) !== this.userPositions.map((p) => p.id));
  }

  /** List of selectable positions = all positions except duplicates and those already assigned. */
  get filteredPositionList() {
    // Extract and trim names
    const filteredList = this.positionList?.map(position => position.name.trim())
      // Remove empty and already selected names
      .filter(name => !this.userPositions.some(value=> name.length===0 || (value.name && value.name.trim()===name))) ?? [];
    // Remove remaining duplicates
    return filteredList.filter((name, index) => (index+1>=filteredList.length || filteredList.indexOf(name, index+1)<0))
      // return result as an array of UserPosition
      .map(name => ({name}));
  }

  set newPositionName(name) {
    name = name ? name.trim() : "";
    // Check if the name of this new position does not already exist in the list
    if( this.positionList && 
        !this.positionList.some(position => position.name===name) ) {
      this.newPosition = {name, source: "MANUAL"};
      this.showEmptyScreen = name && name.length;
    }
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
    const name = position.name.trim();
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
          this.userPositions.push(addedPosition);
          this.showUserPositionSelectionLightbox = false;
          this.saveUpdate();
        }
    });
  }

  removeUserPosition(position: UserPosition) {
    this.userPositions = this.userPositions.filter((p) => p.id !== position.id);
  }

  openUserPositionCreationModal() {
    this.newPosition = { name: this.searchPrefix || "", source: "MANUAL" };
    this.showUserPositionSelectionLightbox = false;
    this.showUserPositionCreationLightbox = true;
  }

  addUserPositionToList(position: UserPosition | undefined) {
    if( position ) {
      this.positionList.push(position);
    }
    this.showUserPositionCreationLightbox = false;
  }

  saveUpdate() {
    if( this.userPositions && this.userPositions.length>0) {
      this.details.userPositions = [...this.userPositions];
    } else {
      this.details.userPositions = [];
    }
    this.spinner
      .perform('portal-content', this.details.updateUserPositions())
      .then(() => {
        this.ns.success(
          'notify.user-position.assign.success.content',
          'notify.user-position.success.title'
        );
        this.userInfoService.setState(this.details);
      })
      .catch(err => {
        this.ns.error(
          'notify.user-position.assign.error.content',
          'notify.user-position.assign.error.title', 
          err
        );
      });
  }

  filteredListChange(filteredList: UserPosition[]) {
    this.showEmptyScreen = filteredList.length === 0;
    this.cdRef.detectChanges();
  }
}
