/**
 * @license Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.html or http://ckeditor.com/license
 */

CKEDITOR.editorConfig = function(config){
	config.removePlugins = 'magicline';
	config.allowedContent = true;
	config.extraPlugins = 'upload,audio';
	config.toolbar = [
		{ name: 'document', groups: [ 'mode', 'document', 'doctools' ], items: ['Templates' ] },
		{ name: 'clipboard', groups: [ 'clipboard', 'undo' ], items: [ 'Undo', 'Redo' ] },
		{ name: 'tools', items: [ 'Maximize', 'ShowBlocks' ] },
		{ name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ], items: [ 'Bold', 'Italic', 'Underline', '-', 'RemoveFormat' ] },
		{ name: 'colors', items: [ 'TextColor', 'BGColor' ] },
		{ name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ], items: [ 'NumberedList', 'BulletedList', '-', 'Blockquote', '-', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock'] },
		{ name: 'links', items: ['Link', 'Unlink'] },
		{ name: 'insert', items: ['Table', 'Smiley', 'SpecialChar'] },
		{ name: 'styles', items: ['Format', 'Font', 'FontSize'] },
		{ name: 'editing', groups: [ 'find', 'selection', 'spellchecker' ], items: [] }
	];

	config.allowedContent = true;

	config.smiley_images= [];
	config.smiley_descriptions= [];

	var moodsPath = $('head')
		.children('link')
		.attr('href')
		.split('/theme.css')[0] + '/../img/icons/';

	config.smiley_path = moodsPath;
	config.smiley_images= ['angry-panda-small.png', 'dreamy-panda-small.png', 'happy-panda-small.png',
		'joker-panda-small.png', 'love-panda-small.png', 'proud-panda-small.png', 'sad-panda-small.png',
		'tired-panda-small.png', 'worried-panda-small.png'];
	config.smiley_descriptions= ['En colère', 'Rêveur', 'Content', 'Farceur', 'Amoureux',
		'Fier', 'Triste', 'Fatigué', 'Embêté'];
	config.templates_replaceContent = false;
};
