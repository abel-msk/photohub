/**
 * Created by abel on 31.10.15.
 */

define(["jquery"],function($){

    var dlg = (function () {
        var defaultOptions = {
            text: "Текст сообщения",
            title: "Сообщение",
            error: false, /* сообщение об ошибке, по умолчанию - false */
            buttons: {},
            type: "default",     //  Error, Small
            size: 'md',          // sm, md, lg
            onClose: function(){ /* обработчик закрытия окна */ }
        };

        var html = function(options) {

            //   Определяем горизонтальные размера окна в терминах  bootstrap
            var frameSize = "col-md-offset-2 col-md-8";
            if (options.size == "sm" ) {
                frameSize = "col-md-offset-3 col-md-6";
            }
            else  if (options.size == "lg" ) {
                frameSize = "col-md-offset-1 col-md-10";
            }

            var htmlStr =
            '<div id="'+options.modalId+'" class="modal fade '+  frameSize +'" role="dialog" tabindex="-1" style="padding-top: 80px;">'+
            '    <div class="modal-dialog" role="document">'+
            '        <div class="modal-content">'+
            '            <div class="modal-header '+options.type+'">'+
            '                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>'+
                             options.title +
            '            </div>'+
            '            <div class="modal-body '+options.type+'">'+
            //'                <div style="padding: 40px 100px 40px 100px;">'+
            //'                <div class="'+options.type+'">'+
            (options.text?options.text:"$nbsp;") +
            //'                </div>'+
            '            </div>'+
            '            <div class="modal-footer '+options.type+'">'+
            '            </div>'+
            '        </div>'+
            '    </div>'+
            '</div>';
            return htmlStr
        };


        function Dialog(options) {
            this.modalId = 'm' + new Date().getTime();
            this.options = $.extend(true,{},defaultOptions,options || {}, {'modalId':this.modalId});
            this.$el = $(html(this.options)).clone().appendTo('body');

            //----------------------------------------
            //   Проходимся по всему масиву кнопок, готовим html представление и
            //   заряжаем обработчик нажатия
            //
            for (var button in options.buttons) {
                var btnClickHandler = options.buttons[button];

                var $btn = $('<button type="button" class="pull-right btn btn-default" data-dismiss="modal" >'+
                        '<span>'+button+'</span></button>')
                    .clone()
                    .prependTo('#'+this.modalId+' .modal-footer');

                //----------------------------------------
                //   Создаем обработчик нажатия на кнопку
                //
                $btn.on('click',{btnClickHandler: btnClickHandler, 'modalId': this.modalId },
                    function(event) {
                        var $el = $(this), result;

                        $el.addClass('disabled');   //  Блокируем кнопку
                        try {
                            if ( event.data.btnClickHandler && ( typeof event.data.btnClickHandler === "function")) {
                                result = event.data.btnClickHandler($el, event.data.modalId);
                            }
                        } catch (e) {
                            //result = debug("[Dialog click handler]", e.message, e);
                            console.log("[Dialog click handler] error: " + e.stack );
                        } finally {
                            $el.removeClass('disabled');  //  Разблокируем кнопку
                        }
                        return result;
                });
            }

            //   Open dialog
            this.$el.modal({'backdrop':true})
                .one('hidden.bs.modal',{'onClose':this},
                function(event) {
                    if ( event.data.onClose &&
                        (typeof event.data.onClose === "function")) {
                        event.data.onClose(this);
                    }
                    $(this).remove();
                });

            return this;
        }

        return Dialog;
    }());



    return {
        open: function(options) {
            return new dlg(options);
        }
    };



});
