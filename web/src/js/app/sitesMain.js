/**
 * Created by abel on 19.11.15.
 */

// requirejs.config({
//     baseUrl: "js/app",
//     paths: {
//         'jquery': '../lib/jquery/dist/jquery',
//         'backbone': '../lib/backbone/backbone',
//         'underscore': '../lib/underscore/underscore-min',
//         // 'storage': '../lib/jStorage/jstorage.min',
//         // 'json': '../lib/json2/json2',
//         'bootstrap': '../lib/bootstrap/dist/js/bootstrap',
//         'dropzone': '../lib/dropzone/dist/dropzone-amd-module'
//     },
//     shim: {
//         'bootstrap' : {
//             deps:['jquery']
//         },
//         'underscore': {
//             exports: '_'
//         },
//         'backbone': {
//             deps: ['underscore', 'jquery'],
//             exports: 'Backbone'
//         },
//         'json': {
//             exports: 'JSON'
//         },
//         'storage': {
//             deps: ['json', 'jquery']
//         }
//     }
// });


define(["jquery",
        "login",
        "modalDialog",
        "logger",
        "sitesCollection",
        "api",
        "connector",
        "sitesProps",
        "sitesActions",
        "sitesTasks",
        "sitesSched",
        "utils",
        "moment",
        "locale/ru",
        "bootstrap"
        ],

    function ($,Login,Dialog,logger,SitesCollection,Api,Connector,SitesProps,Actions,SitesTasks,SitesSched,Utils,Moment) {
    $(document).ready(function() {


        Moment.locale('RU');

        var HashParams = Utils.hashParamsToMap(window.location.hash);
        //  Init site connect btn processor
        var connector = null;
        var actions = null;
        var tasks = null;
        var scheds = null;
        var propFormRows;           //  Current site properties form rows

        var sitesList = new SitesCollection();
        var collection = sitesList.getCollection();
        var siteProps = null;

        //var collection = SitesCollection;   //   Prepare sites collection

        //---------------------------------------------------------------------
        //     EVENTS
        //---------------------------------------------------------------------
        //   Can be triggered during initial sites load
        // $("body")
        //     .on('scanner.changeState', changeScanState);

        $('#menu_back').on('click',{'caller':this},function(event){
            //window.history.back();
            //history.go('src/index.html');
            //window.location.href='index.html';
            window.location.replace('index.html');
            //window.history.replaceState( {} , 'Catalog', 'index.html' );
        });

        //---------------------------------------------------------------------
        //    Collection event handler
        //---------------------------------------------------------------------
        //   Handle collection load
        collection.on('reset', function (loadedCollection) {
            logger.debug("[sites] Collection reset event fired.");
            loadSites(loadedCollection);
            if (loadedCollection.length > 0) {

                if (HashParams.siteId) {
                    selectById(HashParams.siteId);
                }
                else {
                    selectByPos(0);
                }
            }
        }, this);

        collection.on('add', function (model, collection, options) {  //TODO change to sync event
            loadSites(collection);
            selectById(model.get('id'));

            // Close modal dualog
            $("#add_site_dialog").modal('hide');
        });

        collection.on('destroy', function () {
            logger.debug("[sites] Collection destroy event fired.");
            loadSites(collection);
            if (collection.length > 0) {
                selectByPos(0);

            }
        }, this);

        //   Start collection loading
        //collection.fetch({reset: true});
        sitesList.load();

        function getCurrentId() {
            return $('#sites-list .active').attr('id');
        }

        //---------------------------------------------------------------------
        //     ADD site handler
        //---------------------------------------------------------------------
        $('#cmd-add-class').on('click', function (event) {
            logger.debug("[addsite_btn] Click.");
            //var caller = event.data.caller;
            Actions.add(collection);
        });



        //---------------------------------------------------------------------
        //    SCAN Site handler
        //---------------------------------------------------------------------
        $("#site_scan_btn").on('click', {'caller': this}, function (event) {
            if ( validateStorePath() ) {
                actions.doScan();
            }
        });

        //---------------------------------------------------------------------
        //    CLEAN site handler
        //---------------------------------------------------------------------
        $("#site_clean_btn").on('click', {'caller': this}, function (event) {
            actions.doClean();
        });


        //---------------------------------------------------------------------
        //   DELETE site from collection
        //---------------------------------------------------------------------
        //   Handle collections model delete

        $('#delete_site').on('click', {'caller': this}, function(event){
            var siteId = getCurrentId();
            actions.deleteSite(collection.get(siteId));  // called with model
        });

        //---------------------------------------------------------------------
        //    Filling out sites list
        //---------------------------------------------------------------------
        function loadSites(collection) {
            try {
                //
                //    Заполняем Sidebar c именами сайтов
                //
                $("#sites-list").html("");

                collection.each(function (modelItem) {
                    $('<a id="' + modelItem.get('id') + '" class="list-group-item">' + modelItem.get('name') + '</a>')
                        .clone()
                        .appendTo('#sites-list')
                        .on('click', {'caller': this}, function (event) {
                            selectById.call(event.data.caller, this.id);
                        });
                }, this);

            } catch (e) {
                logger.trace("[loadSites] Error ", e);
            }
        }

        //---------------------------------------------------------------------
        //    NAVIGATE / SELECTING
        //---------------------------------------------------------------------
        function selectById(modeId) {
            var model = collection.get(modeId);
            if (model) {
                fillSiteForm(model);
                $('#sites-list .active').removeClass('active');
                $('#sites-list #' + modeId).addClass("active");
            }
        }

        function selectByPos(position) {
            var model = collection.at(position);
            if (model) {
                fillSiteForm(collection.at(position));
                $('#sites-list .active').removeClass('active');
                $('#sites-list #' + model.get("id")).addClass('active');
            }
        }


        //---------------------------------------------------------------------
        //
        //    Filling out  site form
        //    Заполняем страницу данными о выбранном сайте
        //
        //---------------------------------------------------------------------
        function fillSiteForm(model) {
            try {

                //   Заполняем имя сайта и тип.
                $("#site_name").text(model.get('name'));
                $("#connect_btn").attr("site", model.get('id'));
                $("#site_connector_type").text(model.get('connectorType'));



                $('body')
                    .off('connector.changeState')
                    .on('connector.changeState', changeConnectionState)

                    .off("sitesprops.error")
                    .on('sitesprops.error', function(event,data) {
                        selectById(data.siteId);
                    });


                //   Заполняем  состояние коннекта к сайту
                connector = new Connector();
                connector.setState(model.get('connectorState'), model.get('id'));


                //   Заполняем и инициализируем Properties сайта
                siteProps = new SitesProps(model);


                tasks = new SitesTasks(model);

                logger.debug("[SitesTask]  START ");


                scheds = new  SitesSched(model);

                //   В переделку
                //actions = new Actions({'siteId': model.get('id')});



            } catch (e){
                logger.debug("[sites.fillSiteForm] Error:",e);
            }
        }



        //---------------------------------------------------------------------
        //   Site state changed handler
        //
        //    {
        //        state :     Connection status  CONNECT DISCONNECT AUTH-WAIT
        //        siteId:      Sites id
        //    }
        //---------------------------------------------------------------------
        //
        function changeConnectionState(event, data) {
            try {

                var state = data.state == null?"UNKNOWN": data.state ;

                if (data.siteId) {
                    var model = collection.get(data.siteId);
                    if ( model.get("connectorState") !== state) {
                        model.set({'connectorState': state});
                    }
                }
            }
            catch (e) {
                logger.trace("[changeState] Error ", e);
            }
        }


        //  В ПЕРЕДЕЛКУ

        //---------------------------------------------------------------------
        //   Site scan task change state  handler
        //---------------------------------------------------------------------
        //   Data  object format
        //   {
        //            recordId:           taskRecord id  when saved in db
        //            message:            on success last trace message, on error error message
        //            name:               "scan"  task name
        //            site:               the site Object
        //            startTime:          time task started
        //            stopTime:           time task finished
        //            status:             task status. Can be one of: IDLE RUN ERR FIN
        //   }
        //
        // function changeScanState(event,data) {
        //
        //     //var textStatus = "";
        //     var textDate = "Never";
        //     // var statusClass = "status-norm";
        //     // var descr = "";
        //
        //     var html = "";
        //
        //     if (data != null) {
        //         switch (data.status) {
        //             case 'RUN':
        //                 textDate = "";  // getFormattedDate(new Date(data.startTime));
        //                 html='<span class="status-succ">RUNNING. <i class="fa fa-spinner fa-pulse fa-fw"></i> Start at '+
        //                     Utils.toDateString(data.startTime) +'</span>';
        //                 break;
        //             case 'ERR':
        //                 textDate = Utils.toDateString(data.stopTime);  // getFormattedDate(new Date(data.stopTime));
        //                 html='<span class="status-err">ERROR:</span> <span>&nbsp;'+data.message +'</span>';
        //                 break;
        //             default:  // FIN
        //                 textDate = Utils.toDateString(data.stopTime);  // getFormattedDate(new Date(data.stopTime));
        //                 html='<span class="status-norm"></span>';
        //                 break;
        //         }
        //     }
        //     $("#scan_date").text(textDate);
        //     $("#scan_status").html(html);
        // }

        //---------------------------------------------------------------------
        //    Validators
        //---------------------------------------------------------------------
        function validateStorePath() {
            var checkedString  = propFormRows.get("Store path");

            if ((checkedString != "")  &&  (checkedString == null)) {
                Dialog.open({
                    'title': "Site connect",
                    'text': "Sites story path property is required for scanning.",
                    'buttons': {
                        OK: function () {
                        }
                    }
                });
            }
            else {
                return true;
            }

            return false;
        }


    }); // END of REQUIRE on document ready

});