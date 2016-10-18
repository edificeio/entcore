import { http } from './http';
import { ui } from './ui';
import { model } from './modelDefinitions';
import { _ } from './libs/underscore/underscore';

export var skin = {
	addDirectives: undefined as any,
	templateMapping: {},
	skin: 'raw',
	theme: '/assets/themes/raw/default/',
	portalTemplate: '/assets/themes/raw/portal.html',
	basePath: '',
	logoutCallback: '/',
	loadDisconnected: async function(): Promise<any>{
		return new Promise((resolve, reject) => {
			var rand = Math.random();
			http().get('/skin', { token: rand }).done((data) => {
				this.skin = data.skin;
				this.theme = '/assets/themes/' + data.skin + '/skins/default/';
				this.basePath = this.theme + '../../';

				http().get('/assets/themes/' + data.skin + '/template/override.json', { token: rand }, { disableNotifications: true }).done((override) => {
					this.templateMapping = override;
					resolve();
				})
				.e404(() => resolve());
			}).e404(() => {});
		});
	},
	listThemes: function(cb){
		http().get('/themes').done(function(themes){
			if(typeof cb === 'function'){
				cb(themes);
			}
		});
	},
	setTheme: function(theme){
		ui.setStyle(theme.path);
		http().get('/userbook/api/edit-userbook-info?prop=theme&value=' + theme._id);
	},
	loadBookmarks: async function(){
		return new Promise<any>((resolve, reject) => {
			http().get('/userbook/preference/apps').done(function(data){
				if(!data.preference){
					data.preference = null;
				}
				model.me.bookmarkedApps = JSON.parse(data.preference) || [];
				var upToDate = true;
				let remove = [];
				model.me.bookmarkedApps.forEach(function(app, index){
					var foundApp = _.findWhere(model.me.apps, { name: app.name });
					var updateApp = true;
					if(foundApp){
						updateApp = JSON.stringify(foundApp) !== JSON.stringify(app);
						if(updateApp){
							for(var property in foundApp){
								app[property] = foundApp[property];
							}
						}
					}
					else{
						remove.push(app);
					}
					
					upToDate = upToDate && !updateApp;
				});
				remove.forEach(function(app) {
					var index = model.me.bookmarkedApps.indexOf(app);
					model.me.bookmarkedApps.splice(index, 1);
				});
				if(!upToDate){
					http().putJson('/userbook/preference/apps', model.me.bookmarkedApps);
				}

				resolve();
			});
		});
	},
	loadConnected: async function(): Promise<any>{
		var rand = Math.random();
		var that = this;
		return new Promise((resolve, reject) => {
			http().get('/theme').done(function(data){
				that.theme = data.skin;
				that.basePath = that.theme + '../../';
				that.skin = that.theme.split('/assets/themes/')[1].split('/')[0];
				that.portalTemplate = '/assets/themes/' + that.skin + '/portal.html';
				that.logoutCallback = data.logoutCallback;

				http().get('/assets/themes/' + that.skin + '/template/override.json', { token: rand }).done(function(override){
					that.templateMapping = override;
					resolve();
				})
				.e404(() => { 
					resolve(); 
				});
			});
		});
	}
};

if (!(window as any).entcore) {
    (window as any).entcore = {};
}
(window as any).entcore.skin = skin;