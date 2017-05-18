/**
 * Created by abel on 22.11.15.
 */

//
//    Sites list  return format:
//      [
//         {"id":"41","name":"First site","root":"/tmp","defaultSite":false,"connectorType":"Local","connectorState":"DISCONNECT","properties":[],"siteUser":"admin"},
//         {"id":"42","name":"Second site","root":"/tmp","defaultSite":false,"connectorType":"Local","connectorState":"DISCONNECT","properties":[],"siteUser":"admin"}
//      ]

//    Site model:
//        connectorState: "DISCONNECT"
//        connectorType: "Local"
//        defaultSite: false
//        id: "16"
//        name: "Local SIte"
//        properties: []
//        root: "/Volumes/Dual-B/Users/abel/Developing/photohub-root/photo-root"
//        siteUser: "admin"

define(["backbone","api","modalDialog","logger"],function(Backbone,Api,Dialog,logger) {


    function _errorDialog(response) {
        if ( response.rc =='403') return true;
        Dialog.open({
            'error': true,
            'text':  response.message,
            'title': "Error",
            'buttons': {OK: function(){}}
        });
        return false;
    }




    function _sync(from, method, collection, options) {

        var caller = this;
        options || (options = {});

        switch (method) {
            case 'create':
                logger.debug("["+from+".sync] Create.");
                if (from == "model") {
                    Api.addSite(
                        collection.attributes,
                        //    OnSuccess
                        function (response) {
                            if (typeof options.success == 'function') {
                                options.success(response.object);
                            }
                        },
                        //  OnError
                        _errorDialog
                    );
                }
                break;

            case 'update':
                logger.debug("["+from+".sync] Upadte.");
                if ( from === 'model') {
                    Api.updateSite(collection,

                        //  On Success
                        function(response){
                            if (typeof options.success == 'function') {
                                try {    // backbone return error
                                    options.success(response.object);
                                }
                                catch (e) {}
                            }
                        },

                        //  On Error
                        function(response){
                            Dialog.open({
                                'error': true,
                                'title': "Server error",
                                'text': response.message,
                                'buttons': {OK: function(){}}
                            });
                            if (typeof options.error == 'function') {
                                options.error(response);
                            }
                        }
                    )
                }
                break;

            case 'delete':
                logger.debug("["+from+".sync] Delete.");
                if ( from === 'model') {
                    Api.delSite(collection.get('id'),
                        //  On Success
                        function(response){
                            if (typeof options.success == 'function') {
                                try {    // backbone return error
                                    options.success(response);
                                }
                                catch (e) {}
                            }
                        },
                        //  On Error
                        function(response){
                            Dialog.open({
                                'error': true,
                                'title': "Server error",
                                'text': response.message,
                                'buttons': {OK: function(){}}
                            });
                        }
                    )
                }
                break;

            case 'read':
                logger.debug("["+from+".sync] Read.");
                if (from == "collection") {
                    Api.listSites(
                        //    OnSuccess
                        function (response) {
                            collection.url = Api.getActionUrl("listSites");
                            if (typeof options.success == 'function') {
                                options.success(response.object);
                            }
                        },
                        //  OnError
                        _errorDialog
                    );
                }
        }  /// END OF SWITCH
    }   ///  END OF SYNC






    //
    //function  Collection() {
    //    this.collectionAr = [];
    //    this.attributes = {};
    //    this.url = "";
    //    this.callbacks = {};
    //    this.length = 0;
    //}
    //
    //Collection.extend.on = function(eventName,callback){
    //    if ( this.callbacks[eventName] ) {
    //        this.callbacks[eventName] = [];
    //    }
    //    this.callbacks[eventName].push(callback);
    //};
    //
    //Collection.extend.trigger = function(eventName,options) {
    //    if (this.callbacks && this.callbacks[eventName])  {
    //        var handlers = this.callbacks[eventName];
    //        for (var i = 0; i < handlers.length; i++) {
    //            if (handlers[i] && (typeof handlers[i] == "function")) {
    //                if (! handlers[i](options))break;
    //            }
    //        }
    //    }
    //};
    //
    //
    //Collection.extend.fetch = function(options) {
    //
    //    var collection = this;
    //
    //    var localOptions = $.extend(true,{},options || {},
    //        {
    //            'success':function(response) {
    //                $.each(response, function (item, value) {
    //                    collection.add(value);
    //                });
    //                collection.trigger('reset',collection );
    //            }
    //        }
    //    );
    //
    //    if (options.reset) {
    //        this.collectionAr = [];
    //    }
    //    _sync('collection',"read",this,localOptions);
    //
    //};
    //
    //
    //Collection.extend.add = function(model) {
    //    if ( model && model.id) {
    //        model.cid = id;
    //        this.collectionAr[id] = model;
    //    }
    //    else {
    //        model.cid = this.collectionAr.length + 1;
    //        this.collectionAr[model.cid] = model;
    //    }
    //    this.length = this.collectionAr.length;
    //};
    //
    //Collection.extend.get = function(Index) {
    //
    //};
    //Collection.extend.at = function(position) {
    //
    //};
    //Collection.extend.each = function (callback, caller) {
    //    var cl = caller?caller:this;
    //    if (typeof callback == "function") {
    //        for (var i = 0; i < this.collectionAr.length; i++) {
    //            callback.call(cl,this.collectionAr[i]);
    //        }
    //    }
    //};


    function SitesCollection() {

        var collectionPrototype = Backbone.Collection.extend({

            model: Backbone.Model.extend({
                defaults: {
                    "id": null,
                    "name": "",
                    "root": "",
                    "connectorType": "Local",
                    "connectorState": "DISCONNECT",
                    "properties": []
                },
                //,
                sync: function (method, collection, options) {
                    _sync('model', method, collection, options);
                }
            }),


            sync: function (method, collection, options) {
                _sync('collection', method, collection, options);
            }

        });

        this.collection = new collectionPrototype();
    }

    SitesCollection.prototype.load = function() {
        this.collection.fetch({reset: true});
    };

    SitesCollection.prototype.getCollection = function() {
        return this.collection;
    };

    SitesCollection.prototype.getArray = function() {
        return this.collection.models();
    };

    SitesCollection.prototype.get = function(id) {
        return this.collection.get(id);
    };

    SitesCollection.prototype.getFirstWhere = function(attributes) {
        return this.collection.findWhere(attributes);
    };


    return SitesCollection;
});