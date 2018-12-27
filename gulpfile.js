var gulp = require('gulp');

require('./gulpfile-admin.js');
require('./gulpfile-ts.js');
require('./admin/gulpfile.admin.js')
var loader = require('./gulpfile-loader');

gulp.task('build', loader().buildList(), () => {});
gulp.task('build-local', loader().buildLocalList(), () => {})