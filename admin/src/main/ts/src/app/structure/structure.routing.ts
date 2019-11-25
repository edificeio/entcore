import {Routes} from '@angular/router';
import {StructureResolver} from './structure.resolver';
import {StructureComponent} from './structure.component';
import {StructureHomeComponent} from './structure-home/structure-home.component';

export let routes: Routes = [
	{
		path: '', component: StructureComponent, resolve: { structure: StructureResolver },
		children: [
			{ path: '', component: StructureHomeComponent },
			{ path: 'users', loadChildren: () => import('../users/users.module').then(m => m.UsersModule)},
			{ path: 'groups', 	loadChildren: () => import('../groups/groups.module').then(m => m.GroupsModule)},
			{ path: 'imports-exports', loadChildren: () => import('../imports-exports/imports-exports.module').then(m => m.ImportsExportsModule)},
			{ path: 'services', loadChildren: () => import('../services/services.module').then(m => m.ServicesModule)},
			{ path: 'management', loadChildren: () => import('../management/management.module').then(m => m.ManagementModule)}
		]
	}
];
