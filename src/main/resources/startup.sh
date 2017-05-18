#!/bin/bash

#
#  List of usual java places 
#
ORACLE_JAVA_0_91="/Library/Java/JavaVirtualMachines/jdk1.8.0_91.jdk/Contents/Home"

test=@app.home@


java_places_list=(
	"$JAVA_HOME"
	"$ORACLE_JAVA_0_91"
	""
	)

#
#  Chack for possible java places
#
for path in ${java_places_list[@]}; do
	JAVA_HOME=$path
	if [ -e "$path/bin/java" ]; then
		JAVA_EXEC="$path/bin/java"
		break;
	fi
done

if [ "x$JAVA_EXEC" == "x" ]; then
	echo "Cannot fine JAVA_HOME. exiting..."
	exit 2
fi

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
