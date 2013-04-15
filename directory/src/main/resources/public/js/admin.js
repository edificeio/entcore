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
			for (obj in jdata.result){
				htmlString +="<h3>" + jdata.result[obj]["n.ENTStructureNomCourant"] + "</h3>"
					+ "<a call='classes' href='/api/classes?id=" + jdata.result[obj]["n.id"]
					+ "'>{{#i18n}}directory.admin.classes{{/i18n}}</a> - "
					+ "<a href='/api/export?id=" + jdata.result[obj]["n.id"]
					+ "' call='exportAuth'>{{#i18n}}directory.admin.export{{/i18n}}</a>"
					+ "<div id='classes-"+ jdata.result[obj]["n.id"] +"'></div>";
			}
			$("#schools").html(htmlString);
		},
		groupes : function(data){
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			for (obj in jdata.result){
				htmlString += "<h3>" + jdata.result[obj]["n.ENTGroupeNom"] + "</h3>"
					+ "<a call='membres' href='/api/membres?data="
					+ jdata.result[obj]["n.ENTPeople"].replace(/\[/g, '').replace(/\]/g, '').replace(/, /g,'-')
					+ "'>{{#i18n}}directory.admin.people{{/i18n}}</a>";
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
						+ "'>{{#i18n}}directory.admin.people{{/i18n}}</a>"
						+ " - <a href='/api/enseignants?id=" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') + "' call='enseignants'>Ajouter un enseignant</a>" 
						+ " - <a href='/api/export?id=" + jdata.result[obj]["m.id"]
						+ "' call='exportAuth'>{{#i18n}}directory.admin.export{{/i18n}}</a><br />"
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
					+ jdata.result[obj]['m.ENTPersonNom'] + ","
					+ jdata.result[obj]['m.ENTPersonPrenom'] + "\n";
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
				for (obj in document.getElementById('create-user').children){
					if (document.getElementById('create-user').children[obj].localName === 'label'){
						document.getElementById('create-user').children[obj].removeAttribute("style");
					}
				}
				for (obj in data){
					if (obj !== "result"){
						document.getElementById(obj).setAttribute("style", "color:red");
					}
				}
				$('#confirm').html("<span style='color:red'>ERROR !</span>");
			} else {
				$('#confirm').html("OK");
				for (obj in document.getElementById('create-user').children){
					if (document.getElementById('create-user').children[obj].localName === 'label'){
						document.getElementById('create-user').children[obj].removeAttribute("style");
					}
				}
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

	return {
		init : function() {
			admin.ecole('/api/ecole');
			admin.groupes('/api/groupes');
			admin.personnesEcole('/api/personnes?id=4400000002')
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
		ecole : function(url) {
			getAndRender(url, 'ecole');
		},
		classes : function(o) {
			getAndRender(o.url, "classes");
		},
		groupes : function(url) {
			getAndRender(url, "groupes");
		},
		personnesEcole : function(url) {
			getAndRender(url, "personnesEcole");
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
			getAndRender(o.url, "personnes");
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
		createGroup : function(o) {
			var url = o.url.attributes.action.value + '?'
				+ $('#create-group').serialize();
			getAndRender(url, "createGroup");
		}
	}
}();


$(document).ready(function(){
	admin.init(); 
});