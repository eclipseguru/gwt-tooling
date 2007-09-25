This directory contains the included web clients and other support files. 

For your convenience it contains the development files for the GWT hosted mode. 
At production the files will be replaced with the compiled GWT output. Thus,
none of those files that you currently see should goes into production. A build script
will typically generate the files using the GWT compile and not copy anything from
this directory when assembling the WAR file.

