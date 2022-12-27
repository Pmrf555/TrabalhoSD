#! /bin/bash
# This script is used to start the Server
#

app=Server

function compile {
	javac $app.java
}

function run {
	java $app
}

function main {
	compile
	run
	rm -f */*.class *.class
}

main
