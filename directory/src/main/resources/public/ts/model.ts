// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as oldHttp, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

import http from 'axios';
import { Collection, _, idiom as lang, model, moment, notify, http as oldHttp, ui } from 'entcore';

export const directory = {
	directory: undefined,
	favoriteForm: undefined,
	network: undefined,
	classAdmin: undefined,
	account: undefined,
	User: function(data?){
		if(data && !data.mood){
			data.mood = 'default';
		}
		this.updateData(data);
		this.relatives = [];
		this.profiles = [];

		this.open = async function(){
			var that = this;
			var data = (await http.get('/userbook/api/person?id=' + this.id + '&type=' + this.type)).data;
			if(!data.result[0]){
				this.id = undefined;
				return;
			}
			data.result[0].hobbies = _.filter(data.result[0].hobbies, function(hobby){
				return hobby.values
			})
			data.result[0].relatives = _.map(data.result, function(item){
				return new directory.User({ displayName: item.relatedName, id: item.relatedId, type: item.relatedType });
			})
			.filter(function(relative){
				return relative.id;
			});
			data.result[0].relatives = _.filter(data.result[0].relatives, function(user){
				return user.id !== '';
			});
			data.result[0].attachedStructures = data.result[0].schools;
			if(!data.result[0]){
				this.id = undefined;
				return;
			}

			this.updateData(data.result[0]);
			this.trigger('sync');
		};
		this.loadChildren = async function() {
			var data = {
				childrenStructure: (await http.get('/directory/user/' + this.id + '/children')).data
			};
			this.updateData(data);
			this.trigger('sync');
		};
		this.getProfileName = function() {
			return lang.translate("directory." + this.getProfileType());
		};
		this.getProfile = function() {
			return ui.profileColors.match(this.getProfileType());
		};
		this.getProfileType = function() {
			if (this.profile)
				return this.profile;
			else if (this.type) {
				return this.type[0];
			}
			else
				this.profiles[0];
		};
		this.generateOTP = function(callback) {
			http.post('/auth/generate/otp').then(function(res) {
				if(typeof callback === 'function') {
					callback(res);
				}
			})
			.catch(function(e){
				if(typeof callback === 'function') {
					callback(e.response);
				}
			});
		}
	},
	Group: function(data?){
		this.users = [];

		this.getUsers = async function() {
			var response = await http.get('/communication/visible/group/' + this.id);
			this.users = _.map(response.data, function(item) {
				return new directory.User(item);
			});
		}

		this.getName = async function() {
			const response = await http.get('/directory/group/' + this.id);
			this.name = response.data.name;
		}
	},
	Favorite: function(data?){
		this.users = [];
		this.groups = [];

		var registerChanges = (name, members) => {
			this.name = name;
			this.users = [];
			this.groups = [];
			members.forEach(member => {
				if (member.name)
					this.groups.push(member);
				else
					this.users.push(member);
			});
		}

		this.getUsersAndGroups = async function() {
			var response = await http.get('/directory/sharebookmark/' + this.id);
			this.users = _.map(response.data.users, function(item) {
				return new directory.User(item);
			});
			this.groups = response.data.groups;
			directory.directory.sortGroups(this.groups);
			this.groups = _.map(this.groups, function(item) {
				return new directory.Group(item);
			});
		}
		this.save = async function(name, members, editing) {
			registerChanges(name, members);
			var body = {
				name: name,
				members: _.map(members, function(member) {
					return member.id;
				})
			};
			if (editing) {
				await http.put('/directory/sharebookmark/' + this.id, body);
			}
			else {
				var response = await http.post('/directory/sharebookmark', body);
				this.id = response.data.id;
			}
		}
		this.delete = async function() {
			await http.delete('/directory/sharebookmark/' + this.id);
		}
	},
	ClassAdmin: function(){
		this.sync = function(){
			if(model.me.preferences.selectedClass === undefined){
				model.me.preferences.save('selectedClass', model.me.classes[0]);
			}
			oldHttp().get('/directory/class/' + model.me.preferences.selectedClass).done(function(data){
				this.id = model.me.preferences.selectedClass;
				this.updateData(data);
			}.bind(this));
			this.users.sync();
		};

		this.saveClassInfos = function(){
			oldHttp().putJson('/directory/class/' + this.id, { name: this.name, level: this.level })
		};

		this.collection(directory.User, {
			sync: function(){
				oldHttp().get('/directory/class/' + model.me.preferences.selectedClass + '/users', { requestName: 'loadingUsers' }).done(function(data){
					data.sort(function(a, b) {
						return a.lastName > b.lastName?1:-1;
					});
					this.load(data);
				}.bind(this));
			},
			removeSelection: function(){
				oldHttp().postJson('/directory/user/delete', { users : _.map(this.selection(), function(user){ return user.id; }) });
				Collection.prototype.removeSelection.call(this);
			}
		});

		this.users.match = directory.usersMatch.bind(this.users);

		this.importFile = function(file, type){
			var form = new FormData();
			form.append(type.replace(/(\w)(\w*)/g, function(g0,g1,g2){return g1.toUpperCase() + g2.toLowerCase();}), file);
			form.append('classExternalId', this.externalId);
			oldHttp().postFile('/directory/import/' + type + '/class/' + this.id, form)
				.done(function(){
					this.sync();
				}.bind(this))
				.e400(function(e){
					this.sync();
					var error = JSON.parse(e.responseText).message;
					var errWithIdx = error.split(/\s/);
					if (errWithIdx.length === 2) {
						notify.error(lang.translate(errWithIdx[0]) + errWithIdx[1]);
					} else {
						if(error.indexOf('already exists') !== -1){
							notify.error('directory.import.already.exists');
						}
						else{
							notify.error(error);
						}
					}
				}.bind(this));
		};

		this.addUser = function(user){
			user.saveAccount(function(){
				directory.classAdmin.sync();
				directory.directory.sync();
			});
		};

		this.grabUser = function(user){
			oldHttp().put('/directory/class/' + this.id + '/add/' + user.id).done(function(){
				directory.classAdmin.sync();
			});
		};

		this.blockUsers = function(value){
			this.users.selection().forEach(function(user){
				user.blocked = value;
				oldHttp().putJson('/auth/block/' + user.id, { block: value });
			});
		};

		this.resetPasswords = function(){
			this.users.selection().forEach(function(user){
				oldHttp().post('/auth/sendResetPassword', {
					login: user.originalLogin,
					email: model.me.email
				});
			});
		};

		model.on('preferences-updated', function(){
			this.sync();
		}.bind(this));
	},
	Network: function(){
		this.collection(directory.School, {
			sync: function(){
				var that = this
				return http.get('/userbook/structures').then(function(d){
					this.load(d.data);
					_.forEach(that.all, function(struct){
						struct.parents = _.filter(struct.parents, function(parent){
							var parentMatch = _.findWhere(that.all, {id: parent.id})
							if(parentMatch){
								parentMatch.children = parentMatch.children ? parentMatch.children : []
								parentMatch.children.push(struct)
								return true
							} else
								return false
						})
						if(struct.parents.length === 0)
							delete struct.parents
					})
					this.trigger('sync');
				}.bind(this))
			},
			match: function(search){
				return _.filter(this.all, function(school){
					var words = search.split(' ');
					return _.find(words, function(word){
						var formattedOption = lang.removeAccents(school.name).toLowerCase();
						var formattedWord = lang.removeAccents(word).toLowerCase();
						return formattedOption.indexOf(formattedWord) === -1
					}) === undefined;
				});
			},
			allClassrooms: function(){
				var classrooms = [];
				this.forEach(function(school){
					classrooms = classrooms.concat(school.classrooms.all);
				});
				return classrooms;
			},
			getSchool: function(classroomId) {
				var res = undefined;
				this.forEach(function(school){
					school.classrooms.all.forEach(function(classroom){
						if (classroom.id === classroomId) {
							res = school;
							return;
						}
					});
					if (res !== undefined) {
						return;
					}
				});
				return res;
			}
		});

		this.collection(directory.Classroom, {
			sync: function() {
				return http.get('/auth/oauth2/userinfo?version=v2.0')
					.then(function(res) {
						const { classes, realClassesNames } = res.data;

						let results: {id: string, name: string}[];
						if (!classes || !realClassesNames || classes.length !== realClassesNames.length) {
							results = [];
						}
						results =
							classes.map((id: string, index: number) => ({
								id,
								name: realClassesNames[index],
							})
						);
						this.load(results);
						this.trigger('sync');
					}.bind(this));
			},
			match: function(search){
				return _.filter(this.all, function(classroom){
					var words = search.split(' ');
					return _.find(words, function(word){
						var formattedOption = lang.removeAccents(classroom.name).toLowerCase();
						var formattedWord = lang.removeAccents(word).toLowerCase();
						return formattedOption.indexOf(formattedWord) === -1
					}) === undefined;
				});
			},
		});
	},
	Classroom: function(){
		var that = this;

		this.collection(directory.User, {
			sync: function(){
				oldHttp().get('/userbook/api/class', { id: that.id }).done(function(users){
					this.load(users);
					this.trigger('sync');
				}.bind(this))
			}
		});

		this.users.match = directory.usersMatch.bind(this.users);
	},
	School: function(){
		this.collection(directory.User);
		this.users.match = directory.usersMatch.bind(this.users);

		this.collection(directory.Classroom, {
			match: function(search){
				var searchTerm = lang.removeAccents(search).toLowerCase();
				if(!searchTerm){
					return this.all;
				}
				return _.filter(this.all, function(classroom){
					return lang.removeAccents(classroom.name).toLowerCase().indexOf(searchTerm) !== -1;
				});
			}
		});

		this.sync = function(){
			oldHttp().get('/userbook/structure/' + this.id).done(function(d){
				this.classrooms.load(d.classes);
				this.users.load(d.users);
				this.classrooms.trigger('sync');
				this.trigger('sync');
				directory.network.trigger('classrooms-sync');
			}.bind(this));
		}
	},
	Directory: function(){
		this.sortByDisplayName = function(a, b) {
			return a.displayName > b.displayName;
		};
		this.sortByGroupType = function(a, b) {
			return (a.groupType > b.groupType) ? 1 : -1;
		};
		this.sortByGroupName = function(a, b) {
			return ((a.sortName ? a.sortName : a.name) > (b.sortName ? b.sortName : b.name)) ? 1 : -1;
		};
		this.sortGroups = function(groups) {
			groups = groups.sort(this.sortByGroupType);
			groups = groups.sort(this.sortByGroupName);
		};
		this.collection(directory.User, {
			match: function(){
				return this.all;
			},
			searchDirectory: async function(search, filters, callback, all){
				if (!search)
					search = "";
				this.searched = true;

				var body = {
					search: search.toLowerCase(),
					types: all ? filters.types : ["User"]
				};
				
				if (filters.structures)
					body["structures"] = filters.structures;
				if (filters.classes)
					body["classes"] = filters.classes;
				if (filters.profiles)
					body["profiles"] = filters.profiles;
				if (filters.functions)
					body["functions"] = filters.functions;
				if (filters.positions)
					body["positions"] = filters.positions;
				
				body["mood"] = true;
				
				var response = await http.post('/communication/visible', body);
				var users = _.map(response.data.users, function(user){
					if(!user.mood){
						user.mood = 'default';
					}
					return user;
				});
				users = users.sort(directory.directory.sortByDisplayName);
				if (all) {
					var groups = response.data.groups;
					directory.directory.sortGroups(groups);
					groups = _.map(groups, function(group){
						group.isGroup = true;
						return group;
					});
					users = groups.concat(users);
				}
				this.load(users);

				if(typeof callback === 'function'){
					callback();
				}
			},
			getSearchCriteria: async function() {
				return (await http.get('/userbook/search/criteria?getClassesMonoStructureOnly=true')).data;
			},
			getSearchClasses: async function(structureId) {
				let res = await http.get(`/userbook/search/criteria/${structureId}/classes`);
				if (res && res.data) {
					return res.data.classes;
				}
				return null;
			}
		}),
		this.collection(directory.Group, {
			match: function(){
				return this.all;
			},
			searchDirectory: async function(search, filters, callback){
				if (!search)
					search = "";
				this.searched = true;
				
				var body = {
					search: search.toLowerCase(),
					types: (filters.types && filters.types.length > 0) ? filters.types : ["Group"]
				};
				
				if (filters.structures)
					body["structures"] = filters.structures;
				if (filters.classes)
					body["classes"] = filters.classes;
				if (filters.profiles)
					body["profiles"] = filters.profiles;
				if (filters.functions)
					body["functions"] = filters.functions;

				body["nbUsersInGroups"] = true;
				body["groupType"] = true;

				var response = await http.post('/communication/visible', body);
				var groups = response.data.groups;
				directory.directory.sortGroups(groups);
				this.load(groups);

				if(typeof callback === 'function'){
					callback();
				}
			}
		}),
		this.collection(directory.Favorite, {
			match: function() {
				return this.all;
			},
			getAll: async function() {
				var response = await http.get('/directory/sharebookmark/all');
				this.load(response.data);
			}
		})
	},
	usersMatch: function(search){
		var searchTerm = lang.removeAccents(search).toLowerCase();
		if(!searchTerm){
			return this.all;
		}
		return _.filter(this.all, function(user){
			var testDisplayName = '', testNameReversed = '', testFullName = '', testFullNameReversed = '';
			if(user.displayName){
				testDisplayName = lang.removeAccents(user.displayName).toLowerCase();
				if(user.displayName.split(' ').length > 0){
					testNameReversed = lang.removeAccents(user.displayName.split(' ')[1] + ' '
						+ user.displayName.split(' ')[0]).toLowerCase();
				}
			}
			if(user.firstName && user.lastName){
				testFullName = lang.removeAccents(user.firstName + ' ' + user.lastName).toLowerCase();
				testFullNameReversed = lang.removeAccents(user.lastName + ' ' + user.firstName).toLowerCase();
			}

			return testDisplayName.indexOf(searchTerm) !== -1 || testNameReversed.indexOf(searchTerm) !== -1
				|| testFullName.indexOf(searchTerm) !== -1 || testFullNameReversed.indexOf(searchTerm) !== -1;
		});
	},
	discoverVisibleUsers: async function(filters){
		var body = {
			search: filters.search.toLowerCase(),
		};
		
		if (filters.structures)
			body["structures"] = filters.structures;
		if (filters.profiles)
			body["profiles"] = filters.profiles;

		
		var response = await http.post('/communication/discover/visible/users', body);
		return response.data;
	},
	discoverVisibleAcceptedProfiles: async function(){
        try {
            var response = await http.get('/communication/discover/visible/profiles');
            return response.data;
		} catch(e) {
            console.debug("[discoverVisibleAcceptedProfiles] Unauthorized", e)
            return [];
        }
	},
	discoverVisibleStructure: async function(){
		var response = await http.get('/communication/discover/visible/structures');
		return response.data;
	},
	discoverVisibleGetGroups: async function(){
		var response = await http.get('/communication/discover/visible/groups');
		return response.data;
	},
	discoverVisibleGetUsersInGroup: async function(groupId){
		var response = await http.get('/communication/discover/visible/group/' + groupId + "/users");
		return response.data;
	},
	discoverVisibleAddCommuteUsers: async function(receiverId){
		var response = await http.post('/communication/discover/visible/add/commuting/'+receiverId);
		return response.data;
	},
	discoverVisibleRemoveCommuteUsers: async function(receiverId){
		var response = await http.delete('/communication/discover/visible/remove/commuting/'+receiverId);
		return response.data;
	},
	discoverVisibleCreateGroup: async function(name){
		var body = {
			name: name,
		};
		var response = await http.post('/communication/discover/visible/group', body);
		return response.data;
	},
	discoverVisibleEditGroup: async function(groupId, name){
		var body = {
			name: name
		};
		var response = await http.put('/communication/discover/visible/group/'+groupId, body);
		return response.data;
	},
	discoverVisibleAddUserToGroup: async function(groupId, oldUsersId, newUsers){

		var newUsersId = [];
		newUsers.forEach(user => {
			newUsersId.push(user.id);
		});

		var body = {
			oldUsers: oldUsersId,
			newUsers: newUsersId
		};

		var response = await http.put('/communication/discover/visible/group/'+groupId+'/users', body);
		return response.data;
	},
	trackEvent: async function(eventType: String){
		// Track this event.
		if (eventType && eventType.length > 0) {
			const eventJson: any = {
				"event-type": eventType,
			};
		
			try {
				await http.post("/infra/event/web/store", eventJson);
				
			} catch (e) {
				console.debug("[TrackingInternal] failed to trackEvent: ", e);
			}
		}
		
	}

}

directory.User.prototype.saveUserbook = function(){
	for(var i = 0; i < this.hobbies.length; i++)
		if(this.hobbies[i].values === undefined)
			this.hobbies[i].values = ""

	oldHttp().putJson('/directory/userbook/' + this.id, {
		health: this.health,
		hobbies: this.hobbies,
		picture: this.picture
	});
};

directory.User.prototype.saveUserbookProperty = function(prop){
	var data = {} as any;
	data[prop] = this[prop];
	if(prop === 'mood'){
		data.mood = data.mood.id;
	}
	oldHttp().putJson('/directory/userbook/' + this.id, data)
	.done(function(){
		if (prop === 'motto') {
			notify.success(lang.translate('userBook.motto.success'));
		}
	})
};

directory.User.prototype.saveInfos = function(){
	var userData = {
		displayName: this.displayName,
		firstName: this.firstName,
		lastName: this.lastName,
		address: this.address,
		email: this.email,
		homePhone: this.homePhone,
        mobile: this.mobile
	} as any;

	if (this.birthDate) userData.birthDate = moment(this.birthDate).format('YYYY-MM-DD');
	if(this.type === 'Relative'){
		userData.childrenIds = _.map(this.relatives, function(user){
			return user.id;
		});
	}
	return oldHttp().putJson('/directory/user/' + this.id, userData);
};

directory.User.prototype.saveLogin = function(newLoginAlias) {
	return oldHttp().putJson('/directory/user/' + this.id, {loginAlias: newLoginAlias});
}

directory.User.prototype.saveChanges = function(){
	if(this.edit.userbook){
		this.saveUserbook();
	}
	if(this.edit.infos){
		this.saveInfos();
	}
};

directory.User.prototype.saveAccount = function(cb){
	var accountData = {
		lastName : this.lastName,
		firstName: this.firstName,
		type: this.type,
		birthDate: moment(this.birthDate).format('YYYY-MM-DD')
	} as any;
	if(this.type === 'Relative'){
		accountData.childrenIds = _.map(this.relatives, function(user){
			return user.id;
		});
	}
	oldHttp().postJson('/directory/class/' + model.me.preferences.selectedClass + '/user', accountData).done(function(data){
		this.updateData(data);
		if(typeof cb === 'function'){
			cb();
		}
	}.bind(this));
};

directory.User.prototype.thumbs = "";

directory.User.prototype.loadUserbook = async function(){
	this.pictureVersion = 0;

	var data = (await http.get('/directory/userbook/' + this.id)).data;
	if(this.type){
		data.type = this.type;
	}
	if(!data.mood){
		data.mood = 'default';
	}
	if(data.picture === 'no-avatar.jpg' || data.picture === 'no-avatar.svg'){
		data.picture = '';
	}
	var mood = _.findWhere(directory.User.prototype.moods, { id: data.mood });
	if(mood == null)
		mood = _.findWhere(directory.User.prototype.moods, { id: "default" });
	data.mood = {
		id: mood.id,
		icon: mood.icon,
		text: mood.text
	}
	if(this.edit && this.edit.visibility){
		this.loadVisibility();
	}
	this.updateData(data);
};

directory.User.prototype.loadVisibility = function(){
	oldHttp().get('/userbook/api/person').done(function(data){
		model.me.email = data.result[0].email;
		this.updateData({
			schoolName: data.result[0].schools[0].name,
			hobbies: data.result[0].hobbies,
			visible: {
				email: data.result[0].visibleInfos.indexOf("SHOW_EMAIL") !== -1 ? "public" : "prive",
				mail: data.result[0].visibleInfos.indexOf("SHOW_MAIL") !== -1 ? "public" : "prive",
				phone: data.result[0].visibleInfos.indexOf("SHOW_PHONE") !== -1 ? "public" : "prive",
				birthdate: data.result[0].visibleInfos.indexOf("SHOW_BIRTHDATE") !== -1 ? "public" : "prive",
				health: data.result[0].visibleInfos.indexOf("SHOW_HEALTH") !== -1 ? "public" : "prive",
				mobile: data.result[0].visibleInfos.indexOf("SHOW_MOBILE") !== -1 ? "public" : "prive"
			}
		});
	}.bind(this));
};

directory.User.prototype.visibleUser = async function() : Promise<boolean>{
	try {
		await http.get('/directory/user/' + this.id);
		return true;
	} catch (e) {		
		return false;
	}
};

directory.User.prototype.loadInfos = async function(){
	var data = (await http.get('/directory/user/' + this.id)).data;
	var adminStructure, adml;

	if(this.edit && this.edit.visibility && !this.edit.userbook){
		this.loadVisibility();
	}
	data.attachedStructures = [];

	if (data.administrativeStructures !== undefined) {
		adminStructure = _.findWhere(this.schools, {id: data.administrativeStructures[0].id})
		if (adminStructure) {
			adminStructure.admin = true;
			data.attachedStructures.push(adminStructure);
		}
	}

	if (data.functions !== undefined) {
		if (data.functions[0][1]) {
			data.functions[0][1].forEach(id => {
				adml = _.findWhere(this.schools, {id: id});
				if (adml) {
					adml.adml = true;
					if (!adminStructure || (adminStructure && adminStructure.id !== adml.id)) {
						data.attachedStructures.push(adml);
					}
				}
			});
		}	
	}
	
	this.schools.forEach(structure => {
		if (!_.findWhere(data.attachedStructures, {id: structure.id})) {
			data.attachedStructures.push(structure);
		}
	});

	this.updateData(data);
	this.trigger('loadInfos');
};

directory.User.prototype.load = async function(){
	await this.loadInfos();
	if(this.edit.userbook){
		await this.loadUserbook();
	}
	if(model.me.federated){
		directory.account.on('loadInfos', this.loadFederatedAddress);
	}
};

directory.User.prototype.loadFederatedAddress = function(){
	if(model.me.federated){
		oldHttp().get('/directory/conf/public').done(function(conf){
			this.federatedAddress = conf.federatedAddress[directory.account.federatedIDP];
			this.disabledFederatedAdress = conf.disabledFederatedAdress;
			this.trigger('change');
		}.bind(this))
	}
};

directory.User.prototype.uploadAvatar = function(){
	var form = new FormData();
	form.append("image", this.photo[0]);
	oldHttp()
		.putFile("/directory/avatar/" + this.id + "?" + directory.User.prototype.thumbs, form, { requestName: 'photo'})
		.done(function(data){
			this.updateData({
				picture: data.picture
			});
			this.pictureVersion++;
			ui.updateAvatar();
		}.bind(this));
};

directory.User.prototype.toString = function(){
	if(this.displayName){
		return this.displayName;
	}
	if(this.firstName && this.lastName){
		return this.firstName + ' ' + this.lastName;
	}
};

directory.User.prototype.removeRelative = function(relative){
	this.relatives = _.reject(this.relatives, function(user){
		return user.id === relative.id;
	});
};

directory.User.prototype.generateMergeKey = function() {
	oldHttp().get("/directory/duplicate/user/mergeKey").done(function(data) {
		this.mergeKey = data.mergeKey;
        this.trigger('change');
	}.bind(this));
};

directory.User.prototype.mergeByKeys = function(keys, handler) {
	oldHttp().postJson("/directory/duplicate/user/mergeByKey", { mergeKeys : keys })
	.error(function(error)
	{
		if(error != null && error.responseJSON != null && error.responseJSON.error != null)
			notify.error(error.responseJSON.error);
		else
			notify.error("invalid.merge.keys");
		if(typeof handler === 'function') {
			handler(false);
		}
	})
	.done(function(data) {
		this.mergedLogins = data.mergedLogins;
        this.trigger('change');
        if(typeof handler === 'function') {
		handler(true);
        }
	}.bind(this));
};

directory.User.prototype.extractPositionNames = function() {
	return this.positionNames 
		? this.positionNames
		: Array.isArray(this.userPositions) 
			? this.userPositions.map(function(pos) {return pos.name;})
			: [];
};

directory.User.prototype.moods = ["default"];

model.build = function(){
	this.makeModels(directory);
	directory.directory = new directory.Directory();
	directory.favoriteForm = new directory.Directory();
	directory.classAdmin = new directory.ClassAdmin();
	directory.network = new directory.Network();

	if(window.location.href.indexOf('mon-compte') === -1){
		oldHttp().get('/userbook/api/person').done(function(data){
			model.me.email = data.result[0].email;
		});
	}

	var mood_map = function(mood){
		return {
			icon: mood,
			text: lang.translate('userBook.mood.' + mood),
			id: mood
		}
	};

	directory.User.prototype.moods = _.map(directory.User.prototype.moods, mood_map);
	oldHttp().get("/directory/userbook/moods").done(function(data){
		directory.User.prototype.moods = _.map(data , mood_map);
	});
};
