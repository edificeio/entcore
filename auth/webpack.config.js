var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: {
        application: './auth/src/main/resources/public/ts/app.ts',
        behaviours: './auth/src/main/resources/public/ts/behaviours.ts'
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