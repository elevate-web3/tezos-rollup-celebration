#! python3
import json


def main():
    config_list = []
    for row in range(0, 20):
        for col in range(0, 5):
            i = row*5 + col
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
    main()
