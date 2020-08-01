import frida
import time

device = frida.get_usb_device()
pid = device.spawn(["com.smartthings.android"])
#pid = device.spawn(["com.august.luna"])
device.resume(pid)
time.sleep(1)
session = device.attach(pid)
script = session.create_script(open("smartthings.js").read())
script.load()

print ("pid : " + str(pid))

#prevent the python script from terminating
raw_input()


#process = frida.get_usb_device().attach('com.lifx.lifx')
#script = process.create_script(open("s1.js").read())
#script.load()
#raw_input()

