var gulp = require('gulp');
var less = require('gulp-less');
var sass = require('gulp-sass');
var rename = require("gulp-rename");

var localCSSLibPath = '../entcore-css-lib'

gulp.task('copy-csslib', () => {
    return gulp.src(localCSSLibPath + '/**/*')
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib'))
});

gulp.task('sass', ['copy-csslib'], () => {
    return gulp.src('./portal/src/main/resources/public/libs/entcore-css-lib/_css-lib.scss')
        .pipe(rename("css-lib.scss"))
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib'))
        .pipe(sass())
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib/entcore-css-lib'))
});

gulp.task('admin-theme', ['sass'], () => {
    return gulp.src('./portal/src/main/resources/public/admin/**/theme.less')
        .pipe(less())
        .pipe(gulp.dest('./portal/src/main/resources/public/admin'));
});