import { Component } from '@angular/core'

@Component({
    selector: 'export',
    template: `
  	<h4><span><i class="fa fa-wrench"></i> Configuration de l'export </span></h4>


    <div class="row">
    <form #createForm="ngForm" >
    <form-field label="Type de l'export">
    <select [ngModel]="formats" name="structure">
         <option *ngFor="let f of formats | orderBy: ['+label']" [ngValue]="f"><s5l>{{ f.label }}</s5l></option>
     </select>
    </form-field>
    <form-field label="Classe">
    <select [ngModel]="formats" name="structure">
         <option *ngFor="let f of formats | orderBy: ['+label']" [ngValue]="f"><s5l>{{ f.label }}</s5l></option>
     </select>
    </form-field>
    <form-field label="Profile">
    <select class="three cell row-item" ng-model="exportData.params.profile">
        <option value="" ng-if="exportData.filterProfiles('')">
            [[lang.translate("directory.allProfiles")]]
        </option>
        <option value="Teacher" ng-if="exportData.filterProfiles('Teacher')">
            [[lang.translate("directory.Teacher")]]
        </option>
        <option value="Personnel" ng-if="exportData.filterProfiles('Personnel')">
            [[lang.translate("directory.Personnel")]]
        </option>
        <option value="Relative" ng-if="exportData.filterProfiles('Relative')">
            [[lang.translate("directory.Relative")]]
        </option>
        <option value="Student" ng-if="exportData.filterProfiles('Student')">
            [[lang.translate("directory.Student")]]
        </option>
        <option value="Guest" ng-if="exportData.filterProfiles('Guest')">
            [[lang.translate("directory.Guest")]]
        </option>
    </select>
    </form-field>
    <form-field label="Filtre">
    <select class="three cell" ng-model="exportData.params.filterActive">
        <option value="">directory.ignoreActivation</option>
        <option value="active">directory.onlyActivatedUsers</option>
        <option value="inactive">directory.onlyInactiveUsers</option>
    </select>
    </form-field>
    <div class="pull-right">
        <button class=""
            [disabled]="false">
            <s5l>export</s5l>
        </button>
    </div>
    </form>
    </div>





            `
})
export class ExportComponent{
    formats=[];
}
