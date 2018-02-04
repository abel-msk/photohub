/**
 *
 *    Управляет масивом объектов itemObjects
 *    Масив используется для хранения ланных по выделенным фоткам.
 *
 *    itemObject = {
 *        pageOffset -   порядковы номер первой фотки на странице относительно общего начала списка при текущем фильтре
 *        pageLimit  -   к-во фотографий на странице
 *        pageId     -   id  страницы  (назначается во время отрисовки)
 *        id         -   id  фотографии в  DB
 *        pos        -   порядковый номер фотографии на странице
 *    }
 *
 */


define(["jquery","modalDialog","logger","utils"],
    function($,Dialog,logger, Utils) {
        "use strict";

        function Selection() {
            this.items = {};
        }

        //-----------------------------------------------------------------------------------------------------
        //
        //   Создаем новый элемент масива. в качестве параметра получаем DOM элемент описывающий фотку.
        //   Параметры элемента определяется из дополнительных тагов элементов
        //
        //   Expected image element tag. Example:
        //
        //   <div id="img-frame-9747" data-id="1875" data-count="5" class="img-frame"
        //      data-id="1875" - DB element id
        //      data-count     -  position in page object
        //
        //   Expected element tag describing page and placed as one of parent:
        //
        //   <div id="page-230" data-id="0" data-offset="0" class="content-page" data-limit="23"
        //      data-id="0"      - page object ID (usaly page count from beginning)
        //      data-offset="0"  - filters DB requests offset value
        //      data-limit="23"  - filters DB requests limit value^ calculated due to rendering process
        //
        //-----------------------------------------------------------------------------------------------------

        Selection.prototype.putItem = function(element) {
            var itemObject = {};

            var thePageEL = Utils.getParent(element,"content-page");
            if (thePageEL) {
                itemObject.pageOffset = parseInt(thePageEL.getAttribute("data-offset"));
                itemObject.pageLimit = parseInt(thePageEL.getAttribute("data-limit"));
                itemObject.pageId = parseInt(thePageEL.getAttribute("data-id"));
                itemObject.id = parseInt(element.getAttribute("data-id"));
                itemObject.pos = parseInt(element.getAttribute("data-count"));

                this.items[itemObject.id] = itemObject;
            }
        };

        Selection.prototype.getItem = function(id) {
            return this.items[id];
        };

        Selection.prototype.getItemPage = function(id) {
            return this.items[id].pageId;
        };

        Selection.prototype.removeItem = function(id) {
            var removedItem = this.items[id];
            if ( removedItem ) {
                delete this.items[id];
            }
            return  removedItem;
        };

        // Selection.prototype.getSortedById = function() {
        //     return Object.keys(this.items).sort(function(a,b){return a - b;})
        // };
        //
        // Selection.prototype.getSortedByPage = function() {
        //     var caller = this.items;
        //     return Object.keys(this.items).sort(function(a,b){return caller.items[a].pageId - caller.items[b].pageId;})
        // };

        Selection.prototype.getHash = function() {
            return this.items;
        };


        Selection.prototype.length = function() {
            return parseInt(Object.keys(this.items).length);
        };

        Selection.prototype.isEmpty = function() {
            return Object.keys(this.items).length < 1 ;
        };

        Selection.prototype.clean = function() {
            return this.items = [] ;
        };


        return Selection;
    });