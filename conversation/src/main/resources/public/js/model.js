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