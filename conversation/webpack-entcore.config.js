var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: {
        'ng-app': './auth/src/main/resources/public/temp/entcore/ng-app.js'
    },
    output: {
        filename: '[name].js',
        path: __dirname + 'dest'
    },
    resolve: {
        modulesDirectories: ['bower_components', 'node_modules'],
        root: path.resolve('.'),
        alias: {
            'jquery': path.resolve('./node_modules/jquery/dist/jquery.min.js'),
            'underscore': path.resolve('./node_modules/underscore/underscore-min.js'),
            'moment': path.resolve('./node_modules/moment/min/moment-with-locales.min.js'),
            'humane-js': path.resolve('./node_modules/humane-js/humane.min.js'),
            'angular': path.resolve('./node_modules/angular/angular.min.js'),
            'angular-route': path.resolve('./node_modules/angular-route/angular-route.min.js'),
            'angular-sanitize': path.resolve('./node_modules/angular-sanitize/angular-sanitize.min.js')
        },
        extensions: ['', '.js']
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