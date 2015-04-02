Availibility-Manager
====================

This project focuses on the concept of Disaster Management. 
The Availability Manager is used to monitor the statuses of the VMs and hosts. 
If the VM stops reacting, the program checks if the host is alive, if the host has failed, it migrates the VM to another host. 
In case no host is found, a new host is added to restart the VM.
It detects the live status of the VMs by pinging them, if the ping is successful then the VM is alive,
if it is unsuccessful or if it is timed out, the VM is considered to be dead. 
In this case, the vHost is checked for availability too, if the vHost is found dead, 
the VMs on that vHost are migrated to another vHost. If no vHost is available for the migration to occur, 
a new vHost is added to the vCenter, and the VMs are migrated to that (new) vHost.

Requirements

• To write a script to gather statistics and display them in text format

• The script must refresh the backup cache after every 5 minutes. 

• The script must also take backup of the VM and migrate the clone to the different host if 

any failure of running VM occur.

• The script must check if the vHost is alive, if not then it should try to make it alive. Even 

after many attempts it fails to revive the vHost, it should remove the vHost from the list.

• A vHost must be added to the vCenter if all the other vHosts fail. 

• The script must be able to set up an alarm if the VM gets powered off. If the user is 

powering off the machine, then the script must prevent failover from occurring.

Approach:

• To detect network failure:

Network failure is first stimulated in the virtual machine by turning off the network 
manually. First I check status to determine whether the host is connected or not than I check 
virtual machine is connected or not. If the Host is disconnected I try to connect to the Host 
from Client VCenter first if I get a failure than I log in to the Admin VCenter and revert 
the Host to the previous snapshot to bring it back to a working condition. If Still I get a 
failure and there is no other live host available in client VCenter than I add a new Host 
and a Datastore to that host in client VCenter and disconnect the failed host from the client 
VCenter. If the Virtual Machine is not responding to the ping, I again check whether the Host 
is alive or not, if yes, than I try to revert the Virtual Machine to a working state with the help 
of snapshot. If I get a error in the task to revert the Virtual machine to a working state than 
I create a clone of the Virtual Machine. In-case if the host doesn’t reply back for within a 
minute, than I perform a Cold Migration to a Live host.

• How host failures were detected:

Once we connect to the ESXI host using VSphere, we can connect to the host using the 
service instance returned by the EXSI host. By searching for the host entity using the service 
instance and by comparing the result of the host entity call we can determined whether that 
particular host is available or not.
• Able to prevent a failover from occurring when a VM is powered off by a user:
Application monitors for all shut down. If there are any no clean shut downs which are when 
anyone accidentally shut down the VM, then the application automatically tried to restart it. 
This application assumes all VM’s in the resource pool need to be running all the time and all 
other shutdown which are not done by the application manager are considered as accidental 
shut down and so automatically restarted by application. Application uses alarm function to 
notify the application when a shutdown occurs.
