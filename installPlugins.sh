mkdir plugins/

cp ./Plugins/*/build/libs/* ./plugins
ln -s ../plugins ./Index/plugins
ln -s ../plugins ./Provider/plugins
