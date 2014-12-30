#!/bin/sh
# -*- Perl -*-

exec "$COMMANDER_HOME/bin/ec-perl" -x "$0" "${@}"

#!perl
#
#-------------------------------------------------------------------------
# fetchManifest.cgi --
#
# Fetch Manifest xml data of an artifact version.
#
# Arguments:
#
#       artifactVersion - The version of an artifact, example: Apache:Tomcat:8.0.0-382f715e-5901-11e4-a57b-0800278dd49e
#
# Side Effects:
#
#
#-------------------------------------------------------------------------
#
# Copyright (c) 2014 Electric Cloud, Inc.
# All rights reserved

use strict;
use CGI;
use ElectricCommander;

$| = 1;

my $ec = new ElectricCommander();
$ec->abortOnError(0);
my $err    = "";
my $status = "";

BEGIN {
	use ElectricCommander;
	no warnings 'redefine';
	*ElectricCommander::getManifest = sub {
		my $self = shift;

		my $am     = $self->initArtifactManagement();
		my $result = $am->getManifest(@_);
		return ( $result, $am->diagnostics() );
	  }
}

print "Content-type: text/xml; charset=utf-8\n\n";

# Extract the request parameters
my $query             = new CGI;
my $artifactVersion   = $query->param("artifactVersion");
my $repositoryName    = $query->param("repositoryName");
my $emptyManifestHead = "<manifest version=\"1\" format=\"tar\" sha1=\"sha1\">";
my $emptyManifestTail = "</manifest>";
my $manifest;
my $diagnostics;

# Fetch artifact manifest

eval { ($manifest, $diagnostics) = $ec->getManifest($artifactVersion, { repositoryNames => "$repositoryName" });};

if ($@) {
	$err = $@;
	$err =~ s/ at .*? line .*//;
	print $emptyManifestHead;
	print "<directory name=\"$err\" />";
	print $emptyManifestTail;
	exit();
}
else {
	print $manifest . "\n";
}
