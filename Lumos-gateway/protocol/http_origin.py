from __future__ import (absolute_import, print_function, division)

import sys
import traceback
from operator import truediv

import six

#WOOMADE
import re
import urllib2
from . import IoT_Rule

from netlib import tcp
from netlib.exceptions import HttpException, HttpReadDisconnect, NetlibException
from netlib.http import Headers, Request, Response

from h2.exceptions import H2Error

from .. import utils
from ..exceptions import HttpProtocolException, ProtocolException
from ..models import (
    HTTPFlow,
    HTTPRequest,
    HTTPResponse,
    make_error_response,
    make_connect_response,
    Error,
    expect_continue_response
)

from ..models.connections import ClientConnection, ServerConnection

from .base import Layer, Kill


# Code by Byungkwon Choi *Start*
#############################################################################################
import json
import pdb
import zlib
import socket
import urlparse
from netlib.odict import ODict
from os.path import expanduser
import subprocess
from six.moves import urllib
import os
#from netlib import utils

SIG_PATH = "/home/appff/mitmproxy/proxy/"


# It might be STATUS value.
# 0 = standard
# 1 = begin normal record
# 2 = end normal record
# 3 = start trigger record
# 4 = stop trigger record
# 5 = multiple trigger record start
# 6 = multiple trigger record end
# 7 = ready for multiple trigger record

STATUS = 0
Vid = -1

#Interoperability
Interoperability = True

#one light only
Ligton=False

#august Authkey
augustAuth = None

def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    reverse = dict((value, key) for key, value in enums.iteritems())
    enums['reverse_mapping'] = reverse
    return type('Enum', (), enums)

def MyLog(string):
    file1 = open(SIG_PATH+ "/a", 'a')
    file1.write(str(string) + "\n")
    file1.flush()
    file1.close()

def Print_Request(flow):
    MyLog("[Request]")
    MyLog(" - first_line_format: "+str(flow.request.data.first_line_format));
    MyLog(" - method: "+str(flow.request.data.method));
    MyLog(" - scheme: "+str(flow.request.data.scheme));
    MyLog(" - host: "+str(flow.request.data.host));
    MyLog(" - port: "+str(flow.request.data.port));
    MyLog(" - path: "+str(flow.request.data.path));
    MyLog(" - http_version: "+str(flow.request.data.http_version));
    if flow.request.query:
        MyLog(" - Query: " + str(flow.request.query))
    MyLog(" - headers(type:"+str(type(flow.request.data.headers))+"): "+str(flow.request.data.headers));
    #MyLog(" - content: " + str(flow.request.get_decoded_content()));
    MyLog(" - content: " + str(flow.request.urlencoded_form));

def ODICT_Matching(odict1, odict2):
    for t in odict1:
        key = t[0]
        value = t[1]
        result = False
        for v in odict2[key]:
            if v == value:
                result = True
                break
        if not result:
            return False

    return True

POS = enum('URI','HEADER','BODY')
ORIG = enum('REQUEST','RESPONSE')

class Request_Structure:
    def __init__(self):
        self.method = str() # get, post, etc
        self.scheme = str() # http, https
        self.URI = list()
        self.Headers = dict()
        self.Body = ODict()
        self.child = list()
    
    def Set_Child(self, p):
        self.child.append(p)

    def Get_Child(self):
        return self.child

    def Set_Method(self, m):
        self.method = m

    def Set_Scheme(self, s):
        self.scheme = s

    def Get_URI(self):
        return "".join(self.URI)

    def Add_URI(self, uri):
        for item in uri:
            self.URI.append(item)

    def Add_Headers(self, key, value):
        self.Headers[key] = value

    def Add_Body(self, key, value):
        self.Body.add(key, value)

    def Print(self):
        MyLog("[Request Signature]")
        MyLog(" - Method : " + self.method)
        MyLog(" - Scheme : " + self.scheme)
        MyLog(" - URI    : " + self.Get_URI())
        MyLog(" - Headers: " + str(self.Headers))
        MyLog(" - Body   : " + str(self.Body))

class Dependency_Data:
    def __init__(self):
        self.parent_idx = int() # parent flow's index
        self.orig_req_resp = ORIG.REQUEST # parent - position 1 (req/resp)
        self.orig_pos = POS.HEADER # parent - position 2 (header/body)
        self.orig_key = list() # parent - key list (or just a key)
        self.req_pos = POS.HEADER # child - position 1 (uri/header/body)
        self.req_detail_pos = str() # child - position 2 (key)
        self.stat = bool() # status (True: already loaded, False: need to load from parent)

    def Set_Parent_Idx(self, idx):
        self.parent_idx = idx

    def Set_Orig_Req_Resp(self, r):
        self.orig_req_resp = r

    def Set_Orig_Pos(self, r):
        self.orig_pos = r

    def Set_Orig_Key(self, l):
        self.orig_key = l

    def Add_Orig_Key(self, k):
        self.orig_key.append(k)

    def Set_Req_Pos(self, r):
        self.req_pos = r

    def Set_Req_Detail_Pos(self, s):
        self.req_detail_pos = s

    def Set_Status(self, r):
        self.stat = r

    def Print(self):
        MyLog("Parent Index: " + str(self.parent_idx))
        MyLog("Parent Position 1 (Req./Resp.)  : " + str(ORIG.reverse_mapping[self.orig_req_resp]))
        MyLog("Parent Position 2 (Hdr/Body)    : " + str(POS.reverse_mapping[self.orig_pos]))
        MyLog("Parent Position 3 (Key list)    : " + str(self.orig_key))
        MyLog(" Child Position 1 (URI/Hdr/Body): " + str(POS.reverse_mapping[self.req_pos]))
        MyLog(" Child Position 2 (Key)         : " + str(self.req_detail_pos))
        MyLog("Status: " + str(self.stat))

class Request_Track_Dependency:
    def __init__(self):
        self.cnt = int()
        self.dp_list = list() # list of class 'Dependency_Data'

    def Set_Cnt(self, i):
        self.cnt = i

    def Add_DP_List(self, dp):
        self.dp_list.append(dp)

    def Decrease_Cnt(self):
        self.cnt = self.cnt - 1

    def Increase_Cnt(self):
        self.cnt = self.cnt + 1
#WOOMADE
class Trigger_Info:
    def __init__(self):
        self.name = str()
        self.key = str()
        self.numofvisibility = -1

    def Set_Name(self, i):
        self.name = i
 
    def Set_Key(self, i):
        self.key = i

    def Set_num(self, i):
        self.numofvisibility = i

#Prefetched flow(Resp.)
prefetched_flow = list()

#Signatures
Request_Signatures = list()
#WOOMADE
Trigger_Infos = list()
Possible_Infos = list()
Possible_record = list()
normal_record = list()
trigger_flows = list()
normal_flows = list()
SigFilter_result = list()
Rules = list()
now_rule = -1

checker_path = "/home/appff/mitmproxy/proxy/checker"
begin_path = "/home/appff/mitmproxy/proxy/begin"
flow_record = list()

Request_Track_DP = list() # list of class 'class Request_Track_Dependency'

#Request_URI_Signatures = list() # maybe not used
#Request_Header_Signatures = list() # maybe not used
#Request_Body_Signatures = list() # maybe not used

#Dependency Graph
Dependency_Graph = list()

# Dependency Graph Evaluation (DGE) *start*
#------------------------------------------------------------------------------#
# DGE_flag
#  - True : If DGE has been done
#  - False: If DGE has not been done yet
DGE_flag = False

# Return DGE (Dpendency Graph Evaluation) flag
def Check_DGE_Flag():
    global DGE_flag
    return DGE_flag

# Set DGE flag to True
def Set_DGE_True():
    global DGE_flag
    DGE_flag = True

# Set DGE flag to False
def Set_DGE_False():
    global DGE_flag
    DGE_flag = False

def DGE(flow):
    return


#------------------------------------------------------------------------------#
# Dependency Graph Evaluation (DGE) *end*



# Signatures load *start*
#------------------------------------------------------------------------------#
#WOOMADE Triger

#WOOMADE trigger
def Trigger_Start(now_trigger):
    global Trigger_Infos
    global Ligton

    #if i == -1:
    #    return
    #if i == 1 and Ligton:
    #    return
    #if i == 1:
    #    Ligton = True

    f = open(SIG_PATH + "Log", 'a')
    f.write(now_trigger.name + "\n")
    f.close()

    #This is from trigger_infos 
    MyLog("WebHook URL: " + "https://maker.ifttt.com/trigger/"+now_trigger.name+"/with/key/"+now_trigger.key)
    urllib2.urlopen("https://maker.ifttt.com/trigger/"+now_trigger.name+"/with/key/"+now_trigger.key)
    MyLog("https://maker.ifttt.com/trigger/"+now_trigger.name+"/with/key/"+now_trigger.key)

#WOOMADE PLEASE
def Load_Trigger_Info():
    global Trigger_Infos
    global SIG_PATH

    for i in range(0,len(Request_Signatures)):
        f = open(SIG_PATH + "trigger_infos_" + str(i), 'r')
        while True:
            line = f.readline().strip()
            if not line: break
            sp = str(line).split(' ') # split by space ' '
                        
            #example: play_music key (webhooks key) webhook API param
            Trigger_Infos[i].Set_Name(sp[0])
            Trigger_Infos[i].Set_Key(sp[1])
            Trigger_Infos[i].Set_num(sp[2])

        f.close()

def Load_Visibility():
    global SIG_PATH
    global now_rule
    global Rules

    i=0
    MyLog("Rule path : " + SIG_PATH + "Rule"+str(i))
    while os.path.exists(SIG_PATH + "Rule"+str(i)):
        f = open(SIG_PATH + "Rule"+str(i), 'r')
        j = 0
        now_rule = now_rule + 1

        while True:
            line = f.readline().strip()
            if not line: break
            #semantic
            if j==0:
                rule = IoT_Rule.Rule(line)
            #sig
            elif j==1:
                # we need to split again due to HTTP method.
                HTTP_method_and_Sig = line.split(' ');
                rule.method = HTTP_method_and_Sig[0];
                rule.signature = HTTP_method_and_Sig[1];
            #values
            elif j==2:
                sp = str(line).split(' ')  # split by space ' '
                for val in sp:
                    rule.values.append(val)
            else:
                rule.body=line
            j=j+1
        Rules.append(rule)
        i=i+1
        f.close()


#WOOMADE PLEASE
def Load_Possible_Info():
    global Possible_Infos
    global Possible_record
    global Request_Signatures
    global SIG_PATH

    f = open(SIG_PATH + "Possible_Sig", 'r')
    while True:
        line = f.readline().strip()
        if not line: break
        sp = str(line).split(' ', 1) # split by space ' '
        Sig_Parse = urlparse.urlparse(sp[0])
        
        temp = Request_Structure()
        temp.Set_Method("ALL")
        temp.Add_URI(sp[0:])
        temp.Set_Scheme(Sig_Parse.scheme)

        Possible_Infos.append(temp)
    
    f.close()
    for i in range(0,len(Possible_Infos)):
        MyLog("Infos: "+Possible_Infos[i].Get_URI())
        Possible_record.append(0)
        normal_record.append(0)
    
    #Check relationships
    for i in range(0,len(Possible_Infos)):
        for j in range(0,len(Possible_Infos)):
            pattern = re.compile(Possible_Infos[i].Get_URI())
            m = pattern.match(Possible_Infos[j].Get_URI())
            if (m and i != j):
                Possible_Infos[i].Set_Child(j)
                MyLog("parent "+str(i)+" : "+ Possible_Infos[i].Get_URI())
                MyLog("child "+str(j)+" : "+ Possible_Infos[j].Get_URI())
                MyLog("-----")

    for i in range(0,len(Possible_Infos)):
        for j in Possible_Infos[i].Get_Child():
            MyLog("Now "+str(i) +" - "+ str(j))

#This method is called first.
def Load_Request_Sig():
    global Request_Signatures
    global Request_Track_DP
    global Dependency_Graph
    global Trigger_Infos
    global SIG_PATH

    # 1. Initialization
    p = subprocess.Popen(["wc", "-l", SIG_PATH + "requests"], stdout=subprocess.PIPE)
    output, err = p.communicate()
    line_no = int(output.split()[0])

    Request_Signatures = [Request_Structure() for i in range(line_no)]
    Request_Track_DP = [Request_Track_Dependency() for i in range(line_no)]
    Dependency_Graph = [list() for i in range(line_no)]
    Trigger_Infos = [Trigger_Info() for i in range(line_no)]

    # 2. Read request signatures
    f = open(SIG_PATH + "requests", 'r')
    for i in range(line_no):
        line = f.readline().strip()
        sp = str(line).split() # split by space ' '
        #t = Request_Structure()
        #t.method = sp[0]
        #t.URI.append(sp[1])
        Sig_Parse = urlparse.urlparse(sp[1])

        Request_Signatures[i].Set_Method(sp[0])
        Request_Signatures[i].Add_URI(sp[1:])
        Request_Signatures[i].Set_Scheme(Sig_Parse.scheme)

        MyLog(i)
        MyLog(Request_Signatures[0])
        MyLog(Request_Signatures[0].method)
        MyLog(Request_Signatures[0].URI)

        MyLog(Request_Signatures[1])
        MyLog(Request_Signatures[1].method)
        MyLog(Request_Signatures[1].URI)

        #.append(Request_Structure(sp[0], sp[1]))
        # append an empty component to the list
        #Request_Track_DP.append(Request_Track_Dependency()) 

        # append an empty list to Dependency_Graph list
        #Dependency_Graph.append(list())
        #Request_Signatures[i].Print()
    
    f.close()

#------------------------------------------------------------------------------#
# Signatures load *end*




# Request *start*
#------------------------------------------------------------------------------#
def Is_Req_Root(request):
    return False

def Does_Req_Have_Cookie(request):
    return False

def Construct_Req_Resp_List():
    return

#------------------------------------------------------------------------------#
# Request *end*


# Response *start*
#------------------------------------------------------------------------------#
#WOOMADE
def URI_Sig_Regex_Matching(flow, URI): 
    URI_Sig = "".join(URI)
    Sig_Parse = urlparse.urlparse(URI_Sig)

    req_host = flow.request.data.headers["host"]
    req_path = flow.request.data.path
    req_url = req_host + req_path
    Sig = Sig_Parse.netloc + Sig_Parse.path
    pattern = re.compile(Sig)
    m = pattern.match(req_url)

    MyLog("URI_sig =" + str(Sig))
    MyLog("flow =" + str(req_url))
 
    if m:
        MyLog("TRUE")
        return True
    
    MyLog("FALSE")
    return False
    #re.match(URI_Sig, flow)
 
def URI_Sig_Matching(flow, URI): 
    URI_Sig = "".join(URI)
    Sig_Parse = urlparse.urlparse(URI_Sig)

    req_host = flow.request.data.headers["host"]
    req_path = flow.request.data.path

    #MyLog("[Request]   Host: " + str(req_host))
    #MyLog("[Signature] Host: " + str(Sig_Parse.netloc))
    #MyLog("[Request]   Path: " + str(req_path))
    #MyLog("[Signature] Path: " + str(Sig_Parse.path))

    if str(Sig_Parse.netloc) == str(req_host):
        if str(Sig_Parse.path) == str(req_path):
            #MyLog("URI Matching!")
            return True
    return False
    
# comments: HDR_Sig is a dictionary.
def Req_Header_Sig_Matching(flow, HDR_Sig): 
    for HDR_Key in HDR_Sig:
        if str(flow.request.data.headers).find(HDR_Key) == -1:
            return False
    #MyLog("Header Matching!")
    return True

# comments: Body_Sig is a dictionary.
def Req_Body_Sig_Matching(flow, Body_Sig): 
    #Print_Request(flow)
    for Body_Key in Body_Sig:
        #if str(flow.request.get_decoded_content()).find(Body_Key[0]) == -1:
        if str(flow.request.urlencoded_form).find(Body_Key[0]) == -1:
            return False
    #MyLog("Body Matching!")
    return True

#WOOMADE
def Trigger_Matching(flow):
    global Request_Signatures
    global Request_Track_DP
 
    for i in range(0, len(Request_Signatures)):
        #if Request_Track_DP[i].cnt == 0:
        if flow.request.data.method == Request_Signatures[i].method: 
            if URI_Sig_Regex_Matching(flow, Request_Signatures[i].URI):
#            if URI_Sig_Matching(flow, Request_Signatures[i].URI): 
#                if Req_Header_Sig_Matching(flow, Request_Signatures[i].Headers): 
#                    if Req_Body_Sig_Matching(flow, Request_Signatures[i].Body): 
                 return i
    return -1



def Request_vs_Signature(flow):
    global Request_Signatures
    global Request_Track_DP

    for i in range(0, len(Request_Signatures)):
        #if Request_Track_DP[i].cnt == 0:
        if flow.request.data.method == Request_Signatures[i].method: 
            if URI_Sig_Matching(flow, Request_Signatures[i].URI): 
                if Req_Header_Sig_Matching(flow, Request_Signatures[i].Headers): 
                    if Req_Body_Sig_Matching(flow, Request_Signatures[i].Body): 
                        return i
    return -1

def Req_Query_Matching(query1, query2):
    if not query1 and not query2:
        return True
    elif query1 and not query2:
        return False
    elif not query1 and query2:
        return False

    return ODICT_Matching(query1, query2)

def URI_Matching(flow1, flow2):
    # method
    if flow1.request.data.method != flow2.request.data.method:
        return False
    #MyLog("Methods are matched!")
    # scheme
    if flow1.request.data.scheme != flow2.request.data.scheme:
        return False
    #MyLog("Schemes are matched!")
    # host
    if flow1.request.data.headers["host"] != flow2.request.data.headers["host"]:
        #MyLog("[Flow1 Host] : " + str(flow1.request.data.headers["host"]))
        #MyLog("[Flow2 Host] : " + str(flow2.request.data.headers["host"]))
        return False
    #MyLog("Hosts are matched!")

    # path
    if not flow2.request.query:
        if flow1.request.data.path != flow2.request.data.path:
            #MyLog("[Flow1 Path] : " + str(flow1.request.data.path))
            #MyLog("[Flow2 Path] : " + str(flow2.request.data.path))
            return False
    else:
        parse1 = urlparse.urlparse(flow1.request.data.path)
        parse2 = urlparse.urlparse(flow2.request.data.path)

        if parse1.path != parse2.path:  
            #MyLog("[Flow1 Path] : " + str(flow1.request.data.path))
            #MyLog("[Flow2 Path] : " + str(flow2.request.data.path))
            return False

    #MyLog("Pathes are matched!")

    if not Req_Query_Matching(flow1.request.query, flow2.request.query): 
        return False
        #MyLog("Both of two requests have query!!")
        #Print_Request(flow1)
        #Print_Request(flow2)
            
    #MyLog("Queries are matched!")
    #MyLog("URI Matching!")
    return True


def Req_Header_Matching(flow1, flow2):
    hdr1 = flow1.request.data.headers
    hdr2 = flow2.request.data.headers

    for k,v in hdr1.fields:
        if hdr2[k] != v:
            return False

    #MyLog("Header Matching!")
    return True


def Req_Body_Matching(flow1, flow2):
    odict1 = flow1.request.urlencoded_form
    odict2 = flow2.request.urlencoded_form

    if not odict1 and not odict2:
        return True
    elif not odict1 and odict2:
        return False
    elif odict1 and not odict2:
        return False

    return ODICT_Matching(odict1, odict2)

def Is_Req_Target(flow):
    global prefetched_flow
    #MyLog(" - # of Prefetched flow: " + str(len(prefetched_flow)))
    for pre_flow in prefetched_flow:
        #MyLog("[Prefetched flow's Path] : " + str(pre_flow.request.data.path))
        if URI_Matching(flow, pre_flow):
            if Req_Header_Matching(flow, pre_flow):
                if Req_Body_Matching(flow, pre_flow):
                    flow.response = pre_flow.response
                    flow.response.timestamp_start = flow.request.timestamp_end
                    flow.response.timestamp_end = utils.timestamp()
                    return True
    return False

#WOOMADE

def Visibility_check(flow):
    global now_rule
    global Rules
    global STATUS
    global Vid

    MyLog("Visibility_now :" + str(now_rule))

    MyLog("STATUS =" + str(STATUS))

    #0 = normal
    if STATUS == 0:
        for i in range(0, now_rule+1):

           message = Rules[i].signature
           #MyLog("Sig " +message)
           #MyLog("values "+str(Rules[i].values))
           for val in Rules[i].values:
               #MyLog("val "+val)
               message=message.replace("(.*)", val, 1)

           # matcher=Rules[i].target_flow[0]
           # mat_host = matcher.request.data.headers["host"]
           # mat_path = matcher.request.data.path
           # req_host = flow.request.data.headers["host"]
           # req_path = flow.request.data.path
           URI = message
           #MyLog("Visbility URL :"+ URI)
           MyLog(Rules[i].method + "::" + flow.request.method + " " + flow.request.data.headers["host"]) 

           if Rules[i].method == flow.request.method and URI_Sig_Regex_Matching(flow, message):
               MyLog("URI Matching!")
               matcher_body=str(Rules[i].body)
               #JM - urlencoded_form is not always JSON body. It can occur an error. Fixed!
               flow_body =str(flow.request.get_decoded_content())
               MyLog("Flow_Body = "+str(flow_body))
               MyLog("mathcer_Body = "+str(matcher_body))
               if matcher_body=="-1":
                   Vid = i
                   return True
               if matcher_body == flow_body:
                   MyLog("Body also is matched!!")
                   Vid = i
                   return True
               #    sum =0
               #     for j in matcher_body.keys():
               #         for k in flow_body.keys():
               #             MyLog(j)
               #             MyLog(k)
               #             if j==k:
               #                 sum = sum +1
               #     if sum != len(matcher_body.keys())*len(flow_body.keys()):
               #         return False
               #     is_same = True
               #     for j in matcher_body.keys():
               #         for k in flow_body.keys():
               #             if matcher_body.get(j) != flow_body.get(k):
               #                 is_same=False
               #
               #     return is_same

    return False



def show_Visibility():
    global Interoperability
    global Trigger_Infos
    global Vid

    MyLog("Show Visibility start")
    MyLog("Vid "+str(Vid))
    f = open(SIG_PATH + "Log", 'a')
    if Interoperability :
       for i in Trigger_Infos:
           MyLog("name " + i.name)
           MyLog("numofV " + i.numofvisibility)
           if i.numofvisibility == str(Vid):
               Trigger_Start(i)

    else:
        f.write(Rules[Vid].Rule_info+"\n")


    f.close()


def is_start_check():
    global STATUS
    global now_rule
    global Rules

    if os.path.exists(checker_path):

        #init record
        if STATUS == 0 :

            MyLog("start")

            num = 0
            info = ''
            boola = False
            f = open(checker_path, 'r')
            while True:
                line = f.readline().strip()
                if not line: break
                if num ==0:
                    info = str(line)
                #elif num == 1 and int(line) == 0:
                #    boola = False
                #elif num == 1 and int(line) == 1:
                #    boola = True
                #num = num +1

            rule = IoT_Rule.Rule(info)
            Rules.append(rule)
            now_rule = now_rule +1

        #multiple record
        if STATUS ==5 or STATUS ==7:
            STATUS =5
            return True

        STATUS = 3
        return True
    return False

def is_stop_check():
    global STATUS
    if STATUS == 3:
        STATUS = 4
        return True

    if STATUS == 5:
        STATUS = 6
        return True
    if STATUS ==7:
        return False
    return False

def is_begin_check():
    global STATUS

    if os.path.exists(begin_path):
        MyLog("begin")
        STATUS = 1
        return True
    return False

def is_end_check():
    global STATUS
    if STATUS == 1:
        STATUS = 2
        return True
    return False


def stop_record():
    global flow_record
    global SIG_PATH
    global Possible_record
    global Possible_Infos
    global STATUS
    global normal_flows
    global trigger_flows
    global SigFilter_result
    global Rules
    global now_rule

    MyLog("Flow"+str(STATUS) + " "+str(now_rule))

    file_name = ''
    #flow_record includes all of traffic
    if len(flow_record) > 0:
        if STATUS == 2:
            file_name = "normal_record"
            normal_flows = list(flow_record)
        if STATUS == 4 or STATUS==6:
            file_name = "trigger_record"
            trigger_flows = list(flow_record)
            MyLog("now trial "+str(Rules[now_rule].now_trial))
           # if Rules[now_rule].add_flows(trigger_flows):
           #     MyLog("Next is Multiple record")
           #     del flow_record[:]
           #     STATUS =7
           #     return
    else:
        return
    """
    f =  open(SIG_PATH + file_name, 'w')
    for i in flow_record:
        data = i.request.data
        url = data.headers["host"] + data.path +"\n"
        f.write(url)
    f.close()
    """
    del flow_record[:]

    #Sig FILTER
    SigFilter_result = list(Possible_record)

    MyLog("Possible_record")
    for i in range(0, len(Possible_record)) :
        MyLog(Possible_record[i])

    MyLog("normal_record")
    for i in range(0, len(normal_record)):
        MyLog(normal_record[i])

    #When Trigger test done
    if STATUS == 4:
        sum=0.0
        #appearance rate normal
        for i in range(0, len(normal_record)):
            sum=sum+normal_record[i]
        for i in range(0, len(normal_record)):
            normal_record[i] = normal_record[i] / sum
        sum=0.0
        for i in range(0, len(Possible_record)):
            sum=sum+Possible_record[i]
        for i in range(0, len(Possible_record)):
            Possible_record[i] = Possible_record[i] / sum
        index = -1
        max = 0
        for i in range(0, len(Possible_record)):
            if Possible_record[i] - normal_record[i] > max:
                max= Possible_record[i] - normal_record[i]
                index=i
        #Signature filter result
        Rules[now_rule].signature= Possible_Infos[index].Get_URI()
        SigFilter_result[index]=1

        MyLog("String Sig: "+Rules[now_rule].signature)


        """
        #Exclude normal flows
        for i in range(0, len(normal_record)):
            if normal_record[i] > 0:
                SigFilter_result[i]=0
        #count number of signature found
        temp = 0
        for i in range(0, len(SigFilter_result)):
            if SigFilter_result[i] > 0:
                temp = temp +1
        MyLog("Number of SIG from Sig filter :"+str(temp))
        if temp == 0:
            MyLog("ERROR INVALID INPUT")

    #CHECK multiple record
        for i in Rules:
        f =  open(SIG_PATH + "temp", 'w')
        for i in Rules[now_rule].trigger_flow1:
            data = i.request.data
            url = data.headers["host"] + data.path +"\n"
            f.write("1 "+url)
        for i in Rules[now_rule].trigger_flow2:
            data = i.request.data
            url = data.headers["host"] + data.path + "q\n"
            f.write("2 " + url)
        for i in Rules[now_rule].trigger_flow3:
            data = i.request.data
            url = data.headers["host"] + data.path + "\n"
            f.write("3 " + url)
        f.close()
        """


        #Value Filter
        Sig_parse = Possible_Infos[index].Get_URI().replace("http://","")

        pattern = re.compile(Sig_parse)

        index=0
        num_of_values = 0
        MyLog("dd"+Sig_parse)
        while Sig_parse.find("(.+)", index) != -1:
            index = Sig_parse.find("(.+)", index)+1
            MyLog("index : " + str(index))
            num_of_values = num_of_values + 1

        MyLog("numvalues : "+str(num_of_values))

        for j in trigger_flows:
            flow_uri = j.request.data.headers["host"] + j.request.data.path
            m = pattern.match(flow_uri)

            if m and flow_uri.count('/') == Sig_parse.count('/'):
                Rules[now_rule].method=j.request.data.method
                keywords = Sig_parse.split("(.+)")
                #if Sig_parse.endwith("(.+)"):
                #    keywords.append("")
                #if keywords[0] == '':
                #    del keywords[0]
                refine_keywords=list()

                for q in keywords:
                    MyLog("Keywords : "+q)
                    if q!="":
                        refine_keywords.append(q)

                temp = flow_uri
                values = list()
                k=-1
                while len(values) != num_of_values:
                    k=k+1
                    MyLog("temp : "+ temp)
                    MyLog("k: "+str(k))

                    #starts with wild card
                    if k==0 and keywords[0]=='':
                        continue
                    elif k==1 and keywords[0]=='':
                        value =temp.split(keywords[k])[0]
                        temp=temp.split(keywords[k])[1]
                    #end with wild card
                    elif len(values)==num_of_values-1 and keywords[k]=='':
                        value =temp
                    else :
                        #if len(refine_keywords)>=k+1:
                        #    value=temp.split(keywords[k])[0].split(keywords[k + 1])[0]
                        #else:
                        value = temp.split(keywords[k])[0]
                        temp = temp.split(keywords[k])[1]
                    MyLog("v"+value)
                    values.append(value)

                    Rules[now_rule].values = values

                Rules[now_rule].target_flow.append(j)
                #Fixed
                Rules[now_rule].body = j.request.get_decoded_content()
                MyLog("FlowURL :"+flow_uri)

                #Save rule data
                f = open(SIG_PATH + "Rule"+str(now_rule), 'a')
                f.write(Rules[now_rule].Rule_info + "\n")
                f.write(Rules[now_rule].method+ " "+Rules[now_rule].signature + "\n")
                for i in values:
                    f.write(i+ " ")
                f.write("\n")
                if str(Rules[now_rule].body) =="None":
                    f.write("-1")
                else:
                    f.write(str(Rules[now_rule].body))
                f.close()
                break

    STATUS = 0
    return



    #k=0
    #for i in Possible_record:
    #    MyLog("["+Possible_Infos[k].Get_URI()+"] = "+ str(i))
    #    k=k+1

def check_and_record(flow, record):
    global Possible_Infos
    global flow_record

    MyLog("chekc_is_intended ")
    flow_record.append(flow)
    flow_uri = flow.request.data.headers["host"] + flow.request.data.path
    true_list = list()
    MyLog("now_URI : "+flow_uri)
    for i in range(0,len(Possible_Infos)):
        MyLog(str(i)+ Possible_Infos[i].Get_URI())
        Sig_parse = urlparse.urlparse(Possible_Infos[i].Get_URI())
        pattern = re.compile(Sig_parse.netloc + Sig_parse.path)
        m = pattern.match(flow_uri)
        if m:
            #true_list constains index of Possible sig array which is matched to current packet.
            true_list.append(i)
    for i in true_list:
        MyLog("True_list contain "+str(i))

    templist=set()

    for i in true_list:
        MyLog("parent1 " + str(i))
        for j in Possible_Infos[i].Get_Child():
            MyLog("Now "+str(i) +" - "+ str(j))
            #If a packet is matched to parent and child, parent index should be removed [1068 line].
            if j in true_list:
                    templist.add(i)
    for i in templist:
        MyLog("templist:" +str(i))

    for i in templist:
        if i in true_list:
            true_list.remove(i)

    for i in true_list:
        MyLog("True_list contain in "+str(i))
 
    for i in true_list:
        record[i] = record[i] + 1
        #MyLog("addList " + str(i))


def is_Trigger_Request(flow):
    #MyLog(2)
    if Trigger_Matching(flow) != -1:
        return True
    return False


def Should_Resp_Be_Cached(flow):
    if Request_vs_Signature(flow) != -1:
        return True
    return False

def Keep_Resp(flow):
    global prefetched_flow
    prefetched_flow.append(flow)

def Find_Children(flow):
    global Dependency_Graph
    idx = Request_vs_Signature(flow)
    if idx == -1:
        return list()
    return Dependency_Graph[idx]

def Is_Resp_Parent(flow):
    if len(Find_Children(flow)) == 0:
        return False
    return True

def Track_JSON(js, key_list, idx, result):
    r = js.get(key_list[idx])
    if str(type(r)).find("dict") != -1:
        Track_JSON(r, key_list, idx + 1, result)
    elif str(type(r)).find("list") != -1:
        Track_List(r, key_list, idx + 1, result)
    else:
        result.append(r)
    return result

def Track_List(lst, key_list, idx, result):
    for l in lst:
        r = l.get(key_list[idx])
        if str(type(r)).find("dict") != -1:
            Track_JSON(r, key_list, idx + 1, result)
        elif str(type(r)).find("list") != -1:
            Track_List(r, key_list, idx + 1, result)
        else:
            result.append(r)
    return result

# comments: orig_key is a list.
# return value is list.
def Extract_data_from_Resp(orig_pos, orig_key, flow):
    if orig_pos == POS.HEADER:
        return str(flow.response.headers.get_all(orig_key[0]))
    elif orig_pos == POS.BODY:
        # extract json from Resp. body
        if str(flow.response.headers.get_all("content-type")).find("json") != -1:
            if (str(flow.response.headers.get_all("content-encoding")).find("gzip") != -1):
                r_str = str(zlib.decompress(flow.response.content, 16 + zlib.MAX_WBITS))
            else:
                r_str = str(flow.response.content)

            try:
                js = json.loads(r_str)

            except ValueError:
                return None

            return Track_JSON(js, orig_key, 0, list())

    return None

def Extract_data_from_Req(orig_pos, orig_key, flow):
    ret = list()
    if orig_pos == POS.HEADER:
        return flow.request.headers.get_all(orig_key[0])
    elif orig_pos == POS.BODY:
        key_value_list = str(flow.request.get_decoded_content()).split('&')
        for k_v in key_value_list:
            key_value = str(k_v).split('=')
            if str(key_value[0]) == str(orig_key[0]):
                ret.append(key_value[1])
                return ret
    return None
        

def Keep_Content_to_Req(req_idx, dp, data):
    global Request_Signatures

    if dp.req_pos == POS.URI:
        for i in range(0, len(Request_Signatures[req_idx].URI)):
            #MyLog("item: " + str(Request_Signatures[req_idx].URI[i]))
            #MyLog("Position: " + dp.req_detail_pos)
            if str(Request_Signatures[req_idx].URI[i]).find(dp.req_detail_pos) != -1:
                Request_Signatures[req_idx].URI[i] = data
                break
    elif dp.req_pos == POS.HEADER:
        Request_Signatures[req_idx].Headers[dp.req_detail_pos] = data
    elif dp.req_pos == POS.BODY:
        Request_Signatures[req_idx].Body[dp.req_detail_pos] = [data]

    return

def Extract_Content_And_Prefetch_Response(self, flow):
    global Request_Track_DP

    # 1. get index of this flow (parent flow)
    p_idx = Request_vs_Signature(flow) 
    if p_idx == -1:
        return

    # 2. get the corresponding list of children incide
    #c_list = Find_Children(flow)
    c_list = Dependency_Graph[p_idx]

    for i in c_list:
        # check if the child has empty components
        if Request_Track_DP[i].cnt > 0:
            # get dependency information for the child
            for dp in Request_Track_DP[i].dp_list:
                #dp.Print()
                if dp.parent_idx == p_idx and dp.stat == False:
                    if dp.orig_req_resp == ORIG.REQUEST:
                        data = Extract_data_from_Req(dp.orig_pos, dp.orig_key, flow)
                    else:
                        data = Extract_data_from_Resp(dp.orig_pos, dp.orig_key, flow)

                    #MyLog(data)
                    if data != None:
                        # TODO: You should generate multiple requests for the data list.
                        # Currently, only the first one in the data list is used for req construction.
                        # You should construct multiple requests for all items in the data list.
                        #Request_Signatures[i].Print()                
                        #MyLog("data (type: " + str(type(data)) + "): " + str(data))

                        Keep_Content_to_Req(i, dp, data[0])
                        dp.stat = True
                        Request_Track_DP[i].Decrease_Cnt()

                        if Request_Track_DP[i].cnt == 0:
                            #MyLog("Yes!!!! It has been done!")
                            #Request_Signatures[i].Print()                
                            t_flow = Send_Request_And_Prefetch_Response(self, Request_Signatures[i])
                            if t_flow != None:
                                Extract_Content_And_Prefetch_Response(self, t_flow)

    return

def Construct_Body(req_struct):
    body = ""
    for i in req_struct.Body:
        body = body + i + "=" + req_struct.Body[i] + "&"
    # remove last "&" by using '[:len(body)-1]'
    return body[:len(body)-1]

def Send_Request_And_Prefetch_Response(self, req_struct):
    #req_struct.Print()
    # 1. Construct Request
    # - construct URI
    URI = req_struct.Get_URI()
    #MyLog("URI: " + str(URI))
    href_parse = urlparse.urlparse(URI)
    #MyLog("Path: " + str(href_parse.path))
    #MyLog("Host: " + str(href_parse.netloc))

    # - construct headers
    hdrs = Headers(req_struct.Headers.items()) 
    #MyLog("Headers: \n" + str(hdrs))
    #body = Construct_Body(req_struct)
    #MyLog("Body: " + str(body))

    req = HTTPRequest(
                        "relative",
                         req_struct.method,
                         req_struct.scheme,
                         0,
                         0,
                         href_parse.path,
                         "HTTP/1.1",
                         hdrs,
                         "",
                         #body,
                         0,
                         0
                     )

    # - construct Body
    #MyLog("req type: " + str(type(req)))
    #content_odict = ODict()
    #for k,v in req_struct.Body.iteritems():
    #    content_odict.add(k,v)
    #MyLog("content_odict: "+str(content_odict))
    #req.urlencoded_form(content_odict)
    s = [tuple(i) for i in req_struct.Body.lst]
    #req.content = utils.urlencode(content_odict.lst)
    req.content = urllib.parse.urlencode(s, False)

    #MyLog("Test 1")
    #MyLog("Scheme: " + str(req_struct.scheme))

    if req_struct.scheme == "http":
        port = int(80)
    elif req_struct.scheme == "https":
        port = int(443)
    else:
        return None

    #MyLog("Port: "+ str(port))
    addr = socket.gethostbyname(href_parse.netloc),port
    server_conn = ServerConnection(addr)

    #MyLog("Addr: " + str(addr))
    flow = HTTPFlow(None, server_conn, HttpLayer)
    flow.request = req
    flow.request.query_string = href_parse.query
    #MyLog("Query: " + str(flow.request.query_string))


    if flow.request.query_string:
        query = flow.request.query_string
        query_dict = dict(query.split('=') for query in query.split('&'))
        query_odict = ODict()
        for k,v in query_dict.iteritems():
            #MyLog("k: " + str(k) + ", v: " + str(v))
            query_odict.add(k,v)
        flow.request.set_query(query_odict)

    #Print_Request(flow)
    # 2. Send Request for prefetching
    flow = get_response_from_server_prefetch(self, flow, req_struct.scheme)

    # 3. Append the flow
    Keep_Resp(flow)

    return flow

def get_response_from_server_prefetch(self, prefetch_flow, scheme):
    # Make sure that the incoming request matches our expectations
    #MyLog("(Before1) Scheme: " + str(prefetch_flow.request.scheme))
    self.validate_request(prefetch_flow.request)

    #MyLog("(Before2) Scheme: " + str(prefetch_flow.request.scheme))
    self.process_request_hook(prefetch_flow)

    host_header = prefetch_flow.request.headers.get("host", None)
    #MyLog("Host: " + prefetch_flow.server_conn.address.host)
    #MyLog("Port: " + str(prefetch_flow.server_conn.address.port))
    #MyLog("(After) Scheme: " + str(prefetch_flow.request.scheme))
    prefetch_flow.request.host = prefetch_flow.server_conn.address.host
    prefetch_flow.request.port = prefetch_flow.server_conn.address.port
    prefetch_flow.request.scheme = scheme
    if host_header:
        prefetch_flow.request.headers["host"] = host_header

    if not prefetch_flow.response:
        self.establish_server_connection(prefetch_flow)
        self.get_response_from_server(prefetch_flow)
    else:
        # response was set by an inline script.
        # we now need to emulate the responseheaders hook.
        prefetch_flow = self.channel.ask("responseheaders", prefetch_flow)
        if prefetch_flow == Kill:
            raise Kill()

    self.log("response", "debug", [repr(prefetch_flow.response)])
    prefetch_flow = self.channel.ask("response", prefetch_flow)
    if prefetch_flow == Kill:
        raise Kill()

    if self.check_close_connection(prefetch_flow):
        return None

    return prefetch_flow
#------------------------------------------------------------------------------#
# Response *end*

#############################################################################################
# Code by Byungkwon Choi *End*






class _HttpTransmissionLayer(Layer):

    def read_request(self):
        raise NotImplementedError()

    def read_request_body(self, request):
        raise NotImplementedError()

    def send_request(self, request):
        raise NotImplementedError()

    def read_response(self, request):
        response = self.read_response_headers()
        response.data.content = b"".join(
            self.read_response_body(request, response)
        )
        return response

    def read_response_headers(self):
        raise NotImplementedError()

    def read_response_body(self, request, response):
        raise NotImplementedError()
        yield "this is a generator"  # pragma: no cover

    def send_response(self, response):
        if response.content is None:
            raise HttpException("Cannot assemble flow with missing content")
        self.send_response_headers(response)
        self.send_response_body(response, [response.content])

    def send_response_headers(self, response):
        raise NotImplementedError()

    def send_response_body(self, response, chunks):
        raise NotImplementedError()

    def check_close_connection(self, flow):
        raise NotImplementedError()


class ConnectServerConnection(object):

    """
    "Fake" ServerConnection to represent state after a CONNECT request to an upstream proxy.
    """

    def __init__(self, address, ctx):
        self.address = tcp.Address.wrap(address)
        self._ctx = ctx

    @property
    def via(self):
        return self._ctx.server_conn

    def __getattr__(self, item):
        return getattr(self.via, item)

    def __bool__(self):
        return bool(self.via)

    if six.PY2:
        __nonzero__ = __bool__


class UpstreamConnectLayer(Layer):

    def __init__(self, ctx, connect_request):
        super(UpstreamConnectLayer, self).__init__(ctx)
        self.connect_request = connect_request
        self.server_conn = ConnectServerConnection(
            (connect_request.host, connect_request.port),
            self.ctx
        )

    def __call__(self):
        layer = self.ctx.next_layer(self)
        layer()

    def _send_connect_request(self):
        self.send_request(self.connect_request)
        resp = self.read_response(self.connect_request)
        if resp.status_code != 200:
            raise ProtocolException("Reconnect: Upstream server refuses CONNECT request")

    def connect(self):
        if not self.server_conn:
            self.ctx.connect()
            self._send_connect_request()
        else:
            pass  # swallow the message

    def change_upstream_proxy_server(self, address):
        if address != self.server_conn.via.address:
            self.ctx.set_server(address)

    def set_server(self, address, server_tls=None, sni=None):
        if self.ctx.server_conn:
            self.ctx.disconnect()
        address = tcp.Address.wrap(address)
        self.connect_request.host = address.host
        self.connect_request.port = address.port
        self.server_conn.address = address

        if server_tls:
            raise ProtocolException(
                "Cannot upgrade to TLS, no TLS layer on the protocol stack."
            )


class HttpLayer(Layer):

    def __init__(self, ctx, mode):
        super(HttpLayer, self).__init__(ctx)
        self.mode = mode

        self.__initial_server_conn = None
        "Contains the original destination in transparent mode, which needs to be restored"
        "if an inline script modified the target server for a single http request"
        # We cannot rely on server_conn.tls_established,
        # see https://github.com/mitmproxy/mitmproxy/issues/925
        self.__initial_server_tls = None

    def __call__(self):
        if self.mode == "transparent":
            self.__initial_server_tls = self._server_tls
            self.__initial_server_conn = self.server_conn
        while True:
            try:
                request = self.get_request_from_client()
                self.log("request", "debug", [repr(request)])

                # Handle Proxy Authentication
                # Proxy Authentication conceptually does not work in transparent mode.
                # We catch this misconfiguration on startup. Here, we sort out requests
                # after a successful CONNECT request (which do not need to be validated anymore)
                if self.mode != "transparent" and not self.authenticate(request):
                    return

                # Make sure that the incoming request matches our expectations
                self.validate_request(request)

                # Regular Proxy Mode: Handle CONNECT
                if self.mode == "regular" and request.first_line_format == "authority":
                    self.handle_regular_mode_connect(request)
                    return

            except HttpReadDisconnect:
                # don't throw an error for disconnects that happen before/between requests.
                return
            except NetlibException as e:
                self.send_error_response(400, repr(e))
                six.reraise(ProtocolException, ProtocolException(
                    "Error in HTTP connection: %s" % repr(e)), sys.exc_info()[2])

            try:
                flow = HTTPFlow(self.client_conn, self.server_conn, live=self)
                flow.request = request
                # set upstream auth
                if self.mode == "upstream" and self.config.upstream_auth is not None:
                    self.data.headers["Proxy-Authorization"] = self.config.upstream_auth

                self.process_request_hook(flow)

                # Code by Byungkwon *Start*
                # Code for requests
                #############################################################################
                is_resp_prefetched = False 

                #################Interoperability code#################
                Print_Request(flow)
                              

                # check whether the corresponding response has been prefetched
                if not is_resp_prefetched:
                #############################################################################
                # Code by Byungkwon *End*

                # Below part is for: transfer the request to server and get response from the server
                    if not flow.response:
                        #authentication key replacing
                        self.Augustauth(flow)
                        self.establish_server_connection(flow)
                        self.get_response_from_server(flow)
                    else:
                        #self.getAugustauth(flow)
                        # response was set by an inline script.
                        # we now need to emulate the responseheaders hook.
                        flow = self.channel.ask("responseheaders", flow)
                        if flow == Kill:
                            raise Kill()

                #print_request(flow)

                self.log("response", "debug", [repr(flow.response)])
                flow = self.channel.ask("response", flow)
                if flow == Kill:
                    raise Kill()
                self.send_response_to_client(flow)

                if self.check_close_connection(flow):
                    return

                ## Comment by Byungkwon ##
                # We can ignore following parts (Two IF statements).

                # Handle 101 Switching Protocols
                # It may be useful to pass additional args (such as the upgrade header)
                # to next_layer in the future
                if flow.response.status_code == 101:
                    layer = self.ctx.next_layer(self)
                    layer()
                    return

                # Upstream Proxy Mode: Handle CONNECT
                if flow.request.first_line_format == "authority" and flow.response.status_code == 200:
                    self.handle_upstream_mode_connect(flow.request.copy())
                    return

            except (ProtocolException, NetlibException) as e:
                self.send_error_response(502, repr(e))

                if not flow.response:
                    flow.error = Error(str(e))
                    self.channel.ask("error", flow)
                    self.log(traceback.format_exc(), "debug")
                    return
                else:
                    six.reraise(ProtocolException, ProtocolException(
                        "Error in HTTP connection: %s" % repr(e)), sys.exc_info()[2])
            finally:
                if flow:
                    flow.live = False

    def get_request_from_client(self):
        request = self.read_request()
        if request.headers.get("expect", "").lower() == "100-continue":
            # TODO: We may have to use send_response_headers for HTTP2 here.
            self.send_response(expect_continue_response)
            request.headers.pop("expect")
            request.body = b"".join(self.read_request_body(request))
        return request

    def send_error_response(self, code, message):
        try:
            response = make_error_response(code, message)
            self.send_response(response)
        except (NetlibException, H2Error):
            self.log(traceback.format_exc(), "debug")

    def change_upstream_proxy_server(self, address):
        # Make set_upstream_proxy_server always available,
        # even if there's no UpstreamConnectLayer
        if address != self.server_conn.address:
            return self.set_server(address)

    def handle_regular_mode_connect(self, request):
        self.set_server((request.host, request.port))
        self.send_response(make_connect_response(request.data.http_version))
        layer = self.ctx.next_layer(self)
        layer()

    def handle_upstream_mode_connect(self, connect_request):
        layer = UpstreamConnectLayer(self, connect_request)
        layer()

    def send_response_to_client(self, flow):
        if not flow.response.stream:
            # no streaming:
            # we already received the full response from the server and can
            # send it to the client straight away.
            self.send_response(flow.response)
        else:
            # streaming:
            # First send the headers and then transfer the response incrementally
            self.send_response_headers(flow.response)
            chunks = self.read_response_body(
                flow.request,
                flow.response
            )
            if callable(flow.response.stream):
                chunks = flow.response.stream(chunks)
            self.send_response_body(flow.response, chunks)
            flow.response.timestamp_end = utils.timestamp()

            #my_log("[A streamed chunk is sent.] Time (s): "+\
            #        str(flow.response.timestamp_end - flow.response.timestamp_start))
            #my_log("  chunk: "+str(dir(chunks)))
            #my_log("  headers: "+str(flow.response.headers))
            #my_log("  body: "+str(flow.response.body)+"\n")

    def get_response_from_server(self, flow):
        def get_response():
            self.send_request(flow.request)
            flow.response = self.read_response_headers()

        try:
            get_response()
        except NetlibException as v:
            self.log(
                "server communication error: %s" % repr(v),
                level="debug"
            )

            #f = open("/home/brad/mitm_error", 'a')
            #f.write("error: " + repr(v))
            #f.write("\n")
            #f.close()

            # In any case, we try to reconnect at least once. This is
            # necessary because it might be possible that we already
            # initiated an upstream connection after clientconnect that
            # has already been expired, e.g consider the following event
            # log:
            # > clientconnect (transparent mode destination known)
            # > serverconnect (required for client tls handshake)
            # > read n% of large request
            # > server detects timeout, disconnects
            # > read (100-n)% of large request
            # > send large request upstream
            self.disconnect()
            self.connect()
            get_response()

        # call the appropriate script hook - this is an opportunity for an
        # inline script to set flow.stream = True
        flow = self.channel.ask("responseheaders", flow)
        if flow == Kill:
            raise Kill()

        if flow.response.stream:
            flow.response.data.content = None
        else:
            flow.response.data.content = b"".join(self.read_response_body(
                flow.request,
                flow.response
            ))
        flow.response.timestamp_end = utils.timestamp()

        # no further manipulation of self.server_conn beyond this point
        # we can safely set it as the final attribute value here.
        flow.server_conn = self.server_conn

    def process_request_hook(self, flow):
        # Determine .scheme, .host and .port attributes for inline scripts.
        # For absolute-form requests, they are directly given in the request.
        # For authority-form requests, we only need to determine the request scheme.
        # For relative-form requests, we need to determine host and port as
        # well.
        if self.mode == "regular":
            pass  # only absolute-form at this point, nothing to do here.
        elif self.mode == "upstream":
            if flow.request.first_line_format == "authority":
                flow.request.scheme = "http"  # pseudo value
        else:
            # Setting request.host also updates the host header, which we want to preserve
            host_header = flow.request.headers.get("host", None)
            flow.request.host = self.__initial_server_conn.address.host
            flow.request.port = self.__initial_server_conn.address.port
            if host_header:
                flow.request.headers["host"] = host_header
            flow.request.scheme = "https" if self.__initial_server_tls else "http"

        request_reply = self.channel.ask("request", flow)
        if request_reply == Kill:
            raise Kill()
        if isinstance(request_reply, HTTPResponse):
            flow.response = request_reply
            return

    def establish_server_connection(self, flow):
        address = tcp.Address((flow.request.host, flow.request.port))
        tls = (flow.request.scheme == "https")

        if self.mode == "regular" or self.mode == "transparent":
            # If there's an existing connection that doesn't match our expectations, kill it.
            if address != self.server_conn.address or tls != self.server_conn.tls_established:
                self.set_server(address, tls, address.host)
            # Establish connection is neccessary.
            if not self.server_conn:
                self.connect()
        else:
            if not self.server_conn:
                self.connect()
            if tls:
                raise HttpProtocolException("Cannot change scheme in upstream proxy mode.")
            """
            # This is a very ugly (untested) workaround to solve a very ugly problem.
            if self.server_conn and self.server_conn.tls_established and not ssl:
                self.disconnect()
                self.connect()
            elif ssl and not hasattr(self, "connected_to") or self.connected_to != address:
                if self.server_conn.tls_established:
                    self.disconnect()
                    self.connect()

                self.send_request(make_connect_request(address))
                tls_layer = TlsLayer(self, False, True)
                tls_layer._establish_tls_with_server()
            """

    def Augustauth(self, flow):
        global augustAuth
        if augustAuth is not None:
            flow.request.headers["x-august-access-token"] = augustAuth
        else:
            req_host = flow.request.data.headers["host"]
            req_path = flow.request.data.path

            if ("api-production.august.com") in req_host and "locks" in req_path:
                augustAuth = flow.request.headers["x-august-access-token"]
        return

    def getAugustauth(self,flow):

        global augustAuth

        if augustAuth is not None:
            return

        req_host = flow.request.data.headers["host"]
        req_path = flow.request.data.path

        if ("api-production.august.com") in req_host and "locks" in req_path:
            augustAuth = flow.request.headers["x-august-access-token"]

        return

    def validate_request(self, request):
        if request.first_line_format == "absolute" and request.scheme != "http":
            raise HttpException("Invalid request scheme: %s" % request.scheme)

        expected_request_forms = {
            "regular": ("authority", "absolute",),
            "upstream": ("authority", "absolute"),
            "transparent": ("relative",)
        }

        allowed_request_forms = expected_request_forms[self.mode]
        if request.first_line_format not in allowed_request_forms:
            err_message = "Invalid HTTP request form (expected: %s, got: %s)" % (
                " or ".join(allowed_request_forms), request.first_line_format
            )
            raise HttpException(err_message)

        if self.mode == "regular" and request.first_line_format == "absolute":
            request.first_line_format = "relative"

    def authenticate(self, request):
        if self.config.authenticator:
            if self.config.authenticator.authenticate(request.headers):
                self.config.authenticator.clean(request.headers)
            else:
                self.send_response(make_error_response(
                    407,
                    "Proxy Authentication Required",
                    Headers(**self.config.authenticator.auth_challenge_headers())
                ))
                return False
        return True
