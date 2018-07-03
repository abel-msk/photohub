/**
 * Created by abel on 08.11.16.
 *   Обрабатывает меню фильтров в главном окне
 */


define(["jquery","api","modalDialog","logger","utils","moment","dateRangePicker","filteredList"],
    function($,Api,Dialog,logger,Utils,moment,dateRangePicker,FilteredList) {

        "use strict";

        var defFilter = {
            'minDate': null,
            'maxDate': null,
            'sitesList':null
        };

        function FilterClass(viewport,initialFilter) {
            try {
                var caller = this;
                this.viewport = (viewport instanceof jQuery) ? viewport[0] : viewport;
                this.filter = $.extend(true, defFilter, initialFilter || {});

                var datePicker = new dateRangePicker({
                    'actionEl': document.getElementById('range-selector'),
                    'fromEl': null,
                    'toEl': null,
                    'onChange': function (fromDate, toDate) {
                        $("#from-date-input").val(Utils.toDateString(fromDate));
                        $("#to-date-input").val(Utils.toDateString(toDate));
                        logger.debug("[dateRangePicker] On change. From-" +
                            Utils.toDateString(fromDate) + ", to-" + Utils.toDateString(toDate)
                        );
                        caller.filter.minDate = fromDate;
                        caller.filter.maxDate = toDate;
                    },
                    'defRanges': [
                        {'name': 'Last week', 'begin': Date(), 'end': Date()},
                        {'name': 'Last month', 'begin': Date(), 'end': Date()}
                    ]
                });


                $("#apply-filter").on("click", {'caller':this}, function(event) {
                    var target = event.target || event.srcElement;
                    var caller = event.data.caller;
                    caller.load(caller.filter);
                });

                this.load(this.filter);
            }
            catch (e) {
                logger.debug("[FilterClass.init] Error", e);
            }
        }

        //--------------------------------------------------------------------------
        //
        //    Load catalog with new filer
        //
        FilterClass.prototype.load = function(filter) {
            logger.debug("[Filter.load] Date From-" + filter.minDate + " - to-" + filter.maxDate );

            if (this.filteredList) {
                this.filteredList.destroy();
            }
            this.filteredList = new FilteredList(this.viewport,filter);
            this.filter = filter;
        };

        /**--------------------------------------------------------------------------
         *
         *   Load single object at their position number (offset), according to current filter set.
         *   Received object are sent to second parameters function ( onSuccess )
         *
         * @param offset  object offset in objects list with current filter
         * @param onSuccess - called with loaded object
         *
         --------------------------------------------------------------------------*/
        FilterClass.prototype.loadSingle = function(offset, onSuccess) {

            Api.photoList({
                    "limit": 1,
                    "offset": offset,
                    "minDate": this.filter.minDate ? this.filter.minDate.toISOString() : null,
                    "maxDate": this.filter.maxDate ? this.filter.maxDate.toISOString() : null,
                    "sitesList": this.filter.sitesList
                },

                //  on Success
                function(response) {
                    if (response.object) {
                        if (typeof onSuccess === "function") {
                            onSuccess(response.object[0]);
                        }
                    }
                    else {
                        logger.debug("[FilteredList.loadSingle] Error. No response object.");
                    }
                },

                //  on Error
                this._loadError
                // function(response){
                //     Dialog.open({
                //         'error': true,
                //         'title': "Server error",
                //         'text': response.message,
                //         'buttons': {OK: function(){}}
                //     });
                // }
            );
        };


        /**--------------------------------------------------------------------------
         *
         *   Perform on backent object action and return newly created photo object
         *
         * @param photoId
         * @param cmd
         * @param onSuccess
         --------------------------------------------------------------------------*/
        FilterClass.prototype.transform = function(photoId,cmd, onSuccess) {

            logger.debug("[FilteredList.transform] Called.");


            Api.rotateCW(
                photoId,
                (cmd==="rotateCW"),

                //  on Success
                function(response) {
                    if (response.object) {
                        if (typeof onSuccess === "function") {
                            onSuccess(response.object);
                        }

                        //TODO: call filteredList for replace object


                    }
                    else {
                        logger.debug("[FilteredList.transform] Error. No response object.");
                    }
                },
                //  on Error
                this._loadError
            );
        };


        /**--------------------------------------------------------------------------
         *   Loading error process
         * @param response backend response
         * @private
         --------------------------------------------------------------------------*/
        FilterClass.prototype._loadError = function(response) {
            Dialog.open({
                'error': true,
                'title': "Server error",
                'text': response.message,
                'buttons': {OK: function(){}}
            });
        };


        /**--------------------------------------------------------------------------
         *
         * Return photos list created with current filter
         *
         * @returns {module:filteredList|*}
         --------------------------------------------------------------------------*/
        FilterClass.prototype.getFilteredList = function() {
            return this.filteredList;
        };

        return FilterClass;

});