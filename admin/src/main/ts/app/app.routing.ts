import { Routes } from '@angular/router'
import { I18nResolver } from './core/resolvers/i18n.resolver'
import { StructuresResolver } from './core/resolvers/structures.resolver'
import { SessionResolver } from './core/resolvers/session.resolver'

import { AppComponent } from './app.component'
import { AppHomeComponent } from "./app-home.component";
import { NavComponent } from './core/nav/nav.component'

export let routes : Routes = [
	{
		path: 'admin',
		resolve: { session: SessionResolver, StructuresResolver, I18nResolver },
		component: NavComponent,
			children: [
				{ path: '', component: AppHomeComponent },
				{ path: ':structureId', loadChildren: './structure/structure.module#StructureModule' }
			]
	},
	{ 	
		path: '**',
		redirectTo: '/admin'
	}
]