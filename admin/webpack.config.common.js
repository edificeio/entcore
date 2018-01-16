const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')

const path_prefix = './admin/src/main'

module.exports = {
    entry: {
        // styles are included in polyfills as a workaround
        // see : https://github.com/webpack/webpack/issues/1967
        'polyfills': path_prefix + '/ts/libs/polyfills.ts'
    },
    output: {
        filename: 'js/[name].js',
        chunkFilename: 'js/[name].js',
        publicPath: '/admin/public/'
    },
    resolve: {
        extensions: ['.js', '.ts']
    },
    plugins: [
        new webpack.NoEmitOnErrorsPlugin(),
        new HtmlWebpackPlugin({
            filename: '../view/admin.html',
            template: path_prefix + '/resources/view-src/admin.ejs'
        })
    ]
}