const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const ExtractTextPlugin = require('extract-text-webpack-plugin')

const path_prefix = './admin/src/main'

module.exports = {
    entry: {
        'polyfills': path_prefix + '/ts/libs/polyfills.ts',
        'style': path_prefix + '/resources/public/styles/admin.scss'
    },
    output: {
        filename: 'js/[name].js',
        chunkFilename: 'js/[name].js',
        publicPath: '/admin/public/'
    },
    resolve: {
        extensions: ['.js', '.ts']
    },
    module: {
        rules: [
            {
                test: /\.scss$/,
                use: ExtractTextPlugin.extract({
                    use: ['css-loader', 'sass-loader']
                })
            }
        ]
    },
    plugins: [
        new webpack.NoEmitOnErrorsPlugin(),
        new HtmlWebpackPlugin({
            filename: '../view/admin.html',
            template: path_prefix + '/resources/view-src/admin.ejs'
        }),
        new ExtractTextPlugin('styles/admin.css')
    ]
}