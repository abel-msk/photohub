/**
 * Created by abel on 07.12.16.
 */


/**-------------------------------------------------------------------------------------------------

    Scroller class

    Наследует класс scrollerAbstract

    Инициализация
    new Scroller (options);
    options {
        'viewport':         -   фрейм внутри которого будут скролиться данные
        'loadAsLast':       -   функция callback Вызывается когда доскролили до последней страницы (блока)
        'loadAsFirst':      -   функция callback Вызывается когда доскролили до первой страницы (блока)
        'imageBodyHtml':null, -  Функция или  строка содержит/возвращает  HTML для  отрисовки врейма каждой фотки (thumbnail)
         rem - 'renderAll': false  -   Выводить всюсраницу не искать смену даты для установки окончания страницы
    }


    append( dataList,options )
        dataList - Список лбъектов загруженных с сервера
        options
            {
                limit
                offset
            }


    prepend( dataList,options )
        dataList - Список лбъектов загруженных с сервера
        options
            {
                limit
                offset
            }



    Информация по загруженным страницам предшествующим текущим видимым хранится в масиве this.loadedPages.
    Для каждой страницы хранится объект

    {
        'limit':
        'offset':
    };


 -------------------------------------------------------------------------------------------------*/


define(["jquery","scroller/domUtils","scroller/dataPage","scroller/scrollAbstract","logger"],
    function ($,DomUtils,Page,Abstract,logger) {

    "use strict";


        var DEBUG = true;
        var TRACE = false;

        var defaultOptions = {
            'loadAsLast':null,
            'loadAsFirst':null,
            'viewport':null,
            'imageBodyHtml':null,
            'renderAll': false
        };


        function Scroller(optParams) {

            try {
                this.options = $.extend(true, {}, defaultOptions, optParams || {});

                if (optParams) {
                    this.loadAsLast = this.options.loadAsLast;
                    this.loadAsFirst = this.options.loadAsFirst;
                    this.viewport = this.options.viewport;

                    if ( ! this.viewport) {
                        throw new Error("[Scroller.init] Error viewport parmetr required.");
                    }
                }


                var caller = this;

                //   Вызываем конструктор родительского класса
                Abstract.apply(this, [{'viewport':this.viewport}]);

                //
                //   On resize browser window event
                //
                window.addEventListener("resize",function(){ caller._onResize.call(caller); });

                //this.currentView = {'offset':-1, 'element':null};
                this.currentView = null;
                this.loadedPages = [];

            } catch (e) {
                logger.debug("[Scroller.init] Error",e);
            }
        }
        Scroller.prototype = Object.create(Abstract.prototype);
        Scroller.prototype.constructor = Abstract;


        //------------------------------------------------------------------------------------------------
        //
        //   append(dataList,options)
        //   Создает Page  объект с данными полученными из dataList и параметрами из options
        //   Генерирует HTML код для страницы и вставляет его после последней загруженной
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.append = function(dataList,options) {
            try {
                var lastPage = this.getLastViewed()?this.getLastViewed().data:null;
                var newPageId = (lastPage ? lastPage.getId() : -1) + 1;

                //   Создаеь новый объект для страницы
                var page = new Page(dataList, {
                    'limit': options.limit,
                    'offset': options.offset,
                    'id': newPageId,
                    'viewport': this.container,
                    'imageBodyHtml':  this.options.imageBodyHtml
                });

                // Добавляем новую страницу в скроллер ( Вызывем родительский класс )
                //this.insertAtLast(page.getElement());
                Abstract.prototype.append.apply(this,[{'element': page.getElement(), 'data': page}, true]);

                //  Добавляем или переписываем новую страницу в loadedPages
                this.loadedPages[newPageId] = {'limit': page.getOptions().limit, 'offset': options.offset};

                DEBUG && logger.debug("[Scroller.append]  New page id=" + newPageId
                    + ", limit="+this.loadedPages[newPageId].limit
                    + ", offset="+options.offset
                    + ", insert position " + newPageId + 1);
                TRACE && logger.debug("[Scroller.append] Page object:",page);

                this.loadedPages.splice(newPageId + 1);

            }
            catch (e) {
                logger.debug("[Scroller.append] Error:",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //   prepend(dataList,options)
        //   Создает Page  объект с данными полученными из dataList и параметрами из options
        //   Генерирует HTML код для страницы и вставляет его перед первой загруженной
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.prepend = function(dataList,options) {
            try {

                var firstPage = this.getFirstViewed()?this.getFirstViewed().data:null;
                //if (!firstPage) return;

                var newPageId = firstPage.getId() - 1;

                var page = new Page(dataList, {
                    'limit': this.loadedPages[newPageId].limit,
                    'offset': options.offset,
                    'id': newPageId,
                    'viewport': this.container,
                    'renderAll': true,
                    'imageBodyHtml':  this.options.imageBodyHtml
                });

                //this.insertAtFirst(page.getElement());
                var removedPage = Abstract.prototype.prepend.apply(this,[{'element': page.getElement(), 'data': page},true]);

                DEBUG && logger.debug("[Scroller.prepend]  New page id=" + newPageId
                    + ", limit="+this.loadedPages[newPageId].limit
                    + ", offset="+options.offset
                    + ", remove loadedPages from pos="+removedPage.data.getId());

                TRACE && logger.debug("[Scroller.prepend] Page object:" + page);


                //  Удаляем страницы из loadedPages после текущей последней видимой
                this.loadedPages.splice(removedPage.data.getId());

                //   На всякий случай после загрузки первой страницы  обнуляем держатель места в начале
                //   и возвращаем в видимую облать съехавшие фотографии
                if ( options.offset <=0 ) {
                    this.resetTop();
                    this.viewport.scrollTop = this.getScrollPos();
                }
            }
            catch (e) {
                logger.debug("[Scroller.prepend] Error:",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //    _onLast
        //    Функция вызывается когда доскролили до последней загруженной страницы
        //    Если указан loadAsLast callback вызывает его с параментрами
        //       limit  - значение limit для новой страницы или -1  если новая страница еще не загружалась
        //       offset - значение offset для новой страницы
        //       element - DOM  element  для декущей последней страницы
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype._onLast = function(datObj) {
            try {

                var curPageOptions = this.loadedPages[datObj.data.getId()];
                var newPageOptions = this.loadedPages[datObj.data.getId() + 1];

                DEBUG && logger.debug("[Scroller._onLast] calling loadAtLast. Last page id=" + datObj.data.getId()
                    + ",  new page limit=" + (newPageOptions ? newPageOptions.limit : -1)
                    + ",  new page offset=" + (newPageOptions ? newPageOptions.offset : (curPageOptions.offset + curPageOptions.limit))
                    , datObj);


                if (typeof this.loadAsLast == "function") {
                    this.loadAsLast (
                        (newPageOptions?newPageOptions.limit : -1),
                        (newPageOptions?newPageOptions.offset:(curPageOptions.offset + curPageOptions.limit)),
                        datObj.element
                    );
                }
            }
            catch (e) {
                logger.debug("[Scroller._onLast] Error:",e);
            }

        };

        //------------------------------------------------------------------------------------------------
        //
        //    _onFirst
        //    Функция вызывается когда доскролили до первой загруженной страницы
        //    Если указан loadAsFirst callback вызывает его с параментрами
        //       limit  - значение limit для новой страницы или -1  если новая страница еще не загружалась
        //       offset - значение offset для новой страницы
        //       element - DOM  element  для декущей последней страницы
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype._onFirst = function(datObj) {
            try {

                var newPageOptions = this.loadedPages[datObj.data.getId() - 1]; // Предыдущей стрницы нет
                if ( ! newPageOptions ) {
                    this.resetLoadingState();
                }
                else {
                    DEBUG && logger.debug("[Scroller._onFirst] Calling loadAsFirst. Cur page id=" + datObj.data.getId()
                        + ",  new page limit=" + (newPageOptions ? newPageOptions.limit : -1)
                        + ",  new page offset=" + (newPageOptions ? newPageOptions.offset : -1)
                        , datObj);

                    if (typeof this.loadAsFirst == "function") {
                        this.loadAsFirst(
                            (newPageOptions ? newPageOptions.limit : -1),
                            (newPageOptions ? newPageOptions.offset : -1),
                            datObj.element
                        );
                    }
                }
            }
            catch (e) {
                logger.debug("[Scroller._onFirst] Error:",e);
            }
        };


        //------------------------------------------------------------------------------------------------
        //
        //   _onResize
        //   Перетаскиваем элемент кот. раньше (до резайза) был по центру видимой область обратно в центр
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype._onResize = function() {
            try {
                var viewedAr = this.getAllViewed();

                for (var i = 0; i < viewedAr.length; i++) {
                    if (viewedAr[i]) {
                        viewedAr[i].data.redraw();
                    }
                }

                DEBUG && logger.debug("[Scroller._onResize] Scroll back to viewed element.");

               if ( this.currentView ) {
                   this.viewport.scrollTop = this.getScrollPos();
               }
            }
            catch (e) {
                logger.debug("[Scroller._onResize] Error:",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.getScrollPos = function() {

            var list =  this.currentView.pageObject.getList();
            var element = list[this.currentView.count].element;

            var scrollPos = element.getBoundingClientRect().top
                    + this.currentView.offset
                    - this.container.getBoundingClientRect().top
                    - Math.round(DomUtils.getInnerHeight(this.viewport) / 2)
                ;

            DEBUG && logger.debug("[Scroller.getScrollPos] Scroll pos="+scrollPos
                +", element count="+this.currentView.count
                +", el top="+element.getBoundingClientRect().top
                +", el offset="+this.currentView.offset);

            TRACE && logger.debug("[Scroller.getScrollPos] Current element:",element);

            return scrollPos;
        };


        //------------------------------------------------------------------------------------------------
        //
        //    _onScroll
        //    Вычисляем позицию центара видимой области в терминах смещения скролируемого документа
        //    и ищейм фрейм в которую она попадает.
        //    ссылку на  объект для фрейма сохраняем для последующего возврата.
        //    Вызывается как callback для события onScrollBefore
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.onScrollBefore = function() {
            try {
                var viewPosition = {};
                viewPosition.y = this.viewport.scrollTop + Math.round(DomUtils.getInnerHeight(this.viewport) / 2);
                viewPosition.x = Math.round(DomUtils.getInnerWidth(this.viewport) / 2);

                var viewedAR = this.getAllViewed();
                for (var i = 0; i < viewedAR.length; i++) {
                    if (viewedAR[i]) {
                        var pos = viewedAR[i].data.getElementOnScreen(viewPosition);
                        if (pos.offset >= 0) {

                            // TRACE && logger.debug("[Scroller.onScrollBefore] Viewport scroll pos="+ this.viewport.scrollTop
                            //     +" View pos.x="+viewPosition.x  +", pos.y="+viewPosition.y + ", Element ID ="+pos.element.getAttribute('data-id')
                            // );

                            this.currentView = {
                                'offset': pos.offset,
                                'id': pos.element.getAttribute('data-id'),
                                'count': pos.element.getAttribute('data-count'),
                                'pageObject': viewedAR[i].data
                            };
                            break;
                        }
                    }
                }
            }
            catch (e) {
                logger.debug("[Scroller.onScrollBefore] Error ",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //    removeItem
        //
        //    При удаленииобъекта надо указать номер страницы и id объекта на странице
        //    если номер страници  раньше чем первая видимая то сдвигаем все страницы вверх на 1 позицию
        //    (изменяем offset)
        //    Если объект на видимой странице, находим страницу с объектом,  вырезаем объект и изменяем размер страницы.
        //    во всех последующие страницы сдвигаем на 1 (изменяем offset)
        //
        //    Параметры:
        //        pageCount - порядковый номер страницы
        //        itemId    - ID объекта (не порядковый номер)
        //
        //
        //
        //------------------------------------------------------------------------------------------------
        // Scroller.prototype.removeItem = function(itemId,pageId) {
        //
        //
        //
        //
        //
        //     try {
        //         if (this._isBeforeViewed(pageId)) {
        //             this._shiftViewedPages(1,pageId);
        //             DEBUG && logger.debug("[Scroller.removeItem] Remove single Item=" + itemId + ", pageId=" + pageId + "  before all viewed page. Shift viewed.");
        //         }
        //         else if (!this._isAfterViewed(pageId)) {
        //             var page = this._getViewedPageById(pageId);
        //             //var page = this._getViewedPageById(pageId);
        //             if (page) {
        //                 page.removeItem(itemId);
        //                 page.redraw();
        //             }
        //             else {
        //                 DEBUG && logger.debug("[Scroller.removeItem] Cannot find page with ID=" + pageId, this.viewSlots);
        //                 throw new Error("[Scroller.removeItem] Problems with pages counting. Page with ID=" + pageId);
        //             }
        //         }
        //     }
        //     catch (e) {
        //             logger.trace("[Scroller.removeItem] Error:",e);
        //     }
        // };

        //------------------------------------------------------------------------------------------------
        //
        //   removePageItems
        //
        //   Удяляет список обектов из списка объектов указанной страницы
        //   Параметры:
        //       itemsAr
        //       [
        //           ID_1,
        //           ID_2,
        //           ...
        //       ]
        //
        //   ID_X -   image object id.
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.removePageItems = function(itemsAr,pageId) {
            try {
                if (itemsAr.length > 0) {
                    var delta = itemsAr.length;

                    //   Decrease page size(limit) loadedPages[newPageId]
                    this.loadedPages[pageId].limit -= delta;

                    //   Shift offset up by delta in loadedPages ar  for pages after pageId
                    for (var i = pageId + 1; i < this.loadedPages.length; i++) {
                        if (this.loadedPages[i]) {
                            this.loadedPages[i].offset -= delta;
                        }
                    }

                    DEBUG && logger.debug("[Scroller.removePageItems] Decrease page "+ pageId +" limit by" + delta +
                        ", shift pages from "+ (pageId + 1) + " to "+ (this.loadedPages.length -1));

                    //   Shift offset up by delta on in page object for pages after pageId
                    this._shiftViewedPages(delta,pageId);

                    if (!this._isAfterViewed(pageId)) {
                        var page = this._getViewedPageById(pageId);
                        if (page) {
                            for (var item in itemsAr) {
                                page.removeItem(itemsAr[item]);
                            }
                            page.redraw();
                        }
                    }
                }
            }
            catch (e) {
                logger.trace("[Scroller.removePageItems] Error:",e.stack);
            }
        };


        //------------------------------------------------------------------------------------------------
        //
        //    Decrease page size  by amount from parameter 'delta'
        //    Return offset for nest page
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype._shiftViewedPages = function(delta,pageId) {
            var ar = this.viewSlots.getArray();
            for (var i = 0; i < ar.length; i++) {
                if (( ar[i])  && (ar[i].data.getId() > pageId)) {   // lust pages after pageId
                    ar[i].data.shiftPage(delta);
                    DEBUG && logger.debug("[Scroller._shiftViewedPages] Shift by " + delta + " in view slot page " + ar[i].data.getId());
                }
            }
        };


        //------------------------------------------------------------------------------------------------
        //
        //   Перерисовываем указанную станицу, если она звгружена
        //
        //------------------------------------------------------------------------------------------------

        Scroller.prototype.redraw = function(pageId) {
            this._getViewedPageById(pageId).redraw();
        };

        //------------------------------------------------------------------------------------------------
        //
        //------------------------------------------------------------------------------------------------

        Scroller.prototype._isBeforeViewed = function(pageId) {
            return parseInt(pageId) < parseInt(this.getFirstViewed().data.getId());
        };

        Scroller.prototype._isAfterViewed = function(pageId) {
           return  parseInt(pageId) > parseInt(this.getLastViewed().data.getId());
        };




        Scroller.prototype._getViewedPageById = function(pageId) {
            var ar = this.viewSlots.getArray();
            for (var i = 0; i < ar.length; i++) {
                if (ar[i] && (ar[i].data.getId() === pageId)) {
                    return ar[i].data;
                }
            }
            return null;
        };


        /**-------------------------------------------------------------------------
         *
         *   Looking for item in list of viewed pages
         *   if fount substitute with new one and redraw
         *
         *  @param currentObjId    photo obkect id we are looking for
         *  @param newPhotoObject  the new object to substitute
         *
         --------------------------------------------------------------------------*/
        Scroller.prototype.replaceItem = function(currentObjId,newPhotoObject) {
            var foundPage = null;

            for (var i = 0; i < this.viewSlots.length(); i++) {
                var page = this.getData(i);
                if  (page) {
                    var objectsList = page.getList();

                    for (var i = 0; i < objectsList.length; i++) {
                        if (objectsList[i].id == currentObjId) {  //  Item found
                            page.replaceItem(i, newPhotoObject);
                            foundPage = page;
                            break;
                        }
                    }
                    if (foundPage) {
                        break;
                    }
                }
            }

            //   We found page, substitute item so redraw
            // if (foundPage) {
            //     foundPage.redraw();
            // }
        };


        //------------------------------------------------------------------------------------------------
        //
        //   Удаяем и очищаем данные
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.destroy = function() {

            logger.debug("[Scroller.destroy]");
            var caller = this;
            //this.destroy();
            Abstract.prototype.destroy.apply(this);
            window.removeEventListener("resize",function(){ caller._onResize.call(caller); });
            delete this.abstract;
        };


    return Scroller;
});