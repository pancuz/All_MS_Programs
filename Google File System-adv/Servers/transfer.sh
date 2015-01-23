#! /bin/sh

# This shell script is to excute scp to transfer file from one server to other replica after a server failure
# Requires three inputs > $1:destination server name/ip $2:filename with path $3:filepath on destination server 
echo "SCP Started"
`scp $2  pxk131330@$1:$3`
