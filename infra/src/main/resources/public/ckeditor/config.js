/**
 * @license Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.html or http://ckeditor.com/license
 */

CKEDITOR.editorConfig = function( config ) {
	config.allowedContent = true;
	config.extraPlugins = 'upload';
	config.toolbar = [
		{ name: 'document', groups: [ 'mode', 'document', 'doctools' ], items: ['Templates' ] },
		{ name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ], items: [ 'Bold', 'Italic', 'Underline', '-', 'RemoveFormat' ] },
		{ name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ], items: [ 'NumberedList', 'BulletedList', '-', 'Blockquote', '-', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock'] },
		{ name: 'links', items: ['Link', 'Unlink'] },
		{ name: 'insert', items: ['Table', 'Smiley', 'SpecialChar', 'Upload'] },
		{ name: 'styles', items: ['Format', 'Font', 'FontSize'] },
		{ name: 'colors', items: [ 'TextColor', 'BGColor' ] },
		{ name: 'tools', items: [ 'Maximize', 'ShowBlocks' ] },

		{ name: 'editing', groups: [ 'find', 'selection', 'spellchecker' ], items: [] }
	];

// Toolbar groups configuration.
	config.toolbarGroups = [
		{ name: 'document', groups: [ 'mode', 'document', 'doctools' ] },
		{ name: 'clipboard', groups: [ 'clipboard', 'undo' ] },
		{ name: 'editing', groups: [ 'find', 'selection', 'spellchecker' ] },
		{ name: 'forms' },
		'/',
		{ name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
		{ name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ] },
		{ name: 'links' },
		{ name: 'insert' },
		'/',
		{ name: 'styles' },
		{ name: 'colors' },
		{ name: 'tools' },
		{ name: 'others' },
		{ name: 'about' }
	];

	config.allowedContent = true;

	config.smiley_images= [];
	config.smiley_descriptions= [];

	var moodsPath = $('head')
		.children('link')
		.attr('href')
		.split('/css')[0] + '/img/icons/';

	config.smiley_path = moodsPath;
	config.smiley_images= ['angry-panda-small.png', 'dreamy-panda-small.png', 'happy-panda-small.png',
	'joker-panda-small.png', 'love-panda-small.png', 'proud-panda-small.png', 'sad-panda-small.png',
	'tired-panda-small.png', 'worried-panda-small.png'];
	config.smiley_descriptions= ['En colère', 'Rêveur', 'Content', 'Farceur', 'Amoureux',
	'Fier', 'Triste', 'Fatigué', 'Embêté'];
};
