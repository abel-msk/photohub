/**
 * Created by artem.belyaev@gmail.com on 07.12.16.
 */


/*-------------------------------------------------------------------------------------------------

     ScrollerAbstract

     Выполняет  infinite scroll  масива элементов

     Если во время скрола вниз на экране стал видим последний элемента масива
     запрашиваем добавление новго элемента масиива.   Новый элемент вставляется в скролируемый
     фрейм и во внутренний масив вызовами onLastViewed и append  соответственно.
     Элемент вставляется в масив в конце, при этома все элементы масива поднимаютмя на одну позицию вверх.
     верхний элемент удаляется из масива и из скролируемого фрейма, вместо него в скролируемом фрейме
     вставляется элемент такого же размера но пустой (минимизируя общее DOM дерево), таким образом
     рассположение видимых элментов в скролируемом спске не меняется.
     При последующем добавлении в конец масива  удаляется еще один элемент сверху а пустой элемент
     в начале увеличивается на размер удаленного элемента.

     При скроде вверх, алгоритм повторяется зеркально. удаляе элементы из конца масива а вместо него
     вставляя илли увеоичивая  пустой элемент такого же размера.

     При таком подходе в памяти  и в дом дереве всегда содердится не более И элементоа  в районе
     видимой части.

     Options
     viewport             DOM элемент внутри которого скролится содержимое
     memSlots             количество элементов одновременно храняшихся в памяти
     onScrollBefore       callback - вызывается перед началом обработки scroll события
     onScrollAfter        callback - вызывается после всей обработки scroll события
     onLastViewed         callback - вызывается когда доскролили до последнего элемента в памяти
     onFirstViewed        callback - вызывается когда доскролили до первого элемента в памяти

     Методы:
         resetTop
         resetBot
         insertAtFirst(element)  возвращает  element. Добавляет елемент перед первым элементом в скролируемом фрейме
         insertAtLast(element)   возвращает element.  Добавляет елемент после последнего элемента в скролируемом фрейме
         append(element)         возвращает element.  Вставляет елемент в конец масива видимых элементов
         prepend(element)        возвращает element.  Вставляет елемент в начало масива видимых элементов

 -------------------------------------------------------------------------------------------------*/

define(["scroller/domUtils","scroller/slotsArray","logger"],
    function (DomUtils,sa,logger) {

        "use strict";

        var DEBUG = true;
        var TRACE = false;

        var STATE_FREE = 1;
        var STATE_LOAD = 0;


        var defaultOptions = {
            'viewport': null,
            'memSlots': 5,
            'onScrollBefore' : null,
            'onScrollAfter': null,
            'onLastViewed' : null,
            'onFirstViewed' : null
        };



        function ScrollerAbstract(inputOptions) {

            //this.o = $.extend(true,{},defaultOptions,options || {});
            try {
                this.o = {};
                for (var key in defaultOptions) {
                    this.o[key] = inputOptions[key] ? inputOptions[key] : defaultOptions[key];
                }

                if (!this.o.viewport) {
                    throw Error("[ScrollerAbstract.init] Error: viewport required.");
                }

                this.viewSlots = new sa(this.o.memSlots);

                this.o.viewport = (this.o.viewport instanceof jQuery) ? this.o.viewport[0] : this.o.viewport;
                this.o.viewport.style.overflowY = "auto";
                this.o.viewport.style.overflowX = "hidden";
                this.id = "s-" + Math.floor((Math.random() * 10000));

                this.lastScrollTop = 0;
                this.container = null;

                //
                //  Создаем необходимый контейнер для размещения страниц
                //
                this.container = document.createElement('div');
                this.container.id = this.id;
                this.container.className = "content";

                this.headspacer = document.createElement('div');
                this.headspacer.className = "headerspacer";
                this.headspacer.style.height = 0;
                this.container.appendChild(this.headspacer);

                this.tailspacer = document.createElement('div');
                this.tailspacer.className = "tailspacer";
                this.tailspacer.style.height = 0;
                this.container.appendChild(this.tailspacer);

                this.o.viewport.appendChild(this.container);

                var caller = this;
                this.o.viewport.onscroll = function () {
                    caller._onScroll();
                };
                this.loadState = STATE_FREE;
                //this.o.viewport.addEventListener("scroll", this._onScroll());

            } catch (e) {
                logger.debug("[ScrollerAbstract.init] Error: ",e);
            }
        }





        //------------------------------------------------------------------------------------------------
        //
        //  getFirstViewed
        //  Возвращает первый не пустой элкмент масива элементов в памяти
        //  при проходе сверху от 0 до .... конца
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.getFirstViewed = function() {
            for (var i = 0; i < this.viewSlots.length(); i++) {
                if (this.viewSlots.getByPos(i)) return this.viewSlots.getByPos(i);
            }
            return null;
        };

        //------------------------------------------------------------------------------------------------
        //
        //  getLastViewed
        //  Возвращает первый не пустой элкмент масива элементов в памяти
        //  при проходе снизу от length до 0
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.getLastViewed = function() {

            var i = parseInt(this.viewSlots.length() - 1);
            logger.debug("[ScrollerAbstract.getLastViewed] viewSlots.length() = " + i);

            while (i >= 0 ) {
                if (this.viewSlots.getByPos(i)) {
                    return this.viewSlots.getByPos(i);
                }
                i-- ;
            }
            return null;
        };


        //------------------------------------------------------------------------------------------------
        //
        //  getLastViewed
        //  Возвращает первый не пустой элкмент масива элементов в памяти
        //  при проходе снизу от length до 0
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.resetLoadingState = function() {
            this.loadState = STATE_FREE;
        };



        ScrollerAbstract.prototype.resetTop = function() {
            this.headspacer.style.height = 0;
        };

        ScrollerAbstract.prototype.resetBot = function() {
            this.tailspacer.style.height = 0;
        };

        ScrollerAbstract.prototype.getData = function(id) {
            if ((id < 0) || (id >= this.viewSlots.length())) throw new Error("[ScrollerAbstract.getData] ID out of range.");
            if (this.viewSlots.getByPos(id))  return this.viewSlots.getByPos(id).data;
            return null;
        };

        ScrollerAbstract.prototype.getElement = function(id) {
            if (! id) throw new Error("[ScrollerAbstract.getData] ID parameter required.");
            return this.viewSlots.getByPos(id).el;
        };

        ScrollerAbstract.prototype.getAllViewed = function() {
            return this.viewSlots.getArray();
        };

        ScrollerAbstract.prototype.getViewed = function(id) {
            if (! id) throw new Error("[ScrollerAbstract.getData] ID parameter required.");
            return this.viewSlots[id];
        };

        ScrollerAbstract.prototype.getScrollTop = function() {
            return this.o.viewport.scrollTop;
        };

        // ScrollerAbstract.prototype.getFirst = function() {
        //     for (var i = 0; i < this.viewSlots.length; i++) {
        //         if (this.viewSlots[i]) return  this.viewSlots[i];
        //     }
        //     return null;
        // };
        //
        // ScrollerAbstract.prototype.getLast = function() {
        //     for (var i = this.viewSlots.length - 1 ; i >= 0; i--) {
        //         if ( this.viewSlots[i] ) return  this.viewSlots[i];
        //     }
        //     return null;
        // };

        ScrollerAbstract.prototype.destroy = function() {
            logger.debug("[ScrollerAbstract.destroy]");
            this.container.parentNode.removeChild(this.container);
            this.o.viewport.removeEventListener("scroll", this._onScroll());
        };

        //------------------------------------------------------------------------------------------------
        //
        //    Добавляет елемент перед первым элементом в скролируемом фрейме
        //    Возвращает  element.
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.insertAtFirst = function(newNode) {
            try {
                    this.headspacer.parentNode.insertBefore(newNode, this.headspacer.nextSibling);
                    return newNode;
            } catch (e) {
                logger.debug("[ScrollerAbstract.append] Error: ",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //   Добавляет елемент после последнего элемента в скролируемом фрейме
        //   Возвращает element.
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.insertAtLast = function(newNode) {
            try {
                    this.tailspacer.parentNode.insertBefore(newNode, this.tailspacer);
                    return newNode;
            } catch (e) {
                logger.debug("[ScrollerAbstract.append] Error: ",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //   Вставляет елемент в конец масива видимых элементов.
        //
        //   Параметры:
        //      elementObj
        //          {
        //             element,   -   DOM элемент который надо вставлять
        //             data,      -   Payload ( Либой JS объект)
        //          }
        //      insert  - boolean, если TRUE  то добавляет в html    elementObj.element
        //
        //   Возвращает removed  object
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.append = function(elementObj,insert) {
            try {

                //  Добавляем елемент (снизу) в список текущих
                insert && this.tailspacer.parentNode.insertBefore(elementObj.element, this.tailspacer);
                var removedObj = this.viewSlots.append(elementObj);
                TRACE && logger.debug("[ScrollerAbstract.append] Append Element ",elementObj,  this.viewSlots);
                DEBUG && logger.debug("[ScrollerAbstract.append] Append Element '"+elementObj.data.getId()+"' height="+DomUtils.getHeight(elementObj.element));


                //  Если список текущих  достиг максимального размера то удаляем верхний - removedObj
                //  И изменяем размеры  вставок в начале и в конце
                if (removedObj) {
                    var newHeadSpacer = (parseInt(this.headspacer.style.height) + DomUtils.getHeight(removedObj.element))
                    var newTailspacer = Math.max(
                        parseInt(this.tailspacer.style.height) - DomUtils.getHeight(elementObj.element), 0);

                    DEBUG && logger.debug("[ScrollerAbstract.append] Remove old Element '"+elementObj.data.getId()+"'. Element нeight="+DomUtils.getHeight(elementObj.element)+
                        ", HEAD space before="+this.headspacer.style.height+","+ ", after="+newHeadSpacer+
                        ", TAIL space before="+this.tailspacer.style.height+", after="+newTailspacer);

                    this.headspacer.style.height = newHeadSpacer + "px";
                    this.tailspacer.style.height = newTailspacer + "px"
                    removedObj.element.parentNode.removeChild(removedObj.element);
                }
                //DEBUG && logger.debug("[ScrollerAbstract.append] Set loadState="+STATE_FREE);
                this.loadState = STATE_FREE;
                return removedObj;

            } catch (e) {
                logger.debug("[ScrollerAbstract.append] Error: ",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //    Вставляет елемент в начало масива видимых элементов
        //    Возвращает element.
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.prepend = function(elementObj, insert) {
            try {

                insert && this.headspacer.parentNode.insertBefore(elementObj.element, this.headspacer.nextSibling);
                var removedObj = this.viewSlots.prepend(elementObj);
                TRACE && logger.debug("[ScrollerAbstract.prepend] Prepend Element ", elementObj, this.viewSlots);
                DEBUG && logger.debug("[ScrollerAbstract.prepend] Prepend Element '"+elementObj.data.getId()+"' height="+DomUtils.getHeight(elementObj.element));


                if (removedObj) {

                    var newTailspacer = (parseInt(this.tailspacer.style.height) + DomUtils.getHeight(removedObj.element));
                    var newHeadSpacer = Math.max(
                            parseInt(this.headspacer.style.height) - DomUtils.getHeight(elementObj.element), 0) + "px";

                    DEBUG && logger.debug("[ScrollerAbstract.prepend] Remove old Element '"+elementObj.data.getId()+"'. Element нeight="+DomUtils.getHeight(elementObj.element)+
                        ", HEAD space before="+this.headspacer.style.height+","+ ", after="+newHeadSpacer+
                        ", TAIL space before="+this.tailspacer.style.height+", after="+newTailspacer);

                    this.tailspacer.style.height = newTailspacer + "px";
                    this.headspacer.style.height = newHeadSpacer + "px";
                    removedObj.element.parentNode.removeChild(removedObj.element);
                }

                this.loadState = STATE_FREE;
                return removedObj;

            } catch (e) {
                logger.debug("[ScrollerAbstract.prepend] Error: ",e);
            }
        };

        //------------------------------------------------------------------------------------------------
        //
        //    Обрабатывает Scroll событие
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype._onScroll = function() {
            try {
                //var scrollTop = document.body.scrollTop;
                var scrollTop = this.o.viewport.scrollTop;
                var startOffset = this.container.firstElementChild.offsetTop;
                //var viewportHeight = parseInt(document.defaultView.getComputedStyle(this.viewport, '').getPropertyValue('height'));


                if (typeof this.o.onScrollBefore  == "function" ) { this.o.onScrollBefore(scrollTop);}
                this.onScrollBefore(scrollTop);


                //
                //  Скколимся вниз
                //
                if (scrollTop > this.lastScrollTop) {
                    this.lastScrollTop = scrollTop;

                    TRACE && logger.debug("[ScrollerAbstract._onScroll] scrolldown scrollTop="+ scrollTop
                        +", last page offset="+ (this.viewSlots.getLast().element.offsetTop - startOffset)
                        +", scroll pos of bot view area=" + (scrollTop + DomUtils.getInnerHeight(this.o.viewport))
                        );

                    //   Мы на последней странице, пора грузить новую
                    if ((this.loadState == STATE_FREE)
                        && ((this.viewSlots.getLast().element.offsetTop - startOffset) < (scrollTop + DomUtils.getInnerHeight(this.o.viewport))))
                    {
                        DEBUG && TRACE && logger.debug("[ScrollerAbstract._onScroll] Got last block ",
                            this.viewSlots.getLast().element);

                        this.loadState = STATE_LOAD;
                        if ( typeof this.o.onLastViewed == "function") {
                            this.o.onLastViewed(this.viewSlots.getLast());
                        }
                        this._onLast(this.viewSlots.getLast());
                    }
                }

                //
                //   Скролимся вверх
                //
                else  if (this.viewSlots.getFirst()) {
                    var elHeight = DomUtils.getHeight(this.viewSlots.getFirst().element);

                    DEBUG && TRACE && logger.debug("[ScrollerAbstract._onScroll] scrolldown scrollTop="+ scrollTop
                        +", bottom border of first blocl="+ (this.viewSlots.getFirst().element.offsetTop - startOffset + elHeight)
                        +", scroll pos of bot view area=" + (scrollTop + DomUtils.getInnerHeight(this.o.viewport))
                    );

                    //   Мы на первой странице, пора грузить новую
                    if ((this.loadState == STATE_FREE)
                        && ((this.viewSlots.getFirst().element.offsetTop - startOffset + elHeight) > scrollTop))
                    {
                        DEBUG && logger.debug("[ScrollerAbstract._onScroll] Got first block ",
                            this.viewSlots.getFirst().element );

                        this.loadState = STATE_LOAD;
                        if ( typeof this.o.onFirstViewed == "function") {
                            this.o.onFirstViewed(this.viewSlots.getFirst());
                        }
                        this._onFirst(this.viewSlots.getFirst());
                    }
                }
                this.lastScrollTop = scrollTop;

                if (typeof this.o.onScrollAfter  == "function" ) { this.o.onScrollAfter(scrollTop);}
                this.onScrollAfter(scrollTop);

            } catch (e) {
                logger.debug("[ScrollerAbstract._onScroll] Error: ",e);
            }
        };


        //------------------------------------------------------------------------------------------------
        //
        //   Нфбор функций  для переопределения
        //
        //------------------------------------------------------------------------------------------------
        ScrollerAbstract.prototype.onScrollBefore = function() {

        };
        ScrollerAbstract.prototype.onScrollAfter = function() {

        };
        ScrollerAbstract.prototype._onFirst = function() {

        };
        ScrollerAbstract.prototype._onLast = function() {

        };


       return ScrollerAbstract;
    });