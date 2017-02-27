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
        .pipe(webpackstream(prodMode ? prodConf : devConf, webpack)
            .on('error', function(err) {
                console.log
            }))
        .pipe(gulp.dest('./admin/src/main/resources/public'))
}

gulp.task('admin2:clean', function() {
    return del(['./admin/src/main/resources/public/js/*', './admin/src/main/resources/public/templates/*'])
})

gulp.task('admin2:ts', function() {
    return buildTs()
})

gulp.task('admin2:ts-prod', function() {
    return buildTs(true)
})

gulp.task('admin2:sass', function() {
    return gulp.src('./admin/src/main/resources/public/styles/admin.scss')
        .pipe(sass())
        .pipe(gulp.dest('./admin/src/main/resources/public/styles'))
})

gulp.task('admin2:build', ['admin2:ts'], function(){

})
gulp.task('admin2:build-prod', ['admin2:ts-prod'], function(){

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
    for(entry in devConf.entry) {
        devConf.entry[entry] = ["webpack-dev-server/client?http://localhost:9000/", devConf.entry[entry]]
    }
    const compiler = webpack(devConf)
    const server = new WebpackDevServer(compiler, devServConf)
    server.listen(devServConf.port, function(err, stats) {
        if(err) throw new gutil.PluginError("webpack", err)
    })
})