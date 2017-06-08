var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: {
        'ng-app': './directory/src/main/resources/public/temp/entcore/ng-app.js'
    },
    output: {
        filename: '[name].js',
        path: __dirname + 'dest'
    },
    resolve: {
        modulesDirectories: ['bower_components', 'node_modules'],
        root: path.resolve('.'),
        extensions: ['', '.js'],
        alias: {
            'jquery': path.resolve('./bower_components/jquery/dist/jquery.min.js'),
            'lodash': path.resolve('./bower_components/lodash/dist/lodash.min.js'),
            'underscore': path.resolve('./bower_components/underscore/underscore-min.js'),
            'moment': path.resolve('./bower_components/moment/min/moment-with-locales.min.js'),
            'humane-js': path.resolve('./bower_components/humane-js/humane.min.js'),
            'angular': path.resolve('./bower_components/angular/angular.min.js'),
            'angular-route': path.resolve('./bower_components/angular-route/angular-route.min.js'),
            'angular-sanitize': path.resolve('./bower_components/angular-sanitize/angular-sanitize.min.js'),
            'es6-shim': path.resolve('./bower_components/es6-shim/es6-shim.min.js')
        }
    },
    devtool: "source-map",
        module: {
        preLoaders: [
            {
                test: /\.js$/,
                loader: 'source-map-loader'
            }
        ]
    }
}