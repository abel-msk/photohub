/**
 * Created by abel on 31.12.16.
 */

define(["jquery","logger","const"],function($, logger, Const) {
    "use strict";


    var IMAGE_FRAME = "img-view-frame";
    var MAIN_FRAME = "view-panel";
    var BACK_BTN = "close-view-pane";
    var PREV_BTN = "viewPrev";
    var NEXT_BTN = "viewNext";

    var CONTAINER = "media-container";
    var IMAGE_FRAME = "img-view-frame";
    var VIDEO_FRAME = "video-view-frame";


    var ZIN_BTN = "z-in";
    var ZOUT_BTN = "z-out";


    var defaultOptions = {
        'loadNext': null,
        'loadPrev': null
    };

    //-----------------------------------------------------------------------
    //    Return first part (base type) of media mime type.
    //-----------------------------------------------------------------------

    function _getMimeTypeBase (mimetype) {
        return mimetype.substring(0,mimetype.indexOf("/"));
    };

    //-----------------------------------------------------------------------
    //
    //    Инициализация фрейма просмотра фотографий.
    //    Заряжаются обрабтчики пунктов меню.
    //    options {
    //           'loadNext': функция которая будет вызвана
    //                       если пользлватель нажмет на кнопку перехода к следующей фотографии
    //           'loadPrev': функция которая будет вызвана
    //                       если пользлватель нажмет на кнопку перехода к предыдущей фотографии
    //
    //-----------------------------------------------------------------------
    function View(options) {
        this.frame = document.getElementById(MAIN_FRAME);
        var imageFrame = document.getElementById(IMAGE_FRAME);
        var videoFrame = document.getElementById(VIDEO_FRAME);
        this.state = "close";
        this.zoomBy = "height";

        this.options = $.extend(true, {}, defaultOptions, options || {});


        $("#" + BACK_BTN).on("click", {'caller': this}, function (event) {
            event.data.caller.close();
        });

        $("#" + ZIN_BTN).on("click", {'caller': this}, function (event) {
            event.data.caller.zoomIn();
        });

        $("#" + ZOUT_BTN).on("click", {'caller': this}, function (event) {
            event.data.caller.zoomOut();
        });

        $("#viewPrev").on("click", {'caller': this}, function (event) {
            event.data.caller.loadPrev();
        });

        $("#viewNext").on("click", {'caller': this}, function (event) {
            event.data.caller.loadNext();
        });

        $(this.frame).on("mousemove", {'caller': this}, function (event) {
            event.data.caller.onMouseMove();
        });

        $("#fscreen").on("click", {'caller': this}, function (event) {
            event.data.caller.screenToggle();
        });

        $("#media-container").off("click").on("click", {'caller': this}, function (event) {
            event.data.caller.playToggle();
        });



    }

    //-----------------------------------------------------------------------
    //
    //    Загружает фотографию в зону просмотра. И открывает просмотр фтографии.
    //    Параметр:
    //        URL: url загрузки фотографии
    //        vertOrient: если true  то размер фотографии выравнивается таким оьразом,
    //                    что-бы полностью влезьть по вертикаои в текущую зону просмотра.
    //                    false фотография масштабирется что-бы влезть по ширине.
    //
    //      //TODO:  Добавить MimeType в параметры
    //     {
    //         id : id объекта фотографии от бекэнда
    //         pos : позиция фотографии в текущем выводе каталога.
    //         width: ширина фотографии в пикселах
    //         height: высота фотографии в пикселах
    //         mimetype:
    //         isVert
    //         url
    //     }
    //
    //
    //      Tag video description: https://www.w3schools.com/tags/ref_av_dom.asp
    //-----------------------------------------------------------------------

    View.prototype.openPhoto = function(item) {
        try {

            this.item = item;


            if ( _getMimeTypeBase(item.mimeType) ===  "video") {
                $("#"+IMAGE_FRAME).hide();
                $("#"+VIDEO_FRAME).show();

                this.img = document.getElementById(VIDEO_FRAME);
                this.img.src = item.url + "?type="+Const.MEDIA_VIDEO;
                this.img.load();
                this.img.poster = item.url;
                this.img.classList.add("full-height");
                this.img.classList.add("full-width");
                this.img.style.height = "100%";
                this.img.style.width = "100%";

                // Set Play pause handler

            }
            else {
                $("#"+VIDEO_FRAME).hide();
                $("#"+IMAGE_FRAME).show();
                this.img = document.getElementById(IMAGE_FRAME);
                this.img.src = item.url;

                if (item.isVert) {
                    this.img.classList.add("full-height");
                    this.img.classList.remove("full-width");
                    this.img.style.height = "100%";
                    this.zoomBy = "height";
                } else {
                    this.img.classList.add("full-width");
                    this.img.classList.remove("full-height");
                    this.img.style.width = "100%";
                    this.zoomBy = "width";
                }
            }


            this.open();

        } catch (e) {
            logger.debug("[View.viewPhoto] Incorrect object parameter.", e);
        }
    };


    //-----------------------------------------------------------------------
    //
    //     Открывает зону просмотра. Зона просмотра открывается во все окно браузера.
    //     Данные по фотографии принимаются в виде стандартного объекта полученного от бекофиса.
    //
    //-----------------------------------------------------------------------
    View.prototype.open = function() {
        if (this.state === "close") {
            this.frame.style.width = "100%";
            this.frame.style.height = "100%";
            this.frame.style.opacity = "1";
            this.state = "open";
        }
        this.onMouseMove();
    };
    //-----------------------------------------------------------------------
    //
    //     Сварачивает зану просмотра и очищает даные.
    //     Если просмотр был в полноэкранном режиме то переврит браузер в обычный режим
    //
    //-----------------------------------------------------------------------
    View.prototype.close = function() {
        if ( _getMimeTypeBase(this.item.mimeType) ===  "video") {
            this.img.pause()
        }

        this.frame.style.opacity = "0";
        this.frame.style.width = "0";
        this.frame.style.height = "0";
        this.img.src = "";

        this.screenToggle(true);
        this.state = "close";
    };

    //-----------------------------------------------------------------------
    //
    //    Увеличение масштаба изображения (ищбражение  выхоит за границы зоны просмотра.
    //    Появляетсся возможность скрола.
    //
    //-----------------------------------------------------------------------
    View.prototype.zoomIn = function() {
        if (this.zoomBy==="width") {
            this.img.style.width = (parseInt(this.img.style.width) + 50) + "%";
        }
        else {
            this.img.style.height = (parseInt(this.img.style.height) + 50) + "%";
        }
    };

    //-----------------------------------------------------------------------
    //
    //    Уменьшение масштаба
    //    Минимальное уменьшение по размеру окна браузера ( по ширина или по высоте)
    //    в зависимости от указанного при открытии.
    //
    //-----------------------------------------------------------------------
    View.prototype.zoomOut = function() {
        if (this.zoomBy==="width") {
            if (parseInt(this.img.style.width) > 100 ) {
                this.img.style.width = (parseInt(this.img.style.width) - 50) + "%";
            }
        }
        else {
            if (parseInt(this.img.style.height) > 100 ) {
                this.img.style.height = (parseInt(this.img.style.height) - 50) + "%";
            }
        }
    };

    //-----------------------------------------------------------------------
    //
    //    Обработка нажатий  на кнопки Next и Prev
    //
    //-----------------------------------------------------------------------
    View.prototype.loadNext = function() {
        if (typeof this.options.loadNext == "function") {
            this.options.loadNext();
        }
    };

    View.prototype.loadPrev = function() {
        if (typeof this.options.loadPrev == "function") {
            this.options.loadPrev();
        }
    };

    //-----------------------------------------------------------------------
    //
    //    Обработка движения мышкой
    //
    //-----------------------------------------------------------------------
    View.prototype.onMouseMove = function() {
        var caller = this;
        this.chOpacity(1);

        if (this.hide) clearTimeout(this.hide);

        this.hide =  setTimeout(function () {
            caller.chOpacity.call(caller,0);
            }, (1000 * 3));
    };

    //-----------------------------------------------------------------------
    //      Показать или закрыть меню и кнопки перехода след. предыдущ.
    //-----------------------------------------------------------------------
    View.prototype.chOpacity = function(visible) {
        this.opacityList = document.getElementsByClassName("opacity");
        for (var i = 0 ; i < this.opacityList.length; i++) {
            this.opacityList[i].style.opacity = visible;
        }
    };



    View.prototype.playToggle = function() {
      if ( this.img.paused || this.img.ended ) {
          this.img.play()
      }
      else {
          this.img.pause()
      }
    };




    //-----------------------------------------------------------------------
    //     Раскрывает зону просмотра (окно браузера) во весь экран.
    //     Переключение полноэкранного отображения
    //-----------------------------------------------------------------------
    View.prototype.screenToggle = function(isOff) {

        var btnTag = document.getElementById("fscreen");

        if ( document.fullscreenEnabled ||
            document.mozFullscreenEnabled ||
            document.webkitFullscreenEnabled ) {

            var fullscreenElement =
                document.fullscreenElement ||
                document.mozFullscreenElement ||
                document.webkitFullscreenElement;

            //
            //   Переключаем в полноэкранный режим
            //
            if (!fullscreenElement && (! isOff)) {

                if (this.frame.requestFullScreen) {
                    this.frame.requestFullScreen();
                } else if (this.frame.mozRequestFullScreen) {
                    this.frame.mozRequestFullScreen();
                } else if (this.frame.webkitRequestFullScreen) {
                    this.frame.webkitRequestFullScreen();
                }
                btnTag.innerHTML = '<i class="fa fa-compress big-size" aria-hidden="true"></i>';
            }

            //
            //   Переключаем в обычный режим
            //
            else {
                logger.debug("[View.screenToggle] Element", fullscreenElement);
                //   Fullscreen OFF
                if (document.cancelFullScreen) {
                    document.cancelFullScreen();
                } else if (document.mozCancelFullScreen) {
                    document.mozCancelFullScreen();
                } else if (document.webkitCancelFullScreen) {
                    document.webkitCancelFullScreen();
                }
                btnTag.innerHTML = '<i class="fa fa-arrows-alt big-size" aria-hidden="true"></i>';
            }
        }
    };

    //-----------------------------------------------------------------------
    //   Делает видимой кнопку перехода к следующей или предыдущей фотографии
    //-----------------------------------------------------------------------
    View.prototype.showBtn = function(btnType) {
        if (!btnType) return;
        if ( btnType === "next")  {
            $("#"+NEXT_BTN).show();
        }
        else if ( btnType === "prev")  {
            $("#"+PREV_BTN).show();
        }
    };
    //-----------------------------------------------------------------------
    //   Скрывает кнопку перехода к следующей или предыдущей фотографии
    //-----------------------------------------------------------------------
    View.prototype.hideBtn = function(btnType) {
        if (!btnType) return;
        if ( btnType === "next")  {
            $("#"+NEXT_BTN).hide();
        }
        else if ( btnType === "prev")  {
            $("#"+PREV_BTN).hide();
        }
    };



    return View;

});