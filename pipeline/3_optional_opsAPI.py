#!/usr/bin/env python3

import base64
import requests
import xml.etree.ElementTree as ET
import pandas as pd
from elasticsearch import Elasticsearch
import time

consumer_key = ""
consumer_secret_key = ""
token_url = 'https://ops.epo.org/3.2/auth/accesstoken'
request_url = "http://ops.epo.org/3.2/rest-services/published-data/publication/EPODOC/equivalents"
csv_path = "./frame.csv"
es = Elasticsearch(hosts=['http://172.16.64.23:9200/'])


def getAccessToken():

    payload = "grant_type=client_credentials"
    usrPass = consumer_key+":"+consumer_secret_key
    b64Val = base64.b64encode(bytes(usrPass, 'utf-8'))
    header = {'authorization':"Basic %s" %  b64Val.decode('utf-8'),"content-type":"application/x-www-form-urlencoded"}
    request_token = requests.post(token_url, headers=header, data=payload)
    access_token = request_token.json()['access_token']

    return access_token


def getEquivalents(number):

    access_token = getAccessToken()

    equivalent = []
    payload = number
    header = {'authorization': "Bearer %s" % access_token,"content-type":"text/plain"}
    request_equivalent = requests.post(request_url, headers=header, data=payload)
    response = request_equivalent.text
    #print(response)

    try:
        root = ET.fromstring(response)
        for inquiry_result in (list(root.iter("{http://ops.epo.org}equivalents-inquiry"))[0].iter("{http://ops.epo.org}inquiry-result")):
            for publication_reference in inquiry_result.iter("{http://www.epo.org/exchange}publication-reference"):
                for document_id in publication_reference.iter("{http://www.epo.org/exchange}document-id"):
                    for doc_number in document_id.iter("{http://www.epo.org/exchange}doc-number"):

                        equivalent.append(doc_number.text)
        print(equivalent)
    except:
        print("unexpected response or no equivalents :" + number)
    return equivalent

def query_patent_citation_country_docNumber(id):
    return {
  "query": {
    "bool": {
      "filter": [
        {
          "ids": {
            "values": [
              id
            ]
          }
        }
      ]
    }
  }
}

def elasticSearch_process(id):
    response_citation = es.search(index='ep_patent_citations', body=query_patent_citation_country_docNumber(id), size=10000)
    #print(response_citation)
    try:
        # exception list index out of range thrown if citation id does not contain any category_X contents
        country = response_citation.get('hits').get('hits')[0].get('_source').get('country')
        docNumber = response_citation.get('hits').get('hits')[0].get('_source').get('doc-number')
        print(country+docNumber)
        return country+docNumber
    except:
        return "error es_response"

def getPatentCitationIds(csv_path):
    list_of_patent_citation_ids = []
    list_of_equivalents_lists = []

    dataframe = pd.read_csv(csv_path, header=0,skiprows =range(1, 2767211) )
    patent_citation_id_iterator = dataframe["patent_citation_id"]
    #print(len(list(patent_citation_id_iterator.unique())))
    for id in patent_citation_id_iterator.unique():
        list_of_patent_citation_ids.append(id)
        citation_identifier = elasticSearch_process(id)
        if citation_identifier is not "error es_response":
            equivalents_list = getEquivalents(citation_identifier)
        else:
            equivalents_list = ["error es_response"]
        list_of_equivalents_lists.append(equivalents_list)
        time.sleep(6.0)
    return pd.DataFrame({'patent_citation_id':list_of_patent_citation_ids, 'equivalents':list_of_equivalents_lists})


if __name__ == '__main__':
    dataframe_equivalents = getPatentCitationIds(csv_path)
    #dataframe_equivalents.to_csv("./equivalents.csv", index=False)

