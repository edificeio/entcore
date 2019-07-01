const webpack = require('webpack')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')
const ExtractTextPlugin = require('extract-text-webpack-plugin')
const ngtools = require('@ngtools/webpack')
const webpackMerge = require('webpack-merge')

const commonConfig = require('./webpack.config.common.js')

const ENV = process.env.NODE_ENV = process.env.ENV = 'production'
const path_prefix = './admin/src/main'

module.exports = webpackMerge(commonConfig, {
    devtool: 'source-map',

    entry: {
        'admin': path_prefix + '/ts/main.aot.ts',
        'vendor': path_prefix + '/ts/libs/vendor.aot.ts'
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
                        name: '[name].[hash].[ext]',
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
            entryModule:    __dirname + '/src/main/ts/app/app.module#AppModule'
        }),
        new ExtractTextPlugin({
            filename:  (getPath) => {
                return getPath('styles/admin.[hash].css');
            }
        }),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        })
    ]
})
