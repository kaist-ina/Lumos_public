from __future__ import print_function, absolute_import
import os
import signal
import signal
import sys
from six.moves import _thread  # PY3: We only need _thread.error, which is an alias of RuntimeError in 3.3+
from netlib.version_check import check_pyopenssl_version, check_mitmproxy_version
from . import version, cmdline
from .exceptions import ServerException
from .proxy.server import DummyServer, ProxyServer
from .proxy.config import process_proxy_options

server = None

def assert_utf8_env():
    spec = ""
    for i in ["LANG", "LC_CTYPE", "LC_ALL"]:
        spec += os.environ.get(i, "").lower()
    if "utf" not in spec:
        print(
            "Error: mitmproxy requires a UTF console environment.",
            file=sys.stderr
        )
        print(
            "Set your LANG enviroment variable to something like en_US.UTF-8",
            file=sys.stderr
        )
        sys.exit(1)


def get_server(dummy_server, options):
    if dummy_server:
        return DummyServer(options)
    else:
        try:
            return ProxyServer(options)
        except ServerException as v:
            print(str(v), file=sys.stderr)
            sys.exit(1)


def mitmproxy(args=None):  # pragma: no cover
    import time
    starttime = time.time()

    if os.name == "nt":
        print("Error: mitmproxy's console interface is not supported on Windows. "
              "You can run mitmdump or mitmweb instead.", file=sys.stderr)
        sys.exit(1)
    from . import console

    # Interoperability starting point #
    ##################################################
    from .protocol import http
    # http.Load_Request_Sig()
    #  http.Load_Visibility()
    # http.Load_Trigger_Info()
    # http.Load_Possible_Info()
    # from.protocol import dynamic_learner
    # learner = dynamic_learner.learner()
    ##################################################
    # End of starting point #


    # for debugging #
    import sys
    sys.path.append('/home/appff/pycharm-2018.2.3/debug-eggs/pycharm-debug.egg')

    from .protocol import interoperation_manager
    if interoperation_manager.InteroperationManager.debug is True:
        import pydevd
        pydevd.settrace('localhost', port=1234, suspend=False)

    from .protocol import dynamic_learner

    if os.path.exists("/usr/local/lib/python2.7/dist-packages/mitmproxy/lumos_conf/siguipair/Pairs.bin"):
        dynamic_learner.DynamicLearner.load_pairs()
        dynamic_learner.DynamicLearner.print_pairs()
    else:
        dynamic_learner.DynamicLearner.load_sig_ui_pairs()

    from datetime import timedelta

    endtime = time.time() - starttime
    f = open ("/home/appff/Documents/loadtime.txt", "w")
    f.write(str(timedelta(seconds=endtime)))
    f.close()


    check_pyopenssl_version()
    check_mitmproxy_version(version.IVERSION)
    assert_utf8_env()

    parser = cmdline.mitmproxy()
    options = parser.parse_args(args)
    if options.quiet:
        options.verbose = 0

    proxy_config = process_proxy_options(parser, options)
    console_options = console.Options(**cmdline.get_common_options(options))
    console_options.palette = options.palette
    console_options.palette_transparent = options.palette_transparent
    console_options.eventlog = options.eventlog
    console_options.follow = options.follow
    console_options.intercept = options.intercept
    console_options.limit = options.limit
    console_options.no_mouse = options.no_mouse

    global server
    server = get_server(console_options.no_server, proxy_config)

    m = console.ConsoleMaster(server, console_options)
    try:
        m.run()
    except (KeyboardInterrupt, _thread.error):
        pass


def mitmdump(args=None):  # pragma: no cover
    from . import dump

    check_pyopenssl_version()
    check_mitmproxy_version(version.IVERSION)

    parser = cmdline.mitmdump()
    options = parser.parse_args(args)
    if options.quiet:
        options.verbose = 0
        options.flow_detail = 0

    proxy_config = process_proxy_options(parser, options)
    dump_options = dump.Options(**cmdline.get_common_options(options))
    dump_options.flow_detail = options.flow_detail
    dump_options.keepserving = options.keepserving
    dump_options.filtstr = " ".join(options.args) if options.args else None

    server = get_server(dump_options.no_server, proxy_config)

    try:
        master = dump.DumpMaster(server, dump_options)

        def cleankill(*args, **kwargs):
            master.shutdown()

        signal.signal(signal.SIGTERM, cleankill)
        master.run()
    except dump.DumpError as e:
        print("mitmdump: %s" % e, file=sys.stderr)
        sys.exit(1)
    except (KeyboardInterrupt, _thread.error):
        pass


def mitmweb(args=None):  # pragma: no cover
    from . import web

    check_pyopenssl_version()
    check_mitmproxy_version(version.IVERSION)

    parser = cmdline.mitmweb()

    options = parser.parse_args(args)
    if options.quiet:
        options.verbose = 0

    proxy_config = process_proxy_options(parser, options)
    web_options = web.Options(**cmdline.get_common_options(options))
    web_options.intercept = options.intercept
    web_options.wdebug = options.wdebug
    web_options.wiface = options.wiface
    web_options.wport = options.wport
    web_options.wsingleuser = options.wsingleuser
    web_options.whtpasswd = options.whtpasswd
    web_options.process_web_options(parser)

    server = get_server(web_options.no_server, proxy_config)

    m = web.WebMaster(server, web_options)
    try:
        m.run()
    except (KeyboardInterrupt, _thread.error):
        pass
