import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgModule } from "@angular/core";
import { MatSortModule } from "@angular/material/sort";
import { NgxOdeSijilModule } from "ngx-ode-sijil";
import { NgxOdeUiModule } from "ngx-ode-ui";
import { FlexLayoutModule } from "@angular/flex-layout";
import { UserPositionModalComponent } from "./user-position-modal/user-position-modal.component";
import { MatDialogModule } from "@angular/material/dialog";
import { UserPositionListComponent } from "./user-position-list/user-position-list.component";

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgxOdeUiModule,
    NgxOdeSijilModule.forChild(),
    MatDialogModule,
    MatSortModule,
    FlexLayoutModule,
  ],
  declarations: [UserPositionModalComponent, UserPositionListComponent],
  exports: [UserPositionModalComponent, UserPositionListComponent]
})
export class SharedModule {}
