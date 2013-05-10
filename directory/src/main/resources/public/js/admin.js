var admin = function(){

	var getAndRender = function (pathUrl, templateName){
		$.get(pathUrl)
			.done(function(data) {
				template.render(templateName, data);
			})
	};
	var postAndRender = function (form, templateName){
		$.post(form.getAttribute('action'), $('#edit').serialize())
			.done(function(data){
				console.log(data);
				template.render(templateName, data);
			})
	};

	var template = {
		render : function (nom, data) {
			template[nom](data);
		},
		ecole : function (data) {
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			if (!!$('#schools').children().length) {
				$('#schools').html('');
				return;
			}
			for (obj in jdata.result){
				htmlString +="<h3>" + jdata.result[obj]["n.ENTStructureNomCourant"] + "</h3>"
					+ "<a call='classes' href='/api/classes?id=" + jdata.result[obj]["n.id"]
					+ "'>{{#i18n}}directory.admin.classes{{/i18n}}</a> - "
					+ "<a href='/api/export?id=" + jdata.result[obj]["n.id"]
					+ "' call='exportAuth'>{{#i18n}}directory.admin.exports{{/i18n}}</a>"
					+ "<div id='classes-"+ jdata.result[obj]["n.id"] +"'></div>";
			}
			$("#schools").html(htmlString);
		},
		groupes : function(data){
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			if (!!$('#groups').children().length) {
				$('#groups').html('');
				return;
			}
			for (obj in jdata.result){
				htmlString += "<h3>" + jdata.result[obj]["n.ENTGroupeNom"] + "</h3>"
					+ "<a call='membres' href='/api/membres?data="
					+ jdata.result[obj]["n.ENTPeople"].replace(/\[/g, '').replace(/\]/g, '').replace(/, /g,'-')
					+ "'>{{#i18n}}directory.admin.see-people{{/i18n}}</a>";
			}
			$('#groups').html(htmlString);
		},
		classes: function(data) {
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			if (jdata.result != ""){
				if (!!$("#classes-" + jdata.result[0]["n.id"]).children().length) {
					$("#classes-" + jdata.result[0]["n.id"]).html('');
					return;
				}
				for (obj in jdata.result){
					htmlString +="<h4><a>" + jdata.result[obj]['m.ENTGroupeNom'] + "</a></h4>"
						+ "<a call='personnes' href='/api/personnes?id=" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-')
						+ "'>{{#i18n}}directory.admin.see-people{{/i18n}}</a>"
						+ " - <a href='/api/enseignants?id=" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') + "' call='enseignants'>Ajouter un enseignant</a>" 
						+ " - <a href='/api/export?id=" + jdata.result[obj]["m.id"]
						+ "' call='exportAuth'>{{#i18n}}directory.admin.exports{{/i18n}}</a><br />"
						+ "<div id='people-" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') + "'></div>";
				}
				$("#classes-" + jdata.result[0]["n.id"]).html(htmlString);
			}
		},
		personnes : function(data) {
			var htmlString='<br /><span>';
			var jdata = jQuery.parseJSON(data);
			if (jdata.result != ""){
				if (!!$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).children().length) {
					$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html('');
					return;
				}
				for (obj in jdata.result){
					htmlString +="<a call='personne' href='/api/details?id="
						+ jdata.result[obj]['m.id'] +"'>" + jdata.result[obj]['m.ENTPersonNom']
						+ " " +jdata.result[obj]['m.ENTPersonPrenom'] + "</a> - ";
				}
				htmlString += "</span><div id='details'></div>";
				$("#people-" + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html(htmlString);
			}
		},
		enseignants : function(data) {
			var htmlString='<br /><span>';
			var jdata = jQuery.parseJSON(data);
			if (jdata.result[0] !== undefined){
				if (!!$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).children().length) {
					$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html('');
					return;
				}
				for (obj in jdata.result){
					htmlString +="<a call='personne' href='/api/link?"
						+ "class=" + jdata.result[obj]['n.id']
						+ "&id=" + jdata.result[obj]['m.id'] +"'>" + jdata.result[obj]['m.ENTPersonNom']
						+ " " +jdata.result[obj]['m.ENTPersonPrenom'] + "</a> - ";
				}
				htmlString += "</span><div id='details'></div>";
				$("#people-" + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html(htmlString);
			}
		},
		membres : function(data){
			var htmlString='<span>';
			var jdata = jQuery.parseJSON(data);
			for (obj in jdata.result){
				htmlString += jdata.result[obj]['n.ENTPersonNom']
						+ " " +jdata.result[obj]['n.ENTPersonPrenom'] + " - ";
			}
			$('#members').html(htmlString + "</span>");
		},
		personne : function(data) {
			if (!!$('#details').children('form').length) {
				$('#details').html('');
				return;
			}
			var jdata = jQuery.parseJSON(data);
			var htmlString = "Nom : " + jdata.result[0]['n.ENTPersonNom']
				+ " - Prénom : " + jdata.result[0]['n.ENTPersonPrenom']
				+ " - Adresse : " + jdata.result[0]['n.ENTPersonAdresse'];
			$('#details').html(htmlString);
		},
		exportAuth : function(data) {
			var jdata = jQuery.parseJSON(data);
			var textString = "Nom,Prénom,Login,Mot de passe\n";
			for (obj in jdata.result){
				textString += jdata.result[obj]['m.ENTPersonNom'] + ","
					+ jdata.result[obj]['m.ENTPersonPrenom'] + ","
					+ jdata.result[obj]['m.ENTPersonLogin'] + ","
					+ jdata.result[obj]['m.ENTPersonMotDePasse'] + "\n";
			}
			document.location = 'data:Application/octet-stream,' + encodeURIComponent(textString);
		},
		personnesEcole : function(data) {
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			for (obj in jdata.result){
				htmlString +='<input type="checkbox" name="'
					+ jdata.result[obj]['m.id'] + '" value="'
					+ jdata.result[obj]['m.id'] + '" />' + jdata.result[obj]['m.ENTPersonNom']
					+ ' ' + jdata.result[obj]['m.ENTPersonPrenom'] + ' - ';
			}
			$('#users').html(htmlString);
		},
		createUser : function(data) {
			if (data.result === "error"){
				console.log(data);
				$('label').removeAttr('style');
				for (obj in data){
					if (obj !== "result"){
						$('#' + obj).attr("style", "color:red");
					}
				}
				$('#confirm').html("<span style='color:red'>ERROR !</span>");
			} else {
				$('#confirm').html("OK");
				$('label').removeAttr('style');

			}
		},
		createAdmin : function(data) {
			console.log(data);
			var jdata = jQuery.parseJSON(data);
			if (jdata.status === 'ok'){
				$('#confirm').html("OK");
			} else {
				$('#confirm').html("ERREUR !");
			}
		},
		createGroup : function(data) {
			var jdata = jQuery.parseJSON(data);
			if (jdata.status === 'ok'){
				$('#confirm').html("OK");
			} else {
				$('#confirm').html("ERREUR !");
			}
			
		}
	};

	var receiver = function(event) {
		console.log("dir = " + event.origin);
		if (event.origin == "http://localhost:8008") {
			$("head").append("<link rel='stylesheet' href='" + event.data + "' media='all' />");
		}
	};


	return {
		init : function() {
			window.addEventListener('message', receiver, false);
			$('body').delegate('#annuaire', 'click',function(event) {
				console.log(event.target);
				if (event.target.getAttribute('call')){
					event.preventDefault();
					var call = event.target.getAttribute('call');
					admin[call](
						{url : event.target, id: event.id}
					);
				}

			});
		},
		ecole : function(o) {
			getAndRender(o.url, 'ecole');
		},
		classes : function(o) {
			getAndRender(o.url, "classes");
		},
		groupes : function(o) {
			getAndRender(o.url, "groupes");
		},
		personnesEcole : function(o) {
			getAndRender(o.url, "personnesEcole");
		},
		personnes : function(o) {
			getAndRender(o.url, "personnes");
		},
		membres : function(o) {
			getAndRender(o.url, "membres");
		},
		personne : function(o) {
			getAndRender(o.url, "personne");
		},
		enseignants : function(o) {
			getAndRender(o.url, "enseignants");
		},
		exportAuth : function(o) {
			getAndRender(o.url, "exportAuth");
		},
		createUser : function(o) {
			var url = o.url.attributes.action.value + '?'
				+ $('#create-user').serialize()
				+ '&ENTPersonProfils=' + $('#profile').val()
				+ '&ENTPersonStructRattach=' + $('#groupe').val().replace(/ /g,'-');
			getAndRender(url, "createUser");
		},
		createAdmin : function(o) {
			var url = o.url.attributes.action.value + '?'
				+ $('#create-admin').serialize()
				+ '&ENTPerson=' + $('#choice').val();
			getAndRender(url, "createAdmin");
		},
		createGroup : function(o) {
			var url = o.url.attributes.action.value + '?'
				+ $('#create-group').serialize()
				+ '&type=' + $('#type').val()
				+ '&ENTGroupStructRattach=' + $('#parent').val();
			getAndRender(url, "createGroup");
		},
		createSchool : function(o) {
			var url = o.url.attributes.action.value + '?'
				+ $('#create-school').serialize();
			getAndRender(url, "createGroup");
		},
		view: function(o) {
			switch(o.url.attributes.id.value){
				case 'disp':
					$('#creation').attr('hidden', '');
					$('#display').removeAttr('hidden');
					$('#export').attr('hidden', '');
					break;
				case 'exports':
					$('#creation').attr('hidden', '');
					$('#display').removeAttr('hidden');
					$('#export').removeAttr('hidden');
					break;
				case 'create':
					$('#creation').removeAttr('hidden');
					$('#display').attr('hidden', '');
					$('#export').attr('hidden', '');
					break;
			}
		}
	}
}();


$(document).ready(function(){
	admin.init(); 
});