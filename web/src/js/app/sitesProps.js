/**
 * Created by abel on 29.04.17.
 */



define(["jquery","api","modalDialog","form/veList","logger"],function($,Api,Dialog,VEList,logger) {

    "use strict";


    var PROP_EDIT_BTN = "#property_edit_btn";
    var PROP_LIST_AREA = "#site_properties_list";



    function SiteProp(model) {

        this.modelsProperties = model.get('properties');
        this.rootVal = model.get('root');
        this.propFormRowObj = null;

        this.propForm = new VEList({
            'element':PROP_LIST_AREA,
            'btnElement':PROP_EDIT_BTN
        });

        $(PROP_EDIT_BTN).off('click')
            .on('click', {'caller': this, 'model':model }, function(event) {
                var target = event.target || event.srcElement;
                if (!target) { return;  }
                var model  = event.data.model;
                var caller  = event.data.caller;
                caller.doViewEditProperties.call(caller,target,model);
                }
            );

        $(PROP_LIST_AREA).html("");

        for ( var propName in this.modelsProperties) {
            var item = this.modelsProperties[propName];
            if (item.name != "root" ) {
                this.propForm.push(item.name,item.value);
            }
            else {
                //   Если среди свойст встерилось спецальное "root"
                //   вписываем его значение в строку  "root path"
                this.propForm.push("Store path",item.value);
            }
        }

        this.propForm.render();
    }


    //---------------------------------------------------------------------
    //   doViewEditProperties
    //      Ловим нажатие на кнопку EDIT/SAVE
    //      и выполняем ссответствующие действия в форме
    //
    //---------------------------------------------------------------------

    SiteProp.prototype.doViewEditProperties =  function (target,model) {

        var propList = model.get('properties');

        if ( target.getAttribute("data-state") == "view" ) {
            this.propForm.edit();
        }
        else {
            this.propForm.save();
            try {
                for (var i = 0; i < propList.length; i++) {
                    var key = propList[i].name;
                    if ( key == "root" ) key = "Store path";
                    propList[i].value = this.propForm.get(key);
                }

                model.set("root",this.propForm.get("Store path"));
                model.set('properties', propList);

                //   Сохраняем изменения и отсылаем на сервер
                model.save({
                    wait: true,
                    'error': function () {
                        logger.debug("[SiteProp.doViewEditProperties] Error.  Cannot save property");
                        //model.set('properties', propList);
                        //selectById(model.get('id'));

                        //  !!!  Blow Up for refresh
                        $("body").trigger("sitesProps.error",{'siteId' : model.get("id")});

                    }
                });

            } catch (e) {
                logger.debug("[sites.doViewEditProperties] State=Edit. Error:",e);
            }

        }
    };

return SiteProp;

});