const path = require('path');
const { merge } = require('webpack-merge');
const webpackProductionConfig = require('./webpack.prod.js');
module.exports = merge(webpackProductionConfig, {
  mode: 'development',
  output: {
    path: 'C:/Users/samue/Desktop/Servers/plfent-7.3.x-mips-20260706.163615-31/platform-7.3.x-mips-SNAPSHOT',
    filename: 'js/[name].bundle.js'
  }
});