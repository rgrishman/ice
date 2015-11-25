#!/usr/bin/env bash
# Run this script in a fresh directory
JET_PACKAGE=http://cs.nyu.edu/grishman/jet/jet-150509.tar.gz
PARSER_MODEL=http://cs.nyu.edu/grishman/jet/parseModel.gz
NE_MODELS=http://cs.nyu.edu/grishman/jet/AceOntoMeneModel.gz
ICE_JAR=ice-all.jar
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
echo "Fetching Jet package..."
curl $JET_PACKAGE > jet.tar.gz
echo "Fetching parser model..."
curl $PARSER_MODEL > parseModel.gz
echo "Fetching NE tagger models..."
curl $NE_MODELS > AceOntoMeneModel.gz
gunzip AceOntoMeneModel.gz
curl http://daringfireball.net/projects/downloads/Markdown_1.0.1.zip > Markdown_1.0.1.zip
unzip Markdown_1.0.1.zip
rm -rf ice-bin
mkdir ice-bin
cd ice-bin
mv ../jet.tar.gz ./
tar zxvf jet.tar.gz
mv props jet-props
mv ../parseModel.gz data/
mv ../AceOntoMeneModel acedata/
rm build.xml
rm jet.tar.gz
rm -rf parser-stub-src
rm -rf src
rm -rf test
git clone https://github.com/ivanhe/ice.git
cd ice
# Temporary: Should use master branch
git checkout master
ant
perl ../../Markdown_1.0.1/Markdown.pl README.md > README.html
cd ..
perl ../Markdown_1.0.1/Markdown.pl ice/docs/iceman.md > ice/docs/iceman.html
mv ice/README.html ./
cp ice/docs/* docs/
rm docs/*.md
cp ice/LICENSE ./
cp ice/COPYRIGHT ./
cp ice/$ICE_JAR ./
cp ice/src/scripts/runice.sh ./
cp ice/src/scripts/runtagger.sh ./
cp ice/src/props/* ./
cp ice/src/models/data/* ./data/
touch acedata/ice_onoma.dict
touch acedata/EDTypesFromUser.dict
touch acedata/iceRelationModel
chmod u+x runice.sh
chmod u+x runtagger.sh
rm -rf ice
cd ..
rm -rf Markdown*
zip -r -X ice-bin.zip ice-bin/
