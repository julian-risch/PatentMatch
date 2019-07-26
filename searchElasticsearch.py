#!/usr/bin/env python3

from elasticsearch import Elasticsearch
from elasticsearch import helpers
import pandas as pd


def query_citation_id(citation_entry):
    return {
  "query": {
    "ids": {
      "values": [
        citation_entry
      ]
    }
  }
}

def process_hits(response, column_id_pa, column_cit_srprt, column_category_P, column_category_A , column_category_D, column_category_Y , column_category_L , column_category_O , column_category_T , column_category_E , column_category_X):
    all_response_patent_applications = response.get('hits').get('hits')
    for element in all_response_patent_applications:
        element_id_pa = element.get('_id')
        for citation_id in element.get('_source').get('citation_ids'):
            column_id_pa.append(element_id_pa)
            column_cit_srprt.append(citation_id)
            response_citation = es.search(index='ep_patent_citations', body=query_citation_id(citation_id),size=10000, filter_path=['hits.total.value', 'hits.hits'])
            response_citation_entry = response_citation.get('hits').get('hits')[0].get('_source')
            column_category_P.append(response_citation_entry.get('category_P') != None)
            column_category_A.append(response_citation_entry.get('category_A') != None)
            column_category_D.append(response_citation_entry.get('category_D') != None)
            column_category_Y.append(response_citation_entry.get('category_Y') != None)
            column_category_L.append(response_citation_entry.get('category_L') != None)
            column_category_O.append(response_citation_entry.get('category_O') != None)
            column_category_T.append(response_citation_entry.get('category_T') != None)
            column_category_E.append(response_citation_entry.get('category_E') != None)
            column_category_X.append(response_citation_entry.get('category_X') != None)



es = Elasticsearch(hosts=['http://172.16.64.23:9200/'])

query_amount_citation_ids_over_all_patent_applications = {
  "query": {
    "exists": {
      "field": "citation_ids"
    }
  }
}

response = es.search(index='ep_patent_applications', body=query_amount_citation_ids_over_all_patent_applications, scroll='2m')
print(response)

# Get the scroll ID
sid = response.get('_scroll_id')

scroll_size = len(response['hits']['hits'])

#Gehe jedes zurückgelieferte Ergebnis durch und speichere Patent Application ID und dazugehörige Citation IDs, sowie informationen aus citation IDs
column_id_pa = []
column_cit_srprt = []
column_category_P = []
column_category_A = []
column_category_D = []
column_category_Y = []
column_category_L = []
column_category_O = []
column_category_T = []
column_category_E = []
column_category_X = []

process_hits(response, column_id_pa, column_cit_srprt, column_category_P, column_category_A , column_category_D, column_category_Y , column_category_L , column_category_O , column_category_T , column_category_E , column_category_X)

while scroll_size > 0:
    "Scrolling..."
    response = es.scroll(scroll_id=sid, scroll='2m')

    # Process current batch of hits
    process_hits(response, column_id_pa, column_cit_srprt, column_category_P, column_category_A, column_category_D,
                 column_category_Y, column_category_L, column_category_O, column_category_T, column_category_E,
                 column_category_X)

    # Update the scroll ID
    sid = response['_scroll_id']

    # Get the number of results that returned in the last scroll
    scroll_size = len(response['hits']['hits'])




column_data = {'ID_PA': column_id_pa,
    'ID_CIT_SRPRT': column_cit_srprt,
    'CATEGORY_P': column_category_P,
    'CATEGORY_A': column_category_A,
    'CATEGORY_D':column_category_D ,
    'CATEGORY_Y': column_category_Y ,
    'CATEGORY_L': column_category_L ,
    'CATEGORY_O':column_category_O,
    'CATEGORY_T':column_category_T,
    'CATEGORY_E': column_category_E,
    'CATEGORY_X': column_category_X}

#ID Patent Application, ID Citation des Searchreports, Kategorie(n) der Citation des Searchreports (Boolean true= 1 Eintrag oder mehr, false=kein Eintrag)
# ID_PA, ID_CIT_SRPRT, CATEGORY_P, CATEGORY_A, CATEGORY_D ,CATEGORY_Y ,CATEGORY_L ,CATEGORY_O ,CATEGORY_T ,CATEGORY_E ,CATEGORY_X
# Erstelle Pandas Dataframe in genanntem Format
df = pd.DataFrame(data = column_data,columns=['ID_PA', 'ID_CIT_SRPRT', 'CATEGORY_P', 'CATEGORY_A', 'CATEGORY_D', 'CATEGORY_Y' ,'CATEGORY_L' ,'CATEGORY_O', 'CATEGORY_T' ,'CATEGORY_E' ,'CATEGORY_X'])
pd.set_option('display.max_columns', None)  # or 1000
pd.set_option('display.max_rows', None)  # or 1000
pd.set_option('display.max_colwidth', 30)  # or 199
print(df)

