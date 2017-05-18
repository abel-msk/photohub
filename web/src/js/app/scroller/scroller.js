/**
 * Created by abel on 07.12.16.
 */


/*-------------------------------------------------------------------------------------------------

    Scroller class

    Наследует класс scrollerAbstract

    Инициализация
    new Scroller (options);
    options {
        'viewport':         -   фрейм внутри которого будут скролиться данные
        'loadAsLast':       -   функция callback Вызывается когда доскролили до последней страницы (блока)
        'loadAsFirst':      -   функция callback Вызывается когда доскролили до первой страницы (блока)
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

 -------------------------------------------------------------------------------------------------*/


define(["jquery","scroller/domUtils","scroller/dataPage","scroller/scrollAbstract","logger"],
    function ($,DomUtils,Page,Abstract,logger) {

    "use strict";


        var DEBUG = true;
        var TRACE = true;

        function Scroller(optParams) {
            try {
                if (optParams) {
                    this.loadAsLast = optParams.loadAsLast;
                    this.loadAsFirst = optParams.loadAsFirst;
                    this.viewport = optParams.viewport;
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

                var page = new Page(dataList, {
                    'limit': options.limit,
                    'offset': options.offset,
                    'id': newPageId,
                    'viewport': this.container
                });

                //this.insertAtLast(page.getElement());
                Abstract.prototype.append.apply(this,[{'element': page.getElement(), 'data': page}, true]);

                //  Добавляем или переписываем новую страницу в loadedPages
                this.loadedPages[newPageId] = {'limit': page.getOptions().limit, 'offset': options.offset};

                DEBUG && logger.debug("[Scroller.append]  New page id=" + newPageId
                    + ", limit="+this.loadedPages[newPageId].limit
                    + ", offset="+options.offset
                    + ", clear loadedPages from pos="+ (newPageId + 1)
                    , this.loadedPages,page);

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
                    'renderAll': true
                });

                //this.insertAtFirst(page.getElement());
                var removedPage = Abstract.prototype.prepend.apply(this,[{'element': page.getElement(), 'data': page},true]);

                DEBUG && logger.debug("[Scroller.prepend]  New page id=" + newPageId
                    + ", limit="+this.loadedPages[newPageId].limit
                    + ", offset="+options.offset
                    + ", remove loadedPages from pos="+removedPage.data.getId()
                    , this.loadedPages,page);

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
                +", el offset="+this.currentView.offset
                ,element);

            return scrollPos;
        };


        //------------------------------------------------------------------------------------------------
        //
        //    _onScroll
        //    Вычисляем позицию центар видимой области в терминах смещения скролируемого документа
        //    и ищейм врейм в которую она попадает.
        //    ссылку на  объект для фрейм сохраняем для последующего возврата.
        //    Вызывается как калбфесо для onScrollBefore
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

                            TRACE && logger.debug("[Scroller.onScrollBefore] Viewport scroll pos="+ this.viewport.scrollTop
                                +" View pos.x="+viewPosition.x  +", pos.y="+viewPosition.y
                            );

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
        //   Удаяем и очищаем данные
        //
        //------------------------------------------------------------------------------------------------
        Scroller.prototype.destroy = function() {
            var caller = this;
            this.destroy();
            window.removeEventListener("resize",function(){ caller._onResize.call(caller); });
            delete this.abstract;
        };


    return Scroller;
});