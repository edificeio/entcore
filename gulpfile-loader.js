var g = require('gulp');

var buildList = [];
var buildLocalList = [];

var local = {
    paths: {
        cssLib: '../entcore-css-lib',
        infraFront: '../infra-front',
        toolkit: '../toolkit'
    }
};

try{
    local = require('./gulpfile-local');
}
catch(e){}


module.exports = function(name){
    return {
        task: function(taskName, dependencies, exec){
            if(typeof dependencies === 'function'){
                exec = dependencies;
                g.task(name + '-' + taskName, exec);
            }
            else{
                dependencies = dependencies.map((dep) => name + '-' + dep);
                g.task(name + '-' + taskName, dependencies, exec);
            }
            
            if(taskName === 'build'){
                buildList.push(name + '-' + taskName)
            }
            if(taskName === 'build-local'){
                buildLocalList.push(name + '-' + taskName)
            }
        },
        buildList: function(){
            return buildList;
        },
        buildLocalList: function(){
            return buildLocalList;
        },
        local: local,
        src: g.src,
        pipe: g.pipe,
        watch: g.watch,
        dest: g.dest
    };
};