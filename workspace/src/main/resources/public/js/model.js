function buildModel(){
	function RackDocument(data){

	}

	Model.collection(RackDocument, {
		sync: function(){
			var that = this;
			http().get('/workspace/rack/documents').done(function(documents){
				that.load(documents);
			});
		}
	});

	function Document(data){

	}

	Model.collection(Document, {
		filter: 'owner',
		sync: function(){
			var that = this;
			http().get('/workspace/documents').done(function(documents){
				that.load(documents);
			})
		}
	});
}