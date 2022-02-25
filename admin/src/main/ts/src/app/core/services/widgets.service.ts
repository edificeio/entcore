import {Injectable} from '@angular/core';
import { Profile, Structure } from 'src/app/services/_shared/services-types';
import { StructureModel } from '../store/models/structure.model';
import { WidgetModel } from '../store/models/widget.model';
import { NotifyService } from './notify.service';
import http from 'axios';

    // Example {\"Teacher\":[\"Blog\", \"Exercices et évaluations\"]}
export type DefaultBookmarks = {
    [p in Profile]?: Array<string> /** array of translated app names */
};

@Injectable()
export class WidgetService {
    constructor( private notify:NotifyService ) {
    }

    public massLink(widget:WidgetModel, structure:Structure, profiles:Array<Profile>) {
        const url = `/appregistry/widget/${widget.id}/authorize/${structure.id}`;
        return http.put(url + (profiles.length>0 ? "?profile="+profiles.join("&profile=") : ""))
            .then( () => this.notify.info('widget.mass.link.notify.ok') )
            .catch( () => this.notify.error('widget.mass.link.notify.ko') );
    }

    public massUnlink(widget:WidgetModel, structure:Structure, profiles:Array<Profile>){
        const url = `/appregistry/widget/${widget.id}/authorize/${structure.id}`;
        return http.delete(url + (profiles.length>0 ? "?profile="+profiles.join("&profile=") : ""))
            .then( () => this.notify.info('widget.mass.unlink.notify.ok') )
            .catch( () => this.notify.error('widget.mass.unlink.notify.ko') );
    }

    public massSetMandatory(widget:WidgetModel, structure:Structure, profiles:Array<Profile>){
        const url = `/appregistry/widget/${widget.id}/mandatory/${structure.id}/mass`;
        return http.put(url + (profiles.length>0 ? "?profile="+profiles.join("&profile=") : ""))
            .then( () => this.notify.info('widget.notify.ok') )
            .catch( () => this.notify.error('widget.notify.ko') );
    }
    
    public massUnsetMandatory(widget:WidgetModel, structure:Structure, profiles:Array<Profile>){
        const url = `/appregistry/widget/${widget.id}/mandatory/${structure.id}/mass`;
        return http.delete(url + (profiles.length>0 ? "?profile="+profiles.join("&profile=") : ""))
            .then( () => this.notify.info('widget.notify.ok') )
            .catch( () => this.notify.error('widget.notify.ko') );
    }

    public getMyAppsParameters(structure:StructureModel):Promise<DefaultBookmarks|null> {
        const url = `/appregistry/applications/${structure.id}/default-bookmarks`;
        return http.get<{defaultBookmarks:string}>(url)
            .then( b => {
                if( typeof b.data.defaultBookmarks==="string" ) {
                    return JSON.parse(b.data.defaultBookmarks) as DefaultBookmarks;
                } else {
                    // When none are defined, defaultBookmarks may be an empty array.
                    return {};
                }
            })
            .catch( () => {
                this.notify.error('services.widget.myapps.prefs.load.error');
                return null;
            } );
    }

    public setMyAppsParameters(structure:Structure, bookmarks:DefaultBookmarks) {
        const url = `/appregistry/applications/${structure.id}/default-bookmarks`;
        return http.put(url, {apps: bookmarks})
            .then( () => this.notify.info('widget.notify.ok') )
            .catch( () => this.notify.error('widget.notify.ko') );
    }
}