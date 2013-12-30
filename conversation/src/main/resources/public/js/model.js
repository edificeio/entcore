function User(){
	this.toString = function(){
		return (this.displayName || '') + (this.name || '');
	}
}

function Mail(){
	this.sentDate = function(){
		return moment(parseInt(this.date)).calendar();
	};

	this.saveAsDraft = function(){
		var that = this;
		var data = { subject: this.subject, body: this.body };
		data.to = _.pluck(this.to, 'id');
		http().postJson('/conversation/draft', data);
		Model.folders.draft.mails.refresh();
	};

	this.send = function(){
		var data = { subject: this.subject, body: this.body };
		data.to = _.pluck(this.to, 'id');
		var path = '/conversation/send';
		if(this.id){
			path += '?id=' + this.id;
		}
		http().postJson(path, data);
		Model.folders.outbox.mails.push(this);
	};

	this.open = function(){
		var that = this;
		http().getJson('/conversation/message/' + this.id).done(function(data){
			that.updateData(data);
			Model.folders.current.trigger('mails.change');
		});
	};
}

function Folder(api){
	this.pageNumber = 0;

	this.collection(Mail, {
		refresh: function(){
			this.pageNumber = 0;
			this.sync();
		},
		sync: function(pageNumber, emptyList){
			var that = this;
			http().get(this.api.get + '?page=' + pageNumber).done(function(data){
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
		api: api
	});

	this.nextPage = function(){
		if(!this.mails.full){
			this.pageNumber++;
			this.mails.sync(this.pageNumber, false);
		}
	};
}

function buildModel(){
	Model.collection(User, {
		sync: function(){
			var that = this;
			http().get('/conversation/visible').done(function(data){
				that.addRange(data.groups);
				that.addRange(data.users);
			});
		},
		find: function(search, exclude){
			var searchTerm = lang.removeAccents(search).toLowerCase();
			if(!searchTerm){
				return [];
			}
			var found = _.filter(this.all, function(user){
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
			});
			return _.reject(found, function(element){
				return exclude.indexOf(element) !== -1;
			});
		}
	});

	Model.collection(Folder, {
		sync: function(){
			if(this.current === null){
				this.current = this.inbox;
			}

			this.current.mails.sync(this.pageNumber);
		},
		openFolder: function(folderName){
			this.current = this[folderName];
			if(this.current.mails.all.length === 0){
				this.current.mails.sync();
			}
		},
		systemFolders: ['inbox', 'draft', 'outbox', 'trash']
	});

	Model.folders.systemFolders.forEach(function(folderName){
		Model.folders[folderName] = new Folder({
			get: '/conversation/list/' + folderName.toUpperCase()
		});
	});

	Model.folders.draft.saveDraft = function(mailData){
		var draft = Model.create(Mail, mailData);
		draft.saveAsDraft();
		this.mails.push(draft);
	}
}