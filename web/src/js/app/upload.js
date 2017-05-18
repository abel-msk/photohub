/**
 * Created by abel on 26.10.15.
 */



/*

init options
    {
         'dropZoneFrame':DROP_ZONE_FRAME,
         'dialogId': 'upload-panel',
         'onOpen':null,
         'onCLose': function(){menu.closeMenu(MENU_BASE);},
     }


 */
define(["jquery","dropzone","logger","api","modalDialog","sitesCollection"],
    function ($,Dropzone,logger,api,dialog,SitesCollection) {
        "use strict";

        var UPLOAD_SILES_LIST = "site-upload-list";
        var DROP_ZONE_FRAME = "#drop-zone";



        var defaultOptions = {
            'dropZoneFrame': DROP_ZONE_FRAME,
            'siteSelector': null,
            'dialogId' : null,
            'onOpen':null,
            'onClose':null
        };

        //-----------------------------------------------------------------------
        //
        //    Инициализация
        //    options:
        //     {
        //         'dropZoneFrame':DROP_ZONE_FRAME,
        //         'dialogId': 'upload-panel',
        //         'onOpen':null,
        //         'onCLose': function(){menu.closeMenu(MENU_BASE);},
        //     }
        //-----------------------------------------------------------------------
        function Uploader(options) {
            this.options = $.extend(true,{},defaultOptions,options || {});
            var caller = this;

            //
            //   Подготавливаем  Dropzone  для загрузки файлов
            //
            this.myDropzone = new Dropzone(this.options.dropZoneFrame, { // Make the whole body a dropzone
                url: this.getUrl , // Set the url
                thumbnailWidth: 80,
                thumbnailHeight: 80,
                parallelUploads: 3,
                previewTemplate: this.getHTML(),
                autoQueue: true, // Make sure the files aren't queued until manually added
                previewsContainer: "#previews",  // Define the container to display the previews
                clickable: ['.fileinput-button',"div#drop-zone"]
            });

            //
            //   Загружаем  список сайтов и вставляем его в DropDown Menu
            //
            this.sitesList = new SitesCollection();
            this.sitesList.getCollection().on('reset', function (loadedCollection) {
                logger.debug("[sites] Collection reset event fired.");
                var hasDefSite = "";
                $("#"+UPLOAD_SILES_LIST).html("");

                loadedCollection.each(function (modelItem) {
                    //   Fillup  sites list for upload selector. Set default at first local site.
                    if ( (modelItem.get('connectorType') == 'local' ) && ( ! hasDefSite )) {
                        hasDefSite = modelItem.get('id');
                        $("#"+UPLOAD_SILES_LIST).append('<option selected="selected" value="'+hasDefSite+'">'+modelItem.get('name')+'</option>')
                    }
                    else {
                        $("#"+UPLOAD_SILES_LIST).append('<option value="'+modelItem.get('id')+'">'+modelItem.get('name')+'</option>')

                    }
                }, this);
            }, this);


            //
            //   Регестрируем обработчики событий от загрузчика файлов
            //
            this.myDropzone.on("sending", function(file,xhr,formData) {
                xhr.withCredentials = true;
            })
            .on("addedfile", function(file) {
                $(caller.options.dropZoneFrame).hide();
                $("#previews").show();
                //// Hookup the start button
                //file.previewElement.querySelector(".start").onclick = function() { myDropzone.enqueueFile(file); };
            })
            .on("reset",function() {
                $(caller.options.dropZoneFrame).show();
                $("#previews").hide();
            })
            .on("complete", function(file) {
                logger.debug("[myDropzone.complete] Got event. complete for ", file);

                //  Если файл не удалось перелать.  Открываем диалог и помечаем файл красным.
                //  В противном случае помечаем файл красным.
                if ( file.status == "error" ) {
                    //previewElement
                    var response = JSON.parse(file.xhr.responseText);
                    dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
                //   Файл передали без ошибок. Удаляем из списка.
                else {
                    caller.myDropzone.removeFile(file);
                }
                //TODO: Проверить если список файлов пуст то закрыть форму.
            });


            //
            //   Регистрируем обработчики открытия и закрытия основного окна загрузчика.
            //
            if (this.options.dialogId) {
                $("#"+this.options.dialogId)
                    .on('show.bs.modal', function () { caller.enable(); })
                    .on('hide.bs.modal', function () { caller.disable(); });
            }
        }



        //-----------------------------------------------------------------------
        //
        //     Устанавливает занчение пути запроса в зависимости от выбранного
        //     в диалоге сайта
        //
        //-----------------------------------------------------------------------
        Uploader.prototype.getUrl = function(fileObjects) {
            logger.debug("[upload] GET_URL. for ", fileObjects);
            return api.getActionUrl('siteUpload')+$("#"+UPLOAD_SILES_LIST).val()+"/upload";
        };

        Uploader.prototype.enable = function() {
            this.sitesList.load();
            this.myDropzone.enable();
            if ( typeof this.options.onOpen === "function" ) {
                this.options.onOpen();
            }
        };


        Uploader.prototype.disable = function() {
            this.myDropzone.removeAllFiles(true);
            this.myDropzone.disable();
            if ( typeof this.options.onClose === "function") {
                this.options.onClose();
            }
        };
            //-----------------------------------------------------------------------

        Uploader.prototype.getHTML =  function() {
            var templateHTML =
                '<div id="template" class="file-row">'+
                '	<table class="table">'+
                '		<tbody>'+
                '			<tr><td rowspan="2"  style="width: 80px">'+
                '					<span class="preview">'+
                '						<img class="img-thumbnail" data-dz-thumbnail/>'+
                '					</span>'+
                '				</td>'+
                '				<td>'+
                '					<span class="name" data-dz-name></span>'+
                '					<span class="size" data-dz-size></span>'+
                '				</td>'+
                '				<td class="col-sm-3">'+
                '					<div data-dz-remove class="btn delete btn-xs pull-right">'+
                '						<i class="glyphicon glyphicon-remove"></i>'+
                '					</div>'+
                '				</td>'+
                '			</tr>'+
                '			<tr>'+
                '				<td colspan="3">'+
                '					<div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">'+
                '						<div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>'+
                '					</div>'+
                '				</td>'+
                '			</tr>'+
                '		</tbody>'+
                '	</table>'+
                '</div>';
            return templateHTML;
        };


    return Uploader;
});