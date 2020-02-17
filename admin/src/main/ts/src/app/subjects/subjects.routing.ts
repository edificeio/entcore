import {Routes} from '@angular/router';

import {SubjectsComponent} from './subjects.component'
import {SubjectCreate} from "./create/subject-create.component";
import {SubjectsResolver} from "./subjects.resolver";

export let routes: Routes = [
    {
        path: '', component: SubjectsComponent, resolve: {subjectLit: SubjectsResolver},
        children: [
            {
                path: 'create',
                component: SubjectCreate
            }
        ]

    }
];