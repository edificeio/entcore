function Message() {

}

Message.prototype.sync = function (cb) {
    http().get('/auth/configure/welcome').done(function (d) {
        this.content = d.welcomeMessage;
        if (cb) {
            cb();
        }
    }.bind(this))
    .e404(function () {
        this.content = "";
        if (cb) {
            cb();
        }
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