class Rule:
    def __init__(self, info):
        self.trigger_flow1 = list()  #recorded flows
        self.trigger_flow2 = list()
        self.trigger_flow3 = list()
        self.is_multiple_input = False
        self.Rule_info = info
        self.now_trial = 0
        self.signature =""
        self.method =""
        self.target_flow =list()
        self.values=list()
        self.body = ""
        self.numofinteroper=-1

#    def add_flows(self, flow):
#        if self.now_trial == 0 and self.is_multiple_input!=True:
#            self.trigger_flow1=flow
#            self.now_trial = 1
#           return False
#        if self.now_trial == 0 and self.is_multiple_input:
#            self.trigger_flow1=flow
#            self.now_trial = 1
#            return True
#        elif self.now_trial ==1 and self.is_multiple_input:
#            self.trigger_flow2=flow
#            self.now_trial = 2
#            return True
#        elif self.now_trial ==2  and self.is_multiple_input:
#            self.trigger_flow3=flow
#            self.now_trial = 3
#            return False

