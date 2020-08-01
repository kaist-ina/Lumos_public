from jsondiff import diff
import json
import re
import random


def main():

    json1 = {"body":{"homeDevice":{"lastUpdateTime":"20181112060844537","hDId":"NIKE15111600184:72:07:1B:15:63"}},"header":{"resultMsg":"성공_KR","resTime":"20181112060844537","serverUId":"UID0100032685","reqTime":"20181112150842526","resultCode":"22000000","deviceId":"be8a9ce3d9b8dd2c"}}
    json2 = {"body":{"homeDevice":{"lastUpdateTime":"20181112060827699","hDId":"NIKE15111600184:72:07:1B:15:63"}},"header":{"resultMsg":"성공_KR","resTime":"20181112060827699","serverUId":"UID0100032685","reqTime":"20181112150826790","resultCode":"22000000","deviceId":"be8a9ce3d9b8dd2c"}}

    print(diff(json1, json2))
    exit(0)


    # json sample initialization
    response_array = []
    json1 = [{'success': {'lights/1/state/on': True}}, {'success': {'lights/1/state/bri': 128}}]
    json2 = [{'success': {'lights/1/state/on': True}}, {'success': {'lights/1/state/bri': 254}}]

    # for test
    actual = [{'success': {'lights/1/state/on': False}}, {'success': {'lights/1/state/bri': 253}}]

    response_array.append(json1)
    response_array.append(json2)
    # this will be removed due to the difference structure
    response_array.append([[{'success': {'lights/1/state/on': True}}], {'success': {'lights/1/state/bri': 254}}])

    # remove different structure
    response_array, diff_part = remove_different_structure(response_array)

    # calibrate diff_part
    regex = re.compile("[0-9]+:")
    diff_part = diff_part.replace("'", "\"")

    m = regex.findall(diff_part)
    if m:
        for same_part in m:
            diff_part = diff_part.replace(same_part, "\"" + same_part.replace(":", "") + "\":")

        # print("diff_part: " + diff_part)
    # end of calibrate

    # convert into json object
    # print(json.loads(json.dumps(diff_part)))

    # filtering unchangeable values
    response_array = identify_unchangeable_values(response_array, diff_part)

    # print("json entry")
    # for entry in response_array:
    #     print(entry)
    # print("end")

    # unchangeable field의 비교는 response_array의 요소 1개만 써도된다.
    forcheck = response_array[0]

    # 여기서의 diff를 ui-packet sig pair에 unchangeable fields 변수에 집어 넣어둔다.
    # 이후에 interoperation manager에서 사용해야함
    # unchangeable fields에 대한 matching test code
    print(diff(forcheck, actual))
    if check_changeable_fields(forcheck, diff(forcheck, actual)):
        print("exact!")
    else:
        print("not exact!")


def check_changeable_fields(json_entry, diff_json):
    diff_json = json.loads(json.dumps(diff_json))

    for key, value in diff_json.items():
        if key.isdigit():
            return check_changeable_fields(json.loads(json.dumps(json_entry[int(key)])), json.loads(json.dumps(value)))
        else:
            if type(json_entry[key]) is dict:
                return check_changeable_fields(json_entry[key], json.loads(json.dumps(value)))
            else:
                # print(json_entry[key])
                if json_entry[key] == "lumos_changeable":
                    return True
                else:
                    return False


def identify_unchangeable_values(response_array, diff_part):
    diff_json = json.loads(diff_part)
    # print("type: " + str(type(diff_json)))
    # i = 0

    result_array = []
    for entry in response_array:
        changeable, origin = put_random_value_into_changeable_values(entry, diff_json)
        new_json = json.loads(json.dumps(eval(str(entry).replace(str(origin), str(changeable)).replace("'", "\""))))
        result_array.append(eval(str(new_json)))

    return result_array


def put_random_value_into_unchangeable_values(json_entry, suffix):
    for key, value in json_entry.items():
        if type(json_entry[key]) is dict:
            # print("type: " + str(json_entry[key]))
            return put_random_value_into_unchangeable_values(json_entry[key], suffix)
        elif type(json_entry[key]) is list:
            return put_random_value_into_unchangeable_values(json.loads(json.dumps(json_entry[int(key)])),
                                                           suffix)
        else:
            origin = str(value)
            if origin != "lumos_changeable":
                newvalue = suffix + "_" + str(random.randrange(1, 10000))
                return json_entry, str(json_entry).replace(str(origin), (newvalue))
            return json_entry, None
    pass


def put_random_value_into_changeable_values(json_entry, diff_json):
    for key, value in diff_json.items():
        if key.isdigit():
            return put_random_value_into_changeable_values(json.loads(json.dumps(json_entry[int(key)])), json.loads(json.dumps(value)))
        else:
            if type(json_entry[key]) is dict:
                return put_random_value_into_changeable_values(json_entry[key], json.loads(json.dumps(value)))
            else:
                origin = str(json_entry)
                json_entry[key] = "lumos_changeable"
                return json_entry, origin


def iterate_json(dictionary, keys):
    if isinstance(dictionary, list):
        for entry in dictionary:
            iterate_json(entry, keys)
    else:
        for key, value in dictionary.items():
            keys.append(key)
            if isinstance(value, dict):
                iterate_json(value, keys)
                continue
    return


def remove_different_structure(response_array):
    new_array = []
    diff_part = ""
    first = response_array[0]
    new_array.append(first)
    for res_entry in response_array[1:]:
        result = diff(eval(first), eval(res_entry))
        # print(result)
        if ("insert" or "delete") not in str(result):
            # print ("append: " + str(res_entry))
            diff_part = '%s' % result
            new_array.append(res_entry)

    # print ("len : " + str(len(new_array)))
    return new_array, diff_part


if __name__ == '__main__':
    main()




# actual = [{'success':{'lights/1/state/on':False}}, {'success':{'lights/1/state/bri':253}}]
#
# result1 = []
# result2 = []
# iterate_json(json.loads(json.dumps(json1)), result1)
# iterate_json(json.loads(json.dumps(json1)), result2)
#
# #다른 구조의 json은 버린다? -> 필요한가?
#
# if result1 == result2:
#     #print(diff(json1, json2))
#     result1 = []
#
#     #the key order of changeable fields
#     iterate_json(diff(json1, json2), result1)
#     #print("learned: " +str(result1))
#     print("learned: " + str(diff(json1, json2)))
#
#     result2 = []
#     iterate_json(diff(json1, actual), result2)
#     # print("actual: " + str(result2))
#     print("actual: " + str(diff(json1, actual)))
#
#     if result1 == result2:
#         print("exact!")
#     else:
#         print("not exact!")