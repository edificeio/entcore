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

function resolveMyRights(me){
	me.myRights = {
		blog: {
			post: _.where(me.authorizedActions, { name: "edu.one.core.blog.controllers.BlogController|create" }).length > 0
		}
	}
}

function Blog($scope, http, date, _, ui){
	$scope.blogs = [];
	$scope.currentBlog = undefined;
	$scope.currentPost = {};

	$scope.me = {};

	$scope.currentView = '';
	$scope.commentFormPath = '';

	function blogRights(blog){
		blog.myRights = {
			comment: {
				post: true,
				remove: true
			},
			post: {
				post: true,
				remove: true,
				edit: true
			},
			blog: {
				edit: true
			}
		}
		var ownerRights = _.where(blog.shared, { userId: $scope.me.userId, manager: true });

		if(ownerRights.length > 0){
			return;
		}
		var currentSharedRights = _.filter(blog.shared, function(sharedRight){
			return $scope.me.profilGroupsIds.indexOf(sharedRight.groupId) !== -1
				|| sharedRight.userId === $scope.me.userId;
		});

		function setRight(path){
			return _.find(currentSharedRights, function(right){
					return right[path];
				}) !== undefined;
		}

		blog.myRights.comment.post = setRight('edu-one-core-blog-controllers-PostController|comment');
		blog.myRights.comment.remove = setRight('edu-one-core-blog-controllers-PostController|deleteComment');
		blog.myRights.post.post = setRight('edu-one-core-blog-controllers-PostController|submit');
		blog.myRights.post.edit = setRight('edu-one-core-blog-controllers-PostController|update');
		blog.myRights.post.remove = setRight('edu-one-core-blog-controllers-PostController|delete');
		blog.myRights.blog.edit = setRight('manager');
	}

	function refreshBlogList(callback){
		http.get('blog/list/all')
			.done(function(data){
				data.forEach(blogRights);
				$scope.blogs = data;
				if(typeof callback === 'function'){
					callback(data);
				}
				var sp = window.location.href.split('blog=');
				if(!$scope.currentBlog && $scope.blogs.length > 0){
					if(sp.length > 1){
						$scope.currentBlog = _.where($scope.blogs, { _id: sp[1] })[0];
					}
					else{
						$scope.currentBlog = $scope.blogs[0];
					}
				}
				$scope.displayBlog($scope.currentBlog);
				$scope.$apply();
			});
	}

	http.get('/auth/oauth2/userinfo').done(function(data){
		$scope.me = data;
		resolveMyRights($scope.me);
		$scope.$apply();
	})

	$scope.defaultView = function(){
		$scope.currentView = '';
		refreshBlogList();
	};

	$scope.showEverything = function(post){
		post.showEverything = true;
	}

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
		return $scope.currentBlog && ($scope.currentView !== views.createBlog
			&& $scope.currentView !== views.editBlog) && $scope.currentBlog.myRights.post.post;
	}
	$scope.isCurrentView = function(name){
		return ($scope.currentView == views[name]);
	};

	$scope.switchComments = function(post){
		post.showComments = !post.showComments;
	}

	$scope.showCreatePost = function(){
		resetScope();
		$scope.currentView = views.createPost;

	}
	$scope.showCreateBlog= function(){
		$scope.currentBlog = '';
		resetScope();
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
		resetScope()
	}
	$scope.hideCommentForm = function(){
		$scope.commentFormPath = "";
	}

	function resetScope(){
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
	}
	resetScope();

	$scope.formatDate = function(dateString){
		return date.format(dateString, 'dddd LL')
	}

	$scope.createPost = function(){
		http.post('/blog/post/' + $scope.currentBlog._id, $scope.create.post).done(function(newPost){
			http.put('/blog/post/submit/' + $scope.currentBlog._id + '/' + newPost._id).done(function(){
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

	$scope.openConfirmView = function(action, args){
		$scope.lightboxPath = '/blog/public/template/confirm.html';
		$scope.onConfirm = {
			func: function(args){
				ui.hideLightbox();
				action(args);
			},
			args: args
		};
		ui.showLightbox();
	}

	$scope.openSharingView = function(){
		$scope.lightboxPath = '/blog/share/' + $scope.currentBlog._id;
		ui.showLightbox();
		//Small hack until we get final version
		$('body').on('click', '.share input[type=submit]', function(e){
			e.preventDefault();
			http.post($('.share form').attr('action'), $('.share form').serialize()).done(function(){
				ui.hideLightbox();
			})
		})
	};

	$scope.removePost = function(post){
		http.delete('/blog/post/' + $scope.currentBlog._id + '/' + post._id).done(function(){
			$scope.displayBlog($scope.currentBlog);
		});
	};

	$scope.removeComment = function(post, comment){
		http.delete('/blog/comment/' + $scope.currentBlog._id + '/' + post._id + '/' + comment.id).done(function(){
			post.comments = undefined;
			$scope.$apply();
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
					$scope.displayBlog(_.where($scope.blogs, { _id: newBlog._id})[0]);
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