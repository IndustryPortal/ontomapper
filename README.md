# ontomapper
is a comprehensive solution designed for efficient management and utilization of mappings within the IndustryPortal system. It facilitates the extraction, conversion, storage, and alignment of mappings between ontologies. 
Ontommaper comes as web service (implemented in Java) which executes a mechanism to extract mapping from ontology source file hosted in Industryportal.

This project aims at augmenting the standard OntoPortal platform with a utility to support the above-mentioned features of ontology mapping providing 
full support to manage them from use interface. The service is developed and deployed in a manner to be fit to be integrated in the rest of Ontoportal installations.

## main Features
* Automated mappings extraction and conversion to SSSOM format
* Enable bulk mappings upload
* Enable Contextual Mappings (contextual alignment) in Industryportal
* Support complex class Description and constraints in OWL Manchester Syntax

## Design and Architecture

The service relies on the SSSOM standard for mapping representation and structuration, where mappings will be following a tabular format, and to be grouped in mapping sets. 
The schema proposed by the SSSOM standard and that will be used in the service displayed in the following image

<img alt="sssom-mapping-class-schema" src="">

The service will parse the ontologies files hosted in Industryportal, in order to extract mappings using OWLApi library, then gather the needed meta-data and finally format 
the mappings in SSSOM format, in order to be stored finally in a SQL database through the Mysql server that is already installed in the Industryportal(Ontoportal) environment.

<img alt="ontomapper-mapping-extraction" src="">

On the other hand the service will offer some CRUD endpoints to enable adding of mappings that are already in SSSOM format, wether on a bulk upload or through manually typing through the user 
interface