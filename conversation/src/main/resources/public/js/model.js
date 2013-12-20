function buildModel(){
	function User(){
	}

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
			var found = _.filter(this.all, function(user){
				if(user.login){
					var testName = lang.removeAccents(user.lastName + ' ' + user.firstName).toLowerCase();
					var testNameReversed = lang.removeAccents(user.firstName + ' ' + user.lastName).toLowerCase();
					var testDisplayName = lang.removeAccents(user.name).toLowerCase();
					return testName.indexOf(searchTerm) !== -1 ||
						testNameReversed.indexOf(searchTerm) !== -1 ||
						testDisplayName.indexOf(searchTerm) !== -1;
				}
			});
			return _.reject(found, function(element){
				return exclude.indexOf(element) !== -1;
			});
		}
	});

	function Mail(){
		this.sentDate = function(){
			return moment(parseInt(this.date)).calendar();
		};

		this.saveAsDraft = function(){
			http().postJson('/conversation/draft', this.data);
		};

		this.send = function(){
			var data = { subject: this.subject, body: this.body };
			data.to = _.pluck(this.to, 'id');
			http().postJson('/conversation/send?id=' + this.data.id, data);
			Model.folders.outbox.mails.push(this);
		}

		this.open = function(){
			var that = this;
			http().getJson('/conversation/message/' + this.id).done(function(data){
				that.updateData(data);
				Model.folders.current.trigger('mails.change');
			})
		}
	}

	function Folder(api){
		this.pageNumber = 0;

		this.collection(Mail, {
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