/**
 * @license Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.html or http://ckeditor.com/license
 */

CKEDITOR.editorConfig = function(config){
	config.title = false;
	config.extraPlugins = 'mathjax,linker';
	config.allowedContent = true;
	config.toolbar = [
		{ name: 'basicstyles', items: [ 'Bold', 'Italic', 'Underline', '-', 'RemoveFormat' ] },
		{ name: 'colors', items: [ 'TextColor', 'BGColor' ] },
		{ name: 'paragraph', items: [ 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock'] },
		{ name: 'links', items: ['Linker','Mathjax'] },
		{ name: 'styles', items: ['Format', 'Font', 'FontSize'] }
	];

	config.linkShowAdvancedTab = false;
	config.linkShowTargetTab = false;
	config.font_names = 'Arial;Times New Roman;Verdana;EcritureA;KGJune;Comic Sans MS';
	config.fontSize_sizes = "8/8px;10/10px;12/12px;14/14px;16/16px;18/18px;20/20px;22/22px;24/24px;26/26px;28/28px;36/36px;48/48px;72/72px";
	config.mathJaxLib = 'https:\/\/cdn.mathjax.org\/mathjax\/2.2-latest\/MathJax.js?config=TeX-AMS_HTML';
};
