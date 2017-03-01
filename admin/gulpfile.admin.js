const gulp = require('gulp')
const webpackstream = require('webpack-stream')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const changed = require('gulp-changed')
const gutil = require('gulp-util')
const sass = require('gulp-sass')
const del = require('del')

const entCoreVersion = '1.23-SNAPSHOT'
const springboardPath = '../leo'
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

gulp.task('admin2:clean', function() {
    return del([
        './admin/src/main/resources/public/js/*',
        './admin/src/main/resources/public/templates/*',
        './admin/src/main/resources/public/styles/admin.css',
        './admin/src/main/resources/public/styles/admin.css.map',
        './admin/src/main/resources/view/*'])
})

gulp.task('admin2:ts-dev', function() {
    return buildTs(false)
})

gulp.task('admin2:ts', function() {
    return buildTs(true)
})

gulp.task('admin2:sass', function() {
    return gulp.src('./admin/src/main/resources/public/styles/admin.scss')
        .pipe(sass())
        .pipe(gulp.dest('./admin/src/main/resources/public/styles'))
})

gulp.task('admin2:build-dev', ['admin2:ts-dev'], function(){

})
gulp.task('admin2:build', ['admin2:ts'], function(){

})

gulp.task('admin2:copymod', function() {
    return gulp.src(basePath + '/resources/**/*')
        .pipe(changed(target))
        .pipe(gulp.dest(target))
})

gulp.task('admin2:watch', function() {
    gulp.watch(basePath + '/resources/**/*', ['admin2-copymod'])
    //gulp.watch(basePath + '/ts/**/*.ts', ['admin2-ts'])
})

gulp.task('admin2:dev-server', function() {
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