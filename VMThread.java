import java.net.URL;
import java.util.Random;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.CommandLineParser;

public class VMThread extends Thread
{
	VMStatus vmStatus;
	String isVMAlive=null;
	String isVMShutDownUSer=null;
	boolean isSnapshotTaken=false;
	public VMThread(){}
	public VMThread(VMStatus t)
	{
		this.vmStatus=t;
	}
	public void run(HostSystem h , VirtualMachine vm)
	{
	   System.out.println("Thread started for: "+vm.getName());
	   String IP=vm.getGuest().getIpAddress();
	   try
		{
			for(int i=0;i<1;i++)
			 {
				if(VMStatus.checkMachineStatus(h.getName()))
				{
					System.out.println(vm.getGuest().getIpAddress());
					
					if(IP==null)
					{
						try{
						Task t;
						t=vm.revertToCurrentSnapshot_Task(h);
						t.waitForTask();
						t=vm.powerOnVM_Task(h);
						if(t.waitForTask().equalsIgnoreCase("success"));
							IP=vm.getGuest().getIpAddress();
						}
						catch(Exception e)
						{
							System.out.println(e);
						}
					}
						if(VMStatus.checkMachineStatus(vm.getGuest().ipAddress))
						{
							System.out.println(vm.getName()+" is Alive");
							System.out.println(vm.getSummary().getQuickStats().overallCpuUsage);
							System.out.println(vm.getSummary().getQuickStats().getGuestMemoryUsage());
							System.out.println(vm.getNetworks());
							vmStatus.takeSnapshot(vm,"TestSnapShot"+vm.getName());
							isSnapshotTaken=true;
							isVMAlive="true";
							break;
						}
						else
						{
							if( IP!=null)
							{
								if(vm.getTriggeredAlarmState()[0].getOverallStatus().equals(ManagedEntityStatus.red))
								System.out.println("Virtual Machine shutdown by User");
								isVMShutDownUSer="true";
								break;
							}
							else
							{
							System.out.print(vm.getGuest().guestFullName+" Response for "+i+" ping is false");
							isVMAlive="false";
							isVMShutDownUSer="false";
							sleep(30000);
							}
						}
				}
				else
				{
					try
					{
					HostConnectSpec hostSpec=  new HostConnectSpec();
					hostSpec.hostName=h.getName();
					hostSpec.userName="root";
					hostSpec.password="12!@qwQW";
					hostSpec.setForce(false);
					Task t=h.reconnectHost_Task(hostSpec);
					t.waitForTask();
						if(!t.waitForTask().equalsIgnoreCase("success"))
						{
							System.out.println(h.getName());
							ServiceInstance s=VMStatus.getServiceInstance("https://130.65.132.14/sdk", "administrator", "12!@qwQW", true);
							ManagedEntity managedEntity =VMStatus.getNavigatorofInventory(s).searchManagedEntity("VirtualMachine",h.getName());
							VirtualMachine vmTemp=(VirtualMachine)managedEntity;
							HostSystem hTemp=VMStatus.getHostSystem(VMStatus.getServiceInstance("https://130.65.132.14/sdk", "administrator", "12!@qwQW", true), "cumulus3.sjsu.edu");
							Task temp=vmTemp.revertToCurrentSnapshot_Task(hTemp);
							vmTemp.powerOnVM_Task(hTemp);
							temp.waitForTask();
							h.reconnectHost_Task(hostSpec).waitForTask();
							sleep(10000);
						}
					}
					catch(Exception e)
					{
						System.out.println(e);
					}
					sleep(30000);
				}
			}
			if(VMStatus.checkMachineStatus(h.getName()))
			{
				if(!VMStatus.checkMachineStatus(vm.getGuest().ipAddress) && !isSnapshotTaken)
				{	
					
					
					if(IP!=null)
					{
						if(vm.getTriggeredAlarmState()[0].getOverallStatus().equals(ManagedEntityStatus.red))
						{
						System.out.println("Virtual Machine shutdown by User");
						isVMShutDownUSer="true";
						}
					}
					else
					{
						sleep(10000);
						if(VMStatus.checkMachineStatus(vm.getGuest().ipAddress))
						{
							isVMAlive="true";
							vmStatus.takeSnapshot(vm,"TestSnapShot"+vm.getName());
						}
						else
							System.out.print(vm.getGuest().guestFullName+isVMAlive);
					}
				}
			}
			else
			{
				HostConnectSpec hostSpec=  new HostConnectSpec();
				VMStatus.checkAvailableHosts(VMStatus.getServiceInstance("https://130.65.132.14/sdk", "administrator", "12!@qwQW", true));
				if(!VMStatus.availableHosts.isEmpty())
				{
					hostSpec.hostName=VMStatus.availableHosts.get(0);
				}
				hostSpec.userName="root";
				hostSpec.password="12!@qwQW";
				hostSpec.setForce(false);
				String ipVCenter="https://130.65.133.60/sdk";
				URL url = new URL(ipVCenter);
				Datacenter dc=VMStatus.getDatacenter(new ServiceInstance(url,"administrator","12!@qwQW",true));
				try
				{
				Task task =dc.getHostFolder().addStandaloneHost_Task(hostSpec, null, true);
				if(task.waitForTask().equalsIgnoreCase(Task.SUCCESS))
				{
					System.out.println("Addded a new Host");
					HostNasVolumeSpec hnas=new HostNasVolumeSpec();
					hnas.accessMode="readWrite";
					hnas.remoteHost="cumulus7.sjsu.edu";
					hnas.remotePath="/shares/nfs1team18";
					hnas.localPath="nfs1team18";
					try
					{
						HostSystem tempHost=VMStatus.getHostSystem(new ServiceInstance(url,"administrator","12!@qwQW",true),VMStatus.availableHosts.get(0));
						tempHost.getHostDatastoreSystem().createNasDatastore(hnas);
					}
					catch(Exception e)
					{
						System.out.println(e);
					}
				}
				else
				{
					System.out.println("Host Already Exists");
				}
				}
				catch(Exception e)
				{}
				Random r=new Random();
				Task powerOffTask=vm.powerOffVM_Task();
				powerOffTask.waitForTask();
				VMStatus.migrateVM2(vm, VMStatus.getServiceInstance("https://130.65.133.60/sdk", "administrator", "12!@qwQW", true), vm.getName(),VMStatus.availableHosts.get(0));
			}
			 if(isVMAlive!=null && isVMShutDownUSer!=null)
			 {	
				 if(isVMAlive=="false" && isVMShutDownUSer=="false")
					{
						VMStatus.createClone(vm, vm+"clone");
						System.out.println(vm.getGuest().guestFullName+" "+isVMAlive+" Will create a clone");
						vm.powerOffVM_Task().waitForTask();
						Task t=vm.destroy_Task();
						t.waitForTask();
					}
			 }
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
}
