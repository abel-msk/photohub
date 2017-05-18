/**
 * Created by abel on 29.11.16.
 */


define(["jquery","logger"],function ($,logger) {
    "use strict";


    var defaultOptions = {
        'objectId': null,
        'parentSelector': null,
        'viewClass': "",
        'editClass': "",
        'placeholder': "",
        'onEdit': null,
        'onSave': null,
        'value': "",
        'name': "",
        'state': "view",
        'inputClasses': '',
        'viewClasses': ''
    };

    function inputHtml(options) {
        return '<textarea id="input-' + options.objectId + '" type="text" class="viewedit-edit form-control ' + options.inputClasses + '"  style="width:100%;display:' + (options.state === "view" ? 'none' : 'inlineblock') + '" placeholder="' + options.placeholder + '"></textarea>';
    }

    function viewHtml(options) {
        return '<div id="view-' + options.objectId + '"  class="viewedit-view ' + options.viewClasses + '" style=display:"' + (options.state === "view" ? 'inlineblock' : 'none') + ';">' + options.value + '</div>';
    }


    function Viewedit(options) {
        this.options = $.extend(true, {}, defaultOptions, options || {});
        if (!this.options.objectId) {
            this.options.objectId = Math.floor((Math.random() * 10000));
        }
        this.render();
        return this;
    }

    Viewedit.prototype.render = function () {
        try {
            var parent = this.options.parentSelector;
            this.state = 'view';
            if (parent) {
                $(parent).empty();
                this.inputObj = $(inputHtml(this.options)).clone().appendTo(parent);
                this.viewObj = $(viewHtml(this.options)).clone().appendTo(parent);
            }
        } catch (e) {
            logger.trace("[ViewEdit.render] Error: ", e.stack);
        }
    };

    Viewedit.prototype.edit = function (openDialog) {
        try {
            if (this.state == 'view') {
                var height = this.viewObj.outerHeight();
                if (height > 10) {
                    this.inputObj.outerHeight(height);
                }
                else {
                    var parentHeight = $(this.options.parentSelector).parent().height();
                    this.inputObj.outerHeight(parentHeight);
                }

                this.inputObj.show().val(this.options.value);
                this.viewObj.hide();
                this.state = 'edit';
                if (typeof this.options.onEdit == 'function') {
                    this.options.onEdit(this.inputObj, this.viewObj);
                }

            }
        } catch (e) {
            logger.trace("[ViewEdit.edit] Error:", e.stack);
        }
    };
    Viewedit.prototype.save = function (openDialog) {
        try {
            if (this.state == 'edit') {
                this.options.value = this.inputObj.hide().val();
                //this.inputObj.hide();
                this.viewObj.show().text(this.options.value);
                this.state = 'view';
                if (typeof this.options.onSave == 'function') {
                    this.options.onSave(this.inputObj, this.viewObj);
                }
            }
        } catch (e) {
            logger.trace("[ViewEdit.save] Error:", e.stack);
        }
        return this.options.value;
    };

    Viewedit.prototype.setValue = function (val) {
        this.options.value = val;
    };

    Viewedit.prototype.getValue = function () {
        return this.options.value;
    };

    Viewedit.prototype.getName = function () {
        return this.options.name;
    };


    return Viewedit;
});