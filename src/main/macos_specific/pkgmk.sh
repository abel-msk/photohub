#!/bin/bash
#  Use:  pkgbuild for creating mackos inatll package
#  Example: pkgbuild --root ROOT --identifier com.grivettools.bookmarks --scripts ./scripts --version 1.0 bookmarks.pkg
#  See the folowing resources:
#  * https://themacwrangler.wordpress.com/2017/04/28/packaging-guidelines-for-mac-os/
#  * http://thegreyblog.blogspot.ru/2014/06/os-x-creating-packages-from-command_2.html
#  * https://stackoverflow.com/questions/11487596/making-os-x-installer-packages-like-a-pro-xcode-developer-id-ready-pkg

PKG_NAME=$1

PKG_ROOT="@pkg.home@"

cd "$PKG_ROOT"
#find "./ROOT" -type 'd' -exec chmod 755 "{}" \;
#find "./ROOT"  -exec chown 'root:wheel'  "{}" \;
echo "Create package ${PKG_NAME} "
pkgbuild   --root ./ROOT   --identifier home.abel.photohub --scripts ./scripts --version 1.0  "${PKG_NAME}"
