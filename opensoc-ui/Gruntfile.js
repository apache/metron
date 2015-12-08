module.exports = function (grunt) {
  grunt.initConfig({
    // copies frontend assets from bower_components into project
    // bowercopy: {
    //   options: {
    //     clean: true
    //   },
    //   css: {
    //     options: {
    //       destPrefix: 'lib/public/css/vendor'
    //     },
    //     files: {
    //       'bootstrap.css': 'bootstrap/dist/css/bootstrap.css',
    //       'bootstrap-theme.css': 'bootstrap/dist/css/bootstrap-theme.css'
    //     }
    //   },
    //   libs: {
    //     options: {
    //       destPrefix: 'lib/public/js/vendor'
    //     },
    //     files: {
    //       'angular.js': 'angular/angular.js'
    //     }
    //   }
    // }
  });

  grunt.loadNpmTasks('grunt-bowercopy');
};
