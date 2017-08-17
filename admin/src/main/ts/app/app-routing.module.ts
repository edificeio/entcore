import { Routes } from '@angular/router'
import { I18nResolve } from './core/resolvers/i18n.resolve'
import { StructuresResolve } from './core/resolvers/structures.resolve'
import { SessionResolve } from './core/resolvers/session.resolve'

import { AppComponent } from './app.component'
import { AppHomeComponent } from "./app-home.component";
import { NavComponent } from './core/nav/nav.component'

export let routes : Routes = [
	{
		path: 'admin',
		resolve: { session: SessionResolve, structures : StructuresResolve, i18n: I18nResolve },
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