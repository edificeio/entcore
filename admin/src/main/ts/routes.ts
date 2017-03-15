//import { GroupsModule } from '../modules/groups/groups.module'
import { Routes } from '@angular/router'
import { I18nResolve } from './routing/i18n.resolve'
import { StructuresResolve } from './routing/structures.resolve'
import { StructureResolve } from './routing/structure.resolve'
import { SessionResolve } from './routing/session.resolve'
import { Portal, Home, StructureHome } from './components'
import { routing } from './routing/routing.utils'

export let routes : Routes = [
	{
		path: 'admin',
		resolve: { session: SessionResolve, structures : StructuresResolve, i18n: I18nResolve },
		component: Portal,
			children: [
				{ path: '', component: Home },
				{ path: ':structureId', resolve: { structure: StructureResolve }, children: [
					{ path: '', 		component: StructureHome },
					{ path: 'users', 	loadChildren: './modules/users/users.module#UsersModule' },
					{ path: 'groups', 	loadChildren: './modules/groups/groups.module#GroupsModule' }
				]}
			]
	},
	{ path: '**', redirectTo: '/admin' }
]