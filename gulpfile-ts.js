var gulp = require('./gulpfile-loader')('ts');
var rootGulp = require('gulp');
var webpack = require('webpack-stream');
var merge = require('merge2');
var watch = require('gulp-watch');
var rev = require('gulp-rev');
var revReplace = require("gulp-rev-replace");
var clean = require('gulp-clean');
var sourcemaps = require('gulp-sourcemaps');
var typescript = require('typescript');
var argv = require('argv');
var fs = require('fs');

var apps = ['auth', 'timeline', 'conversation', 'archive', 'workspace', 'directory', 'portal'];

var paths = {
    infra: '../infra-front'
};

function startWebpack(mode) {
    var streams = [];
    apps.forEach(a => {
        var webpackConf = require('./' + a + '/webpack.config.js');
        if(mode === 'dev'){
            webpackConf.devtool = 'inline-source-map';
        }
        var str = gulp.src('./' + a + '/src/main/resources/public/**/*.ts')
            .pipe(webpack(webpackConf))
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist'))
            .pipe(rev())
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist'))
            .pipe(rev.manifest('./' + a + '/rev-manifest.json', { merge: true }))
            .pipe(gulp.dest('./'));
        streams.push(str);
    });

    return merge(streams);
}

function updateRefs() {
    console.log('Updating hashs in views');
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src('./' + a + '/src/main/resources/view-src/**/*.+(html|txt|json)')
            .pipe(revReplace({manifest: gulp.src('./' + a + '/rev-manifest.json') }))
            .pipe(gulp.dest('./' + a + '/src/main/resources/view'));
        streams.push(str);
    });

    return merge(streams);
}

function updateLibs(){
    
}

gulp.task('drop-old-files', function () {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src([
            './' + a + '/src/main/resources/public/dist'
        ], { read: false })
            .pipe(clean());
        streams.push(str);
    })
    return merge(streams);
});

gulp.task('update-libs', ['drop-old-files'], function(){
    var streams = [];
    streams.push(
        gulp.src('./node_modules/pixi.js/dist/pixi.min.js')
            .pipe(gulp.dest('./infra/src/main/resources/public/js'))
    );
    apps.forEach(a => {
        var html = gulp.src('./node_modules/entcore/src/template/**/*.html')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/template/entcore'));
        var bundle = gulp.src('./node_modules/entcore/bundle/*')
            .pipe(rev())
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist/entcore'))
            .pipe(rev.manifest('./' + a + '/rev-manifest.json', { merge: true }))
            .pipe(gulp.dest('./'));
            
        streams.push(html, bundle);
    });
    return merge(streams);
});

gulp.task('webpack', ['update-libs'], function(){ return startWebpack() });

gulp.task('drop-temp', ['webpack'], () => {
    var streams = [];
    apps.forEach(a => {
        var copyMaps = gulp.src('./node_modules/entcore/bundle/ng-app.js.map')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist/entcore'));
        streams.push(copyMaps);
    });

    return merge(streams);
});

gulp.task('build', ['drop-temp'], function () {
    var streams = [];
    apps.forEach(a => {
        var refs = updateRefs();
        var copyBehaviours = gulp.src('./' + a + '/src/main/resources/public/dist/behaviours.js')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/js'));
        streams.push(refs, copyBehaviours);
    });
    
    return merge(streams);
});

function getModName(fileContent, app){
    var getProp = function(prop){
        return fileContent.split(prop + '=')[1].split(/\r*\n/)[0];
    }
    return getProp('modowner') + '~' + app + '~' + getProp('version');
}

apps.forEach((app) => {
    rootGulp.task('watch-' + app, () => {
        var springboard = argv.springboard;
        if(!springboard){
            springboard = '../springboard-open-ent/';
        }
        if(springboard[springboard.length - 1] !== '/'){
            springboard += '/';
        }
    
        rootGulp.watch('./' + app + '/src/main/resources/public/ts/**/*.ts', () => startWebpack('dev'));
    
        fs.readFile("./gradle.properties", "utf8", function(error, content){
            var modName = getModName(content, app);
            rootGulp.watch(['./' + app + '/src/main/resources/public/template/**/*.html', '!./' + app + '/src/main/resources/public/template/entcore/*.html'], () => {
                console.log('Copying resources to ' + springboard + 'mods/' + modName);
                rootGulp.src('./' + app + '/src/main/resources/**/*')
                    .pipe(rootGulp.dest(springboard + 'mods/' + modName));
            });
    
            rootGulp.watch('./' + app + '/src/main/resources/view/**/*.html', () => {
                console.log('Copying resources to ' + springboard + 'mods/' + modName);
                rootGulp.src('./' + app + '/src/main/resources/**/*')
                    .pipe(rootGulp.dest(springboard + 'mods/' + modName));
            });

            rootGulp.watch(['./' + app + '/src/main/resources/public/dist/**/*.js', '!./' + app + '/src/main/resources/public/dist/entcore/**/*.js'], () => {
                console.log('Copying resources to ' + springboard + 'mods/' + modName);
                rootGulp.src('./' + app + '/src/main/resources/public/dist/**/*')
                    .pipe(rootGulp.dest(springboard + 'mods/' + modName + '/public/dist'));
            });

            rootGulp.watch('./' + app + '/rev-manifest.json', () => {
                updateRefs();
            });
        });
    });
})
