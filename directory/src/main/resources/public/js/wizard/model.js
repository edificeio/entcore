function Wizard() {}

Wizard.prototype.validate = function(callback) {
    http().postFile("/directory/wizard/validate", this.toFormData())
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

Wizard.prototype.import = function(callback) {
    http().postFile("/directory/wizard/import", this.toFormData())
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
}

Wizard.prototype.toFormData = function() {
    var formData = new FormData();
    for (var attr in this) {
        // TODO remove useless objects
        if ((typeof this[attr] === 'function')) continue;
        if (this[attr]) {
            formData.append(attr, this[attr]);
        }
    }
    return formData;
};

Wizard.prototype.loadAvailableFeeders = function(callback){
    http().get('/directory/conf/public')
    .done(function(data) {
        if(typeof callback === 'function') {
            callback(data);
        }
    }).e400(function(e){
        var error = JSON.parse(e.responseText);
        if(typeof callback === 'function') {
            callback(error);
        } else {
            notify.error(error.error);
        }
    });
};

function Structure() {}

model.build = function() {
    this.makeModels([Structure]);
    this.collection(Structure, {
        sync: function(callback) {
            var that = this;
            http().get('structure/admin/list').done(function(structures) {
                that.load(structures);
                if(typeof callback === 'function') {
                    callback();
                }
            }).bind(this);
        }
    });
}
