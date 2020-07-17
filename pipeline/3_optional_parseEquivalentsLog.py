#!/usr/bin/env python3
import pandas as pd

csv_paths = ["./logOPS1.out", "./logOPS2.out", "./logOPS3.out","./logOPS4.out","./logOPS5.out","./logOPS6.out","./logOPS7.out",]
counter_error= 0
counter_success= 0


def process_csv(path):
    global counter_error
    global counter_success
    with open(path) as f:
        lines = f.readlines()

    follow_up_next_line = False
    current_id = ""
    for line in lines:
        if follow_up_next_line is True:
            equivalents_list = (line.replace("[", "").replace("]", "").replace("'", "").rstrip()).split(
                ", ")  # .append(current_id)
            equivalents_list.append(current_id)
            equivalents_list = list(dict.fromkeys(equivalents_list))
            column_id.append(current_id)
            column_equivalents.append(equivalents_list)
            follow_up_next_line = False
            counter_success = counter_success + 1
        elif str.__contains__(line, "unexpected response or no equivalents :"):
            counter_error = counter_error + 1
        else:
            current_id = line.rstrip()
            follow_up_next_line = True


if __name__ == '__main__':

    column_id = []
    column_equivalents = []

    for path in csv_paths:
        process_csv(path)

    print("success: " + str(counter_success))
    print("error or non-eu/us-patent: " + str(counter_error))
    data = {'patent_id': column_id, 'equivalent_patents': column_equivalents}
    df = pd.DataFrame(data=data)
    pd.set_option('display.max_columns', None)  # or 1000
    pd.set_option('display.max_rows', None)  # or 1000
    pd.set_option('display.max_colwidth', 30)  # or 199
    df.head()
    df.to_csv("./equivalents_1_bis_7.csv", index=False)

