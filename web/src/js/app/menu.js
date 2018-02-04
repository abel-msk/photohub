
/*


Options:

     triggerBindTag:   элемент длякоторого будут генерироваться собфтия для открытия и закрытия меню

     menuList : [
            {
             menuTagId :      id элементоа можменю
             toggleBtnId :    id кнопки  нажатие которой открыввает или закрывает элемент
            },
            ...
         ]


 Example HTML code:

     <ul class="nav navbar-nav animated-nav navbar-right nav-main">
         <li>
            <a id="base-menu-toggle"><span class="glyphicon glyphicon-menu-hamburger"></span>Menu</a>
         </li>
         <li>
            <a id="filter-menu-toggle">Filter</a>
         </li>
         <li data-toggle="modal" data-target="#login-panel"><a>Login</a></li>
     </ul>

     ...
     <div id="base-submenu" class="navbar-inverse container collapse">
         <ul class="nav navbar-nav">
            <li data-toggle="modal" data-target="#upload-panel">
                <a id="upload" href="#">
                    <span class="border">
                    <span class="glyphicon glyphicon-upload"></span>Upload photos
                    </span>
                </a>
            </li>

     ...

 */




define(["jquery","api","modalDialog","logger"],
    function($,Api,Dialog,logger) {

        "use strict";

        var DEBUG = false;

        var defaultOptions = {
            'triggerBindTag':'body',
            'menuList': []
        };

        //------------------------------------------------------------
        //
        //    Переключатель открытия/закрытия меню
        //
        function Menu(options) {
            var o = $.extend(true, options, defaultOptions);
            this.menuList = o.menuList;
            this.el = o.triggerBindTag;

            //   Закрываем все меню
            for ( var i = 0; i < this.menuList.length; i++ ) {
                if (this.menuList[i].menuTagId)
                    $("#"+this.menuList[i].menuTagId).collapse({  toggle: false  });
            }

            //
            //   Main Menu clicking for toggle submenu's
            //
            $('#top-navbar li a').on('click',{'caller':this},function(event){
                var caller = event.data.caller;
                var btnId  = this.id;

                for ( var i = 0; i < caller.menuList.length; i++ ) {
                    if ((caller.menuList[i])
                        && (btnId === caller.menuList[i].toggleBtnId)
                        && (caller.menuList[i].menuTagId ))
                    {
                        if (caller.isMenuOpen(caller.menuList[i].menuTagId)) caller.closeMenu(caller.menuList[i].menuTagId);
                        else caller.openMenu(caller.menuList[i].menuTagId);
                        break;
                    }
                }
            });


            //
            //   Sub Menu clicking
            //
            $('#selection-submenu li a').on('click',{'caller':this},function(event){
                var parentElement = this;
                var caller = event.data.caller;

                switch (parentElement.id) {
                    case 'sel-clear':
                        caller.closeMenu(MENU_SELECTION);
                        break;
                    default :
                        logger.debug("[#selection-submenu.click] Unknown item=" + parentElement.id);
                }
            });
        }


        //------------------------------------------------------------
        //
        //   Проверяет открыто ли в данный момент меню  menuTagId
        //
        Menu.prototype.isMenuOpen = function(menuTagId) {
            return $("#"+menuTagId).attr('aria-expanded') === "true";
        };

        //------------------------------------------------------------
        //
        //   Возвращает название открытого сейчас меню
        //
        Menu.prototype.getOpenMenuID =  function() {
            for ( var i = 0; i < this.menuList.length; i++ ) {
                if ((this.menuList[i].menuTagId)
                    && this.isMenuOpen(this.menuList[i].menuTagId))
                {
                    return this.menuList[i].menuTagId;
                }
            }
            return '';
        };

        //------------------------------------------------------------
        //
        //   Закрывает меню menuName
        //
        Menu.prototype.closeMenu = function(menuName) {
            var openedMenu = this.getOpenMenuID();
            if ( openedMenu === menuName ) {
                $("#"+menuName).collapse('hide');
                $(this.el).trigger("menu.close", [menuName]);
                DEBUG && logger.debug("[Menu.closeMenu] Close menu="+menuName);
            }
        };

        //------------------------------------------------------------
        //
        //   Открывает меню menuName
        //
        Menu.prototype.openMenu = function(menuName) {
            var openedMenu = this.getOpenMenuID();
            if ( openedMenu  && (openedMenu !== menuName )) this.closeMenu(openedMenu);
            $("#"+menuName).collapse('show');
            $(this.el).trigger("menu.open", [menuName]);
            DEBUG && logger.debug("[Menu.openMenu] Open menu="+menuName);
        };

        return Menu;

    });