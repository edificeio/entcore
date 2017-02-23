const webpack = require('webpack')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')
const ngtools = require('@ngtools/webpack')
const webpackMerge = require('webpack-merge')

const commonConfig = require('./webpack.config.common.js')

const ENV = process.env.NODE_ENV = process.env.ENV = 'production'
const path_prefix = './admin/src/main'

module.exports = webpackMerge(commonConfig, {
    devtool: 'source-map',

    entry: {
        'admin': path_prefix + '/ts/app.aot.ts'
    },

    output: {
        filename: 'js/[name].[hash].js',
        chunkFilename: 'js/[name].[hash].js'
    },

    module: {
        rules: [
            {
                test: /\.ts$/,
                loader: '@ngtools/webpack',
            },
            {
                test: /\.html$/,
                use: 'html-loader'
            }
        ]
    },

    plugins: [
        new webpack.optimize.CommonsChunkPlugin({
            name: ['admin', 'polyfills']
        }),
        new webpack.DefinePlugin({
            'process.env': {
                'ENV': JSON.stringify(ENV)
            }
        }),
        new UglifyJsPlugin({
            screw_ie8: true,
            sourceMap: true,
            comments: false
        }),
        new ngtools.AotPlugin({
            tsConfigPath:   __dirname + '/src/main/ts/tsconfig.aot.json',
            entryModule:    __dirname + '/src/main/ts/admin.module#AdminModule'
        })
    ]
})
