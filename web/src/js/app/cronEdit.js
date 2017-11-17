/**
 * Created by abel on 09.05.17.
 */

define(["jquery","modalDialog","utils","logger"],
    function($,Dialog,Utils,logger) {
        "use strict";

        var DEBUG = true;

        var month_ru = ['янв','фев','март','апр','май','июнь','июль','авг','сент','откт','ноя','дек'];
        var week_ru = ['пн','вт','ср','чт','пт','сб','вс'];

        var defHtmlOptions = {
            'cellwidth' : 'width:11%',
            'week' : week_ru,
            'month': month_ru,
            'cron' : null,
            'secId': '-min-id',
            'minId': '-min-id',
            'hourId': '-hour-id',
            'dayId': '-day-id',
            'monthId': '-month-Id',
            'dayOfWeekId':'-weekd-id'
        };

        //---------------------------------------------------------------------------
        //
        //    Подготовить HTML блока
        //
        //---------------------------------------------------------------------------
        function cronHTML(param_options) {
            var options = $.extend(true,defHtmlOptions,param_options);

            var retstr = '<div class="form-cell first" style="'+options.cellwidth+'">'+
                '	<label for="'+options.secId+'" >Sec.</label>'+
                '	<textarea id="'+options.secId+'" type="text" class="form-control" placeholder="*"></textarea>'+
                '</div>'+
                '<div class="form-cell" style="'+options.cellwidth+'">'+
                '	<label for="'+options.minId+'" >Min.</label>'+
                '	<textarea id="'+options.minId+'" type="text" class="form-control" placeholder="*"></textarea>'+
                '</div>'+
                '<div class="form-cell" style="'+options.cellwidth+'">'+
                '	<label for="'+options.hourId+'" >Hours</label>'+
                '	<select id="'+options.hourId+'" multiple class="form-control" >'+
                '		<option value="*" selected="selected">*</option>'+
                '		<option value="0">00</option>'+
                '		<option value="1">01</option>'+
                '		<option value="2">02</option>'+
                '		<option value="3">03</option>'+
                '		<option value="4">04</option>'+
                '		<option value="5">05</option>'+
                '		<option value="6">06</option>'+
                '		<option value="7">07</option>'+
                '		<option value="8">08</option>'+
                '		<option value="9">09</option>'+
                '		<option value="10">10</option>'+
                '		<option value="11">11</option>'+
                '		<option value="12">12</option>'+
                '		<option value="13">13</option>'+
                '		<option value="14">14</option>'+
                '		<option value="15">15</option>'+
                '		<option value="16">16</option>'+
                '		<option value="17">17</option>'+
                '		<option value="18">18</option>'+
                '		<option value="19">19</option>'+
                '		<option value="20">20</option>'+
                '		<option value="21">21</option>'+
                '		<option value="22">22</option>'+
                '		<option value="23">23</option>'+
                '	</select>'+
                '</div>'+
                '<div class="form-cell" style="'+options.cellwidth+'">'+
                '	<label for="'+options.dayId+'" >Day</label>'+
                '	<select id="'+options.dayId+'" multiple class="form-control" >'+
                '		<option value="*" selected="selected">*</option>'+
                '		<option value="1">01</option>'+
                '		<option value="2">02</option>'+
                '		<option value="3">03</option>'+
                '		<option value="4">04</option>'+
                '		<option value="5">05</option>'+
                '		<option value="6">06</option>'+
                '		<option value="7">07</option>'+
                '		<option value="8">08</option>'+
                '		<option value="9">09</option>'+
                '		<option value="10">10</option>'+
                '		<option value="11">11</option>'+
                '		<option value="12">12</option>'+
                '		<option value="13">13</option>'+
                '		<option value="14">14</option>'+
                '		<option value="15">15</option>'+
                '		<option value="16">16</option>'+
                '		<option value="17">17</option>'+
                '		<option value="18">18</option>'+
                '		<option value="19">19</option>'+
                '		<option value="20">20</option>'+
                '		<option value="21">21</option>'+
                '		<option value="22">22</option>'+
                '		<option value="23">23</option>'+
                '		<option value="24">24</option>'+
                '		<option value="25">25</option>'+
                '		<option value="26">26</option>'+
                '		<option value="27">27</option>'+
                '		<option value="28">28</option>'+
                '		<option value="29">29</option>'+
                '		<option value="30">30</option>'+
                '		<option value="31">31</option>'+
                '	</select>'+
                '</div>'+
                '<div class="form-cell" style="'+options.cellwidth+'">'+
                '	<label for="'+options.monthId+'" >Month</label>'+
                '	<select id="'+options.monthId+'" multiple class="form-control" >'+
                '		<option value="*" selected="selected">*</option>'+
                '		<option value="1">'+options.month[0]+'</option>'+
                '		<option value="2">'+options.month[1]+'</option>'+
                '		<option value="3">'+options.month[2]+'</option>'+
                '		<option value="4">'+options.month[3]+'</option>'+
                '		<option value="5">'+options.month[4]+'</option>'+
                '		<option value="6">'+options.month[5]+'</option>'+
                '		<option value="7">'+options.month[6]+'</option>'+
                '		<option value="8">'+options.month[7]+'</option>'+
                '		<option value="9">'+options.month[8]+'</option>'+
                '		<option value="10">'+options.month[9]+'</option>'+
                '		<option value="11">'+options.month[10]+'</option>'+
                '		<option value="12">'+options.month[11]+'</option>'+
                '	</select>'+
                '</div>'+
                '<div class="form-cell" style="'+options.cellwidth+'">'+
                '	<label for="'+options.dayOfWeekId+'" >Week-D</label>'+
                '	<select id="'+options.dayOfWeekId+'" multiple class="form-control" >'+
                '		<option value="*" selected="selected">*</option>'+
                '		<option value="1">'+options.week[0]+'</option>'+
                '		<option value="2">'+options.week[1]+'</option>'+
                '		<option value="3">'+options.week[2]+'</option>'+
                '		<option value="4">'+options.week[3]+'</option>'+
                '		<option value="5">'+options.week[4]+'</option>'+
                '		<option value="6">'+options.week[5]+'</option>'+
                '		<option value="7">'+options.week[6]+'</option>'+
                '	</select>'+
                '</div>' ;
            return  retstr;
        }


        //---------------------------------------------------------------------------
        //
        //
        //
        //
        //
        //
        //---------------------------------------------------------------------------
        function CronEdit(appendTo,cronObj,onChangeCB) {
            this.cronObj = cronObj;
            this.change = false;
            this.onChangeCB = onChangeCB;
            this.elementId = appendTo;


            this.idSet = {
                'secId': cronObj.id.toString()+'-sec-id',
                'minId': cronObj.id.toString()+'-min-id',
                'hourId': cronObj.id.toString()+'-hour-id',
                'dayId': cronObj.id.toString()+'-day-id',
                'monthId': cronObj.id.toString()+'-month-Id',
                'dayOfWeekId':cronObj.id.toString()+'-weekd-id'
            };


            var htmlStr = cronHTML($.extend(false,defHtmlOptions,this.idSet));
            $("#"+appendTo).append(htmlStr);
            this.setCron(cronObj);
            this.setListeners();
        }


        //---------------------------------------------------------------------------
        //
        //     Set default values to creon edit form
        //
        //---------------------------------------------------------------------------
        CronEdit.prototype.setCron = function(cron) {
            $('#'+this.idSet.secId).val(cron.seconds || '*');
            $('#'+this.idSet.minId).val(cron.minute || '*');
            $('#'+this.idSet.hourId).val(cron.hour.split(",") || "*");
            $('#'+this.idSet.dayId).val(cron.dayOfMonth.split(",") || '*');
            $('#'+this.idSet.monthId).val(cron.month.split(",") || '*');
            $('#'+this.idSet.dayOfWeekId).val(cron.dayOfWeek.split(",") || '*');

            this.cronObj = cron;
        };

        //---------------------------------------------------------------------------
        //
        //     Return current cron object
        //
        //---------------------------------------------------------------------------
        CronEdit.prototype.getCron = function() {
            return this.cronObj;
        };

        //---------------------------------------------------------------------------
        //
        //     Устанавливаем обработчики на изменение cron параметров
        //
        //---------------------------------------------------------------------------

        CronEdit.prototype.setListeners = function(onChange) {

            if ( onChange && typeof onChange == "function" ) {
                this.onChange = onChange;
            }

            $('#'+this.idSet.secId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.seconds = $(event.target || event.srcElement).val();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });

            $('#'+this.idSet.minId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.minute = $(event.target || event.srcElement).val();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });

            $('#'+this.idSet.hourId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.hour = $(event.target || event.srcElement).val().join();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });

            $('#'+this.idSet.dayId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.dayOfMonth = $(event.target || event.srcElement).val().join();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });

            $('#'+this.idSet.monthId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.month = $(event.target || event.srcElement).val().join();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });

            $('#'+this.idSet.dayOfWeekId).off('change').
            on('change',{'caller':this}, function(event) {
                event.data.caller.cronObj.dayOfWeek = $(event.target || event.srcElement).val().join();
                event.data.caller.onChange(event.data.caller.elementId,event.data.caller.cronObj);
            });
        };


        //---------------------------------------------------------------------------
        //
        //     Compile cron object after changes and call callback
        //
        //---------------------------------------------------------------------------
        CronEdit.prototype.onChange = function( ) {

            debug.print("[CronEdit.onChange] Cron setting changed. Cron="+
                this.cronObj.seconds + " " +
                this.cronObj.minute +  " " +
                this.cronObj.hour +  " " +
                this.cronObj.dayOfMonth +  " " +
                this.cronObj.month +  " " +
                this.cronObj.dayOfWeek
            );

            if ( typeof this.onChangeCB == "function" ) {
                this.onChangeCB(this.elementId,this.cronObj);
            }
            this.validate(this.cronObj);
        };


        return CronEdit;
    }
);

