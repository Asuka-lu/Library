module.exports = {
    // 改成数组形式
    transpileDependencies: ['vue-router'],

    devServer: {
        port: 9876,
        proxy: {
            '/api': {
                target: 'http://localhost:9090',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': ''
                }
            }
        }
    }
}