/**
 * Created by abel on 07.12.16.
 */

/*-------------------------------------------------------------------------------------------------

 SlotsArray

 Инициализирует масив из Х элементов и обеспучивает добавление в конец или в начало со
 сдвигом всего масива. При этом вытесняя элементы в проивоположном конце масива.

-------------------------------------------------------------------------------------------------*/

define(["scroller/domUtils","logger"],function (DomUtils, logger) {

    "use strict";


    function SlotsArray(iniLen) {
        try {
            this.LastIndex = iniLen-1;
            this.ar = [];
            for (var i=0; i <  iniLen; i++) { this.ar[i] = null; }   // размечаем и очищаем масив слотов
        } catch (e) {
            logger.debug("[SlotsArray.init] Error", e);
        }
    }

    SlotsArray.prototype.length = function() {
        return  this.LastIndex + 1;
    };

    SlotsArray.prototype.append = function(object) {
        try {
            var lostObject = this.ar.shift();
            this.ar[this.LastIndex] = object;
            return lostObject;

        } catch (e) {
            logger.debug("[SlotsArray.append] Error", e);
        }
    };

    SlotsArray.prototype.prepend = function(object) {
        try {
            var lostObject = this.ar.pop();
            this.ar.unshift(object);
            //this.ar[0] = object;
            return lostObject;
        } catch (e) {
            logger.debug("[SlotsArray.prepend] Error", e);
        }
    };


    SlotsArray.prototype.getByPos = function (pos) {
        try {
            return this.ar[pos];
        } catch (e) {
            logger.debug("[SlotsArray.getObjById] Error", e);
        }
    };

    SlotsArray.prototype.getLast = function () {
        return  this.ar[this.LastIndex];
    };

    SlotsArray.prototype.getFirst = function () {
        return  this.ar[0];
    };

    SlotsArray.prototype.getArray = function () {
        return  this.ar;
    };


    return SlotsArray;
});