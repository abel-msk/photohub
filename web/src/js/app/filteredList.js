define(["jquery","scroller/scroller","api","modalDialog","logger"],
    function ($,Scroller,Api,Dialog,logger) {

        "use strict";
        var DEBUG = true;
        var DEFAULT_PAGE_SIZE = 50;
        var defFilter = {
            'minDate': null,
            'maxDate': null,
            'sitesList':null
        };

        function FilteredList(viewportElement,initialFilter) {
            var caller = this;
            this.viewportElement = (viewportElement instanceof jQuery) ? viewportElement[0] : viewportElement;
            this.filter = $.extend(true, defFilter, initialFilter || {});

            this.setFilter(this.filter);
        }

        //------------------------------------------------------------------
        //
        //   Set filtered data
        //
        //------------------------------------------------------------------
        FilteredList.prototype.setFilter = function(filter) {
            try {
                this.scroller && this.scroller.destroy();
                delete this.scroller;
                var caller = this;

                if (filter) {
                    this.filter = {
                        'limit': DEFAULT_PAGE_SIZE,
                        'offset': 0,
                        'sitesList': filter.sitesList ? filter.sitesList : null,
                        'minDate': filter.fromDate ? filter.fromDate.toISOString() : null,
                        'maxDate': filter.toDate ? filter.toDate.toISOString() : null
                    };

                    this.scroller = new Scroller({
                            'viewport': this.viewportElement,
                            'loadAsLast': function (limit, offset, el) {
                                caller._loadAsLast.call(caller, limit, offset, el);
                            },
                            'loadAsFirst': function (limit, offset, el) {
                                caller._loadAsFirst.call(caller, limit, offset, el);
                            }
                        }
                    );

                    this._loadPage("append", this.filter, null);
                }
            } catch (e) {
                logger.debug("[FilteredList.setFilter] Load",e);
            }
        };

        //------------------------------------------------------------------
        //
        //   Вызывается когда скролл доходит до последней страницы
        //
        //------------------------------------------------------------------
        FilteredList.prototype._loadAsLast = function(limit,offset,el) {
            this.filter.limit = limit<0?50:limit;
            this.filter.offset = offset<0?0:offset;
            this._loadPage("append",this.filter,el);
        };

        //------------------------------------------------------------------
        //
        //   Вызывается когда скролл доходит до первой страницы
        //
        //------------------------------------------------------------------
        FilteredList.prototype._loadAsFirst = function(limit,offset,el) {
            this.filter.limit = limit<0?50:limit;
            this.filter.offset = offset<0?0:offset;
            this._loadPage("prepend",this.filter,el);
        };

        //------------------------------------------------------------------
        //
        //   Загружаем данные от сервера
        //
        //------------------------------------------------------------------
        FilteredList.prototype._loadPage = function(direction,filter,el) {

            DEBUG && logger.debug("[FilteredList._loadPage]  Direction="+ direction
                +", Filter="+filter);

            var caller = this;
            Api.photoList(
                filter,
                //  on Success
                function(response) {
                    if (response.object) {
                        if (direction === "append") {
                            caller.scroller.append(response.object, {
                                'limit': response.limit,
                                'offset': response.offset
                            });
                        }
                        else {
                            caller.scroller.prepend(response.object, {
                                'limit': response.limit,
                                'offset': response.offset
                            });
                        }
                    }
                    else {
                        logger.debug("[FilteredList.loadPage] Error. Api success return w/o task object");
                    }
                },

                //  on Error
                function(response){
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
        };

        //--------------------------------------------------------------------------
        //
        //   Delete photo items
        //
        //     itemObject = {
        //         pageOffset -   порядковы номер первой фотки на странице относительно общего начала списка при текущем фильтре
        //         pageLimit  -   к-во фотографий на странице
        //         pageId     -   id  страницы  (назначается во время отрисовки)
        //         id         -   id  фотографии в  DB
        //         pos        -   порядковый номер фотографии на странице
        //     }
        //
        //   Params:
        //       itemArray - is an object where each method return of itemObject where method name  id the ID of itemObject
        //--------------------------------------------------------------------------

        FilteredList.prototype.removeItems = function(itemArray) {

            var sortedIndex = Object.keys(itemArray).sort(
                function(a,b) {
                    return itemArray[a].pageId - itemArray[b].pageId;
                });
            var curPage = -1;
            var curPageItemsAR = [];

            for (var i = 0; i < sortedIndex.length; i++) {
                var pgId = itemArray[sortedIndex[i]].pageId;
                if (pgId !== curPage ) {
                    if (curPage >= 0  ) { //close array and start delete from one page.
                        this.scroller.removePageItems(curPageItemsAR,curPage);
                        curPageItemsAR = [];
                    }
                    curPage = pgId;
                    curPageItemsAR.push(itemArray[sortedIndex[i]].id);
                }
                else {
                    curPageItemsAR.push(itemArray[sortedIndex[i]].id);
                }
            }

            // process last page
            if ( curPageItemsAR.length > 0 ) {
                this.scroller.removePageItems(curPageItemsAR,curPage);
            }
        };



        //------------------------------------------------------------------
        //
        //   destroy
        //
        //------------------------------------------------------------------
        FilteredList.prototype.destroy = function() {
            this.scroller.destroy();
        };

        return FilteredList;
    });