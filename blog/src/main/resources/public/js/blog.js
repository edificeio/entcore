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
	},
	"lastPosts":{},
	"displayBlog":{}
}

function Blog($scope, http, lang, date, notify){
	$scope.blogs = [];
	$scope.currentBlog = {};

	$scope.mainContentPath = '';
	$scope.currentView = '';

	http.get('public/mock/mock-blogs-list.json')
		.done(function(data){
			$scope.blogs = data;
			$scope.$apply();
		});

	$scope.displayLastPosts = function(){
		http.get('public/mock/mock-last-posts.json')
		.done(function(data){
			$scope.currentBlog = data;
			$scope.currentView= views.lastPosts;
			$scope.$apply();
		});
	}
	$scope.displayLastPosts();
	
	$scope.displayBlog = function(id){
		http.get('public/mock/mock-blog-' + id + '.json')
			.done(function(data){
				$scope.currentBlog = data;
				$scope.currentView= views.displayBlog;
				$scope.$apply();
			});
	};

	$scope.isSelected = function(id){
		return (id == $scope.currentBlog.id);
	}
	$scope.isVisible = function(){
		return ($scope.currentView !== views.createBlog && $scope.currentView !== views.lastPosts);
	}
	$scope.isCurrentView = function(name){
		return ($scope.currentView == views[name]);
	}

	$scope.showCreatePost = function(){
		$scope.currentView = views.createPost;
	}
	$scope.showCreateEditBlog= function(){
		$scope.currentBlog = '';
		$scope.currentView = views.createBlog;
	}

	$scope.createPost = function(){};
	$scope.createBlog = function(){};

}