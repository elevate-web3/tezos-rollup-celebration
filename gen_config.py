#!/usr/bin/env python

import json
import sys
import getopt


def main(argv):
    # Parse command line arguments
    row_arg = ""
    col_arg = ""
    opts, args = getopt.getopt(argv, "r:c:", ["rows=", "columns="])
    for opt, arg in opts:
        if opt == "-r":
            row_arg = arg
        elif opt == "-c":
            col_arg = arg
    rows = int(row_arg)
    cols = int(col_arg)
    # Generate a config file for the nodes
    config_list = []
    for row in range(0, rows):
        for col in range(0, cols):
            i = row*cols + col
            port = 1200 + i
            config = {
                "host": "localhost",
                "port": port,
                "row": row,
                "column": col
            }
            json_config = json.dumps(config)
            config_list.append(json_config)

    with open("config.json", "w") as f:
        json_string = "[" + ",".join(config_list) + "]"
        f.write(json_string)


if __name__ == "__main__":
    main(sys.argv[1:])
