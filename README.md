# patent-indexing
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
