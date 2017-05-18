module.exports = function(grunt) {
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        //----------------------------------------------------------------------------------------
        //
        //   Устанавливаем, загруженные через bower библиотеки в рабочую директорию  src/js/lib
        //
        //----------------------------------------------------------------------------------------

        bower: {
            dev: {
                dest: 'src/js/app/',
                js_dest:"src/js/lib",
                css_dest: 'src/css',
                fonts_dest: 'src/fonts',
                options: {
                    //stripAffix: true,
                    keepExpandedHierarchy: false,
                    //stripGlobBase: true,
                    packageSpecific: {
                        'moment' : {
                            keepExpandedHierarchy: true,
                            files: [
                                'moment.js',
                                'locale/ru.js',
                                'locale/en.js'
                            ]
                        },
                        'bootstrap' : {
                            files: [
                                'dist/css/bootstrap.css',
                                'dist/fonts/**',
                                'dist/js/bootstrap.js'
                            ]
                        },
                        'font-awesome': {
                            files: [
                                'css/font-awesome.css',
                                'fonts/**'
                            ]
                        },
                        'dropzone': {
                            files:[
                                'dist/dropzone-amd-module.js'
                            ]
                        },
                        'IE8': {
                            files:[
                                'build/ie8.max.js'
                            ]
                        }
                    }
                }
            }
        },

        //----------------------------------------------------------------------------------------
        //
        //   Очистка дистрибутива
        //
        //----------------------------------------------------------------------------------------

        clean: {
            all: [
                'build/*'
            ],
            // js_lib: [
            //     'src/js/lib/*'
            // ],
            css:[
                'build/css'
            ]

        },

        //----------------------------------------------------------------------------------------
        //
        //     Сборка дистрибутива
        //
        //----------------------------------------------------------------------------------------

        less: {
            development: {
                // options: {
                //     paths: ["src/less/"]
                // },
                files: {
                    'src/css/photohub.css': 'src/less/photohub.less'
                }
            }
            //production: {
            //    options: {
            //        paths: ["src/"],
            //        cleancss: true
            //    },
            //    files: {"css/photohub.css": "src/less/*.less"}
            //}
        },


        //-------------------------------------------------
        //   Копируем html файлы в дистрибутивную папку
        //
        copy: {
            main: {
                expand: true,
                cwd:'src/',
                src: [
                    'fonts/**',
                    '*.html',
                    //'css/*'
                    'property.json'

                ],
                dest: 'build/'
            }
        },

        //-------------------------------------------------
        //   Минифицируем картинки
        //
        imagemin: {
            dynamic: {
                files: [{
                    expand: true,
                    cwd: 'src/',
                    src: ['img/*.{png,jpg,gif}'],
                    dest: 'build/'
                }]
            }
        },






        //-------------------------------------------------
        //   Объединяем и минимизируем  CSS файлы
        //

        useminPrepare: {
            html: [
                'build/index.html',
                'build/sites.html'
            ],
            options: {
                root: './src/',
                dest: 'build/'
            }
        },
        usemin: {
            html: [
                'build/index.html',
                'build/sites.html'
            ],
            options: {
                root: './src/',
                dest: 'build/'
            }
        },


        htmlmin: {
            dist: {
                options: {
                    removeComments: true,
                    collapseWhitespace: false
                },
                files: {
                    'build/index.html': 'build/index.html',
                    'build/sites.html': 'build/sites.html'
                }
            }
        },

        //----------------------------------------------------------------------------------------
        //
        //   Оптимизируем и устанавливам JS  requireJS код и библиотеки
        //
        //----------------------------------------------------------------------------------------

        requirejs: {   //https://github.com/requirejs/example-multipage/blob/master/tools/build.js
            compile: {
                options: {
                    appDir: 'src',
                    //baseUrl: '../',
                    mainConfigFile: 'src/js/common.js',
                    dir: 'build',
                    optimize: 'uglify2',
                    removeCombined: true,
                    findNestedDependencies: true,
                    optimizeAllPluginResources: true,
                    fileExclusionRegExp: /^\.|^less|^img$|^fonts|^css|.*?html$|.*?txt$/,
                    
                    modules: [
                        //First set up the common build layer.
                        {
                            //module names are relative to baseUrl
                            name: '../common',
                            //List common dependencies here. Only need to list
                            //top level dependencies, "include" will find
                            //nested dependencies.
                            include: ['jquery',
                                'moment',
                                'locale/ru',
                                'backbone',
                                'underscore',
                                'bootstrap',
                                'dropzone',
                                'pikaday',
                                'api',
                                'utils',
                                'modalDialog',
                                'login',
                                'logger'
                            ]
                        },

                        //Now set up a build layer for each page, but exclude
                        //the common one. "exclude" will exclude
                        //the nested, built dependencies from "common". Any
                        //"exclude" that includes built modules should be
                        //listed before the build layer that wants to exclude it.
                        //"include" the appropriate "app/main*" module since by default
                        //it will not get added to the build since it is loaded by a nested
                        //requirejs in the page*.js files.
                        {
                            //module names are relative to baseUrl/paths config
                            name: '../init',
                            include: ['main'],
                            exclude: ['../common']
                        },

                        {
                            //module names are relative to baseUrl
                            name: '../sites',
                            include: ['sitesMain'],
                            exclude: ['../common']
                        }

                    ]
                }
            }
        }



        // watch: {
        //     less: {
        //         files: "src/less/*",
        //         tasks: ["less:development"]
        //     },
        //     lib: {
        //         files: "bower_components/**"
        //     }
        // }

    });

    //==========================================================================================
    //
    //      ПОДГРУЗКА ДОП МОДУЛЕЙ
    //


    //   Очистка перед компиляцией.
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');  //  Копируем
    grunt.loadNpmTasks('grunt-text-replace');  //  Заменяем текст внутри
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-imagemin');

    // grunt.loadNpmTasks('grunt-processhtml');
    grunt.loadNpmTasks('grunt-usemin');
    grunt.loadNpmTasks('grunt-contrib-htmlmin');

    //-------------------------------------------------
    //   Перевод LESS файлов в CSS
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-watch');

    //-------------------------------------------------
    //   Работаем с bower и require js
    grunt.loadNpmTasks('grunt-contrib-requirejs');
    grunt.loadNpmTasks('grunt-bower');            //  Копируем библиотеки  установленный через bower



    //==========================================================================================
    //
    //      Задачи для компиляции
    //

    grunt.registerTask('bowerinstall',['bower']);

    grunt.registerTask('processMin',['copy','useminPrepare','concat', 'cssmin','uglify','usemin']);
    grunt.registerTask('deploy',['clean:all','requirejs','processMin','imagemin','htmlmin']);
    grunt.registerTask('default',['deploy']);
    grunt.registerTask('lesstest', ['less:development']);
};