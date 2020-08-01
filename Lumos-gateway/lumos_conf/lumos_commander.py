import requests
import sys

#hue
#python lumos_commander st 0x3f03001f


if len(sys.argv) is 1:
    print("you need to input args.")

elif sys.argv[1] == "st":
    URL = 'http://192.168.0.205'
    params = {'start_record': 'x', sys.argv[2]: 'x'}
    response = requests.get(URL, params=params)
    print(response.status_code)

elif sys.argv[1] == "end":
    URL = 'http://192.168.0.205'
    params = {'end_record': 'x'}
    response = requests.get(URL, params=params)
    print (response.status_code)