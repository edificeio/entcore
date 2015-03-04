// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.


/// Utilities ///
var hookCheck = function(hook){
    if(typeof hook === 'function')
        hook()
}

function Launcher(countdown, action){
	return {
		count: countdown,
		launch: function(){
			if(!--this.count){
				action()
			}
		}
	}
}
////////////////

function User(data){
	this.toString = function(){
		return (this.displayName || '') + (this.name || '');
	};

	this.findData = function(cb){
		var that = this;
		http().get('/userbook/api/person?id=' + this.id).done(function(userData){
			that.updateData({ id: that.id, displayName: userData.result[0].displayName });
			if(typeof cb === "function"){
				cb.call(that, userData.result[0]);
			}
		})
	}
}

User.prototype.mapUser = function(displayNames, id){
	return _.map(_.filter(displayNames, function(user){
		return user[0] === id;
	}), function(user){
		return new User({ id: user[0], displayName: user[1] });
	})[0];
};

function Mail(data){
	this.sentDate = function(){
		return moment(parseInt(this.date)).calendar();
	};

	this.longDate = function(){
		return moment(parseInt(this.date)).format('dddd DD MMMM YYYY')
	};

	this.sender = function(){
		var that = this;
		return User.prototype.mapUser(this.displayNames, this.from);
	};

	this.map = function(id){
		if(id instanceof User){
			return id;
		}
		return User.prototype.mapUser(this.displayNames, id);
	};

	this.saveAsDraft = function(){
		var that = this;
		var data = { subject: this.subject, body: this.body };
		data.to = _.pluck(this.to, 'id');
		data.cc = _.pluck(this.cc, 'id');
		if(!data.subject){
			data.subject = lang.translate('nosubject');
		}
		var path = '/conversation/draft';
		if(this.id){
			http().putJson(path + '/' + this.id, data).done(function(newData){
				that.updateData(newData);
				model.folders.draft.mails.refresh();
			});
		}
		else{
			http().postJson(path, data).done(function(newData){
				that.updateData(newData);
				model.folders.draft.mails.refresh();
			});
		}
	};

	this.send = function(cb){
		var data = { subject: this.subject, body: this.body };
		data.to = _.pluck(this.to, 'id');
		data.cc = _.pluck(this.cc, 'id');
		var path = '/conversation/send?';
		if(!data.subject){
			data.subject = lang.translate('nosubject');
		}
		if(this.id){
			path += 'id=' + this.id + '&';
		}
		if(this.parentConversation){
			path += 'In-Reply-To=' + this.parentConversation.id;
		}
		http().postJson(path, data).done(function(result){
			model.folders.outbox.mails.refresh();
			model.folders.draft.mails.refresh();
			if(typeof cb === 'function'){
				cb(result);
			}
		});
	};

	this.open = function(cb){
		var that = this;

		this.unread = false;
		http().getJson('/conversation/message/' + this.id).done(function(data){
			that.updateData(data);
			that.to = _.map(that.to, function(user){
				var n = '';
				var foundUser = _.find(that.displayNames, function(name){
					return name[0] === user;
				});
				if(foundUser){
					n = foundUser[1];
				}
				return new User({
					id: user,
					displayName: n
				})
			});

			that.cc = _.map(that.cc, function(user){
				return new User({
					id: user,
					displayName: _.find(that.displayNames, function(name){
						return name[0] === user;
					})[1]
				})
			});

			model.folders.inbox.countUnread();
			model.folders.current.mails.refresh();

			if(typeof cb === 'function'){
				cb();
			}
		});
	};

	this.remove = function(){
		if(model.folders.current.folderName !== 'trash'){
			http().put('/conversation/trash?id=' + this.id).done(function(){
				model.folders.current.mails.refresh();
				model.folders.trash.mails.refresh();
			});
		}
		else{
			http().delete('/conversation/delete?id=' + this.id).done(function(){
				model.folders.trash.mails.refresh();
			});
		}
	};

	this.removeFromFolder = function(){
		return http().put('move/root?id=' + this.id)
	}

	this.move = function(destinationFolder){
		http().put('move/userfolder/' + destinationFolder.id + '?id=' + this.id).done(function(){
			model.folders.current.mails.refresh();
		});
	}

	this.trash = function(){
		http().put('/conversation/trash?id=' + this.id ).done(function(){
			model.folders.current.mails.refresh();
		});
	}
}

function Folder(api){
	var thatFolder = this
	this.pageNumber = 0;

	this.collection(Mail, {
		refresh: function(){
			this.pageNumber = 0;
			return this.sync();
		},
		sync: function(pageNumber, emptyList){
			if(!pageNumber){
				pageNumber = 0;
			}
			var that = this;
			return http().get(this.api.get + '?page=' + pageNumber).done(function(data){
				data.sort(function(a, b){ return b.date - a.date})
				if(emptyList === false){
					that.addRange(data);
					if(data.length === 0){
						that.full = true;
					}
				}
				else{
					that.load(data);
				}
			});
		},
		removeMails: function(){
			http().put('/conversation/trash?' + http().serialize({ id: _.pluck(this.selection(), 'id') })).done(function(){
				model.folders.trash.mails.refresh();
			});
			this.removeSelection();
		},
		moveSelection: function(destinationFolder){
			http().put('move/userfolder/' + destinationFolder.id + '?' + http().serialize({ id: _.pluck(this.selection(), 'id') })).done(function(){
				model.folders.current.mails.refresh();
			});
		},
		api: api
	});

	this.nextPage = function(){
		if(!this.mails.full){
			this.pageNumber++;
			this.mails.sync(this.pageNumber, false);
		}
	};
}

function UserFolder(){
	var thatFolder = this
	this.pageNumber = 0;

	this.collection(UserFolder, {
		sync: function(hook){
			var that = this
			http().get('folders/list?parentId='+thatFolder.id).done(function(data){
				_.forEach(data, function(item){
					item.parentFolder = thatFolder
				})
				that.load(data)
				that.forEach(function(item){
					item.userFolders.sync()
				})
				hookCheck(hook)
			})
		}
	})

	this.collection(Mail, {
		refresh: function(){
			this.pageNumber = 0;
			this.sync();
		},
		sync: function(pageNumber, emptyList){
			if(!pageNumber){
				pageNumber = 0;
			}
			var that = this;
			http().get('/conversation/list/' + thatFolder.id + '?restrain=&page=' + pageNumber).done(function(data){
				data.sort(function(a, b){ return b.date - a.date})
				if(emptyList === false){
					that.addRange(data);
					if(data.length === 0){
						that.full = true;
					}
				}
				else{
					that.load(data);
				}
			});
		},
		removeMails: function(){
			http().put('/conversation/trash?' + http().serialize({ id: _.pluck(this.selection(), 'id') })).done(function(){
				model.folders.trash.mails.refresh();
			});
			this.removeSelection();
		},
		moveSelection: function(destinationFolder){
			http().put('move/userfolder/' + destinationFolder.id + '?' + http().serialize({ id: _.pluck(this.selection(), 'id') })).done(function(){
				model.folders.current.mails.refresh();
			});
		},
		removeFromFolder: function(){
			return http().put('move/root?' + http().serialize({ id: _.pluck(this.selection(), 'id') }))
		}
	});

	this.nextPage = function(){
		if(!this.mails.full){
			this.pageNumber++;
			this.mails.sync(this.pageNumber, false);
		}
	};
}

UserFolder.prototype.create = function(){
	var json = !this.parentFolderId ? {
		name: this.name
	} : {
		name: this.name,
		parentId: this.parentFolderId
	}

	return http().postJson('folder', json)
}
UserFolder.prototype.update = function(){
	var json = {
		name: this.name
	}
	return http().putJson('folder/' + this.id, json)
}
UserFolder.prototype.trash = function(){
	return http().put('folder/trash/' + this.id)
}
UserFolder.prototype.restore = function(){
	return http().put('folder/restore/' + this.id)
}
UserFolder.prototype.delete = function(){
	return http().delete('folder/' + this.id)
}

model.build = function(){
	this.makeModels([Folder, UserFolder, User, Mail]);

	this.collection(User, {
		sync: function(){
			http().get('/conversation/visible').done(function(data){
				this.addRange(data.groups);
				this.addRange(data.users);
				this.trigger('sync');
			}.bind(this));
		},
		find: function(search, include, exclude){
			var searchTerm = lang.removeAccents(search).toLowerCase();
			if(!searchTerm){
				return [];
			}
			var found = _.filter(
				this.filter(function(user){
					return _.findWhere(include, { id: user.id }) === undefined
				})
				.concat(include), function(user){
					var testDisplayName = '', testNameReversed = '';
					if(user.displayName){
						testDisplayName = lang.removeAccents(user.displayName).toLowerCase();
						testNameReversed = lang.removeAccents(user.displayName.split(' ')[1] + ' '
							+ user.displayName.split(' ')[0]).toLowerCase();
					}
					var testName = '';
					if(user.name){
						testName = lang.removeAccents(user.name).toLowerCase();
					}

					return testDisplayName.indexOf(searchTerm) !== -1 ||
						testNameReversed.indexOf(searchTerm) !== -1 ||
						testName.indexOf(searchTerm) !== -1;
				}
			);
			return _.reject(found, function(element){
				return _.findWhere(exclude, {id :element.id });
			});
		}
	});

	this.collection(Folder, {
		sync: function(){
			if(this.current === null){
				this.current = this.inbox;
			}

			this.current.mails.sync(this.pageNumber);
		},
		openFolder: function(folderName, cb){
			this.current = this[folderName];
			this.current.mails.sync();
			if(this.current.userFolders){
				this.current.userFolders.sync()
			}
		},
		systemFolders: ['inbox', 'draft', 'outbox', 'trash']
	});

	this.folders.systemFolders.forEach(function(folderName){
		model.folders[folderName] = new Folder({
			get: '/conversation/list/' + folderName.toUpperCase()
		});
		model.folders[folderName].folderName = folderName;
	});

	this.folders.draft.saveDraft = function(draft){
		draft.saveAsDraft();
		this.mails.push(draft);
	};

	this.folders.trash.mails.restoreMails = function(){
		http().put('/conversation/restore?' + http().serialize({ id: _.pluck(this.selection(), 'id') }));
		this.removeSelection();
		model.folders.inbox.mails.refresh();
		model.folders.outbox.mails.refresh();
		model.folders.draft.mails.refresh();
	};

	this.folders.trash.mails.removeMails = function(){
		http().delete('/conversation/delete?' + http().serialize({ id: _.pluck(this.selection(), 'id') }));
		this.removeSelection();
	};

	this.folders.trash.collection(UserFolder, {
		sync: function(hook){
			var that = this
			http().get('folders/list?trash=').done(function(data){
				that.load(data)
			})
		}
	})

	this.folders.inbox.countUnread = function(){
		var that = this;
		http().get('/conversation/count/INBOX', { unread: true }).done(function(data){
			that.nbUnread = parseInt(data.count);
		});
	}

	this.folders.inbox.countUnread();

	this.collection(UserFolder, {
		sync: function(hook){
			var that = this
			http().get('folders/list').done(function(data){
				that.load(data)
				that.forEach(function(item){
					item.userFolders.sync()
				})
				hookCheck(hook)
			})
		}
	})
};
