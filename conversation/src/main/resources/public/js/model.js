function buildModel(){
	function Mail(){
		this.sentDate = function(){
			return moment(parseInt(this.date)).calendar();
		};

		this.saveAsDraft = function(){
			http().postJson('/conversation/draft', this.data);
		};

		this.open = function(){
			var that = this;
			http().getJson('/conversation/message/' + this.id).done(function(data){
				that.updateData(data);
				Model.folders.current.trigger('mails.change');
			})
		}
	}

	function Folder(api){
		this.collection(Mail, {
			sync: function(){
				var that = this;
				http().get(this.api.get).done(function(data){
					that.load(data);
				})
			},
			api: api
		});
	}

	Model.collection(Folder, {
		sync: function(){
			if(this.current === null){
				this.current = this.inbox;
			}

			this.current.mails.sync();
		},
		openFolder: function(folderName){
			this.current = this[folderName]
			this.current.mails.sync();
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