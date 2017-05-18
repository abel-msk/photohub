/**
 * Created by abel on 26/10/16.
 *
 *
 */
define(["jquery","pikaday","modalDialog","utils","moment","logger"],function($,Pikaday,Dialog,Utils,moment,logger) {
    "use strict";

    //'defRanges':[
    //    {'name':'Last week','begin':Date(),'end':Date()},
    //    {'name':'Last month','begin':Date(),'end':Date()}
    //]

    var today = new Date();

    var BTN_APPLY="trp-apply";
    var defaultOptions = {
        'actionEl' : null,
        'fromEl': null,
        'toEl': null,
        'onChange':null,
        'onApply':null,
        'defRanges':[
                {'name':'Last week','begin':today,'end':today},
                {'name':'Last month','begin':today,'end':today}
        ]
    };



    //var DD_MENU_ATTR='data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"';

    function getPanelsHtml(DDMenuId,frameId) {

        var str = '<div id="'+frameId+'" class="dropdown-menu" aria-labelledby="'+DDMenuId+'" style="width:680px;height:auto">' +
        '		<div  class="panel-body date-range-picker">' +
        //'			<div id="dif-'+frameId+'" class="clearfix input-frame">'+
        //'					<label for="fdi-'+frameId+'" class="">From date</label>'+
        //'					<input id="fdi-'+frameId+'" type="text" class="form-control date-input" aria-hidden="true" placeholder="00/00/00">'+
        //'					<label for="tdi-'+frameId+'" class="">To date</label>'+
        //'					<input id="tdi-'+frameId+'" type="text" class="form-control date-input" aria-hidden="true" placeholder="00/00/00">'+
        //'			</div>'+
        '			<div class="clearfix">' +

        '<div class="pull-left calendar-frame">'+
        '   <div class="input-group hasExtInput">'+
        '		<input id="fdi-'+frameId+'" type="text" class="form-control"'+
        '		   placeholder="Date ...">'+
        '	</div>'+
        '	<div id="from-'+frameId+'"   class="calendar-area"></div>'+
        '</div>'+
        '<div class="pull-left calendar-frame">'+
        '	<div class="input-group hasExtInput">'+
        '		<input id="tdi-'+frameId+'" type="text" class="form-control"'+
        '		   placeholder="Date ...">'+
        '	</div>'+
        '	<div id="to-'+frameId+'" class="calendar-area"></div>'+
        '</div>'+

        //'				<div id="from-'+frameId+'"  class="from-date-area pull-left"></div>' +
        //'				<div id="to-'+frameId+'"  class="to-date-area pull-left"></div>' +

        '				<div id="dra-'+frameId+'" class="def-ranges-area hasExtInput pull-left">' +
        //'					<p><button type="button" class="btn btn-default btn-block" >Last Week</button></p>' +
        //'					<p><button type="button" class="btn btn-default btn-block" >Last Month</button></p>' +
        //'					<p><button type="button" class="btn btn-default btn-block" >Last 3 Month</button></p>' +
        //'					<p><button type="button" class="btn btn-default btn-block" >Last Year</button></p>' +
        '					<div class="apply">' +
        '						<button id="apply-'+frameId+'" type="button" class="btn btn-primary btn-default btn-block"  data-dismiss="dropdown-menu">' +
        '							<span>Apply</span>' +
        '						</button>' +
        '					</div>' +
        '				</div>' +
        '			</div>' +
        '		</div>' +
        '</div>';
        return str;
    }



    //---------------------------------------------------------------------------
    //
    //   Конструктор
    //
    //---------------------------------------------------------------------------
    function DataRangePicker(options) {
        this.options = $.extend(true,{},defaultOptions,options || {});


        //   Берем ID для кликабелього эдемента для которого открываем пикер
        //   если  ID  не установлен то генерируем его
        if (! this.options.actionEl )  {
            throw new Error("[DataRangePicker:Init] ERROR: Action element should be defined.");
        }
        try {
            var caller = this;

            this.actionItemId = $(this.options.actionEl).attr("id");

            if (!this.actionItemId) {
                this.actionItemId = "drp-" + Math.floor((Math.random() * 10000));
                $(this.options.actionEl).attr("id", this.actionItemId);
            }

            //  Генерируем ID для выпадающего фрейма
            this.frameId = "frm-" + this.actionItemId;

            //  Добавляем атрибуты для вызывающего объекта, что бы привязать к нему выпадающее меню
            $('#' + this.actionItemId)
                .attr({
                    "data-toggle": "dropdown",
                    "aria-haspopup": "true",
                    "aria-expanded": "false"
                })
                .after(getPanelsHtml(this.actionItemId, this.frameId));


            //  Создаем кнопки интервалов
            if (this.options.defRanges &&
                this.options.defRanges.constructor==Array &&
                this.options.defRanges.length!=0) {
                for (var i = 0; (i < this.options.defRanges.length) && i < 5 ; i++) {
                    var btn = '<p><button data-index="' + i + '" type="button" class="range-btn btn btn-default btn-block" style="top:'+(32*i)+'px">' +
                        this.options.defRanges[i].name + '</button></p>';
                    $("#dra-"+this.frameId).append(btn);
                }
            }

            //   Устанавливаем  обработчик нажатий на кнопки интервалов
            $("#dra-"+this.frameId + " .range-btn").on("click",function(event) {
                var dataId =  $(this).attr("data-index");
                logger.debug("[DataRangePicker.onClick] btnID="+dataId);
            });

            //  Если поля ввода дат на заданы то используем собственные.
            //  Для этого их надо сделать видимыми. т.е  удалить все вхождения класса hasExtInput
            if ((this.options.fromEl) || ( this.options.toEl)) {
                $("#"+this.actionItemId+ " .hasExtInput").removeClass("hasExtInput");
            }

            if ( ! this.options.toEl ) {
                this.options.toEl = document.getElementById('tdi-' + this.frameId);
            }
            if (! this.options.toEl.value) {
                this.options.toEl.value = Utils.toDateString(new Date());
            }


            //  Определяем начальные значения дат на привязанных Input
            if ( ! this.options.fromEl ) {
                this.options.fromEl = document.getElementById('fdi-' + this.frameId);
            }
            if (! this.options.fromEl.value) {
                this.options.fromEl.value = Utils.toDateString(
                    new Date(
                        Utils.dateStr2obj(this.options.toEl.value).getTime() - (24*60*60*1000)
                    )
                );
            }


            //
            //var fromInput = this.fromInput =  null;
            //var toInput = this.toInput = null;
            //
            //if ( this.options.fromEl) {
            //    fromInput = this.fromInput = this.options.fromEl;
            //} else {
            //    fromInput = this.fromInput =  document.getElementById('fdi-' + this.frameId);
            //    //fromInput.value = moment().subtract(1, 'day').format("l");
            //    fromInput.value =  Utils.toDateString(new Date(parseInt(Date.now()) -  (24*60*60*1000) ));
            //}
            //
            //if ( this.options.toEl) {
            //    toInput = this.toInput = this.options.toEl;
            //} else {
            //    toInput = this.toInput = document.getElementById('tdi-' + this.frameId);
            //    toInput.value = Utils.toDateString(new Date());
            //}


            //   Графический   календарь Pickaday

            this.pickerFromObj = new Pikaday({
                defaultDate: Utils.dateStr2obj(this.options.fromEl.value),
                setDefaultDate: Utils.dateStr2obj(this.options.fromEl.value),
                format:'l',
                field: this.options.fromEl,
                bound: false,
                container: document.getElementById('from-' + this.frameId),
                'onSelect': function(date) {
                    caller._onFromChange.call(caller,date);
                }
            });
            this.pickerFromObj.gotoDate(Utils.dateStr2obj(this.options.fromEl.value));


            //   Графический   календарь Pickaday

            this.pickerToObj = new Pikaday({
                defaultDate: Utils.dateStr2obj(this.options.toEl.value),
                setDefaultDate: Utils.dateStr2obj(this.options.toEl.value),
                format:'l',
                field: this.options.toEl,
                bound: false,
                container: document.getElementById('to-' + this.frameId),
                onSelect: function(date) {
                    caller._onToChange.call(caller,date);
                }
            });
            this.pickerToObj.gotoDate(Utils.dateStr2obj(this.options.toEl.value));




            //  Добавляем обработчик событий смены месяца и года для начальной даты

            $("#from-" + this.frameId).on("change",{"caller":caller}, function(event) {

                var caller = event.data.caller;
                var target = event.target || event.srcElement;
                if (!target) { return;  }

                var newDate = null;
                if (Utils.hasClass(target, 'pika-select-month')) {
                    newDate= caller.getFromDate().setMonth(target.value);
                }
                else if (Utils.hasClass(target, 'pika-select-year')) {
                    newDate = caller.getFromDate().setYear(target.value);
                }
                else {
                    return;
                }
                caller._onFromChange.call(caller,new Date(newDate));
                //event.stopImmediatePropagation();
            });


            //  Добавляем обработчик событий для смен месяца и года для конечной даты

            $("#to-" + this.frameId).on("change",{"caller":caller}, function(event) {
                var caller = event.data.caller;
                var target = event.target || event.srcElement;
                if (!target) {
                    return;
                }
                var newDate = null;
                if (Utils.hasClass(target, 'pika-select-month')) {
                    newDate = caller.getToDate().setMonth(target.value);
                }
                else if (Utils.hasClass(target, 'pika-select-year')) {
                    newDate = caller.getToDate().setYear(target.value);
                }
                else {
                    return;
                }
                caller._onToChange.call(caller,new Date(newDate));
                //event.stopImmediatePropagation();
            });


            //  Усанавливаем обработчик для перехвата закрываения попапа при клике на элементе фрейма
            $("#" + this.frameId)
                .on("click", function (event) {
                    event.stopImmediatePropagation();
                })
                .on("click", function (event) {
                    event.stopImmediatePropagation();
                    //e.stopPropagation();
                    //e.preventDefault();
                });


            //   Обработчик кнопки APPLY
            $('#' + 'apply-'+this.frameId).on("click", {'caller': caller, 'options': this.options},
                function (event) {
                    var options = event.data.options;

                    if ((options) && (typeof options.onApply == "function")) {
                        options.onApply(event.data.caller.getFromDate(), event.data.caller.getToDate());
                    }
                    else {
                        // Если нет доп. обработчика то после обработки события закрываем DD-меню
                        $('#' + caller.actionItemId).dropdown('toggle');
                    }
                });


        } catch (e) {
            logger.debug("[DataRangePicker.Init] ERROR: " + e, e.stack);
        }
    }


    DataRangePicker.prototype._onFromChange = function(date) {
        logger.debug("[dataRangePicker._onFromChange] From date changed = " + moment(date).format("l") +
            ", toDateString=" + Utils.toDateString(date));

        if  (this.pickerToObj.getDate() && (date.getTime() > this.pickerToObj.getDate().getTime())) {
            var logMsg = "[dateRangePicker.onSelect]  Ending date cannot be earlier than beginning date.";
            logger.debug(logMsg);
            throw Error(logMsg);
        }

        this.options.fromEl.value = Utils.toDateString(date);
        this.pickerFromObj.setDate(date,true);
        //this.pickerFromObj.gotoDate(date);

        if (this.options.onChange && (typeof this.options.onChange == "function")) {
            this.options.onChange(date,this.getToDate());
        }
    };


    DataRangePicker.prototype._onToChange = function(date) {
        logger.debug("[dataRangePicker._onToChange] To date changed = " + moment(date).format("l"));

        if  (this.pickerFromObj.getDate() && (date.getTime() < this.pickerFromObj.getDate().getTime())) {
            var logMsg = "[dateRangePicker.onSelect]  Ending date cannot be earlier than beginning date.";
            logger.debug(logMsg);
            throw Error(logMsg);
        }

        this.options.toEl.value = Utils.toDateString(date);
        this.pickerToObj.setDate(date,true);
        //this.pickerToObj.gotoDate(date);

        if (this.options.onChange && (typeof this.options.onChange == "function")) {
            this.options.onChange(this.getFromDate(),date);
        }
    };


    DataRangePicker.prototype.close = function() {
        $('#'+ this.actionItemId ).dropdown('toggle');
    };

    DataRangePicker.prototype.getStrFromDate = function() {
        return this.fromField.value;
    };
    DataRangePicker.prototype.getStrToDate = function() {
        return this.toField.value;
    };

    DataRangePicker.prototype.getFromDate = function() {
        return this.pickerFromObj.getDate();
    };
    DataRangePicker.prototype.getToDate = function() {
        return this.pickerToObj.getDate();
    };

    return DataRangePicker;
});