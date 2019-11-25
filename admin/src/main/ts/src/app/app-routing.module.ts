import {AppHomeComponent} from './app-home.component';
import {NavComponent} from './core/nav/nav.component';
import {ConfigResolver} from './core/resolvers/config.resolver';
import {I18nResolver} from './core/resolvers/i18n.resolver';
import {StructuresResolver} from './core/resolvers/structures.resolver';
import {SessionResolver} from './core/resolvers/session.resolver';
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';


const routes: Routes = [
  {
      path: 'admin',
      resolve: {session: SessionResolver, structures: StructuresResolver, i18n: I18nResolver, config: ConfigResolver},
      component: NavComponent,
      children: [
          {path: '', component: AppHomeComponent},
          {path: ':structureId', loadChildren: () => import('./structure/structure.module').then(m => m.StructureModule)}
      ]
  },
  {
      path: '**',
      redirectTo: '/admin'
  }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
