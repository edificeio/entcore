function FlashMsg(){
    this.startDate = this.startDate ? new Date(this.startDate) : new Date()
    this.endDate = this.endDate ? new Date(this.endDate) : new Date()
    this.lang = this.lang || currentLanguage
}
FlashMsg.prototype.api = {
    post:   "/timeline/flashmsg",
    put:    "/timeline/flashmsg/:_id",
    delete: "/timeline/flashmsg/:_id"
}
FlashMsg.prototype.duplicate = function(cb){
    http().put("/timeline/flashmsg/" + this._id + "/duplicate").done(function(data){
        var duplicate = new FlashMsg()
        duplicate.updateData(data)
        if(cb)
            cb(duplicate)
    })
}
FlashMsg.prototype.toJSON = function(){
    return {
        title :         this.title,
        startDate:      this.startDate,
        endDate:        this.endDate,
        profiles:       this.profiles,
        lang:           this.lang || currentLanguage,
        color:          this.color,
        customColor:    this.customColor,
        content:        this.content
    }
}

model.build = function () {
    model.makeModel(FlashMsg)

    RTE.baseToolbarConf.options.all = _.filter(RTE.baseToolbarConf.options.all, function(option){
        var keep = [
            "undo",
            "redo",
            "bold",
            "italic",
            "underline",
            "justifyLeft",
            "justifyRight",
            "justifyCenter",
            "justifyFull",
            "font",
            "fontSize",
            "linker" ]

        return keep.indexOf(option.name) > -1
    })

    this.edited = {}
    this.languages = []
    this.syncLanguages = function(){
        http().get('/languages').done(function (langs) {
            langs.forEach(function(lang){
                this.languages.push(lang)
            }.bind(this))
        }.bind(this))
    }

    this.collection(FlashMsg, {
        sync: "/timeline/flashmsg/listadmin",
        delete: function(flashMsgs) {
            if(flashMsgs.length < 1){
                return
            }
            var queryString = ""
            flashMsgs.forEach(function(msg){
                if(!queryString)
                    queryString += "?id=" + msg._id
                else
                    queryString += "&id=" + msg._id
            })
            return http().delete("/timeline/flashmsg" + queryString)
        }
    })
}
