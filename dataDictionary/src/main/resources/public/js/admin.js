var admin = function(){
	var getAndRender = function (pathUrl, templateName){
		$.get(pathUrl)
			.done(function(data) {
				template.render(templateName, data);
			})
			.error(function() { // TODO: Manage error message maybe with human js
				template.render("error");
			});
	};

	var template = {
		render : function (nom, data) {
			template[nom](data);
		},
		error : function () {
			$('#datas').html('<p style="color:red">ERROR</p>')
		},
		dictionary: function(data) {
			var dataAsHtml = '<tr>';
			for(var field in data.personnes[0]) {
				dataAsHtml += '<th>' + field+ '</th>';
			}
			dataAsHtml += '</tr>';

			for(var i = 1; i < data.personnes.length; i++) {
				dataAsHtml += '<tr>';
				for(var field in data.personnes[i]) {
					var canEdit = field !== 'label' ? 'contenteditable="true"' : '';
					dataAsHtml += '<td '+ canEdit +'>' + data.personnes[i][field] + '</td>';
				}
				dataAsHtml += '</tr>';
			}
			$('#datas').html(dataAsHtml);
		}
	};

	return {
		init : function() {
			$('body').delegate('#menu', 'click',function(event) {
				event.preventDefault();
				if (!event.target.getAttribute('call')) return;
				var call = event.target.getAttribute('call');
				admin[call]({url : event.target.getAttribute('href'), id: event.id});
			});
		},
		dictionary : function(o) {
			getAndRender(o.url, "dictionary")
		}
	}
}();
$(document).ready(function(){ admin.init();});
