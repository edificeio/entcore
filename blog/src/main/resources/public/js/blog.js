var views = {
	"createBlog":{
		"path":"public/template/create-blog.html",
		"allow":true
	},
	"editBlog":{
		"path":"public/template/edit-blog.html",
		"allow":true
	},
	"createPost":{
		"path":"public/template/create-post.html",
		"allow":true
	},
	"editPost":{
		"path":"public/template/edit-post.html",
		"allow":true
	},
	"lastPosts":{},
	"displayBlog":{}
}

function Blog($scope, http, lang, date, notify){
	$scope.blogs = [];
	$scope.currentBlog = {};
	$scope.currentPostId = '';
	$scope.dataToEdit = {};

	$scope.currentView = '';
	$scope.commentFormPath = '';

	http.get('public/mock/mock-blogs-list.json')
		.done(function(data){
			$scope.blogs = data;
			$scope.$apply();
		});

	$scope.displayLastPosts = function(){
		http.get('public/mock/mock-last-posts.json')
		.done(function(data){
			$scope.currentBlog = data;
			$scope.dataToEdit = '';
			$scope.currentView= views.lastPosts;
			$scope.$apply();
		});
	}
	$scope.displayLastPosts();
	
	$scope.displayBlog = function(id){
		http.get('public/mock/mock-blog-' + id + '.json')
			.done(function(data){
				$scope.currentBlog = data;
				$scope.dataToEdit = '';
				$scope.currentView= views.displayBlog;
				$scope.$apply();
			});
	};

	$scope.isSelected = function(id){
		if ($scope.currentView !== views.lastPosts){
			return ($scope.dataToEdit.id === undefined) 
				? (id == $scope.currentBlog.id) : (id == $scope.dataToEdit.id);
		} else {
			return false;
		}
	}
	$scope.isVisible = function(){
		return ($scope.currentView !== views.createBlog 
			&& $scope.currentView !== views.lastPosts 
			&& $scope.currentView !== views.editBlog);
	}
	$scope.isCurrentView = function(name){
		return ($scope.currentView == views[name]);
	}

	$scope.showCreatePost = function(){
		$scope.currentView = views.createPost;
	}
	$scope.showCreateBlog= function(){
		$scope.currentBlog = '';
		$scope.dataToEdit = '';
		$scope.currentView = views.createBlog;
	}
	$scope.showEditBlog = function(id){
		http.get('public/mock/mock-blog-' + id + '.json')
			.done(function(data){
				$scope.dataToEdit = data;
				$scope.currentBlog = '';
				$scope.currentView= views.editBlog;
				$scope.$apply();
			});
	}
	$scope.showEditPost = function(id){
		http.get('public/mock/mock-post-' + id + '.json')
			.done(function(data){
				$scope.dataToEdit = data;
				$scope.currentView= views.editPost;
				$scope.$apply();
			});
	}
	$scope.showCommentPost = function(id){
		$scope.commentFormPath = "public/template/comment-post.html";
		$scope.currentPostId = id;
	}
	$scope.hideCommentForm = function(){
		$scope.commentFormPath = "";
	}

	$scope.createPost = function(){};
	$scope.createBlog = function(){};

}