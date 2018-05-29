/**
 * Created by abel on 07.12.16.
 */
/*
   options
       {
         'id':         id для этого ряда
         'areaWidth':  ширина области в которой будет выводиться ряд
         'bodyHTML':   Функция вызов которой возвращает строку строку которую необходимо вставить в подготовленный фрейм
       }

   При вызове функции bodyHTML в нее передается параметор являющийся JSON объектом
   Параметор:
     {
         'frameWidth'  - ширина внутренеей области фрейма
         'frameHeight' - высота внутренней области фрейма
         'leftIndent'  - Ширина отступа от левой границы  ( в случае если  это начало новой фото сесии и нажно
                         визуально разбить ряд
         'header'      - Нужно ли добавлять заголовок
         'left'        - девый отступ от наяаля ряда
         'top'         - отступ от верха ряда
         'rowPos'      - Позиция в ряду
         'item'        - JSON объект который передали на обработку добавленными полями
     }

   item
     {
         min
         max
         aspect
         drawWidth
         drawHeight
         startNewSession
         element          - Созданый  DOM элемент
     }

 */

define(["scroller/domUtils","logger","utils"],function(DomUtils, logger, utils) {
    "use strict";


    var DEBUG = true;
    var TRACE = false;
    var DEBUG_ITEM = '';

    var IMG_HEIGHT_MIN = 200;
    var IMG_HEIGHT_MAX = 250;
    var IMG_HEAD_HEIGHT = 30;
    var IMG_HEIGHT_OVERHEAD = 10;
    var IMG_WIDTH_OVERHEAD = 10;
    var BLOCK_SPACE = 40;

    var defaultOptions = {
        'id': null,
        'areaWidth': null,
        'bodyHTML': ""
    };

    //==========================================================================================
    //
    //  FLEXWIDTH
    //
    //==========================================================================================
    function FlexWidth(areaWidth)  {
        this.areaWidth = areaWidth;
        this.min = 0;
        this.max = 0;
    }

    FlexWidth.prototype.increase = function(minMaxObj) {
        this.min += minMaxObj.min;
        this.max += minMaxObj.max;
    };

    FlexWidth.prototype.canInsert = function(minMaxObj) {
        //    Если по максисуму вылезает а по минимому нет
        if (((this.min + minMaxObj.min) <= this.areaWidth) &&
            ((this.max + minMaxObj.max) >= this.areaWidth)) {
            return 1;
        }
        //    Иначе если по максимому все равно влезает и еще остается место
        else if ((this.max + minMaxObj.max) < this.areaWidth) {
            return 2;
        }
        return 0;
    };


    //==========================================================================================
    //
    //   ROW:  Accumulate and render images row
    //
    //  options:
    //    {
    //       'id':
    //       'areaWidth':
    //       'bodyHTML':
    //    }
    //
    //==========================================================================================
    function Row(options) {

        this.options = {};
        for (var key in defaultOptions) {
            this.options[key] = options[key] ? options[key] : defaultOptions[key];
        }

        if (!this.options.areaWidth) {
            throw Error("[Row.init] Error: areaWidth required.");
        }

        this.id = Math.floor((Math.random() * 10000));
        this.rowEl = null;
        this.areaWidth = this.options.areaWidth;
        this.rowObjects = [];
        this.widthRuler = new FlexWidth(this.options.areaWidth);
        this.rowFull = false;
        this.rowClosed = false;
        this.rowHeight = 0;
        this.aspectSum = 0;
        this.realHeight = 0;
        this.widthNoOverhead = this.options.areaWidth;
        this.withHeader = false;
    }

    //------------------------------------------------------------------------------------------
    //   Возвращает true если ряд заполнен полностью
    //------------------------------------------------------------------------------------------
    Row.prototype.isFull = function () {
        return this.rowFull;
    };

    //------------------------------------------------------------------------------------------
    //   Возвращает DOM элемент для всего рядя
    //------------------------------------------------------------------------------------------
    Row.prototype.getEl = function () {
        return this.rowEl;
    };

    //------------------------------------------------------------------------------------------
    //   Возвращает вычиленную высоту
    //------------------------------------------------------------------------------------------
    Row.prototype.getHeight = function () {
        //return this.realHeight;
        return this.rowHeight;
    };

    //------------------------------------------------------------------------------------------
    //   Возвращает масив всех объектов добавленных в этот ряд.
    //   Аозвращает масив объектов.
    //   [
    //       {
    //             id
    //             count     - зщзиция элемента от начала страницы
    //             min       -  минимальная ширина объекта, пересчитано в процессе добавления
    //             max       -  максивальная ширина объекта, пересчитано в процессе добавления
    //             view      -  показывать или нет
    //             mimeType  - image type
    //             url       - url for load thumb image
    //             height    - thumb height
    //             width     - thumb width
    //             aspect    -  aspect
    //             drawWidth  -   Ширина которая будет установлена при отрисовке объекта (назначается при заполнении всего ряда)
    //             drawHeight -   Высота которая будет установлена при отрисовке объекта. Выбирается единой для всего ряда.
    // }
    //       ...
    //    ]


    //------------------------------------------------------------------------------------------
    Row.prototype.getRowObjects = function () {
        return this.rowObjects;
    };

    //------------------------------------------------------------------------------------------
    //    Добавляем обект к текущему ряду.
    //    Возвращаем true если удалось добавить (влезло)
    //    иначе возвращаем false и не добавляем.
    //
    //    object  {
    //             id
    //             count     - зщзиция элемента от начала страницы
    //             min
    //             max
    //             view      -  показывать или нет
    //             type      - video or image
    //             mimeType  - mimetype of main media object
    //             url       - url for load thumb image
    //             height    - thumb height
    //             width     - thumb width
    //             aspect    -  aspect
    //         }
    //
    //------------------------------------------------------------------------------------------
    Row.prototype.append = function (object) {
        if (this.rowFull) return false;
        var appended = false;

        try {

            //
            //  Подготавливаем объект
            //
            object.max = Math.round((IMG_HEIGHT_MAX * object.aspect) + IMG_WIDTH_OVERHEAD);
            object.min = Math.round((IMG_HEIGHT_MIN * object.aspect) + IMG_WIDTH_OVERHEAD);

            //  Для объекта первого в сесии и не первого в ряду, предусматриваем левый отступ
            if ( (this.rowObjects.length != 0) && object.startNewSession) {
                object.max += BLOCK_SPACE;
                object.min += BLOCK_SPACE;
            }

            var canInsert = this.widthRuler.canInsert(object);

            //    Если по максисуму вылезает а по минимому нет
            //    Значит в этот ряд больше не добавляем. Закрываем его.
            if (canInsert==1) {
                this._push(object);
                this.widthRuler.increase(object);
                this.closeRow(true);
                appended = true;
            }

            //   Иначе если по максимому все равно меньше, значит в этот рад еще может влезть.
            //   Добавляем картинку, но не закрываем.
            else if (canInsert==2) {
                this._push(object);
                this.widthRuler.increase(object);
                appended = true;
            }

            if (!appended) {
                this.closeRow(true);
                //logger.debug("[Row.append]  Add image"+object.id+" to row "+this.id+". - REJECTED");
            }

        } catch (e) {
            logger.debug("[Row.append] Error: ",e);
            throw e;
        }
        return appended;
    };

    //------------------------------------------------------------------------------------------
    //      Добавляем объект
    //------------------------------------------------------------------------------------------
    Row.prototype._push = function(object) {

        this.aspectSum += object.aspect;
        if (object.startNewSession)  {
            this.withHeader = true;
        }

        this.widthNoOverhead = this.widthNoOverhead - IMG_WIDTH_OVERHEAD;

        //  Для объекта первого в сесии и не первого в ряду, предусматриваем левый отступ
        if ( (this.rowObjects.length != 0) && object.startNewSession) {
            this.widthNoOverhead = this.widthNoOverhead - BLOCK_SPACE;
        }

        this.rowObjects.push(object);

    };

    //------------------------------------------------------------------------------------------
    //   После того как объекты в ряду определены  высчитываем ширину и высоту дял каждого объекта
    //   так чтобы заполнить весь ряд по ширине
    //------------------------------------------------------------------------------------------
    Row.prototype.closeRow = function(isFull) {
        try {
            if (this.rowFull || this.rowClosed)  return;
            this.rowFull = isFull;
            this.rowClosed = true;

            if (this.rowFull) {
                this.realHeight = Math.round(this.widthNoOverhead / this.aspectSum);

                for (var y = 0; y < this.rowObjects.length; y++) {
                    this.rowObjects[y].drawWidth = Math.round(this.realHeight * this.rowObjects[y].aspect);
                    this.rowObjects[y].drawHeight = Math.round(this.realHeight);
                }
                this.rowHeight = this.realHeight + IMG_HEIGHT_OVERHEAD;
            }

            else {
                var midHeight = Math.round((IMG_HEIGHT_MIN + IMG_HEIGHT_MAX) / 2);
                for (var i = 0; i < this.rowObjects.length; i++) {
                    this.rowObjects[i].drawWidth =  Math.round(midHeight  * this.rowObjects[i].aspect);
                    this.rowObjects[i].drawHeight = Math.round(midHeight);
                }
                this.rowHeight = midHeight + IMG_HEIGHT_OVERHEAD;
            }

            if (this.withHeader) {
                this.rowHeight += IMG_HEAD_HEIGHT;
            }

            TRACE && logger.debug("[Row.closeRow] Close row with length="+ this.rowObjects.length + " objects.");

        }
        catch (e) {
            logger.debug("[Row.closeRow] Error:",e);
            throw e;
        }
    };

    //------------------------------------------------------------------------------------------
    //   Render row
    //------------------------------------------------------------------------------------------
    Row.prototype.render = function() {
        var realRowHeight = this.rowHeight;
        var rowFrame = null;
        if (!this.rowFull) {
            //   DEBUG && logger.debug("[Row.render] Rendering not closed row");
            this.closeRow(false);
        }

        try {
            rowFrame = document.createElement('div');
            rowFrame.classList.add("img-frames-row");
            rowFrame.style.width = this.areaWidth + "px";

            var curLeftPos = 0;

            for (var i = 0; i < this.rowObjects.length; i++) {

                var item = this.rowObjects[i];
                if (DEBUG &&  DEBUG_ITEM && (item.id == DEBUG_ITEM )) {
                    logger.debug("[Row.render] Prepare item id="+item.id, item);
                }
                var itemData = {
                    'frameWidth':Math.round(
                        item.drawWidth + IMG_WIDTH_OVERHEAD
                    ),
                    'frameHeight': Math.round(item.drawHeight
                        + IMG_HEIGHT_OVERHEAD
                        + (this.withHeader?IMG_HEAD_HEIGHT:0)
                    ),
                    'leftIndent': Math.round(IMG_WIDTH_OVERHEAD / 2),
                    'header': this.withHeader,
                    'left':curLeftPos,
                    'top':0,
                    'rowPos':i,
                    'item':item
                };


                if (item.startNewSession && (i>0) && this.withHeader) {
                    itemData.leftIndent += BLOCK_SPACE;
                    itemData.frameWidth += BLOCK_SPACE;
                }

                this.rowObjects[i].element = this.createImgFrameEl(itemData);
                rowFrame.appendChild(this.rowObjects[i].element);

                var event = this.startEvent(this.rowObjects[i].element,itemData);
                //rowFrame.dispatchEvent(event);
                //document.body.dispatchEvent(event);


                realRowHeight = Math.max(realRowHeight,parseInt(itemData.frameHeight));
                curLeftPos += parseInt(itemData.frameWidth);
            }

            rowFrame.style.height = parseInt(realRowHeight) + "px";
            this.rowEl = rowFrame;
            this.rowHeight = realRowHeight;

            TRACE && logger.debug("[Row.render] Render row with "+ this.rowObjects.length + " objects.");
            return rowFrame;

        }
        catch (e) {
            logger.debug("[Row.render] Error:",e);
            throw e;
        }
    };


    //------------------------------------------------------------------------------------------
    //    Готовим DOM элемент для фрейма фотографии с заголовкам
    //------------------------------------------------------------------------------------------
    Row.prototype.clear = function() {
        this.rowEl.parentNode.removeChild(this.rowEl);
        this.rowEl  = null;
    };


    //------------------------------------------------------------------------------------------
    //    Готовим DOM элемент для фрейма фотографии с заголовкам
    //------------------------------------------------------------------------------------------
    Row.prototype.createImgFrameEl = function(o) {
        var imgElement = document.createElement('div');
        //imgElement.id = "i-" + this.id + "-" + o.item.count;
        imgElement.id = "img-frame-"+this.id;
        imgElement.setAttribute("data-id", o.item.id);
        imgElement.setAttribute("data-count", o.item.count);
        imgElement.setAttribute("data-mimetype", o.item.mimeType);
        imgElement.setAttribute("data-name", o.item.name);
        imgElement.setAttribute("data-type", o.item.type);  // folder or object
        imgElement.classList.add("img-frame");
        imgElement.style.left = o.left + "px";
        imgElement.style.top = o.top + "px";
        imgElement.style.paddingLeft = o.leftIndent == 0 ? '0' : (o.leftIndent + "px");
        imgElement.style.position = "absolute";
        imgElement.style.width = o.frameWidth + "px";
        imgElement.style.height = o.frameHeight + "px";

        var strHTML = "";
        if (typeof this.options.bodyHTML == "function") {
            strHTML = this.options.bodyHTML(o);
        }
        else if ((this.options.bodyHTML) && (typeof this.options.bodyHTML == "string")) {
            strHTML = this.options.bodyHTML;
        }
        else {
            strHTML = this.getBodyHtml(o);
        }
        imgElement.innerHTML = strHTML;
        return imgElement;
    };


    //------------------------------------------------------------------------------------------
    //
    //     Generate custom event
    //     insert photo_image_id
    //     dispatch bubbling event from image-frame
    //
    //     Событие генерируется для каждого объекта как только его вставили в
    //     отображаемый документ (объект может быть скрыт из=зм скрола)
    //
    //------------------------------------------------------------------------------------------
    Row.prototype.startEvent = function(el,object) {

        //DEBUG && logger.debug("[Row.startEvent] dispatch event. id="+object.item.id);

        var event = new CustomEvent("photorendered", {
            bubbles: true,
            cancelable: true,
            detail: { 'id': object.item.id, 'element':object.item.element}
        });

        document.body.dispatchEvent(event);
        //object.item.element.dispatchEvent(event);
        return event;
    };


    //------------------------------------------------------------------------------------------
    //
    //     Готовит HTML для содержимого контейнера фотографии
    //     Шспользуется только если при создании класса не включен параметр bodyHTML
    //
    //------------------------------------------------------------------------------------------
    Row.prototype.getBodyHtml = function(o) {
        var headerHTML =  '';
        if ( o.item.startNewSession ) {
            headerHTML = '<div class="img-header">' + utils.toDateString(o.item.createTime) + '</div>';
        } else if (o.header) {
            headerHTML =  '<div class="img-header"></div>'
        }

        //logger.debug("[getBodyHtml] input object o=",o);

        var siteIcon = "fa-google-plus";
        switch (o.item.siteType) {
            case "Google":
                siteIcon = "fa-google-plus";
                break;
            case "Local":
                siteIcon = "fa-database";
                break;
        }

        return headerHTML +
            '<div id="img-' + o.item.id + '" class="img-bg" style="'+
            '   background-image:url('+o.item.url+');'+
            '   background-size:'+o.item.drawWidth+'px '+o.item.drawHeight+'px;'+
            '   width:'+o.item.drawWidth+'px;height:'+o.item.drawHeight+'px;'+
            '">'+
            '   <div class="img-btn-bar">' +
            '      <div class="img-btn img-select"><i class="fa fa-check-circle"></i></div>' +
            '      <div class="img-btn img-info"><i class="fa fa-info-circle"></i></div>' +
            '   </div>' +
            '   <div class="info-bar">'+
            '      <div class="sign pull-right"><i class="fa '+siteIcon+'"></i></div>' +
            '   </div>' +
            '</div>';
    };

    return Row;

});