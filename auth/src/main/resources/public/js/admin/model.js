function Message() {

}

Message.prototype.sync = function () {
    http().get('/auth/configure/welcome').done(function (d) {
        this.content = d.welcomeMessage;
    }.bind(this))
    .e404(function () {
        this.content = ""
    }.bind(this))
}

Message.prototype.save = function () {
    var savedData = {};
    savedData[currentLanguage] = this.content;
    http().putJson('/auth/configure/welcome', savedData)
        .done(function () {
            notify.success('notify.saved')
        }.bind(this))
        .error(function(){
        }.bind(this));
};

model.build = function () {
    model.makeModel(Message);
    this.message = new Message();
}