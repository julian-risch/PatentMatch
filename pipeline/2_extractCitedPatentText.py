#!/home/nialde/anaconda3/bin/python python

import pandas as pd
import nltk

csv_path = "./frame.csv"
testsaetze = "paragraph 0021 - paragraph 0023, paragraph [0024] - [0026], paragraphs 1, paragraphs [8], [0099] - [0100]" # es muss immer das wort paragraph davorstehen
testsatz2 = "* paragraphs [0016] - [0018],  [0040] - [0049],  [0087] - [0090] ** figures 1-5 *"



def desirable(tag):
    return (tag[0] in ["paragraph", "-", "["] or (tag[1] in ["CD"] and tag[0].isdigit()))

def syntax_right(tag_before_tag, tag):
    if tag[1] != "CD":
        return True
    else:
        return (tag[1] == "CD" and ("paragraph" in tag_before_tag[0]) or ("[" in tag_before_tag[0]))

def text_is_range(tag_before_tag, tag, tag_after_tag):
    return (tag_before_tag[1] == "CD" and tag[0] == "-" and tag_after_tag[1]=="CD")


def extract_paragraphs(text):
    tokens = nltk.word_tokenize(text.lower().replace("paragraphs","paragraph"))
    pos_tags = nltk.pos_tag(tokens)
    #print(pos_tags)
    # sortiere alle token aus, die nichts mit paragraph zu tun haben bzw. spaeter benoetigt werden
    pos_tags = [tag for tag in pos_tags if desirable(tag)]   # behalte wuenschenswerte token (siehe Funktion oben)
    pos_tags = [tag for tag_before_tag, tag in zip([("","")] + pos_tags[:-1],pos_tags) if syntax_right(tag_before_tag,tag) ] # behalte nur Nummern (CD), deren vorheriges wort "paragraph(s)" oder "[" lautet
    pos_tags = [tag for tag in pos_tags if ((not "paragraph" in tag[0]) and (not "[" in tag[0]))] # alle zahlen beziehen sich nun auf paragraphen. deswegen kann das "paragraphen" keyword geloescht werden
    pos_tags_ranges = [list(range(int(tag_before_tag[0]),int(tag_after_tag[0])+1)) for tag_before_tag, tag, tag_after_tag in zip([("","")] + pos_tags[:-1],pos_tags, pos_tags[1:]+[("","")]) if text_is_range(tag_before_tag, tag, tag_after_tag)] # bekomme alle zahlen, die als range 0003-0008 geschrieben sind. Da paragraphen nicht doppelt vorkommen muessen, koennen wir hier sets benutzen
    pos_tags_numbers_only = [int(tag[0]) for tag in pos_tags if tag[0] != "-"]

    end_result_paragraph_numbers = pos_tags_numbers_only
    for range_list in pos_tags_ranges:
        end_result_paragraph_numbers = end_result_paragraph_numbers + range_list

    end_result_paragraph_numbers = list(set(end_result_paragraph_numbers))
    return end_result_paragraph_numbers






# Falls vorherige Faelle nicht eindeutig, logge "nicht eindeutig, uebersprungen" und ueberspringe Fall


    # Extrahiere Paragraphen Angaben (bzw. andere Komponenten) mithilfe von spezialisierten Parser
    # Ueberfuehre extrahierte Angaben in einheitliches Format
    # Fuege geladener Tabelle neue Spalte mit Paragraphenangaben hinzu im Format Liste von Paaren:
    # [[paragraph, 12][paragraph, 3],...]
# speichere neue CSV mit zusaetzlichen Angaben ab (noindex option aktivieren)

if __name__ == '__main__':
    # Lade CSV
    # CSV Format: ,patent_application_id,patent_citation_id,application_claim_number,application_claim_text,related_passages_against_claim,category
    #print(extract_paragraphs(testsatz2))

    #dataframe = pd.read_csv(csv_path, header=0, skiprows =range(1, 1000001), nrows = 1500000)
    #dataframe = pd.read_csv(csv_path, header=0, skiprows = (list(range(1,2000001))+list(range(2109670,2109680))), nrows = 500000)
    dataframe = pd.read_csv(csv_path, header=0, skiprows = range(1,3000001), nrows = 503743)

    print(len(dataframe))
    related_passages_against_claim_iterator = dataframe["related_passages_against_claim"].astype(str)

    # Fuer jede Reihe betrachte "related_passages_against_claim", falls vorhanden und extrahiere paragraphes
    column_paragraphes = []
    column_occured_paragraph_keyword = []
    for text in related_passages_against_claim_iterator:
        print(len(column_paragraphes))
        print(text)
        # print(text)
        #print(extract_paragraphs(text))
        column_paragraphes.append(extract_paragraphs(text))
        if "paragraph" in text.lower():
            column_occured_paragraph_keyword.append("true")
        else:
            column_occured_paragraph_keyword.append("false")
    dataframe["paragraphes"] = column_paragraphes
    dataframe["paragraph_keyword_found"] = column_occured_paragraph_keyword


    #pd.set_option('display.max_columns', None)  # or 1000
    #pd.set_option('display.max_rows', None)  # or 1000
    #pd.set_option('display.max_colwidth', 49)  # or 199
    #print(dataframe[["related_passages_against_claim","paragraphes"]])
    dataframe.to_csv("./frame_v2.csv")
