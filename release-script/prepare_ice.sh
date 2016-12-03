#!/usr/bin/env bash
#
#  input: JET distribution tar
#         $ICE_HOME
#  output:  ICE distribution tar
#
# Run this script in a fresh directory
#
JET_PACKAGE=$JET_HOME/export/jet-161201.tar.gz
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
curl http://daringfireball.net/projects/downloads/Markdown_1.0.1.zip > Markdown_1.0.1.zip
unzip Markdown_1.0.1.zip
echo "Unpacking Jet package..."
tar zxvf $JET_PACKAGE
mv props jet-props
cp jet-all.jar $ICE_HOME/lib
pushd $ICE_HOME
# ant dist-all-jar
popd
perl Markdown_1.0.1/Markdown.pl $ICE_HOME/README.md > README.html
perl Markdown_1.0.1/Markdown.pl $ICE_HOME/docs/iceman.md > docs/iceman.html
perl Markdown_1.0.1/Markdown.pl $ICE_HOME/docs/ICE_Design.md > docs/ICE_Design.html
cp $ICE_HOME/docs/*.png docs/
cp $ICE_HOME/LICENSE ./
cp $ICE_HOME/COPYRIGHT ./
#  scripts
cp $ICE_HOME/src/scripts/runice.sh ./bin
cp $ICE_HOME/src/scripts/runtagger.sh ./bin
cp $ICE_HOME/src/scripts/icecli ./bin
cp $ICE_HOME/src/scripts/icecli6 ./bin
#  ice.yml, iceprops, onomaprops, parseprops, props
cp $ICE_HOME/src/props/* ./
#  quantifierPatterns and ACE DTD
cp $ICE_HOME/src/models/data/* ./data/
#  files for export from ICE
touch acedata/ice_onoma.dict
touch acedata/EDTypesFromUser.dict
touch acedata/iceRelationModel
chmod u+x bin/runice.sh
chmod u+x bin/runtagger.sh
chmod u+x bin/icecli6
chmod u+x bin/icecli
rm -rf Markdown*
eco "Building ICE tar"
set date = `date +'%y%m%d'`
tar zcvf ice-$date.tar.gz *
