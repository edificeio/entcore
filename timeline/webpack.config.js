var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: {
        'timeline/application': './timeline/src/main/resources/public/ts/timeline/app.ts',
        'externalNotifs/application': './timeline/src/main/resources/public/ts/externalNotifs/app.ts',
        'history/application': './timeline/src/main/resources/public/ts/history/app.ts',
        behaviours: './timeline/src/main/resources/public/ts/behaviours.ts'
    },
    output: {
        filename: '[name].js',
        path: __dirname + 'dest'
    },
    externals: {
        "entcore/entcore": "entcore",
        "entcore": "entcore",
        "entcore/libs/moment/moment": "entcore",
        "entcore/libs/underscore/underscore": "_",
        "entcore/libs/jquery/jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['bower_components', 'node_modules'],
        root: path.resolve(__dirname),
        extensions: ['', '.ts', '.js']
    },
    devtool: "source-map",
    module: {
        loaders: [
            {
                test: /\.ts$/,
                loader: 'awesome-typescript-loader'
            }
        ]
    }
}