# Documentation

## Step 0: Parse and utilize datasets 
The basis of our dataset is the <a href='https://www.epo.org/searching-for-patents/data/bulk-data-sets/text-analytics.html'>EP full-text data for text analytics</a> by the EPO. It contains the XML formatted full-texts and publication meta-data of all filed patent applications and published patent documents processed by the EPO since 1978. From 2012 onwards, the search reports for all patent applications are also included. In these reports, patent examiners cite paragraphs from prior art documents if these paragraphs are relevant for judging the novelty and inventive step of an application claim. While there are no search reports available for applications filed before 2012, these older applications are still contained in our dataset because their corresponding published patent documents are frequently referenced as prior art. We use the available search reports to create a dataset of claims of patent applications matched with prior art, more precisely, paragraphs of cited “X” documents and “A” documents. Our data processing pipeline uses Elasticsearch for storing and searching through this large corpus of about 200GB text data.

Elasticsearch is a search engine that allows to query indexed data. To make use of the patent datasets, we upload and index the data into our elasticsearch engine, so we have an uniform and instant access to the data via queries. Further work is based on that access. As a first step, an XML parser extracts the full text and meta-data from the raw XML files. Further, for each citation within a search report, it extracts claim number, patent application ID, date, paragraph number, and the type of the references, i.e., “X” document or “A” document. The extracted information is then uploaded into a predefined schema to our elasticsearch engine. The predefined schema corresponds to the "Fields" listing of each processed dataset. 

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/0_parse.py'>---> Go to parsing and uploading file <---</a>


### Datasets / Elastic Search Indices
##### EP full-text data for text analytics
The dataset is split into two sub-datasets. Dataset 1 "EP_Patent_Applications" contains all patent applications with abstract, title, claims,...,citation ids. Dataset 2 "EP_Citations" contains all citation_ids with the corresponding citation information. Dataset 1 and 2 are connected via those citation ids. The unique id of dataset 1 is a concatenation of "Application Number" + "Application Category" + "Application Date". The unique id of dataset 2 is a concatenation of "Application Number" + "Application Category" + "Application Date" + "Citation Number", whereas citation number is the citation count that is annotated to each citation within one single application. To find any citations for an application, one therefore has to iterate through the citation ids and concatenate them to the current application id. Applications of this dataset do not necessarily contain all fields that are referenced in this report. Some entries are missing its application dates. Those entries are ignored and not uploaded.

- **Dataset Source**
https://www.epo.org/searching-for-patents/data/bulk-data-sets/text-analytics.html

- **Dataset Description** http://documents.epo.org/projects/babylon/eponet.nsf/0/2BC42D0D0015756EC125840B00277AEF/$FILE/EP_full_text_data_for_text_analytics-user_guide_v1.0_en.pdf

###### EP_Patent_Applications
- **Name** "EP_Patent_Applications"
- **Fields**: Application_Category, Citation_IDs, Claims, Amended_Claims_Statements, Application_Date, Citation_IPCR_Classification, Description, Abstract, Amended_Claims, Application_Number, Title, Publication_URL
- **Number of dataset files**: 35
- **Number of utilized files**: 35

###### EP_Citations
- **Name** "EP_Citations"
- **Fields**: Doc_Number, Dnum, Date, Country, Kind, Publication_url, Nplcit, Name, Category_A, Category_D, Category_E, Category_P, Category_O, Category_L, Category_X, Category_T, Category_Y, Rel-passage_D, Rel-passage_A, Rel-passage_L, Rel-passage_E, Rel-passage_T, Rel-passage_P, Rel-passage_O, Rel-passage_Y, Rel-passage_X
- **Number of dataset files**: 35
- **Number of utilized files**: 35


### Description of uploaded data
#### EP full-text data for text analytics (EP_Patent_Applications/EP_Citations)
<p align="center">
  <img width="530" src="https://github.com/cgsee1/patent-indexing/blob/master/dataset_applications.png">
</p>
<p align="center">
  <img width="500" src="https://github.com/cgsee1/patent-indexing/blob/master/dataset_citations.png">
</p>
<p align="center">
  <img width="460" src="https://github.com/cgsee1/patent-indexing/blob/master/ep_citations.png">
</p>
<p align="center">
  <img width="460" src="https://github.com/cgsee1/patent-indexing/blob/master/ep_citations_log.png">
</p>
<p align="center">
  <img width="460" src="https://github.com/cgsee1/patent-indexing/blob/master/amounts.png">
</p>

#### New Dataframe, combined from EP_Patent_Applications/EP_Citations

<p align="center">
  <img width="660" src="https://github.com/cgsee1/patent-indexing/blob/master/dataframe_statistics.png">
</p>

## Step 1: Start creating and CSV dataset: Filter categories and separate claims
The data stored in Elasticsearch will be used from now on to create and structure the dataset step by step. The results themselves are done in CSV's, the data in Elasticsearch is not changed. Since we later used the same scripts to generate not only positive (category X) but also negative samples (category A), only scripts with major differences are separated accordingly. We produce a CSV file, where each row contains information about the "Patent Application ID", "Patent Citation ID" (one quoted patent in search report, the ID format does not correspond to the Patent Application ID in Elasticsearch, but contains the original data patent id), "Application Claim Number" (number of the claim in the patent), "Application Claim Text" (the original claim text), "Related Passages Against Claim" (information from the Search Report Writer which passages of the cited patent are relevant) and "Category" (Category X for positive /A for negative) Each date contains only one claim. All patents that are included in the data are filtered for containing information within the fields "citation_ids" and "claims", as well as citations in the Search Report of category X or A. 
A general remark to the linked scripts: As we create one dataset for positive samples and another dataset for negatives samples, we process them separaretly with the help of the same scripts. Usually there is only an adjustment regarding the passed input files/paths. Input files/paths and potential authentification credentials have to adjusted individually to get the scripts working. Two separate scripts are only linked, if major differences occur in the script.

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/1_createDataFrameClaims_positiveSamples.py'>---> Go to file for positive samples <---</a>
  
<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/1_createDataFrameClaims_NegativeSamples.py'>---> Go to file for negative samples <---</a>

## Step 2: Structuring the CSV dataset: Standardize data format of paragraph references
A second parsing step standardizes the data format of paragraph references. References like “[paragraph 23]-[paragraph 28]” or “0023 - 28” are converted to complete enumerations of paragraph numbers “[23,24,25,26,27,28]”. References by patent examiners comprise not only text paragraphs but also figures, figure captions or the whole document. In the standardization process, all references that do not resolve to text paragraphs are discarded.
Due to the massive amount of data to be processed, errors have regularly occurred after several hundred thousand processed patents. Therefore this script was executed several times (about 10 times). The CSV's were subsequently merged into a single CSV.

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/2_extractCitedPatentText.py'>---> Go to file for paragraph standardization <---</a>
  
<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/3_mergeFramesWithExtractedParagraphes.py'>---> Go to file for merging the CSVs <---</a>
  
## (Optional) Step 3: Obtaining equivalent patent ids for dataset
The same patents are often published in several jurisdictions in order to enjoy international legal protection. In this optional step (that we not used for the final data set), all patents from Elasticsearch are checked for equivalent identifiers from other geographic jurisdictions. This allows the subsequent extraction of cited passages from the cited search report patents if the equivalent patent from another jurisdiction is indexed in Elasticsearch. For example, for a citation on a US patent, the equivalent EU patent could be found and this could be used for the extraction of the relevant text passages and vice versa. The first script requests the <a href='https://www.epo.org/searching-for-patents/data/web-services/ops_de.html'>--->official Open Patent Services API of the European Patent Office<---</a> (registration necessary) for obtaining equivalent ids for all relevant Elasticsearch patents that are contained in our dataset. We experienced difficulties while requesting the (free/non-paid) Open Patent Services API on a very high frequency basis. Therefore, we had to restart the script several times and produced several outputs. The second script parses each output log of the first script, merges them and enriches the csv from step 2. 
  
<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/3_optional_opsAPI.py'>--->Go to file for OPS API Requests<---</a>
  
<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/3_optional_parseEquivalentsLog.py'>--->Go to file for parsing the API logs<---</a>

Server Link Equivalents CSV: sftp://172.20.11.11/home/nialde/PatentParagraphExtraction/equivalents.csv

  
## Step 4: Extracting the paragraph text passages of the cited patents
In this step, we use the Elasticsearch index to resolve the referenced paragraph numbers (together with the corresponding document identifiers) to the paragraph texts. Similarly, we resolve the full texts corresponding to the claim numbers. Again, this script was executed several times to continue work after thrown errors. Several CSVs were generated and subsequentlly merged by the second script. To execute the script with the optional step 3, merely uncomment the marked lines for the desired purpose. As we dont need the information about equivalent ids after this step anymore, the final dataset drops the relevant column. To not loose the information, the script contains a method to save each patent (key) where we could obtain at least one equivalent (values) to a dictionary that is saved as a pickle object to disk. 

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/4_createFrameWithParagraphTexts_withoutEquivalents.py'>---> Go to file for extracting cited paragraph texts<---</a>
  
<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/5_mergeFramesWithExtractedParagraphes.py'>--->Go to file for merging files from first script<---</a>
  
## Step 5: Creating the final dataset

The final data set is generated with the first script (of the link list at the bottom of this section). Until now, all relevant passages from a cited patent for ONE claim were available unseparated (that means one cell with concatened paragraph text). In this script, the dataset is structured so that each date consists of exactly one claim and one paragraph. The output is the final dataset, which consists of a master and satellite CSV (for positive and negative samples). The master CSV contains (separately for positive / negative samples) global primary keys (incremented) for the claims with their respective text and referenced paragraphs. The satellite CSV contains the global claim keys and their text, as well as exactly one cited paragraph to this claim and further information on this date.

Through the second script, we combine positive and negative samples into a global dataset and split it into train, validation and test set.
We obtain a global dataset that consists of a total of 6.259.703 samples, where each sample contains a claim text, a referenced paragraph text, and a label indicating the
type of the reference (“X” document or “A” document). We also provide two variations of the data for simplified usage in machine learning scenarios. The first variation balances the label distributions by downsampling the majority class. For each sample with a claim text and a referenced paragraph labeled “X”, there is also a sample with the same claim text with a different referenced paragraph labeled “A” and vice versa. This balanced training set consists of 347.880 samples. However, in this version of the dataset, different claim texts can have different numbers of references. The number of “X” and “A” labels is only balanced for each claim text itself. The second variation balances not only the label distribution but also the distribution of claim texts. Further downsampling ensures that there is exactly one sample with label “X” and one sample with label
“A” for each claim text. As a result, every claim in the dataset occurs in exactly two samples. This restriction reduces the dataset to 25.340 samples.

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/6_createDatasetsFromCSV.py'>--->Go to file for creating master and satellite datasets<---</a>

Server Links: 
sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/negatives_master.csv
sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/negatives_satellite.csv
sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/positives_master.csv
sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/positives_satellite.csv
sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/patentX.zip

<a href='https://github.com/julian-risch/patent-indexing/blob/master/pipeline/7_createDatasets.py'>--->Go to file for generating train/validation/test splits (global dataset using all positive and negative samples)<---</a>

Server Links: sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/patentmatch_train.zip

 
<a href='tba'>--->Go to file for generating train/validation/test splits (variation one)<---</a>

Server Links: sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/patentmatch_train_balanced.zip

<a href='tba'>--->Go to file for generating train/validation/test splits (variation two)<---</a>
  
Server Links: sftp://172.20.11.11/mnt/data/datasets/patents/patent_matching/patentmatch_train_ultrabalanced.zip



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

# Additional Remarks
Though only the EP full-text data for text analytics dataset was utilized to keep focus, we started with several more datasets that were uploaded and examined in Elasticsearch. They are listed and described below.

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
