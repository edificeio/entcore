var gulp = require('./gulpfile-loader')('admin');
var less = require('gulp-less');
var sass = require('gulp-sass');
var rename = require("gulp-rename");
var merge = require('merge2');

gulp.task('copy-csslib', () => {
    var lib = gulp.src(gulp.local.paths.cssLib + '/**/*')
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib'));
    var editorTemplates = gulp.src(gulp.local.paths.cssLib + '/editor-resources/img/**/*')
        .pipe(gulp.dest('./portal/src/main/resources/public/entcore-css-lib/editor-resources/img'));

    return merge([lib, editorTemplates]);
});

gulp.task('sass', ['copy-csslib'], () => {
    return gulp.src('./portal/src/main/resources/public/libs/entcore-css-lib/_css-lib.scss')
        .pipe(rename("css-lib.scss"))
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib'))
        .pipe(sass())
        .pipe(gulp.dest('./portal/src/main/resources/public/libs/entcore-css-lib/entcore-css-lib'))
});

gulp.task('build', ['sass'], () => {
    return gulp.src('./portal/src/main/resources/public/admin/**/theme.less')
        .pipe(less())
        .pipe(gulp.dest('./portal/src/main/resources/public/admin'));
});