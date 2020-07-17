#!/usr/bin/env python3

#1 Lese Patentnummer und Paragraphen
# frage elasticsearch, ob wir paragraphentext für patentnummer haben, falls ja - füge text in neue spalte ein
# falls nein, frage für alle equivalents of paragraphentext indexiert, falls ja - füge text in neue spalte ein
# falls nein, vermerke "not available"
# falls fehler geworfen, vermerke "error during retrieval"

import pandas as pd
from elasticsearch import Elasticsearch

csv_path_data = "./frame_v2.csv"
#csv_path_equivalents="./equivalents.csv"
es = Elasticsearch(hosts=['http://172.16.64.23:9200/'])


def elasticsearch_request_getDnum(citation_id):
    return {
  "query": {
    "bool": {
      "filter": [
        {
          "ids": {
            "values": [
              citation_id
            ]
          }
        }
      ]
    }
  }
}

def elasticsearch_request_getParagraphText(application_number, application_category):
    return {
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "application_number": application_number
          }
        },
        {
          "term": {
            "application_category": application_category
          }
        }
      ]
    }
  }
}


def getPatentDetails(citation_id):

    response = es.search(index='ep_patent_citations', body=elasticsearch_request_getDnum(citation_id))
    print(response)
    try:
        dnum = response['hits']['hits'][0]['_source']['dnum']
        docNumber= response['hits']['hits'][0]['_source']['doc-number']
        patentCountry= response['hits']['hits'][0]['_source']['country']
        patentCategory = response['hits']['hits'][0]['_source']['kind']
    except:
        return "not found"

    return dnum, docNumber, patentCountry, patentCategory

def dataframeToDict(dataframe, dictionary):

    for index,entry in dataframe.iterrows():
        id_list = entry["equivalent_patents"].strip('][').split(', ')
        clean_id_list = []
        for value in id_list:
            clean_id_list.append(value.replace("\'", ""))

        dictionary[entry["patent_id"]] = clean_id_list

    return dictionary


def getParagraphText(dnum, application_category, paragraphs):

    # Falls mehrere Treffer (versch. Kategorien, nimm ersten Treffer)

    response = es.search(index='ep_patent_applications', body=elasticsearch_request_getParagraphText(dnum, application_category))

    try:
        paragraph_field = response['hits']['hits'][0]['_source']['description']
    except:
        return "not found"

    extracted_paragraph = ""
    for paragraph in paragraphs:
        found_paragraph_position_start = paragraph_field.find("<p id=\"p"+ ("%04d" % int(paragraph)) +"\" num=\""+("%04d" % int(paragraph)) + "\">")
        found_paragraph_position_end = paragraph_field.find("</p>",found_paragraph_position_start)+3
        extracted_paragraph = extracted_paragraph +" " + paragraph_field[found_paragraph_position_start:found_paragraph_position_end]

    return extracted_paragraph


if __name__ == '__main__':
    dataframe_data = pd.read_csv(csv_path_data, header=0)
    #dataframe_equivalents = pd.read_csv(csv_path_equivalents, header=0)
    equivalentPatents_dictionary = {}

    #print(dataframe_equivalents.head())

    citation_patent_docNumber_column = []
    citation_patent_dnum_column = []
    citation_patent_country_column = []
    #citation_patent_category = []
    claim_number_column = []
    extracted_paragraph_column = []
    paragraph_number_list_column = []
    used_citation_id_column = []
    application_claim_text_column = []
    patent_application_id_column = []
    patent_citation_id_column = []

    #equivalentPatents_dictionary = dataframeToDict(dataframe_equivalents, equivalentPatents_dictionary)

    for index, row in dataframe_data.iterrows():
        citation_id = row["patent_citation_id"]
        result_tuples = getPatentDetails(citation_id)

        #patent citation id is not found
        if(result_tuples == "not found"):
            print("Elasticsearch Citation ID Response failed or empty")
            continue

        citation_patent_dnum_column.append(result_tuples[0])
        citation_patent_docNumber_column.append(result_tuples[1])
        citation_patent_country_column.append(result_tuples[2])
        #citation_patent_category.append(result_tuples[3])

### EP Equivalents Processing
        '''
        ep_equivalents = []
        if (equivalentPatents_dictionary.get("EP"+result_tuples[1], "empty") != "empty"):
            equivalent_list = equivalentPatents_dictionary.get("EP"+result_tuples[1])
            ep_equivalents = [s.replace("EP","") for s in equivalent_list if "EP" in s]
            if (result_tuples[2] == "EP"):
                ep_equivalents = [result_tuples[1]] + ep_equivalents
        elif result_tuples[2] == "EP" :
            ep_equivalents = [result_tuples[1]]



        if (len(ep_equivalents) == 0):
            del citation_patent_dnum_column[-1]
            del citation_patent_docNumber_column[-1]
            del citation_patent_country_column[-1]
            #del citation_patent_category[-1]
            print("No EP-citations available")
            continue
        '''

        desired_paragraphs_list = []
        if(len(row['paragraphes'].strip("]["))==0):
            desired_paragraphs_list = []
        else:
            desired_paragraphs_list = row['paragraphes'].strip("][").split(",")

        extracted_paragraph = "not found"

        if result_tuples[2] == "EP" and len(desired_paragraphs_list) > 0:
            extracted_paragraph = getParagraphText(result_tuples[1],result_tuples[3],desired_paragraphs_list)

        if extracted_paragraph != "not found":
            used_citation_id_column.append(result_tuples[1])
            extracted_paragraph_column.append(extracted_paragraph)
            claim_number_column.append(row['application_claim_number'])
            paragraph_number_list_column.append(desired_paragraphs_list)
            application_claim_text_column.append(row['application_claim_text'])
            patent_application_id_column.append(row['patent_application_id'])
            patent_citation_id_column.append(citation_id)


        if (extracted_paragraph == "not found"):
            del citation_patent_dnum_column[-1]
            del citation_patent_docNumber_column[-1]
            del citation_patent_country_column[-1]
            #del citation_patent_category[-1]
            print("Elasticsearch Paragraph Extraction Response failed or no patent available")




    column_data = {'patent_application_id' : patent_application_id_column,
                   'patent_citation_id':patent_citation_id_column,
                   'original_cited_patent_dnum': citation_patent_dnum_column,
                   'original_cited_patent_docNumber': citation_patent_docNumber_column,
                   'original_cited_patent_country': citation_patent_country_column,
                   'application_claim_text': application_claim_text_column,
                   'application_claim_number': claim_number_column,
                   'extracted_paragraph_column_of_citation': extracted_paragraph_column,
                   'actual_used_patent_dnum_application_number_for_paragraph_extraction' : used_citation_id_column,
                   'novelty_reducing_paragraphs' : paragraph_number_list_column

                   }
    df = pd.DataFrame(data=column_data, columns=['patent_application_id',
                                                 'patent_citation_id',
                                                 'original_cited_patent_dnum',
                                                 'original_cited_patent_docNumber',
                                                 'original_cited_patent_country',
                                                 'application_claim_text',
                                                 'application_claim_number',
                                                 'extracted_paragraph_column_of_citation',
                                                 'actual_used_patent_dnum_application_number_for_paragraph_extraction',
                                                 'novelty_reducing_paragraphs'])
    df.to_csv("./frame_v3_withoutEquivalents.csv",index=False)