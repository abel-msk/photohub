/**
 * Created by abel on 18.10.15.
 */
// requirejs.config({
//     baseUrl: "js/app",
//     // packages: [{
//     //     name: 'moment',
//     //     // This location is relative to baseUrl. Choose bower_components
//     //     // or node_modules, depending on how moment was installed.
//     //     location: '../lib/moment/',
//     //     main: 'moment',
//     //
//     // }],
//     // deps: [
//     //     'moment/locale/ru'
//     // ],
//
//     paths: {
//         'moment':'../lib/moment/moment',
//         'locale/ru':'../lib/moment/locale/ru',
//
//         'jquery': '../lib/jquery/dist/jquery',
//         'backbone': '../lib/backbone/backbone-min',
//         'underscore': '../lib/underscore/underscore-min',
//         // 'storage': '../lib/jStorage/jstorage.min',
//         // 'json': '../lib/json2/json2',
//         'bootstrap': '../lib/bootstrap/dist/js/bootstrap.min',
//         'dropzone': '../lib/dropzone/dist/dropzone-amd-module',
//         'pikaday': '../lib/pikaday/pikaday'
//     },
//     shim: {
//         // 'locale/ru': {
//         //     deps:['moment']
//         // },
//         'bootstrap' : {
//             deps:['jquery']
//         },
//         'underscore': {
//             exports: '_'
//         },
//         'backbone': {
//             deps: ['underscore', 'jquery'],
//             exports: 'Backbone'
//         },
//         // 'json': {
//         //     exports: 'JSON'
//         // },
//         // 'storage': {
//         //     deps: ['json', 'jquery']
//         // },
//         'pikaday': {
//             deps: ['jquery',"moment"]
//         }
//     }
//
// });


define(["jquery","login","modalDialog","api","menu","filter",
        "imageInfo","logger","upload","utils","viewImgLoader","bootstrap"],
    function ($,Login,Dialog,api,Menu,Filter,ImageInfo,logger,Upload,Utils,ViewImgLoader) {

    function _getParent(element,className) {
        var res = element;
        while((! res.classList.contains(className)) && (res.tagName != "body")) {
            res = res.parentElement;
        }
        return res;
    }

    $(document).ready(function() {
        console.log("APP START");
        "use strict";

        var MENU_SELECTION = 'selection-submenu';
        var MENU_BASE = 'base-submenu';
        var MENU_FILTER = "filter-submenu";
        var SCROLLED_CATALOG = 'scroller-catalog';
        var UPLOAD_DIALOG_BTN = 'upload-panel';
        var selectedItems = [];
        var filter = null;

        var menu = new Menu(
            {
                'triggerBindTag':'body',
                'menuList': [
                    { 'menuTagId': MENU_BASE,       'toggleBtnId': 'base-menu-toggle'},
                    { 'menuTagId': MENU_SELECTION,  'toggleBtnId': null },
                    { 'menuTagId': MENU_FILTER,     'toggleBtnId': 'filter-menu-toggle' }
                ]
            }
        );

        $("body").on("menu.close",function(event,menuname) {
            if ( menuname === MENU_SELECTION ) {
                selectedItems = [];
                $('.selected').toggleClass("selected", false);
            }
        });

        var uploader = new Upload({
            'dialogId': UPLOAD_DIALOG_BTN,
            'onOpen':null,
            'onCLose': function(){menu.closeMenu(MENU_BASE);}
        });


        var viewImgLoader = new ViewImgLoader({
            'loadNext': viewNext,
            'loadPrev': viewPrev
        });


        var imageInfo = new ImageInfo({'dialogId': '#photo_info'});

        //------------------------------------------------------------
        //   Navigate to sites page
        //------------------------------------------------------------
        $('#load-sites').on('click',function(event) {
            window.location.replace('sites.html');
            //window.history.replaceState({}, 'Sites', 'sites.html');
        });

        //------------------------------------------------------------
        //   Scroll area resizing  processing and event handling
        //------------------------------------------------------------
        function resizeScrollArea() {
            var areaHeight = parseInt($(window).innerHeight()) - parseInt($("#header").outerHeight(true));
            $("#"+SCROLLED_CATALOG).outerHeight(areaHeight);
        }

        resizeScrollArea();  // Initial resize
        $('#header').on('shown.bs.collapse',resizeScrollArea).on('hidden.bs.collapse',resizeScrollArea);
        $(window).on('resize',resizeScrollArea);


        //------------------------------------------------------------
        //   Image selection processing
        //------------------------------------------------------------
        $("#"+SCROLLED_CATALOG)
            .on('click',"div.img-select",function(event) {
                var imgFrame = _getParent(this,"img-frame");
                logger.debug("[init.info.click] Got element:",imgFrame);
                itemSelect(imgFrame);
                event.stopImmediatePropagation();
            })
            .on('click', '.img-info', function (event) {
                var imgFrame = _getParent(this,"img-frame");
                logger.debug("[init.info.click] Got element:",imgFrame);
                imageInfo.open(imgFrame, imgFrame.getAttribute('data-id'));
                event.stopImmediatePropagation();
            })
            .on('click', '.img-bg', function () {
                var cs = getComputedStyle(this);
                var imgFrame = _getParent(this,"img-frame");
                var imgData = {
                    'imgId':imgFrame.getAttribute('data-id'),
                    'imgPos':imgFrame.getAttribute('data-count')
                };
                imgFrame = _getParent(this,"content-page");

                imgData.imgPos = parseInt(imgData.imgPos) + parseInt(imgFrame.getAttribute("data-offset"));
                logger.debug("[init.img.click] Got image id ="+imgData.imgId+", pos="+imgData.imgPos);

                viewImgLoader.open(imgData.imgId,imgData.imgPos,parseInt(cs.width),parseInt(cs.height));

            })
        ;

        //------------------------------------------------------------------------
        //
        //  Check login and loaded catalog
        //
        //------------------------------------------------------------------------
        Login.checkLogin(true, function() {
            filter = new Filter(document.getElementById(SCROLLED_CATALOG));
        });

        //------------------------------------------------------------------------
        //
        //  Make visible selection on  photo
        //
        //------------------------------------------------------------------------
        function itemSelect(itemElement) {

            //  Look for selected element in array
            var itemId = itemElement.getAttribute('data-id');
            //var $element = $('#img-frame-'+itemId);
            var isSelected = itemElement.classList.contains("selected");

            //
            //  Если этот элемент(фотка) уже выделен то снимаем выделение и удаляем из масива выделенных эл.
            //
            if (isSelected) {
                itemElement.classList.remove("selected");

                for ( var i = 0; i < selectedItems.length; i++) {
                    if (selectedItems[i].objectId == itemId) {
                        selectedItems.splice(i,1);
                        break;
                    }
                }
                //
                //  Если больше не осталось выделенных елементов закрываем меню
                if ( selectedItems.length <= 0 ) {
                    menu.closeMenu(MENU_SELECTION);
                }
            }

            //
            //  Если элемент(Фотка) не выделен, то выделяем.
            //
            else {
                var itemCount = parseInt(_getParent(itemElement,"content-page").getAttribute("data-offset")) +
                    parseInt(itemElement.getAttribute("data-count"));

                selectedItems.push({
                        'count': itemCount,
                        'objectId': itemId
                    });

                itemElement.classList.add("selected");
                //
                //   Во время выделения первого элемента открываем меню
                if ( selectedItems.length  == 1)  menu.openMenu(MENU_SELECTION);
            }
        }



        function viewNext(offset) {
            if ( filter ) {
                filter.loadSingle(offset, function(object) {
                    var mediaObj = Utils.getMediaObject(object);
                    if (mediaObj) {
                        viewImgLoader.append(object.id, offset, mediaObj.width,mediaObj.height);
                    }
                });
            }
        }

        function viewPrev(offset) {
            if ( filter ) {
                filter.loadSingle(offset, function(object) {
                    var mediaObj = Utils.getMediaObject(object);
                    if (mediaObj) {
                        viewImgLoader.prepend(object.id, offset, mediaObj.width,mediaObj.height);
                    }
                });
            }
        }



    }); // on document ready

});