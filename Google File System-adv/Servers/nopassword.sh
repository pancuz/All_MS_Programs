#!/bin/bash

# Copyright (c) 2007 Glynn Tucker Consulting Engineers
# License: Dual-license under GPL/BSD license
# 	GPL (v2 or any later version): http://www.fsf.org/licensing/licenses/gpl.txt
#	MIT License: http://www.opensource.org/licenses/mit-license.html

# Define our functions
setup_ssh_certificates()
{
	# parameters: 
	#	1: ip/hostname of the mirror
	#	2: username to login with
	#	3: password
	# purpose: 	Set up passwordless login between this host and the mirror
       #		specified using ssh public key authentication
	MIRROR_IP=$1
	MIRROR_USER=$2
	MIRROR_PASSWORD=$3

	# Verify we can login
	CAN_SSH="`ssh -o PasswordAuthentication=no $MIRROR_IP exit > /dev/null 2>&1; echo $?`"

	if [ "$CAN_SSH" -eq 255 ]; then
		# SSH server is listening, we can login and do our work
		echo -n Attempting to setup passwordless login via SSH keys...

		if [ -f ~/.ssh/id_dsa -o -f ~/.ssh/id_dsa.pub ]; then
			echo
			echo "Using pre-existing keys at ~/.ssh/id_dsa*"
		else
			if ssh-keygen -t dsa -f ~/.ssh/id_dsa -N ""; then
				echo Sorry, failed to create public/private key pair
				exit 1
			fi
		fi
		
		# Log in to mirror and install keys
		# This requires some nasty tricks because ssh
		# won't let us supply a password
		# Quick steps:
		# 1. Set DISPLAY to something
		# 2. Generate a script that outputs the password
		# 3. Set SSH_ASKPASS to the name of that script
		# 4. Use setsid to disassociate ssh from this script's tty

		ASKPASS_SCRIPT_NAME=./nopassword.sh
		# We certainly don't want our password hanging around if the script fails
		sleep 10 && rm $ASKPASS_SCRIPT_NAME 2>/dev/null &

		if [ "$DISPLAY" == "" ]; then
			DISPLAY=localhost:0.0
		fi
		echo "#!/bin/bash

		echo $MIRROR_PASSWORD " > $ASKPASS_SCRIPT_NAME
		chmod 700 $ASKPASS_SCRIPT_NAME 
		
		SSH_ASKPASS=$ASKPASS_SCRIPT_NAME
		setsid scp ~/.ssh/id_dsa.pub $MIRROR_USER@$MIRROR_IP:~/`hostname`_id.pub < /dev/null

		setsid ssh $MIRROR_USER@$MIRROR_IP \
			mkdir -p '~/.ssh' \; chmod 700 '~/.ssh; touch ~/.ssh/authorized_keys2'

		setsid ssh $MIRROR_USER@$MIRROR_IP "
		if grep \"\`cat `hostname`_id.pub\`\" .ssh/authorized_keys2 > /dev/null;
		then
			true
		else
			cat `hostname`_id.pub >> .ssh/authorized_keys2
		fi
		rm `hostname`_id.pub"
		rm $ASKPASS_SCRIPT_NAME
	fi

	echo -n Testing remote login ability...
	if ssh -o PasswordAuthentication=no $MIRROR_USER@$MIRROR_IP exit; then
		echo OK
	else
		echo Failed!
		echo Sorry, I was unable to set up the automatic logins you requested
		exit 5
	fi
}

# Ensure we have a network
echo -n Checking for network interface...
if /sbin/ifconfig | grep -q eth[0-9]; then
	echo OK
else
	echo Failed!
	echo You need to be connected to the network to run this script.
	echo This script assumes your network interface is on eth[0-9]
	exit 2
fi

# Ask for IP of the mirror
echo -n "What is the IP Address of the mirror? : "
read input
if [ "$input" != "" ]; then
	MIRROR_IP=$input
fi

# Check for existence of this mirror
echo -n Attempting to ping the mirror at $MIRROR_IP...
if ping -c 1 -w 5 -n $MIRROR_IP > /dev/null 2>&1; then
	echo OK
else
	echo Failed!
	echo The mirror you specified can\'t be pinged on the network.
	echo Please ensure you have a linux installation at $MIRROR_IP then run this script again.
	exit 3
fi

# Ask for username of the mirror
echo -n "What is the username for the remote host?: "
read MIRRORHOST_USER
echo

# Ask for password of the mirror
echo -n "What is the password for the remote host?: "
read -s MIRRORHOST_PASSWORD
echo

# Create public key certificates
setup_ssh_certificates $MIRROR_IP $MIRRORHOST_USER $MIRRORHOST_PASSWORD

