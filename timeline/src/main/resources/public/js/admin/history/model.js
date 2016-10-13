function PendingNotification() {}
PendingNotification.prototype.action = function(structureId, action) {
    return http().put('/timeline/' + this._id + '/action/' + action +
        '?structure=' + structureId)
}

function TreatedNotification() {}

function Structure() {}

model.build = function() {
    model.makeModels([PendingNotification, TreatedNotification, Structure])

    var getCollProps = function(pending) {
        return {
            page: 0,
            lastPage: false,
            loading: false,
            reset: function() {
                this.page = 0
                this.lastPage = false
                this.loading = false,
                this.all = []
            },
            feed: function(structureId) {
                if (this.loading || this.lastPage)
                    return

                http().get("/timeline/reported", {
                    structure: structureId,
                    page: this.page,
                    pending: pending
                }).done(function(data) {
                    if (data.length > 0) {
                        this.addRange(data);
                        this.page++;
                    } else {
                        this.lastPage = true;
                    }
                    this.trigger('sync')
                    this.loading = false
                }.bind(this)).error(function(err) {
                    notify.error(err)
                    this.loading = false
                })
            }
        }
    }

    this.collection(PendingNotification, getCollProps(true))
    this.collection(TreatedNotification, getCollProps(false))

    this.collection(Structure, {
        sync: function() {
            var that = this
            http().get('/directory/structure/admin/list').done(function(data) {
                that.load(data)
                _.forEach(that.all, function(struct) {
                    struct.parents = _.filter(struct.parents, function(parent) {
                        var parentMatch = _.findWhere(that.all, {
                            id: parent.id
                        })
                        if (parentMatch) {
                            parentMatch.children = parentMatch.children ? parentMatch.children : []
                            parentMatch.children.push(struct)
                            return true
                        } else
                            return false
                    })
                    if (struct.parents.length === 0)
                        delete struct.parents
                })
            })
        }
    })
}
