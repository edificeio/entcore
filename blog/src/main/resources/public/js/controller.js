var views = {
	"createBlog":{
		"path":"/blog/public/template/create-blog.html"
	},
	"listPosts": {
		path: "/blog/public/template/list-posts.html"
	},
	"editBlog":{
		"path":"/blog/public/template/edit-blog.html"
	},
	"createPost":{
		"path":"/blog/public/template/create-post.html"
	},
	"editPost":{
		"path":"/blog/public/template/edit-post.html"
	},
	'viewPost': {
		path: '/blog/public/template/view-post.html'
	},
	'viewSubmitted': {
		path: '/blog/public/template/view-submitted.html'
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

function Blog($scope, http, date, _, ui, lang, notify){
	$scope.translate = lang.translate;

	$scope.blogs = [];
	$scope.currentBlog = undefined;
	$scope.currentPost = {};

	$scope.me = {};

	$scope.currentView = '';
	$scope.commentFormPath = '';
	$scope.notify = notify;

	$scope.displayOptions = {
		showAll: false,
		showPosts: true,
		showSubmitted: false,
		showDrafts: false
	};

	function blogRights(blog){
		blog.myRights = {
			comment: {
				post: true,
				remove: true
			},
			post: {
				post: true,
				remove: true,
				edit: true,
				publish: true
			},
			blog: {
				edit: true
			},
			manager: true
		}
		var ownerRights = _.where(blog.shared, { userId: $scope.me.userId, manager: true });

		if(ownerRights.length > 0){
			return;
		}
		var currentSharedRights = _.filter(blog.shared, function(sharedRight){
			if(!$scope.me.profilGroupsIds){
				return false;
			}
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
		blog.myRights.post.publish = setRight('edu-one-core-blog-controllers-PostController|publish');
		blog.myRights.blog.edit = setRight('manager');
		blog.myRights.manager = setRight('manager');
	}

	function shortenedTitle(title){

		var shortened = title || '';
		console.log(shortened);
		if(shortened.length > 40){
			shortened = shortened.substr(0, 38) + '...';
		}
		return shortened;
	}

	function refreshBlogList(callback){
		http.get('blog/list/all')
			.done(function(data){
				data.forEach(function(blog){
					blogRights(blog);
					blog.shortened = shortenedTitle(blog.title);
				});
				$scope.blogs = data;
				if(typeof callback === 'function'){
					callback(data);
				}
				var sp = window.location.href.split('blog=');
				if(!$scope.currentBlog && $scope.blogs.length > 0){
					if(sp.length > 1 && _.where($scope.blogs, { _id: sp[1] }).length > 0){
						$scope.currentBlog = _.where($scope.blogs, { _id: sp[1] })[0];
					}
					else if(sp.length > 1 && !_.where($scope.blogs, { _id: sp[1] }).length){
						$scope.currentBlog = $scope.blogs[0];
						notify.error('notfound');
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
		refreshBlogList();
		$scope.$apply();
	})

	$scope.defaultView = function(){
		$scope.currentView = views.listPosts;
		refreshBlogList();
	};

	$scope.showEverything = function(post){
		post.showEverything = true;
	}

	$scope.currentBlogView  = function(){
		$scope.currentView = '';
		$scope.displayBlog($scope.currentBlog);
	}

	$scope.seeMore = function(){
		var slots = 0;
		if(!$scope.currentBlog){
			return false;
		}
		if($scope.currentBlog.posts && ($scope.displayOptions.showAll || $scope.displayOptions.showPosts)){
			slots += $scope.currentBlog.posts.length;
		}
		if($scope.currentBlog.drafts && ($scope.displayOptions.showAll || $scope.displayOptions.showDrafts)){
			slots += $scope.currentBlog.drafts.length;
		}
		if($scope.currentBlog.submitted && ($scope.displayOptions.showAll || $scope.displayOptions.showSubmitted)){
			slots += $scope.currentBlog.submitted.length;
		}
		return $scope.maxResults < slots;
	};

	$scope.defaultView();

	$scope.publish = function(post){
		if($scope.currentBlog.myRights.manager){
			post.state = 'PUBLISHED';
			http.put('/blog/post/publish/' + $scope.currentBlog._id + '/' + post._id);
		}
		else{
			$scope.submit(post);
		}
	};

	$scope.saveAndSubmit = function(post){
		$scope.updatePost(post);
		$scope.publish(post);

		$scope.displayBlog($scope.currentBlog);
	};

	$scope.submit = function(post){
		post.state = 'SUBMITTED';
		http.put('/blog/post/submit/' + $scope.currentBlog._id + '/' + post._id);
	};

	$scope.displayBlog = function(blog){
		if(!blog){
			return;
		}
		resetScope();
		$scope.currentBlog = blog;
		http.get('/blog/post/list/all/' + blog._id).done(function(data){
			$scope.currentBlog.posts = data;
			$scope.currentView= views.listPosts;
			initMaxResults();
			$scope.$apply();
		});

		http.get('/blog/post/list/all/' + blog._id + '?state=SUBMITTED').done(function(data){
			$scope.currentBlog.submitted = data;
			initMaxResults();
			$scope.$apply();
		});

		http.get('/blog/post/list/all/' + blog._id + '?state=DRAFT').done(function(data){
			$scope.currentBlog.drafts = data;
			initMaxResults();
			$scope.$apply();
		});
	};

	$scope.nbComments = function(post){
		if(!post.comments){
			post.comments = [];
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
	};

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
				blogRights(data);
				$scope.currentBlog = data;
				$scope.currentView= views.editBlog;
				$scope.$apply();

			});
	};

	$scope.postTemplate = function(post){
		if(post === $scope.editPost){
			return views.editPost;
		}
		if(post.state === 'SUBMITTED' || post.state === 'DRAFT'){
			return views.viewSubmitted;
		}
		return views.viewPost;
	}

	$scope.showEditPost = function(post){
		$scope.currentPost = post;
		http.get('/blog/post/' + $scope.currentBlog._id + '/' + $scope.currentPost._id + '?state=' + post.state)
			.done(function(data){
				$scope.currentPost = data;
				$scope.editPost = post;
				$scope.$apply();
			});
	};

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
				state: 'SUBMITTED'
			},
			blog: {
				thumbnail: '',
				'comment-type': 'IMMEDIATE',
				description: ''
			},
			comment: {
				comment: ''
			}
		};
		$scope.editPost = undefined;
	}
	resetScope();

	$scope.formatDate = function(dateString){
		return date.format(dateString, 'dddd LL')
	}

	$scope.isEditing = function(){
		return $scope.editPost || ($scope.create.post && $scope.currentView === views.createPost);
	}

	$scope.saveDraft = function(){
		$scope.create.post.state = 'DRAFT';
		http.post('/blog/post/' + $scope.currentBlog._id, $scope.create.post).done(function(createdPost){
			$scope.create.post._id = createdPost._id;
		});
		notify.info('Brouillon enregistré');
	};

	$scope.savePost = function(){
		$scope.create.post.state = 'SUBMITTED';
		if($scope.create.post._id !== undefined){
			$scope.publish($scope.create.post);
			$scope.displayBlog($scope.currentBlog);
		}
		else{
			$scope.createPost(function(newPost){
				$scope.create.post._id = newPost._id;
				$scope.publish($scope.create.post);
				$scope.displayBlog($scope.currentBlog);
			});
		}

	};

	$scope.createPost = function(callback){
		http.post('/blog/post/' + $scope.currentBlog._id, $scope.create.post).done(callback)
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

		http.postFile('/workspace/document?application=blog-newblog&protected=true&thumbnail=100x100&name=' + $scope.photo.file.name, formData)
			.done(function(e){
				blog.thumbnail = '/workspace/document/' + e._id + '?thumbnail=100x100';
				$scope.$apply();
			})
	}

	$scope.updatePost = function(){
		http.put('/blog/post/' + $scope.currentBlog._id + '/' + $scope.currentPost._id, $scope.currentPost).done(function(){
			$scope.displayBlog($scope.currentBlog);
			window.scrollTo(0, 0);
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

	function initMaxResults(){
		$scope.maxResults = 3;
	}
	initMaxResults();
	$scope.addResults = function(){
		$scope.maxResults += 3;
	};

	$scope.nbResults = function(postState){
		var remainingSlots = $scope.maxResults;
		if(!$scope.currentBlog){
			return 0;
		}
		if((postState === 'posts' || postState === 'submitted') &&
			($scope.currentBlog.drafts && ($scope.displayOptions.showDrafts || $scope.displayOptions.showAll))){
			remainingSlots -= $scope.currentBlog.drafts.length;
		}
		if((postState === 'posts') && ($scope.currentBlog.submitted && ($scope.displayOptions.showAll || $scope.displayOptions.showSubmitted))){
			remainingSlots -= $scope.currentBlog.submitted.length;
		}
		if(remainingSlots < 0){
			remainingSlots = 0;
		}
		return remainingSlots;
	}

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
		$scope.sharedResources = [$scope.currentBlog];
		$scope.lightboxPath = '/blog/public/template/share.html'
		ui.showLightbox();

	};

	$scope.updatePublishType = function(){
		http.put('/blog/' + $scope.currentBlog._id, { 'publish-type': $scope.currentBlog['publish-type'] });
	}

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