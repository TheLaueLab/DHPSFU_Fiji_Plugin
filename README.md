DHPSFU - An ImageJ Plugins for Double Helix PSF Analysis 
===========================================================

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

The DHPSFU plugin provides a suite of tools for analysing 3D SMLM/SPT data obtained using Double Helix (DH) PSF microscopy. 

Features:

- DHPSFU: 
Converts a list of 2D localisations into 3D coordinates. 
Requires a DH calibration file and a 2D data file.
Outputs the 3D coordinates into a tab-separated file (.3d) in “x y z Intensity Frame” format.  

- DHPSFU-Multibeads:
On top of DHPSFU, corrects lateral spatial variation of the DH across the FOV.
Requires multiple calibration files from different parts of the imaging FOV. 
Outputs the 3D coordinates into a tab-separated file (.3d) in “x y z Intensity Frame” format.  

- Drift Correction:
Corrects drift during acquisition using cross-correlation of images of the same imaging sample. 
Inspired by and developed from Mennella et al.

- Blinking Correction:
Temporal grouping or tracking of localisations.

- Overlay: 
Overlays the results onto the raw image stack.

- Load File Localisations: 
Loads a list of 2D/3D localisations from file to FIJI memory.
Available pre-defined formats are GDSC PeakFit output file (.xls) for 2D localisations and ViSP format (.3d) for 3D localisations.
Any other format can be loaded by specifying custom columns.

- View and Save localisations:
Savse results to a table, a file, an image and/or to FIJI memory
Creates localisation density images.


Installation
------------

The DHPSFU plugins are distributed using an ImageJ2/Fiji update site.

To install the plugins using Fiji (an ImageJ distribution) just follow the
instructions [How_to_follow_a_3rd_party_update_site](http://fiji.sc/How_to_follow_a_3rd_party_update_site)
and add the DHPSFU update site. All the plugins will appear under the 'Plugins > DHPSFU' menu.


Documentation
-------------

For detailed instructions on how to use the plugin, click the 'Help' button provided within the interface. 
This will provide you with guidance on how to use the function and explanations for each parameter.


Modifying the source
--------------------

The source code is accessed using git and built using Maven.
The code depends on the GDSC SMLM plugin (https://github.com/aherbert/gdsc-smlm).  
you will have to install these to your local Maven repository before building:
The DHPSF code was developed using the [Eclipse IDE](https://eclipse.org/).

License
-------

DHPSFU is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE.txt)


# About #

###### Authors ######
Dr Ziwei Zhang, Department of Chemistry, University of Cambridge
Dr Alex Herbert, School of Life Sciences, University of Sussex
Siqi Liu, Department of Computer Science, University of St Andrews

###### Other contributors ######
Dr Aleks Ponjavic, School of Physics and Astronomy, University of Leeds
Dr Aleksandra Ochirova, Department of Biochemistry, University of Cambridge


###### Owner(s) ######
The Laue Lab, Department of Biochemistry, University of Cambridge

