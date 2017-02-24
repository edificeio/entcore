const path_prefix = './admin/src/main'

module.exports = {
    contentBase: path_prefix + "/resources/public",
    compress: true,
    port: 9000,
    publicPath: '/admin/public/',
    proxy: {
        "**": {
            target: "http://localhost:8090"
        }
    },
    stats: {
		colors: true
	}
}