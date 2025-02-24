// const {task, src, dest, series} = require('gulp');
const gulp = require('gulp');
const webpack = require('webpack-stream');
const merge = require('merge2');
const rev = require('gulp-rev');
const clean = require('gulp-clean');
const argv = require('yargs').argv;
const fs = require('fs');
const replace = require('gulp-replace');

// const adminBuild = require('./gulpfile-admin').adminBuild;

let apps = ['archive', 'auth', 'directory', 'portal', 'timeline', 'workspace'];
let i = process.argv.indexOf("--module");
// check if a module is specified and if it matches one of apps
if (i > -1 && process.argv.length > (i+1) && apps.indexOf(process.argv[i+1]) > -1) {
    apps = [process.argv[i+1]];
}

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
            .pipe(replace('@@VERSION', Date.now()))
            .pipe(gulp.dest('./' + a + '/src/main/resources/view'));
        streams.push(str);
    });

    return merge(streams);
}

function dropOldFiles() {
    var streams = [];
    apps.forEach(a => {
        var str = gulp.src([
            './' + a + '/src/main/resources/public/dist'
        ], { read: false, "allowEmpty": true })
            .pipe(clean());
        streams.push(str);
    })
    return merge(streams);
};

function updateLibs(){
    var streams = [];
    streams.push(
        gulp.src('./node_modules/pixi.js/dist/pixi.min.js')
            .pipe(gulp.dest('./infra/src/main/resources/public/js'))
    );
    return merge(streams);
}

function build() {
    var streams = [];
    apps.forEach(a => {
        var refs = updateRefs();
        var copyBehaviours = gulp.src('./' + a + '/src/main/resources/public/dist/behaviours.js', {allowEmpty: true})
            .pipe(gulp.dest('./' + a + '/src/main/resources/public/js'));
        streams.push(refs, copyBehaviours);
    });

    return merge(streams);
}

gulp.task('drop-old-files', dropOldFiles);
gulp.task('update-libs', updateLibs);
gulp.task('webpack', startWebpack);
gulp.task('webpack-dev', () => startWebpack('dev'));
gulp.task('build', build);


function getModName(fileContent, app){
    return fileContent.trim();
}

let springboardPath = '../recette';
if (argv.springboard) {
    springboardPath = argv.springboard;
    console.log('Using springboard at ' + springboardPath);
}

apps.forEach((app) => {
    gulp.task('watch-' + app, () => {
        gulp.watch('./' + app + '/src/main/resources/public/ts/**/*.ts', () => startWebpack('dev'));

        fs.readFile("./.version.properties", "utf8", function(error, content){
            var modName = getModName(content, app);
            gulp.watch(['./' + app + '/src/main/resources/public/template/**/*.html', '!./' + app + '/src/main/resources/public/template/entcore/*.html'], () => {
                console.log('Copying resources to ' + springboardPath + '/mods/' + modName);
                return  gulp.src('./' + app + '/src/main/resources/**/*')
                .pipe(gulp.dest(springboardPath + '/mods/' + modName));
            });

            gulp.watch('./' + app + '/src/main/resources/view/**/*.html', () => {
                console.log('Copying resources to ' + springboardPath + '/mods/' + modName);
                return gulp.src('./' + app + '/src/main/resources/**/*')
                .pipe(gulp.dest(springboardPath + '/mods/' + modName));
            });

            gulp.watch(['./' + app + '/src/main/resources/public/dist/**/*.js', '!./' + app + '/src/main/resources/public/dist/entcore/**/*.js'], () => {
                console.log('Copying resources to ' + springboardPath + '/mods/' + modName);
                return gulp.src('./' + app + '/src/main/resources/**/*')
                .pipe(gulp.dest(springboardPath + '/mods/' + modName));
            });

            gulp.watch('./' + app + '/rev-manifest.json', (cb) => {
                updateRefs();
                cb();
            });
        });
    });
})

exports.build = gulp.series('drop-old-files', 'update-libs', 'webpack', 'build');