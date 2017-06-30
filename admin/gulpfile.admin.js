const gulp = require('../gulpfile-loader')('adminV2');
const webpackstream = require('webpack-stream')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const changed = require('gulp-changed')
const gutil = require('gulp-util')
const sass = require('gulp-sass')
const rename = require('gulp-rename')
const del = require('del')

const entCoreVersion = '2.0-SNAPSHOT'
const springboardPath = '../springboard-open-ent'
const basePath = './admin/src/main'
const target = springboardPath + '/mods/org.entcore~admin~' + entCoreVersion

const devConf       = require('./webpack.config.dev.js')
const prodConf      = require('./webpack.config.prod.js')
const devServConf   = require('./webpack.config.devserver.js')

const buildTs = function(prodMode) {
    return gulp.src('./admin')
        .pipe(webpackstream(prodMode ? prodConf : devConf, webpack).on('error', console.log))
        .pipe(gulp.dest('./admin/src/main/resources/public'))
}

gulp.task('clean', function() {
    return del([
        './admin/src/main/resources/public/js/*',
        './admin/src/main/resources/public/templates/*',
        './admin/src/main/resources/public/styles/admin.css',
        './admin/src/main/resources/public/styles/admin.css.map',
        './admin/src/main/resources/public/styles/flatpickr-confetti.css',
        './admin/src/main/resources/view/*'])
})

gulp.task('ts-dev', function() {
    return buildTs(false)
})

gulp.task('ts', ['clean'], function() {
    return buildTs(true)
})

gulp.task('sass', function() {
    return gulp.src('./admin/src/main/resources/public/styles/admin.scss')
        .pipe(sass())
        .pipe(gulp.dest('./admin/src/main/resources/public/styles'))
})

gulp.task('build-dev', ['ts-dev', 'copy-flatpickr-css'], function(){})

gulp.task('build', ['ts', 'copy-flatpickr-css'], function(){})

gulp.task('copymod', function() {
    return gulp.src(basePath + '/resources/**/*')
        .pipe(changed(target))
        .pipe(gulp.dest(target))
})

gulp.task('watch', function() {
    gulp.watch(basePath + '/resources/**/*', ['copymod'])
    //gulp.watch(basePath + '/ts/**/*.ts', ['ts'])
})

gulp.task('dev-server', function() {
    const conf = devConf
    for(entry in conf.entry) {
        conf.entry[entry] = [`webpack-dev-server/client?http://localhost:${devServConf.port}/`, conf.entry[entry]]
    }
    const compiler = webpack(conf)
    const server = new WebpackDevServer(compiler, devServConf)
    server.listen(devServConf.port, function(err, stats) {
        if(err) throw new gutil.PluginError("webpack", err)
    })
})

gulp.task('copy-flatpickr-css', function() {
    return gulp.src('./node_modules/flatpickr/dist/themes/confetti.css')
        .pipe(rename('flatpickr-confetti.css'))
        .pipe(gulp.dest(basePath + '/resources/public/styles'))
})
