/**
 * Created by abel on 29.12.15.
 */


define(["jquery","scroller/domUtils","scroller/dataRow","logger","utils","api"],
    function($,DomUtils,Row,logger,utils,Api) {
        "use strict";

        var MEDIA_THUMB = 11;
        var MIN_PAGE_LIMIT = 20;

        var IMG_HEAD_HEIGHT = 30;
        var IMG_HEIGHT_OVERHEAD = 10;
        var IMG_WIDTH_OVERHEAD = 10;
        var BLOCK_SPACE = 40;


        var defaultOptions = {
            'limit': 0,
            'offset': 0,
            'id': new Date().getTime(),
            'viewport': null,
            'imageBodyHtml':""
        };

        var DEBUG = true;
        var TRACE = false;


        //==========================================================================================
        //
        //   PAGE:  Accumulate and render block of rows
        //
        //   Параметры:
        //
        //      objectsList - Список JSON объектов - фоток, полученных от сервера
        //      pageOptions   Опции для подготовки страницы
        //             {
        //                 limit  -  макимальное к-во элементов возвращаемое при запросе от сервера
        //                 offset -  номер  позиции первого обрабатываемого и возвращаемого элемента от сервера
        //                 id     -  id этой страницы
        //                 viewport -
        //                 'convToObject': convertToInternalFormat  --  функция предобработки считанных данных для фоток
        //                 'imageBodyHtml':  -  функция которая генерирует  HTML код для
        //                                      отображения одного фрейма фотографии
        //             }
        //
        //     // Удалить
        //     // $viewPortObj  DOM объект для основного фрейма  в который бедет втавлен HTML код для страницы
        //
        //==========================================================================================
        function Page(objectsList, initOptions) {
            this.pageOptions = $.extend(true, {}, defaultOptions, initOptions || {});
            if (!initOptions.viewport) throw Error("[Page.init] Error: viewport element must be present.");

            this.objectId = Math.floor((Math.random() * 10000));
            this.viewport = (initOptions.viewport instanceof jQuery) ? initOptions.viewport[0] : initOptions.viewport;
            this.areaWidth = DomUtils.getInnerWidth(this.viewport);
            this.pageHeight = 0;
            this.pageFrame = null;
            this.rows = [];
            this.objectsList = [];
            this.render(objectsList);
        }

        //------------------------------------------------------------------------------------------
        //   Utils
        //------------------------------------------------------------------------------------------

        Page.prototype.getOptions = function () {
            return this.pageOptions;
        };

        Page.prototype.setOptions = function (options) {
            this.pageOptions = $.extend(true, this.pageOptions, options|| {});
        };


        //   Возвращает DOM объект в который вписаны все фотки этой страницы
        Page.prototype.getElement = function () {
            return this.pageFrame;
        };

        Page.prototype.getId = function() {
            return this.pageOptions.id;
        };

        Page.prototype.remove = function () {
            this.clean();
            if (this.pageFrame) {
                this.pageFrame.parentNode.removeChild(this.pageFrame);
                this.pageFrame = null;
            }
        };
        Page.prototype.clean = function () {
            if (this.pageFrame) {
                this.pageFrame.innerHTML = "";
            }
            this.rows = [];
        };

        //
        //   Возвращает список текущих выведенных объектов
        //
        Page.prototype.getList = function () {
            return this.objectsList;
        };

        Page.prototype.getHeight = function () {
            return this.pageHeight;
        };

        // Page.prototype.removeItem = function (id) {
        //     this.objectsList.splice(id, 1);  // Удаляем один элемент из масива с измеением размера масива
        // };


        Page.prototype.getItemById= function(id) {
            for (var itemIndex = 0; itemIndex < this.objectsList.length; itemIndex++) {
                if (this.objectsList[itemIndex].id == id ) {
                    return this.objectsList[itemIndex];
                }
            }
            return null;
        };


        //------------------------------------------------------------------------------------------
        //
        //  render
        //
        //     Наполняет страницу данными полученными от сервера
        //     Обрабатываем полученные элементы,
        //     разбиваем по строкам,
        //     определяем начало сессии,
        //     определяем длину страници,
        //     добавляем выводимые данные во внутренний масив (this.objectsList)  и отрисовываем страницу
        //
        //
        //  Параметры
        //     objectsList - Список JSON объектов - фоток, полученных от сервера
        //     constLength - true - выводим все данные, ничего не отбрасывая в конце для вырявнивания ряда
        //
        //  Возвращает DOM объект для фрейма страницы
        //
        //
        //------------------------------------------------------------------------------------------
        Page.prototype.render = function (objectsList,constLength) {

            try {
                this.pageHeight = 0;
                var rowId = 0;
                var stamp = 0;
                var lastRowStartIndex = 0;
                var isPageClosed = false;
                this.objectsList = [];
                this.areaWidth = DomUtils.getInnerWidth(this.viewport);

                var rowParams = {
                    'id':rowId++,
                    'areaWidth':this.areaWidth,
                    'bodyHTML': this.pageOptions.imageBodyHtml
                };
                //var curRow = new Row({'id':rowId++, 'areaWidth':this.areaWidth});
                var curRow = new Row(rowParams);

                //  Render Frame
                var pageFrame = document.createElement('div');
                pageFrame.setAttribute('id', "page-"+this.objectId);
                pageFrame.setAttribute('data-id', this.pageOptions.id);
                pageFrame.setAttribute('data-offset', this.pageOptions.offset);
                pageFrame.classList.add("content-page");
                pageFrame.style.display = "table";
                this.pageFrame = pageFrame;


                var itemIndex;
                for (itemIndex = 0; itemIndex < objectsList.length; itemIndex++) {
                    //
                    //   Вытвскиваем из полученного объекта с сервера все необходимые данные и сохраняем у себя
                    //
                    var item = this.toObject(objectsList[itemIndex]);
                    //item.count = itemIndex + this.pageOptions.offset;
                    item.count = itemIndex;
                    //
                    //   Проверяем смену даты. Объект с новой датой считаем началом сессии

                    if (stamp != utils.getDateStamp(item.createTime)) {
                        item.startNewSession = true;
                        stamp = utils.getDateStamp(item.createTime);
                    }
                    //
                    //   Добавляем объект в текущую строку
                    var isAppended = curRow.append(item);
                    //
                    //   Проверяем влезло или нет.
                    //   Объект не добавили.  Переносим в новый ряд.
                    //   Закрываем существующий ряд
                    //
                    if (!isAppended) {
                        lastRowStartIndex = itemIndex;

                        //   Выводим на экран заполненную строку
                        this._addRow(curRow);
                        this.objectsList = this.objectsList.concat(curRow.getRowObjects());

                        //
                        //  Если это начало сессии и начало строки и выведено уже больше MIN_PAGE_LIMIT объектов
                        //  то закрываем страницу - Прерываем вывод и меняем limit
                        //
                        if ((!constLength) && item.startNewSession && (itemIndex > MIN_PAGE_LIMIT)) {
                            this.pageOptions.limit = itemIndex + 1;
                            isPageClosed = true;
                            DEBUG && logger.debug("[Page.appendAll] Close normally.");
                            break;
                        }
                        //
                        //  Ряд закрыт  но не вывели больше  MIN_PAGE_LIMIT объектов
                        //  Открываем новый ряд
                        //
                        else {
                            curRow = new Row({'id':rowId++, 'areaWidth':this.areaWidth});
                            curRow.append(item);
                        }
                    }
                }

                //
                //    Если дошли до конца  и не остановились между сессиями.
                //
                if (!isPageClosed) {
                    //
                    //  Если на входе получили меньше чем полную страницу данных
                    //  Подозреваем что это последняя страница и выводим ее со всеми остатками.(последния ряд)
                    //  или если constLength == true  то значит выводим данные без обрезания
                    //
                    if (constLength || (itemIndex == (objectsList.length - 1))
                        && (this.pageOptions.limit <= MIN_PAGE_LIMIT)) {
                        this._addRow(curRow);
                        this.objectsList = this.objectsList.concat(curRow.getRowObjects());
                    }
                    //
                    //  Инфче если доехали до конца полной страницы, поледний неполный ряд не выводм,
                    //  расчитываем  что он появится в новой странице
                    //
                    else {
                        this.pageOptions.limit = lastRowStartIndex + 1;
                        DEBUG && logger.debug("[Page.appendAll] Close at the end.");
                    }
                }

                //  Сохраняем в теге колчество объектов, которе мы реально отобразили в HTML
                //  И выставляем просумированную по рядам высоту страницы
                pageFrame.setAttribute('data-limit', this.pageOptions.limit);
                pageFrame.style.height = this.pageHeight + "px";
                this.pageFrame = pageFrame;

                return pageFrame;
            } catch (e) {
                logger.debug("[Page2.apendAll] Error:", e);
                throw e;
            }
        };

        //------------------------------------------------------------------------------------------
        //
        //  addRow  Добляет новый ряд в конец страницы
        //
        //------------------------------------------------------------------------------------------
        Page.prototype._addRow = function (rowObj) {
            try {
                this.pageFrame.appendChild(rowObj.render());
                this.pageHeight += rowObj.getHeight();
                //this.objectsList = this.objectsList.concat(rowObj.getRowObjects());

            } catch (e) {
                logger.debug("[Page2.apendAll] Error:", e);
                throw e;
            }
        };

        //------------------------------------------------------------------------------------------
        //
        //   redraw
        //
        //   Перерисовываем страницу. В случае когда ширина области вывода изменилась, или
        //   изменилось содержимое  станицы
        //
        //   options - parameter as object
        //        {
        //            append: boolean
        //            parent: DomElement  -
        //            areaWidth
        //        }
        //------------------------------------------------------------------------------------------
        Page.prototype.redraw = function () {

            try {
                //
                //    Удаляем все ряды из страницы
                //
                this.clean();

                this.areaWidth = DomUtils.getInnerWidth(this.viewport);
                var rowId = 0;
                this.pageHeight = 0;
                var curRow = new Row({'id':rowId++, 'areaWidth':this.areaWidth });

                //
                //    Проходимся по всем подготовленным  элементам
                //
                for (var itemIndex = 0; itemIndex < this.objectsList.length; itemIndex++) {
                    var item = this.objectsList[itemIndex];
                    //item.count = itemIndex + this.pageOptions.offset;
                    item.count = itemIndex;
                    //   Добавляем объект в текущую строку
                    var isAppended = curRow.append(item);

                    //   Проверяем добавили или нет.
                    if (!isAppended) {
                        this._addRow(curRow);
                        curRow = new Row({'id':rowId++, 'areaWidth':this.areaWidth});
                        curRow.append(item);
                    }
                }
                this._addRow(curRow);

                //  Сохраняем в теге колчество объектов, которе мы реально отобразили в HTML
                //  И выставляем просумированную по рядам высоту страницы
                this.pageOptions.limit = this.objectsList.length + 1;
                this.pageFrame.setAttribute('data-limit', this.pageOptions.limit);
                this.pageFrame.setAttribute('data-offset', this.pageOptions.offset);
                this.pageFrame.style.height = this.pageHeight + "px";

                return this.pageFrame;
            }
            catch (e) {
                logger.debug("[Page.redraw] Error:", e);
                throw e;
            }
        };

        //------------------------------------------------------------------------------------------
        //
        //   getElementOnScreen
        //
        //   Находим  img-frame  внутрь рамки которого попадает ууказанна позиция.
        //   Используется позиция по вертикали
        //   Параметр
        //      viewPosition  - json объект
        //          {
        //              x - offsetTop  - минус офсет родительского объекта
        //              y - offsetLeft - минус офсет родительского объекта
        //          }
        //
        //   Возвращает:
        //   Если искомый элемент найден среди фреймов этой страниццы
        //    return {
        //         'element':      DOM  елемент  для фрейма фотографии
        //         'pageElement':  DOM елемент для фрейма страницы
        //         'offset':       Смешение внутри фрейма фотографии до центра фидимой област по вертикали.
        //     }
        //
        //   Если искомый фрейм не найден
        //     return {
        //         'element':null,
        //         'pageElement':null,
        //         'offset': -1
        //     };
        //------------------------------------------------------------------------------------------
        Page.prototype.getElementOnScreen = function(viewPosition) {

            try {
                if (this.pageFrame) {
                    var offsetBegin = this.pageFrame.parentNode.getBoundingClientRect();
                    var rowBeginPosition = {
                        'x':1,
                        'y': viewPosition.y
                    };
                    var resultElment = null;

                    //
                    //  Если заданная позиция на попадает внутрь страницы то выходим
                    //
                    if (this._checkRegion(this.pageFrame, viewPosition, offsetBegin) >= 0) {

                        //
                        //   Проходимся по всем элемента на странице
                        //
                        for (var itemIndex = 0; itemIndex < this.objectsList.length; itemIndex++) {

                            //
                            //   Проверяем, это первый элемент в ряду ?
                            //
                            var imgFrameOffset = Math.round(
                                this._checkRegion(this.objectsList[itemIndex].element, rowBeginPosition, offsetBegin)
                            );
                            if (imgFrameOffset >= 0) {
                                //   Сохраняем на случай если в середине элеента не найдём
                                resultElment = {
                                    'offset': imgFrameOffset,
                                    'element': this.objectsList[itemIndex].element,
                                    'pageElement': this.pageFrame
                                };
                            }

                            //
                            //   Проверяем центральный элемент в центральном ряду ?
                            //
                            imgFrameOffset = Math.round(
                                this._checkRegion(this.objectsList[itemIndex].element, viewPosition, offsetBegin)
                            );
                            if (imgFrameOffset >= 0) {
                                resultElment =  {
                                    'offset':imgFrameOffset,
                                    'element':this.objectsList[itemIndex].element,
                                    'pageElement':this.pageFrame
                                };
                                break;  //  Элкмент найден выходим.
                            }
                        }

                        //
                        //  Элемент был найден. Выходим.
                        //
                        if (resultElment) {
                            TRACE && logger.debug("[Page.getElementOnScreen] Found element=" ,resultElment.element);
                            return resultElment;
                        }

                        //
                        //  Eсли мы еще тут значит не подошел ни один внутренний фрейм, хотя страница подошла
                        //
                        logger.debug("[Page.getElementOnScreen] Internal ERROR. "
                            +"Cannot find frame on the screen position ", viewPosition, this.pageFrame);
                    }
                }
                return {'offset':-1, 'element':null, 'pageElement':null};
            }

            catch (e) {
                logger.debug("[Page.getElementOnScreen] Error:",e);
            }
        };

        //---------------------------------------------------------------------------------------------------
        //
        //   Проверяет попадает ли указанная точка pos{x,y} в пространство занимаемое указанным елементом
        //   Если попадает то возвращает смешение от верхней границы элемента до точки по оси Y
        //   Иначе возвращает -1
        //
        //---------------------------------------------------------------------------------------------------
        Page.prototype._checkRegion = function(element,pos, parentsOffset) {

            // getBoundingClientRect() returns {bottom,height,left,right,top,width}
            var elementRect = element.getBoundingClientRect();
            var x = pos.x + parentsOffset.left;
            var y = pos.y + parentsOffset.top;
            // var x = pos.x + parentsOffset.left;
            // var y = pos.y + parentsOffset.top;

            if (((elementRect.top - DomUtils.getMargin(element, "top")) <= y)
                && ((elementRect.bottom + DomUtils.getMargin(element, "bot")) >= y)
                && ((elementRect.left - DomUtils.getMargin(element, "left")) <= x)
                && (elementRect.right + DomUtils.getMargin(element, "right")>= x ))
            {

                TRACE && logger.debug("[Page._checkRegion] Image frame found. Requested: Pos x="+pos.x+", pos y="+pos.y
                        +", Calc x="+x+", y="+y
                    +", elementRect.top=" + (elementRect.top  - DomUtils.getMargin(element, "top"))
                    +", elementRect.left=" + (elementRect.left  - DomUtils.getMargin(element, "left"))
                );

                return  y - elementRect.top;
            }

            return -1;

        };

        //------------------------------------------------------------------------------------------
        //
        //  removeItem
        //  Удаляет объект из масива, уменьшает значение limit (общее к-во объектов вмасиве)
        //  и перерисовывает страницу
        //
        //     Парметры
        //        itemCount  - позиция объекта в списе
        //
        //     Возвращает удяленный объект
        //
        //
        //------------------------------------------------------------------------------------------

        Page.prototype.removeItem = function(itemId) {
            var removedObject = null;

            var found = false;
            for ( var i = 0; i<this.objectsList.length; i++) {
                if ( this.objectsList[i].id == itemId) {
                    removedObject = this.objectsList[i];
                    this.objectsList.splice(i,1);
                    DEBUG && logger.debug("[Page.removeItem] Page="+ this.pageOptions.id +",Remove item " + itemId, removedObject);
                    found = true;
                    break;
                }
            }
            if ( ! found ) {
                logger.debug("[Page.removeItem] Cannot find Item="+itemId+" on page", this);
                throw new Error("[Page.removeItem] Cannot find Item="+itemId+" on page=" + this.pageOptions.id );
            }
            this.pageOptions.limit -= 1;
            return removedObject;
        };


        //------------------------------------------------------------------------------------------
        //------------------------------------------------------------------------------------------
        Page.prototype.shiftPage = function(delta) {
            //this.pageOptions.offset -= delta;
            this.pageFrame.setAttribute('data-offset', (this.pageOptions.offset -= delta));
        };

        //------------------------------------------------------------------------------------------
        //
        //  toObject
        //
        //  Копирует  и добавляет данные из объекта полученного от сервера.
        //  Добавляет данными параметрами thumbnail  из списка медиа для объекта
        //
        //     Парметры
        //        photoObject  - JSON  объект описывающий фотографию, полученный от сервера
        //     Возвращает
        //        JSON {
        //             id        - ID of photo object
        //             count     - позиция элемента от начала страницы
        //             min
        //             max
        //             name      - имя медиа объекта
        //             view      - показывать или нет
        //             type      - фолдер, объект или серия объектов
        //             mimeType  - mediaType, the mimetype of main media object
        //             url       - url for load thumb image
        //             height    - thumb height
        //             width     - thumb width
        //             aspect    - aspect
        //         }
        //
        //------------------------------------------------------------------------------------------

        Page.prototype.toObject = function(photoObject) {

            var object = {
                'min': 0,
                'max': 0,
                'view': true
            };

            try {

                object.id = photoObject.id;
                object.name = photoObject.name;
                object.type = photoObject.type;
                object.siteId = photoObject.siteBean.id;
                object.siteType = photoObject.siteBean.connectorType;
                object.siteName = photoObject.siteBean.name;
                object.realUrl = photoObject.realUrl;
                object.mimeType = photoObject.mediaType;
                object.startNewSession = false;
                object.createTime = photoObject.createTime;


                var mediaFound = false;

                for (var i = 0; i < photoObject.mediaObjects.length; i++) {
                    if (photoObject.mediaObjects[i].type == MEDIA_THUMB) {
                        var mt =  photoObject.mediaObjects[i].mimeType;
                        var thumbFlExt =  mt.substring(mt.indexOf("/") + 1);

                        //object.thumbMimeType = photoObject.mediaObjects[i].mimeType;
                        var imgFolder = photoObject.id.substring(parseInt(photoObject.id.length) - 2);
                        object.url = Api.getActionUrl("thumbUrl") + '/' + imgFolder + "/" + photoObject.id + "." + thumbFlExt;
                        object.height = parseInt(photoObject.mediaObjects[i].height);
                        object.width = parseInt(photoObject.mediaObjects[i].width);
                        object.aspect = parseFloat(parseFloat(object['width']) / parseFloat(object['height']));
                        mediaFound = true;
                        break;
                    }
                }

                if (!mediaFound) {
                    DEBUG && logger.debug("[convertToInternalFormat] Error: Cannot find media object for thumb.", photoObject);
                    object.view = false;
                }

            } catch (e) {
                object = null;
                logger.trace("[convertToInternalFormat] Error:",e);
                throw e;
            }
            return object;
        };


        return Page;

});
