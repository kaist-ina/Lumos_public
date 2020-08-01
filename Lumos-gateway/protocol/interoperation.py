import models.flow


class Interoperation:
    # Important
    # flows include learned data.
    # this means that changeable fields have already known.
    # changeable field's value is changeable.
    # thus, if a value of a key is "changeable", this module will ignore such field when tracking conditions.
    # condition_check variable is represented to appearances of each condition.
    # if actions are executed, condition_check variable will be cleared.

    conditions = []     # type: List[models.HTTPFlow]
    actions = []        # type: List[models.HTTPFlow]
    condition_check = []        # type: List[bool]

    def __init__(self):
        pass

    @staticmethod
    def add_condition(flow      # type: myRequest
                      ):
        Interoperation.conditions.append(flow)

    @staticmethod
    def add_action(flow     # type: myRequest
                   ):
        Interoperation.actions.append(flow)

    class MyRequest:
        headers = {}
        uri = ""
        method = ""
        query = ""
        postbody = {}

