import { Routes } from '@angular/router'
import { StructureResolve } from './structure.resolve'
import { StructureComponent } from './structure.component'
import { StructureHomeComponent } from './structure-home.component';

export let routes : Routes = [
	{
		path: '', component: StructureComponent, resolve: { structure: StructureResolve },
		children: [
			{ path: '', component: StructureHomeComponent },
			{ path: 'users', loadChildren: '../users/users.module#UsersModule' },
			{ path: 'groups', 	loadChildren: '../groups/groups.module#GroupsModule' }
		]
	}
]