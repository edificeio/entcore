const gulp = require('../gulpfile-loader')('adminV2');
const webpackstream = require('webpack-stream')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const changed = require('gulp-changed')
const gutil = require('gulp-util')
const sass = require('gulp-sass')
const rename = require('gulp-rename')
const del = require('del')

const entCoreVersion = '3.4-SNAPSHOT'
const springboardPath = '../recette'
const basePath = './admin/src/main'
const target = springboardPath + '/mods/org.entcore~admin~' + entCoreVersion

const devConf       = require('./webpack.config.dev.js')
const prodConf      = require('./webpack.config.prod.js')
const devServConf   = require('./webpack.config.devserver.js')

const buildTs = function(prodMode) {
    return gulp.src('./admin')
        .pipe(webpackstream(prodMode ? prodConf : devConf, webpack))
        .pipe(gulp.dest('./admin/src/main/resources/public'))
}

gulp.task('clean', function() {
    return del([
        './admin/src/main/resources/public/js/*',
        './admin/src/main/resources/public/templates/*',
        './admin/src/main/resources/public/styles/admin.css',
        './admin/src/main/resources/public/styles/admin.*.css',
        './admin/src/main/resources/public/styles/admin.*.css.map',
        './admin/src/main/resources/public/styles/generic-icons-1.0.0.woff',
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

gulp.task('build-dev', ['ts-dev', 'copy-resources'], function(){})

gulp.task('build', ['ts', 'copy-resources'], function(){})

const copymod = function() {
    console.log('Call to copymod')
    return gulp.src(basePath + '/resources/**/*')
        .pipe(changed(target))
        .pipe(gulp.dest(target))
}

gulp.task('copymod', copymod)

gulp.task('watch', function() {
    gulp.watch(basePath + '/resources/**/*', copymod)
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

gulp.task('copy-entcore-generic-icons', function() {
    return gulp.src('./node_modules/entcore-generic-icons/fonts/generic-icons.woff')
        .pipe(rename('generic-icons-1.0.0.woff'))
        .pipe(gulp.dest(basePath  + '/resources/public/styles'))
})

gulp.task('copy-resources', ['copy-entcore-generic-icons'])
