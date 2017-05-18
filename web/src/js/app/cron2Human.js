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


return Cron2Human;
});