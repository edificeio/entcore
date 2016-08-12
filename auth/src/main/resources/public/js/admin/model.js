function Message() {

}

model.build = function () {
    model.makeModel(Message);

    this.edited = {
        message: new Message()
    };

    this.collection(Message, {
        save: function () {
            var savedData = { enabled: !this.hide };
            this.forEach(function (message) {
                savedData[message.lang] = message.content;
            })

            http().putJson('/auth/configure/welcome', savedData)
                .done(function () {
                    notify.success('notify.saved')
                }.bind(this))
                .error(function () {
                }.bind(this));
        },
        sync: function () {
            http().get('/languages').done(function (languages) {
                this.all = [];
                this.addRange(_.map(languages, function (lg) {
                    return new Message({ lang: lg, content: "" });
                }));

                http().get('/auth/configure/welcome?allLanguages')
                    .done(function (messages) {
                        this.hide = !messages.enabled;
                        var message;
                        for (var property in messages) {
                            message = this.findWhere({ lang: property });
                            if (message) {
                                message.content = messages[property];
                            }
                        }
                        this.trigger('sync');
                    }.bind(this))
                        .e404(function () {
                    });

            }.bind(this));
            
        }
    })
}