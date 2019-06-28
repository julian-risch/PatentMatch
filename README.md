# Documentation of Progress

## Step 1: Utilize Datasets 
### Datasets
##### Patent Applications
- **Source of dataset**: https://bulkdata.uspto.gov/
- **Name**: "Patent Application Full Text Data (No Images) (MAR 15, 2001 - PRESENT)"
- **Fields**: summary, appl_date, publ_docNo, appl_type, assignees, abstract, classification, title, publ_date, publ_kind, claims, details, appl_docNo, authors
- **Number of dataset files**: 897
- **Number of utilized files**: 717
- **Number of uploaded documents to elasticsearch**: 4.281.343
- **Comment**: The dataset contains 896 zip archives in total. Each zip archives contains one corresponding xml file in a specified version. While 717 of those xml files were successfully uploaded to elasticsearch, 180 xml files aborted and were not uploaded. The errors seem to be bound to specific xml files. The precise source of error is unknown yet.

##### Patent Grants
- **Source of dataset**: https://bulkdata.uspto.gov/
- **Name**: "Patent Grant Full Text Data (No Images) (JAN 1976 - PRESENT)"
- **Fields**: summary, appl_date, publ_docNo, appl_type, assignees, abstract, classification, title, patent_citations, publ_date, publ_kind, citations, claims, details, appl_docNo, authors
- **Number of dataset files**: 2318
- **Number of utilized files**: unknown/to be evaluated
- **Number of documents uploaded to elasticsearch**: 3.740.538 
- **Comment**: The dataset contains on 2318 zip archives in total. Each zip archives contains one of three formats: PG (XML), IPG (XML), APS (TXT). The APS files consist of 1356 files, but are not yet uploaded. The APS format only applies to patents from 1976-2001. Since the data from the office actions concerns 2.2 million patents with office actions for the years 2008-2017, the uploads of APS files can probably be ommited. The remaining 962 files, constituted from PG and IPG formats, were uploaded. It is yet unknown whether there is an error rate under those uploaded files. The number of 3.740.538 uploaded documents suggest a reasonably success rate in the first place.

##### Office Actions
- **Source of dataset**: https://developer.uspto.gov/product/patent-application-office-actions-data-stata-dta-and-ms-excel-csv
- **Name**: "Patent assignment economics data for academia and researchers: created/maintained by the USPTO Chief Economist (JAN 1970 - DEC 2017)"
- **Source of dataset documentation**: https://bulkdata.uspto.gov/data/patent/office/actions/bigdata/2017/USPTO%20Patent%20Prosecution%20Research%20Data_Unlocking%20Office%20Action%20Traits.pdf

The dataset consists of three sub-datasets.
###### Office Actions
- **Name**: "Office Actions"
- **Fields**: rejection_dp, closing_missing, header_missing, uspc_subclass, allowed_claims, signature_type, fp_missing, rejection_101, rejection_112, cite102_gt1, rejection_102, rejection_fp_mismatch, rejection_103, document_cd, ifw_number, uspc_class, cite103_gt3, art_unit, cite103_max, app_id, cite103_eq1, mail_dt, objection
- **Number of dataset files**: 1
- **Number of utilized files**: 1
- **Number of documents uploaded to elasticsearch**: 4.384.532
- **Comment**: Since the documentation specifies the number of office actions to 4.4 million, the uploaded number of office actions appears to be complete.

###### Rejections
- **Name**: "Rejections"
- **Fields**: action_type, alice_in, header_missing, action_subtype, rejection_101, rejection_102, rejection_103, bilski_in, art_unit, app_id, cite103_eq1, claim_numbers, mail_dt, objection, rejection_dp, closing_missing, uspc_subclass, allowed_claims, fp_missing, mayo_in, rejection_112, cite102_gt1, rejection_fp_mismatch, myriad_in, document_cd, ifw_number, cite103_gt3
- **Number of dataset files**: 1
- **Number of utilized files**: 1
- **Number of documents uploaded to elasticsearch**: 10.113.601  
- **Comment**: The documentation specifies 10.1 million unique document-action pairs in the rejections data file (page 10, last paragraph). Relying on the correctness of that number, the uploaded number of rejections appears to be complete.

###### Citations
- **Name**: "Citations"
- **Fields**: action_type, citation_pat_pgpub_id, header_missing, form1449, action_subtype, rejection_101, rejection_102, rejection_103, form892, art_unit, app_id, cite103_eq1, mail_dt, objection, rejection_dp, closing_missing, uspc_subclass, allowed_claims, fp_missing, rejection_112, cite102_gt1, rejection_fp_mismatch, document_cd, ifw_number, cite103_gt3, parsed, citation_in_oa
- **Number of dataset files**: 1
- **Number of utilized files**: 1
- **Number of documents uploaded to elasticsearch**: 52.546.031 
- **Comment**: The documentation specifies 58.9 million unique application-citation pairs in the citations data file (page 10, last paragraph). Relying on the correctness of that number, 6 million application-citation pairs were not uploaded. The reason was not evaluated yet.

### Dataset Patent Miner and Elasticsearch
Elasticsearch is a search engine that allows to query indexed data. To make use of the patent datasets, we upload and index the data into our elasticsearch engine, so we have an uniform and instant access to the data via queries. Further work is based on that access. To transfer our data to elasticsearch, we deployed an XML/CSV Parser that parses each file, extracts all entries with its relevant informations. The extracted information is then uploaded into a predefined schema to our elasticsearch engine. The predefined schema corresponds to the "Fields" listing of each processed dataset.

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
