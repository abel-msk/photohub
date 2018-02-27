module.exports = function(grunt) {
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),


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
            },


            //----------------------------------------------------------------------------------------
            //
            //     Подготовка библиотек
            //
            //----------------------------------------------------------------------------------------
            copylibs: {
                files: [

                    //
                    //    Copy LIBS
                    //
                    {
                        expand: true,
                        nonull: true,
                        flatten: true,
                        filter: 'isFile',
                        cwd: 'node_modules/',
                        src: [
                            'backbone/backbone.js',
                            'bootstrap/dist/js/bootstrap.js',
                            //'dropzone/dist/dropzone.js',
                            'jquery/dist/jquery.js',
                            'moment/moment.js',    //moment/locale/ru.js
                            'pikaday/pikaday.js',
                            'requirejs/require.js',
                            'underscore/underscore.js'
                        ],
                        dest: 'src/js/lib/'
                    },

                    {
                        expand: true,
                        nonull: true,
                        flatten: true,
                        filter: 'isFile',
                        cwd: 'node_modules/',
                        src: [
                            'moment/locale/ru.js'
                        ],
                        dest: 'src/js/lib/locale/'
                    },

                    //
                    //    Copy FONTS
                    //
                    {
                        expand: true,
                        nonull: true,
                        flatten: true,
                        filter: 'isFile',
                        cwd: 'node_modules/',
                        src: [
                            'font-awesome/fonts/**',
                            'bootstrap/fonts/**'
                        ],
                        dest: 'src/fonts/'
                    },

                    //
                    //    Copy CSS
                    //
                    {
                        expand: true,
                        nonull: true,
                        flatten: true,
                        filter: 'isFile',
                        cwd: 'node_modules/',
                        src: [
                            //'font-awesome/fonts/**',
                            'font-awesome/css/font-awesome.css',
                            'bootstrap/dist/css/bootstrap.css',
                            'pikaday/css/pikaday.css',
                        ],
                        dest: 'src/css/'
                    }
                ]
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
                                'logger',
                                'const'
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
    //grunt.loadNpmTasks('grunt-bower');            //  Копируем библиотеки  установленный через bower



    //==========================================================================================
    //
    //      Задачи для компиляции
    //

    grunt.registerTask('update',['copy:copylibs']);

    grunt.registerTask('processMin',['copy:main','useminPrepare','concat', 'cssmin','uglify','usemin']);
    grunt.registerTask('deploy',['clean:all','requirejs','processMin','imagemin','htmlmin']);
    grunt.registerTask('default',['deploy']);
    grunt.registerTask('lesstest', ['less:development']);
};