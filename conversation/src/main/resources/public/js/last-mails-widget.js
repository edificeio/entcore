var lastMailsWidget = model.widgets.findWidget('lastMails');

http().get('/conversation/list/INBOX?page=0').done(function(mails){
	lastMailsWidget.mails = _.where(mails, { unread: true });
	model.widgets.apply();
});