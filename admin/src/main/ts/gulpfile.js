var gulp = require('gulp')
var rename = require('gulp-rename');
var clean = require('gulp-clean');

function copy(srcDir, destDir) {
    console.log(`Copying ${srcDir} to ${destDir}...`);
    return gulp.src(srcDir)
        .pipe(gulp.dest(destDir));
}

function renameIndexFile(srcIndexFile, newIndexFileBasename, destDir) {
    console.log(`Renaming ${srcIndexFile} to ${destDir}${newIndexFileBasename}`);
    return gulp.src(srcIndexFile)
        .pipe(rename(function (path) { path.basename = newIndexFileBasename; }))
        .pipe(gulp.dest(destDir));
}

function clean(dir) {
    console.log(`Deleting ${dir}`);
    return gulp.src(dir, {read: false})
        .pipe(clean({force: true}));
}

gulp.task('build', () => {
    clean('../resources/public/dist');
    copy('./dist/**/*', '../resources/public/dist/');
    return renameIndexFile('./dist/index.html', 'admin', '../resources/view/');
});

gulp.task('buildWithDockerPath', () => {
    clean('../resources/public/dist');
    copy('/home/node/app/dist/**/*', '/home/node/base/src/main/resources/public/dist/');
    return renameIndexFile('/home/node/app/dist/index.html', 'admin', '/home/node/base/src/main/resources/view/');
});
