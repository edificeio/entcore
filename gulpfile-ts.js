var gulp = require('./gulpfile-loader')('ts');
var ts = require('gulp-typescript');
var webpack = require('webpack-stream');
var bower = require('gulp-bower');
var merge = require('merge2');
var watch = require('gulp-watch');
var rev = require('gulp-rev');
var revReplace = require("gulp-rev-replace");
var clean = require('gulp-clean');
var sourcemaps = require('gulp-sourcemaps');
var typescript = require('typescript');

var apps = ['auth'];

var paths = {
    infra: '../infra-front'
};

function compileTs(){
    var streams = [];
    apps.forEach(a => {
        var tsProject = ts.createProject('./' + a + '/src/main/resources/public/ts/tsconfig.json', {
            typescript: typescript
        });
        var tsResult = gulp.src('./' + a + '/src/main/resources/public/ts/**/*.ts')
            .pipe(sourcemaps.init())
            .pipe(tsProject());
        
        streams.push(tsResult.js
            .pipe(sourcemaps.write('.'))
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/temp'))
        );
    });

    return merge(streams);
}

function startWebpackEntcore(isLocal) {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src('./' + a + '.src/main/resources/public/**/*.js')
            .pipe(webpack(require('./' + a + '/webpack-entcore.config.js')))
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist/entcore'))
            .pipe(rev())
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/dist/entcore'))
            .pipe(rev.manifest('./' + a + '/rev-manifest.json', { merge: true }))
            .pipe(gulp.dest('./'));
        streams.push(str);
    });

    return merge(streams);
}

function startWebpack(isLocal) {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src('./' + a + '/src/main/resources/public/**/*.js')
            .pipe(webpack(require('./' + a + '/webpack.config.js')))
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
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src('./' + a + '/src/main/resources/view-src/**/*.html')
            .pipe(revReplace({manifest: gulp.src('./' + a + '/rev-manifest.json') }))
            .pipe(gulp.dest('./' + a + '/src/main/resources/view'));
        streams.push(str);
    });

    return merge(streams);
}

gulp.task('drop-old-files', function () {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src([
            './' + a + '/src/main/resources/public/temp', 
            './' + a + '/src/main/resources/public/dist', 
            './' + a + '/src/main/resources/public/ts/entcore'
        ], { read: false })
            .pipe(clean());
        streams.push(str);
    })
    return merge(streams);
});

gulp.task('copy-local-libs', ['drop-old-files'], () => {
    var streams = [];
    apps.forEach(a => {
        var ts = gulp.src(paths.infra + '/src/ts/**/*.ts')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/ts/entcore'));
            
        var module = gulp.src(paths.infra + '/src/ts/**/*.ts')
            .pipe(gulp.dest('./node_modules/entcore'));

        var html = gulp.src(paths.infra + '/src/template/**/*.html')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/template/entcore'));
        streams.push(html, ts, module);
    });
    return merge(streams);
});

gulp.task('drop-cache', ['drop-old-files'], function(){
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src(['./bower_components', './' + a + '/src/main/resources/public/dist'], { read: false })
		    .pipe(clean());
        streams.push(str);
    });
    return merge(streams);
});

gulp.task('bower', ['drop-cache'], function(){
    return bower({ directory: './bower_components', cwd: '.', force: true });
});

gulp.task('update-libs', ['bower'], function(){
    var streams = [];
    apps.forEach(a => {
        var html = gulp.src('./bower_components/entcore/src/template/**/*.html')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/template/entcore'));

        var ts = gulp.src('./bower_components/entcore/src/ts/**/*.ts')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/ts/entcore'));

        var module = gulp.src('./bower_components/entcore/src/ts/**/*.ts')
            .pipe(gulp.dest('./node_modules/entcore'));
            
        streams.push(html, ts);
    });
    return merge(streams);
});

gulp.task('ts-local', ['copy-local-libs'], function () { return compileTs() });
gulp.task('webpack-local', ['ts-local'], function(){ return startWebpack() });
gulp.task('webpack-entcore-local', ['webpack-local'], function(){ return startWebpackEntcore() });

gulp.task('ts', ['update-libs'], function () { return compileTs() });
gulp.task('webpack', ['ts'], function(){ return startWebpack() });
gulp.task('webpack-entcore', ['webpack'], function(){ return startWebpackEntcore() });

gulp.task('drop-temp', ['webpack-entcore'], () => {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src([
            './' + a + '/src/main/resources/public/**/*.map.map',
            './' + a + '/src/main/resources/public/temp',
            './' + a + '/src/main/resources/public/dist/entcore/ng-app.js',
            './' + a + '/src/main/resources/public/dist/entcore/ng-app.js.map',
            './' + a + '/src/main/resources/public/dist/application.js',
            './' + a + '/src/main/resources/public/dist/application.js.map'
        ], { read: false })
            .pipe(clean());
        streams.push(str);
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

gulp.task('build-local', ['webpack-entcore-local'], function () {
    var streams = [];
    apps.forEach(a => {
        console.log('Building TS for ' + a);
        var refs = updateRefs();
        var copyBehaviours = gulp.src('./' + a + '/src/main/resources/public/dist/behaviours.js')
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/js'));
        streams.push(refs, copyBehaviours);
    })
    
    return merge(streams);
});