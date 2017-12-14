/**
 * Created by abel on 17/05/2017.
 */


/*
Объект рассписания   CronObj:

        "id": null,
        "taskName": "TNAME_EMPTY",
        "enable": true,
        "seconds": "*",
        "minute": "*",
        "hour": "*",
        "dayOfMonth": "*",
        "month": "*",
        "dayOfWeek": "*"

Проверяем на попадание в границы допустимого значения  для  каждой позиции

        minute        0-59
        hour          0-23
        day of month  1-31
        month         1-12 (or names, see below)
        day of week   0-7 (0 or 7 is Sun, or use names)


Внутренний объект для хранения параметрой рассписания

    type: 0 - 4      [ 0-disabled  1 - hourly, 2 - daily, 3 - monthly, 4 - weekly ]
    day
    dayWeek
    hour
    min


                                                     Seconds	Minete	Hour	DayOfMonth	month	dayOfWeek
Period by hour       start at  12 hour 18 min   |    1		18	*	*		*	*
          Day        start at  12 hour 18 min   |    1		18	12	*		*	*
          month      start at  12 hour 18 min   |    1		18	12	today(daOfMont)	*	*
          week       start at  12 hour 18 min   |    1		18	12	*		*	today(dayOfWeek)



 */

define(["jquery","logger","moment"],
    function($,logger,moment) {
        "use strict";

        var cronObjKey = ["seconds","minute","hour","dayOfMonth","month","dayOfWeek"];

        var DAY_SEL = "-day-sl";
        var DAYWEEK_SEL = "-dayweek-sl";
        var HOUR_SEL = "-hour-sl";
        var MIN_SEL = "-min-sl";
        var PERIOD_SEL = "-period-sl";

        var TKN_START = "-start-at";
        var TKN_DAY = "-day-tkn";
        var TKN_HOUR = "-hour-tkn";
        var TKN_MIN = "-min-tkn";


        function schedFormHTML(schedObj) {

            var htmlStr = "<div style='padding-top:15px'></div>"+
                "<div class='text-right row-label'>"+
                "    <span style='line-height: 30px'>Period</span>"+
                "</div>"+
                ""+
                "<form class='row-data form-inline'>"+
                "    <div class='form-group form-group-sm'>"+
                "        <select  class='form-control' id='"+schedObj.id+PERIOD_SEL+"' style='font-size: inherit'>"+
                "            <option value='0' selected='selected'>Disabled</option>"+
                "            <option value='1'>Hourly</option>"+
                "            <option value='2'>Daily</option>"+
                "            <option value='3'>Weekly</option>"+
                "            <option value='4'>Monthly</option>"+
                "        </select>"+
                "    </div>"+
                ""+
                "    <span id='"+schedObj.id+TKN_START+"' class=''>Start at:</span>"+

                "    <div class='form-group form-group-sm'>"+
                "        <select  class='form-control' id='"+schedObj.id+DAYWEEK_SEL+"' style='font-size: inherit'>"+
                "            <option value='*' selected='selected'>*</option>"+
                "            <option value='1'>Mon</option>"+
                "            <option value='2'>Tue</option>"+
                "            <option value='3'>Wed</option>"+
                "            <option value='4'>Thu</option>"+
                "            <option value='5'>Fri</option>"+
                "            <option value='6'>Sat</option>"+
                "            <option value='7'>Sun</option>"+
                "        </select>"+

                "        <select  class='form-control' id='"+schedObj.id+DAY_SEL+"' style='font-size: inherit'>"+
                "            <option value='*' selected='selected'>*</option>";

                for (var i = 1; i < 32; i++) {
                    htmlStr = htmlStr +   "<option value='"+i+"'>"+i+"</option>";
                }

            htmlStr = htmlStr +   ""+
                "        </select>"+
                "    </div>"+

                "    <span id='"+schedObj.id+TKN_DAY+"' class=''>day </span>"+
                "    <div class='form-group form-group-sm'>"+
                "        <div class='input-group'>"+
                "            <select  class='form-control ' id='"+schedObj.id+HOUR_SEL+"' style='font-size: inherit'>"+
                "                <option value='*' selected='selected'>*</option>"+
                "                <option value='1'>01</option>"+
                "                <option value='2'>02</option>"+
                "                <option value='3'>03</option>"+
                "                <option value='4'>04</option>"+
                "                <option value='5'>05</option>"+
                "                <option value='6'>06</option>"+
                "                <option value='7'>07</option>"+
                "                <option value='8'>08</option>"+
                "                <option value='9'>09</option>"+
                "                <option value='10'>10</option>"+
                "                <option value='11'>11</option>"+
                "                <option value='12'>12</option>"+
                "                <option value='13'>13</option>"+
                "                <option value='14'>14</option>"+
                "                <option value='15'>15</option>"+
                "                <option value='16'>16</option>"+
                "                <option value='17'>17</option>"+
                "                <option value='18'>18</option>"+
                "                <option value='19'>19</option>"+
                "                <option value='20'>20</option>"+
                "                <option value='21'>21</option>"+
                "                <option value='22'>22</option>"+
                "                <option value='23'>23</option>"+
                "                <option value='24'>24</option>"+
                "            </select>"+
                "        </div>"+
                "        <span id='"+schedObj.id+TKN_HOUR+"' class=''>hour</span>"+
                ""+
                "        <div class='form-group'>"+
                "            <select  class='form-control ' id='"+schedObj.id+MIN_SEL+"' style='font-size: inherit'>"+
                "                <option value='*' selected='selected'>*</option>"+
                "                <option value='0'>00</option>"+
                "                <option value='5'>05</option>"+
                "                <option value='10'>10</option>"+
                "                <option value='15'>15</option>"+
                "                <option value='20'>20</option>"+
                "                <option value='25'>25</option>"+
                "                <option value='30'>30</option>"+
                "                <option value='35'>35</option>"+
                "                <option value='40'>40</option>"+
                "                <option value='45'>45</option>"+
                "                <option value='50'>50</option>"+
                "                <option value='55'>55</option>"+
                "            </select>"+
                "        </div>"+
                "        <span id='"+schedObj.id+TKN_MIN+"' class=''>min</span>"+
                "    </div>"+
                "</form>";

            return htmlStr;

        }



        //---------------------------------------------------------------------------
        //
        //    Проверяем значение для колонок крон таблицы на соответствие границам
        //
        //---------------------------------------------------------------------------
        function cronValidate(value,key) {

            if ( value.search('[^0-9]+') != -1 ) throw new Error("Incorrect value "+ value+" for " + key);

            switch (key) {
                case cronObjKey[0]:
                    if ((parseInt(value) >= 0) && (parseInt(value) < 60))  return  value;
                case cronObjKey[1]:
                    if ((parseInt(value) >= 0) && (parseInt(value) < 60))  return  value;
                case cronObjKey[2]:
                    if ((parseInt(value) >= 0) && (parseInt(value) < 24))  return  value;
                case cronObjKey[3]:
                    if ((parseInt(value) >= 1) && (parseInt(value) < 32))  return  value;
                case cronObjKey[4]:
                    if ((parseInt(value) >= 1) || (parseInt(value) < 13))  return  value;
                case cronObjKey[5]:
                    if ((parseInt(value) >= 0) || (parseInt(value) < 7))  return  value;
            }

            throw new Error("Incorrect value "+ value+" for " + key);
        }

        //---------------------------------------------------------------------------
        //
        //     Разбиваем сложные выражения типа x,y или x-y  и
        //     вызываем валидатор для x и y отдельно
        //
        //---------------------------------------------------------------------------
        function cronSplit(value,key) {

            //var inputStr = value;
            //  x,y
            if (value.search(',') != -1) {
                var ar = value.split(',');
                for (var i in ar ) {
                    cronSplit(ar[i],key);
                }
            }
            //  x-y
            else if (value.search('-') != -1) {
                var ar = value.split('-');
                for (var i in ar ) {
                    cronSplit(ar[i],key);
                }
            }
            //   x/y
            else if (value.search('/') != -1) {
                var ar = value.split('-');
                if ( ar.length > 2 ) throw new Error("Incorrect value "+ value+" for " + key);
                if (! cronValidate(ar[1],key)) throw new Error("Incorrect value "+ value+" for " + key);

            }
            else {
                if (! cronValidate(value,key)) throw new Error("Incorrect value "+ value+" for " + key);
            }
            return value;
        }

        //---------------------------------------------------------------------------
        //
        //     Упрощенная выгрузка из колонки рег эксп
        //     выбираем репвые цифрф (до спей символов) и возвращаем
        //---------------------------------------------------------------------------
        function cronValidateSimple(value,key) {
            try {
                if (value == "*") return "";
                var sValue = value.match(/[\d]+/);

                if (sValue && (value != "*")) {
                    return cronValidate(sValue[0],key);
                }
            } catch (e) {
                logger.debug("[cronShEdit.cronValidaSimple] Ignored Validation error ", e.stack);
            }
            return "";

        }


        //---------------------------------------------------------------------------
        //
        //     CLASS CronShEdit - генерирует форму задания рассписания
        //     обрабатывает редактирование
        //     выдает cron крон рассписание на ее основе.
        //
        //---------------------------------------------------------------------------

        function CronShEdit(htmlElId,cronObj) {
            this.cronObj = cronObj;
            this.schedObj = this.cron2sched(cronObj);

             //   Генерируем HTML
            $("#"+htmlElId).prepend(schedFormHTML(this.schedObj));


            //    Set on change listeners

            $('#'+this.schedObj.id+PERIOD_SEL).on('change',{'caller':this},function(event) {
                event.data.caller.readForm(event.data.caller.schedObj);
                event.data.caller.refreshForm(event.data.caller.schedObj);
                });
            $('#'+this.schedObj.id+DAYWEEK_SEL).on('change',{'caller':this},function(event) {
                event.data.caller.schedObj = event.data.caller.readForm(event.data.caller.schedObj);
                });
            $('#'+this.schedObj.id+HOUR_SEL).on('change',{'caller':this},function(event) {
                event.data.caller.schedObj = event.data.caller.readForm(event.data.caller.schedObj);
                });
            $('#'+this.schedObj.id+MIN_SEL).on('change',{'caller':this},function(event) {
                event.data.caller.schedObj = event.data.caller.readForm(event.data.caller.schedObj);
                });

            //    Actualize  visibility and values
            this.refreshForm(this.schedObj);

        }


        //---------------------------------------------------------------------------
        //
        //     Кновертируем cronObj в shedObj
        //
        //     "id": null,
        //     "taskName": "TNAME_EMPTY",
        //     "seconds": "*",
        //     "minute": "*",
        //     "hour": "*",
        //     "dayOfMonth": "*",
        //     "month": "*",
        //     "dayOfWeek": "*"
        //
        //---------------------------------------------------------------------------

        CronShEdit.prototype.cron2sched = function(cronObj) {

            // если установлен dayOfWeek  то тип расписания  weekly = 4
            // если установлен dayOfMonth то тип расписания  monthly = 3
            // если установлен hour       то тип расписания  daily = 2
            // если установлен min        то тип расписания  hourly = 1
            // в противном случае считаем что рассписание не установлено

            var schedObj = {
                'id': cronObj.id,
                'type':0,
                'day':"*",
                'dayWeek':'*',
                'hour':"*",
                'min':"*"
            };

            if ( ! cronObj.enable ) return schedObj;

            var tmpVar  = cronValidateSimple(cronObj.minute,"minute");
            if (tmpVar) {
                schedObj.type = 1;
                schedObj.min = parseInt(tmpVar/5)*5;
            }
            else return schedObj;

            tmpVar  = cronValidateSimple(cronObj.hour,"hour");
            if (tmpVar) {
                schedObj.type = 2
                schedObj.min = tmpVar;
            }
            else return schedObj;

            tmpVar  = cronValidateSimple(cronObj.dayOfMonth,"dayOfMonth");
            if (tmpVar) {
                schedObj.type = 3
                schedObj.day = tmpVar;
                return schedObj;
            }
            tmpVar  = cronValidateSimple(cronObj.dayOfWeek,"dayOfWeek");
            if (tmpVar) {
                schedObj.type = 4
                schedObj.dayWeek = tmpVar;
                return schedObj;
            }
            return schedObj;
        };



        //---------------------------------------------------------------------------
        //
        //    Возвращает время и дату следующего события определенного в crone с учетеом
        //    правил и ограничений нашего типа рассписания schedule
        //
        //---------------------------------------------------------------------------
        CronShEdit.prototype.getNextDate = function() {
            //var schedObj = this.cron2sched(cronObj);
            var nextDate = "";

            switch (parseInt(this.schedObj.type)) {
                case 0: return "";
                case 1:    // Hourly
                    nextDate = moment().startOf('hour').add(this.schedObj.min,'m');
                    if ( moment().isAfter(nextDate)) {
                        nextDate = nextDate.add(1, 'h');
                    }
                    break;
                case 2:    // Daily
                    nextDate = moment().startOf('day').add(this.schedObj.hour,'h').add(this.schedObj.min,'m');
                    if ( moment().isAfter(nextDate)) {
                        nextDate = nextDate.add(1, 'd');
                    }
                    break;
                case 3:    // Monthly
                    nextDate = moment().startOf('month').add(this.schedObj.day,'d').add(this.schedObj.hour,'h').add(this.schedObj.min,'m');
                    if ( moment().isAfter(nextDate)) {
                        nextDate = nextDate.add(1, 'М');
                    }
                    break;
                case 4:    // Weekly
                    nextDate = moment().startOf('week').add(this.schedObj.dayWeek,'w').add(this.schedObj.hour,'h').add(this.schedObj.min,'m');
                    if ( moment().isAfter(nextDate)) {
                        nextDate = nextDate.add(1, 'w');
                    }
            }
            return nextDate.format('llll');


        };

        //---------------------------------------------------------------------------
        //
        //     Устанавливаем новый CronObj
        //
        //---------------------------------------------------------------------------
        CronShEdit.prototype.setCron = function(cronObj) {
            this.cronObj = cronObj;
            this.schedObj = this.cron2sched(cronObj);
            this.refreshForm(this.schedObj);
        };
        //---------------------------------------------------------------------------
        //
        //     Возвращаем подготовденый (измененный если редактировали)  cronObj
        //
        //---------------------------------------------------------------------------
        CronShEdit.prototype.getCron = function() {
            return this.sched2cron(this.schedObj,this.cronObj);
        };

        //---------------------------------------------------------------------------
        //
        //     Кновертируем  shedObj в cronObj
        //
        //---------------------------------------------------------------------------
        CronShEdit.prototype.sched2cron = function(schedObj,cronObj) {
            cronObj.seconds = '1';
            cronObj.month = "*";
            cronObj.enable = true;

            switch (parseInt(schedObj.type)) {

                case 0:    // Disable
                    cronObj.seconds = "*";
                    cronObj.minute = "*";
                    cronObj.hours = "*";
                    cronObj.dayOfMonth = "*";
                    cronObj.dayOfWeek = "*";
                    cronObj.enable = false;
                    break;
                case 1:    // Hourly
                    cronObj.minute = schedObj.min;
                    cronObj.hours = "*";
                    cronObj.dayOfMonth = "*";
                    cronObj.dayOfWeek = "*";
                    break;
                case 2:    // Daily
                    cronObj.minute = schedObj.min;
                    cronObj.hours = schedObj.hour;
                    cronObj.dayOfMonth =  "*";
                    cronObj.dayOfWeek =  "*";
                    break;
                case 3:    // Monthly
                    cronObj.minute = schedObj.min;
                    cronObj.hours = schedObj.hour;
                    cronObj.dayOfMonth = schedObj.day;
                    cronObj.dayOfWeek = "*";
                    break;
                case 4:    // Weekly
                    cronObj.minute = schedObj.min;
                    cronObj.hours = schedObj.hour;
                    cronObj.dayOfMonth = "*";
                    cronObj.dayOfWeek = schedObj.day;
                    break;
            }
            return cronObj;
        };


        //---------------------------------------------------------------------------
        //
        //     Прячет или открывает соответствующие контролы выбора дней часов минут
        //     в соответствии с типом переодичности
        //
        //---------------------------------------------------------------------------

        CronShEdit.prototype.refreshForm = function(schedObj) {

            $('#'+schedObj.id+PERIOD_SEL).val(schedObj.type);
            // $('#'+schedObj.id+TKN_START).show();
            // $('#'+schedObj.id+TKN_DAY).show();

            switch (parseInt(schedObj.type)) {

                case 0:    // Disable
                    $('#'+schedObj.id+TKN_START).hide();
                    $('#'+schedObj.id+TKN_DAY).hide();
                    $('#'+schedObj.id+TKN_HOUR).hide();
                    $('#'+schedObj.id+TKN_MIN).hide();
                    $('#'+schedObj.id+DAYWEEK_SEL).hide();
                    $('#'+schedObj.id+DAY_SEL).hide();
                    $('#'+schedObj.id+HOUR_SEL).hide();
                    $('#'+schedObj.id+MIN_SEL).hide();
                    break;
                case 1:    // Hourly
                    $('#'+schedObj.id+TKN_START).show();
                    $('#'+schedObj.id+TKN_DAY).hide();
                    $('#'+schedObj.id+TKN_HOUR).hide();
                    $('#'+schedObj.id+TKN_MIN).show();
                    $('#'+schedObj.id+DAYWEEK_SEL).hide();
                    $('#'+schedObj.id+DAY_SEL).hide();
                    $('#'+schedObj.id+HOUR_SEL).hide();
                    $('#'+schedObj.id+MIN_SEL).show().val(schedObj.min);
                    break;
                case 2:    // Daily
                    $('#'+schedObj.id+TKN_START).show();
                    $('#'+schedObj.id+TKN_DAY).hide();
                    $('#'+schedObj.id+TKN_HOUR).show();
                    $('#'+schedObj.id+TKN_MIN).show();
                    $('#'+schedObj.id+DAYWEEK_SEL).hide();
                    $('#'+schedObj.id+DAY_SEL).hide();
                    $('#'+schedObj.id+HOUR_SEL).show().val(schedObj.hour);
                    $('#'+schedObj.id+MIN_SEL).show().val(schedObj.min);
                    break;
                case 3:    // Monthly
                    $('#'+schedObj.id+TKN_START).show();
                    $('#'+schedObj.id+TKN_DAY).show();
                    $('#'+schedObj.id+TKN_HOUR).show();
                    $('#'+schedObj.id+TKN_MIN).show();
                    $('#'+schedObj.id+DAYWEEK_SEL).hide();
                    $('#'+schedObj.id+DAY_SEL).show().val(schedObj.day);
                    $('#'+schedObj.id+HOUR_SEL).show().val(schedObj.hour);
                    $('#'+schedObj.id+MIN_SEL).show().val(schedObj.min);
                    break;
                case 4:    // Weekly
                    $('#'+schedObj.id+TKN_START).show();
                    $('#'+schedObj.id+TKN_DAY).show();
                    $('#'+schedObj.id+TKN_HOUR).show();
                    $('#'+schedObj.id+TKN_MIN).show();
                    $('#'+schedObj.id+DAYWEEK_SEL).show().val(schedObj.dayWeek);
                    $('#'+schedObj.id+DAY_SEL).hide().hide();
                    $('#'+schedObj.id+HOUR_SEL).show().val(schedObj.hour);
                    $('#'+schedObj.id+MIN_SEL).show().val(schedObj.min);
                    break;
            }
        };

        //---------------------------------------------------------------------------
        //
        //     Сщхраняет значение формы в schedObj  и возвращает его
        //
        //---------------------------------------------------------------------------

        CronShEdit.prototype.readForm = function(schedObj) {

            schedObj.type = $('#'+schedObj.id+PERIOD_SEL).val();
            schedObj.dayWeek = $('#'+schedObj.id+DAYWEEK_SEL).val();
            schedObj.day = $('#'+schedObj.id+DAY_SEL).val();
            schedObj.hour = $('#'+schedObj.id+HOUR_SEL).val();
            schedObj.min = $('#'+schedObj.id+MIN_SEL).val();

            return schedObj;

        };

    return CronShEdit;
});