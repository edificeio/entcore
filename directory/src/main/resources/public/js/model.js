function Test(){

}

function User(data){
	this.updateData(data);

	this.collection(Test, {
		sync: function(){

		}
	})
}

Model.build = function(){
	this.collection(User, {
		sync: function(){
			var that = this;
			http().get('/userbook/api/class').done(function(data){
				that.load(_.map(data.result, function(user){
					return user;
				}));
			});
		}
	})
};