/**
 * Created by abel on 12.02.16.
 */
define(["jquery","logger","scroller/domUtils","utils","form/veList","modalDialog","api"],
    function ($,logger,DomUtils,Utils,VEList,Dialog,Api) {
    "use strict";


    var PHOTO_SINGLE = 1;
    var PHOTO_FOLDER = 2;
    var PHOTO_COLLECTION = 3;

    var MEDIA_THUMB = 11;
    var MEDIA_PHOTO = 12;
    var MEDIA_VIDEO = 13;

    var ACCESS_NET = 21;
    var ACCESS_LOCAL = 22;



    var DIALOG_WIDTH = 370;
    var DIALOG_HEIGHT = 490;
    var DIALOG_ROUND_SPACE = 20;

    var DIALOG_HEADER_HEIGHT = 55;
    var DIALOG_FOOTER_HEIGHT = 55;

    var dialogLeft = DIALOG_ROUND_SPACE;
    var dialogTop = DIALOG_ROUND_SPACE;
    var dialogTopOffset = 10;


    var defaultOptions = {
        'dialogId': "#photo_info"
    };

    //var monthStr =  ["Янв","Февр","Март","Апр", "Май", "Июнь", "Июль", "Авг", "Сент", "Окт", "Ноя", "Дек"];
    ////10 Окт 2015  17:37:28
    //function toDateTimeString(timeStamp) {
    //    var createDate = new Date(timeStamp);
    //    var monthDigit = parseInt(createDate.getMonth())+1;
    //
    //    return createDate.getDate()+' '+
    //        //(monthDigit < 10?'0'+monthDigit:monthDigit)+' '+
    //        monthStr[createDate.getMonth()]+' '+
    //        createDate.getFullYear()+" "+
    //        createDate.getHours()+":"+
    //        (createDate.getMinutes() < 10?("0"+createDate.getMinutes()):createDate.getMinutes()) + ":" +
    //        (createDate.getSeconds() < 10?("0"+createDate.getSeconds()):createDate.getSeconds());
    //}

    //------------------------------------------------------------------------
    //
    //    GMAP: Init/Open embedded google map object
    //
    //------------------------------------------------------------------------
    function initGoogleMap(mapElementId,gpsPointObj) {
        //var myLatLng = {'lat':55.6344833, lng: 38.0856972};
        var gps = {'lat':gpsPointObj.lat, 'lng':gpsPointObj.lon};

        // Create a map object and specify the DOM element for display.
        var map = new google.maps.Map(document.getElementById(mapElementId), {
            center: gps,
            scrollwheel: false,
            mapTypeControl: false,
            scaleControl: false,
            zoom: 13
        });

        // Create a marker and set its position.
        var marker = new google.maps.Marker({
            map: map,
            position: gps,
            title: gpsPointObj.title
        });
    }

    //------------------------------------------------------------------------
    //
    //    INIT: object and required event
    //
    //------------------------------------------------------------------------
    function ImageInfoDialog(options) {
        this.options = $.extend(true,{},defaultOptions,options || {});
        var dialogId = this.options.dialogId;
        var caller = this;
        this.editableFields = null;

        if (dialogId.length <= 0) {
            throw Error("[ImageInfoDialog.init] Cannot find dialog html code in DOM.");
        }

        this.element = null;

        //   Обработка событий открытия и закрытя панелей акордиона
        $("body")

            //   OPEN ACCORDION PANE
            .on('show.bs.collapse',dialogId + ' .collapse', function (event) {
                $(dialogId + ' .collapse.in').collapse('hide');
                var selector = this.parentElement.querySelector('a[href="'+'#'+this.id+'"] i');
                selector.classList.add("fa-rotate-90");
            })

            .on('shown.bs.collapse',dialogId + ' .collapse',{'caller':this}, function (event) {
                var caller = event.data.caller;
                if ( this.id == "acc-loc") {
                    if (caller.options.location.lat &&  caller.options.location.lon) {
                        initGoogleMap('map_view', caller.options.location);
                    }
                    else {
                        $('#map_view').empty();
                    }
                }
            })
            //   CLOSE ACCORDION PANE
            .on('hide.bs.collapse',dialogId + ' .collapse', function (event) {
                var selector = this.parentElement.querySelector('a[href="'+'#'+this.id+'"] i');
                selector.classList.remove("fa-rotate-90");
                $(this).find(".accordion-selector i").removeClass("fa-rotate-90");
            })

            .on('hidden.bs.modal',dialogId,function(event) {
                caller.destroy();
            })

            .on('click','#info_edit_btn',function(event) {
                caller.ViewEditInfo(event);
            });
    }

    //------------------------------------------------------------------------
    //
    //   OPEN: Request image data and open/show dialog.
    //
    //------------------------------------------------------------------------
    ImageInfoDialog.prototype.open = function(imageElement,Imageid) {
        if ( (! this.options.dialogId ) || ($(this.options.dialogId).length <= 0)) {
            throw Error("[ImageInfoDialog.open] Cannot find dialog html code in DOM.");
        }

        this.options.location = {'lat' : null, 'lon' : null};
        this.options.element = imageElement;
        var caller = this;

        //    Call Api for get image info
        Api.getPhoto(Imageid,

            //  SUCCESS
            function(response){
                if (response.object) {
                    caller.fill(response.object);
                }
                else {
                    logger.debug("[FilteredList.loadPage] Error. Api success return w/o task object");
                }
            },

            //  ERROR
            function(response){
                $(caller.options.dialogId).modal('hide');
                Dialog.open({
                    'error': true,
                    'title': "Server error",
                    'text': response.message,
                    'buttons': {OK: function(){}}
                });
            });

        this.show(imageElement);
    };


    //------------------------------------------------------------------------
    //
    //   SHOW: Calc wnd position  and show it
    //
    //------------------------------------------------------------------------
    ImageInfoDialog.prototype.show = function(element) {

        var $dialogFrame = $(this.options.dialogId);
        var leftPos = DomUtils.getLeft(element);
        var dialogOnLeft = false;

        //   Если окно влезает справа от картинки
        if (($(window).innerWidth() - (leftPos + DomUtils.getWidth(element))) > (DIALOG_WIDTH + DIALOG_ROUND_SPACE)) {
            dialogLeft = leftPos + DomUtils.getWidth(element);
        }
        //   Если окно влезает слева от картинки
        else if ( leftPos > (DIALOG_WIDTH + DIALOG_ROUND_SPACE) ) {
            dialogLeft = leftPos - DIALOG_WIDTH;
            dialogOnLeft = true;
        }

        //   Если окно влезает ниже верхней граници картинки
        if ( ($(window).innerHeight() - DomUtils.getTop(element) - dialogTopOffset) > (DIALOG_HEIGHT + DIALOG_ROUND_SPACE)) {
            dialogTop = DomUtils.getTop(element);
        }
        //   Если окно влезает по высоте тo прижимаем окно к низу окна браузера
        else if (($(window).innerHeight() - (DIALOG_HEIGHT + DIALOG_ROUND_SPACE) ) > DIALOG_ROUND_SPACE){
            dialogTop =  $(window).innerHeight() - (DIALOG_HEIGHT + DIALOG_ROUND_SPACE);
        }
        //  Если не влезает то сжимаем окно по высоте
        else {
            DIALOG_HEIGHT = parseInt($(window).innerHeight()) - (DIALOG_ROUND_SPACE * 2);
        }

        ////  ????
        //$(element).find('.img-btn').css("opacity", "1");


        //  Передвигаем стрелку-указатель напротив картинки для кторой открываем диалог
        var arrowTop = DomUtils.getTop(element);
        if ( dialogOnLeft ) {
            $dialogFrame.find('#dialog-arrow i')
                .removeClass('fa-rotate-270')
                .addClass('fa-rotate-90');

            $dialogFrame.find('#dialog-arrow').css({
                'left': (dialogLeft + DIALOG_WIDTH - 1) + "px",
                'top': (arrowTop + 22) + "px"
            });
        }
        else {
            $dialogFrame.find('#dialog-arrow i')
                .removeClass('fa-rotate-90')
                .addClass('fa-rotate-270');
            $dialogFrame.find('#dialog-arrow').css({
                'left': (dialogLeft - 30 ) + "px",
                'top': (arrowTop + 22) + "px"
            });
        }

        //  Позиционируем диалог на экране
        $(this.options.dialogId+' .modal-content').css({
        //    'max-height': DIALOG_HEIGHT + "px",
            'width':DIALOG_WIDTH + "px",
            'margin-top' : dialogTop + "px",
            'margin-left': dialogLeft + "px"
        });

        //$(this.options.dialogId+' .modal-body').css({
        //    //'max-height': (DIALOG_HEIGHT - DIALOG_HEADER_HEIGHT - DIALOG_FOOTER_HEIGHT) + "px"
        //    //'max-height': parseInt($(window).innerHeight()) - dialogTop - DIALOG_ROUND_SPACE
        //    'height': parseInt($(window).innerHeight()) - dialogTop - DIALOG_ROUND_SPACE
        //});

        $dialogFrame.find('#image_info_dialog').css({
            'height': parseInt($(window).innerHeight()) - dialogTop - DIALOG_ROUND_SPACE
        });

        //   Всегда открываем первое пано(акордион)
        $('#acc-name').collapse('show');

        $(this.options.dialogId).modal('show');

    };

    //------------------------------------------------------------------------
    //
    //   FILLOUT: dialog data
    //
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //
    // {
    //     aperture: "2.8"
    //     camMake: "Apple"
    //     camModel: "iPhone 4"
    //     createTime: 1401633538000
    //     descr: ""
    //     digitTime: null
    //     dpi: ""
    //     expMode: ""
    //     expTime: "0.06666667"
    //     focalLen: "3.85"
    //     focusDist: ""
    //     gpsAlt: null
    //     gpsDir: 0
    //     gpsLat: 55.6378111
    //     gpsLon: 38.0873638
    //     hidden: false
    //     id: "9639"
    //     isoSpeed: "125"
    //     mediaObjects: [
    //         {
    //             accessType: 21
    //             height: 2592
    //             id: "16635"
    //             mimeType: "image/jpeg"
    //             path: "https://lh3.googleusercontent.com/-NcIdJ35AKQ8/U4t3Bpb3HjI/AAAAAAAAF6U/QWOTTG0afVI/I/IMG_0055.JPG"
    //             size: 0
    //             type: 12
    //             width: 1936
    //         }
    //         {
    //
    //             accessType: 22
    //             height: 400
    //             id: "16636"
    //             mimeType: "image/jpeg"
    //             path: "/Volumes/Dual-B/Users/abel/Developing/photohub-root/photo-thumbnail/39/9639.png"
    //             size: 38822
    //             type: 11
    //             width: 299
    //         }
    //     ]
    //     mediaType: null
    //     name: "IMG_0055.JPG"
    //     onSiteId: "1000000418474851.6020036197097086514"
    //     realUrl: "https://lh3.googleusercontent.com/-NcIdJ35AKQ8/U4t3Bpb3HjI/AAAAAAAAF6U/QWOTTG0afVI/I/IMG_0055.JPG"
    //     siteBean: {
    //         connectorState: "CONNECT"
    //         connectorType: "Google"
    //         id: "1"
    //         lastScanDate: null
    //         name: "Gsite"
    //         properties: [{name: "gPerson", value: "", description: null}]
    //         root: "/Volumes/Dual-B/Users/abel/Developing/photohub-sites"
    //         siteUser: "admin"
    //         type: 1
    //     }
    //     unicId: "6064868bfa9256f20000000000000000"
    //     updateTime: 1453846011975
    // }
    //}
    //------------------------------------------------------------------------
    ImageInfoDialog.prototype.fill = function(ImageObject) {

        var $dlgObj = $(this.options.dialogId);

        //$dlgObj.find("#info_image_name span.row-data").text(ImageObject.name);
        //$dlgObj.find("#info_image_descr span.row-data").text(ImageObject.descr);

        VEList.prototype.rowHTML = function (tagId,name) {
            return '<span class="row-label">'+name+'</span>' +
            '<span id="'+tagId+'"class="row-data">Loading ...</span>'
        };

        this.editableFields = new VEList({
            'element':"#info_image_name span.row-data",
            'btnElement':"#info_edit_btn"
        });

        this.editableFields.push("Name:",ImageObject.name,"#info_image_name_pos");
        this.editableFields.push("Description:",ImageObject.descr,"#info_image_descr");
        this.editableFields.render();

        // this.editableFields.push(new FormObjects.ViewEdit({
        //         'name': 'name',
        //         'parentSelector': '#info_image_name span.row-data',
        //         'value': ImageObject.name
        //     }
        // ));
        //
        // this.editableFields.push(new FormObjects.ViewEdit({
        //         'name': 'descr',
        //         'parentSelector': '#info_image_descr span.row-data',
        //         'value': ImageObject.descr
        //     }
        // ));

        $dlgObj.find("#info_image_created span.row-data").text(Utils.toDateTimeString(ImageObject.createTime));
        $dlgObj.find("#info_image_changed span.row-data").text(Utils.toDateTimeString(ImageObject.updateTime));

        for (var i = 0; i < ImageObject.mediaObjects.length; i++) {
            if ( ImageObject.mediaObjects[i].type == MEDIA_PHOTO ) {
                $dlgObj.find("#info_image_url span.row-data a").attr('href', ImageObject.mediaObjects[i].path);

                //  Get Size and dimension
            }
        }

        $dlgObj.find("#info_image_site span.row-data a").text(ImageObject.siteBean.name)
            .attr({
                'href': 'sites.html#siteId=' + ImageObject.siteBean.id,
                'target': '_blank'
            });


        $dlgObj.find("#info_image_lat").text(ImageObject.gpsLat);
        $dlgObj.find("#info_image_lon").text(ImageObject.gpsLon);
        this.options.location = {'lat' : ImageObject.gpsLat, 'lon' : ImageObject.gpsLon};

        //info_image_onSiteId

        $dlgObj.find("#info_image_UUID span.row-data").text(ImageObject.unicId).css('word-wrap','break-word');

        $dlgObj.find("#info_image_onSiteId span.row-data").text(ImageObject.onSiteId).css('word-wrap','break-word');
        //info_image_aperture
        $dlgObj.find("#info_image_aperture span.row-data").text(ImageObject.aperture);

        //info_image_camera
        $dlgObj.find("#info_image_camera span.row-data").text(ImageObject.camMake);

        //info_image_cam_model
        $dlgObj.find("#info_image_cam_model span.row-data").text(ImageObject.camModel);

        //info_image_dpi
        //$dlgObj.find("#info_image_dpi span.row-data").text(ImageObject.onSiteId);

        //info_image_expmode
        //$dlgObj.find("#info_image_expmode span.row-data").text(ImageObject.expMode);

        //info_image_exptime
        $dlgObj.find("#info_image_exptime span.row-data").text(ImageObject.expTime);

        //info_image_focallen
        $dlgObj.find("#info_image_focallen span.row-data").text(ImageObject.focalLen);

        //info_image_focaldist
        $dlgObj.find("#info_image_focaldist span.row-data").text(ImageObject.focusDist);

        //info_image_iso
        $dlgObj.find("#info_image_iso span.row-data").text(ImageObject.isoSpeed);

    };


    //------------------------------------------------------------------------
    //
    //   SAVE: Edit and save image info items
    //
    //------------------------------------------------------------------------
    ImageInfoDialog.prototype.ViewEditInfo = function(event) {

        try {
            var target = event.target || event.srcElement;
            if (!target) {
                return;
            }

            if (target.getAttribute("data-state") == "view") {
                this.editableFields.edit();
            }
            else {
                this.editableFields.save();
            }
        } catch (e) {
            logger.trace("[imageInfo.ViewEditInfo] Change state error:",e);
        }


        // if ( $("#info_edit_btn").attr('data-state') == "view") {
        //     $.each(this.editableFields, function (index,Item) {
        //         Item.edit();
        //     });
        //     $('#info_edit_btn').text("Save").attr("data-state", "save");
        // }
        // else {
        //     $('#info_edit_btn').text("Edit").attr("data-state", "view");
        //     try {
        //         $.each(this.editableFields, function (index,Item) {
        //             Item.save();
        //         });
        //
        //     } catch (e) {
        //         logger.trace("[imageInfo.ViewEditInfo] State=Edit. Error:",e);
        //     }
        // }

    };



    //------------------------------------------------------------------------
    //
    //   DESTROY: on dialog closed
    //
    //------------------------------------------------------------------------
    ImageInfoDialog.prototype.destroy = function() {

    };


    return ImageInfoDialog;
});