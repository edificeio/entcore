var index = function(){
	var getAndRender = function (pathUrl, templateName){
		$.get(pathUrl)
			.done(function(data) {
				template.render(templateName, data);
			})
			.error(function() { // TODO: Manage error message
				template.render("error");
			});
	};

	var template = {
		render : function (nom, data) {
			template[nom](data);
		},
		error : function () {
			$('#log').html('<p style="color:red">ERROR</p>')
		},
		myAccount : function (data) {
			var htmlString = "";
			htmlString += '<img src="' + data.picture + '">';
			$('#picture').html(htmlString);
			
			htmlString = "";
			htmlString += 
				'<p><span>' + data.userData.firstName + ' ' + data.userData.lastName 
				+ '</span></p>';
			var attributes = data.userData.attributes;
			for (i = 0; i < attributes.length; i++) {
				htmlString += 
					'<p><span>' + attributes[i].label + ' : </span>'
					+ '<span>' + attributes[i].value + '</span></p>';
			}
			$('#data').html(htmlString);
			
			htmlString = "";
			htmlString += 
				'<p><span>' + data.motto.label + ' : </span>'
				+ '<span>' + data.motto.value + '</span></p>';
			$('#motto').html(htmlString);
			
			htmlString = "";
			var interests = data.interests;
			for (i = 0; i < interests.length; i++) {
				htmlString += '<p><span>' + interests[i].label + ' : </span>';
				var values = interests[i].values;
				htmlString += '<span>';
				for (j = 0; j < values.length; j++) {
					if (j == 0) {
						htmlString += values[j];
					} else {
						htmlString += ', ' + values[j];
					}
				}
				htmlString += '</span>';
			}
			htmlString += '</p>';
			$('#interests').html(htmlString);
			
			htmlString = "";
			htmlString += 
				'<p><span>' + data.health + '</span></p>';
			$('#health').html(htmlString);
		}
	};

	return {
		init : function() {
			index.myAccount('/load');
		},
		myAccount : function(url) {
			getAndRender(url, "myAccount");
		}
	}
}();

$(document).ready(function(){
	index.init(); 
});