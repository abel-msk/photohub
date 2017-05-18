/**
 * Created by abel on 07.01.17.
 */
define(["jquery","api","viewImg","logger"], function($,Api,View,logger) {
    "use strict";
    var DEBUG = true;

    var defaultOptions = {
        'loadNext': null,
        'loadPrev': null };

    //-----------------------------------------------------------------------
    //    Класс Загружвет фотографию для просмотра.
    //    Вызывает класс View для отображения фотографии.
    //    Заранее подгружает следующую и предыдущую фотографию в кеш браузера.
    //
    //    options:
    //       'loadNext': функция которая будет вызвана
    //                   если пользлватель нажмет на кнопку перехода к следующей фотографии
    //       'loadPrev': функция которая будет вызвана
    //                   если пользлватель нажмет на кнопку перехода к предыдущей фотографии
    //
    //    При нажатии на кнопку переходя к след или пердыдущей  передает для просмотра
    //    подгруженную фотографию  и снова подгружает новую следующую иои предыдущую.
    //
    //    Для подгрузки фотографии вызывает  loadNext или loadNext и ожидает от
    //    вызывающего класса вызова  append или prepend с данными для подгрузки
    //    новой фотографии
    //
    //-----------------------------------------------------------------------
    function ViewImgLoader(options) {
        this.options = $.extend(true,{},defaultOptions,options || {});
        var caller = this;
        this.view = new View( {
                'loadNext': function() { caller._loadNext.call(caller) },
                'loadPrev': function() { caller._loadPrev.call(caller) }
            });
        this.prev = null;
        this.cur = null;
        this.next = null;
        this.urlPreffix = Api.getActionUrl('imageUrl') + "/";
    }

    //-----------------------------------------------------------------------
    //
    //   Первоначальное открытие и подгрузка фотографии
    //   Параметры:
    //      id - id объекта фотографии от бекэнда
    //      pos  -  позиция фотографии в текущем выводе каталога.
    //              Первая отображаемая фотография = 0
    //      w    -  ширина фотографии в пикселах
    //      h    -  высота фотографии в пикселах
    //
    //   Примечание:  w,h испоользуются для вычисления соотношения сторон.
    //      на основании чего определяется по высоте или ширине
    //      масштабировать фотографию относительно размера окна браузера.
    //      Масштабирование определяется таким образом чтобы вся вотография былла в окне браузера.
    //
    //-----------------------------------------------------------------------
    ViewImgLoader.prototype.open = function (id,pos,w,h) {
        try {
            this.view.hideBtn("next");
            this.view.hideBtn("prev");


            var theUrl = this.urlPreffix + id;
            this.view.openPhoto(theUrl, this.isVert(w,h));

            this.cur = {'pos': pos, 'isVert': this.isVert(w,h), 'id': id, 'url':theUrl };

            if (typeof this.options.loadNext == "function") {
                typeof this.options.loadNext(parseInt(this.cur.pos) + 1);
            }
            if ((typeof this.options.loadPrev == "function") && (parseInt(this.cur.pos) > 0)) {
                typeof this.options.loadPrev(parseInt(this.cur.pos) - 1);
            }
        } catch (e) {
            logger.debug("[ViewImgLoader.open] Error:",e);
        }
    };

    //-----------------------------------------------------------------------
    //
    //   Функция определяет по высоте или ширине  масштабировать фотографию относительно
    //   размера окна браузера. Масштабирование определяется таким образом чтобы вся вотография
    //   былла в окне браузера.
    //   Параметры:
    //
    //      w    -  ширина фотографии в пикселах
    //      h    -  высота фотографии в пикселах
    //
    //   Возвращает:    true если масштабировать надо по высоте
    //                  false  если масштабировать надо по ширине.
    //
    //-----------------------------------------------------------------------

    ViewImgLoader.prototype.isVert = function (w,h) {
        //  Get display aspect.
        var dH = $(window).innerHeight();
        var dW = $(window).innerWidth();
        var displayAspect = parseFloat(dW) / parseFloat(dH);
        var imgAspect = parseFloat(w)/parseFloat(h);
        return (displayAspect>imgAspect?true:false);
    };

    //-----------------------------------------------------------------------
    //
    //    Вызывается при нажатии на кнопку (в зоне просмотра) перейти на след. фотографию.
    //    Переводит предзагруженную следующую фотограюию в сотсояние текущей и открывает ее для просмотра.
    //    Обращяется к вызывающему классу для загрузки новой следующей.
    //    Дополнительно обращается к View для скрытия  кнопки перехода к следующей т.к
    //    если следующая фотография есть и будет передана для предзагрузки кнопка перехода возвращается.
    //
    //-----------------------------------------------------------------------
    ViewImgLoader.prototype._loadNext = function() {
        try {
            if (this.next) {
                this.prev = this.cur;
                if (this.prev) {this.view.showBtn("prev");}
                this.cur = this.next;
                this.view.openPhoto(this.next.url, this.next.isVert);

                if (typeof this.options.loadNext == "function") {
                    typeof this.options.loadNext(parseInt(this.cur.pos) + 1);
                }
            }
            DEBUG && logger.debug("[ViewImgLoader._loadNext] "
                +" Prev id="+this.prev.id+", src="+this.prev.url
                +", Cur id="+this.cur.id+", src="+this.cur.url);

            this.view.hideBtn("next");
        } catch (e) {
            logger.debug("[ViewImgLoader._loadNext] Error:",e);
        }
    };

    //-----------------------------------------------------------------------
    //
    //    Вызывается при нажатии на кнопку (в зоне просмотра) перейти на пред. фотографию.
    //    Переводит предзагруженную предыдущую фотограюию в сотсояние текущей и открывает ее для просмотра.
    //    Обращяется к вызывающему классу для загрузки новой предыдущей.
    //    Дополнительно обращается к View для скрытия  кнопки перехода к предудущей т.к
    //    если предыдущая фотография есть и будет передана для предзагрузки кнопка перехода возвращается.
    //
    //-----------------------------------------------------------------------
    ViewImgLoader.prototype._loadPrev = function() {
        try {
            if (this.prev) {
                this.next = this.cur;
                if (this.next) {this.view.showBtn("next");}
                this.cur = this.prev;
                this.view.openPhoto(this.prev.url, this.prev.isVert);

                if (typeof this.options.loadPrev == "function") {
                    if (parseInt(this.cur.pos) > 0) {
                        typeof this.options.loadPrev(parseInt(this.cur.pos) - 1);
                    }
                    else {
                        this.prev = null;
                    }
                }
            }

            DEBUG && logger.debug("[ViewImgLoader._loadPrev] "
                + "Cur id="+this.cur.id+", src="+this.cur.url
                +", Next id="+this.next.id+", src="+this.next.url);

            this.view.hideBtn("prev");
        } catch (e) {
            logger.debug("[ViewImgLoader._loadPrev] Error:",e);
        }
    };

    //-----------------------------------------------------------------------
    //
    //   Вызывается для добавлния фотографии в качестве новой следующей.
    //   После досбавдления и предзагрузки, возвращает на место кнопку перехода к следующей.
    //   Параметры:
    //      id - id объекта фотографии от бекэнда
    //      pos  -  позиция фотографии в текущем выводе каталога.
    //              Первая отображаемая фотография = 0
    //      w    -  ширина фотографии в пикселах
    //      h    -  высота фотографии в пикселах
    //-----------------------------------------------------------------------
    ViewImgLoader.prototype.append = function(id,pos,w,h) {

        this.view.showBtn("next");
        var img = new Image();
        img.src = this.urlPreffix + id;
        this.next = { 'pos':pos, 'isVert':this.isVert(w,h), 'id':id, 'url': img.src};

        DEBUG && logger.debug("[ViewImgLoader.append] Next image id="+this.next.id
            +", pos="+this.next.pos+", src="+this.next.url);
    };

    //-----------------------------------------------------------------------
    //
    //   Вызывается для добавлния фотографии в качестве новой предыдущей.
    //   После досбавдления и предзагрузки, возвращает на место кнопку перехода к предыдущей.
    //   Параметры:
    //      id - id объекта фотографии от бекэнда
    //      pos  -  позиция фотографии в текущем выводе каталога.
    //              Первая отображаемая фотография = 0
    //      w    -  ширина фотографии в пикселах
    //      h    -  высота фотографии в пикселах
    //-----------------------------------------------------------------------
    ViewImgLoader.prototype.prepend = function(id,pos,w,h) {

        this.view.showBtn("prev");
        var img = new Image();
        img.src = this.urlPreffix + id;
        this.prev = { 'pos':pos, 'isVert':this.isVert(w,h), 'id':id , 'url': img.src};

        DEBUG && logger.debug("[ViewImgLoader.prepend] Prev image id="+this.prev.id
            +", pos="+this.prev.pos+", src="+this.prev.url);

    };

    return ViewImgLoader;
});