import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Assignment} from '../../../shared/services-types';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';

@Component({
    selector: 'ode-application-assignment',
    templateUrl: './application-assignment.component.html'
})
export class ApplicationAssignmentComponent {
    @Input()
    application: ApplicationModel;
    @Input()
    assignmentGroupPickerList: GroupModel[];

    @Output()
    remove: EventEmitter<Assignment> = new EventEmitter();
    @Output()
    add: EventEmitter<Assignment> = new EventEmitter();

    selectedRole: RoleModel;
    showRoleAttributionLightbox = false;

    // Dirty hack to display an alert message to ADML about the target (1D or 2D) of the application
    appsTarget = appsTarget;

    public openRoleAttributionLightbox(role: RoleModel) {
        this.selectedRole = role;
        this.showRoleAttributionLightbox = true;
    }
}

const appsTarget = {
    'admin-large': '1D-2D',
    'workspace-large': '1D-2D',
    'conversation-large': '2D-1D',
    'wiki-large': '1D-2D',
    'collaborative-wall-large': '2D',
    'rack-large': '1D-2D',
    'timelinegenerator-large': '1D-2D',
    'bookmark-large': '2D',
    'Xiti-large': '1D-2D',
    'stats-large': '1D',
    'rbs-large': '2D',
    schoolbook: '1D',
    'scrap-book-large': '1D-2D',
    'searchengine-large': '2D',
    'poll-large': '2D',
    'actualites-large': '1D-2D',
    'mindmap-large': '1D-2D',
    'pages-large': '2D',
    'support-large': '1D-2D',
    'rss-large': '2D',
    'Cursus-large': '2D',
    'statistics-large': '2D',
    'exercizer-large': '2D',
    'community-large': '2D',
    'Maxicours-large': '2D',
    'calendar-large': '2D',
    'pad-large': '1D-2D',
    'cns-large': '2D',
    'forum-large': '2D',
    'sharebigfiles-large': '2D',
    'cahier-de-texte-large': '1D',
    'userbook-large': '1D-2D',
    'settings-class-large': '1D',
    competences: '2D',
    parametrage: '2D',
    'archive-large': '1D-2D',
    'blog-large': '1D-2D',
    'cahier-textes-large': '2D'
};
