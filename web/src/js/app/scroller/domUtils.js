/**
 * Created by abel on 31.12.15.
 */

define(['logger'], function(logger) {

    var DomUtilsClass  =  {
        //------------------------------------------------------------------------
        //   Calculate margin height
        //------------------------------------------------------------------------
        'getMargin': function(element, position) {

            var margin = 0;

            if ( ! element ) return 0;

         // IE

            if ((document.all) && (position === "top")) {
                return parseInt(element.currentStyle.marginTop, 10);

            } else if ((document.all) && (position === "bot")) {
                return parseInt(element.currentStyle.marginBottom, 10);

            } else if ((document.all) && (position === "left")) {
                return parseInt(element.currentStyle.marginLeft, 10);

            } else if ((document.all) && (position === "right")) {
                return parseInt(element.currentStyle.marginRight, 10);

        // Mozilla

            } else if (position === "top") {
                //var computedStyle = getComputedStyle(element);
                //return parseInt(computedStyle.marginTop);
                margin = document.defaultView.getComputedStyle(element, '').getPropertyValue('margin-top');
                //margin = parseInt(document.defaultView.getComputedStyle(element, '').getPropertyValue('margin-top'));

            } else if (position === "left") {
                margin = document.defaultView.getComputedStyle(element, '').getPropertyValue('margin-left');
                //return parseInt(m?m:0);

            } else if (position === "bot"){
                //elmHeight = (document.defaultView.getComputedStyle(element, '').getPropertyValue('height')).match(/\d+/)[0];
                margin =  document.defaultView.getComputedStyle(element, '').getPropertyValue('margin-bottom');
                //return parseInt(m?m:0);
            }
             else if (position === "right"){
                margin = document.defaultView.getComputedStyle(element, '').getPropertyValue('margin-right');
            }
            return parseInt(margin?margin:0);
        },

        //------------------------------------------------------------------------
        //   GetTop position
        //   Y-coordinate, relative to the viewport origin, of the top of the rectangle box
        //      minus  margin-top height.
        //
        //   getBoundingClientRect -
        //      Координаты относительно окна не учитывают прокрутку,
        //      они высчитываются от границ текущей видимой области.
        //------------------------------------------------------------------------
        'getTop': function(element,noBorder) {
            var topPos = element.getBoundingClientRect().top - this.getMargin(element, "top");
            //logger.debug("[DomUtil.getTop] Element top "+topPos + " Element,",element );
            return parseInt(topPos);
        },
        //------------------------------------------------------------------------
        //   GetLeft position
        //   X-coordinate, relative to the viewport origin, of the top of the rectangle box
        //      minus  margin-top height.
        //
        //   getBoundingClientRect -
        //      Координаты относительно окна не учитывают прокрутку,
        //      они высчитываются от границ текущей видимой области.
        //------------------------------------------------------------------------
        'getLeft': function(element,noBorder) {
            var leftPos = element.getBoundingClientRect().left - this.getMargin(element, "left");
            return parseInt(leftPos);
        },
        //------------------------------------------------------------------------
        //
        //
        //------------------------------------------------------------------------
        'getBot': function(element,noBorder) {
            var pos = element.getBoundingClientRect().bottom + this.getMargin(element, "bot");
            return parseInt(pos);
        },
        //------------------------------------------------------------------------
        //
        //
        //------------------------------------------------------------------------
        'getRight': function(element,noBorder) {
            var pos = element.getBoundingClientRect().right + this.getMargin(element, "right");
            return parseInt(pos);
        },
        //------------------------------------------------------------------------
        //   Get rendered html height
        //------------------------------------------------------------------------
        'getHeight': function(element) {
            var height =
                    this.getMargin(element, "top") +
                    parseInt(element.style.borderTopWidth?element.style.borderTopWidth:0) +
                    element.scrollHeight +
                    parseInt(element.style.borderBottomWidth?element.style.borderBottomWidth:0) +
                    this.getMargin(element, "bot")
                ;
            return parseInt(height);
        },

        //------------------------------------------------------------------------
        //   Get rendered html height
        //------------------------------------------------------------------------
        'getWidth': function(element) {
            var height =
                    this.getMargin(element, "left") +
                    parseInt(element.style.borderLeftWidth?element.style.borderLeftWidth:0) +
                    element.scrollWidth +
                    parseInt(element.style.borderRightWidth?element.style.borderRightWidth:0) +
                    this.getMargin(element, "right")
                ;
            return parseInt(height);
        },

        //------------------------------------------------------------------------
        //   Get height w/o padding
        //------------------------------------------------------------------------
        'getInnerWidth': function(element) {
            var computedStyle = getComputedStyle(element);
            return parseInt(element.clientWidth) -
                parseInt(computedStyle.paddingLeft)  -
                parseInt(computedStyle.paddingRight);
        },

        //------------------------------------------------------------------------
        //   Get height w/o padding
        //------------------------------------------------------------------------
        'getInnerHeight': function(element) {
            var computedStyle = getComputedStyle(element);
            return parseInt(element.clientHeight) -
                parseInt(computedStyle.paddingTop)  -
                parseInt(computedStyle.paddingBottom);
        },


        /***
         * get live runtime value of an element's css style
         *   http://robertnyman.com/2006/04/24/get-the-rendered-style-of-an-element
         *     note: "styleName" is in CSS form (i.e. 'font-size', not 'fontSize').
         ***/
        'getStyle': function (e, styleName) {
            var styleValue = "";
            if(document.defaultView && document.defaultView.getComputedStyle) {
                styleValue = document.defaultView.getComputedStyle(e, "").getPropertyValue(styleName);
            }
            else if(e.currentStyle) {
                styleName = styleName.replace(/\-(\w)/g, function (strMatch, p1) {
                    return p1.toUpperCase();
                });
                styleValue = e.currentStyle[styleName];
            }
            return styleValue;
        }
    };


    return DomUtilsClass;

});
