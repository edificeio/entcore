function User(data){
	this.updateData(data);
	this.relatives = [];

	this.select = function(){
		var that = this;
		http().get('/userbook/api/person?id=' + this.id + '&type=' + this.type).done(function(data){
			data.result[0].hobbies = _.map(data.result[0].category, function(cat, i){
				return { category: cat, value: data.result[0].values[i] };
			});
			data.result[0].relatives = _.map(data.result, function(item){
				return new User({ displayName: item.relatedName, id: item.relatedId, type: item.relatedType });
			});
			data.result[0].relatives = _.filter(data.result[0].relatives, function(user){
				return user.id !== '';
			});

			that.selected = true;
			that.updateData(data.result[0]);
			model.directory.users.setCurrent(that);
			model.myClass.users.setCurrent(that);
		});
	};

	this.deselect = function(){
		this.selected = false;
		model.directory.users.setCurrent(undefined);
		model.myClass.users.setCurrent(undefined);
	};
}

User.prototype.saveUserbook = function(){
	http().putJson('/directory/userbook/' + this.id, {
		health: this.health,
		hobbies: this.hobbies,
		picture: this.picture
	});
};

User.prototype.saveUserbookProperty = function(prop){
	var data = {};
	data[prop] = this[prop];
	if(prop === 'mood'){
		data.mood = data.mood.id;
	}
	http().putJson('/directory/userbook/' + this.id, data);
}

User.prototype.saveInfos = function(){
	var userData = {
		displayName: this.displayName,
		firstName: this.firstName,
		lastName: this.lastName,
		address: this.address,
		email: this.email,
		homePhone: this.homePhone,
		birthDate: moment(this.birthDate).format('DD/MM/YYYY')
	};
	if(this.type === 'Relative'){
		userData.childrenIds = _.map(this.relatives, function(user){
			return user.id;
		});
	}
	http().putJson('/directory/user/' + this.id, userData);
};

User.prototype.saveVisibility = function(){

};

User.prototype.saveChanges = function(){
	if(this.edit.userbook){
		this.saveUserbook();
	}
	if(this.edit.infos){
		this.saveInfos();
	}
};

User.prototype.saveAccount = function(cb){
	var accountData = {
		lastName : this.lastName,
		firstName: this.firstName,
		type: this.type,
		birthDate: moment(this.birthDate).format('DD/MM/YYYY')
	};
	if(this.type === 'Relative'){
		accountData.childrenIds = _.map(this.relatives, function(user){
			return user.id;
		});
	}
	http().postJson('/directory/class/' + model.me.classes[0] + '/user', accountData).done(function(data){
		this.updateData(data);
		if(typeof cb === 'function'){
			cb();
		}
	}.bind(this));
};

User.prototype.thumbs = "thumbnail=290x290&thumbnail=82x82&thumbnail=48x48&thumbnail=100x100";
User.prototype.moods = ['default', 'happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];

User.prototype.loadUserbook = function(){
	this.pictureVersion = 0;

	http().get('/directory/userbook/' + this.id).done(function(data){
		if(this.type){
			data.type = this.type;
		}
		if(!data.mood){
			data.mood = 'default';
		}
		data.mood = _.findWhere(User.prototype.moods, { id: data.mood });
		data.photo = {};
		if(this.edit.visibility){
			this.loadVisibility();
		}
		this.updateData(data);
	}.bind(this));
};

User.prototype.loadVisibility = function(){
	http().get('/userbook/api/person').done(function(data){
		this.updateData({
			schoolName: data.result[0].schoolName,
			hobbies: _.map(this.hobbies, function(hobby, index){
				hobby.visibility = data.result[0].visibility[index];
				return hobby;
			}),
			visible: {
				email: data.result[0].visibleInfos.indexOf("SHOW_EMAIL") !== -1 ? "public" : "prive",
				mail: data.result[0].visibleInfos.indexOf("SHOW_MAIL") !== -1 ? "public" : "prive",
				phone: data.result[0].visibleInfos.indexOf("SHOW_PHONE") !== -1 ? "public" : "prive",
				birthdate: data.result[0].visibleInfos.indexOf("SHOW_BIRTHDATE") !== -1 ? "public" : "prive",
				health: data.result[0].visibleInfos.indexOf("SHOW_HEALTH") !== -1 ? "public" : "prive"
			}
		});
	}.bind(this));
};

User.prototype.loadInfos = function(){
	http().get('/directory/user/' + this.id).done(function(data){
		if(this.edit.visibility && !this.edit.userbook){
			this.loadVisibility();
		}
		this.updateData(data);
	}.bind(this));
};

User.prototype.load = function(){
	this.loadInfos();
	if(this.edit.userbook){
		this.loadUserbook();
	}
};

User.prototype.uploadAvatar = function(){
	var form = new FormData();
	form.append("image", this.photo[0]);
	http()
		.putFile("/directory/avatar/" + this.id + "?" + User.prototype.thumbs, form, { requestName: 'photo'})
		.done(function(data){
			this.updateData({
				picture: data.picture
			});
			this.pictureVersion++;
			ui.updateAvatar();
		}.bind(this));
};

User.prototype.toString = function(){
	if(this.displayName){
		return this.displayName;
	}
	if(this.firstName && this.lastName){
		return this.firstName + ' ' + this.lastName;
	}
};

User.prototype.removeRelative = function(relative){
	this.relatives = _.reject(this.relatives, function(user){
		return user.id === relative.id;
	});
};

function usersMatch(search){
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
}

function MyClass(){
	this.name = '';

	this.collection(User, {
		sync: function(){
			var that = this;
			http().get('/userbook/api/class').done(function(data){
				that.load(_.map(data.result, function(user){
					if(!user.mood){
						user.mood = 'default';
					}
					return user;
				}));
			});
		}
	});

	this.users.match = usersMatch.bind(this.users);
}

function Classroom(){
	this.collection(User, {

	});

	this.users.match = usersMatch.bind(this.users);
}

function School(){
	this.collection(User, {

	});

	this.users.match = usersMatch.bind(this.users);

	this.collection(Classroom, {
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
}

function EntProject(){
	this.collection(School, {

	});
}

function Directory(){
	this.collection(User, {
		match: function(){
			return this.all;
		},
		searchDirectory: function(search){
			var that = this;
			var searchTerm = lang.removeAccents(search).toLowerCase();
			this.loading = true;
			http().get('/userbook/api/search?name=' + searchTerm).done(function(result){
				that.loading = false;
				that.load(_.map(result, function(user){
					if(!user.mood){
						user.mood = 'default';
					}
					return user;
				}));
			});
		}
	})
}

function ClassAdmin(){
	this.sync = function(){
		http().get('/directory/class/' + model.me.classes[0]).done(function(data){
			this.id = model.me.classes[0];
			this.updateData(data);
		}.bind(this));
		this.users.sync();
	};

	this.saveClassInfos = function(){
		http().putJson('/directory/class/' + this.id, { name: this.name, level: this.level })
	};

	model.directory.users.searchDirectory('');

	this.collection(User, {
		sync: function(){
			http().get('/directory/class/' + model.me.classes[0] + '/users', { requestName: 'loadingUsers' }).done(function(data){
				data.sort(function(a, b) {
					return a.lastName > b.lastName?1:-1;
				});
				this.load(data);
			}.bind(this));
		}
	});

	this.users.match = usersMatch.bind(this.users);

	this.importFile = function(file, type){
		var form = new FormData();
		form.append('file', file);
		http().postFile('/directory/csv/' + type + '/class/' + this.id, form)
			.done(function(){
				this.sync();
			}.bind(this))
			.e400(function(e){
				this.sync()
				var error = JSON.parse(e.responseText).message;
				var errWithIdx = error.split(/\s/);
				if (errWithIdx.length === 2) {
					notify.error(lang.translate(errWithIdx[0]) + errWithIdx[1]);
				} else {
					notify.error(error);
				}
			}.bind(this));
	}

	this.addUser = function(user){
		user.saveAccount(function(){
			model.classAdmin.sync();
			model.directory.sync();
		});
	};

	this.grabUser = function(user){
		http().put('/directory/class/' + this.id + '/add/' + user.id).done(function(){
			model.classAdmin.sync();
		});
	};

	this.blockUsers = function(value){
		this.users.selection().forEach(function(user){
			user.blocked = value;
			http().putJson('/auth/block/' + user.id, { block: value });
		});
	};

	this.resetPasswords = function(){
		this.users.selection().forEach(function(user){
			http().post('/auth/sendResetPassword', {
				login: user.login,
				email: model.me.email
			});
		});
	}
}

model.build = function(){
	this.makeModels([User, MyClass, Directory, ClassAdmin, EntProject, Classroom, School]);
	this.myClass = new MyClass();
	this.directory = new Directory();
	this.classAdmin = new ClassAdmin();
	this.collection(Project, {
		sync: function(){
			http().get('/directory/public/json/schools.json').done(function(projects){
				this.load(projects);
			}.bind(this));
		}
	});

	http().get('/userbook/api/person').done(function(data){
		model.me.email = data.result[0].email;
	});

	User.prototype.moods = _.map(User.prototype.moods, function(mood){
		return {
			icon: mood,
			text: lang.translate('userBook.mood.' + mood),
			id: mood
		}
	});
};
