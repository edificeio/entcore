const webpack = require('webpack')
const webpackMerge = require('webpack-merge')

const commonConfig = require('./webpack.config.common.js')
const path_prefix = './admin/src/main'

module.exports = webpackMerge(commonConfig, {
    devtool: 'cheap-module-eval-source-map',

    entry: {
        'admin': path_prefix + '/ts/app.ts',
        'vendor': path_prefix + '/ts/libs/vendor.ts'
    },

    module: {
        rules: [
            {
                test: /\.ts$/,
                use: [
                    'ts-loader?tsconfig=' + path_prefix + '/ts/tsconfig.json',
                    'angular-router-loader',
                    'angular2-template-loader?keepUrl=true'
                ]
            },
            {
                test: /\.html$/,
                use: 'file-loader?name=templates/[name].[ext]'
            }
        ]
    },

    plugins: [
        new webpack.optimize.CommonsChunkPlugin({
            name: ['admin', 'vendor', 'polyfills']
        })
    ],

    devServer: require('./webpack.config.devserver.js')
})