function User(data){
	this.updateData(data);

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
		},
		match: function(search){
			var searchTerm = lang.removeAccents(search).toLowerCase();
			if(!searchTerm){
				return this.all;
			}
			return _.filter(this.all, function(user){
				var testDisplayName = '', testNameReversed = '';
				if(user.displayName){
					testDisplayName = lang.removeAccents(user.displayName).toLowerCase();
					testNameReversed = lang.removeAccents(user.displayName.split(' ')[1] + ' '
						+ user.displayName.split(' ')[0]).toLowerCase();
				}

				return testDisplayName.indexOf(searchTerm) !== -1 ||
					testNameReversed.indexOf(searchTerm) !== -1;
			});
		}
	})
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
				that.load(_.map(result.result, function(user){
					if(!user.mood){
						user.mood = 'default';
					}
					return user;
				}));

			});
		}
	})
}

model.build = function(){
	this.makeModels([User, MyClass, Directory]);
	this.myClass = new MyClass();
	this.directory = new Directory();
};
