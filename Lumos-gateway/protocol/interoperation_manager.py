import os
import urllib2
import zlib
from .. import models
from .. import tnetstring, flow_format_compat
from .. import main
from . import dynamic_learner
import re


class InteroperationManager:

    _path = "/usr/local/lib/python2.7/dist-packages/mitmproxy/lumos_conf/interops"
    _inter_op_conf = []
    _called = False
    _ran_interoperation = False
    debug = False

    def __init__(self):
        pass


    @staticmethod
    def get_ran_interoperation():
        return InteroperationManager._ran_interoperation

    # purpose: this method is to perform interoperation among IoT devices.
    @staticmethod
    def interoperation_runner(cflow      # type:models.HTTPFlow
                              ):
        # TODO: this method sends control packet or check conditions.
        # Thus, it can build http request and monitor request from interoperation_conf.dump by serialized pickle.
        # hard case - if a interoperation that includes multiple conditions, this module tracks
        # each condition at run-time. To handle that case, we builds a check table
        # that represents appearances of target condition.

        #InteroperationManager._inter_op_conf.append(cflow.get_state())
        # if InteroperationManager._called is False:
        #    InteroperationManager.interoperation_conf_save()

        # Netflix & Wemo
        if InteroperationManager.find_matchedurl(cflow.request.pretty_url,3):
            if cflow.request.body.find("target") != -1:
                InteroperationManager.just_send_request(dynamic_learner.DynamicLearner.Pairs[5].actualpacket[4])



        pass

    @staticmethod
    def find_matchedurl(url,  # type: str
                        idx     # type int
                        ):
        pattern = re.compile(dynamic_learner.DynamicLearner.Pairs[idx].urisig)
        m = pattern.match(url)
        if m:
            return True
        return False

    @staticmethod
    def interoperation_conf_save():
        InteroperationManager._called = True
        f = open(InteroperationManager._path + "/interops.bin", 'w+b')
        tnetstring.dump(InteroperationManager._inter_op_conf, f)
        f.close()

    @staticmethod
    def interoperation_conf_load(currentflow     # type: models.HTTPFlow
                                 ):
        if os.path.exists(InteroperationManager._path + "/interops.bin"):
            f = open(InteroperationManager._path + "/interops.bin", "rb")
            InteroperationManager._inter_op_conf = None
            InteroperationManager._inter_op_conf = tnetstring.load(f)
            f.close()

            for i in InteroperationManager._inter_op_conf:

                try:
                    data = flow_format_compat.migrate_flow(i)
                except ValueError as e:
                    raise str(e)

                converted_flow = models.HTTPFlow.from_state(data)       # type: models.HTTPFlow
                InteroperationManager.send_request(converted_flow)

    @staticmethod
    def tuple_to_dic(ori):
        d = dict()
        for key, val in ori:
            d[key] = val
        return d


    @staticmethod
    def just_send_request(
                    req
                    ):

        # request.scheme + "://" + this.pretty_host(request) + port + request.path
        from . import SigUIPacketPairs
        flow_headers_dict_req = InteroperationManager.tuple_to_dic(req["request"]["headers"])

        newHeader = {}
        for key, value in flow_headers_dict_req.items():
            newHeader[key] = value

        #str(req["request"]["method"]) + " " + str(req["request"]["scheme"]) + "://"
                        #+ str(req["request"]["host"]) + str(req["request"]["path"])

        # TODO: we need to cover POST method case. None <-- keyvalue pair !!
        url = str(req["request"]["scheme"]) + "://"+ str(req["request"]["host"])+ ":" \
              + str(req["request"]["port"]) + str(req["request"]["path"])

        #.replace("54.192.150.165", newHeader["authority"])
        if req["request"]["method"] == "POST":
            req = urllib2.Request(url.replace("212", "18"), req["request"]["content"],
                                  headers=newHeader)
        elif req["request"]["method"] == "GET":
            req = urllib2.Request(url, None, headers=newHeader)
        urllib2.urlopen(req).read()

        return

    # this method is for analyzing response data.
    # for example, getting sensors' status
    @staticmethod
    def send_request(
            req     # type: models.HTTPFlow
    ):
        # request.scheme + "://" + this.pretty_host(request) + port + request.path

        newHeader = {}
        for key, value in req.request.headers.items():
            newHeader[key.replace(":", "")] = value

        # 54.192.150.165
        # TODO: we need to cover POST method case. None <-- keyvalue pair !!
        req = urllib2.Request(req.request.pretty_url.replace("54.192.150.165", newHeader["authority"]), None,
                              headers=newHeader)
        data = urllib2.urlopen(req).read()

        f = open("response.html", "wb")

        # TODO: we need to json convertor.
        if str(newHeader["accept-encoding"]).find("gzip") != -1:
            r_str = str(zlib.decompress(data, 16 + zlib.MAX_WBITS))
        else:
            r_str = str(data)

        f.write(r_str)
        f.close()

        return

