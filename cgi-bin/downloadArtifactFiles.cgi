#!/bin/sh
# -*- Perl -*-

exec "$COMMANDER_HOME/bin/ec-perl" -x "$0" "${@}"

#!perl
#
#-------------------------------------------------------------------------
# downloadArtifactFiles.cgi --
# artifactVersion: the specified artifact version
#
# artifactFiles:   Selected file to download. Sample string looks like this:
#
# 	 "./resources/images/abc.jpg","./resources/images/serversettings.jpg",
#
# First, entire artifact version is loaded into cache.
#       
#-------------------------------------------------------------------------
#
# Copyright (c) 2014 Electric Cloud, Inc.
# All rights reserved

use strict;
use CGI;
#use CGI::Carp qw(fatalsToBrowser);
use ElectricCommander;
use File::Copy qw(move);
use File::Copy qw(copy);
use File::Basename;
use File::Path qw(mkpath);
use File::Path qw(rmtree);
#use Data::Dumper;
use Archive::Zip qw( :ERROR_CODES :CONSTANTS );

#$| = 1;

my $ec = new ElectricCommander({abortOnError => 0});
my $err = "";
my $status = "";
my @downloadedFiles;

# Extract the request parameters
my $query = new CGI;
my $artifactVersion = $query->param("artifactVersion");
my $repositoryName = $query->param("repositoryName");
my $artifactZipFileName = $artifactVersion;
$artifactZipFileName =~ s/":"/"_"/g;
$artifactZipFileName = "$artifactZipFileName.zip";
my $artifactFiles = $query->param("artifactFiles");

my $tempDownloadLocation = "$ENV{COMMANDER_HOME}/apache/htdocs/temp/$artifactVersion";
my $finalDownloadLocationBase = "$ENV{COMMANDER_HOME}/apache/htdocs/commander/downloads/";
my $finalDownloadLocation = "$finalDownloadLocationBase$artifactVersion/";
my $finalZipFileLocation = "$finalDownloadLocationBase$artifactZipFileName";
my $cacheDirectory = "$ENV{COMMANDER_HOME}/artifact-cache";
my @files = split(",", $artifactFiles);

#print "$artifactVersion\n";

# check if tempDownloadLocation directory exists, if not then create it.
if ( !-d "$tempDownloadLocation" ) {
	mkpath ("$tempDownloadLocation", 0 , 0777) or die "Unable to create $tempDownloadLocation, $!";
}
	
# retrieve artifact version to temporary download location
$ec->retrieveArtifactVersions ({ 
				artifactVersionName => "$artifactVersion", 
				toDirectory => "$tempDownloadLocation", 
				cacheDirectory => "$cacheDirectory",
				repositoryNames => "$repositoryName",
    });


foreach my $file (@files) {

	my $filePathtoFetch = $file;
	my $fileBaseName = basename($filePathtoFetch);
	my $fileSubdir = dirname($filePathtoFetch);
	
    #print "Filepath to fetch:$filePathtoFetch to $tempDownloadLocation\n";

	# check if directory exists, if not then create it.
	if ( !-d "$finalDownloadLocation$fileSubdir" ) {
		mkpath ("$finalDownloadLocation$fileSubdir", 0 , 0777) or die "Unable to create $finalDownloadLocation$fileSubdir, $!";
	}
	
	copy("$tempDownloadLocation/$filePathtoFetch","$finalDownloadLocation$fileSubdir") or die "Copy failed: $!";

}

#zip the final download location folder.
my $zip = Archive::Zip->new();
local $Archive::Zip::UNICODE = 1;
$zip->addTree("$finalDownloadLocation", "$artifactVersion", sub { -f && -r }, COMPRESSION_STORED);
unless ( $zip->writeToFileNamed( "$finalZipFileLocation" ) == AZ_OK ) {
   die 'zip file error';
}

rmtree("$tempDownloadLocation") or die "Unable to remove directory: $!";
rmtree("$finalDownloadLocation") or die "Unable to remove directory: $!";

if ($@) {
print "Content-type: text/html; charset=utf-8\n\n";
	$err = $@;
	$err =~ s/ at .*? line .*//;
	print $err;
	exit();
} else {

print $query->header(-type            => 'application/x-download',
                    -attachment      => $artifactZipFileName,
                    -Content_length  => -s "$finalZipFileLocation",
);

binmode(STDOUT);

open(my $DLFILE, '<', "$finalZipFileLocation") or die "can't open : $!";
binmode $DLFILE;
print while <$DLFILE>;
undef ($DLFILE);
unlink ("$finalZipFileLocation") or die "Unable to remove zip file: $!";
}
