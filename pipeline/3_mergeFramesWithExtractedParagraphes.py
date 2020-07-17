#!/usr/bin/env python3

import pandas as pd


if __name__ == '__main__':
    all_frames = []
    all_filenames = ["./frame_v2_1.csv","./frame_v2_2.csv","./frame_v2_3.csv","./frame_v2_4.csv","./frame_v2_5.csv","./frame_v2_6.csv","./frame_v2_7.csv"]

    for filename in all_filenames:
      df = pd.read_csv(filename, index_col=None, header=0, usecols = ['patent_application_id','patent_citation_id','application_claim_number','application_claim_text','related_passages_against_claim','category','paragraphes','paragraph_keyword_found'])
      all_frames.append(df)

    merged_frame = pd.concat(all_frames, axis=0, ignore_index=True)

    print(merged_frame.head())
    merged_frame.to_csv("./frame_v2.csv", index=False)
