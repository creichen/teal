#! /bin/bash

BASEDIR=`dirname $0`
# Changes defaults to only issue CodeProber style reports and default to checking instead of running
export TEAL_CODEPROBER_MODE=true

if [ x${CODEPROBER_JAR} == x ]; then
	CODEPROBER_JAR=${BASEDIR}/libs/code-prober.jar
fi

TEAL_JAR_PATTERN=(${BASEDIR}/compiler/teal-?.jar)
[[ -e "${TEAL_JAR_PATTERN[@]}" ]] && TEAL_JAR="$TEAL_JAR_PATTERN"

if [ ! -e "${TEAL_JAR}" ]; then
    echo "There must be exactly ONE of 'teal-0.jar', 'teal-1.jar', 'teal-2.jar', or 'teal-3.jar' in ${BASEDIR}/compiler/"
    exit 1
fi

#java -Djava.security.manager=allow -jar ${BASEDIR}/libs/code-prober.jar ${TEAL_JAR} $@
if [ x$1 != x ]; then
	SOURCE="-Dcpr.backing_file=$1"
fi

AUTOPROBES='lang.ast.Program:nameErrors'
AUTOPROBES='lang.ast.Program:semanticErrors',${AUTOPROBES}
AUTOPROBES='*:reports',${AUTOPROBES}
AUTOPROBES='*:mayNullReport',${AUTOPROBES}
AUTOPROBES='*:mustNullReport',${AUTOPROBES}
AUTOPROBES='*:nullDomainValue',${AUTOPROBES}

# disable distracting UI options
# args override
DISABLE_UI=control-should-override-main-args
# custom file sffix
DISABLE_UI=${DISABLE_UI},control-customize-file-suffix
# position recovery strategy
DISABLE_UI=${DISABLE_UI},control-position-recovery-strategy
# AST cache strategy
DISABLE_UI=${DISABLE_UI},ast-cache-strategy
# syntax highlighting
DISABLE_UI=${DISABLE_UI},syntax-highlighting
# location style
DISABLE_UI=${DISABLE_UI},location-style
# read-only mode
DISABLE_UI=${DISABLE_UI},control-read-only-mode
# version/update info
#DISABLE_UI=${DISABLE_UI},version

echo java -jar ${SOURCE} ${CODEPROBER_JAR} --force-read-only --force-disable-ui=${DISABLE_UI} --ast-cache-strategy=FULL --change-tracking --default-probes=${AUTOPROBES} --force-syntax-highlighting=teal ${TEAL_JAR}
java -jar ${SOURCE} ${CODEPROBER_JAR} --force-read-only --force-disable-ui=${DISABLE_UI} --ast-cache-strategy=FULL --change-tracking --default-probes=${AUTOPROBES} --force-syntax-highlighting=teal ${TEAL_JAR}

