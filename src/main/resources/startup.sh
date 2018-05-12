#!/bin/bash

#
#  List of usual java places 
#

set +x

if [ "x$JAVA_HOME" == "x" ]; then


    if [ "x$JAVA_HOME" == "x" ]; then
        #
        #  Check java frameworks location on macosx
        #
        if [[ "$OSTYPE" =~ ^darwin ]]; then
            JAVA_ROOT="/Library/Java/JavaVirtualMachines/"
            for file in `ls -1Ur "$JAVA_ROOT"`; do
                JAVA_FW=$file;
            done;

            if [ "x$JAVA_FW" != "x" ]; then
                export JAVA_HOME=`find  "${JAVA_ROOT}/${JAVA_FW}" -type d -name 'jre' -print | head -n 1`
            fi
        fi
    fi

    if [ "x$JAVA_HOME" == "x" ]; then
        jrs_path=`command -v jrunscript`
        if [ "x$jrs_path" != "x" ]; then
            export JAVA_HOME=`jrunscript -e 'java.lang.System.out.println(java.lang.System.getProperty("java.home"));'`
        fi
    fi
fi

if [ "x$JAVA_HOME" == "x" ]; then
	echo "Cannot fine JAVA_HOME. exiting..."
	exit 2
fi

JAVA_EXEC="$JAVA_HOME/bin/java"

#SOURCE="/Volumes/Dual-B/Users/abel/Developing/git/photohub2/photohub2-web/target/photohub-exec.war"
#APP_HOME="/Volumes/Dual-B/Users/abel/Developing/photohub-root/app"

#
#    Variaples installed from java build
#

APP_HOME="@app.home@"
APP_BIN="bin"
APP_NAME="@archive.name@"
APP_CONF_PATH="@app.config.path@"
APP_CONF_FILE="@app.config.file@"
APP_LOG_CONFIG="@app.log.config@"
APP_CONFIG="${APP_CONF_PATH}/${APP_CONF_FILE}"

if [ -e "${APP_CONFIG}" ]; then
	DB_PATH=`cat "${APP_CONFIG}" | grep 'db.name='  | awk -F = '{ s = $2} END {print s}'`
	THUMB_PATH=`cat "${APP_CONFIG}" | grep 'localThumbPath='  | awk -F = '{ s = $2} END {print s}'`
	##  Createdb directory if not exist yet
	mkdir -p "$DB_PATH"
	mkdir -p "$THUMB_PATH"
else
	error "Misconfiguration. Cannot find application config file: ${APP_CONFIG}"
	exit 1
fi

cd $APP_HOME

echo "JAVA_HOME=$JAVA_HOME"
echo "JAVA_EXEC=$JAVA_EXEC"
echo ""

echo "Starting application ..."

#nohup $JAVA_EXEC -jar $APP_HOME/bin/$APP_NAME.jar \
#        "--logging.config=${APP_CONF_PATH}${APP_LOG_CONFIG}" \
#        "--spring.config.location=${APP_CONF_PATH}${APP_CONF_FILE}" < /dev/null > $APP_HOME/logs/app.log 2>&1 &

$JAVA_EXEC -jar "$APP_BIN/$APP_NAME.jar" \
	"--logging.config=${APP_CONF_PATH}/${APP_LOG_CONFIG}" \
    "--spring.config.location=${APP_CONF_PATH}/${APP_CONF_FILE}"
