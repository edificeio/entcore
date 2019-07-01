const webpack = require('webpack')
const webpackMerge = require('webpack-merge')

const commonConfig = require('./webpack.config.common.js')
const path_prefix = './admin/src/main'

const ExtractTextPlugin = require('extract-text-webpack-plugin')

module.exports = webpackMerge(commonConfig, {
    devtool: 'eval-source-map',

    entry: {
        'admin': path_prefix + '/ts/main.ts',
        'vendor': path_prefix + '/ts/libs/vendor.ts'
    },

    module: {
        rules: [
            {
                test: /\.ts$/,
                use: [
                    'ts-loader',
                    'angular-router-loader',
                    'angular2-template-loader?keepUrl=true'
                ]
            },
            {
                test: /\.html$/,
                use: 'file-loader?name=templates/[name].[ext]'
            },
            {
                test: /\.(scss|css)$/,
                use: ExtractTextPlugin.extract({
                    use: ['css-loader', 'sass-loader'],
                    allChunks: true
                })
            },
            {
                test: /\.woff$/,
                use: [{
                    loader: 'file-loader',
                    options: {
                        name: '[name].[ext]',
                        outputPath: 'styles/',
                        publicPath: './'
                    }
                }]
            },
            {
                test: /\.svg$/,
                use: [{
                    loader: 'file-loader',
                    options: {
                        name: '[name].[ext]',
                        outputPath: 'styles/',
                        publicPath: './'
                    }
                }]
            }
        ]
    },

    plugins: [
        new webpack.optimize.CommonsChunkPlugin({
            name: ['admin', 'vendor', 'polyfills']
        }),
        new ExtractTextPlugin({
            filename:  (getPath) => {
                return getPath('styles/admin.css');
            }
        }),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        })
    ],

    devServer: require('./webpack.config.devserver.js')
})