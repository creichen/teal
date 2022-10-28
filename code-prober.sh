#! /bin/bash

BASEDIR=`dirname $0`

TEAL_JAR_PATTERN=(${BASEDIR}/compiler/teal-?.jar)
[[ -e "${TEAL_JAR_PATTERN[@]}" ]] && TEAL_JAR="$TEAL_JAR_PATTERN"

if [ ! -e "${TEAL_JAR}" ]; then
    echo "There must be exactly ONE of 'teal-0.jar', 'teal-1.jar', 'teal-2.jar', or 'teal-3.jar' in ${BASEDIR}/compiler/"
    exit 1
fi

#java -Djava.security.manager=allow -jar ${BASEDIR}/libs/code-prober.jar ${TEAL_JAR} $@
if [ x$1 != x ]; then
	SOURCE="--source $1"
fi
echo java -jar ${BASEDIR}/libs/code-prober.jar --autoprobes nameErrors,semanticErrors,reports --syntax teal "${SOURCE}" ${TEAL_JAR} -D
java -jar ${BASEDIR}/libs/code-prober.jar --autoprobes nameErrors,semanticErrors,reports --syntax teal ${SOURCE} ${TEAL_JAR} -D
