var views = {
	"createBlog":{
		"path":"public/template/create-blog.html",
		"allow":true
	},
	"editBlog":{
		"path":"public/template/create-edit-blog.html",
		"allow":true
	},
	"createPost":{
		"path":"public/template/create-post.html",
		"allow":true
	},
	"editPost":{
		"path":"public/template/create-edit-post.html",
		"allow":true
	},
	"commentBlog":{
		"path":"public/template/comment-post.html",
		"allow":true
	}
}

function Blog($scope, http, lang, date, notify){
	$scope.blogs = [];
	$scope.currentBlog = {};

	$scope.creationFormPath = '';
	$scope.mainContentPath = '';
	$scope.currentView = '';

	http.get('public/mock/mock-blogs-list.json')
		.done(function(data){
			$scope.blogs = data;
			$scope.$apply();
		});

	http.get('public/mock/mock-last-posts.json')
		.done(function(data){
			$scope.currentBlog = data;
			$scope.currentView= views.createPost;
			$scope.$apply();
		});

	$scope.displayBlog = function(id){
		http.get('public/mock/mock-blog-' + id + '.json')
			.done(function(data){
				$scope.currentBlog = data;
				$scope.creationFormPath = '';
				$scope.currentView= '';
				$scope.$apply();
			});
	};

	$scope.isSelected = function(id){
		return (id == $scope.currentBlog.id);
	}
	$scope.isDisabled = function(){
		return ($scope.currentView === views.createPost);
	}

	$scope.showCreatePost = function(){
		$scope.currentView = views.createPost;
		$scope.creationFormPath = $scope.currentView.path;
	}
	$scope.showCreateBlog= function(){
		$scope.currentBlog = '';
		$scope.currentView = views.createBlog,
		$scope.creationFormPath = $scope.currentView.path;
	}

	$scope.createPost = function(){};
	$scope.createBlog = function(){};

}