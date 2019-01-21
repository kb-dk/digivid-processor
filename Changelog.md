Version 2.9
===========
Changed the year to be determined from week based to day based

Version 2.8
===========
Added method that parses username alias instead of a strict path

Version 2.7
===========
Dateformat changed from yy-MM-dd to yyyy-MM-dd

Version 2.6
===========
Files that starts with "temp" do not get shown

Version 2.4
===========
Files that start with "temp" are not shown in the GUI

Version 2.3
===========
Changed path to digivid-processor.properties and removed config/windows

Version 2.2
===========
Changed value of digivid.processor.recordsdir
Always move comments file first (ugly hack)!

Version 1.4
===========
Changed to make a windows specific version

Version 1.3
===========
Always move comments file first (ugly hack)!
Error check happens around reading the config files
It is now possible to read old JSON files
The temporary Colossus files are ignored
Warnings and error messages displayed correctly 
Configurable autosaveinterval
Fixed so that an error in commit does NOT cause you to lose all metadata
The recordsdir must exist
Fixed the processed issue
Should fix the double file bug
Fix for the datetimepicker skin crash
Made datetime picker use 24h
Monitored properties and jackson for serializatoin
Focus fixed

Version 1.2
===========
Temporary files do no longer get shown in the video file list
Files that start with "temp" are skipped
When localDate is null in either startdataepicker.setConverter or enddatepicker.setConverter the used parameter is the current time instead


Version 1.0
===========
Initial release.
Just unpack the zip-file and customise config/digivid-processor.properties. Double click on start.bat to start.