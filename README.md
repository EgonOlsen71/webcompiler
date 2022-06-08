# WebCompiler / MOSCloud

This is a quick and dirty web application that enables you to use <a href="https://github.com/EgonOlsen71/basicv2">MOSpeed</a> on a server to compile BASICV2 source code either from your browser or from your Commodore 64 using the <a href="https://www.wic64.de/">WiC64</a> to connect to the internet.

This project is threefold and depending on what you actually want to do with it, you won't need all three parts of it. The following should give you an overview of what you need to build and run any part of this project.

# I just want to play around with MOSCloud on my Commodore 64

MOSCloud is a compiler that runs on the Commodore 64 but actually compiles on a remote server. You need a WiC64 for it to run.

If you just want to run the compiler, all you need is the moscloud.d64-image in /basic/build/. Just mount that in your 64 or copy it onto a physical disk and load "moscloud" to run it.

If you want to modify the compiler, you'll find the source code in /basic/moscloud.bas. While you can run it in the interpreter, it's not advised to do so, because it would be much too slow. You can compile it using MOSpeed (or another version of MOSCloud, as it can compile itself). To ease that, there's a build.cmd script for Windows in the /basic/build-folder. To build it on other platforms, you have to write yourself a similar script or do it all by hand.

# I want to host my own server

The server allows you to compile BASIC programs by using a simple web application or by using MOSCloud, if you modify MOSCloud in a way that it finds your server (more on that later).

Please note that running or building the server requires Java 11 or higher to be installed.

To run the server, you need:

* Some machine that can run a Tomcat server
* Maybe a PHP capable server (see below)

To build the server, you need:

* Maven installed
* The basicv2 artifact being present in your local Maven repository. You can put it there by getting <a href="https://github.com/EgonOlsen71/basicv2">MOSpeed</a> and run "mvn clean package install" on it.

To build the server, run the build.cmd (or something equivalent for your OS) in the project's root directory. The build will finish with a file WebCompiler-0.1-SNAPSHOT.war in "target". The web application is supposed to be run using the "WebCompiler" context path, so you have to rename the file before deploying it. For more information about the port to be used for the Tomcat server, see below.

The server will create a directory "/uploaddata" on your machine on first usage. If that fails due to insufficient permissions, it might be a good idea to create it yourself.

The index.html contains informational text about the server on which my installation is running on. You might want to change and/or remove this.

# Customize the server

By default, the server is looking for a configuration file called "wiconf.ini" in "/webdata/". If none can be found, it will use a hard coded default configuration, but that's most likely not what you want. You can find a template for this configuration file in /config/.

If you need the server to use another file, you have to modify the ContextListener. The path is hard coded into a constant.

If your server is behind a dynamic IP, you have to set the correct token in the configuration to update this IP. See the "PHP" section below for more information.

# Run the server

As mentioned, you need a Tomcat server to run it. Just deploy the the built WAR-file (renamed to WebCompiler.war) into the server's webapps-directory and you should be fine. Make sure that it can create its "/uploaddata" directory or create it yourself. Also, create "/webdata" and copy a modified version of the wiconf.ini file into it.
 
You should then be able to call the web-application at http(s)://(server name or ip):(port)/WebCompiler

# A note to users of Debian based OSs

Debian (an in turn Raspberry Pi OS) sandboxes Tomcats for "security reasons" (<- mumble that in a deep voice!). In its current configuration, this server has to be able to write into /uploaddata in the root directory. By default, Debian doesn't allow Tomcat to write into this directory regardless of the permissions set. You'll find documentation on how to fix this in your Tomcat installation here: /usr/share/doc/tomcat9/README.Debian

# Logging

The server logs messages by using its own Logger class. This class is veeeery basic, all it does is printing to the console. In case of a standard Tomcat installation, this means that the output will go into the catalina.out file of your Tomcat installation. Make sure somehow, that this file doesn't grow too large or if you want better logging, modify the Logger class to your liking.

# Why do I need PHP for all this?

You might not, but I did. The project contains a /php-directory which contains three simple scripts. The purpose of these is to work around a problem that you might have with trying to find a cheap server hosting provider that supports Java/Tomcat (Hint: There are none!). My "solution" for this is to host the server myself. But to keep costs down, I'm doing so on a Raspberry Pi behind a normal DSL connection. This connection has no fixed IP and there's no domain for this server either (obviously...). So you have to know the dynamic IP to call it. For that, I've created these scripts. You can deploy them on any server that supports PHP and configure the actual server to use them to keep track of the current IP. The server will call the configured script (refresh.url in wiconf.ini) every 15 minutes to update it's IP. For this to work, the token configured in your wiconf.ini has to match the token that the ipstore.php script expects. See "Security concers" for more details.

If you want to access the web compiler in this setup, you don't do so directly but by using https://(your php server)/(path to scripts>) instead. The index.php will redirect you to the actual server.

The PHP script are hard coded to expect the actual serve to run on port 8192. If that's not the case with your server, change this in the scripts.

If you just want to run the server on your local machine and don't want to make it available to the internet, just ignore the PHP scripts and set refresh.enabled in your server's wiconf.ini to false.

# MOSCloud and my own server

If you want to use MOSCloud with your own server, you have to modify it slightly. For local operation, just remove the REM in the line that sets gu\$ to a fixed URL (lines 62000++) and set gu\$ to your server's URL. Then recompile MOSCloud. There's a build.cmd in /basic/build to compile MOSCloud and create a new d64-image.

If you want it to access a server in the internet, you can do the same, if that server is publicly available. If it's (like mine) located on a dynamic IP, you have to modify line 45700++ to change the URL to the one that your PHP script uses (see above).

# Security concers

"But wait...If I'm using the PHP script to locate my actual server, then everybody can call that and set the server's IP to its own!"...I hear you shout! Yes...but no, actually not. At least not that easy. The PHP script that sets the IP (ipstore.php) checks for a token in the request. Only if this token matches the one that it expects, it will update the IP. For your Tomcat server, you can set this token in the wiconf.ini.

The PHP script itself generates the expected token from hashing the current DOCUMENT_ROOT. This isn't really secure and you probably shouldn't do it this way. I did it, because on my PHP server, this path is strange enough so that people shouldn't be able to easily guess it. And that was good enough for me. You can either just change that part of the script and use another token, or do it properly and store it in some configuration file. After all, the damage that one can do in this context is very limited. In the worst case, a user gets redirected to a server that isn't yours and uploads his or her BASIC program to it instead...not much of a security concern IMHO...but again: Do as I say, not as I do!




