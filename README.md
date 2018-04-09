# OGEMA backup parser
Tools for analysing resource graph dumps and log data from OGEMA gateways

## Overview
This repository contains two bundles providing the parsing functionality, and two simple visualization apps:
* `backup-parser`: the low-level parsing API. Pass a file or folder, and it returns the stored resource graph representation.
* `backup-gateway-analysis`: the slightly more high-level parsing API. Parses a folder for subfolders containing gateway data: zip files of resource graph dumps and the SlotsDb/FendoDb log data folders
* `memory-gateway-visualisation`: visualisation for `backup-parser`. 
* `backup-gateway-viz`: visualisation for `backup-gateway-analysis`.

The collective Maven group id is *org.smartrplace.analysis*, the artifact id is the project name.

## Dependencies
* [OGEMA](https://github.com/ogema/ogema): bundles `org.ogema.core:api`, `org.ogema.core:models`, `org.ogema.ref-impl:util` (version >= 2.1.3)
* [FendoDB](https://github.com/smartrplace/fendodb) (optional at runtime; required to parse log data)
* [OGEMA](https://github.com/ogema/ogema) and [OGEMA widgets](https://github.com/ogema/ogema-widgets) (for visualisation apps only)

## Configuration
Set the base path for gateway data via the system or framework property `org.smartrplace.analysis.backup.parser.basepath`. Default is *ogemaCollect/rest*. 

## Build
Prerequisites: git, Java and Maven installed; FendoDB API bundle available (checkout and build https://github.com/smartrplace/fendodb)

1. Clone this repository
2. In a shell, navigate to the base folder and execute `mvn clean install` 

## License
[GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html)