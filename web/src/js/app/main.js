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
        "imageInfo","logger","upload","utils","viewImgLoader","itemSelection","bootstrap"],
    function ($,Login,Dialog,api,Menu,Filter,ImageInfo,logger,Upload,Utils,ViewImgLoader,ItemSelection) {




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

        var itemSelection = new  ItemSelection();

        var MENU_SELECTION = 'selection-submenu';
        var MENU_BASE = 'base-submenu';
        var MENU_FILTER = "filter-submenu";
        var SCROLLED_CATALOG = 'scroller-catalog';
        var UPLOAD_DIALOG_BTN = 'upload-panel';
        //var selectedItems = [];
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

        $("body")
            .off("menu.close")
            .on("menu.close",function(event,menuname) {
                if ( menuname === MENU_SELECTION ) {
                    itemSelection.clean();
                    $('.selected').toggleClass("selected", false);
                }
            })

            .off("photorendered")
            .on("photorendered",function(event) {

                //logger.debug("[main.renderSelected] Got event for object for id="+event.detail.id+" event=",event);

                renderSelected(event.detail.id,event.detail.element);
            })
            .off("click","#del-selected")
            .on("click","#del-selected", function(event){
                //logger.debug("[main.del-selected] Got event delete selected");
                removeSelected();
            });

        ;


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
            .off('click',"div.img-select")
            .on('click',"div.img-select",function(event) {
                var imgFrame = _getParent(this,"img-frame");
                logger.debug("[init.info.click] Got element:",imgFrame);
                itemSelect(imgFrame);
                event.stopImmediatePropagation();
            })
            .off('click', '.img-info')
            .on('click', '.img-info', function (event) {
                var imgFrame = _getParent(this,"img-frame");
                logger.debug("[init.info.click] Got element:",imgFrame);
                imageInfo.open(imgFrame, imgFrame.getAttribute('data-id'));
                event.stopImmediatePropagation();
            })
            .off('click', '.img-bg')
            .on('click', '.img-bg', function () {
                var cs = getComputedStyle(this);
                // var imgFrame = _getParent(this,"img-frame");
                // var imgData = {
                //     'imgId':imgFrame.getAttribute('data-id'),
                //     'imgPos':imgFrame.getAttribute('data-count')
                // };
                // imgFrame = _getParent(this,"content-page");
                //
                // imgData.imgPos = parseInt(imgData.imgPos) + parseInt(imgFrame.getAttribute("data-offset"));
                // logger.debug("[init.img.click] Got image id ="+imgData.imgId+", pos="+imgData.imgPos);
                //
                // viewImgLoader.open(imgData.imgId,imgData.imgPos,parseInt(cs.width),parseInt(cs.height));
                //

                var imgFrame = _getParent(this,"img-frame");
                var pageFrame = _getParent(this,"content-page");

                viewImgLoader.open({
                    'id'      : imgFrame.getAttribute('data-id'),
                    'pos'     : parseInt(imgFrame.getAttribute('data-count')) + parseInt(pageFrame.getAttribute("data-offset")),
                    'width'   : parseInt(cs.width),
                    'height'  : parseInt(cs.height),
                    'mimeType': imgFrame.getAttribute('data-mimetype')
                });

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

            //   DO UNSELECT
            if (isSelected) {
                itemSelection.removeItem(itemId);

                itemElement.classList.remove("selected");

                // for ( var i = 0; i < selectedItems.length; i++) {
                //     if (selectedItems[i].objectId == itemId) {
                //         selectedItems.splice(i,1);
                //         break;
                //     }
                // }

                //
                //  Если больше не осталось выделенных елементов закрываем меню
                //
                if ( itemSelection.length() <= 0 ) {
                    menu.closeMenu(MENU_SELECTION);
                }
            }

            //
            //  Если элемент(Фотка) не выделен, то выделяем.
            //

            //   DO SELECT
            else {

                itemSelection.putItem(itemElement);

                // var itemCount = parseInt(_getParent(itemElement,"content-page").getAttribute("data-offset")) +
                //     parseInt(itemElement.getAttribute("data-count"));
                //
                // selectedItems.push({
                //         'count': itemCount,
                //         'objectId': itemId
                //     });

                itemElement.classList.add("selected");

                //
                //   Во время выделения первого элемента открываем меню
                if (itemSelection.length() == 1)  menu.openMenu(MENU_SELECTION);
            }
        }



        //------------------------------------------------------------------------
        //
        //    Перед отрисовкаой каждого элемента   проверяем  помечен ли он как выделенный
        //    Если так, рендереим как выделенный
        //
        //------------------------------------------------------------------------
        function renderSelected(id,element) {
            //logger.debug("[main.renderSelected] Got event for object id="+id, element);
            if ( itemSelection.getItem(id) ) {
                element.classList.add("selected");
            }

        }

        //------------------------------------------------------------------------
        //
        //
        //------------------------------------------------------------------------

        function viewNext(offset) {
            if ( filter ) {
                filter.loadSingle(offset, function(object) {
                    var mediaObj = loadDefaultMedia(object);
                    if (mediaObj) {
                        //viewImgLoader.append(object.id, offset, mediaObj.width, mediaObj.height);
                        viewImgLoader.append({
                            'id': object.id,
                            'pos': offset,
                            'width': mediaObj.width,
                            'height': mediaObj.height,
                            'mimeType': mediaObj.mimeType
                        });
                    }
                });
            }
        }

        function viewPrev(offset) {
            if ( filter ) {
                filter.loadSingle(offset, function(object) {
                    var mediaObj =loadDefaultMedia(object);
                    if (mediaObj) {
                        //viewImgLoader.prepend(object.id, offset, mediaObj.width,mediaObj.height);
                        viewImgLoader.append({
                            'id': object.id,
                            'pos': offset,
                            'width': mediaObj.width,
                            'height': mediaObj.height,
                            'mimeType': mediaObj.mimeType
                        });
                    }
                });
            }
        }

        //------------------------------------------------------------------------
        //
        //
        //------------------------------------------------------------------------

        function loadDefaultMedia (object) {
            // var type = object.mediaType.substring(0,object.mediaType.index("/"));
            // var mt = MEDIA_IMAGE;
            // if ( type === "video") {
            //     mt = MEDIA_VIDEO;
            // }
            for (var i = 0; i < object.mediaObjects.length; i++) {
                if  (object.mediaObjects[i].mimeType == object.mediaType) {
                    return object.mediaObjects[i];
                }
            }
            return null;
        }

        //------------------------------------------------------------------------
        //
        //  Check login and loaded catalog
        //
        //  Берет масив объектов описывающий выделенный элемент и передает его в Scroller

        //------------------------------------------------------------------------
        function removeSelected() {

            if (filter && (! itemSelection.isEmpty())) {

                //var hashArray = itemSelection.getHash();

                filter.getFilteredList().removeItems(itemSelection.getHash());


                itemSelection.clean();
                $('.selected').toggleClass("selected", false);
                menu.closeMenu(MENU_SELECTION);
            }
        }


        //------------------------------------------------------------------------
        //
        //      Lad single object photo by their ID
        //
        function loadObjectbyId(objectId, callback) {

        }







    }); // on document ready

});