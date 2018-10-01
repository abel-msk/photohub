/**
 * Created by abel on 30/09/2018.
 */

define(["jquery"],  function ($) {
    "use strict";


    function getHTML(id) {
        var htmlStr =  '<div id="'+id+'" style=" z-index: 2500; position: fixed; top: 0; bottom: 0; left: 0; right: 0; ">'+
            '<div  class="text-center" style="  margin-top: 2px; font-size: 40px; color: rgba(0,0,0,0.5);" >'+
            '<i class="fa fa-refresh fa-pulse fa-3x fa-fw margin-bottom"></i>' +
            '<span class="sr-only">Loading...</span>'+
            '</div> </div>';
        return htmlStr;
    }


    function Spinner() {

        console.log("Spinner start");

        this.frameId =  "spinner_" + Math.floor((Math.random() * 10000));
        this.open();
    }

    //   Remove spinner from screen and remove event listener
    Spinner.prototype.open = function() {
        $(window).on("resize", {'caller': this },this._onResize)
        $("body").append($(getHTML(this.frameId)));
        $(document).on("keydown",{'caller': this },this._abort);
        this._onResize();

        //console.log("Spinner open. " + getHTML(this.frameId));
    };

    //   Remove spinner from screen and remove event listener
    Spinner.prototype.close = function() {
        $("#"+this.frameId).remove();
        $(window).off("resize",this._onResize);
        $(document).off("keydown",this._abort);
    };

    //  Move spiner icon to middle findow on resize
    Spinner.prototype._onResize = function(event) {
        var caller = this;
        if (event) {
            caller = event.data.caller;
        }
        var topPos = ($(window).height() / 2) - 80;
        $("#"+caller.frameId + " div").css("margin-top",topPos+"px");
    };


    //  Break spinner
    Spinner.prototype._abort = function(event) {
        var caller = this;
        if (event) {
            caller = event.data.caller;
        }
        // You may replace `c` with whatever key you want
        if ((event.metaKey || event.ctrlKey) && ( String.fromCharCode(event.which).toLowerCase() === 'c')) {
            //console.log("You pressed CTRL + C");
            caller.close();
        }
    };


    return Spinner;
});


