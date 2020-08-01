from .. import flow_format_compat
from ..models import HTTPFlow
import json
from ..jsondiff import diff
import re
import random
import zlib


# SigUIPacketPairs class
urisig = ""
params = {}
humanreadable = []
hexid = []
isreq = None
actualpacket = []
unchangeable_res = None
unchangeable_req = None


class SigUIPacketPairs:
    def __init__(self):
        self.urisig = ""
        self.params = {}
        self.humanreadable = []
        self.hexid = []
        self.isreq = True
        self.actualpacket = []
        self.unchangeable_res = None
        self.unchangeable_req = None
        # TODO: we need a unchangeable_req

    @staticmethod
    def tuple_to_dic(ori):
        d = dict()
        for key, val in ori:
            d[key] = val
        return d

    # purpose: to find unchangable fields in response body
    def identifying_unchangeable(self):
        response_array = []
        for flow_state in self.actualpacket:
            r_str = None

            flow_headers_dict_req = SigUIPacketPairs.tuple_to_dic(flow_state["request"]["headers"])

            if flow_state["response"]["content"] == '':
                continue

            if str(flow_headers_dict_req["Content-Type"]).find("json") != -1:
                if flow_headers_dict_req.has_key("Accept-Encoding"):
                    if str(flow_headers_dict_req["Accept-Encoding"]).find("gzip") != -1:
                        r_str = str(zlib.decompress(flow_state["response"]["content"], 16 + zlib.MAX_WBITS))
                else:
                    r_str = str(flow_state["response"]["content"])

            # no response contents
            if r_str is None:
                continue

            if r_str.startswith("["):
                response_array.append(eval(r_str.replace("true", "True")))
            else:
                response_array.append(r_str.replace("true", "True"))

        # response data is 1
        if len(response_array) == 1:
            return

        # remove different structure
        response_array, diff_part = self.remove_different_structure(response_array)

        # calibrate diff_part
        diff_part = self.calibrate_diff_part(diff_part)

        # if nonchangeable part is not exist
        if diff_part == "{}":
            return

        # filtering unchangeable values
        response_array = self.identify_unchangeable_values(response_array, diff_part)

        forcheck = response_array[0]

        self.unchangeable_res = forcheck

        # do not test
        return

        # for test
        actual = [{'success': {'/lights/1/state/on': True}}, {'success': {'/lights/1/state/bri': 456}}]

        # print(diff(forcheck, actual))
        if self.check_changeable_fields(forcheck, diff(forcheck, actual)):
            # print("exact!")
            pass
        else:
            # print("not exact!")
            pass

    def calibrate_diff_part(self, diff_part):
        regex = re.compile("[0-9]+:")
        diff_part = diff_part.replace("'", "\"")

        m = regex.findall(diff_part)
        if m:
            for same_part in m:
                diff_part = diff_part.replace(same_part, "\"" + same_part.replace(":", "") + "\":")

        return diff_part

    def check_changeable_fields(self, json_entry, diff_json):
        diff_json = json.loads(json.dumps(diff_json))

        if type(diff_json) is list:
            for diff_entry in diff_json:
                for key, value in diff_entry.items():

                    if key.isdigit():
                        return self.check_changeable_fields(json.loads(json.dumps(json_entry[int(key)])),
                                                       json.loads(json.dumps(value)))
                    else:
                        if type(json_entry[str(key)]) is dict:
                            return self.check_changeable_fields(json_entry[key], json.loads(json.dumps(value)))
                        else:
                            # print(json_entry[key])
                            if json_entry[str(key)] == "lumos_changeable":
                                return True
                            else:
                                return False
        else:
            for key, value in diff_json.items():
                if key.isdigit():
                    return self.check_changeable_fields(json.loads(json.dumps(json_entry[int(key)])),
                                                        json.loads(json.dumps(value)))
                else:
                    if type(json_entry[key]) is dict:
                        return self.check_changeable_fields(json_entry[key], json.loads(json.dumps(value)))
                    else:
                        # print(json_entry[key])
                        if json_entry[key] == "lumos_changeable":
                            return True
                        else:
                            return False

    def identify_unchangeable_values(self, response_array, diff_part):
        diff_json = json.loads(diff_part)
        # print("type: " + str(type(diff_json)))
        # i = 0

        result_array = []
        for entry in response_array:
            res = {}
            self.put_random_value_into_changeable_values(eval(entry), diff_json, res)
            new_json = json.dumps(json.loads(entry))
            for changeable in res.keys():
                origin = res[changeable]
                new_json = new_json.replace(str(origin), str(changeable))

            result_array.append(json.loads(new_json))

        return result_array

    def put_random_value_into_unchangeable_values(self, json_entry, suffix):
        for key, value in json_entry.items():
            if type(json_entry[key]) is dict:
                # print("type: " + str(json_entry[key]))
                return self.put_random_value_into_unchangeable_values(json_entry[key], suffix)
            elif type(json_entry[key]) is list:
                return self.put_random_value_into_unchangeable_values(json.loads(json.dumps(json_entry[int(key)])),
                                                                 suffix)
            else:
                origin = str(value)
                if origin != "lumos_changeable":
                    newvalue = suffix + "_" + str(random.randrange(1, 10000))
                    return json_entry, str(json_entry).replace(str(origin), (newvalue))
                return json_entry, None
        pass

    def put_random_value_into_changeable_values(self, json_entry, diff_json, result_dict):
        for key, value in diff_json.items():
            if key.isdigit():
                self.put_random_value_into_changeable_values(json.loads(json.dumps(json_entry[int(key)])),
                                                               json.loads(json.dumps(value)), result_dict)
            else:
                if type(json_entry[str(key)]) is dict:
                    self.put_random_value_into_changeable_values(json_entry[key], json.loads(json.dumps(value)), result_dict)
                else:
                    origin = json.dumps(json_entry)
                    json_entry[str(key)] = "lumos_changeable"
                    result_dict[json.dumps(json_entry)] = origin.replace("{u", "{")
                    #return json_entry, origin.replace("{u", "{")

        return
    def iterate_json(self, dictionary, keys):
        if isinstance(dictionary, list):
            for entry in dictionary:
                self.iterate_json(entry, keys)
        else:
            for key, value in dictionary.items():
                keys.append(key)
                if isinstance(value, dict):
                    self.iterate_json(value, keys)
                    continue
        return

    def remove_different_structure(self, response_array):
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
