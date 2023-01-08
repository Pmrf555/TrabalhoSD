#! /bin/bash
# This script is used to start the Server
#

Dimension=20
Radius=2
Scooters=800

app=Server

function help_ {
	echo "Usage: $0 [-r radius] [-d dimension] [-s scooters]"
	echo "  -h help: show this help"
	echo "  -r radius <Default 2>"
	echo "  -d dimension <Default 20>"
	echo "  -s scooters <Default 800>"
	exit 1
}

function compile {
	javac $app.java
}

function run {
	java -Dradius=$Radius -Ddimension=$Dimension -Dscooters=$Scooters $app 
}

function main {
	compile
	run
	rm -f */*.class *.class
}

while getopts r:d:s:h flag
do
    case "${flag}" in
        r) Radius=${OPTARG};;
        d) Dimension=${OPTARG};;
        s) Scooters=${OPTARG};;
        h) help_;;
    esac
done

main