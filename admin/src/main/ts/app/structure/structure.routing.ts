import { Routes } from '@angular/router'
import { StructureResolver } from './structure.resolver'
import { StructureComponent } from './structure.component'
import { StructureHomeComponent } from './structure-home.component';

export let routes : Routes = [
	{
		path: '', component: StructureComponent, resolve: { structure: StructureResolver },
		children: [
			{ path: '', component: StructureHomeComponent },
			{ path: 'users', loadChildren: '../users/users.module#UsersModule' },
			{ path: 'groups', 	loadChildren: '../groups/groups.module#GroupsModule' }
		]
	}
]