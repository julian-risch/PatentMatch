import pandas as pd
from sklearn.model_selection import train_test_split
import numpy as np


def execute():
    path = "/mnt/data/datasets/patents/patent_matching"
    positives =pd.read_csv(path+"/positives_satellite.csv",header=0,dtype={'application_claim_text': str,'patent_searchReport_paragraph':str})
    negatives =pd.read_csv(path+"/negatives_satellite.csv",header=0,dtype={'application_claim_text': str,'patent_searchReport_paragraph':str})
    sample_size=1.0

    positives = positives[['application_claim_text', 'patent_searchReport_paragraph']]
    positives["label"]="1"

    positives = positives.rename(columns={"application_claim_text": "text", "patent_searchReport_paragraph": "text_b"})
    negatives = negatives[['application_claim_text', 'patent_searchReport_paragraph']]
    negatives["label"]="0"
    negatives = negatives.rename(columns={"application_claim_text": "text", "patent_searchReport_paragraph": "text_b"})

    allSamples = positives.append(negatives).dropna()

    # Remove "</p" at the end of paragraphs
    allSamples['text_b'] = allSamples['text_b'].str.replace('<\/p', '', regex=True)
    allSamples['text'] = allSamples['text'].str.replace('<\/p', '', regex=True)
    # Remove everything written in "<...>"
    allSamples['text_b'] = allSamples['text_b'].str.replace('\<.+?\>', '', regex=True)
    allSamples['text'] = allSamples['text'].str.replace('\<.+?\>', '', regex=True)
    # Remove --->
    allSamples['text_b'] = allSamples['text_b'].str.replace('--\>', '', regex=True)
    allSamples['text'] = allSamples['text'].str.replace('--\>', '', regex=True)
    
    # Remove remaining "
    allSamples['text_b'] = allSamples['text_b'].str.replace('\"', '', regex=True)
    allSamples['text'] = allSamples['text'].str.replace('\"', '', regex=True)

    #For the rest: Keep only letter, numbers, whitespace, dot
    allSamples['text_b'] = allSamples['text_b'].str.replace('[^A-Za-z0-9\s.]+', '', regex=True)
    allSamples['text'] = allSamples['text'].str.replace('[^A-Za-z0-9\s.]+', '', regex=True)

    #Remove leading whitespaces
    allSamples['text_b'].replace("^\s", '', regex=True, inplace=True)
    allSamples['text'].replace("^\s", '', regex=True, inplace=True)

    #Remove space characters
    allSamples['text_b'].replace('\B\s+|\s+\B', '', regex = True, inplace=True)
    allSamples['text'].replace('\B\s+|\s+\B', '', regex=True, inplace=True)

    #Replace empty strings with Nan, so dropna removes it
    allSamples['text_b'].replace("^[\s]*$", np.nan, regex=True, inplace=True)
    allSamples['text'].replace("^[\s]*$", np.nan, regex=True, inplace=True)

    #train,test,dev=train_test_dev_split(allSamples,0.4,0.3,0.3)
    allSamples = allSamples.sort_values(by=['text']).dropna()
    train, test_dev = train_test_split(allSamples,test_size=0.2, shuffle=False)
    test,dev = train_test_split(test_dev,test_size=0.5,shuffle=False)

    #Shuffle and sample from data
    train = train.sample(frac=sample_size)
    test = test.sample(frac=sample_size)
    dev = dev.sample(frac=sample_size)

    print("Check for intersection values:")
    print("Train in Test")
    print((train['text'].isin(test['text'])).value_counts())
    print("Train in Dev")
    print((train['text'].isin(dev['text'])).value_counts())
    print("Test in Dev")
    print((test['text'].isin(dev['text'])).value_counts())


    train.to_csv(path+"/train.tsv", sep="\t", index=False)
    test.to_csv(path+"/test.tsv", sep="\t", index=False)
    dev.to_csv(path+"/dev.tsv", sep="\t", index=False)


if __name__ == '__main__':
    execute()
