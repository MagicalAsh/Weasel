mkdir plugins/

# Create a dummy directory for use when testing the plugin loader
mkdir ./CommonConfiguration/plugins

# Create a link for the actual plugins to be available during testing
cp ./Plugins/*/build/libs/* ./plugins
ln -s ../plugins ./Index/plugins
ln -s ../plugins ./Provider/plugins
