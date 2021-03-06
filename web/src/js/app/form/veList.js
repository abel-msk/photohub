/**
 * Created by abel on 29.11.16.
 */
define(["jquery","logger","form/viewEdit"],function ($,logger,ViewEdit) {
    "use strict";


    var  defaultOptions = {
        'element':null,
        'btnElement': null,
        'html':'<div class="row panel-body-row">' +
            '<span class="col-md-4 col-sm-4 row-label">{name}</span>' +
            '<div id="{id}" class="col-md-8 col-sm-8 row-data"></div>' +
            '</div>'
    };

    function VeList(options) {
        this.options = $.extend(true, {}, defaultOptions, options || {});
        this.id = "v" + Math.floor((Math.random() * 10000));

        this.rows = {};

        if ( ! this.options.element ) throw Error("[VEeList.init] element parameter required.");
    }

    VeList.prototype.rowHTML = function(htmlStr,uid,name) {
        var namePattern = /\{name\}/g;
        var idPattern = /\{id\}/g;

        htmlStr =  htmlStr.replace(namePattern,name);
        htmlStr = htmlStr.replace(idPattern,uid);
        // return '<div class="row panel-body-row">' +
        //     '<span class="col-md-4 col-sm-4 row-label">' + name + '</span>' +
        //     '<div id="' + uid + '" class="col-md-8 col-sm-8 row-data"></div>' +
        //     '</div>'
        //     ;
        return htmlStr;
    };

    VeList.prototype.push = function(key, value, tag) {
        this.rows[key] = {
            'key':key,
            'value': value,
            'parentSelector': tag
        };
    };

    VeList.prototype.get = function (key) {
        if ( this.rows[key] &&  this.rows[key].object )
            return this.rows[key].object.getValue();
        else
            return null;
    };

    VeList.prototype.getOriginal = function (key) {
        if ( this.rows[key])
            return this.rows[key].value;
        else
            return null;
    };

    VeList.prototype.render = function() {
        var counter = 0;
        var key;

        for ( key in this.rows) {
            var row = this.rows[key];
            var rowId = this.id + String(counter++);
            var rowEl = this.rows[key].parentSelector?this.rows[key].parentSelector:this.options.element;

            $(rowEl).html("");
            $(this.rowHTML(this.options.html,rowId,key)).clone().appendTo(rowEl);

            row["object"] = new ViewEdit({
                'name': row.key,
                'value': row.value,
                'parentSelector': "#" + rowId
            });
        }
    };

    VeList.prototype.save = function () {
        var key;
        for ( key in this.rows) {
            var row = this.rows[key].object;
            row.save();
        }
        $(this.options.btnElement).text("Edit").attr("data-state", "view");
    };

    VeList.prototype.edit = function () {
        var key;
        for ( key in this.rows) {
            var row = this.rows[key].object;
            row.edit();
        }
        $(this.options.btnElement).text("Save").attr("data-state", "edit");
    };

    VeList.prototype.isChange = function(key) {
        return  this.rows[key] && (this.rows[key].value == this.rows[key].object.getValue())
    };

    //
    //
    //  Вызывает функцию для каждого элемента списка
    //  Параметры  func,caller
    //      func - вызываемая функция
    //      caller - значение оператора this при вызове
    //
    //  В вызываемую функцию будут переданы параметры:
    //     key - имя элкмента списка
    //     value - текущее значение элемента
    //     origValue - начально установленное значение элемента
    //
    //
    VeList.prototype.each = function(func,caller) {
        if (!caller) caller = this;

        if (typeof func == "function") {
            for (var key in this.rows) {
                if (typeof func == "function") {
                    func.call(caller,key,this.rows[key].object.getValue(),this.rows[key].value);
                }
            }
        }
    };


    return VeList;
});