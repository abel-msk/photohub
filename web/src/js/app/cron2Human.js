/**
 * Created by abel on 17/05/2017.
 */


define(["logger","moment"],
    function(logger,moment) {
        "use strict";


        var EVERY = "Every";
        var MINUTES = "min";
        var HOUR = "hour";
        var IN = "in";
        var DAY = "";  // число
        var MONTH = "month";  // месяца
        var AT = "at";

        var EACH_DAY = "each day";
        var EACH_MONTH = "each month";
        var EACH = "each";
        var OF = "of";
        var AND = "and";

        /*
         cronObj.seconds
         cronObj.minute
         cronObj.hour
         cronObj.dayOfMonth +
         cronObj.month
         cronObj.dayOfWeek;
        */

        var cronObjKey = ["seconds","minute","hour","dayOfMonth","month","dayOfWeek"];


        //---------------------------------------------------------------------------
        //
        //      Проверяем на попадание в границы допустимого значения  для  каждой позиции
        //
        // minute        0-59
        // hour          0-23
        // day of month  1-31
        // month         1-12 (or names, see below)
        // day of week   0-7 (0 or 7 is Sun, or use names)
        //
        function cronValidate(value,key) {

            if ( value.search('[^0-9]+') != -1 ) return false;

            switch (key) {
                case cronObjKey[0]:
                    if ((parseInt(value) < 0) || (parseInt(value) > 59))  return false;
                    break;
                case cronObjKey[1]:
                    if ((parseInt(value) < 0) || (parseInt(value) > 59))  return false;
                    break;
                case cronObjKey[2]:
                    if ((parseInt(value) < 0) || (parseInt(value) > 23))  return false;
                    break;
                case cronObjKey[3]:
                    if ((parseInt(value) < 1) || (parseInt(value) > 31))  return false;
                    break;
                case cronObjKey[4]:
                    if ((parseInt(value) < 1) || (parseInt(value) > 12))  return false;
                    break;
                case cronObjKey[5]:
                    if ((parseInt(value) < 0) || (parseInt(value) > 7))  return false;
                    break;
            }
            return true;
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
        //     CLASS Cron2Human - Переводит Cron строку в читабельный формат.
        //
        //---------------------------------------------------------------------------

        function Cron2Human(cronObj) {
            this.humanStr  = "";
        }

        Cron2Human.prototype.toString = function (cronObj) {

            var timeStr = EVERY;
            var dateStr = IN;
            var emptyTime  =  true;
            var emptyDate  =  true;
            var emptyMonth  =  true;
            var emptyDWeek  =  true;

            //   HOUR

            if ( cronObj.hour != "*")  {
                timeStr += " " +  cronObj.hour ;
                emptyTime = false;
            }
            else {
                //timeStr += " " +  AT;
            }

            timeStr += " " + HOUR;

            //   MIN

            if ( cronObj.minute != "*")  {
                timeStr += " " +  this.normAnd(cronObj.minute);
                emptyTime = false;
            }
            else {
                timeStr +=  " 00";
            }

            timeStr += " " + MINUTES;



            //  DATE

            if ( cronObj.dayOfMonth != "*")  {
                dateStr += " " + cronObj.dayOfMonth + " " + DAY;
                emptyDate = false;
            }
            else {
                dateStr +=  " "+ EACH_DAY;
            }

            //  MONTH

            if ( cronObj.month != "*")  {
                dateStr += " " +OF+ " "+ this.normMonth(cronObj.month);
                emptyMonth = false;
            }
            else {
                dateStr += " "+ OF + " " + EACH_MONTH;
            }

            //  DAY OF WEEK

            if ( cronObj.dayOfWeek != "*")  {
                if  ((!emptyDate) || (!emptyMonth)) {
                    dateStr += " " + AND;
                }
                else {
                    dateStr = "";
                }

                dateStr += " " +IN+ " "+ EACH+ " " + this.normDayOfWeek(cronObj.dayOfWeek);
                emptyDWeek = false;
            }

            if  ((!emptyDate) || (!emptyMonth)  || (!emptyDWeek)) {
                timeStr += ", " +  dateStr;
            }
            else {
                timeStr += ", "+OF + " "  + EACH_DAY;
            }


            if (  emptyTime && emptyDate && emptyMonth && emptyDWeek ) {
                timeStr = "is empty";
            }

            return timeStr;
        };


        Cron2Human.prototype.normAnd = function (inputStr) {
            var retStr  = "";
            if (inputStr.search(',') != -1) {
                var ar = inputStr.split(',');
                for (var i = 0; i < ar.length; i++) {
                    retStr += ar[i] + " " + ((i != ar.length-1)? ("," + " "):" ");
                }
            }
            else {
                return inputStr;
            }
            return retStr
        };

        Cron2Human.prototype.normMonth = function(inputStr) {
            var retStr  = "";
            if (inputStr.search(',') != -1) {
                var ar = inputStr.split(',');
                for (var i = 0; i < ar.length; i++) {
                    retStr += moment.monthsShort(parseInt(ar[i] -1)) + ((i != ar.length-1)? (", "):" ");
                }
            }
            else retStr = moment.monthsShort(parseInt(inputStr -1));
            return retStr
        };


        Cron2Human.prototype.normDayOfWeek = function (inputStr) {
            var retStr  = "";
            if (inputStr.search(',') != -1) {
                var ar = inputStr.split(',');
                for (var i = 0; i < ar.length; i++) {
                    retStr += moment.weekdaysShort(parseInt(ar[i])) + ((i != ar.length-1)? (", "):" ");
                }
            }
            else retStr = moment.weekdaysShort(parseInt(inputStr));
            return retStr
        };

        //---------------------------------------------------------------------------
        //
        //     Проверяем правильность задания значения cron
        //
        //---------------------------------------------------------------------------
        Cron2Human.prototype.isEmpty = function(strValue) {
            return (! strValue) || strValue === "*";
        };

        //---------------------------------------------------------------------------
        //
        //     Проверяем правильность задания значения cron
        //
        //---------------------------------------------------------------------------
        Cron2Human.prototype.validate =  function(cronObj) {

            var message = "";
            try {

                //  Проверка  Часы и минуты
                var timeEmpty = false;
                for (var i = 0; i < 3; i++) {
                    var key = cronObjKey[i];
                    if (this.isEmpty(cronObj[key])) {
                        timeEmpty = true;
                    }
                    else if ( timeEmpty ) {
                        throw new Error("Found undefined value before " + key + " position");
                    }
                    else
                        cronSplit(cronObj[key],key);
                }

                //  Проверка день и месяц

                // else if (timeEmpty) {
                //     throw new Error("Found undefined value before " + key + " or defining date w/o time");
                // }
                // else if (dateEmpty || timeEmpty) {
                //     throw new Error("Found undefined value before " + key + " or defining date w/o time");
                // }

                var dateEmpty = true;
                for (i = 3; i < 5; i++) {
                    key = cronObjKey[i];
                    if ( ! this.isEmpty(cronObj[key])) {
                        if (timeEmpty) {
                            throw new Error("Found undefined value before " + key + " or defining date w/o time");
                        }
                        cronSplit(cronObj[key], key);
                        dateEmpty = false;
                    }
                }

                //  Проверка день недели
                key = cronObjKey[5];
                var dwEmpty = this.isEmpty(cronObj[key]);

                if (timeEmpty && ((!dwEmpty)||(!dateEmpty))) {
                    throw new Error("Found undefined value before " + key + " or defining date w/o time.");
                }
                else if ((dateEmpty == dwEmpty) && (!dwEmpty)) {
                    throw new Error("Found defined date and day of fweek.");
                }
                else if (cronObj[key] !== '*') {
                    cronSplit(cronObj[key],key);
                }




            } catch (e) {
                logger.debug("[CronEdit.validate]  Cron validation error +", e);
                return e.message;
            }
            return message;
        };


return Cron2Human;
});