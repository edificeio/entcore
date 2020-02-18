import {Routes} from '@angular/router';

import {SubjectsComponent} from './subjects.component'
import {SubjectCreate} from "./create/subject-create.component";
import {SubjectsResolver} from "./subjects.resolver";
import {SubjectDetails} from "./details/subject-details.component";

export let routes: Routes = [
    {
        path: '', component: SubjectsComponent, resolve: {subjectLit: SubjectsResolver},

        children: [
            {
                path: 'create',
                component: SubjectCreate
            },
            {
                path: ':subjectId/details',
                component: SubjectDetails
            },
        ]
    }
];