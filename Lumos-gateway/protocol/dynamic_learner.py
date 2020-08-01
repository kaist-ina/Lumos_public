# Author: jeongmin Kim
# Purpose: to learn actual packet learning.
# comment:
import json
import re
import zlib
import os
import pickle
from .. import models
from typing import List
from .. import tnetstring, flow_format_compat
from .SigUIPacketPairs import SigUIPacketPairs


def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    reverse = dict((value, key) for key, value in enums.iteritems())
    enums['reverse_mapping'] = reverse
    return type(str("Enum"), (), enums)


POS = enum('URI', 'HEADER', 'BODY')
ORIG = enum('REQUEST', 'RESPONSE')


class DynamicLearner:
    Pairs = []  # type: List[SigUIPacketPairs]
    recorded_flows = []     # type: List[models.HTTPFlow]
    target_res = ""
    target_human = ""
    run_learning = False
    current_pair = None

    def __init__(self):
        # self.Pairs.append(self.SigUIPairs())
        # self.Pairs[0].uri = "abcd"
        # self.Pairs[0].params = {"key", "value"}
        pass

    @staticmethod
    def print_pairs():
        i = 0
        for pair in DynamicLearner.Pairs:
            f = open("/usr/local/lib/python2.7/dist-packages/mitmproxy/lumos_conf/siguipair/" + "print_learningdata_" +str(i) + ".txt", 'w')
            f.write("Pair information" + "\n")

            f.write("uri sig: " + pair.urisig + "\n")
            f.write("human: " + str(pair.humanreadable) + "\n")
            f.write("hexid: " + str(pair.hexid) + "\n")
            f.write("Request packets" + "\n")

            j = 0
            for req in pair.actualpacket:
                f.write("Req[" + str(j) + "]\n")
                f.write("URL" + "\n")
                f.write(str(req["request"]["method"]) + " " + str(req["request"]["scheme"]) + "://"
                        + str(req["request"]["host"]) + str(req["request"]["path"]) + "\n")
                f.write("HEADER" + "\n")
                f.write(str(req["request"]["headers"]) + "\n")
                f.write("CONTENT" + "\n")
                f.write(str(req["request"]["content"]) + "\n")

                if req.has_key("urlencoded_form"):
                    f.write("URL ENCODED FORM" + "\n")
                    f.write(str(req["urlencoded_form"]) + "\n")
                f.write("=============================" + "\n")
                j = j + 1

            j = 0
            for res in pair.actualpacket:
                f.write("Res[" + str(j) + "]" + "\n")
                f.write("HEADER" + "\n")
                f.write(str(req["response"]["headers"]) + "\n")
                f.write("CONTENT" + "\n")
                flow_headers_dict_req = SigUIPacketPairs.tuple_to_dic(res["request"]["headers"])

                if res["response"]["content"] == '':
                    continue

                if str(flow_headers_dict_req["Content-Type"]).find("json") != -1:
                    if flow_headers_dict_req.has_key("Accept-Encoding"):
                        if str(flow_headers_dict_req["Accept-Encoding"]).find("gzip") != -1:
                            r_str = str(zlib.decompress(res["response"]["content"], 16 + zlib.MAX_WBITS))
                    else:
                        r_str = str(res["response"]["content"])
                else:
                    r_str = str(res["response"]["content"])
                # else:
                #     r_str = str(res["response"]["content"])

                f.write(r_str + "\n")
                f.write("=============================" + "\n")
                j = j + 1

            f.close()
            i = i + 1
        return

    @staticmethod
    def manual_learning_save_one_flow(flow        # type: models.HTTPFlow
                        ):
        DynamicLearner.target_human = "wemo_plugon"
        matchedpair = DynamicLearner.find_matchedurl(flow.request.pretty_url)
        if matchedpair is not None:
            DynamicLearner.current_pair = matchedpair
            res_dict = flow.get_state()
            res_dict["urlencoded_form"] = flow.request.urlencoded_form
            DynamicLearner.current_pair.actualpacket.append(res_dict)

    @staticmethod
    def manual_learning_identify_unchangeable():
        #DynamicLearner.current_pair.identifying_unchangeable()
        # in this point, self.Pairs should be serialized.
        DynamicLearner.save_pairs()

    @staticmethod
    def start_learning(flow     # type: models.HTTPFlow
                       ):

        # 0 is start_record
        # 1 is end_record
        if DynamicLearner.check_packet_types(flow) is 0:
            DynamicLearner.run_learning = True
        elif DynamicLearner.check_packet_types(flow) is 1:
            DynamicLearner.target_res = ""
            DynamicLearner.run_learning = False

        if DynamicLearner.run_learning:
            matchedpair = DynamicLearner.find_matchedurl(flow.request.pretty_url)
            if matchedpair is not None:
                DynamicLearner.current_pair = matchedpair
                return DynamicLearner.find_matchedparam(flow, matchedpair)
        else:
            if DynamicLearner.current_pair:

                # TODO: we need a learning module for request data but not now.
                # actual learning for response data.
                DynamicLearner.current_pair.identifying_unchangeable()

                #in this point, self.Pairs should be serialized.
                DynamicLearner.save_pairs()
                DynamicLearner.current_pair = None

        return None

    # purpose - to serialize Pairs[]
    @staticmethod
    def save_pairs():
        _path = "/usr/local/lib/python2.7/dist-packages/mitmproxy/lumos_conf/siguipair"
        pickle.dump(DynamicLearner.Pairs, open(_path + "/Pairs.bin", 'wb'))


    @staticmethod
    def load_pairs():
        _path = "/usr/local/lib/python2.7/dist-packages/mitmproxy/lumos_conf/siguipair/Pairs.bin"
        file = open(_path, 'rb')
        DynamicLearner.Pairs = pickle.load(file)
        # DynamicLearner.Pairs[2].humanreadable = ["video_stream_control_overate_lock"]
        file.close()

    @staticmethod
    def load_sig_ui_pairs():

        # Finished: we need to implement this method
        # Purpose: to load SigUI pairs.
        # SigUI pairs is exist for each apps.
        # Path - lumos_conf/siguipair/AppName/sigui.json

        siguipairs = {}

        # implementation
        for (path, dir, files) in os.walk("/home/appff/mitmproxy_code/lumos_conf/siguipair"):
            for filename in files:
                ext = os.path.splitext(filename)[-1]
                if ext == '.json':
                    filedata = open(path+"/"+filename, "r").read()
                    parsedjson = json.loads(filedata)
                    siguipairs[path[path.rindex("/")+1:]] = parsedjson

        # Finished: convert json to siguipath for each appname
        for key in siguipairs:
            pairobj = SigUIPacketPairs()
            jsondata = siguipairs.get(key)

            for pair in jsondata["pair"]:

                # finding body sig in Sig value

                for sig in pair["Sig"]:
                    if "BODY" in sig:
                        for splited in sig.split(" "):
                            if "BODY" in splited:
                                continue
                            else:
                                if "RESPONSEBODY" in splited:
                                    pairobj.isreq = False

                                keyvalue = splited.split("=")
                                pairobj.params[keyvalue[0]] = keyvalue[1]
                    else:
                        # POST / GET skip
                        pairobj.urisig = sig.split(" ")[1]

                for hexid in pair["UI"]:
                    pairobj.hexid.append(hexid)
                pairobj.humanreadable.append(pair["Human_readable_id"])

            DynamicLearner.Pairs.append(pairobj)
        pass

    # please check whether self.extract_data_from_req is right or not.
    @staticmethod
    def check_packet_types(flow     # type: models.HTTPFlow
                           ):

        if "host" not in flow.request.data.headers:
            return

        flow_uri = flow.request.data.headers["host"] + flow.request.data.path

        # TODO: we need to check parameter to identify a packet for dynamic learner.
        # This may have a bug due to modification of extract_data_from_req
        lumos_operation = DynamicLearner.extract_data_from_req(POS.BODY, flow)
        if lumos_operation is None:
            return -1
        elif "start_record" in lumos_operation:
            # packet learning start

            # in this point, getting target res-id
            DynamicLearner.target_res = lumos_operation[1]
            return 0
        elif "end_record" in lumos_operation:
            # packet learning end
            return 1

        # I think this might not be needed.
        elif "before_interaction" in lumos_operation:

            # TODO: getting resid's value
            # have to get res id
            #resid = DynamicLearner.extract_data_from_req(POS.BODY, "resid", flow)

            return 2
        else:
            return -1

    @staticmethod
    def find_matchedurl(url   # type: str
                     ):
        # TODO: in this point, we need to access UISig pairs
        # It just tests regex matcher methods.

        for pair in DynamicLearner.Pairs:  # type: dynamic_learner.SigUIPairs
            if pair.isreq is True:
                if DynamicLearner.target_res is not "":
                    if DynamicLearner.target_res in pair.hexid:
                        pattern = re.compile(pair.urisig)
                        m = pattern.match(url)
                        if m:
                            return pair
                elif DynamicLearner.target_human is not "":
                    if DynamicLearner.target_human in pair.humanreadable:
                        pattern = re.compile(pair.urisig)
                        m = pattern.match(url)
                        if m:
                            return pair
            else:
                continue

        return None

    @staticmethod
    def extract_data_from_req(orig_pos,
                              flow          # type: models.HTTPFlow
                              ):
        # TODO: if req body is formed as JSON, we need to parse that JSON data.
        ret = list()
        if orig_pos == POS.HEADER:
            return flow.request.headers.get_all()
        elif orig_pos == POS.BODY:
            if str(flow.request.method) == "GET":
                #key_value_list = str(flow.request.get_decoded_content()).split('&')
                if flow.request.query is None:
                    return None
                key_value_list = flow.request.query
                for k, v in key_value_list:
                    ret.append(k)
                return ret
            elif str(flow.request.method) == "PUT" or str(flow.request.method) == "POST":
                encodeddata = flow.request.urlencoded_form

                if encodeddata is None:
                    return None

                # checking whether this can be converted to json or not.
                jsonkv = DynamicLearner.is_json(encodeddata)
                if jsonkv is not None:
                    # TODO: debugging this method -> check ret is right or not
                    DynamicLearner.iterate_json(jsonkv, ret)
                    return ret
                else:
                    key_value_list = flow.request.urlencoded_form
                    for key in key_value_list:
                        key_value = str(key)
                        ret.append(key_value)
                    return ret
        return None

    @staticmethod
    def is_json(jsoncandidate):
        json_object = None
        try:
            for value in jsoncandidate:
                if type(value) is tuple:
                    for value2 in value:
                        json_object = json.loads(value2)
        except ValueError as e:
            return json_object
        return json_object

    @staticmethod
    def iterate_json(dictionary, keys):
        for key, value in dictionary.items():
            if isinstance(value, dict):
                DynamicLearner.iterate_json(value, keys)
                continue
            keys.append(key)
        return

    # comments: orig_key is a list.
    # return value is list.
    @staticmethod
    def extract_data_from_resp(orig_pos, orig_key, flow):

        if orig_pos == POS.HEADER:
            return str(flow.response.headers.get_all(orig_key[0]))
        elif orig_pos == POS.BODY:
            # extract json from Resp. body
            if str(flow.response.headers.get_all("content-type")).find("json") != -1:
                if str(flow.response.headers.get_all("content-encoding")).find("gzip") != -1:
                    r_str = str(zlib.decompress(flow.response.content, 16 + zlib.MAX_WBITS))
                else:
                    r_str = str(flow.response.content)

                try:
                    js = json.loads(r_str)

                except ValueError:
                    return None

                return DynamicLearner.track_json(js, orig_key, 0, list())

        return None

    # purpose: for extracting for all key, values
    @staticmethod
    def extract_data_from_resp_all(orig_pos, orig_key, flowstate):

        try:
            data = flow_format_compat.migrate_flow(flowstate)
        except ValueError as e:
            raise str(e)

        flow = models.HTTPFlow.from_state(data)  # type: models.HTTPFlow

        if orig_pos == POS.HEADER:
            return str(flow.response.headers.get_all(orig_key[0]))
        elif orig_pos == POS.BODY:
            # extract json from Resp. body
            if str(flow.response.headers.get_all("content-type")).find("json") != -1:
                if str(flow.response.headers.get_all("content-encoding")).find("gzip") != -1:
                    r_str = str(zlib.decompress(flow.response.content, 16 + zlib.MAX_WBITS))
                else:
                    r_str = str(flow.response.content)

                try:
                    js = json.loads(r_str)

                except ValueError:
                    return None

                return js

        return None

    @staticmethod
    def track_json(js, key_list, idx, result):
        r = js.get(key_list[idx])
        if str(type(r)).find("dict") != -1:
            DynamicLearner.track_json(r, key_list, idx + 1, result)
        elif str(type(r)).find("list") != -1:
            DynamicLearner.track_list(r, key_list, idx + 1, result)
        else:
            result.append(r)
        return result

    @staticmethod
    def track_list(lst, key_list, idx, result):
        for l in lst:
            r = l.get(key_list[idx])
            if str(type(r)).find("dict") != -1:
                DynamicLearner.track_json(r, key_list, idx + 1, result)
            elif str(type(r)).find("list") != -1:
                DynamicLearner.track_list(r, key_list, idx + 1, result)
            else:
                result.append(r)
        return result

    @staticmethod
    def find_matchedparam(
            flow,     # type: models.HTTPFlow
            pair      # type: DynamicLearner.SigUIPairs
    ):
        matched_key = DynamicLearner.extract_data_from_req(POS.BODY, flow)

        if matched_key is None:
            return None

        if matched_key[0] is '':
            return None

        # if param is None, return this pair promptly.
        if len(pair.params) is 0:
            return pair

        for key in pair.params:
            if key not in matched_key:
                return None

        # if all of the keys are included in matched_key, in this point, we do not receive response yet.
        # pair.actualpacket = flow.get_state()
        return pair
