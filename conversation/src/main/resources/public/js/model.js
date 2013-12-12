function buildModel(){
	function Mail(){

	}

	function Folder(api){
		this.collection(Mail, {
			sync: function(){
				var that = this;
				http().get(this.api.get).done(function(data){
					that.load(data);
					that.trigger('change');
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
		openInbox: function(){
			this.current = this.inbox;
		},
		openOutbox: function(){
			this.current = this.outbox;
		}
	});

	Model.folders.inbox = new Folder({
		get: '/conversation/public/mocks/inbox.json'
	});
	Model.folders.outbox = new Folder({
		get: '/conversation/public/mocks/outbox.json'
	});
}