
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;



import java.security.KeyStore.Entry;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.CommandLineParser;

import java.security.*;
import java.util.*;
import java.lang.Object;
import java.util.Map;

public class VMStatus
{
	//Login Credentials for VCenter
	static String userName="administrator";
	static String userPassword="12!@qwQW";
	//IP Address for VCenter
	static String ipVCenter="";
	static Alarm[] alarmList;
	static ManagedEntity[] managedEntity;
	public static Map<HostSystem, List<VirtualMachine>> mapHostToVM = new HashMap<HostSystem, List<VirtualMachine>>();
	static ManagedEntity[] hosts;
	static List<String> availableHosts=new ArrayList<String>();
	static HostSystem hnew;
	
	//Get Virtual Machine by giving the name of the Virtual Machine
	public static VirtualMachine getVirtualMachine( ServiceInstance serviceInstance,String vmname ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		ManagedEntity managedEntity =getNavigatorofInventory(serviceInstance).searchManagedEntity("VirtualMachine","vmname");
		return ((VirtualMachine)managedEntity);
	}
		
	public static HostSystem getHostSystem( ServiceInstance serviceInstance, String hostName ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		ManagedEntity managedEntity =getNavigatorofInventory(serviceInstance).searchManagedEntity("HostSystem",hostName);
		return ((HostSystem)managedEntity);
	}
	
	public static void getHosts( ServiceInstance serviceInstance ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		hosts = getNavigatorofInventory(serviceInstance).searchManagedEntities("HostSystem");
		
	}
	
	public static Datacenter getDatacenter( ServiceInstance serviceInstance ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		ManagedEntity managedEntity =getNavigatorofInventory(serviceInstance).searchManagedEntity("Datacenter","Lab2");
		return ((Datacenter)managedEntity);
	}
	
	public static HostSystem getHostSystem2( ServiceInstance serviceInstance ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		ManagedEntity managedEntity =getNavigatorofInventory(serviceInstance).searchManagedEntity("HostSystem","130.65.133.61");
		return ((HostSystem)managedEntity);
	}
	
	public static void mapHostToVm( ServiceInstance serviceInstance ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		ManagedEntity[] managedEntityHosts =getNavigatorofInventory(serviceInstance).searchManagedEntities("HostSystem");
		for (ManagedEntity managedEntity : managedEntityHosts) {
			HostSystem h=(HostSystem)managedEntity;
			List<VirtualMachine> temp=new ArrayList<VirtualMachine>();
			
			VirtualMachine[] vms = h.getVms();
			
			for(int i=0;i<vms.length;i++)
			{
				temp.add(vms[i]);
			}
			mapHostToVM.put(h,temp);
		}
	}
	

	public static void getVMs( ServiceInstance serviceInstance ) throws InvalidProperty, RuntimeFault, RemoteException
	{
		managedEntity =getNavigatorofInventory(serviceInstance).searchManagedEntities("VirtualMachine");
		  System.out.println(managedEntity.length);
	}
	
	public static ServiceInstance getServiceInstance(String strUrl,
			String username, String password, boolean ignoreCert) {

		ServiceInstance si = null;
		try {
			URL url = new URL(strUrl);
			si = new ServiceInstance(url, username, password, ignoreCert);
		} catch (MalformedURLException e) {
			System.err.println("...Incorrect URL");
			e.printStackTrace();
		} catch (RemoteException e) {
			System.err.println("Exception occurred while connecting");
			e.printStackTrace();
		}
		return si;
	}
	
	public static InventoryNavigator getNavigatorofInventory(ServiceInstance serviceInstance)
	{
		InventoryNavigator inventoryNavigator=new InventoryNavigator(serviceInstance.getRootFolder());
		return inventoryNavigator;
	}
	
	
	public static void getConfigurationInformation(VirtualMachine vm) throws InvalidProperty, RuntimeFault, RemoteException
	{
		System.out.println("Name of Virtual Machine : "+vm.getName());
	}
	
	//To logout of the Virtual Client
	private static void logout(ServiceInstance serviceInst) 
	{
		serviceInst.getServerConnection().logout();
	}

	//Creating Clone
	public static void createClone(VirtualMachine vm, String cloneName) throws InvalidProperty, RuntimeFault, RemoteException
	{
		System.out.println("Name of Virtual Machine : "+vm.getName());
		System.out.println("Parent of Virtual Machine : "+vm.getParent());
		VirtualMachineCloneSpec virtualMachineCloneSpec = new VirtualMachineCloneSpec();
		VirtualMachineRelocateSpec virtualMachineRelocateSpec = new VirtualMachineRelocateSpec();
		System.out.println(virtualMachineRelocateSpec);
		virtualMachineRelocateSpec.diskMoveType = "createNewChildDiskBacking";
		virtualMachineCloneSpec.setLocation(virtualMachineRelocateSpec);
		virtualMachineCloneSpec.setPowerOn(false);
		virtualMachineCloneSpec.setTemplate(false);
		try
		{
			virtualMachineCloneSpec.snapshot = vm.getCurrentSnapShot().getMOR();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		System.out.println("Cloning from " + vm.getName() + " into " + "TestClone");
		try{
		Task task = vm.cloneVM_Task((Folder)vm.getParent(), "TestClone1",
				virtualMachineCloneSpec);
		String status = task.waitForTask();

		if (status.equalsIgnoreCase(Task.SUCCESS)) 
		{
			System.out.println("Virtual Machine cloned successfully ");
		} 
		else 
		{
			TaskInfo info = task.getTaskInfo();
			System.out.println(info.getError().getFault());
			throw new RuntimeException("Error while cloning VM");
		}
		}catch(Exception e){System.out.println(e);}

	}

	public static void migrateVM2(VirtualMachine vm,
			ServiceInstance serviceInst1, String fromCloneName,String newHostIP)
			throws InvalidProperty, RuntimeFault, RemoteException,
			InterruptedException 
			{
		System.out.println("------------------------------------------");
		System.out.println("Migrating Clone : " + fromCloneName);
		System.out.println("------------------------------------------");
		
		String vmName = vm.getName();
		Folder rootFolder = serviceInst1.getRootFolder();

		VirtualMachine newVM = (VirtualMachine) new InventoryNavigator(
				rootFolder)
				.searchManagedEntity("VirtualMachine", fromCloneName); 

		HostSystem newHost = (HostSystem) new InventoryNavigator(rootFolder)
				.searchManagedEntity("HostSystem", newHostIP);
		ComputeResource cr = (ComputeResource) newHost.getParent();
		String[] checks = new String[] 
				{
				"cpu", "software" 
				};
		HostVMotionCompatibility[] vmcs = serviceInst1
				.queryVMotionCompatibility(newVM, new HostSystem[] 
						{newHost },checks);
		String[] comps = vmcs[0].getCompatibility();
		if (checks.length != comps.length) 
		{
			System.out.println("Error - CPU/software NOT compatible. Exit.");
			logout(serviceInst1);
			throw new RuntimeException(
					"Error - CPU/software NOT compatible. Exit.");
		}

		// TargetResPool, TargetHost, Priority, State
		Task task = newVM.migrateVM_Task(cr.getResourcePool(), newHost,
				VirtualMachineMovePriority.highPriority,
				VirtualMachinePowerState.poweredOff);

		if (task.waitForTask().equalsIgnoreCase(Task.SUCCESS)) 
		{
			System.out.println("Migrating : " + vmName
					+ " has been migrated successfully!");
			newVM.powerOnVM_Task(null);
		} else {
			System.out.println("VM could not be migrated!");
			TaskInfo info = task.getTaskInfo();
			System.out.println(info.getError().getFault());
			throw new RuntimeException("Error - VM could not be migrated!");
		}

	}

	
	//Take a snapshot
	public void takeSnapshot(VirtualMachine vm, String snapshotName) 
	{
		if (vm != null && !snapshotName.isEmpty()) 
		{
			String Vm_Name = vm.getName();
			System.out.println("------------------------------------------");
			System.out.println("Snapshot for " + Vm_Name);

			try 
			{
				vm.removeAllSnapshots_Task();
				
				Task task = vm.createSnapshot_Task(snapshotName,
						"Snapshot for " + Vm_Name, false, false);

				System.out.println("Current snapshot updated for " + Vm_Name);

				String status = null;

				status = task.waitForTask();

				System.out.println(task.getServerConnection());

				if (status.equalsIgnoreCase(Task.SUCCESS)) 
				{
					System.out.println("VM cloned");
				}
				else 
				{
					System.out.println("Error, VM not cloned!");
					TaskInfo info = task.getTaskInfo();
					System.out.println(info.getError().getFault());
					throw new RuntimeException("Error while cloning VM");
				}

			} 
			catch (SnapshotFault e) 
			{
				e.printStackTrace();
			} 
			catch (TaskInProgress e) 
			{
				e.printStackTrace();
			}
			catch (InvalidState e) 
			{
				e.printStackTrace();
			}
			catch (RuntimeFault e) 
			{
				e.printStackTrace();
			}
			catch (RemoteException e) 
			{
				e.printStackTrace();
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}

			System.out.println("Snapshot taken successfully -> " + Vm_Name);
			System.out.println("**********************************************");
		}
	}
	
	public static Boolean checkMachineStatus(String ip) throws IOException 
	{
		Boolean isReachable = false;
		Runtime r = Runtime.getRuntime();
			Process pingProcess = r.exec("ping " + ip);
			String pingResult = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(
					pingProcess.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) 
			{
				System.out.println(inputLine);
				pingResult += inputLine;
			}
// Ping fails
			if ((pingResult.contains("Request timed out") && !(pingResult.contains("Reply from"))) || ip==null) 
			{
				System.out.println("Host Not Found");
				isReachable = false;
			} 
// Ping Success 			
			else 
			{
				isReachable = true;
				System.out.println("Host is live");
			}
		return isReachable;
	}
	
	
	public void ManageHosts(ServiceInstance serviceInstance)
	{
		try
		{
			getHosts(serviceInstance);
			
			
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	
	public void ManageVMs(ServiceInstance serviceInstance)
	{
		//Get all the VMs in the current Virtual Center
		try
		{
			if(mapHostToVM.isEmpty())
				mapHostToVm(serviceInstance);
		
			List<VMThread> threads=new ArrayList<VMThread>();
			threads.clear();
			
			Set set = mapHostToVM.entrySet();
			Iterator it=set.iterator();
			System.out.println(mapHostToVM.size());
			 List<VirtualMachine> tempList = null;
			 while(it.hasNext()) {
				 Map.Entry me = (Map.Entry)it.next();
			      HostSystem keyObject=(HostSystem)me.getKey();
			      tempList = mapHostToVM.get(keyObject);
			      if (tempList != null) {
			         for (VirtualMachine value: tempList) {
			        	 threads.add(new VMThread(this));
			         }
			      }
			   }
			
			 int counter=0;
			 Set set1 = mapHostToVM.entrySet();
				Iterator it2=set1.iterator();
				System.out.println(mapHostToVM.size());
				 List<VirtualMachine> tempList2 = null;
				 while(it2.hasNext()) {
					 Map.Entry me = (Map.Entry)it2.next();
				      HostSystem keyObject=(HostSystem)me.getKey();
				      tempList2 = mapHostToVM.get(keyObject);
				      if (tempList2 != null) {
				         for (VirtualMachine value: tempList2) {
				        	 threads.get(counter).run((HostSystem)keyObject,(VirtualMachine)value);
								counter++;
				         }
				      }
				   }
			
			for (VMThread vmThread : threads) {
				vmThread.join();//Waiting for all threads to complete their operation
			}
		
			wait(300000);
				
			this.ManageVMs(serviceInstance);
		}
		catch(Exception e)
		{
			System.out.print(e);
		}
	}
	
	public static void createAlarmManager(VirtualMachine vm1,
			ServiceInstance serviceInst1) throws InvalidName, DuplicateName,
			RuntimeFault, RemoteException 
			{
		boolean alarmExists=false;
		System.out.println("------------------------------------------");
		 System.out.println("Creating Alarm: AlarmOnPowerOff");
		AlarmManager am = serviceInst1.getAlarmManager();
		alarmList =am.getAlarm(vm1);
		for (Alarm a : alarmList) {
			if(a.getAlarmInfo().name.contains("VmPowerStateAlarm"))
				{
					alarmExists=true;
					break;
				}
		}
		if(!alarmExists)
		{
				StateAlarmExpression sae = new StateAlarmExpression();
				sae.setType("VirtualMachine");
				sae.setStatePath("runtime.powerState");
				sae.setOperator(StateAlarmOperator.isEqual);
				sae.setRed("poweredOff");
				
				SendEmailAction action = new SendEmailAction();
				action.setToList("eshan740@gmail.com");
				action.setCcList("eshan740@gmail.com");
				action.setSubject("Alarm trigger");
				action.setBody("User powered off the VM.");
				
				AlarmTriggeringAction alarmAction = new AlarmTriggeringAction();
				alarmAction.setYellow2red(true);
				alarmAction.setAction(action);
				AlarmSetting as = new AlarmSetting();
				as.setReportingFrequency(0); // as often as possible
				as.setToleranceRange(0);
				AlarmSpec spec = new AlarmSpec();
				spec.setAction(alarmAction);
				spec.setExpression(sae);
				Random r=new Random();
				int alarmNum=r.nextInt();
				System.out.println(alarmNum);
				spec.setName("VmPowerStateAlarm"+alarmNum);
				spec.setDescription("Monitor VM state and send email if VM power's off");
				spec.setEnabled(true);
				spec.setSetting(as);
				am.createAlarm(vm1, spec);
				System.out.println("Successfully created Alarm: AlarmOnPowerOff for "+vm1.getName());
				}
	}
	
	public static void checkAvailableHosts(ServiceInstance serviceInstance) throws InvalidProperty, RuntimeFault, RemoteException
	{
		
		ManagedEntity managedEntity1 =getNavigatorofInventory(serviceInstance).searchManagedEntity("ResourcePool","Team18_vHOSTS");
		ResourcePool resourcePool=(ResourcePool)managedEntity1;
		VirtualMachine[] vmHosts=resourcePool.getVMs();
		availableHosts.clear();
		VMStatus temp=new VMStatus();
		for(int i=0;i<vmHosts.length;i++)
		{
			System.out.println(vmHosts[i].getName());
			try
			{
			if(checkMachineStatus(vmHosts[i].getName()))
			{
				temp.takeSnapshot(vmHosts[i],"hostSnap"+vmHosts[i].getName());
				availableHosts.add(vmHosts[i].getName());
			}
			}
			catch(Exception e)
			{
				
			}
		}
		
	}

	public static void main(String[] args) throws Exception 
	{
		userName="administrator";
		userPassword="12!@qwQW";
		ipVCenter="https://130.65.133.60/sdk";
		String adminIPVcenter="https://130.65.132.14/sdk";
		URL urlAdmin=new URL(adminIPVcenter);
		URL url = new URL(ipVCenter);
		VMStatus vmStatus=new VMStatus();
		ServiceInstance serviceInstance=new ServiceInstance(url,userName,userPassword,true);
		ServiceInstance serviceInstanceAdmin=new ServiceInstance(urlAdmin,userName,userPassword,true);
		//Checking for Live Hosts in Admin VCenter
		checkAvailableHosts(serviceInstanceAdmin);
		//Program begins
		mapHostToVm(serviceInstance);
		Set set = mapHostToVM.entrySet();
		Iterator it2=set.iterator();
		System.out.println(mapHostToVM.size());
		 List<VirtualMachine> tempList2 = null;
		 while(it2.hasNext()) {
			 Map.Entry me = (Map.Entry)it2.next();
		      HostSystem keyObject=(HostSystem)me.getKey();
		      tempList2 = mapHostToVM.get(keyObject);
		      if (tempList2 != null) {
		         for (VirtualMachine value: tempList2) {
		        	 createAlarmManager(value,serviceInstance);
		         }
		      }
		      
		   }
		vmStatus.ManageVMs(serviceInstance);
	}
}