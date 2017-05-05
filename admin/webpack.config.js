var webpack = require('webpack')
var HtmlWebpackPlugin = require('html-webpack-plugin')
var CommonsChunkPlugin = require("webpack/lib/optimize/CommonsChunkPlugin")

let path_prefix = './admin/src/main'

module.exports = {
    entry: {
        'admin': path_prefix + '/ts/app.ts',
        'vendor': path_prefix + '/ts/libs/vendor.ts',
        'polyfills': path_prefix + '/ts/libs/polyfills.ts',
    },
    output: {
        //path: path_prefix + '/resources/public',
        filename: 'js/[name].js',
        chunkFilename: '[id].js',
        publicPath: '/admin/public/'
    },
    resolve: {
        alias: {
            'infra-components/dist': 'infra-components/dist/bundle/infra-components.bundle.js',
            'sijil': 'sijil/dist/bundles/sijil.module.umd.js'
        },
        extensions: ['', '.js', '.ts']
    },
    module: {
        loaders: [{
            test: /\.ts$/,
            loaders: [
                'ts-loader?tsconfig=' + path_prefix + '/ts/tsconfig.json'
            ]
        }, {
            test: /\.html$/,
            loader: 'file?name=templates/[name].[ext]'
        }]
    },
    devtool: "source-map",
    plugins: [
        new CommonsChunkPlugin({
            name: ['admin', 'vendor', 'polyfills']
        }),
        new webpack.NoErrorsPlugin(),
        new webpack.optimize.DedupePlugin()
        /*,new webpack.optimize.UglifyJsPlugin({
            mangle: {
                keep_fnames: true
            },
            compress: {
                warnings: false
            }
        })*/
    ]
}