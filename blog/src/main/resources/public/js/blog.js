function Blog($scope, http, lang, date, notify){
	$scope.blogs = [];
	$scope.currentBlog = {};

	$scope.createPostPath = '';

	http.get('public/mock/mock-blogs-list.json')
		.done(function(data){
			$scope.blogs = data;
			$scope.$apply();
		});

	http.get('public/mock/mock-last-posts.json')
		.done(function(data){
			$scope.currentBlog = data;
			$scope.$apply();
		});

	$scope.displayBlog = function(id){
		http.get('public/mock/mock-blog-' + id + '.json')
			.done(function(data){
				$scope.currentBlog = data;
				$scope.$apply();
			});
	};
	$scope.isSelected = function(id){
		return (id === $scope.currentBlog.id) ? true : false;
	}

	$scope.showCreatePost = function(){
		$scope.createPostPath = ($scope.createPostPath === '') ? 'public/template/create-post.html' : '';
	}

	$scope.createPost = function(){};
	$scope.createBlog = function(){};

}