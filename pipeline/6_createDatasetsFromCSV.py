#!/usr/bin/env python3

import pandas as pd
import sys

csv_input_path_data = "../frame_v3_withoutEquivalents_negativeSamples.csv"
csv_output_path = "./"

def getParagraphFromText(paragraphsText,paragraphNumber):

    found_paragraph_position_start = paragraphsText.find("<p id=\"p"+ ("%04d" % int(paragraphNumber)) +"\" num=\""+("%04d" % int(paragraphNumber)) + "\">")
    found_paragraph_position_end = paragraphsText.find("</p",found_paragraph_position_start)+3
    extracted_paragraph = paragraphsText[found_paragraph_position_start:found_paragraph_position_end]

    return extracted_paragraph


if __name__ == '__main__':


    # Initialize rows for master file
    patent_application_id_master = []
    application_claim_text_master = []
    application_global_claim_id_master = []

    application_claim_number_master = []
    patent_searchReport_citation_id_master = []
    patent_searchReport_dnum_master = []
    patent_searchReport_docNumber_master = []
    patent_searchReport_country_master = []
    patent_searchReport_paragraphs_master = []

    #Initialize rows for satellite file
    application_global_claim_id_satellite = []
    application_claim_text_satellite = []
    patent_searchReport_paragraph_satellite = []
    patent_searchReport_paragraph_number_satellite = []

    # Read dataframe from csv
    data = pd.read_csv(csv_input_path_data, header=0)

    # Initialize counter for numbering claim_ids globally in a unique way
    counter_claim_id = -1

    for index, row in data.iterrows():
        counter_claim_id = counter_claim_id + 1
        desired_paragraphs_list = row['novelty_reducing_paragraphs'].replace("\"","").replace("'","").replace(" ","").strip("][").split(",")
        #print(desired_paragraphs_list)

        # Save information in master file columns
        patent_application_id_master.append(row['patent_application_id'])
        application_claim_text_master.append(row['application_claim_text'])
        application_global_claim_id_master.append(counter_claim_id)

        application_claim_number_master.append(row['application_claim_number'])
        patent_searchReport_citation_id_master.append(row['patent_citation_id'])
        patent_searchReport_dnum_master.append(row['original_cited_patent_dnum'])
        patent_searchReport_docNumber_master.append(row['original_cited_patent_docNumber'])
        patent_searchReport_country_master.append(row['original_cited_patent_country'])
        patent_searchReport_paragraphs_master.append(desired_paragraphs_list)

        for paragraph_number in desired_paragraphs_list:
            paragraph_text = getParagraphFromText(row["extracted_paragraph_column_of_citation"],paragraph_number)

            # Save information in satellite file columns
            application_global_claim_id_satellite.append(counter_claim_id)
            application_claim_text_satellite.append(row['application_claim_text'])
            patent_searchReport_paragraph_satellite.append(paragraph_text)
            patent_searchReport_paragraph_number_satellite.append(paragraph_number)

    # Save information into master file "master.csv"
    column_data_master = {"patent_application_id":patent_application_id_master,
        "application_claim_text":application_claim_text_master,
        "application_global_claim_id":application_global_claim_id_master,
        "application_claim_number" :application_claim_number_master,
        "patent_searchReport_citation_id":patent_searchReport_citation_id_master,
        "patent_searchReport_dnum":patent_searchReport_dnum_master,
        "patent_searchReport_docNumber":patent_searchReport_docNumber_master,
        "patent_searchReport_country":patent_searchReport_country_master,
        "patent_searchReport_paragraphs":patent_searchReport_paragraphs_master}

    df = pd.DataFrame(data=column_data_master)
    df.to_csv(csv_output_path + "master.csv", index=False)

    # Save information into satellite file "satellite.csv"
    column_data_satellite = {
    "application_global_claim_id":application_global_claim_id_satellite,
    "application_claim_text":application_claim_text_satellite,
    "patent_searchReport_paragraph":patent_searchReport_paragraph_satellite,
    "patent_searchReport_paragraph_number":patent_searchReport_paragraph_number_satellite}

    df = pd.DataFrame(data=column_data_satellite)
    df.to_csv(csv_output_path + "satellite.csv", index=False)

    sys.exit(0)

