# Documentation of Progress

## Utilize Datasets 
### Datasets
Source of all datasets: https://bulkdata.uspto.gov/
##### Patent Applications
Name: "Patent Application Full Text Data (No Images) (MAR 15, 2001 - PRESENT)"
Fields: summary, appl_date, publ_docNo, appl_type, assignees, abstract, classification, title, publ_date, publ_kind, claims, details, appl_docNo, authors
Number of dataset files: 897
Number of utilized files: 717
Number of documents in elasticsearch: 4.281.343
Comment: The dataset contains 896 zip archives in total. Each zip archives contains one corresponding xml file.
While 717 of those xml files were successfully uploaded to elasticsearch, 180 xml files aborted and were not uploaded. The errors seem to be bound to specific xml files. The precise source of error is unknown yet.

##### Patent Grants
Name: "Patent Grant Full Text Data (No Images) (JAN 1976 - PRESENT)"
Fields: summary, appl_date, publ_docNo, appl_type, assignees, abstract, classification, title, patent_citations, publ_date, publ_kind, citations, claims, details, appl_docNo, authors
Number of dataset files: 2318
Number of utilized files: Unknown
Number of documents in elasticsearch:
Comment: 3.740.538 




# Technical FAQ

## patent-indexing
1. parse patent XML files with python and extract patent ID and claim for each patent
2. index patents with elasticsearch, which is running on our isfet server

#### To access elasticsearch on isfet from our idun server:
1. connect with ssh to idun
2. activate your pythonvirtual environment, eg;
source activate tensorflowenv

3. install elasticsearch dependency from https://elasticsearch-py.readthedocs.io/en/master/ 
pip install elasticsearch
4. start python session or jupyter notebook
python
5. connect to elasticsearch 
from elasticsearch import Elasticsearch
ES = Elasticsearch(['172.16.64.23:9200'])
print(ES.indices.exists('new_telegraph’))

#### es_helpers.py
This file is two years old so it might not work with the current elasticsearch version 7.
Use this as example code to create indices.

#### es_mappings.json
This file contains a so called mapping: https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html
