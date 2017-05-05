const gulp = require('gulp')
const webpack = require('webpack-stream')
const changed = require('gulp-changed');
const sass = require('gulp-sass')

const entCoreVersion = '1.23-SNAPSHOT'
const springboardPath = '../leo'
const basePath = './admin/src/main'
const target = springboardPath + '/mods/org.entcore~admin~' + entCoreVersion

gulp.task('admin2-ts', function() {
    return gulp.src('./admin')
        .pipe(webpack(require('./webpack.config.js'))
            .on('error', function(err) {
                console.log
            }))
        .pipe(gulp.dest('./admin/src/main/resources/public'))
})

gulp.task('admin2-sass', function() {
    return gulp.src('./admin/src/main/resources/public/styles/admin.scss')
        .pipe(sass())
        .pipe(gulp.dest('./admin/src/main/resources/public/styles'))
})

gulp.task('admin2-build', ['admin2-ts', 'admin2-sass'], function() {

})

gulp.task('admin2-copymod', function() {
    return gulp.src(basePath + '/resources/**/*')
        .pipe(changed(target))
        .pipe(gulp.dest(target))
})

gulp.task('admin2-watch', function() {
    gulp.watch(basePath + '/resources/**/*', ['admin2-copymod']);
    //gulp.watch(basePath + '/ts/**/*.ts', ['admin2-ts']);
})