const path = require('path');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

const config = {
  entry: {
    commonLayoutComponents: './src/main/webapp/vue-app/common/main.js',
    siteNavigation: './src/main/webapp/vue-app/site-navigation/main.js',
    siteManagement: './src/main/webapp/vue-app/site-management/main.js',
    siteLayoutEditor: './src/main/webapp/vue-app/site-layout-editor/main.js',
    layoutEditor: './src/main/webapp/vue-app/layout-editor/main.js',
    pageLayout: './src/main/webapp/vue-app/page-layout/main.js',
    pageTemplates: './src/main/webapp/vue-app/page-template/main.js',
    portlets: './src/main/webapp/vue-app/portlets/main.js',
    portletEditor: './src/main/webapp/vue-app/portlet-editor/main.js',
    sectionTemplates: './src/main/webapp/vue-app/section-template/main.js',
    sectionEditor: './src/main/webapp/vue-app/section-editor/main.js',
    siteTemplates: './src/main/webapp/vue-app/site-template/main.js',
  },
  mode: 'production',
  context: path.resolve(__dirname, '.'),
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
        ]
      }
    ]
  },
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/vue-app/*.js',
        './src/main/webapp/vue-app/*.vue',
        './src/main/webapp/vue-app/**/*.js',
        './src/main/webapp/vue-app/**/*.vue',
      ]
    }),
    new VueLoaderPlugin()
  ],
  output: {
    path: path.join(__dirname, 'target/layout/'),
    filename: 'js/[name].bundle.js',
    libraryTarget: 'amd'
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
    jquery: '$',
  },
};

module.exports = config;
