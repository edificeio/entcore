function buildModel(){
	function Tree(){

	}

	Model.collection(Tree, {
		sync: function(){
			this.load([{
				displayName: 'Mes documents'
			}, {
				displayName: 'Mon casier'
			}, {
				displayName: 'Documents partagés avec moi'
			}, {
				displayName: 'Corbeille'
			}])
		}
	})
}