#!/usr/bin/env python3

from elasticsearch import Elasticsearch
from elasticsearch import helpers
import pandas as pd


def query_exist_claim():
    return {
  "query": {
    "bool": {
      "filter": [
        {
          "exists": {
            "field": "citation_ids"
          }
        },
        {
          "exists": {
            "field": "claims"
          }
        }
      ]
    }
  }
}

def query_citation_id(citation_entry):
    return {
  "query": {
    "bool": {
      "filter": [
        {
          "exists": {
            "field": "category_X"
          }
        },
        {
          "ids": {
            "values": [
              citation_entry
            ]
          }
        }
      ]
    }
  }
}

def process_hits(es, response, patent_application_id_column, patent_citation_column, application_claim_number_column,application_claim_text_column,related_passages_against_claim_column,category_column):
    print(response)
    all_response_patent_applications = response.get('hits').get('hits')
    for element in all_response_patent_applications:
        patent_application_id = element.get('_id')
        claims_text_raw = element.get('_source').get('claims')
        max_claim = int(claims_text_raw.split("<claim id=\"c-en-00")[-1][:2])

        #process claims_text in a list of claims and corresponding text
        #speichere jeweils in die korrespondierenden columns ab
        for claim in range(1,max_claim+1):
            for citation_id in element.get('_source').get('citation_ids'):
                print(citation_id)
                response_citation = es.search(index='ep_patent_citations', body=query_citation_id(citation_id),size=10000)
                print(response_citation)
                try:
                    #exception list index out of range thrown if citation id does not contain any category_X contents
                    response_citation.get('hits').get('hits')[0].get('_source')
                except:
                    continue
                response_rel_claims = response_citation.get('hits').get('hits')[0].get('_source').get('category_X')
                response_rel_passage = response_citation.get('hits').get('hits')[0].get('_source').get('rel-passage_X')
                if claim in response_rel_claims:
                    try:
                        application_claim_text_column.append(claims_text_raw.split(
                            "<claim id=\"c-en-00" + "{:02d}".format(claim) + "\" num=\"00" + "{:02d}".format(
                                claim) + "\">")[1].split("</claim>")[0])
                    except:
                        #occurs if claim tag is malformed, e.g <claim id="c-en-0001" num=""> instead of <claim id="c-en-0001" num="0001">
                        #entry is then discarded
                        print("Discarded Claim. ID: " + str(claim) +", Patent Application ID: "+ str(patent_application_id))
                        continue
                    patent_application_id_column.append(patent_application_id)
                    patent_citation_column.append(citation_id)
                    application_claim_number_column.append(claim)
                    related_passages_against_claim_column.append(response_rel_passage)
                    category_column.append("X")



def main():
    patent_application_id_column = []
    patent_citation_column = []
    application_claim_number_column = []
    application_claim_text_column = []
    related_passages_against_claim_column = []
    category_column = []


    es = Elasticsearch(hosts=['http://172.16.64.23:9200/'])

    response = es.search(index='ep_patent_applications', body=query_exist_claim(), scroll='2m')
    print(response)
    # Get the scroll ID
    sid = response.get('_scroll_id')

    scroll_size = len(response['hits']['hits'])

    process_hits(es, response, patent_application_id_column, patent_citation_column, application_claim_number_column,application_claim_text_column,related_passages_against_claim_column,category_column)


    while scroll_size > 0:
        "Scrolling..."
        response = es.scroll(scroll_id=sid, scroll='2m')

        # Process current batch of hits
        process_hits(es, response, patent_application_id_column, patent_citation_column, application_claim_number_column,application_claim_text_column,related_passages_against_claim_column,category_column)


        # Update the scroll ID
        sid = response['_scroll_id']

        # Get the number of results that returned in the last scroll
        scroll_size = len(response['hits']['hits'])




    column_data = {'patent_application_id': patent_application_id_column,
    'patent_citation_id': patent_citation_column,
    'application_claim_number': application_claim_number_column,
    'application_claim_text': application_claim_text_column,
    'related_passages_against_claim':related_passages_against_claim_column ,
    'category': category_column}

    print(column_data)

    df = pd.DataFrame(data = column_data,columns=['patent_application_id','patent_citation_id','application_claim_number','application_claim_text','related_passages_against_claim','category'])
    pd.set_option('display.max_columns', None)  # or 1000
    pd.set_option('display.max_rows', None)  # or 1000
    pd.set_option('display.max_colwidth', 30)  # or 199
    df.to_csv("./frame.csv")




if __name__ == '__main__':
    main()


