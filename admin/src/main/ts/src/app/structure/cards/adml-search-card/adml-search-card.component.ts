import { Component, OnInit } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-adml-search-card',
    template: `
        <div class="card-header">
            <span>
                <i class="fa fa-search" aria-hidden="true"></i>
                <s5l>admc.search</s5l>
            </span>
        </div>
        <div class="card-body">
            <button routerLink="../users/list/filter">
                <i class="fa fa-search" aria-hidden="true"></i>
                <s5l>Recherche dans la structure courante</s5l>
            </button>
            <button routerLink="../users/tree-list/search">
                <i class="fa fa-user" aria-hidden="true"></i>
                <s5l>Rechercher dans toutes les structures liées</s5l>
            </button>
        </div>
    `
})
export class AdmlSearchCardComponent extends OdeComponent implements OnInit {

    ngOnInit(): void {}
}