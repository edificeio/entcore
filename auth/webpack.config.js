var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: {
        application: './auth/src/main/resources/public/ts/app.ts',
        'validate-mail/application': './auth/src/main/resources/public/ts/validate-mail/app.ts',
        'validate-mfa/application': './auth/src/main/resources/public/ts/validate-mfa/app.ts',
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
        "angular": "angular",
        "ode-ts-client": 'window.entcore["ode-ts-client"]',
        "ode-ngjs-front": 'window.entcore["ode-ngjs-front"]',
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
                loader: 'ts-loader'
            },
            // html-loader will handle all files with `.html` but not `.lazy.html` extensions
            {
                test: /\.html$/,
                exclude: /\.lazy\.html$/,
                loader: 'html-loader',
                options: {
                  attrs: false, // Disables attributes processing
                  minimize: true,
//                  sources: false, // Disables attributes processing
                },
            },
        ]
    }
}