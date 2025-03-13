const path = require('path');
const { merge } = require('webpack-merge');
const webpackProductionConfig = require('./webpack.prod.cjs');
module.exports = merge(webpackProductionConfig, {
  output: {
    path: '/exo-server/webapps/layout/',
    filename: 'js/[name].bundle.js'
  }
});