var lastMailsWidget = model.widgets.findWidget('lastMails');

http().get('/conversation/list/INBOX?page=0').done(function(mails){
	lastMailsWidget.mails = mails;
	model.widgets.apply();
});