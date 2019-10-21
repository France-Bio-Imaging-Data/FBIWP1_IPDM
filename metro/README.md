### What is Metro for?

Metro is a collection of Icy's plugin (Blocks).
It gives you full access to bio-images stored those were stored in your OpenImadis or 
Omero remote servers.
It also provides some tools to feed, populate metadata for your images and display
meta information for analysis interacting with all plugins available in Icy.

As Metro runs over Icy's protocols at the end you would design your complete solution for metrology.

* Version 0.0.1 (beta)

### How do I get set up?

** For eclipse developers:

* Go to File -> Import -> Git (Project from git)
* Clone the project https://yourusername@bitbucket.org/fbiwp1/metro.git (master)
* Right click over "metro" project. (Configure->Convert to Maven project)
* Right click over « metro » project Properties->Tick Icy plugin (Icy Plugin Nature)
* Run this from eclipse as you usually do in your Icy’s plugins (D)
* Have fun

** For console developers: 

* Install maven version 3 or superior
* clone the project: (git clone https://yourusername@bitbucket.org/fbiwp1/metro.git)
* Enter to the project folder
* to clean execute: mvn clean
* to compile execute: mvn compile
* to create a jar execute: mvn package
* to create code coverage report execute: mvn cobertura:cobertura
* you will find your jar file in target/ folder
* Have fun

** No developers can find the plugin over Icy’s site (Not yet available)

### How to run in headless mode ?

* Go to the Icy folder.
* execute : java -jar icy.jar -hl -x plugins.ofuica.metro.Metro <serverDriver> <guid> <protocolProcess>
* <serverDriver> could be OpenImadis or Omero
* <guid> image id
* <protocolProcess> could be: psf_profiler

### Executing protocol file

* Go to icy folder
* execute : java -jar icy.jar -hl -x plugins.adufour.protocols.Protocol protocol=<path_to_proto> psf_id=<psf id> image_id=<image id>
* (e.g) java -jar ./icy.jar -hl -x plugins.adufour.protocols.Protocols protocol=./benchmark.protocol psf_id=11844 image_id=11866
* So benchmark protocol will execute.

### Publisher

* Using a terminal software, go to the metro's application folder publisher/apps/IcyProtocol
* Then execute the command javac *.java. You will find a dot class generated file.

### Executing the publisher on a computer or cluster

In your computer:

* Using terminal software
* Go to metro's application folder and publisher.
* run ./run-compute.bat

In a cluster:

* You will need copy all the publisher's folder to the cluster location.
* Compile using your app javac *.java
* Run ./run-compute.bat (It is necessary specify some parameters depending of the needs.)
* Go to the OpenImadis web interface and execute the process for an image.

![Capture d’écran 2017-05-24 à 14.10.16.png](https://bitbucket.org/repo/epEBxB/images/736063935-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.10.16.png)

## Metro blocks operations

### Metro Login

![Capture d’écran 2017-05-24 à 14.03.52.png](https://bitbucket.org/repo/epEBxB/images/2816409166-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.03.52.png)

### Metro Get Sequence

![Capture d’écran 2017-05-24 à 14.16.17.png](https://bitbucket.org/repo/epEBxB/images/680456197-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.16.17.png)

### Metro ROI
![Capture d’écran 2017-05-24 à 14.17.26.png](https://bitbucket.org/repo/epEBxB/images/3058877014-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.17.26.png)

### Metro Search

![Capture d’écran 2017-05-24 à 14.19.59.png](https://bitbucket.org/repo/epEBxB/images/362850631-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.19.59.png)

### Metro Upload

![Capture d’écran 2017-05-24 à 14.20.42.png](https://bitbucket.org/repo/epEBxB/images/691247429-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.20.42.png)

### Metro Annotate

![Capture d’écran 2017-05-24 à 14.36.03.png](https://bitbucket.org/repo/epEBxB/images/737682286-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.36.03.png) 

### Metro Attach Sequence

![Capture d’écran 2017-05-24 à 14.37.06.png](https://bitbucket.org/repo/epEBxB/images/2821291729-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.37.06.png)

### Metro time chart

![Capture d’écran 2017-05-24 à 14.38.10.png](https://github.com/PerrineGilloteaux/FBIWP1_IPDM/raw/master/metro/images/105636929-Capture%2520d%E2%80%99e%CC%81cran%25202017-05-24%2520a%CC%80%252014.38.10.png)

### Metro Comment

![Capture d’écran 2017-05-24 à 14.39.50.png](https://bitbucket.org/repo/epEBxB/images/241097863-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.39.50.png)

### Metro Tiles Loop 

![Capture d’écran 2017-05-29 à 15.50.35.png](https://bitbucket.org/repo/epEBxB/images/3048115218-Capture%20d%E2%80%99e%CC%81cran%202017-05-29%20a%CC%80%2015.50.35.png)

## Usage examples

Now, using your imagination and creativity you can create some "protocols" on Icy Software using the blocks operations defined bellow. 

For motivation, I will let you some examples :)

### Omero to OpenImadis process

![Capture d’écran 2017-05-17 à 15.16.55.png](https://bitbucket.org/repo/epEBxB/images/2326035168-Capture%20d%E2%80%99e%CC%81cran%202017-05-17%20a%CC%80%2015.16.55.png)

### Displaying results in a Chart

![Capture d’écran 2017-05-24 à 14.34.50.png](https://bitbucket.org/repo/epEBxB/images/1812777273-Capture%20d%E2%80%99e%CC%81cran%202017-05-24%20a%CC%80%2014.34.50.png)

### Metro Tiles Loop + spot detector + Metro ROI

![Capture d’écran 2017-05-29 à 16.01.18.png](https://bitbucket.org/repo/epEBxB/images/3273943544-Capture%20d%E2%80%99e%CC%81cran%202017-05-29%20a%CC%80%2016.01.18.png)

### Search annotated cells images in Omero server

![Capture d’écran 2017-06-12 à 17.24.54.png](https://bitbucket.org/repo/epEBxB/images/1086801721-Capture%20d%E2%80%99e%CC%81cran%202017-06-12%20a%CC%80%2017.24.54.png)

![Capture d’écran 2017-06-12 à 17.17.11.png](https://bitbucket.org/repo/epEBxB/images/1582456220-Capture%20d%E2%80%99e%CC%81cran%202017-06-12%20a%CC%80%2017.17.11.png)


### Generating and storing central moments for all PSF

![Capture d’écran 2017-07-11 à 18.01.04.png](https://bitbucket.org/repo/epEBxB/images/233061413-Capture%20d%E2%80%99e%CC%81cran%202017-07-11%20a%CC%80%2018.01.04.png)


### Real application to annotated indicator of performance
This is an real application sample to calculate an indicator using the difference of computed resolutions.

The final version should use an indicator calculated with central moments
![Capture d’écran 2017-06-08 à 10.59.14.png](https://bitbucket.org/repo/epEBxB/images/1273009748-Capture%20d%E2%80%99e%CC%81cran%202017-06-08%20a%CC%80%2010.59.14.png)

### How to compare two PSF (Measured and calculated)

![test PSF Moments.protocol_screenshot.png](http://hft.io/psf_moments.png)
