const gulp = require('../gulpfile-loader')('adminV2');
const webpackstream = require('webpack-stream')
const webpack = require('webpack')
const WebpackDevServer = require('webpack-dev-server')
const changed = require('gulp-changed')
const gutil = require('gulp-util')
const del = require('del')

const entCoreVersion = '3.5-adminv2-SNAPSHOT'
const springboardPath = '../recette'
const basePath = './admin/src/main'
const target = springboardPath + '/mods/org.entcore~admin~' + entCoreVersion

const devConf       = require('./webpack.config.dev.js')
const prodConf      = require('./webpack.config.prod.js')
const devServConf   = require('./webpack.config.devserver.js')

// CLEAN
// -----
gulp.task('clean', function() {
    return del([
        './admin/src/main/resources/public/js/*',
        './admin/src/main/resources/public/templates/*',
        './admin/src/main/resources/public/styles/admin.css',
        './admin/src/main/resources/public/styles/admin.*.css',
        './admin/src/main/resources/public/styles/admin.*.css.map',
        './admin/src/main/resources/public/styles/generic-icons.woff',
        './admin/src/main/resources/public/styles/generic-icons.*.woff',
        './admin/src/main/resources/view/*'])
})

// BUILD
// -----
const buildTs = function(prodMode) {
    return gulp.src('./admin')
        .pipe(webpackstream(prodMode ? prodConf : devConf, webpack))
        .pipe(gulp.dest('./admin/src/main/resources/public'))
}

gulp.task('build-dev', function() {
    return buildTs(false)
})

gulp.task('build', ['clean'], function() {
    return buildTs(true)
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

// WATCH
// -----
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
