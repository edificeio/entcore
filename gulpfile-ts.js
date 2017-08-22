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

var apps = ['auth', 'timeline', 'conversation', 'archive', 'workspace', 'directory', 'portal'];

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
        var str = gulp.src('./' + a + '/src/main/resources/view-src/**/*.+(html|txt|json)')
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


gulp.task('ts', ['update-libs'], function () { return compileTs() });
gulp.task('webpack', ['ts'], function(){ return startWebpack() });

gulp.task('drop-temp', ['webpack'], () => {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src([
            './' + a + '/src/main/resources/public/**/*.map.map',
            './' + a + '/src/main/resources/public/temp'
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