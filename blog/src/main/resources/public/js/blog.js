var views = {
	"createBlog":{
		"path":"/blog/public/template/create-blog.html",
		"allow":true
	},
	"editBlog":{
		"path":"/blog/public/template/edit-blog.html",
		"allow":true
	},
	"createPost":{
		"path":"/blog/public/template/create-post.html",
		"allow":true
	},
	"editPost":{
		"path":"/blog/public/template/edit-post.html",
		"allow":true
	},
	"lastPosts":{},
	"displayBlog":{}
}

function Blog($scope, http, date, _, ui){
	$scope.blogs = [];
	$scope.currentBlog = {};
	$scope.currentPost = {};

	$scope.currentView = '';
	$scope.commentFormPath = '';

	function refreshBlogList(callback){
		http.get('blog/list/all')
			.done(function(data){
				$scope.blogs = data;
				if(typeof callback === 'function'){
					callback(data);
				}
				$scope.$apply();
			});
	}


	$scope.defaultView = function(){
		$scope.currentView = '';
		refreshBlogList(function(){
			if($scope.blogs.length > 0){
				$scope.displayBlog($scope.blogs[0]);
			}
		});
	};

	$scope.currentBlogView  = function(){
		$scope.currentView = '';
		$scope.displayBlog($scope.currentBlog);
	}

	$scope.defaultView();

	$scope.displayBlog = function(blog){
		$scope.currentBlog = blog;
		http.get('/blog/post/list/all/' + blog._id).done(function(data){
			$scope.currentBlog.posts = data;
			$scope.currentView= views.displayBlog;
			$scope.$apply();
		});
	};

	$scope.nbComments = function(post){
		if(!post.comments){
			http.get('/blog/comments/' + $scope.currentBlog._id + '/' + post._id).done(function(comments){
				post.comments = comments;
				$scope.$apply();
			});
			return '-';
		}
		return post.comments.length;
	}

	$scope.isSelected = function(id){
		return id === $scope.currentBlog._id && $scope.currentView !== views.lastPosts;
	}
	$scope.isVisible = function(){
		return ($scope.currentView !== views.createBlog 
			&& $scope.currentView !== views.lastPosts 
			&& $scope.currentView !== views.editBlog);
	}
	$scope.isCurrentView = function(name){
		return ($scope.currentView == views[name]);
	};

	$scope.switchComments = function(post){
		post.showComments = !post.showComments;
	}

	$scope.showCreatePost = function(){
		$scope.currentView = views.createPost;
	}
	$scope.showCreateBlog= function(){
		$scope.currentBlog = '';
		$scope.currentView = views.createBlog;
	}
	$scope.showEditBlog = function(blog){
		http.get('/blog/' + blog._id)
			.done(function(data){
				$scope.currentBlog = data;
				$scope.currentView= views.editBlog;
				$scope.$apply();

			});
	}
	$scope.showEditPost = function(post){
		$scope.currentPost = post;
		http.get('/blog/post/' + $scope.currentBlog._id + '/' + $scope.currentPost._id)
			.done(function(data){
				$scope.currentPost = data;
				$scope.currentView= views.editPost;
				$scope.$apply();
			});
	}
	$scope.showCommentPost = function(post){
		post.showComments = true;
		$scope.commentFormPath = "/blog/public/template/comment-post.html";
		$scope.currentPost = post;
	}
	$scope.hideCommentForm = function(){
		$scope.commentFormPath = "";
	}

	$scope.create = {
		post: {
			state: 'DRAFT'
		},
		blog: {
			thumbnail: '',
			'comment-type': 'IMMEDIATE',
			'publish-type': 'IMMEDIATE',
			description: ''
		},
		comment: {
			comment: ''
		}
	}

	$scope.formatDate = function(dateString){
		return date.format(dateString, 'dddd LL')
	}

	$scope.createPost = function(){
		http.post('/blog/post/' + $scope.currentBlog._id, $scope.create.post).done(function(newPost){
			http.put('/blog/post/publish/' + $scope.currentBlog._id + '/' + newPost._id).done(function(){
				$scope.displayBlog($scope.currentBlog);
				$scope.$apply();
			})
		})
	};

	$scope.blogThumbnail = function(blog){
		if(blog.thumbnail !== ''){
			return blog.thumbnail;
		}
		return '/blog/public/img/blog.png';
	}

	$scope.photo = { file: undefined }
	$scope.updateBlogImage = function(blog){
		var formData = new FormData();
		formData.append('file', $scope.photo.file);

		http.postFile('/workspace/document?application=blog-newblog&protected=true&name=' + $scope.photo.file.name, formData)
			.done(function(e){
				blog.thumbnail = '/workspace/document/' + e._id;
				$scope.$apply();
			})
	}

	$scope.updatePost = function(){
		http.put('/blog/post/' + $scope.currentBlog._id + '/' + $scope.currentPost._id, $scope.currentPost).done(function(){
			$scope.displayBlog($scope.currentBlog);
		})
	};

	$scope.commentPost = function(){
		http.post('/blog/comment/' + $scope.currentBlog._id + '/' + $scope.currentPost._id, $scope.create.comment).done(function(e){
			http.get('/blog/comments/' + $scope.currentBlog._id + '/' + $scope.currentPost._id).done(function(comments){
				$scope.currentPost.comments = comments;
				$scope.hideCommentForm();
				$scope.$apply();
			})
		})
	};

	$scope.openSharingView = function(){
		ui.showLightbox();
	};

	$scope.removePost = function(post){
		http.delete('/blog/post/' + $scope.currentBlog._id + '/' + post._id).done(function(){
			$scope.displayBlog($scope.currentBlog);
		});
	}

	$scope.saveBlogChanges = function(){
		http.put('/blog/' + $scope.currentBlog._id, $scope.currentBlog).done(function(){
			refreshBlogList(function(){
				$scope.displayBlog($scope.currentBlog);
			});
		})
	}

	$scope.createBlog = function(){
		http.post('/blog', $scope.create.blog)
			.done(function(newBlog){
				refreshBlogList(function(){
					$scope.displayBlog(newBlog);
				});
			});
	};

	$scope.removeBlog = function(){
		http.delete('/blog/' + $scope.currentBlog._id).done(function(){
			refreshBlogList(function(){
				$scope.currentBlog = '';
				$scope.currentView = '';
			})
		})
	}
}